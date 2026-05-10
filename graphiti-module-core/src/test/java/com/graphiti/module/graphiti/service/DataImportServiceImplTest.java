package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.service.impl.DataImportServiceImpl;
import com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataImportServiceImplTest {

    @Mock private GraphNeo4jService graphNeo4jService;
    @Mock private TemporalService temporalService;
    @Mock private OntologyValidationService validationService;
    @Mock private EmbedderService embedderService;

    private DataImportServiceImpl dataImportService;

    @BeforeEach
    void setUp() {
        dataImportService = new DataImportServiceImpl(
            graphNeo4jService, temporalService, validationService, embedderService);
    }

    @Test
    void addEntityNode_withNoOntology_bypassesValidation() {
        when(validationService.hasOntology("graph-1")).thenReturn(false);
        doNothing().when(temporalService).invalidateFacts(anyString(), anyList());
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(
            eq("graph-1"), anyString(), eq("TestEntity"), eq("Entity"),
            eq(""), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "x", "name", "TestEntity"));

        dataImportService.addEntityNode("graph-1",
            Map.of("name", "TestEntity", "type", "Entity"));

        verify(validationService, never()).validateNode(anyString(), anyString(), any(Map.class));
        verify(graphNeo4jService).createEntityNode(
            eq("graph-1"), anyString(), eq("TestEntity"), eq("Entity"),
            eq(""), any(float[].class), any(Map.class));
    }

    @Test
    void addEntityNode_withOntology_passesValidation() {
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        when(validationService.validateNode(eq("graph-2"), eq("Person"), any(Map.class)))
            .thenReturn(ValidationResultVO.pass());
        doNothing().when(temporalService).invalidateFacts(anyString(), anyList());
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(
            eq("graph-2"), anyString(), eq("Alice"), eq("Person"),
            eq(""), any(float[].class), any(Map.class)))
            .thenReturn(Map.of("uuid", "y", "name", "Alice"));

        dataImportService.addEntityNode("graph-2",
            Map.of("name", "Alice", "type", "Person"));

        verify(validationService).validateNode(eq("graph-2"), eq("Person"), any(Map.class));
    }

    @Test
    void addEntityNode_withOntology_failsValidation() {
        when(validationService.hasOntology("graph-3")).thenReturn(true);
        List<ValidationErrorVO> errors = List.of(new ValidationErrorVO(
            2, "ONT002", "缺少必需属性: code", "code", null));
        when(validationService.validateNode(eq("graph-3"), eq("Product"), any(Map.class)))
            .thenReturn(ValidationResultVO.fail(2, errors));

        assertThrows(OntologyValidationException.class, () ->
            dataImportService.addEntityNode("graph-3",
                Map.of("name", "Widget", "type", "Product")));
        verify(graphNeo4jService, never()).createEntityNode(
            anyString(), anyString(), anyString(), anyString(), anyString(), any(), any());
        verify(embedderService, never()).embed(anyString());
        verify(temporalService, never()).invalidateFacts(anyString(), anyList());
    }
}
