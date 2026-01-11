package com.mvc_client.Config;

import com.mvc_client.Config.constant.DefaultSystem;
import io.micrometer.observation.ObservationRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AiClients {

    private final Advisor retrievalAugmentationAdvisor;

        @Bean("qwen")
        ChatClient deepseek_chat(ChatClient.Builder chatBuilder,
                               List<McpSyncClient> mcpClients ,
                               @Qualifier("JDBCChatMemory") ChatMemory JDBCChatMemory) {
            ToolCallbackProvider mcpTools = new SyncMcpToolCallbackProvider(mcpClients);
            return chatBuilder
                    .defaultToolCallbacks(mcpTools)
                    .defaultOptions(
                            OpenAiChatOptions.builder()
                                    .model("qwen")
                                    .temperature(0.7)
                                    .maxTokens(5120)
                                    .build()
                    )
                    .defaultSystem(DefaultSystem.FILEHELPER.getText())
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(JDBCChatMemory).build(),
                            retrievalAugmentationAdvisor
                    )
                    .build();
        }


}
