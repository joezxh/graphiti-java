package com.ontograph.module.graphiti.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ontograph.module.graphiti.service.impl.SearchResultCacheServiceImpl;
import com.ontograph.module.graphiti.vo.search.SearchResultsRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchResultCacheServiceImpl 单元测试
 *
 * <p>验证搜索结果二级缓存（L1 Caffeine + L2 Redis）的核心逻辑：
 * <ul>
 *   <li>L1 Caffeine 缓存命中</li>
 *   <li>L2 Redis 缓存命中</li>
 *   <li>缓存未命中时执行计算</li>
 *   <li>缓存 Key 生成（MD5）</li>
 *   <li>缓存失效</li>
 * </ul>
 */
class SearchResultCacheServiceImplTest {

    private Cache<String, SearchResultsRespVO> localCache;
    private RedisTemplate<String, SearchResultsRespVO> redisTemplate;
    private ValueOperations<String, SearchResultsRespVO> valueOperations;
    private SearchResultCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        localCache = Caffeine.newBuilder().maximumSize(1000).build();
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new SearchResultCacheServiceImpl(localCache, redisTemplate);
    }

    @Nested
    @DisplayName("getOrCompute 缓存命中测试")
    class CacheHitTests {

        @Test
        @DisplayName("L1 Caffeine 缓存命中直接返回")
        void testL1CacheHit() {
            SearchResultsRespVO cached = new SearchResultsRespVO();
            cached.setFacts(new ArrayList<>());
            cached.setTotalCount(5);

            String cacheKey = service.buildCacheKey("test query", "graph1", "");
            localCache.put(cacheKey, cached);

            SearchResultsRespVO result = service.getOrCompute(
                    "test query", "graph1",
                    () -> fail("不应调用 computer")
            );

            assertNotNull(result);
            assertEquals(5, result.getTotalCount());
            verify(valueOperations, never()).get(anyString());
        }

        @Test
        @DisplayName("L2 Redis 缓存命中，写入 L1 并返回")
        void testL2CacheHit() {
            SearchResultsRespVO cached = new SearchResultsRespVO();
            cached.setFacts(new ArrayList<>());
            cached.setTotalCount(10);

            when(valueOperations.get(anyString())).thenReturn(cached);

            SearchResultsRespVO result = service.getOrCompute(
                    "query1", "g1",
                    () -> fail("不应调用 computer")
            );

            assertNotNull(result);
            assertEquals(10, result.getTotalCount());
            verify(valueOperations).get(startsWith("search:result:"));
            // L2 命中后应写入 L1
            String expectedKey = service.buildCacheKey("query1", "g1", "");
            assertNotNull(localCache.getIfPresent(expectedKey));
        }

        @Test
        @DisplayName("L1 和 L2 都未命中，执行计算并缓存")
        void testCacheMiss() {
            SearchResultsRespVO computed = new SearchResultsRespVO();
            computed.setFacts(new ArrayList<>());
            computed.setTotalCount(99);

            when(valueOperations.get(anyString())).thenReturn(null);

            SearchResultsRespVO result = service.getOrCompute(
                    "miss query", "graph1",
                    () -> computed
            );

            assertNotNull(result);
            assertEquals(99, result.getTotalCount());
            verify(valueOperations).set(anyString(), eq(computed), any());
        }
    }

    @Nested
    @DisplayName("buildCacheKey 缓存 Key 生成测试")
    class CacheKeyTests {

        @Test
        @DisplayName("相同输入生成相同 Key")
        void testSameInputSameKey() {
            String key1 = service.buildCacheKey("query", "graph1", "config1");
            String key2 = service.buildCacheKey("query", "graph1", "config1");

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("不同输入生成不同 Key")
        void testDifferentInputDifferentKey() {
            String key1 = service.buildCacheKey("query1", "graph1", "config1");
            String key2 = service.buildCacheKey("query2", "graph1", "config1");

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Key 长度为 32（MD5）")
        void testKeyLength() {
            String key = service.buildCacheKey("test", "graph1", "");
            assertEquals(32, key.length());
        }

        @Test
        @DisplayName("null 输入安全处理")
        void testNullInput() {
            String key = service.buildCacheKey(null, null, null);
            assertNotNull(key);
            assertEquals(32, key.length());
        }

        @Test
        @DisplayName("带配置 Key 生成")
        void testWithConfigKey() {
            String keyNoConfig = service.buildCacheKey("query", "graph1", "");
            String keyWithConfig = service.buildCacheKey("query", "graph1", "rrf_config");

            assertNotEquals(keyNoConfig, keyWithConfig);
        }
    }

    @Nested
    @DisplayName("invalidate 缓存失效测试")
    class InvalidateTests {

        @Test
        @DisplayName("invalidateByGraphId 按图谱失效（键包含图谱ID时生效）")
        void testInvalidateByGraphId() {
            // 当缓存key包含图谱ID时才能正确过滤
            localCache.put("graph1:key1", new SearchResultsRespVO());
            localCache.put("graph1:key2", new SearchResultsRespVO());
            localCache.put("graph2:key3", new SearchResultsRespVO());

            when(redisTemplate.delete(anyString())).thenReturn(true);

            service.invalidateByGraphId("graph1");

            assertEquals(1, localCache.estimatedSize());
            assertNotNull(localCache.getIfPresent("graph2:key3"));
            assertNull(localCache.getIfPresent("graph1:key1"));
        }

        @Test
        @DisplayName("invalidateByGraphId 不存在的图谱不报错")
        void testInvalidateNonExistentGraphId() {
            assertDoesNotThrow(() -> service.invalidateByGraphId("non-existent"));
        }
    }

    @Nested
    @DisplayName("Redis 异常容错测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Redis 读取失败 fallback 到计算")
        void testRedisReadFailureFallback() {
            SearchResultsRespVO computed = new SearchResultsRespVO();
            computed.setFacts(new ArrayList<>());
            computed.setTotalCount(42);

            when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

            SearchResultsRespVO result = service.getOrCompute(
                    "query", "graph1",
                    () -> computed
            );

            assertNotNull(result);
            assertEquals(42, result.getTotalCount());
        }
    }
}
