package com.aiagent.plugin.agent.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * Tool: list_files
 * Args: { "path": "<directory path>" }
 * Returns a structured list of files and subdirectories.
 */
public class ListFilesTool implements AgentTool {

    /** Max entries to return to avoid flooding context */
    private static final int MAX_ENTRIES = 200;

    @Override
    public String getName() { return "list_files"; }

    @Override
    public String execute(JSONObject args, String projectPath) {
        String path = args.optString("path", "");
        File dir = path.isEmpty()
                ? new File(projectPath)
                : ReadFileTool.resolveFile(projectPath, path);

        if (!dir.exists())   return "ERROR: Path not found: " + dir.getAbsolutePath();
        if (!dir.isDirectory()) return "ERROR: Not a directory: " + dir.getAbsolutePath();

        StringBuilder sb = new StringBuilder();
        sb.append("Directory: ").append(dir.getAbsolutePath()).append("\n");
        sb.append("─".repeat(50)).append("\n");

        File[] entries = dir.listFiles();
        if (entries == null || entries.length == 0) {
            sb.append("(empty directory)");
            return sb.toString();
        }

        // Directories first, then files
        int count = 0;
        for (File f : entries) {
            if (f.isDirectory()) {
                sb.append("📁 ").append(f.getName()).append("/\n");
                if (++count >= MAX_ENTRIES) break;
            }
        }
        for (File f : entries) {
            if (f.isFile()) {
                sb.append("📄 ").append(f.getName())
                  .append("  (").append(humanSize(f.length())).append(")\n");
                if (++count >= MAX_ENTRIES) {
                    sb.append("... (truncated at ").append(MAX_ENTRIES).append(" entries)");
                    break;
                }
            }
        }
        return sb.toString();
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
