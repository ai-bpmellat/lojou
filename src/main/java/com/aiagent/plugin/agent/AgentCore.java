package com.aiagent.plugin.agent;

import com.aiagent.plugin.agent.tools.*;
import com.aiagent.plugin.llm.LlamaClient;
import com.aiagent.plugin.llm.LlamaServer;
import com.aiagent.plugin.llm.PromptBuilder;
import com.aiagent.plugin.settings.PluginSettings;
import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Core AI Agent logic.
 *
 * Implements a ReAct-style loop:
 *   1. Build prompt with context
 *   2. Call LLM
 *   3. Parse tool call from response
 *   4. Execute tool
 *   5. Feed result back to LLM
 *   6. Repeat until "answer" tool is called or max steps reached
 *
 * All long-running calls must happen on a background thread.
 * UI updates are delivered via the provided callbacks.
 */
public class AgentCore {

    private static final Logger LOG = Logger.getInstance(AgentCore.class);

    /** Maximum reasoning steps before giving up */
    private static final int MAX_STEPS = 12;

    // ─── Registered tools ─────────────────────────────────────────────────
    private final Map<String, AgentTool> tools = new HashMap<>();

    // ─── Callbacks (called on background thread — update UI via EDT) ───────
    /** Called when agent starts a step (e.g., "🔍 Searching code...") */
    private final Consumer<String> onStep;

    /** Called when a token arrives during streaming */
    private final Consumer<String> onToken;

    /** Called when the agent finishes with a final answer */
    private final Consumer<String> onDone;

    /** Called on error */
    private final Consumer<String> onError;

    public AgentCore(
            Consumer<String> onStep,
            Consumer<String> onDone,
            Consumer<String> onError
    ) {
        this(onStep, token -> {}, onDone, onError);
    }

    public AgentCore(
            Consumer<String> onStep,
            Consumer<String> onToken,
            Consumer<String> onDone,
            Consumer<String> onError
    ) {
        this.onStep  = onStep;
        this.onToken = onToken;
        this.onDone  = onDone;
        this.onError = onError;

        // Register all tools
        registerTool(new ReadFileTool());
        registerTool(new WriteFileTool());
        registerTool(new EditFileTool());
        registerTool(new ListFilesTool());
        registerTool(new SearchCodeTool());

        // Aliases for varied model naming styles
        tools.put("create_file", tools.get("write_file"));
        tools.put("createfile", tools.get("write_file"));
        tools.put("save_file", tools.get("write_file"));
        tools.put("savefile", tools.get("write_file"));
        tools.put("search", tools.get("search_code"));
        tools.put("grep", tools.get("search_code"));
        tools.put("find", tools.get("search_code"));
        tools.put("replace", tools.get("edit_file"));
        tools.put("modify_file", tools.get("edit_file"));
        tools.put("readfile", tools.get("read_file"));
        tools.put("writefile", tools.get("write_file"));
        tools.put("listfiles", tools.get("list_files"));
    }

