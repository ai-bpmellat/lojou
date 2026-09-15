package com.aiagent.plugin.agent.tools;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class EditFileToolTest {

    @Test
    public void testCrlfAndWhitespaceTolerance() throws Exception {
        File tempFile = File.createTempFile("test_edit", ".java");
        tempFile.deleteOnExit();

        // Write content with CRLF and specific indentation
        String fileContent = "public class Sample {\r\n" +
                             "    public void run() {\r\n" +
                             "        int comparison = dat0a.compareTo(root.data);\r\n" +
                             "        return;\r\n" +
                             "    }\r\n" +
                             "}\r\n";
        Files.write(tempFile.toPath(), fileContent.getBytes());

        EditFileTool tool = new EditFileTool();

        // 1. Test LF in old_text matching CRLF in file
        JSONObject args1 = new JSONObject();
        args1.put("path", tempFile.getAbsolutePath());
        args1.put("old_text", "        int comparison = dat0a.compareTo(root.data);\n        return;");
        args1.put("new_text", "        int comparison = data.compareTo(root.data);\n        return;");

        String result = tool.execute(args1, tempFile.getParent());
        Assert.assertTrue(result.startsWith("OK"));

        String updated = new String(Files.readAllBytes(tempFile.toPath()));
        Assert.assertTrue(updated.contains("data.compareTo(root.data)"));
        Assert.assertFalse(updated.contains("dat0a"));

        // 2. Test whitespace / trimmed matching
        JSONObject args2 = new JSONObject();
        args2.put("path", tempFile.getAbsolutePath());
        // LLM uses 2 spaces indentation instead of 4 spaces
        args2.put("old_text", "  int comparison = data.compareTo(root.data);");
        args2.put("new_text", "  int comparison = data.compareTo(root.data); // fixed");

        String result2 = tool.execute(args2, tempFile.getParent());
        Assert.assertTrue(result2.startsWith("OK"));

        String updated2 = new String(Files.readAllBytes(tempFile.toPath()));
        Assert.assertTrue(updated2.contains("// fixed"));
    }
}
