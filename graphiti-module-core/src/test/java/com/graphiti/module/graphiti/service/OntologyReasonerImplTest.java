package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.service.impl.OntologyReasonerImpl;
import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OntologyReasonerImplTest {

    private OntologyReasonerImpl reasoner;

    @BeforeEach
    void setUp() {
        reasoner = new OntologyReasonerImpl();
    }

    @Test
    void testIsWarmedUp_beforeWarmUp() {
        assertFalse(reasoner.isWarmedUp("graph-test-1"));
    }

    @Test
    void testWarmUpAndShutdown() {
        String graphId = "graph-test-2";
        reasoner.warmUp(graphId);
        assertTrue(reasoner.isWarmedUp(graphId));

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