    private void registerTool(AgentTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * Process a user message.  Must be called from a background thread.
     *
     * @param userMessage  What the user asked
     * @param context      Current IDE context (project path, open file, selection)
     */
    public void process(String userMessage, AgentContext context) {
        PluginSettings.State cfg = PluginSettings.getInstance().getState();
        assert cfg != null;

        // ── Ensure LLM backend is available ───────────────────────────────
        boolean isLlamaConfigured = !cfg.llamaServerPath.trim().isEmpty() && !cfg.modelPath.trim().isEmpty();
        boolean ollamaActive = cfg.useOllama || (!isLlamaConfigured && isOllamaReachable(cfg.ollamaPort));

        if (ollamaActive) {
            cfg.useOllama = true;
            if (!isOllamaReachable(cfg.ollamaPort)) {
                onError.accept("[Error] Ollama is not running on port " + cfg.ollamaPort + "!\n\n" +
                        "Please start Ollama:\n" +
                        "  1. Run: ollama serve\n" +
                        "  2. Then try again.\n\n" +
                        "Or configure llama-server in Settings -> Tools -> AI Agent.");
                return;
            }

            // Verify or auto-pick model
            java.util.List<String> models = PluginSettings.getOllamaModels(cfg.ollamaPort);
            if (!models.isEmpty()) {
                if (cfg.ollamaModel == null || cfg.ollamaModel.trim().isEmpty() || !models.contains(cfg.ollamaModel)) {
                    cfg.ollamaModel = models.contains("qwen3.5:2b") ? "qwen3.5:2b" : models.get(0);
                }
            }

            onStep.accept("[Ollama] Using model \"" + cfg.ollamaModel + "\" (port " + cfg.ollamaPort + ")");
        } else {
            // llama-server mode: start if not running
            LlamaServer server = LlamaServer.getInstance();
            if (!server.isRunning()) {
                onStep.accept("Starting llama-server...");
                if (!server.start()) {
                    onError.accept("[Error] Could not start llama-server:\n" + server.getLastError() +
                            "\n\nTip: If you have Ollama, you can enable it in Settings -> Tools -> AI Agent.");
                    return;
                }
                onStep.accept("llama-server is ready.");
            }
        }

        String systemPrompt = PromptBuilder.buildSystemPrompt(
                context.getProjectPath(),
                context.getCurrentFilePath(),
                context.getCurrentPackage()
        );

        // Maintain message history in JSONArray
        org.json.JSONArray messages = new org.json.JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));

        // Build rich user prompt with context (selection & cursor line window)
        StringBuilder fullMsg = new StringBuilder(userMessage);
        if (context.hasSelectedText()) {
            fullMsg.append("\n\n**Selected Code in Editor (line ").append(context.getCursorLine()).append("):**\n```java\n")
                   .append(context.getSelectedText()).append("\n```\n")
                   .append("INSTRUCTION: The user specifically selected this code block to fix/edit. Call 'edit_file' directly on this block.");
        } else if (context.hasSurroundingCode()) {
            fullMsg.append("\n\n**Focused Code around Cursor (line ").append(context.getCursorLine()).append("):**\n```java\n")
                   .append(context.getSurroundingCode()).append("\n```\n")
                   .append("INSTRUCTION: The user's cursor is around line ").append(context.getCursorLine()).append(". Focus your fix on this section.");
        }
        String fullUserMessage = fullMsg.toString();
        messages.put(new JSONObject().put("role", "user").put("content", fullUserMessage));

        // ── ReAct loop ────────────────────────────────────────────────────
        String lastThought = "";
        String lastCallSig = "";
        int repeatCount = 0;

        for (int step = 0; step < MAX_STEPS; step++) {
            onStep.accept("Agent thinking... (step " + (step + 1) + "/" + MAX_STEPS + ")");

            String llmResponse;
            try {
                llmResponse = LlamaClient.chat(messages);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("exceeds the available context size") && messages.length() > 2) {
                    pruneMessages(messages);
                    try {
                        llmResponse = LlamaClient.chat(messages);
                    } catch (Exception e2) {
                        onError.accept("[Error] LLM call failed: " + e2.getMessage());
                        LOG.error("LLM call failed after prune", e2);
                        return;
                    }
                } else {
                    onError.accept("[Error] LLM call failed: " + e.getMessage());
                    LOG.error("LLM call failed", e);
                    return;
                }
            }

            // ── Parse JSON from LLM response ──────────────────────────────
            JSONObject parsed = extractJson(llmResponse);
            if (parsed == null) {
                // If model failed to return valid JSON on early steps, nudge it to return JSON
                if (step < 2) {
                    messages.put(new JSONObject().put("role", "assistant").put("content", llmResponse));
                    messages.put(new JSONObject().put("role", "user").put("content",
                            "ERROR: You must respond ONLY with a valid JSON tool call. Example: {\"thought\": \"...\", \"tool\": \"read_file\", \"args\": {\"path\": \"...\"}}. Output the JSON now:"));
                    continue;
                }
                onDone.accept(llmResponse.trim());
                return;
            }

