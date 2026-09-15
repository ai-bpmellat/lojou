package com.aiagent.plugin.agent.tools;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Tool: edit_file
 * Args: { "path": "<path>", "old_text": "<text to find>", "new_text": "<replacement>" }
 * Replaces the first occurrence of old_text with new_text inside the file.
 */
public class EditFileTool implements AgentTool {

    @Override
    public String getName() { return "edit_file"; }

    @Override
    public String execute(JSONObject args, String projectPath) {
        String path    = args.optString("path", "");
        String oldText = args.optString("old_text", "");
        String newText = args.optString("new_text", "");

        if (path.isEmpty())    return "ERROR: 'path' argument is required.";
        if (oldText.isEmpty()) return "ERROR: 'old_text' argument is required.";

        File file = ReadFileTool.resolveFile(projectPath, path);
        if (!file.exists()) return "ERROR: File not found: " + file.getAbsolutePath();

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String content = new String(bytes, StandardCharsets.UTF_8);

            if (!content.contains(oldText)) {
                return "ERROR: The text to replace was not found in " + file.getAbsolutePath() +
                       "\nMake sure 'old_text' exactly matches content in the file (including whitespace).";
            }

            // Replace first occurrence
            String updated = content.replace(oldText, newText);
            Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));

            // Refresh VFS
            ApplicationManager.getApplication().invokeLater(() ->
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            );

            return "OK: Edit applied successfully to " + file.getAbsolutePath();
        } catch (Exception e) {
            return "ERROR editing file: " + e.getMessage();
        }
    }
}
