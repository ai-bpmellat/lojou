package com.aiagent.plugin.agent.tools;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

/**
 * Tool: write_file
 * Args: { "path": "<file path>", "content": "<content>" }
 * Creates or overwrites a file with the given content.
 * Triggers VFS refresh so IntelliJ shows the new file immediately.
 */
public class WriteFileTool implements AgentTool {

    @Override
    public String getName() { return "write_file"; }

    @Override
    public String execute(JSONObject args, String projectPath) {
        String path    = args.optString("path", "");
        String content = args.optString("content", "");

        if (path.isEmpty())    return "ERROR: 'path' argument is required.";

        File file = ReadFileTool.resolveFile(projectPath, path);

        try {
            // Create parent directories if needed
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // For Java files, ensure the package declaration matches the directory location
            if (file.getName().endsWith(".java")) {
                String expectedPkg = deduceJavaPackage(file.getAbsolutePath());
                if (expectedPkg == null || expectedPkg.isEmpty()) {
                    expectedPkg = deduceJavaPackage(path);
                }
                content = ensureJavaPackage(content, expectedPkg);
            }

            Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);

            // Refresh VFS so IntelliJ picks up the new/changed file
            ApplicationManager.getApplication().invokeLater(() ->
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            );

            return "OK: File written successfully → " + file.getAbsolutePath() +
                   " (" + content.length() + " chars)";
        } catch (Exception e) {
            return "ERROR writing file: " + e.getMessage();
        }
    }

    /**
     * Deduce the Java package name from a file path relative to standard source folders.
     */
    public static String deduceJavaPackage(String filePath) {
        if (filePath == null) return null;
        String normalized = filePath.replace('\\', '/');
        String[] markers = {
                "/src/main/java/",
                "/src/test/java/",
                "src/main/java/",
                "src/test/java/",
                "/src/",
                "src/"
        };

        for (String marker : markers) {
            int idx = normalized.indexOf(marker);
            if (idx != -1) {
                String sub = normalized.substring(idx + marker.length());
                int lastSlash = sub.lastIndexOf('/');
                if (lastSlash != -1) {
                    return sub.substring(0, lastSlash).replace('/', '.');
                } else {
                    return ""; // directly in root source directory
                }
            }
        }
        return null;
    }

    /**
     * Ensures that Java code has the correct package declaration matching its path.
     */
    public static String ensureJavaPackage(String content, String expectedPkg) {
        if (expectedPkg == null || expectedPkg.trim().isEmpty()) {
            return content;
        }

        java.util.regex.Pattern pkgPattern = java.util.regex.Pattern.compile(
                "^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;",
                java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher m = pkgPattern.matcher(content);

        if (m.find()) {
            String currentPkg = m.group(1);
            if (!currentPkg.equals(expectedPkg)) {
                return m.replaceFirst("package " + expectedPkg + ";");
            }
            return content;
        } else {
            // Missing package -> prepend at top
            return "package " + expectedPkg + ";\n\n" + content.trim() + "\n";
        }
    }
}