            String toolName = parsed.optString("tool", "").trim().toLowerCase();
            JSONObject toolArgs = parsed.optJSONObject("args");
            if (toolArgs == null) {
                if (parsed.has("parameters")) toolArgs = parsed.optJSONObject("parameters");
                else if (parsed.has("arguments")) toolArgs = parsed.optJSONObject("arguments");
            }
            if (toolArgs == null) toolArgs = new JSONObject();

            // Fallbacks for key names used by smaller models
            if (toolName.isEmpty() && toolArgs.has("tool")) {
                toolName = toolArgs.optString("tool", "").trim().toLowerCase();
            }
            if (toolName.isEmpty() && parsed.has("action")) {
                toolName = parsed.optString("action", "").trim().toLowerCase();
            }
            if (toolName.isEmpty() && parsed.has("name")) {
                toolName = parsed.optString("name", "").trim().toLowerCase();
            }
            if (toolName.isEmpty() && parsed.has("function")) {
                toolName = parsed.optString("function", "").trim().toLowerCase();
            }

            if (toolArgs.isEmpty() && (parsed.has("path") || parsed.has("old_text") || parsed.has("content") || parsed.has("query"))) {
                toolArgs = parsed;
            }

            // ── Intelligent Heuristics for Small Models (0.5b / 2b) ─────────
            // If arguments clearly indicate the tool, override the tool name
            if (toolArgs.has("old_text") || toolArgs.has("new_text")) {
                toolName = "edit_file";
            } else if (toolArgs.has("content") && !"edit_file".equals(toolName)) {
                toolName = "write_file";
            } else if (toolArgs.has("query") && !"edit_file".equals(toolName)) {
                toolName = "search_code";
            } else if (("list_files".equals(toolName) || "listfiles".equals(toolName)) && toolArgs.has("path")) {
                String p = toolArgs.optString("path", "");
                if (p.endsWith(".java") || p.endsWith(".kt") || p.endsWith(".xml") || p.endsWith(".txt") || p.endsWith(".md")) {
                    toolName = "read_file";
                }
            }

            String currentThought = parsed.optString("thought", "").trim();
            if (!currentThought.isEmpty()) {
                lastThought = currentThought;
            }

            // If the model produced thought but NO tool:
            if (toolName.isEmpty()) {
                // Did it explicitly provide an answer or text field?
                String explicitAnswer = parsed.optString("answer", parsed.optString("text", parsed.optString("response", "")));
                if (!explicitAnswer.isEmpty()) {
                    onDone.accept(explicitAnswer);
                    return;
                }

                // If it only outputted thought, do NOT terminate! Prompt it to specify the tool
                if (step < MAX_STEPS - 1) {
                    messages.put(new JSONObject().put("role", "assistant").put("content", llmResponse));
                    messages.put(new JSONObject().put("role", "user").put("content",
                            "You specified a 'thought', but did not provide 'tool' or 'args'. " +
                            "To inspect or edit code, call 'read_file' or 'edit_file'. " +
                            "To finish, call 'answer'. " +
                            "Output format: {\"thought\": \"...\", \"tool\": \"edit_file\", \"args\": {\"path\": \"...\", \"old_text\": \"...\", \"new_text\": \"...\"}}"));
                    continue;
                }
            }

            // ── Loop Breaker ──────────────────────────────────────────────
            String callSig = toolName + ":" + toolArgs.toString();
            if (callSig.equals(lastCallSig)) {
                repeatCount++;
                if (repeatCount >= 2) {
                    // Small model is looping on the same call — break out!
                    if ("read_file".equals(toolName) || "list_files".equals(toolName) || "search_code".equals(toolName)) {
                        messages.put(new JSONObject().put("role", "assistant").put("content", llmResponse));
                        messages.put(new JSONObject().put("role", "user").put("content",
                                "Stop repeating '" + toolName + "'. Proceed directly to call 'edit_file' to fix the code, or call 'answer'."));
                        continue;
                    }
                }
            } else {
                repeatCount = 0;
                lastCallSig = callSig;
            }

