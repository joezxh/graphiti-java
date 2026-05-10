package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OntologyPropertyServiceImplTest {

    @Test
    void testOntPropertyVO_builder() {
        OntPropertyVO vo = OntPropertyVO.builder()
            .localName("hasPrice")
            .propertyUri("http://example.org/hasPrice")
            .propertyType("DATATYPE")
            .rangeDataType("float")
            .isRequired(true)
            .isMultiple(false)
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.valueOf(10000))
            .build();
        assertEquals("hasPrice", vo.getLocalName());
        assertEquals("DATATYPE", vo.getPropertyType());
        assertTrue(vo.getIsRequired());
        assertFalse(vo.getIsMultiple());
        assertEquals(BigDecimal.ZERO, vo.getMinValue());
    }

    @Test
    void testOntPropertyVO_allowedValues() {
        OntPropertyVO vo = new OntPropertyVO();
        vo.setLocalName("status");
        vo.setAllowedValues(List.of("active", "inactive", "pending"));
        assertEquals(3, vo.getAllowedValues().size());
        assertTrue(vo.getAllowedValues().contains("active"));
    }
}
