package com.aiagent.plugin;

import com.aiagent.plugin.agent.AgentContext;
import com.aiagent.plugin.llm.PromptBuilder;
import org.junit.Assert;
import org.junit.Test;

public class SqlContextTest {

    @Test
    public void testSqlSystemPromptGeneration() {
        String prompt = PromptBuilder.buildSystemPrompt("/my/project", "queries/reports.sql", null);
        
        Assert.assertNotNull(prompt);
        Assert.assertTrue(prompt.contains("DataGrip"));
        Assert.assertTrue(prompt.contains("Active Editor Type: SQL / Database Script"));
        Assert.assertTrue(prompt.contains("SELECT"));
        Assert.assertTrue(prompt.contains("CREATE TABLE"));
        Assert.assertTrue(prompt.contains("SQL Rule"));
        Assert.assertFalse(prompt.contains("Current Java package"));
    }

    @Test
    public void testJavaSystemPromptGeneration() {
        String prompt = PromptBuilder.buildSystemPrompt("/my/project", "src/main/java/com/example/User.java", "com.example");
        
        Assert.assertNotNull(prompt);
        Assert.assertTrue(prompt.contains("DataGrip"));
        Assert.assertTrue(prompt.contains("Current Java package: com.example"));
        Assert.assertTrue(prompt.contains("Java Package Rule"));
    }
}
