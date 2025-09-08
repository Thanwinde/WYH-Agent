package com.mvc_client;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class McpHealth implements ApplicationRunner {
    List<McpSyncClient> clients;
    SyncMcpToolCallbackProvider tools;
  McpHealth(List<McpSyncClient> clients, SyncMcpToolCallbackProvider tools) {
      this.clients = clients;
      this.tools = tools;
  }
  @Override public void run(ApplicationArguments args) {
    System.out.println("MCP Clients: " + clients.size());
    for (McpSyncClient client : clients) {
        System.out.println(client.getServerInfo());
    }
    System.out.println("Tools: " + tools.getToolCallbacks().length);
      ToolCallback[] toolCallbacks = tools.getToolCallbacks();
      for (ToolCallback callback : toolCallbacks) {
        System.out.println(callback.getToolDefinition());
    }
  }
}
