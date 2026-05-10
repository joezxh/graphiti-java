package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.service.impl.NodeServiceImpl;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceImplTest {

    @Mock private GraphNeo4jService graphNeo4jService;
    @Mock private EmbedderService embedderService;
    @Mock private OntologyValidationService validationService;

    @InjectMocks private NodeServiceImpl nodeService;

    @Test
    void createNode_withNoOntology_bypassesValidation() {
        when(validationService.hasOntology("graph-1")).thenReturn(false);
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(
            eq("graph-1"), anyString(), eq("Test"), eq("Entity"),
            eq(""), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "abc", "name", "Test", "type", "Entity"));

        NodeInfoRespVO result = nodeService.createNode("graph-1",
            Map.of("name", "Test", "type", "Entity"));

        verify(validationService, never()).validateNode(anyString(), anyString(), any(Map.class));
        assertNotNull(result);
    }

    @Test
    void createNode_withOntology_passesValidation() {
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        when(validationService.validateNode(eq("graph-2"), eq("Person"), any(Map.class)))
            .thenReturn(ValidationResultVO.pass());
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(
            eq("graph-2"), anyString(), eq("Alice"), eq("Person"),
            eq(""), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "def", "name", "Alice", "type", "Person"));

        NodeInfoRespVO result = nodeService.createNode("graph-2",
            Map.of("name", "Alice", "type", "Person"));

        verify(validationService).validateNode(eq("graph-2"), eq("Person"), any(Map.class));
        assertNotNull(result);
    }

    @Test
    void createNode_withOntology_failsValidation() {
        when(validationService.hasOntology("graph-3")).thenReturn(true);
        var errors = java.util.List.of(
            new com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO(
                2, "ONT002", "缺少必需属性: age", "age", null));
        when(validationService.validateNode(eq("graph-3"), eq("Person"), any(Map.class)))
            .thenReturn(ValidationResultVO.fail(2, errors));

        assertThrows(OntologyValidationException.class, () ->
            nodeService.createNode("graph-3",
                Map.of("name", "Bob", "type", "Person")));
    }
}
