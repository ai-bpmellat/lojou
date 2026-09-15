package com.aiagent.plugin.agent.tools;

import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;

/**
 * Tool: read_file
 * Args: { "path": "<file path>" }
 * Returns the file content as a string (truncated to 8000 chars if large).
 */
public class ReadFileTool implements AgentTool {

    @Override
    public String getName() { return "read_file"; }

    @Override
    public String execute(JSONObject args, String projectPath) {
        String path = args.optString("path", "");
        if (path.isEmpty()) return "ERROR: 'path' argument is required.";

        File file = resolveFile(projectPath, path);
        if (!file.exists()) return "ERROR: File not found: " + file.getAbsolutePath();
        if (!file.isFile()) return "ERROR: Not a file: " + file.getAbsolutePath();

        try {
            String content = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            // Truncate very large files to avoid exceeding context
            if (content.length() > 8000) {
                content = content.substring(0, 8000) +
                          "\n\n... [TRUNCATED - file is " + content.length() + " chars total] ...";
            }
            return content;
        } catch (Exception e) {
            return "ERROR reading file: " + e.getMessage();
        }
    }

    static File resolveFile(String projectPath, String path) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(projectPath, path);
    }
}
