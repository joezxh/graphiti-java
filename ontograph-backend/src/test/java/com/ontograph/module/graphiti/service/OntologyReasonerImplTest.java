package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntConstraintMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.ontograph.module.graphiti.service.impl.OntologyReasonerImpl;
import com.ontograph.module.graphiti.vo.ontology.ConsistencyResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class OntologyReasonerImplTest {

    private OntologyReasonerImpl reasoner;

    @Mock
    private OntDefinitionMapper definitionMapper;

    @Mock
    private OntClassMapper classMapper;

    @Mock
    private OntPropertyMapper propertyMapper;

    @Mock
    private OntConstraintMapper constraintMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reasoner = new OntologyReasonerImpl(definitionMapper, classMapper, propertyMapper, constraintMapper);
    }

    @Test
    void testIsWarmedUp_beforeWarmUp() {
        assertFalse(reasoner.isWarmedUp("graph-test-1"));
    }

    @Test
    void testWarmUpAndShutdown() {
        String graphId = "graph-test-2";
        // 由于 Mock 环境下无数据库数据,warmUp 会跳过实际初始化
        reasoner.warmUp(graphId);
        // warmUp 在无数据时不会创建推理机,所以 isWarmedUp 为 false
        assertFalse(reasoner.isWarmedUp(graphId));

        reasoner.shutdown(graphId);
        assertFalse(reasoner.isWarmedUp(graphId));
    }

    @Test
    void testGetAncestorClasses_whenNotWarmedUp() {
        var result = reasoner.getAncestorClasses("graph-unknown", "http://example.org/Thing");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetDescendantClasses_whenNotWarmedUp() {
        var result = reasoner.getDescendantClasses("graph-unknown", "http://example.org/Thing");
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckConsistency_whenNotWarmedUp() {
        ConsistencyResultVO result = reasoner.checkConsistency("graph-unknown");
        assertTrue(result.isConsistent());
        assertTrue(result.getInconsistencies().contains("推理机未初始化"));
    }

    @Test
    void testIsSatisfiable_whenNotWarmedUp() {
        assertTrue(reasoner.isSatisfiable("graph-unknown", "http://example.org/Thing"));
    }

    @Test
    void testInferTypes_returnsEmpty() {
        var result = reasoner.inferTypes("graph-test", java.util.Map.of("name", "Alice"));
        assertTrue(result.isEmpty());
    }
}
