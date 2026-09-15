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
        String projectPath = project.getBasePath() != null
                ? project.getBasePath()
                : System.getProperty("user.home");

        String currentFile = null;
        String selection   = null;

        try {
            FileEditorManager fem = FileEditorManager.getInstance(project);
            VirtualFile[] openFiles = fem.getSelectedFiles();
            if (openFiles.length > 0) {
                currentFile = openFiles[0].getPath();
            }

            Editor editor = fem.getSelectedTextEditor();
            if (editor != null) {
                selection = editor.getSelectionModel().getSelectedText();
            }
        } catch (Exception ignored) {
            // Safe fallback - context is optional
        }

        return new AgentContext(projectPath, currentFile, selection);
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
