package com.ontograph.module.graphiti.dal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntConstraintDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class OntDOTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testOntClassDO_settersAndGetters() {
        OntClassDO ontClass = new OntClassDO();
        ontClass.setId(1L);
        ontClass.setClassUri("http://example.org/Person");
        ontClass.setLocalName("Person");
        ontClass.setDomainHint("KNOWLEDGE");
        ontClass.setParentClassId(null);
        ontClass.setEquivalentTo("[\"http://schema.org/Person\"]"); // JSON string
        ontClass.setMetadata("{\"icon\":\"user\"}"); // JSON string

        assertEquals(1L, ontClass.getId());
        assertEquals("Person", ontClass.getLocalName());
        assertEquals("KNOWLEDGE", ontClass.getDomainHint());
        // Verify JSON string fields round-trip
        assertEquals("[\"http://schema.org/Person\"]", ontClass.getEquivalentTo());
        assertEquals("{\"icon\":\"user\"}", ontClass.getMetadata());
    }

    @Test
    void testOntPropertyDO_requiredAndMultiple() {
        OntPropertyDO prop = new OntPropertyDO();
        prop.setPropertyUri("http://example.org/hasName");
        prop.setLocalName("hasName");
        prop.setPropertyType("DATATYPE");
        prop.setRangeDataType("string");
        prop.setIsRequired(true);
        prop.setIsMultiple(false);
        prop.setDomainClassId(10L);
        prop.setRangeClassId(20L);
        prop.setAllowedValues("[\"red\",\"green\",\"blue\"]"); // JSON string

        assertTrue(prop.getIsRequired());
        assertFalse(prop.getIsMultiple());
        assertEquals("DATATYPE", prop.getPropertyType());
        assertEquals(10L, prop.getDomainClassId());
        assertEquals(20L, prop.getRangeClassId());
        assertEquals("[\"red\",\"green\",\"blue\"]", prop.getAllowedValues());
    }

    @Test
    void testOntPropertyDO_cardinalityAndRange() {
        OntPropertyDO prop = new OntPropertyDO();
        prop.setMinCardinality(1);
        prop.setMaxCardinality(5);
        prop.setMinValue(new BigDecimal("0.0"));
        prop.setMaxValue(new BigDecimal("100.0"));
        prop.setPattern("^[A-Z].*");

        assertEquals(1, prop.getMinCardinality());
        assertEquals(5, prop.getMaxCardinality());
        assertEquals(new BigDecimal("0.0"), prop.getMinValue());
        assertEquals(new BigDecimal("100.0"), prop.getMaxValue());
        assertEquals("^[A-Z].*", prop.getPattern());
    }

    @Test
    void testOntConstraintDO_jsonValue() {
        OntConstraintDO constraint = new OntConstraintDO();
        String rangeConstraint = "{\"min\": 1, \"max\": 5}";
        constraint.setValue(rangeConstraint);
        constraint.setConstraintType("RANGE");
        constraint.setSeverity("ERROR");

        assertEquals("RANGE", constraint.getConstraintType());
        assertEquals("ERROR", constraint.getSeverity());
        // Verify it's stored as a plain JSON string (no TypeHandler needed for TEXT)
        assertEquals(rangeConstraint, constraint.getValue());

        // Verify we can parse it back
        assertDoesNotThrow(() -> objectMapper.readTree(constraint.getValue()));
    }
}
