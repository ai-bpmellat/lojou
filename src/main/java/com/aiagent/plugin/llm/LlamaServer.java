package com.aiagent.plugin.llm;

import com.aiagent.plugin.settings.PluginSettings;
import com.intellij.openapi.diagnostic.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the lifecycle of the llama-server process.
 * Starts/stops the server as a subprocess.
 */
public class LlamaServer {

    private static final Logger LOG = Logger.getInstance(LlamaServer.class);
    private static LlamaServer instance;

    private Process serverProcess;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>("");

    public static synchronized LlamaServer getInstance() {
        if (instance == null) instance = new LlamaServer();
        return instance;
    }

    /** Start the llama-server subprocess. Returns true on success. */
    public synchronized boolean start() {
        if (running.get()) return true;

        PluginSettings.State cfg = PluginSettings.getInstance().getState();
        assert cfg != null;

        if (cfg.llamaServerPath.isEmpty()) {
            lastError.set("llama-server path is not configured. Go to Settings → AI Agent.");
            return false;
        }
        if (cfg.modelPath.isEmpty()) {
            lastError.set("Model (.gguf) path is not configured. Go to Settings → AI Agent.");
            return false;
        }

        File serverExe = new File(cfg.llamaServerPath);
        if (!serverExe.exists()) {
            lastError.set("llama-server executable not found: " + cfg.llamaServerPath);
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    cfg.llamaServerPath,
                    "--model",        cfg.modelPath,
                    "--port",         String.valueOf(cfg.serverPort),
                    "--ctx-size",     String.valueOf(cfg.contextSize),
                    "--threads",      String.valueOf(cfg.threads),
                    "--n-gpu-layers", String.valueOf(cfg.gpuLayers),
                    "--host",         "127.0.0.1",
                    "--log-disable"
            );
            pb.redirectErrorStream(true);
            serverProcess = pb.start();

            // Drain output in background thread so process doesn't block
            Thread drainer = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        LOG.info("[llama-server] " + line);
                    }
                } catch (IOException ignored) {}
            }, "llama-server-output-drainer");
            drainer.setDaemon(true);
            drainer.start();

            // Wait up to 20 s for the server to become ready
            if (waitForReady(cfg.serverPort, 20_000)) {
                running.set(true);
                LOG.info("llama-server started on port " + cfg.serverPort);
                return true;
            } else {
                lastError.set("llama-server did not become ready within 20 seconds.");
                stopProcess();
                return false;
            }
        } catch (Exception e) {
            lastError.set("Failed to start llama-server: " + e.getMessage());
            LOG.error("Failed to start llama-server", e);
            return false;
        }
    }

    /** Stop the server process. */
    public synchronized void stop() {
        stopProcess();
        running.set(false);
    }

    public boolean isRunning() {
        if (!running.get()) return false;
        if (serverProcess == null || !serverProcess.isAlive()) {
            running.set(false);
            return false;
        }
        return true;
    }

    public String getLastError() { return lastError.get(); }

    // ─────────────────────────── Private Helpers ─────────────────────────────

    private void stopProcess() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            try { serverProcess.waitFor(); } catch (InterruptedException ignored) {}
            serverProcess = null;
        }
    }

    /** Poll /health endpoint until server is up or timeout expires. */
    private boolean waitForReady(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                URL url = new URL("http://127.0.0.1:" + port + "/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(500);
                conn.setReadTimeout(500);
                if (conn.getResponseCode() == 200) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
