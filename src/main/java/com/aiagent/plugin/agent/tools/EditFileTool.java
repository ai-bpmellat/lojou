package com.aiagent.plugin.agent.tools;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tool: edit_file
 * Args: { "path": "<path>", "old_text": "<text to find>", "new_text": "<replacement>" }
 * Replaces old_text with new_text inside the file with multi-level tolerance (CRLF/LF, whitespace).
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
            String originalContent = new String(bytes, StandardCharsets.UTF_8);

            boolean isCrlf = originalContent.contains("\r\n");

            // 1. Direct exact match
            if (originalContent.contains(oldText)) {
                String updated = replaceFirst(originalContent, oldText, newText);
                Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
                refreshVfs(file);
                return "OK: Edit applied successfully to " + file.getAbsolutePath();
            }

            // 2. Line-ending normalized match (CRLF vs LF)
            String normContent = originalContent.replace("\r\n", "\n");
            String normOldText = oldText.replace("\r\n", "\n");
            String normNewText = newText.replace("\r\n", "\n");

            if (normContent.contains(normOldText)) {
                String updated = replaceFirst(normContent, normOldText, normNewText);
                if (isCrlf) {
                    updated = updated.replace("\n", "\r\n");
                }
                Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
                refreshVfs(file);
                return "OK: Edit applied successfully to " + file.getAbsolutePath();
            }

            // 3. Trimmed-lines matching (whitespace / indentation tolerance)
            String updated = tryLineTrimmedReplace(normContent, normOldText, normNewText);
            if (updated != null) {
                if (isCrlf) {
                    updated = updated.replace("\n", "\r\n");
                }
                Files.write(file.toPath(), updated.getBytes(StandardCharsets.UTF_8));
                refreshVfs(file);
                return "OK: Edit applied successfully (with indentation matching) to " + file.getAbsolutePath();
            }

            return "ERROR: The text to replace was not found in " + file.getName() + ".\n" +
                   "Make sure to call 'read_file' first to see the exact current code, or use 'write_file' to rewrite the file.";

        } catch (Exception e) {
            return "ERROR editing file: " + e.getMessage();
        }
    }

    private static String replaceFirst(String text, String target, String replacement) {
        int index = text.indexOf(target);
        if (index < 0) return text;
        return text.substring(0, index) + replacement + text.substring(index + target.length());
    }

    private static String tryLineTrimmedReplace(String content, String oldText, String newText) {
        List<String> contentLines = new ArrayList<>(Arrays.asList(content.split("\n", -1)));
        List<String> oldLines = Arrays.asList(oldText.split("\n", -1));

        // Trim leading/trailing blank lines in oldLines
        int start = 0;
        while (start < oldLines.size() && oldLines.get(start).trim().isEmpty()) {
            start++;
        }
        int end = oldLines.size();
        while (end > start && oldLines.get(end - 1).trim().isEmpty()) {
            end--;
        }
        if (start >= end) return null;

        List<String> trimmedOld = oldLines.subList(start, end);
        int oldSize = trimmedOld.size();

        int matchStart = -1;
        for (int i = 0; i <= contentLines.size() - oldSize; i++) {
            boolean allMatch = true;
            for (int j = 0; j < oldSize; j++) {
                String cLine = contentLines.get(i + j).trim();
                String oLine = trimmedOld.get(j).trim();
                if (!cLine.equals(oLine)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                matchStart = i;
                break;
            }
        }

        if (matchStart == -1) {
            return null;
        }

        List<String> newLines = Arrays.asList(newText.split("\n", -1));
        List<String> resultLines = new ArrayList<>();
        for (int i = 0; i < matchStart; i++) {
            resultLines.add(contentLines.get(i));
        }
        resultLines.addAll(newLines);
        for (int i = matchStart + oldSize; i < contentLines.size(); i++) {
            resultLines.add(contentLines.get(i));
        }

        return String.join("\n", resultLines);
    }

    private static void refreshVfs(File file) {
        try {
            ApplicationManager.getApplication().invokeLater(() ->
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            );
        } catch (Throwable ignored) {}
    }
}
