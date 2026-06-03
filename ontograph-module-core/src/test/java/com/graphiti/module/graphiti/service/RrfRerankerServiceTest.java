package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.model.search.SearchResults.EdgeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.NodeResult;
import com.ontograph.module.graphiti.service.impl.RrfRerankerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RRF (Reciprocal Rank Fusion) 算法单元测试
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:rrf()
 */
class RrfRerankerServiceTest {

    private RrfRerankerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RrfRerankerServiceImpl();
    }

    @Nested
    @DisplayName("RRF UUID 融合测试")
    class RrfUuidsTests {

        @Test
        @DisplayName("两个列表融合：正确排序")
        void testTwoListsFusion() {
            List<String> list1 = List.of("A", "B", "C");
            List<String> list2 = List.of("B", "D", "E");

            List<String> result = service.rrfUuids(List.of(list1, list2), 1);

            // A 和 C 各出现 1 次，RRF = 1/1 = 1.0
            // B 出现 2 次，RRF = 1/1 + 1/2 = 1.0 + 0.5 = 1.5 (最高)
            // D 和 E 各出现 1 次，RRF = 1/2 = 0.5
            assertEquals("B", result.get(0));
            assertTrue(result.contains("A"));
            assertTrue(result.contains("C"));
            assertTrue(result.contains("D"));
            assertTrue(result.contains("E"));
        }

        @Test
        @DisplayName("k=60 参数：降低排名差异影响")
        void testK60Smoothing() {
            List<String> list1 = List.of("A", "B", "C");
            List<String> list2 = List.of("X", "Y", "Z");

            List<String> k1 = service.rrfUuids(List.of(list1, list2), 1);
            List<String> k60 = service.rrfUuids(List.of(list1, list2), 60);

            // k=1 时排名权重更敏感
            assertNotNull(k1);
            assertNotNull(k60);
            assertEquals(6, k1.size());
            assertEquals(6, k60.size());

            // A 和 X 的 RRF 分数在 k=1 时更高
            int posA_k1 = k1.indexOf("A");
            int posA_k60 = k60.indexOf("A");
            // k=60 时排名差异更小
            assertTrue(posA_k1 <= posA_k60);
        }

        @Test
        @DisplayName("空列表处理")
        void testEmptyList() {
            List<String> result = service.rrfUuids(List.of(), 1);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("单列表：保留原有顺序")
        void testSingleList() {
            List<String> list = List.of("X", "Y", "Z");
            List<String> result = service.rrfUuids(List.of(list), 1);
            assertEquals(List.of("X", "Y", "Z"), result);
        }
    }

    @Nested
    @DisplayName("RRF Edge 融合测试")
    class RrfEdgesTests {

        @Test
        @DisplayName("相同 UUID 合并：分数累加")
        void testDuplicateMerge() {
            EdgeResult e1 = edge("e1", "fact1");
            EdgeResult e2 = edge("e2", "fact2");
            EdgeResult e1Dup = edge("e1", "fact1");

            List<List<EdgeResult>> lists = List.of(
                    List.of(e1, e2),
                    List.of(e1Dup)  // e1 再次出现，排名更靠后
            );

            List<EdgeResult> result = service.rrfEdges(lists, 1);

            // e1 出现 2 次：RRF = 1/(0+1) + 1/(2+1) = 1.0 + 0.33 = 1.33
            // e2 出现 1 次：RRF = 1/(1+1) = 0.5
            assertEquals("e1", result.get(0).getUuid());
            assertEquals("e2", result.get(1).getUuid());
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("空输入处理")
        void testEmptyInput() {
            List<List<EdgeResult>> empty = List.of();
            List<EdgeResult> result = service.rrfEdges(empty, 1);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("RRF 加权融合测试")
    class RrfWeightedTests {

        @Test
        @DisplayName("加权 UUID：分数乘以权重因子")
        void testWeightedUuids() {
            List<java.util.Map<String, Double>> pairs = List.of(
                    java.util.Map.of("A", 1.0, "B", 0.8),
                    java.util.Map.of("A", 0.9, "C", 0.7)
            );

            List<String> result = service.rrfWeightedUuids(pairs, 1);

            // A 的加权分数 = 1.0/(1+1) + 0.9/(1+1) = 0.5 + 0.45 = 0.95
            // B 的加权分数 = 0.8/(1+1) = 0.4
            // C 的加权分数 = 0.7/(1+1) = 0.35
            assertEquals("A", result.get(0));
            assertTrue(result.contains("B"));
            assertTrue(result.contains("C"));
        }
    }

    private EdgeResult edge(String uuid, String fact) {
        EdgeResult e = new EdgeResult();
        e.setUuid(uuid);
        e.setFact(fact);
        return e;
    }
}
