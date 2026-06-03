package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.service.impl.MmrRerankerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MMR (Maximal Marginal Relevance) 算法单元测试
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:maximal_marginal_relevance()
 */
class MmrRerankerServiceTest {

    private MmrRerankerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MmrRerankerServiceImpl();
    }

    @Nested
    @DisplayName("余弦相似度测试")
    class CosineSimilarityTests {

        @Test
        @DisplayName("完全相同向量：相似度 = 1")
        void testIdenticalVectors() {
            double[] a = {1.0, 0.0, 0.0};
            double[] b = {1.0, 0.0, 0.0};
            assertEquals(1.0, service.cosineSimilarity(a, b), 0.0001);
        }

        @Test
        @DisplayName("正交向量：相似度 = 0")
        void testOrthogonalVectors() {
            double[] a = {1.0, 0.0, 0.0};
            double[] b = {0.0, 1.0, 0.0};
            assertEquals(0.0, service.cosineSimilarity(a, b), 0.0001);
        }

        @Test
        @DisplayName("相反向量：相似度 = -1")
        void testOppositeVectors() {
            double[] a = {1.0, 0.0, 0.0};
            double[] b = {-1.0, 0.0, 0.0};
            assertEquals(-1.0, service.cosineSimilarity(a, b), 0.0001);
        }

        @Test
        @DisplayName("零向量：相似度 = 0")
        void testZeroVector() {
            double[] a = {0.0, 0.0, 0.0};
            double[] b = {1.0, 0.0, 0.0};
            assertEquals(0.0, service.cosineSimilarity(a, b), 0.0001);
        }
    }

    @Nested
    @DisplayName("L2 归一化测试")
    class NormalizeTests {

        @Test
        @DisplayName("归一化后范数为 1")
        void testNormalizedNorm() {
            double[] v = {3.0, 4.0};
            double[] norm = service.normalizeL2(v);
            double n = Math.sqrt(norm[0] * norm[0] + norm[1] * norm[1]);
            assertEquals(1.0, n, 0.0001);
        }

        @Test
        @DisplayName("零向量保持不变")
        void testZeroVectorNormalization() {
            double[] v = {0.0, 0.0, 0.0};
            double[] norm = service.normalizeL2(v);
            assertEquals(0.0, norm[0]);
            assertEquals(0.0, norm[1]);
            assertEquals(0.0, norm[2]);
        }
    }

    @Nested
    @DisplayName("MMR 文本相似度测试")
    class MmrTextTests {

        @Test
        @DisplayName("lambda=1: 纯相关性选择")
        void testPureRelevance() {
            List<String> candidates = List.of("doc1 about AI", "doc2 about food", "doc3 about AI technology");
            Map<String, String> textMap = new HashMap<>();
            textMap.put("doc1 about AI", "doc1 about AI");
            textMap.put("doc2 about food", "doc2 about food");
            textMap.put("doc3 about AI technology", "doc3 about AI technology");

            List<String> result = service.mmrByText(candidates, textMap, 1.0, 2);

            // lambda=1 时，只选相关性最高的
            assertEquals(2, result.size());
            // doc3 的词更多但相关性与 doc1 相同
            assertTrue(result.contains("doc1 about AI") || result.contains("doc3 about AI technology"));
        }

        @Test
        @DisplayName("lambda=0: 纯多样性选择")
        void testPureDiversity() {
            List<String> candidates = List.of("doc1 about AI", "doc2 about food", "doc3 about weather");
            Map<String, String> textMap = new HashMap<>();
            textMap.put("doc1 about AI", "doc1 about AI");
            textMap.put("doc2 about food", "doc2 about food");
            textMap.put("doc3 about weather", "doc3 about weather");

            List<String> result = service.mmrByText(candidates, textMap, 0.0, 3);

            // lambda=0 时，最大化多样性
            assertEquals(3, result.size());
            // 选择第一个
            assertEquals("doc1 about AI", result.get(0));
        }

        @Test
        @DisplayName("lambda=0.5: 平衡相关性与多样性")
        void testBalanced() {
            List<String> candidates = List.of("AI", "machine learning", "food recipe", "cooking tips");
            Map<String, String> textMap = new HashMap<>();
            for (String c : candidates) textMap.put(c, c);

            List<String> result = service.mmrByText(candidates, textMap, 0.5, 3);

            assertNotNull(result);
            assertTrue(result.size() <= 3);
        }

        @Test
        @DisplayName("空列表处理")
        void testEmptyInput() {
            List<String> result = service.mmrByText(List.of(), Map.of(), 0.5, 10);
            assertTrue(result.isEmpty());
        }
    }
}
