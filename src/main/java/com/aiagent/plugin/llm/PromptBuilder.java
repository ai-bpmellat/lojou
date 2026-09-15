package com.aiagent.plugin.llm;

/**
 * Builds structured prompts for the AI agent.
 * Defines the system prompt including tool descriptions and output format.
 *
 * Compatible with:
 * - llama-server (llama.cpp)  → port configured in settings
 * - Ollama                    → typically port 11434
 */
public class PromptBuilder {

    /**
     * Build the system prompt for the agent.
     *
     * @param projectPath   Absolute path of the open project
     * @param currentFile   Currently focused file (may be null)
     * @return              Full system prompt string
     */
    public static String buildSystemPrompt(String projectPath, String currentFile) {
        return buildSystemPrompt(projectPath, currentFile, null);
    }

    public static String buildSystemPrompt(String projectPath, String currentFile, String currentPackage) {
        return "You are offAiAgent, an expert offline AI coding assistant embedded inside IntelliJ IDEA.\n" +
               "You help the user write, read, edit, and understand code.\n\n" +
               "## Project Info\n" +
               "- Project path: " + projectPath + "\n" +
               (currentFile != null ? "- Currently open file: " + currentFile + "\n" : "") +
               (currentPackage != null && !currentPackage.isEmpty() ? "- Current Java package: " + currentPackage + "\n" : "") +
               "\n" +
               "## Available Tools\n" +
               "You can use the following tools to interact with the project files:\n\n" +
               "1. `read_file`    – Read the content of a file.\n" +
               "   Args: { \"path\": \"<relative-or-absolute-path>\" }\n\n" +
               "2. `write_file`   – Create or overwrite a file with content.\n" +
               "   Args: { \"path\": \"<path>\", \"content\": \"<complete file content>\" }\n\n" +
               "3. `edit_file`    – Replace a specific text block inside a file.\n" +
               "   Args: { \"path\": \"<path>\", \"old_text\": \"<exact text to replace>\", \"new_text\": \"<replacement>\" }\n\n" +
               "4. `list_files`   – List files and directories at a given path.\n" +
               "   Args: { \"path\": \"<directory path>\" }\n\n" +
               "5. `search_code`  – Search for a text pattern across all project files.\n" +
               "   Args: { \"query\": \"<search text>\" }\n\n" +
               "6. `answer`       – Respond to the user with a final answer (no more tools needed).\n" +
               "   Args: { \"text\": \"<your answer to the user>\" }\n\n" +
               "## Response Format\n" +
               "ALWAYS respond ONLY with a single JSON object. No markdown text before or after the JSON.\n" +
               "```\n" +
               "{\n" +
               "  \"thought\": \"brief 1-2 sentence reasoning\",\n" +
               "  \"tool\": \"<tool name>\",\n" +
               "  \"args\": { ... }\n" +
               "}\n" +
               "```\n\n" +
               "## Rules\n" +
               "- Fast Action Rule: Take action directly in Step 1. If creating a new file, call `write_file` immediately. If editing an existing file whose exact code you haven't seen, call `read_file` first to inspect the lines to change, or use `write_file` with the complete code.\n" +
               "- Java Package Rule: ALWAYS include the correct `package <name>;` declaration at the very top of any Java file, matching its directory under `src/main/java/` (e.g. `package test;` for `src/main/java/test/BST.java`). Never omit package statements.\n" +
               "- Keep \"thought\" extremely concise (1 short sentence) to save token processing time.\n" +
               "- Inside JSON strings (like `content`), properly escape newlines as \\n and quotes as \\\".\n" +
               "- If no file tool is needed, or you are directly answering or explaining, ALWAYS use the `answer` tool:\n" +
               "  { \"thought\": \"I will provide the answer\", \"tool\": \"answer\", \"args\": { \"text\": \"<your response>\" } }\n" +
               "- When you are done using tools, call the `answer` tool.\n" +
               "- Use tools step by step. After each tool result, continue reasoning.\n" +
               "- Always use relative paths from the project root when possible.\n" +
               "- Write clean, well-commented, complete idiomatic code without omitting parts.\n" +
               "- If the user's language is Persian (Farsi), respond in Persian in the answer/thought.\n";
    }

    /**
     * Build a follow-up message that includes a tool result.
     *
     * @param toolName      Name of the tool that was called
     * @param toolResult    Output returned by the tool
     * @return              String to inject back into the conversation
     */
    public static String buildToolResultMessage(String toolName, String toolResult) {
        return "Tool `" + toolName + "` result:\n```\n" + toolResult + "\n```\n" +
               "Now continue with your next JSON action.";
    }
}
