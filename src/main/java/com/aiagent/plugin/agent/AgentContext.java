package com.aiagent.plugin.agent;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Builds context information about the current IDE state.
 * Provides project path, currently open file, and selected text.
 */
public class AgentContext {

    private final String projectPath;
    private final String currentFilePath;
    private final String selectedText;

    private AgentContext(String projectPath, String currentFilePath, String selectedText) {
        this.projectPath     = projectPath;
        this.currentFilePath = currentFilePath;
        this.selectedText    = selectedText;
    }

    /** Build context from the currently active project state. */
    public static AgentContext from(Project project) {
        String projectPath = (project != null && project.getBasePath() != null)
                ? project.getBasePath()
                : System.getProperty("user.home");

        final String[] currentFile = {null};
        final String[] selection   = {null};

        if (project != null && !project.isDisposed()) {
            try {
                com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        if (project.isDisposed()) return;
                        FileEditorManager fem = FileEditorManager.getInstance(project);
                        VirtualFile[] openFiles = fem.getSelectedFiles();
                        if (openFiles != null && openFiles.length > 0 && openFiles[0] != null) {
                            currentFile[0] = openFiles[0].getPath();
                        }

                        Editor editor = fem.getSelectedTextEditor();
                        if (editor != null && editor.getSelectionModel() != null) {
                            selection[0] = editor.getSelectionModel().getSelectedText();
                        }
                    } catch (Throwable ignored) {
                        // Safe fallback - context is optional
                    }
                });
            } catch (Throwable ignored) {
                // Safe fallback - context is optional
            }
        }

        return new AgentContext(projectPath, currentFile[0], selection[0]);
    }

    public String getProjectPath()     { return projectPath; }
    public String getCurrentFilePath() { return currentFilePath; }
    public String getSelectedText()    { return selectedText; }

    public String getCurrentPackage() {
        if (currentFilePath != null && currentFilePath.endsWith(".java")) {
            return com.aiagent.plugin.agent.tools.WriteFileTool.deduceJavaPackage(currentFilePath);
        }
        return null;
    }

    public boolean hasSelectedText() {
        return selectedText != null && !selectedText.isEmpty();
    }
}
