package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.model.search.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchConfig 模型单元测试
 *
 * <p>验证配置模型的创建、工厂方法、默认值
 */
class SearchConfigModelTest {

    @Nested
    @DisplayName("SearchConfig 工厂方法测试")
    class SearchConfigFactoryTests {

        @Test
        @DisplayName("combinedHybridRrf 默认配置")
        void testCombinedHybridRrf() {
            SearchConfig config = SearchConfig.combinedHybridRrf();

            assertNotNull(config.getEdgeConfig());
            assertNotNull(config.getNodeConfig());
            assertNotNull(config.getEpisodeConfig());
            assertNotNull(config.getCommunityConfig());
            assertEquals(10, config.getLimit());

            // Edge: BM25 + Cosine + RRF
            assertTrue(config.getEdgeConfig().getSearchMethods().contains(EdgeSearchMethod.bm25));
            assertTrue(config.getEdgeConfig().getSearchMethods().contains(EdgeSearchMethod.cosine_similarity));
            assertEquals(RerankerType.rrf, config.getEdgeConfig().getReranker());
        }

        @Test
        @DisplayName("edgeOnlyRrf 仅边配置")
        void testEdgeOnlyRrf() {
            SearchConfig config = SearchConfig.edgeOnlyRrf();

            assertNotNull(config.getEdgeConfig());
            assertNull(config.getNodeConfig());
            assertNull(config.getEpisodeConfig());
            assertNull(config.getCommunityConfig());
        }
    }

    @Nested
    @DisplayName("SearchConfig.fromVo 转换测试")
    class FromVoTests {

        @Test
        @DisplayName("hybrid 模式转换")
        void testHybridModeConversion() {
            com.ontograph.module.graphiti.vo.search.SearchConfigVO vo =
                    new com.ontograph.module.graphiti.vo.search.SearchConfigVO();
            vo.setMode("hybrid");
            vo.setEnableMmr(true);
            vo.setMmrLambda(0.7);
            vo.setBfsDepth(3);
            vo.setMaxFacts(20);

            SearchConfig config = SearchConfig.fromVo(vo);

            assertEquals(20, config.getLimit());
            assertEquals(RerankerType.mmr, config.getEdgeConfig().getReranker());
            assertEquals(0.7, config.getEdgeConfig().getMmrLambda());
            assertEquals(3, config.getEdgeConfig().getBfsMaxDepth());
            assertTrue(config.getEdgeConfig().getSearchMethods().contains(EdgeSearchMethod.bm25));
            assertTrue(config.getEdgeConfig().getSearchMethods().contains(EdgeSearchMethod.cosine_similarity));
        }

        @Test
        @DisplayName("bm25 模式转换")
        void testBm25ModeConversion() {
            com.ontograph.module.graphiti.vo.search.SearchConfigVO vo =
                    new com.ontograph.module.graphiti.vo.search.SearchConfigVO();
            vo.setMode("bm25");

            SearchConfig config = SearchConfig.fromVo(vo);

            assertEquals(1, config.getEdgeConfig().getSearchMethods().size());
            assertEquals(EdgeSearchMethod.bm25, config.getEdgeConfig().getSearchMethods().get(0));
            assertEquals(RerankerType.rrf, config.getEdgeConfig().getReranker());
        }

        @Test
        @DisplayName("bfs 模式转换")
        void testBfsModeConversion() {
            com.ontograph.module.graphiti.vo.search.SearchConfigVO vo =
                    new com.ontograph.module.graphiti.vo.search.SearchConfigVO();
            vo.setMode("bfs");
            vo.setBfsDepth(4);

            SearchConfig config = SearchConfig.fromVo(vo);

            assertTrue(config.getEdgeConfig().getSearchMethods().contains(EdgeSearchMethod.bfs));
            assertEquals(4, config.getEdgeConfig().getBfsMaxDepth());
        }

        @Test
        @DisplayName("null 输入返回默认配置")
        void testNullInputReturnsDefault() {
            SearchConfig config = SearchConfig.fromVo(null);
            assertNotNull(config);
            assertEquals(10, config.getLimit());
        }
    }

    @Nested
    @DisplayName("SearchResults 模型测试")
    class SearchResultsTests {

        @Test
        @DisplayName("empty 结果正确")
        void testEmptyResults() {
            SearchResults results = SearchResults.empty();

            assertNotNull(results.getEdges());
            assertNotNull(results.getNodes());
            assertNotNull(results.getEpisodes());
            assertNotNull(results.getCommunities());
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("非空判断")
        void testIsEmptyWithData() {
            SearchResults results = new SearchResults();
            SearchResults.EdgeResult edge = new SearchResults.EdgeResult();
            edge.setUuid("test");
            results.setEdges(java.util.List.of(edge));

            assertFalse(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("SearchFilters 模型测试")
    class SearchFiltersTests {

        @Test
        @DisplayName("empty 过滤器")
        void testEmptyFilters() {
            SearchFilters filters = SearchFilters.empty();
            assertNotNull(filters);
        }

        @Test
        @DisplayName("byGraphId 工厂方法")
        void testByGraphId() {
            SearchFilters filters = SearchFilters.byGraphId("graph-1");
            assertNotNull(filters.getPropertyFilters());
            assertEquals(1, filters.getPropertyFilters().size());
            assertEquals("graph_id", filters.getPropertyFilters().get(0).getProperty());
            assertEquals(SearchFilters.ComparisonOperator.eq, filters.getPropertyFilters().get(0).getOperator());
        }

        @Test
        @DisplayName("byNodeLabels 工厂方法")
        void testByNodeLabels() {
            SearchFilters filters = SearchFilters.byNodeLabels(java.util.List.of("Person", "Organization"));
            assertNotNull(filters.getNodeLabels());
            assertEquals(2, filters.getNodeLabels().size());
            assertTrue(filters.getNodeLabels().contains("Person"));
        }
    }
}
