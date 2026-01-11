package com.mvc_client.RAG;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
@RequiredArgsConstructor
public class AdvisorConfig {

    private final JdbcChatMemoryRepository chatMemoryRepository;

    @Bean
    public ChatMemory JDBCChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    private final HybridRrfDocumentRetriever hybridRrfDocumentRetriever;

    @Bean
    Advisor retrievalAugmentationAdvisor(ChatClient.Builder chatBuilder) {

        ChatClient.Builder builder = chatBuilder.defaultOptions(
                OpenAiChatOptions.builder()
                        .model("deepseek-chat")
                        .temperature(0.0)
                        .maxTokens(5120)
                        .build()
        );

        // 1) 改写
        PromptTemplate rewriteTpl = PromptTemplate.builder().template("""
仅改写下面的查询，使其更适合检索。保留关键实体，不添加解释{target}：
{query}
""").build();

        // 2) 翻译
        PromptTemplate translateTpl = PromptTemplate.builder().template("""
将下面查询翻译成{targetLanguage}，保留专有名词原文。如果本来就是{targetLanguage}，不做改变直接返回；只返回译文：
{query}
""").build();

        // 3) 多路扩展
        PromptTemplate expandTpl = PromptTemplate.builder().template("""
基于下面查询生成{number}个语义多样但等价的查询，每行一个，不要解释：
{query}
""").build();

        // 4) 上下文增强（用于“检索前的查询增强”，而非最终回答）
        PromptTemplate ctxTpl = PromptTemplate.builder().template("""
{context}

在仅有上述上下文且没有任何先验知识的情况下，回答该问题。

遵循以下规则：

如果答案不在上下文中，就直接说你不知道。

避免使用诸如“根据上下文……”或“所提供的信息……”之类的表述。

问题：{query}

回答：
""").build();

        PromptTemplate emptyCtxTpl = PromptTemplate.builder().template("{input}").build();

        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                        RewriteQueryTransformer.builder()
                                .promptTemplate(rewriteTpl)
                                .targetSearchSystem("")
                                .chatClientBuilder(builder.build().mutate())
                                .build(),
                        TranslationQueryTransformer.builder()
                                .chatClientBuilder(builder)
                                .targetLanguage("chinese")
                                .promptTemplate(translateTpl)
                                .build()
                )
                .queryExpander(
                        MultiQueryExpander.builder()
                                .chatClientBuilder(builder)
                                .numberOfQueries(3)
                                .includeOriginal(true)
                                .promptTemplate(expandTpl)
                                .build()
                )
                .documentRetriever(hybridRrfDocumentRetriever)

                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .promptTemplate(ctxTpl)
                        .emptyContextPromptTemplate(emptyCtxTpl) // 无上下文时走这个模版
                        .allowEmptyContext(true)                  // 可选：允许空上下文
                        .build())
                .build();
    }
}
