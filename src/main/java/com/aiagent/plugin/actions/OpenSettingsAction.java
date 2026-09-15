package com.aiagent.plugin.actions;

import com.aiagent.plugin.settings.SettingsConfigurable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action: Open AI Agent Settings dialog directly.
 * Available from Tools menu: Tools → AI Agent Settings...
 */
public class OpenSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable.class);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }
}
