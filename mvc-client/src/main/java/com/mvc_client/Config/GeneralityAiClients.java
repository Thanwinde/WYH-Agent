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
public class GeneralityAiClients {

    private final Advisor retrievalAugmentationAdvisor;

    @Bean("qwen-omni-turbo")
    public ChatClient qwenChatClient(
            List<McpSyncClient> mcpClients,
            RestClient.Builder rest, WebClient.Builder web,
            @Qualifier("JDBCChatMemory") ChatMemory JDBCChatMemory,
            @Value("${spring.ai.openai.chat1.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat1.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat1.options.model}") String model
    ) {
        ToolCallbackProvider mcpTools = new SyncMcpToolCallbackProvider(mcpClients);

        OpenAiApi api = new OpenAiApi(
                baseUrl,
                new SimpleApiKey(apiKey),
                authHeaders(apiKey),
                "/v1/chat/completions",
                "/v1/embeddings",
                rest, web,
                errorHandler()
        );

        OpenAiChatModel chatModel = new OpenAiChatModel(
                api,
                OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.7)
                        .maxTokens(2048)
                        .outputModalities(List.of("text"))
                        .build(),
                toolCallingManager(),
                retryTemplate(),
                ObservationRegistry.NOOP
        );
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(mcpTools)
                .defaultSystem(DefaultSystem.MULTIMODAL.getText())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(JDBCChatMemory).build(),
                        retrievalAugmentationAdvisor
                        )
                .build();
    }

    private static MultiValueMap<String, String> authHeaders(String apiKey) {

        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("Authorization", "Bearer " + apiKey);
        return headers;

    }

    private static ResponseErrorHandler errorHandler() {

        return new DefaultResponseErrorHandler();

    }

    private static ToolCallingManager toolCallingManager() {

        return ToolCallingManager.builder().build();

    }

    private static RetryTemplate retryTemplate() {

        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(200, 2.0, 2000)
                .build();

    }


}
