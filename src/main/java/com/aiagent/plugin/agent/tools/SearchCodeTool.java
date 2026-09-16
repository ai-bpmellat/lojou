package com.aiagent.plugin.agent.tools;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool: search_code
 * Args: { "query": "<search text>" }
 * Searches all text files in the project for the given query.
 * Returns up to 30 matches with file + line number.
 */
public class SearchCodeTool implements AgentTool {

    private static final int MAX_RESULTS = 10;

    /** File extensions to search (text-based source files) */
    private static final List<String> SEARCHABLE_EXTENSIONS = List.of(
            ".java", ".kt", ".groovy", ".xml", ".json", ".yaml", ".yml",
            ".properties", ".txt", ".md", ".py", ".js", ".ts", ".html",
            ".css", ".scss", ".gradle", ".kts", ".toml", ".sql", ".ddl", ".dml"
    );

    /** Directories to skip */
    private static final List<String> SKIP_DIRS = List.of(
            ".git", ".idea", "build", "out", "target", ".gradle",
            "node_modules", "__pycache__", ".venv"
    );

    @Override
    public String getName() { return "search_code"; }

    @Override
    public String execute(JSONObject args, String projectPath) {
        String query = args.optString("query", "").trim();
        if (query.isEmpty()) return "ERROR: 'query' argument is required.";

        List<String> results = new ArrayList<>();
        try {
            searchDirectory(new File(projectPath), query, projectPath, results);
        } catch (Exception e) {
            return "ERROR during search: " + e.getMessage();
        }

        if (results.isEmpty()) {
            return "No matches found for: \"" + query + "\"";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" match(es) for: \"").append(query).append("\"\n");
        sb.append("─".repeat(60)).append("\n");
        for (String r : results) sb.append(r).append("\n");
        if (results.size() >= MAX_RESULTS) {
            sb.append("... (showing first ").append(MAX_RESULTS).append(" matches)");
        }
        return sb.toString();
    }

    // ─────────────────────────── Private ────────────────────────────────────

    private void searchDirectory(File dir, String query, String projectRoot, List<String> results) throws IOException {
        if (results.size() >= MAX_RESULTS) return;

        File[] entries = dir.listFiles();
        if (entries == null) return;

        for (File f : entries) {
            if (results.size() >= MAX_RESULTS) return;

            if (f.isDirectory()) {
                if (!SKIP_DIRS.contains(f.getName())) {
                    searchDirectory(f, query, projectRoot, results);
                }
            } else if (f.isFile() && isSearchable(f.getName())) {
                searchFile(f, query, projectRoot, results);
            }
        }
    }

    private void searchFile(File file, String query, String projectRoot, List<String> results) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        String lowerQuery = query.toLowerCase();

        for (int i = 0; i < lines.size() && results.size() < MAX_RESULTS; i++) {
            if (lines.get(i).toLowerCase().contains(lowerQuery)) {
                String relativePath = new File(projectRoot).toURI()
                        .relativize(file.toURI()).getPath();
                results.add(String.format("%-50s line %d:  %s",
                        relativePath, i + 1, lines.get(i).trim()));
            }
        }
    }

    private boolean isSearchable(String name) {
        String lower = name.toLowerCase();
        return SEARCHABLE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
