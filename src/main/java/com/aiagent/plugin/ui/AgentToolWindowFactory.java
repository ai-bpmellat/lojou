package com.aiagent.plugin.ui;

import com.aiagent.plugin.llm.LlamaServer;
import com.aiagent.plugin.settings.PluginSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory that creates the AI Agent tool window content.
 * Called by IntelliJ when the user opens the tool window.
 */
public class AgentToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ChatPanel chatPanel = new ChatPanel(project);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(chatPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Auto-start server if setting is enabled
        PluginSettings.State cfg = PluginSettings.getInstance().getState();
        if (cfg != null && cfg.autoStartServer &&
            !cfg.llamaServerPath.isEmpty() && !cfg.modelPath.isEmpty()) {
            new Thread(() -> LlamaServer.getInstance().start(), "llama-server-autostart").start();
        }
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
