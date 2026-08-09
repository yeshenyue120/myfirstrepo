package com.example.calorieserver.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置：启用 Spring Cache 注解，为各缓存区配置独立 TTL。
 *
 * 设计说明：食物库的缓存查询都过滤 isPublic=true，用户自定义食物（isPublic=false）
 * 从不进缓存，且系统食物只靠手动 SQL 导入、App 内不变——因此这些缓存无需 @CacheEvict，
 * 长 TTL 即可，省去缓存一致性的复杂度。
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** 默认过期时间：30 分钟 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** 各缓存区独立 TTL（秒） */
    private static final Map<String, Duration> CACHE_TTL = new HashMap<>() {{
        put("foodCategories", Duration.ofHours(1));        // 分类列表，几乎不变
        put("foodCommon", Duration.ofHours(1));            // 常见食物，高频展示
        put("foodCategory", Duration.ofMinutes(30));       // 分类下食物（9000+ 全量）
        put("foodCategoryCommon", Duration.ofMinutes(30)); // 分类下常见食物
        put("foodLibrary", Duration.ofMinutes(30));        // 全库分页
        put("foodSearchPage", Duration.ofMinutes(10));     // 分页搜索（高动态，短 TTL）
    }};

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // GenericJackson2JsonRedisSerializer 基于 Jackson 2，需手动注册 jsr310 才能序列化
        // LocalDateTime/LocalDate 等 Java 8 时间类型（否则食品缓存的 createdAt 会抛异常）。
        ObjectMapper redisMapper = new ObjectMapper();
        redisMapper.registerModule(new JavaTimeModule());
        redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 与 GenericJackson2JsonRedisSerializer 默认一致：写入类型信息，反序列化才能还原具体类型
        redisMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        // 用 withInitialCacheConfigurations 为各缓存区配置独立 TTL（覆盖默认）
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        CACHE_TTL.forEach((name, ttl) ->
                configs.put(name, baseConfig.entryTtl(ttl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}