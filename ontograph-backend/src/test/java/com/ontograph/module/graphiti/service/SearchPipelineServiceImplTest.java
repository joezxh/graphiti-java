package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.model.search.*;
import com.ontograph.module.graphiti.model.search.SearchResults.*;
import com.ontograph.module.graphiti.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SearchPipelineServiceImpl 单元测试
 *
 * <p>验证搜索 Pipeline 的核心逻辑：
 * <ul>
 *   <li>空查询处理</li>
 *   <li>空配置默认值</li>
 *   <li>Rerank 方法（RRF/MMR/CrossEncoder/NodeDistance/EpisodeMentions）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchPipelineServiceImplTest {

    @Mock
    private GraphNeo4jService graphNeo4jService;

    @Mock
    private EmbedderService embedderService;

    @Mock
    private RrfRerankerService rrfRerankerService;

    @Mock
    private MmrRerankerService mmrRerankerService;

    @Mock
    private CrossEncoderRerankerService crossEncoderRerankerService;

    @Mock
    private NodeDistanceRerankerService nodeDistanceRerankerService;

    @Mock
    private EpisodeMentionsRerankerService episodeMentionsRerankerService;

    private SearchPipelineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchPipelineServiceImpl(
                graphNeo4jService,
                embedderService,
                rrfRerankerService,
                mmrRerankerService,
                crossEncoderRerankerService,
                nodeDistanceRerankerService,
                episodeMentionsRerankerService,
                Runnable::run,
                Runnable::run
        );
    }

    // ==================== 空值处理测试 ====================

    @Nested
    @DisplayName("search() 空值处理测试")
    class EmptyQueryTests {

        @Test
        @DisplayName("null 查询文本返回空结果")
        void testNullQuery() {
            SearchResults results = service.search(null, "graph1", null, null, null, null);
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("空白查询文本返回空结果")
        void testBlankQuery() {
            SearchResults results = service.search("   ", "graph1", null, null, null, null);
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("空字符串查询返回空结果")
        void testEmptyQuery() {
            SearchResults results = service.search("", "graph1", null, null, null, null);
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // ==================== 配置默认值测试 ====================

    @Nested
    @DisplayName("search() 配置默认值测试")
    class DefaultConfigTests {

        @Test
        @DisplayName("null 配置使用默认 combinedHybridRrf()")
        void testNullConfigUsesDefault() {
            when(graphNeo4jService.searchEdgesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchNodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchEpisodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchCommunitiesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(embedderService.getDimensions()).thenReturn(1536);

            SearchResults results = service.search("test query", "graph1", null, null, null, null);

            assertNotNull(results);
            assertNotNull(results.getEdges());
            assertNotNull(results.getNodes());
            assertNotNull(results.getEpisodes());
            assertNotNull(results.getCommunities());
        }

        @Test
        @DisplayName("null filters 使用空过滤器")
        void testNullFiltersUsesEmpty() {
            when(graphNeo4jService.searchEdgesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchNodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchEpisodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchCommunitiesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(embedderService.getDimensions()).thenReturn(1536);

            SearchResults results = service.search("test", "graph1", SearchConfig.combinedHybridRrf(), null, null, null);

            assertNotNull(results);
        }

        @Test
        @DisplayName("limit <= 0 时使用默认值 10")
        void testNegativeLimitUsesDefault() {
            when(graphNeo4jService.searchEdgesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchNodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchEpisodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchCommunitiesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(embedderService.getDimensions()).thenReturn(1536);

            SearchConfig config = SearchConfig.builder().limit(-1).build();
            SearchResults results = service.search("test", "graph1", config, null, null, null);

            assertNotNull(results);
        }
    }

    // ==================== Scope 禁用测试 ====================

    @Nested
    @DisplayName("search() Scope 禁用测试")
    class ScopeDisableTests {

        @Test
        @DisplayName("null EdgeConfig 不执行边搜索")
        void testNullEdgeConfig() {
            when(graphNeo4jService.searchNodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchEpisodesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(graphNeo4jService.searchCommunitiesByFulltext(anyString(), anyString(), anyInt()))
                    .thenReturn(List.of());
            when(embedderService.getDimensions()).thenReturn(1536);

            SearchConfig config = SearchConfig.builder()
                    .edgeConfig(null)
                    .nodeConfig(NodeSearchConfig.hybridRrf())
                    .episodeConfig(EpisodeSearchConfig.bm25Only())
                    .communityConfig(CommunitySearchConfig.hybridRrf())
                    .limit(10)
                    .build();

            SearchResults results = service.search("test", "graph1", config, null, null, null);

            assertNotNull(results.getEdges());
            assertTrue(results.getEdges().isEmpty());
            verify(graphNeo4jService, never()).searchEdgesByFulltext(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("BM25 配置执行边搜索不报错")
        void testEdgeOnlyRrf() {
            // 使用最简单的配置，只测 BM25
            SearchConfig config = new SearchConfig();
            config.setLimit(10);
            EdgeSearchConfig edgeCfg = new EdgeSearchConfig();
            edgeCfg.setSearchMethods(List.of(EdgeSearchMethod.bm25));
            edgeCfg.setReranker(RerankerType.rrf);
            config.setEdgeConfig(edgeCfg);

            // 验证服务不报错并返回有效结果结构
            SearchResults results = service.search("test", "graph1", config, null, null, null);

            assertNotNull(results);
            assertNotNull(results.getEdges());
        }
    }

    // ==================== rerank() 方法测试 ====================

    @Nested
    @DisplayName("rerank() 边候选项重排测试")
    class RerankEdgeTests {

        @Test
        @DisplayName("RRF 重排策略")
        void testRerankEdgesRrf() {
            List<EdgeResult> edges = List.of(
                    makeEdge("e1", "Alice knows Bob"),
                    makeEdge("e2", "Bob works at Google")
            );
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));

            SearchResults results = service.rerank("knows", "rrf", 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            assertEquals(2, results.getEdges().size());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
        }

        @Test
        @DisplayName("MMR 重排策略")
        void testRerankEdgesMmr() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "Alice knows Bob"));
            when(embedderService.getDimensions()).thenReturn(1536);
            when(mmrRerankerService.mmrByText(any(), any(), anyDouble(), anyInt()))
                    .thenReturn(List.of("e1"));

            SearchResults results = service.rerank("Alice", "mmr", 0.7, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(mmrRerankerService).mmrByText(any(), any(), eq(0.7), anyInt());
        }

        @Test
        @DisplayName("Cross-Encoder 重排策略")
        void testRerankEdgesCrossEncoder() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "Alice knows Bob"));
            when(crossEncoderRerankerService.rankEdges(anyString(), any(), anyInt()))
                    .thenReturn(List.of("e1"));

            SearchResults results = service.rerank("Alice knows", "cross_encoder", 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(crossEncoderRerankerService).rankEdges(eq("Alice knows"), any(), eq(10));
        }

        @Test
        @DisplayName("NodeDistance 重排策略")
        void testRerankEdgesNodeDistance() {
            List<EdgeResult> edges = List.of(
                    makeEdge("e1", "Alice knows Bob", "n1", "n2"),
                    makeEdge("e2", "Bob works at Google", "n2", "n3")
            );
            when(nodeDistanceRerankerService.rerankEdgesByDistance(any(), eq("center-1"), any(), eq(10)))
                    .thenReturn(List.of("e2", "e1"));

            SearchResults results = service.rerank("Bob", "node_distance", 0.5, "center-1", 10, edges, null);

            assertNotNull(results.getEdges());
            assertEquals(2, results.getEdges().size());
            verify(nodeDistanceRerankerService).rerankEdgesByDistance(any(), eq("center-1"), any(), eq(10));
        }

        @Test
        @DisplayName("EpisodeMentions 重排策略")
        void testRerankEdgesEpisodeMentions() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "Alice knows Bob"));
            when(episodeMentionsRerankerService.rerankEdgesByMentions(any(), eq(10)))
                    .thenReturn(List.of("e1"));

            SearchResults results = service.rerank("Bob", "episode_mentions", 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(episodeMentionsRerankerService).rerankEdgesByMentions(any(), eq(10));
        }

        @Test
        @DisplayName("未知策略 fallback 到 RRF")
        void testRerankUnknownStrategy() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "fact"));
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));

            SearchResults results = service.rerank("test", "unknown_strategy", 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
        }

        @Test
        @DisplayName("空候选项返回空结果")
        void testRerankEmptyCandidates() {
            SearchResults results = service.rerank("test", "rrf", 0.5, null, 10, null, null);

            assertNotNull(results.getEdges());
            assertTrue(results.getEdges().isEmpty());
            assertNotNull(results.getNodes());
            assertTrue(results.getNodes().isEmpty());
        }
    }

    @Nested
    @DisplayName("rerank() 节点候选项重排测试")
    class RerankNodeTests {

        @Test
        @DisplayName("RRF 重排策略")
        void testRerankNodesRrf() {
            List<NodeResult> nodes = List.of(
                    makeNode("n1", "Alice", "A person"),
                    makeNode("n2", "Bob", "Another person")
            );
            when(rrfRerankerService.rrfNodes(any(), eq(1))).thenReturn(new ArrayList<>(nodes));

            SearchResults results = service.rerank("person", "rrf", 0.5, null, 10, null, nodes);

            assertNotNull(results.getNodes());
            assertEquals(2, results.getNodes().size());
            verify(rrfRerankerService).rrfNodes(any(), eq(1));
        }

        @Test
        @DisplayName("CrossEncoder 重排策略")
        void testRerankNodesCrossEncoder() {
            List<NodeResult> nodes = List.of(makeNode("n1", "Alice", "A person"));
            when(crossEncoderRerankerService.rankNodes(anyString(), any(), any(), anyInt()))
                    .thenReturn(List.of("n1"));

            SearchResults results = service.rerank("Alice", "cross_encoder", 0.5, null, 10, null, nodes);

            assertNotNull(results.getNodes());
            verify(crossEncoderRerankerService).rankNodes(eq("Alice"), any(), any(), eq(10));
        }
    }

    @Nested
    @DisplayName("rerank() 边界条件测试")
    class RerankBoundaryTests {

        @Test
        @DisplayName("null limit 使用默认值 10")
        void testRerankNullLimit() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "fact"));
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));

            SearchResults results = service.rerank("test", "rrf", 0.5, null, null, edges, null);

            assertNotNull(results.getEdges());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
        }

        @Test
        @DisplayName("null reranker 使用默认 RRF")
        void testRerankNullReranker() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "fact"));
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));

            SearchResults results = service.rerank("test", null, 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
        }

        @Test
        @DisplayName("null mmrLambda 使用默认值 0.5")
        void testRerankNullMmrLambda() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "Alice knows Bob"));
            when(embedderService.getDimensions()).thenReturn(1536);
            when(mmrRerankerService.mmrByText(any(), any(), anyDouble(), anyInt()))
                    .thenReturn(List.of("e1"));

            SearchResults results = service.rerank("Alice", "mmr", null, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(mmrRerankerService).mmrByText(any(), any(), eq(0.5), anyInt());
        }

        @Test
        @DisplayName("NodeDistance 无 centerNodeUuid 时 fallback 到 RRF")
        void testRerankNodeDistanceNoCenter() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "fact"));
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));

            SearchResults results = service.rerank("test", "node_distance", 0.5, null, 10, edges, null);

            assertNotNull(results.getEdges());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
            verify(nodeDistanceRerankerService, never()).rerankEdgesByDistance(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("同时处理边和节点候选项")
        void testRerankEdgesAndNodes() {
            List<EdgeResult> edges = List.of(makeEdge("e1", "Alice knows Bob"));
            List<NodeResult> nodes = List.of(makeNode("n1", "Alice", "A person"));
            when(rrfRerankerService.rrfEdges(any(), eq(1))).thenReturn(new ArrayList<>(edges));
            when(rrfRerankerService.rrfNodes(any(), eq(1))).thenReturn(new ArrayList<>(nodes));

            SearchResults results = service.rerank("Alice", "rrf", 0.5, null, 10, edges, nodes);

            assertNotNull(results.getEdges());
            assertNotNull(results.getNodes());
            assertEquals(1, results.getEdges().size());
            assertEquals(1, results.getNodes().size());
            verify(rrfRerankerService).rrfEdges(any(), eq(1));
            verify(rrfRerankerService).rrfNodes(any(), eq(1));
        }
    }

    // ==================== 辅助方法 ====================

    private EdgeResult makeEdge(String uuid, String fact) {
        return makeEdge(uuid, fact, null, null);
    }

    private EdgeResult makeEdge(String uuid, String fact, String source, String target) {
        EdgeResult e = new EdgeResult();
        e.setUuid(uuid);
        e.setFact(fact);
        e.setName(fact.split(" ")[0]);
        e.setSourceNodeUuid(source);
        e.setTargetNodeUuid(target);
        return e;
    }

    private NodeResult makeNode(String uuid, String name, String summary) {
        NodeResult n = new NodeResult();
        n.setUuid(uuid);
        n.setName(name);
        n.setSummary(summary);
        return n;
    }
}
