package com.mvc_client.RAG;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {

    @Value("${rag.redis.index-name:vector-index}")
    private String indexName;

    @Bean
    JedisPooled jedisPooled(JedisConnectionFactory jedisConnectionFactory) {
        String host = jedisConnectionFactory.getHostName();
        int port = jedisConnectionFactory.getPort();
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().ssl(jedisConnectionFactory.isUseSsl()).clientName(jedisConnectionFactory.getClientName()).timeoutMillis(jedisConnectionFactory.getTimeout()).password(jedisConnectionFactory.getPassword()).build();
        return new JedisPooled(new HostAndPort(host, port), clientConfig);
    }

    @Bean
    RedisVectorStore redisVectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)
                .metadataFields(
                        RedisVectorStore.MetadataField.text("content"),
                        RedisVectorStore.MetadataField.tag("hash"),
                        RedisVectorStore.MetadataField.tag("file_name")
                )
                .initializeSchema(true)
                .build();
    }

}
