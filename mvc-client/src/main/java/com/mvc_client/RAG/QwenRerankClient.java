package com.mvc_client.RAG;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QwenRerankClient {

    private static final Logger logger = LoggerFactory.getLogger(QwenRerankClient.class);

    private final RestClient.Builder restClientBuilder;

    @Value("${rag.rerank.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${rag.rerank.api-key:${spring.ai.openai.api-key}}")
    private String apiKey;

    @Value("${rag.rerank.model:qwen3-rerank}")
    private String model;

    public List<Document> rerank(String query, List<Document> documents, int topN) {
        if (documents.isEmpty()) {
            return documents;
        }

        int limit = Math.min(topN, documents.size());
        List<String> textDocs = documents.stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        Map<String, Object> request = Map.of(
                "model", model,
                "query", query,
                "documents", textDocs,
                "top_n", limit
        );
        RestClient restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        try {
            RerankResponse response = restClient.post()
                    .uri("/v1/rerank")
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return documents;
            }

            List<Document> reranked = new ArrayList<>();
            for (RerankResponse.Result result : response.results()) {
                if (result.index() < 0 || result.index() >= documents.size()) {
                    continue;
                }
                Document original = documents.get(result.index());
                reranked.add(Document.builder()
                        .id(original.getId())
                        .text(original.getText())
                        .metadata(original.getMetadata())
                        .media(original.getMedia())
                        .score(result.relevanceScore())
                        .build());
            }

            if (!reranked.isEmpty()) {
                return reranked;
            }
        } catch (Exception ex) {
            logger.warn("Qwen rerank failed, fallback to original order: {}", ex.getMessage());
        }

        return documents.stream()
                .sorted(Comparator.comparing(Document::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private record RerankResponse(List<Result> results) {
        private record Result(int index, @JsonProperty("relevance_score") double relevanceScore) {
        }
    }
}
