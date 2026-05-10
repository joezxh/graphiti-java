package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OntologyClassServiceImplTest {

    @Test
    void testOntClassVO_setters() {
        OntClassVO vo = new OntClassVO();
        vo.setLocalName("Person");
        vo.setClassUri("http://example.org/Person");
        vo.setDomainHint("KNOWLEDGE");
        vo.setDescription("Represents a person");
        vo.setEquivalentTo(List.of("http://example.org/Human"));
        assertEquals("Person", vo.getLocalName());
        assertEquals("KNOWLEDGE", vo.getDomainHint());
        assertEquals(1, vo.getEquivalentTo().size());
    }

    @Test
    void testOntClassVO_builder() {
        OntClassVO vo = OntClassVO.builder()
            .localName("Product")
            .classUri("http://example.org/Product")
            .domainHint("ECOMMERCE")
            .description("A product")
            .build();
        assertEquals("Product", vo.getLocalName());
        assertEquals("ECOMMERCE", vo.getDomainHint());
    }

    @Test
    void testClassHierarchyVO_builder() {
        ClassHierarchyVO child = ClassHierarchyVO.builder()
            .localName("Doctor")
            .classUri("http://example.org/Doctor")
            .children(List.of())
            .build();
        ClassHierarchyVO root = ClassHierarchyVO.builder()
            .localName("Person")
            .classUri("http://example.org/Person")
            .children(List.of(child))
            .build();
        assertEquals(1, root.getChildren().size());
        assertEquals("Doctor", root.getChildren().get(0).getLocalName());
    }
}
