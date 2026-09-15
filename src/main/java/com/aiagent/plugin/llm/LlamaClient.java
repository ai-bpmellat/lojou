package com.aiagent.plugin.llm;

import com.aiagent.plugin.settings.PluginSettings;
import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * HTTP client that communicates with the LLM server.
 *
 * Supports two backends (OpenAI-compatible API):
 * - llama-server (llama.cpp)  endpoint: http://127.0.0.1:{serverPort}/v1/chat/completions
 * - Ollama                    endpoint: http://127.0.0.1:{ollamaPort}/v1/chat/completions
 *
 * Both use the same OpenAI-compatible /v1/chat/completions path.
 */
public class LlamaClient {

    private static final Logger LOG = Logger.getInstance(LlamaClient.class);

    /**
     * Send a prompt and return the full response (blocking).
     * Must be called from a background thread.
     */
    public static String chat(String systemPrompt, String userMessage) throws IOException {
        PluginSettings.State cfg = PluginSettings.getInstance().getState();
        assert cfg != null;

        String endpoint = buildEndpoint(cfg);
        String modelName = cfg.useOllama ? cfg.ollamaModel : "local";

        JSONObject body = buildRequestBody(modelName, systemPrompt, userMessage, cfg, false);

        String raw = postJson(endpoint, body.toString());
        JSONObject json = new JSONObject(raw);
        return json.getJSONArray("choices")
                   .getJSONObject(0)
                   .getJSONObject("message")
                   .getString("content");
    }

    /**
     * Send a prompt and stream the response token by token (SSE).
     *
     * @param onToken  Called with each text token as it streams
     * @param onDone   Called with the full assembled response when done
     */
    public static void chatStreaming(
            String systemPrompt,
            String userMessage,
            Consumer<String> onToken,
            Consumer<String> onDone
    ) throws IOException {
        PluginSettings.State cfg = PluginSettings.getInstance().getState();
        assert cfg != null;

        String endpoint = buildEndpoint(cfg);
        String modelName = cfg.useOllama ? cfg.ollamaModel : "local";

        JSONObject body = buildRequestBody(modelName, systemPrompt, userMessage, cfg, true);
        StringBuilder fullText = new StringBuilder();

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JSONObject chunk = new JSONObject(data);
                        JSONArray choices = chunk.optJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
                            if (delta != null && delta.has("content")) {
                                String token = delta.getString("content");
                                fullText.append(token);
                                onToken.accept(token);
                            }
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not parse SSE chunk: " + data, e);
                    }
                }
            }
        }

        onDone.accept(fullText.toString());
    }

    // ─────────────────────────── Private Helpers ─────────────────────────────

    /**
     * Build the base endpoint URL based on current settings.
     * Ollama: http://127.0.0.1:11434/v1/chat/completions
     * llama-server: http://127.0.0.1:{port}/v1/chat/completions
     */
    private static String buildEndpoint(PluginSettings.State cfg) {
        int port = cfg.useOllama ? cfg.ollamaPort : cfg.serverPort;
        return "http://127.0.0.1:" + port + "/v1/chat/completions";
    }

    private static JSONObject buildRequestBody(
            String model,
            String systemPrompt,
            String userMessage,
            PluginSettings.State cfg,
            boolean stream
    ) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", cfg.temperature);
        body.put("top_p", cfg.topP);
        body.put("max_tokens", cfg.maxTokens);
        body.put("stream", stream);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userMessage));
        body.put("messages", messages);
        return body;
    }

    private static String postJson(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(120_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (code >= 400) {
            throw new IOException("HTTP " + code + ": " + sb);
        }
        return sb.toString();
    }
}
