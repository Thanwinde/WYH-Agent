package com.mvc_client.RAG;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {

    private JedisPooled jedisPooled(JedisConnectionFactory jedisConnectionFactory) {
        String host = jedisConnectionFactory.getHostName();
        int port = jedisConnectionFactory.getPort();
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().ssl(jedisConnectionFactory.isUseSsl()).clientName(jedisConnectionFactory.getClientName()).timeoutMillis(jedisConnectionFactory.getTimeout()).password(jedisConnectionFactory.getPassword()).build();
        return new JedisPooled(new HostAndPort(host, port), clientConfig);
    }

    @Bean
    RedisVectorStore redisVectorStore(JedisConnectionFactory jedisConnectionFactory, EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = this.jedisPooled(jedisConnectionFactory);
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("vector-index")
                .metadataFields(
                        RedisVectorStore.MetadataField.text("content"),
                        RedisVectorStore.MetadataField.tag("hash"),
                        RedisVectorStore.MetadataField.tag("file_name")
                )
                .initializeSchema(true)
                .build();
    }

}
