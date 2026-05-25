package com.ontograph.module.graphiti.dal.mysql.ont;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OntMapperTest {
    @Test
    void testOntDefinitionMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper"));
    }

    @Test
    void testOntClassMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper"));
    }

    @Test
    void testOntPropertyMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper"));
    }

    @Test
    void testOntConstraintMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntConstraintMapper"));
    }

    @Test
    void testOntVersionHistoryMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntVersionHistoryMapper"));
    }

    @Test
    void testOntMappingMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName(
            "com.graphiti.module.graphiti.dal.mysql.ont.OntMappingMapper"));
    }
}
