package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.vo.edge.EdgeInfoRespVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdgeServiceImplTest {

    @Mock private GraphNeo4jService graphNeo4jService;
    @Mock private EmbedderService embedderService;
    @Mock private OntologyValidationService validationService;

    @InjectMocks private EdgeServiceImpl edgeService;

    @Test
    void createEdge_withNoOntology_bypassesValidation() {
        when(validationService.hasOntology("graph-1")).thenReturn(false);
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createRelationship(
            eq("graph-1"), anyString(), eq("a"), eq("b"), eq("WORKS_FOR"),
            isNull(), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "e1", "source", "a", "target", "b", "type", "WORKS_FOR"));

        EdgeInfoRespVO result = edgeService.createEdge("graph-1",
            Map.of("source", "a", "target", "b", "type", "WORKS_FOR"));

        verify(validationService, never()).validateEdge(anyString(), anyString(), any(Map.class));
        assertNotNull(result);
    }

    @Test
    void createEdge_withOntology_passesValidation() {
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        when(validationService.validateEdge(eq("graph-2"), eq("WORKS_FOR"), any(Map.class)))
            .thenReturn(ValidationResultVO.pass());
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createRelationship(
            eq("graph-2"), anyString(), eq("a"), eq("b"), eq("WORKS_FOR"),
            isNull(), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "e2", "source", "a", "target", "b", "type", "WORKS_FOR"));

        EdgeInfoRespVO result = edgeService.createEdge("graph-2",
            Map.of("source", "a", "target", "b", "type", "WORKS_FOR"));

        verify(validationService).validateEdge(eq("graph-2"), eq("WORKS_FOR"), any(Map.class));
        assertNotNull(result);
    }

    @Test
    void createEdge_withOntology_failsValidation() {
        when(validationService.hasOntology("graph-3")).thenReturn(true);
        List<ValidationErrorVO> errors = List.of(new ValidationErrorVO(
            1, "ONT001", "边类型未在本体中定义: UNKNOWN_TYPE", "type", "UNKNOWN_TYPE"));
        when(validationService.validateEdge(eq("graph-3"), eq("UNKNOWN_TYPE"), any(Map.class)))
            .thenReturn(ValidationResultVO.fail(1, errors));

        assertThrows(OntologyValidationException.class, () ->
            edgeService.createEdge("graph-3",
                Map.of("source", "a", "target", "b", "type", "UNKNOWN_TYPE")));
    }
}
