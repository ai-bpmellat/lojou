package com.aiagent.plugin.agent.tools;

import org.json.JSONObject;

/**
 * Interface that every agent tool must implement.
 */
public interface AgentTool {

    /** Unique name used by the LLM to call this tool (e.g., "read_file"). */
    String getName();

    /**
     * Execute the tool with the provided arguments.
     *
     * @param args       JSON object of arguments
     * @param projectPath Absolute path of the open project (used as base dir)
     * @return           String result to feed back to the LLM
     */
    String execute(JSONObject args, String projectPath);
}
