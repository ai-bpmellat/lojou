package com.aiagent.plugin.agent;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Builds context information about the current IDE state.
 * Provides project path, currently open file, selected text, cursor position, and surrounding code.
 */
public class AgentContext {

    private final String projectPath;
    private final String currentFilePath;
    private final String selectedText;
    private final int cursorLine;
    private final String surroundingCode;

    private AgentContext(String projectPath, String currentFilePath, String selectedText, int cursorLine, String surroundingCode) {
        this.projectPath     = projectPath;
        this.currentFilePath = currentFilePath;
        this.selectedText    = selectedText;
        this.cursorLine      = cursorLine;
        this.surroundingCode = surroundingCode;
    }

    /** Build context from the currently active project state. */
    public static AgentContext from(Project project) {
        String projectPath = (project != null && project.getBasePath() != null)
                ? project.getBasePath()
                : System.getProperty("user.home");

        final String[] currentFile     = {null};
        final String[] selection       = {null};
        final int[] cursorLine         = {1};
        final String[] surroundingCode = {null};

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
                        if (editor != null) {
                            if (editor.getSelectionModel() != null && editor.getSelectionModel().hasSelection()) {
                                selection[0] = editor.getSelectionModel().getSelectedText();
                            }

                            // Capture cursor line (1-indexed)
                            int line = editor.getCaretModel().getLogicalPosition().line;
                            cursorLine[0] = line + 1;

                            // Extract surrounding lines (up to 25 lines before & after cursor)
                            Document doc = editor.getDocument();
                            int totalLines = doc.getLineCount();
                            if (totalLines > 0) {
                                int startL = Math.max(0, line - 25);
                                int endL   = Math.min(totalLines - 1, line + 25);
                                int startOffset = doc.getLineStartOffset(startL);
                                int endOffset   = doc.getLineEndOffset(endL);
                                surroundingCode[0] = doc.getText(new TextRange(startOffset, endOffset));
                            }
                        }
                    } catch (Throwable ignored) {
                        // Safe fallback - context is optional
                    }
                });
            } catch (Throwable ignored) {
                // Safe fallback - context is optional
            }
        }

        return new AgentContext(projectPath, currentFile[0], selection[0], cursorLine[0], surroundingCode[0]);
    }

    public String getProjectPath()      { return projectPath; }
    public String getCurrentFilePath()  { return currentFilePath; }
    public String getSelectedText()     { return selectedText; }
    public int getCursorLine()          { return cursorLine; }
    public String getSurroundingCode()  { return surroundingCode; }

    public String getCurrentPackage() {
        if (currentFilePath != null && currentFilePath.endsWith(".java")) {
            return com.aiagent.plugin.agent.tools.WriteFileTool.deduceJavaPackage(currentFilePath);
        }
        return null;
    }

    public boolean hasSelectedText() {
        return selectedText != null && !selectedText.trim().isEmpty();
    }

    public boolean hasSurroundingCode() {
        return surroundingCode != null && !surroundingCode.trim().isEmpty();
    }
}
