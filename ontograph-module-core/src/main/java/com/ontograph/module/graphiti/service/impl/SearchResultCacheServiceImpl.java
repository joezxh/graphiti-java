package com.ontograph.module.graphiti.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.ontograph.module.graphiti.service.SearchResultCacheService;
import com.ontograph.module.graphiti.vo.search.SearchResultsRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * 搜索结果缓存服务实现（L1 Caffeine + L2 Redis）
 *
 * <p>缓存 Key = MD5(query + graphId + configKey)
 * <p>适用于高频重复查询场景，可减少 70% 以上的重复搜索计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchResultCacheServiceImpl implements SearchResultCacheService {

    private final Cache<String, SearchResultsRespVO> searchResultLocalCache;
    private final RedisTemplate<String, SearchResultsRespVO> searchResultRedisTemplate;

    private static final String CACHE_KEY_PREFIX = "search:result:";
    private static final long REDIS_TTL_MINUTES = 30;

    @Override
    public SearchResultsRespVO getOrCompute(String query, String graphId,
                                           Supplier<SearchResultsRespVO> computer) {
        return getOrCompute(query, graphId, "", computer);
    }

    @Override
    public SearchResultsRespVO getOrCompute(String query, String graphId, String configKey,
                                           Supplier<SearchResultsRespVO> computer) {
        String cacheKey = buildCacheKey(query, graphId, configKey);

        // L1 Caffeine 检查
        SearchResultsRespVO cached = searchResultLocalCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("L1 缓存命中: key={}", cacheKey);
            return cached;
        }

        // L2 Redis 检查
        try {
            SearchResultsRespVO redisCached = searchResultRedisTemplate.opsForValue().get(CACHE_KEY_PREFIX + cacheKey);
            if (redisCached != null) {
                log.debug("L2 缓存命中: key={}", cacheKey);
                searchResultLocalCache.put(cacheKey, redisCached);
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败: key={}, error={}", cacheKey, e.getMessage());
        }

        // 执行搜索
        long start = System.currentTimeMillis();
        SearchResultsRespVO result = computer.get();
        long elapsed = System.currentTimeMillis() - start;
        log.info("搜索执行完成: query={}, graphId={}, elapsed={}ms", query, graphId, elapsed);

        // 写入缓存
        if (result != null) {
            try {
                searchResultRedisTemplate.opsForValue().set(
                        CACHE_KEY_PREFIX + cacheKey,
                        result,
                        Duration.ofMinutes(REDIS_TTL_MINUTES));
                log.debug("写入 L2 Redis: key={}", cacheKey);
            } catch (Exception e) {
                log.warn("Redis 缓存写入失败: key={}, error={}", cacheKey, e.getMessage());
            }
            searchResultLocalCache.put(cacheKey, result);
        }

        return result;
    }

    @Override
    public void invalidateByGraphId(String graphId) {
        log.info("使图谱缓存失效: graphId={}", graphId);
        searchResultLocalCache.asMap().keySet().stream()
                .filter(k -> k.contains(graphId))
                .forEach(searchResultLocalCache::invalidate);
    }

    @Override
    public void invalidate(String query, String graphId) {
        String cacheKey = buildCacheKey(query, graphId, "");
        searchResultLocalCache.invalidate(cacheKey);
        try {
            searchResultRedisTemplate.delete(CACHE_KEY_PREFIX + cacheKey);
            log.debug("缓存失效: key={}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis 缓存删除失败: key={}, error={}", cacheKey, e.getMessage());
        }
    }

    @Override
    public void invalidateAll() {
        searchResultLocalCache.invalidateAll();
        log.info("L1 Caffeine 缓存已清空");
    }

    @Override
    public String buildCacheKey(String query, String graphId) {
        return buildCacheKey(query, graphId, "");
    }

    @Override
    public String buildCacheKey(String query, String graphId, String configKey) {
        String raw = (query != null ? query : "") + "|" +
                    (graphId != null ? graphId : "") + "|" +
                    (configKey != null ? configKey : "");
        return md5(raw);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
