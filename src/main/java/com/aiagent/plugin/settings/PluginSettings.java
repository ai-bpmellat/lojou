package com.aiagent.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the AI Agent plugin.
 * Stored in IDE's application-level config (AIAgentPlugin.xml).
 */
@State(
    name = "AIAgentPluginSettings",
    storages = @Storage("AIAgentPlugin.xml")
)
public class PluginSettings implements PersistentStateComponent<PluginSettings.State> {

    public static class State {
        /** Path to llama-server executable */
        public String llamaServerPath = "";

        /** Path to the GGUF model file */
        public String modelPath = "";

        /** Port for the llama-server HTTP API */
        public int serverPort = 8787;

        /** Context size in tokens */
        public int contextSize = 8192;

        /** Number of CPU threads */
        public int threads = 4;

        /** GPU layers to offload (0 = CPU only) */
        public int gpuLayers = 0;

        /** Temperature for generation (0.0 - 2.0) */
        public float temperature = 0.7f;

        /** Top-P sampling */
        public float topP = 0.9f;

        /** Max tokens to generate per response */
        public int maxTokens = 4096;

        /** Keep llama-server running between chats */
        public boolean keepServerAlive = true;

        /** Auto-start server when plugin loads */
        public boolean autoStartServer = false;

        // ─── Ollama Mode ─────────────────────────────────────────────────────
        /**
         * When true, skip llama-server and talk directly to a running Ollama instance.
         * Ollama must be already running (ollama serve).
         */
        public boolean useOllama = true;

        /** Ollama model name (e.g. "qwen3.5:2b", "codellama:7b") */
        public String ollamaModel = "qwen3.5:2b";

        /** Ollama server port (default 11434) */
        public int ollamaPort = 11434;
    }

    private State myState = new State();

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.myState = state;
    }

    public static PluginSettings getInstance() {
        return ApplicationManager.getApplication().getService(PluginSettings.class);
    }

    /**
     * Query Ollama API for the list of downloaded models.
     */
    public static java.util.List<String> getOllamaModels(int port) {
        java.util.List<String> list = new java.util.ArrayList<>();
        try {
            java.net.URL url = new java.net.URL("http://127.0.0.1:" + port + "/api/tags");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            if (conn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray models = obj.optJSONArray("models");
                    if (models != null) {
                        for (int i = 0; i < models.length(); i++) {
                            String name = models.getJSONObject(i).optString("name");
                            if (!name.isEmpty()) list.add(name);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
