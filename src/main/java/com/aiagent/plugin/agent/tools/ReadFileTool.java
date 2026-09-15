package com.aiagent.plugin.agent.tools;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Tool: read_file
 * Args: { "path": "<file path>", "start_line": <opt>, "end_line": <opt> }
 * Returns the file content or a specific line slice.
 */
public class ReadFileTool implements AgentTool {

    /** Maximum characters to return before truncation (approx 600 lines) */
    private static final int MAX_CHARS = 25000;

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
            int startLine = args.optInt("start_line", -1);
            int endLine   = args.optInt("end_line", -1);

            if (startLine > 0 || endLine > 0) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int from = (startLine > 0) ? Math.max(0, startLine - 1) : 0;
                int to   = (endLine > 0)   ? Math.min(lines.size(), endLine) : lines.size();

                StringBuilder sb = new StringBuilder();
                sb.append("Showing lines ").append(from + 1).append(" to ").append(to)
                  .append(" of ").append(file.getName()).append(":\n");
                for (int i = from; i < to; i++) {
                    sb.append(i + 1).append(": ").append(lines.get(i)).append("\n");
                }
                return sb.toString();
            }

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS) +
                          "\n\n... [TRUNCATED at " + MAX_CHARS + " chars - total size is " + content.length() +
                          " chars. You can call read_file with 'start_line' and 'end_line' to read remaining parts] ...";
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
