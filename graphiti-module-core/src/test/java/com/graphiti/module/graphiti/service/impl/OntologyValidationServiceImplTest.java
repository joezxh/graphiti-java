package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationWarningVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OntologyValidationServiceImplTest {

    @Test
    void testValidationResultVO_pass() {
        ValidationResultVO result = ValidationResultVO.pass();
        assertTrue(result.isPassed());
        assertEquals(0, result.getLevel());
    }

    @Test
    void testValidationResultVO_fail() {
        List<ValidationErrorVO> errors = List.of(
            new ValidationErrorVO(2, "ONT002", "缺少必需属性: name", "name", null));
        ValidationResultVO result = ValidationResultVO.fail(2, errors);
        assertFalse(result.isPassed());
        assertEquals(2, result.getLevel());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testValidationResultVO_passWithWarnings() {
        List<ValidationWarningVO> warnings = List.of(
            new ValidationWarningVO(1, "边类型未在本体中定义", "建议在本体中添加边类型定义"));
        ValidationResultVO result = ValidationResultVO.passWithWarnings(warnings);
        assertTrue(result.isPassed());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    void testValidationErrorVO_of() {
        ValidationErrorVO err = ValidationErrorVO.of(
            1, "ONT001", "类型未定义", "type", "UnknownType");
        assertEquals("ONT001", err.getCode());
        assertEquals("类型未定义", err.getMessage());
        assertEquals("UnknownType", err.getAttemptedValue());
    }
}
