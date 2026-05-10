package com.graphiti.module.graphiti.dal;

import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OntDOTest {
    @Test
    void testOntClassDO_settersAndGetters() {
        OntClassDO ontClass = new OntClassDO();
        ontClass.setId(1L);
        ontClass.setClassUri("http://example.org/Person");
        ontClass.setLocalName("Person");
        ontClass.setDomainHint("KNOWLEDGE");
        ontClass.setParentClassId(null);

        assertEquals(1L, ontClass.getId());
        assertEquals("Person", ontClass.getLocalName());
        assertEquals("KNOWLEDGE", ontClass.getDomainHint());
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

        assertTrue(prop.getIsRequired());
        assertFalse(prop.getIsMultiple());
        assertEquals("DATATYPE", prop.getPropertyType());
    }
}