            // Check if this is an answer or final response
            boolean isAnswer = "answer".equals(toolName)
                    || "none".equals(toolName)
                    || "null".equals(toolName)
                    || "finish".equals(toolName)
                    || "final_answer".equals(toolName)
                    || "response".equals(toolName)
                    || "message".equals(toolName);

            if (isAnswer) {
                String answer = toolArgs.optString("text", toolArgs.optString("answer", toolArgs.optString("response", "")));
                if (answer.isEmpty()) answer = parsed.optString("text", parsed.optString("answer", parsed.optString("response", "")));
                if (answer.isEmpty()) answer = parsed.optString("thought", llmResponse.trim());

                onDone.accept(answer);
                return;
            }

            // ── Execute tool ───────────────────────────────────────────────
            AgentTool tool = tools.get(toolName);
            if (tool == null) {
                messages.put(new JSONObject().put("role", "assistant").put("content", llmResponse));
                messages.put(new JSONObject().put("role", "user").put("content",
                        "Tool '" + toolName + "' does not exist. Available tools: " +
                        String.join(", ", tools.keySet()) + ", answer."));
                continue;
            }

            String thought = currentThought;
            String stepInfo = "Using tool: " + toolName +
                    (toolArgs.has("path") ? " (" + toolArgs.optString("path") + ")" : "");
            if (!thought.isEmpty()) {
                onStep.accept(thought + "\n\n🛠 " + stepInfo);
            } else {
                onStep.accept(stepInfo);
            }

            String toolResult = tool.execute(toolArgs, context.getProjectPath());

            // ── Fast-Path Optimization ────────────────────────────────────
            // For file modifications (write_file / edit_file), if execution succeeded with OK:,
            // terminate immediately without wasting another LLM generation roundtrip!
            if (("write_file".equals(toolName) || "edit_file".equals(toolName) || "create_file".equals(toolName) || "save_file".equals(toolName) || "replace".equals(toolName)) && toolResult != null && toolResult.startsWith("OK:")) {
                String thoughtMsg = thought.isEmpty() ? "تغییرات با موفقیت روی فایل اعمال شد." : thought;
                onDone.accept(thoughtMsg + "\n\n" + toolResult);
                return;
            }

