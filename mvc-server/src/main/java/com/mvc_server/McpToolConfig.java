package com.mvc_server;// 把工具对象注册为 MCP 工具
import com.mvc_server.Tools.Functions;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

  @Bean
  public ToolCallbackProvider tools(Functions functions) {
      System.out.println("Tool 导入...");
    return MethodToolCallbackProvider.builder()
        .toolObjects(functions)
        .build();
  }
}
