package com.mvc_client.RAG;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.SearchResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HybridRrfDocumentRetriever implements DocumentRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HybridRrfDocumentRetriever.class);

    private final RedisVectorStore redisVectorStore;
    private final JedisPooled jedisPooled;
    private final QwenRerankClient qwenRerankClient;

    @Value("${rag.redis.index-name:vector-index}")
    private String indexName;

    @Value("${rag.retrieval.bm25-top-k:20}")
    private int bm25TopK;

    @Value("${rag.retrieval.vector-top-k:20}")
    private int vectorTopK;

    @Value("${rag.retrieval.fused-top-k:20}")
    private int fusedTopK;

    @Value("${rag.retrieval.rrf-k:60}")
    private int rrfK;

    @Value("${rag.retrieval.prf-docs:3}")
    private int prfDocs;

    @Value("${rag.retrieval.second-top-k:12}")
    private int secondTopK;

    @Value("${rag.retrieval.rerank-top-n:8}")
    private int rerankTopN;

    @Override
    public List<Document> retrieve(DocumentRetriever.Query query) {
        String queryText = extractQueryText(query);
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }

        List<Document> bm25Docs = bm25Search(queryText);
        List<Document> vectorDocs = vectorSearch(queryText, vectorTopK);
        List<Document> fusedDocs = fuseWithRrf(bm25Docs, vectorDocs);
        String expandedQuery = expandQuery(queryText, fusedDocs);
        List<Document> secondPassDocs = vectorSearch(expandedQuery, secondTopK);
        return qwenRerankClient.rerank(queryText, secondPassDocs, rerankTopN);
    }

    private String extractQueryText(DocumentRetriever.Query query) {
        String[] candidates = {"query", "getQuery", "text", "getText"};
        for (String method : candidates) {
            try {
                Object value = query.getClass().getMethod(method).invoke(query);
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        logger.warn("Unable to extract query text for hybrid retrieval.");
        return null;
    }

    private List<Document> bm25Search(String queryText) {
        redis.clients.jedis.search.Query searchQuery = new redis.clients.jedis.search.Query(queryText)
                .limit(0, bm25TopK)
                .returnFields("content", "hash", "file_name")
                .setWithScores();

        try {
            SearchResult result = jedisPooled.ftSearch(indexName, searchQuery);
            return result.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("hash", doc.getString("hash"));
                        metadata.put("file_name", doc.getString("file_name"));
                        return Document.builder()
                                .id(doc.getId())
                                .text(Optional.ofNullable(doc.getString("content")).orElse(""))
                                .metadata(metadata)
                                .score(doc.getScore())
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            logger.warn("BM25 search failed, fallback to empty result: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<Document> vectorSearch(String queryText, int topK) {
        SearchRequest request = SearchRequest.query(queryText)
                .withTopK(topK)
                .withSimilarityThreshold(0.0);
        return redisVectorStore.similaritySearch(request);
    }

    private List<Document> fuseWithRrf(List<Document> bm25Docs, List<Document> vectorDocs) {
        Map<String, ScoredDocument> scored = new HashMap<>();
        applyRrfScores(scored, bm25Docs);
        applyRrfScores(scored, vectorDocs);
        return scored.values().stream()
                .sorted(Comparator.comparing(ScoredDocument::score).reversed())
                .limit(fusedTopK)
                .map(ScoredDocument::document)
                .collect(Collectors.toList());
    }

    private void applyRrfScores(Map<String, ScoredDocument> scored, List<Document> docs) {
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String id = Optional.ofNullable(doc.getId()).orElse(doc.getText());
            ScoredDocument current = scored.computeIfAbsent(id, key -> new ScoredDocument(doc, 0.0));
            current.score += 1.0 / (rrfK + i + 1.0);
        }
    }

    private String expandQuery(String originalQuery, List<Document> docs) {
        if (docs.isEmpty() || prfDocs <= 0) {
            return originalQuery;
        }
        StringBuilder builder = new StringBuilder(originalQuery);
        docs.stream()
                .limit(prfDocs)
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .forEach(text -> builder.append(' ').append(trimSnippet(text)));
        return builder.toString();
    }

    private String trimSnippet(String text) {
        int maxLength = 240;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private static class ScoredDocument {
        private final Document document;
        private double score;

        private ScoredDocument(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        private Document document() {
            return Document.builder()
                    .id(document.getId())
                    .text(document.getText())
                    .metadata(document.getMetadata())
                    .media(document.getMedia())
                    .score(score)
                    .build();
        }

        private double score() {
            return score;
        }
    }
}