            // ── Feed result back to LLM with full conversation history ───
            messages.put(new JSONObject().put("role", "assistant").put("content", llmResponse));
            messages.put(new JSONObject().put("role", "user").put("content",
                    PromptBuilder.buildToolResultMessage(toolName, toolResult)));
        }

        if (!lastThought.isEmpty()) {
            onDone.accept(lastThought);
        } else {
            onError.accept("Agent reached maximum steps (" + MAX_STEPS + ") without completing.");
        }
    }

    // ─────────────────────────── Private Helpers ─────────────────────────────

    /**
     * Check if Ollama is reachable by pinging its health endpoint.
     */
    private boolean isOllamaReachable(int port) {
        try {
            java.net.URL url = new java.net.URL("http://127.0.0.1:" + port + "/api/tags");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract a valid JSON object from the LLM response, with high resilience against:
     * - Markdown code fences (```json ... ```)
     * - Unescaped raw newlines/tabs inside string literals
     * - Truncated responses (auto-close open strings and braces)
     * - Damaged syntax via regex fallback
     */
    private JSONObject extractJson(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;

        // 1. Direct parse attempt
        try {
            return new JSONObject(text);
        } catch (Exception ignored) {}

        // 2. Find outermost JSON start
        int start = text.indexOf('{');
        if (start < 0) {
            return extractWithRegex(text);
        }

        // 3. Scan & sanitize string literals (escape raw newlines, tabs) and track brace depth
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        int braceDepth = 0;
        boolean reachedZeroBrace = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                sb.append(c);
                continue;
            }

            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                    if (braceDepth == 0) {
                        sb.append(c);
                        reachedZeroBrace = true;
                        break; // Successfully closed root object!
                    }
                }
                sb.append(c);
            } else {
                // Inside string literal: escape control characters that violate JSON spec
                if (c == '\n') {
                    sb.append("\\n");
                } else if (c == '\r') {
                    // skip CR
                } else if (c == '\t') {
                    sb.append("\\t");
                } else {
                    sb.append(c);
                }
            }
        }

        // If JSON was cut off / truncated before closing:
        if (!reachedZeroBrace) {
            if (inString) {
                sb.append("\""); // Close open string
            }
            while (braceDepth > 0) {
                sb.append("}"); // Close unclosed objects
                braceDepth--;
            }
        }

        try {
            return new JSONObject(sb.toString());
        } catch (Exception ignored) {}

        // 4. Fallback: regex extraction
        return extractWithRegex(text);
    }

    /**
     * Regex fallback for extracting tool calls even from badly malformed/truncated output.
     */
    private JSONObject extractWithRegex(String text) {
        try {
            java.util.regex.Pattern toolPat = java.util.regex.Pattern.compile(
                    "\"(?:tool|action|name|function)\"\\s*:\\s*\"([^\"]+)\"",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher m = toolPat.matcher(text);
            if (!m.find()) return null;

            String tool = m.group(1).trim();
            JSONObject root = new JSONObject();
            root.put("tool", tool);
            JSONObject args = new JSONObject();

            // Extract thought if present
            java.util.regex.Matcher thoughtMat = java.util.regex.Pattern.compile("\"thought\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (thoughtMat.find()) {
                root.put("thought", thoughtMat.group(1));
            }

            // Extract path if present
            java.util.regex.Matcher pathMat = java.util.regex.Pattern.compile("\"path\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (pathMat.find()) {
                args.put("path", pathMat.group(1));
            }

            // Extract query if search_code
            java.util.regex.Matcher qMat = java.util.regex.Pattern.compile("\"query\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (qMat.find()) {
                args.put("query", qMat.group(1));
            }

            // Extract text/answer if answer tool
            java.util.regex.Matcher textMat = java.util.regex.Pattern.compile("\"(?:text|answer|response)\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (textMat.find()) {
                args.put("text", textMat.group(1));
            }

            // Extract content if write_file
            int contentIdx = text.indexOf("\"content\":");
            if (contentIdx != -1) {
                int quoteStart = text.indexOf('"', contentIdx + 10);
                if (quoteStart != -1) {
                    String rest = text.substring(quoteStart + 1);
                    int quoteEnd = -1;
                    for (int j = 0; j < rest.length(); j++) {
                        if (rest.charAt(j) == '"' && (j == 0 || rest.charAt(j - 1) != '\\')) {
                            quoteEnd = j;
                            break;
                        }
                    }
                    String content = (quoteEnd != -1) ? rest.substring(0, quoteEnd) : rest;
                    args.put("content", content.replace("\\n", "\n").replace("\\t", "\t"));
                }
            }

            // Extract old_text and new_text if edit_file
            java.util.regex.Matcher oldMat = java.util.regex.Pattern.compile("\"old_text\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (oldMat.find()) args.put("old_text", oldMat.group(1));
            java.util.regex.Matcher newMat = java.util.regex.Pattern.compile("\"new_text\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
            if (newMat.find()) args.put("new_text", newMat.group(1));

            root.put("args", args);
            return root;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Prunes older or large messages in history to fit context size when an exceed error occurs.
     */
    private void pruneMessages(org.json.JSONArray messages) {
        for (int i = 1; i < messages.length(); i++) {
            JSONObject msg = messages.optJSONObject(i);
            if (msg != null) {
                String c = msg.optString("content", "");
                if (c.length() > 2000) {
                    msg.put("content", c.substring(0, 1500) + "\n\n... [Content trimmed to fit context window] ...");
                }
            }
        }
    }
}
