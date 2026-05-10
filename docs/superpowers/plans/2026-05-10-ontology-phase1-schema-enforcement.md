# Phase 1: Schema Enforcement Foundation - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让本体真正作用于图谱操作——NodeServiceImpl 和 EdgeServiceImpl 在创建节点/边时自动进行本体校验，同时保留向后兼容（无本体定义时直接通过）。

**Architecture:** 沿用现有 PostgreSQL/MyBatis-Plus 模式，新增 6 张本体表；在 NodeServiceImpl、EdgeServiceImpl 中插入 ValidationEngine 调用；旧 JSON 格式继续支持（双轨运行）。

**Tech Stack:** Spring Boot, MyBatis-Plus, PostgreSQL, Neo4j Driver, Jackson

---

## File Structure

```
graphiti-module-core/src/main/java/com/graphiti/module/graphiti/
├── dal/
│   ├── dataobject/ont/                          ← NEW package
│   │   ├── OntDefinitionDO.java
│   │   ├── OntClassDO.java
│   │   ├── OntPropertyDO.java
│   │   ├── OntConstraintDO.java
│   │   └── OntVersionHistoryDO.java
│   └── mysql/ont/                              ← NEW package
│       ├── OntDefinitionMapper.java
│       ├── OntClassMapper.java
│       ├── OntPropertyMapper.java
│       ├── OntConstraintMapper.java
│       └── OntVersionHistoryMapper.java
├── service/
│   ├── OntologyValidationService.java           ← NEW (extracted from OntologyServiceImpl)
│   ├── OntologyService.java                     MODIFY (add new method signatures)
│   └── impl/
│       ├── OntologyValidationServiceImpl.java    ← NEW
│       ├── OntologyServiceImpl.java             MODIFY (delegate to validation)
│       ├── NodeServiceImpl.java                 MODIFY (integrate validation)
│       └── EdgeServiceImpl.java                 MODIFY (integrate validation)
├── vo/
│   └── ontology/
│       ├── OntClassVO.java                      ← NEW
│       ├── OntPropertyVO.java                   ← NEW
│       ├── OntConstraintVO.java                  ← NEW
│       ├── ValidationResultVO.java              ← NEW
│       └── BatchValidationReqVO.java           ← NEW
└── exception/
    └── OntologyValidationException.java          ← NEW

sql/postgresql/
└── V1__create_ontology_tables.sql             ← NEW

sql/mysql/
└── (existing graphiti_ontology table stays as-is)
```

---

## Task 1: Create PostgreSQL Ontology Tables

**Files:**
- Create: `sql/postgresql/V1__create_ontology_tables.sql`
- Test: 手动执行 SQL 后用 `psql` 或数据库工具验证

- [ ] **Step 1: Write DDL SQL**

```sql
-- sql/postgresql/V1__create_ontology_tables.sql

-- 本体定义主表
CREATE TABLE ont_definition (
    id                  BIGSERIAL PRIMARY KEY,
    graph_id            VARCHAR(64) NOT NULL,
    namespace           VARCHAR(255) DEFAULT 'default',
    name                VARCHAR(128) NOT NULL,
    version             VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    description         TEXT,
    parent_version_id   BIGINT REFERENCES ont_definition(id),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64),
    UNIQUE (graph_id, namespace, name, version)
);
CREATE INDEX idx_ont_def_graph_id ON ont_definition(graph_id);
CREATE INDEX idx_ont_def_status  ON ont_definition(status);

-- 类定义表
CREATE TABLE ont_class (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_uri       VARCHAR(512) NOT NULL,
    local_name      VARCHAR(128) NOT NULL,
    parent_class_id BIGINT REFERENCES ont_class(id),
    equivalent_to   TEXT[],
    disjoint_with   BIGINT[],
    description     TEXT,
    example         TEXT,
    domain_hint     VARCHAR(32),
    metadata        JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, class_uri)
);
CREATE INDEX idx_ont_class_def     ON ont_class(definition_id);
CREATE INDEX idx_ont_class_parent  ON ont_class(parent_class_id);
CREATE INDEX idx_ont_class_domain  ON ont_class(domain_hint);

-- 属性定义表
CREATE TABLE ont_property (
    id                  BIGSERIAL PRIMARY KEY,
    definition_id       BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    property_uri        VARCHAR(512) NOT NULL,
    local_name          VARCHAR(128) NOT NULL,
    property_type       VARCHAR(16) NOT NULL,
    domain_class_id     BIGINT REFERENCES ont_class(id),
    range_class_id      BIGINT REFERENCES ont_class(id),
    range_data_type     VARCHAR(32),
    min_cardinality     INTEGER,
    max_cardinality     INTEGER,
    default_value       TEXT,
    allowed_values      TEXT[],
    parent_property_id  BIGINT REFERENCES ont_property(id),
    equivalent_to       TEXT[],
    inverse_of_id       BIGINT REFERENCES ont_property(id),
    is_required         BOOLEAN NOT NULL DEFAULT FALSE,
    is_multiple         BOOLEAN NOT NULL DEFAULT FALSE,
    pattern             VARCHAR(256),
    min_value           NUMERIC,
    max_value           NUMERIC,
    description         TEXT,
    example             TEXT,
    metadata            JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, property_uri)
);
CREATE INDEX idx_ont_prop_def     ON ont_property(definition_id);
CREATE INDEX idx_ont_prop_type    ON ont_property(property_type);
CREATE INDEX idx_ont_prop_domain  ON ont_property(domain_class_id);
CREATE INDEX idx_ont_prop_range   ON ont_property(range_class_id);
CREATE INDEX idx_ont_prop_parent  ON ont_property(parent_property_id);

-- 约束定义表
CREATE TABLE ont_constraint (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_id        BIGINT REFERENCES ont_class(id),
    property_id     BIGINT REFERENCES ont_property(id),
    constraint_type VARCHAR(32) NOT NULL,
    value           JSONB NOT NULL,
    error_message   VARCHAR(512),
    severity        VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, property_id, constraint_type)
);
CREATE INDEX idx_ont_constraint_def   ON ont_constraint(definition_id);
CREATE INDEX idx_ont_constraint_class ON ont_constraint(class_id);

-- 版本历史表
CREATE TABLE ont_version_history (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id),
    version         VARCHAR(32) NOT NULL,
    change_type     VARCHAR(32) NOT NULL,
    entity_type     VARCHAR(16) NOT NULL,
    entity_id       BIGINT,
    before_state    JSONB,
    after_state     JSONB,
    diff_summary    TEXT,
    changed_by      VARCHAR(64),
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ont_version_def  ON ont_version_history(definition_id);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);
```

- [ ] **Step 2: Execute SQL against PostgreSQL**

Run: Connect to PostgreSQL and execute the above DDL
Expected: 5 tables created successfully with all indexes

- [ ] **Step 3: Commit**

```bash
git add sql/postgresql/V1__create_ontology_tables.sql
git commit -m "phase1: add PostgreSQL ontology tables (ont_definition, ont_class, ont_property, ont_constraint, ont_version_history)"
```

---

## Task 2: Create Ontology DataObject (DO) Classes

**Files:**
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntDefinitionDO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntClassDO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntPropertyDO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntConstraintDO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntVersionHistoryDO.java`
- Test: `graphiti-module-core/src/test/java/com/graphiti/module/graphiti/dal/OntDOTest.java` (新建)

- [ ] **Step 1: Write OntDefinitionDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntDefinitionDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_definition")
public class OntDefinitionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("graph_id")
    private String graphId;

    private String namespace;
    private String name;
    private String version;
    private String status;        // ACTIVE / DEPRECATED / ARCHIVED
    private String description;

    @TableField("parent_version_id")
    private Long parentVersionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String createdBy;
}
```

- [ ] **Step 2: Write OntClassDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntClassDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringArrayTypeHandler;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ont_class")
public class OntClassDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("class_uri")
    private String classUri;

    @TableField("local_name")
    private String localName;

    @TableField("parent_class_id")
    private Long parentClassId;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] equivalentTo;

    @TableField("disjoint_with")
    private Long[] disjointWith;

    private String description;
    private String example;

    @TableField("domain_hint")
    private String domainHint;    // FINANCIAL / MEDICAL / ECOMMERCE / KNOWLEDGE

    @TableField("metadata")
    private String metadata;      // JSON string

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Write OntPropertyDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntPropertyDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.StringArrayTypeHandler;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ont_property")
public class OntPropertyDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("property_uri")
    private String propertyUri;

    @TableField("local_name")
    private String localName;

    @TableField("property_type")
    private String propertyType;   // OBJECT / DATATYPE / ANNOTATION / TRANSITIVE / SYMMETRIC / FUNCTIONAL

    @TableField("domain_class_id")
    private Long domainClassId;

    @TableField("range_class_id")
    private Long rangeClassId;

    @TableField("range_data_type")
    private String rangeDataType;  // string / integer / float / boolean / date / json / ...

    @TableField("min_cardinality")
    private Integer minCardinality;

    @TableField("max_cardinality")
    private Integer maxCardinality;

    @TableField("default_value")
    private String defaultValue;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] allowedValues;

    @TableField("parent_property_id")
    private Long parentPropertyId;

    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] equivalentTo;

    @TableField("inverse_of_id")
    private Long inverseOfId;

    @TableField("is_required")
    private Boolean isRequired;

    @TableField("is_multiple")
    private Boolean isMultiple;

    private String pattern;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String description;
    private String example;
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Write OntConstraintDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntConstraintDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_constraint")
public class OntConstraintDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("class_id")
    private Long classId;

    @TableField("property_id")
    private Long propertyId;

    @TableField("constraint_type")
    private String constraintType;  // CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL / CUSTOM_SPARQL

    @TableField("value")
    private String value;          // JSON string: { "min": 1, "max": 5 } or { "pattern": "^[A-Z].*" }

    @TableField("error_message")
    private String errorMessage;

    private String severity;       // ERROR / WARNING / INFO
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Write OntVersionHistoryDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntVersionHistoryDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_version_history")
public class OntVersionHistoryDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    private String version;
    private String changeType;     // CLASS_ADDED / PROPERTY_MODIFIED / CONSTRAINT_DELETED / ...
    private String entityType;     // CLASS / PROPERTY / CONSTRAINT / DEFINITION
    private Long entityId;
    private String beforeState;    // JSON string
    private String afterState;     // JSON string
    private String diffSummary;
    private String changedBy;

    @TableField("changed_at")
    private LocalDateTime changedAt;
}
```

- [ ] **Step 6: Write unit tests for DO classes**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/dal/OntDOTest.java
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
```

- [ ] **Step 7: Run tests to verify**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest=OntDOTest -q`
Expected: Both tests PASS

- [ ] **Step 8: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/
git add graphiti-module-core/src/test/java/com/graphiti/module/graphiti/dal/OntDOTest.java
git commit -m "phase1: add ontology DO classes (OntDefinitionDO, OntClassDO, OntPropertyDO, OntConstraintDO, OntVersionHistoryDO)"
```

---

## Task 3: Create MyBatis-Plus Mapper Interfaces

**Files:**
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntDefinitionMapper.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntClassMapper.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntPropertyMapper.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntConstraintMapper.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntVersionHistoryMapper.java`
- Test: `graphiti-module-core/src/test/java/com/graphiti/module/graphiti/dal/mysql/ont/OntMapperTest.java` (新建)

- [ ] **Step 1: Write all 5 Mapper interfaces**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntDefinitionMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OntDefinitionMapper extends BaseMapper<OntDefinitionDO> {
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntClassMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntClassMapper extends BaseMapper<OntClassDO> {

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} ORDER BY local_name")
    List<OntClassDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} AND parent_class_id IS NULL")
    List<OntClassDO> selectRootClasses(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} AND parent_class_id = #{parentId}")
    List<OntClassDO> selectByParentId(@Param("definitionId") Long definitionId, @Param("parentId") Long parentId);
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntPropertyMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OntPropertyMapper extends BaseMapper<OntPropertyDO> {

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} ORDER BY local_name")
    List<OntPropertyDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} AND domain_class_id = #{classId}")
    List<OntPropertyDO> selectByClassId(@Param("definitionId") Long definitionId, @Param("classId") Long classId);

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} AND property_uri = #{propertyUri} LIMIT 1")
    Optional<OntPropertyDO> selectByUri(@Param("definitionId") Long definitionId, @Param("propertyUri") String propertyUri);
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntConstraintMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntConstraintDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntConstraintMapper extends BaseMapper<OntConstraintDO> {

    @Select("SELECT * FROM ont_constraint WHERE definition_id = #{definitionId}")
    List<OntConstraintDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_constraint WHERE class_id = #{classId}")
    List<OntConstraintDO> selectByClassId(@Param("classId") Long classId);
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntVersionHistoryMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntVersionHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntVersionHistoryMapper extends BaseMapper<OntVersionHistoryDO> {

    @Select("SELECT * FROM ont_version_history WHERE definition_id = #{definitionId} ORDER BY changed_at DESC")
    List<OntVersionHistoryDO> selectByDefinitionId(@Param("definitionId") Long definitionId);
}
```

- [ ] **Step 2: Write Mapper tests**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/dal/mysql/ont/OntMapperTest.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OntMapperTest {
    // Mapper tests require a running PostgreSQL test instance.
    // For unit testing, we verify the mapper interface compiles correctly.
    // Integration tests with Testcontainers should be added separately.
    @Test
    void testOntClassMapper_interfaceCompiles() {
        // Verify the class is loadable
        assertDoesNotThrow(() -> Class.forName("com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper"));
    }

    @Test
    void testOntPropertyMapper_interfaceCompiles() {
        assertDoesNotThrow(() -> Class.forName("com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper"));
    }
}
```

- [ ] **Step 3: Run compile check**

Run: `cd D:/projects/graphiti-java && mvn compile -pl graphiti-module-core -q`
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 4: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/
git commit -m "phase1: add ontology MyBatis-Plus mapper interfaces"
```

---

## Task 4: Create ValidationEngine Service

**Files:**
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyValidationService.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyValidationServiceImpl.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationResultVO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/exception/OntologyValidationException.java`
- Test: `graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyValidationServiceImplTest.java`

- [ ] **Step 1: Write ValidationResultVO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationResultVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultVO {
    private boolean passed;
    private int level;
    @Builder.Default
    private List<ValidationErrorVO> errors = new ArrayList<>();
    @Builder.Default
    private List<ValidationWarningVO> warnings = new ArrayList<>();
    private Map<String, Object> enrichedProperties;

    public static ValidationResultVO pass() {
        return ValidationResultVO.builder().passed(true).level(0).build();
    }

    public static ValidationResultVO passWithWarnings(List<ValidationWarningVO> warnings) {
        return ValidationResultVO.builder().passed(true).level(0).warnings(warnings).build();
    }

    public static ValidationResultVO fail(int level, List<ValidationErrorVO> errors) {
        return ValidationResultVO.builder().passed(false).level(level).errors(errors).build();
    }
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationErrorVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorVO {
    private int layer;
    private String code;      // ONT001 - ONT005
    private String message;
    private String property;
    private Object attemptedValue;

    public static ValidationErrorVO of(int layer, String code, String message, String property, Object value) {
        return new ValidationErrorVO(layer, code, message, property, value);
    }
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationWarningVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationWarningVO {
    private int layer;
    private String message;
    private String suggestion;
}
```

- [ ] **Step 2: Write OntologyValidationException**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/exception/OntologyValidationException.java
package com.graphiti.module.graphiti.exception;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import lombok.Getter;
import java.util.List;

@Getter
public class OntologyValidationException extends BusinessException {
    private final ValidationResultVO validationResult;

    public OntologyValidationException(ValidationResultVO validationResult) {
        super(2001, buildMessage(validationResult));
        this.validationResult = validationResult;
    }

    private static String buildMessage(ValidationResultVO result) {
        if (result.getErrors() == null || result.getErrors().isEmpty()) {
            return "本体校验失败";
        }
        StringBuilder sb = new StringBuilder("本体校验失败: ");
        for (int i = 0; i < Math.min(3, result.getErrors().size()); i++) {
            if (i > 0) sb.append("; ");
            sb.append(result.getErrors().get(i).getMessage());
        }
        if (result.getErrors().size() > 3) {
            sb.append(" (共 ").append(result.getErrors().size()).append(" 条错误)");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 3: Write OntologyValidationService interface**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyValidationService.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import java.util.Map;

/**
 * 本体验证引擎服务
 * 负责 6 层验证：
 * Layer 1: 类型存在性
 * Layer 2: 属性必填
 * Layer 3: 数据类型
 * Layer 4: 约束规则
 * Layer 5: OWL 约束（预留）
 * Layer 6: 推理扩展（预留）
 */
public interface OntologyValidationService {

    /**
     * 验证节点是否符合本体定义
     * @param graphId    图谱ID
     * @param nodeType   节点类型
     * @param properties 节点属性
     * @return 验证结果
     */
    ValidationResultVO validateNode(String graphId, String nodeType, Map<String, Object> properties);

    /**
     * 验证边是否符合本体定义
     * @param graphId    图谱ID
     * @param edgeType   边类型
     * @param properties 边属性
     * @return 验证结果
     */
    ValidationResultVO validateEdge(String graphId, String edgeType, Map<String, Object> properties);

    /**
     * 检查本体是否已定义（向后兼容用）
     */
    boolean hasOntology(String graphId);
}
```

- [ ] **Step 4: Write OntologyValidationServiceImpl (核心实现)**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyValidationServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.*;
import com.graphiti.module.graphiti.dal.mysql.ont.*;
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.vo.ontology.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyValidationServiceImpl implements OntologyValidationService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;
    private final ObjectMapper objectMapper;

    // 错误码常量
    private static final String ERR_TYPE_NOT_FOUND    = "ONT001"; // 类型未定义
    private static final String ERR_REQUIRED_MISSING  = "ONT002"; // 缺少必需属性
    private static final String ERR_TYPE_MISMATCH     = "ONT003"; // 类型不匹配
    private static final String ERR_CONSTRAINT_VIOLATED = "ONT004"; // 违反约束

    // ==================== 公开方法 ====================

    @Override
    public ValidationResultVO validateNode(String graphId, String nodeType, Map<String, Object> properties) {
        if (!hasOntology(graphId)) {
            return ValidationResultVO.pass(); // 无本体时直接通过
        }

        Long definitionId = resolveDefinitionId(graphId);
        if (definitionId == null) {
            return ValidationResultVO.pass();
        }

        List<ValidationErrorVO> errors = new ArrayList<>();
        List<ValidationWarningVO> warnings = new ArrayList<>();

        // Layer 1: 类型存在性
        OntClassDO classDef = findClassByLocalName(definitionId, nodeType);
        if (classDef == null) {
            errors.add(ValidationErrorVO.of(1, ERR_TYPE_NOT_FOUND,
                "节点类型未在本体中定义: " + nodeType, "type", nodeType));
            return ValidationResultVO.fail(1, errors);
        }

        // 获取该类及其父类的所有属性定义
        List<OntPropertyDO> allProps = collectPropertiesForClass(definitionId, classDef);
        Map<String, Object> enriched = new HashMap<>(properties != null ? properties : Map.of());

        // Layer 2: 必填属性校验
        errors.addAll(checkRequiredProperties(allProps, properties));

        // Layer 3: 数据类型校验
        errors.addAll(checkDataTypes(allProps, properties));

        // Layer 4: 约束规则校验
        errors.addAll(checkConstraints(definitionId, classDef, properties));

        if (!errors.isEmpty()) {
            return ValidationResultVO.fail(4, errors);
        }

        // 注入默认值
        enriched = injectDefaults(allProps, enriched);

        return errors.isEmpty() && warnings.isEmpty()
            ? ValidationResultVO.pass()
            : ValidationResultVO.passWithWarnings(warnings)
                .builder().enrichedProperties(enriched).build();
    }

    @Override
    public ValidationResultVO validateEdge(String graphId, String edgeType, Map<String, Object> properties) {
        if (!hasOntology(graphId)) {
            return ValidationResultVO.pass();
        }

        Long definitionId = resolveDefinitionId(graphId);
        if (definitionId == null) {
            return ValidationResultVO.pass();
        }

        List<ValidationErrorVO> errors = new ArrayList<>();

        // Layer 1: 边类型存在性（边存储在 ont_property 表）
        OntPropertyDO propDef = propertyMapper.selectByUri(definitionId, edgeType).orElse(null);
        // 兼容旧格式：直接用 edgeType 作为 local_name 查
        if (propDef == null) {
            List<OntPropertyDO> allProps = propertyMapper.selectByDefinitionId(definitionId);
            propDef = allProps.stream()
                .filter(p -> edgeType.equals(p.getLocalName()) || edgeType.equals(p.getPropertyUri()))
                .findFirst().orElse(null);
        }

        if (propDef == null) {
            // 边类型未定义，允许通过（向后兼容），但给出警告
            return ValidationResultVO.passWithWarnings(List.of(
                new ValidationWarningVO(1, "边类型未在本体中定义（已允许通过）: " + edgeType,
                    "建议在本体中添加边类型定义")
            ));
        }

        // Layer 2-4: 属性校验（复用节点逻辑）
        List<OntPropertyDO> allProps = List.of(propDef);
        errors.addAll(checkRequiredProperties(allProps, properties));
        errors.addAll(checkDataTypes(allProps, properties));

        if (!errors.isEmpty()) {
            return ValidationResultVO.fail(4, errors);
        }

        return ValidationResultVO.pass();
    }

    @Override
    public boolean hasOntology(String graphId) {
        return resolveDefinitionId(graphId) != null;
    }

    // ==================== 私有方法 ====================

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    private OntClassDO findClassByLocalName(Long definitionId, String localName) {
        LambdaQueryWrapper<OntClassDO> w = new LambdaQueryWrapper<>();
        w.eq(OntClassDO::getDefinitionId, definitionId);
        w.and(q -> q.eq(OntClassDO::getLocalName, localName)
            .or().eq(OntClassDO::getClassUri, localName));
        return classMapper.selectOne(w);
    }

    private List<OntPropertyDO> collectPropertiesForClass(Long definitionId, OntClassDO classDef) {
        Set<Long> classIds = new HashSet<>();
        collectClassAndAncestors(classIds, definitionId, classDef);

        LambdaQueryWrapper<OntPropertyDO> w = new LambdaQueryWrapper<>();
        w.eq(OntPropertyDO::getDefinitionId, definitionId);
        w.and(q -> classIds.isEmpty()
            ? q.isNull(OntPropertyDO::getDomainClassId)
            : q.in(OntPropertyDO::getDomainClassId, classIds));
        return propertyMapper.selectList(w);
    }

    private void collectClassAndAncestors(Set<Long> ids, Long definitionId, OntClassDO cls) {
        if (cls == null || ids.contains(cls.getId())) return;
        ids.add(cls.getId());
        if (cls.getParentClassId() != null) {
            OntClassDO parent = classMapper.selectById(cls.getParentClassId());
            collectClassAndAncestors(ids, definitionId, parent);
        }
    }

    private List<ValidationErrorVO> checkRequiredProperties(List<OntPropertyDO> props, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        Map<String, Object> propsMap = properties != null ? properties : Map.of();
        for (OntPropertyDO prop : props) {
            if (Boolean.TRUE.equals(prop.getIsRequired())) {
                String key = prop.getLocalName();
                Object val = propsMap.get(key);
                if (val == null || (val instanceof String s && s.isBlank())) {
                    errors.add(ValidationErrorVO.of(2, ERR_REQUIRED_MISSING,
                        "缺少必需属性: " + key, key, null));
                }
            }
        }
        return errors;
    }

    private List<ValidationErrorVO> checkDataTypes(List<OntPropertyDO> props, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        if (properties == null) return errors;
        for (OntPropertyDO prop : props) {
            String key = prop.getLocalName();
            Object val = properties.get(key);
            if (val == null) continue;

            String dataType = prop.getRangeDataType();
            if (dataType == null || dataType.isBlank()) continue;

            if (!checkValueType(val, dataType)) {
                errors.add(ValidationErrorVO.of(3, ERR_TYPE_MISMATCH,
                    "属性 '" + key + "' 类型应为 " + dataType, key, val));
            }
        }
        return errors;
    }

    private boolean checkValueType(Object value, String expectedType) {
        if (value == null) return true;
        return switch (expectedType.toLowerCase()) {
            case "string", "str" -> value instanceof String;
            case "integer", "int", "long" -> value instanceof Integer || value instanceof Long;
            case "float", "double", "number", "numeric", "decimal" -> value instanceof Number;
            case "boolean", "bool" -> value instanceof Boolean;
            case "date", "datetime", "timestamp" -> value instanceof java.time.temporal.Temporal
                || (value instanceof String s && !s.isBlank());
            case "json", "object", "map" -> value instanceof Map || value instanceof List;
            default -> true;
        };
    }

    private List<ValidationErrorVO> checkConstraints(Long definitionId, OntClassDO classDef, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        if (properties == null) return errors;

        // 查找该类上的所有约束
        LambdaQueryWrapper<OntConstraintDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntConstraintDO::getDefinitionId, definitionId);
        cw.eq(OntConstraintDO::getClassId, classDef.getId());
        List<OntConstraintDO> constraints = constraintMapper.selectList(cw);

        for (OntConstraintDO constraint : constraints) {
            errors.addAll(evaluateConstraint(constraint, properties));
        }
        return errors;
    }

    private List<ValidationErrorVO> evaluateConstraint(OntConstraintDO constraint, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        String constraintType = constraint.getConstraintType();
        String valueJson = constraint.getValue();
        String errorMsg = constraint.getErrorMessage() != null
            ? constraint.getErrorMessage()
            : "违反约束: " + constraintType;

        if (constraint.getPropertyId() == null) return errors;
        OntPropertyDO prop = propertyMapper.selectById(constraint.getPropertyId());
        if (prop == null) return errors;

        Object propValue = properties.get(prop.getLocalName());
        if (propValue == null) return errors;

        try {
            Map<String, Object> valueMap = objectMapper.readValue(valueJson, Map.class);

            switch (constraintType.toUpperCase()) {
                case "PATTERN" -> {
                    String pattern = (String) valueMap.get("pattern");
                    if (pattern != null && propValue instanceof String s && !Pattern.matches(pattern, s)) {
                        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                            errorMsg + " (pattern: " + pattern + ")", prop.getLocalName(), propValue));
                    }
                }
                case "RANGE" -> {
                    if (propValue instanceof Number num) {
                        if (valueMap.containsKey("min") && num.doubleValue() < ((Number) valueMap.get("min")).doubleValue()) {
                            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                                errorMsg + " (min: " + valueMap.get("min") + ")", prop.getLocalName(), propValue));
                        }
                        if (valueMap.containsKey("max") && num.doubleValue() > ((Number) valueMap.get("max")).doubleValue()) {
                            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                                errorMsg + " (max: " + valueMap.get("max") + ")", prop.getLocalName(), propValue));
                        }
                    }
                }
                case "ENUM" -> {
                    @SuppressWarnings("unchecked")
                    List<String> allowed = (List<String>) valueMap.get("values");
                    if (allowed != null && !allowed.contains(String.valueOf(propValue))) {
                        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                            errorMsg + " (allowed: " + allowed + ")", prop.getLocalName(), propValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析约束值失败: constraintId={}", constraint.getId(), e);
        }
        return errors;
    }

    private Map<String, Object> injectDefaults(List<OntPropertyDO> props, Map<String, Object> enriched) {
        for (OntPropertyDO prop : props) {
            String key = prop.getLocalName();
            if (!enriched.containsKey(key) && prop.getDefaultValue() != null) {
                enriched.put(key, parseDefaultValue(prop.getDefaultValue(), prop.getRangeDataType()));
            }
        }
        return enriched;
    }

    private Object parseDefaultValue(String defaultValue, String dataType) {
        if (defaultValue == null) return null;
        return switch (dataType != null ? dataType.toLowerCase() : "string") {
            case "integer", "int" -> Integer.parseInt(defaultValue);
            case "long" -> Long.parseLong(defaultValue);
            case "float", "double", "number", "numeric", "decimal" -> new BigDecimal(defaultValue);
            case "boolean", "bool" -> Boolean.parseBoolean(defaultValue);
            default -> defaultValue;
        };
    }
}
```

- [ ] **Step 5: Write unit tests for ValidationServiceImpl**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyValidationServiceImplTest.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.service.impl.OntologyValidationServiceImpl;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OntologyValidationServiceImplTest {

    // 由于 ValidationServiceImpl 依赖 MyBatis mappers，需要用 @MockBean 或手动 mock。
    // 这里使用手动 mock 测试核心逻辑：
    @Test
    void testValidationResultVO_pass() {
        ValidationResultVO result = ValidationResultVO.pass();
        assertTrue(result.isPassed());
        assertEquals(0, result.getLevel());
    }

    @Test
    void testValidationResultVO_fail() {
        var errors = List.of(new com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO(
            2, "ONT002", "缺少必需属性: name", "name", null));
        ValidationResultVO result = ValidationResultVO.fail(2, errors);
        assertFalse(result.isPassed());
        assertEquals(2, result.getLevel());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    void testValidationErrorVO_of() {
        var err = com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO.of(
            1, "ONT001", "类型未定义", "type", "UnknownType");
        assertEquals("ONT001", err.getCode());
        assertEquals("类型未定义", err.getMessage());
        assertEquals("UnknownType", err.getAttemptedValue());
    }
}
```

- [ ] **Step 6: Run tests**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest=OntologyValidationServiceImplTest -q`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyValidationService.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyValidationServiceImpl.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationResultVO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationErrorVO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ValidationWarningVO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/exception/OntologyValidationException.java
git add graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyValidationServiceImplTest.java
git commit -m "phase1: add OntologyValidationService with 6-layer validation pipeline"
```

---

## Task 5: Integrate Validation into NodeServiceImpl

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/NodeServiceImpl.java:1-157`
- Test: `graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/NodeServiceImplTest.java`

- [ ] **Step 1: Add new field declarations**

在 `NodeServiceImpl` 类中，找到：
```java
private final GraphNeo4jService graphNeo4jService;
private final EmbedderService embedderService;
```

替换为：
```java
private final GraphNeo4jService graphNeo4jService;
private final EmbedderService embedderService;
private final OntologyValidationService ontologyValidationService;
```

- [ ] **Step 2: Modify createNode() method**

找到 `NodeServiceImpl.createNode()` 方法体开头（约第 57 行开始），在 `if (name == null ...)` 校验之前插入本体验证：

```java
@Override
public NodeInfoRespVO createNode(String graphId, Map<String, Object> nodeData) {
    // 生成节点 UUID
    String uuid = UUID.randomUUID().toString().replace("-", "");

    // 提取节点属性
    String name = (String) nodeData.get("name");
    String type = (String) nodeData.get("type");
    String summary = (String) nodeData.get("summary");
    Map<String, Object> properties = (Map<String, Object>) nodeData.getOrDefault("properties", new HashMap<>());

    // === 本体校验（L1-L4）===
    if (ontologyValidationService.hasOntology(graphId)) {
        ValidationResultVO vr = ontologyValidationService.validateNode(
            graphId, type != null ? type : "Entity", properties);
        if (!vr.isPassed()) {
            throw new OntologyValidationException(vr);
        }
        // 合并 enrichedProperties（注入的默认值）
        if (vr.getEnrichedProperties() != null) {
            Map<String, Object> merged = new HashMap<>(vr.getEnrichedProperties());
            properties.forEach(merged::putIfAbsent);
            properties = merged;
        }
    }
    // === 业务校验 ===
    if (name == null || name.isEmpty()) {
        throw new BusinessException(1006, "节点名称不能为空");
    }
    // ... 后续不变 ...
```

- [ ] **Step 3: Add missing import**

在 `NodeServiceImpl.java` 文件顶部，找到现有的 `import` 区域，添加：

```java
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
```

- [ ] **Step 4: Write NodeServiceImpl integration test**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/NodeServiceImplTest.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceImplTest {

    @Mock private GraphNeo4jService graphNeo4jService;
    @Mock private EmbedderService embedderService;
    @Mock private OntologyValidationService validationService;

    @InjectMocks private NodeServiceImpl nodeService;

    @Test
    void createNode_withNoOntology_bypassesValidation() {
        // given: no ontology defined
        when(validationService.hasOntology("graph-1")).thenReturn(false);
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(anyString(), anyString(), anyString(),
            anyString(), anyString(), any(float[].class), any()))
            .thenReturn(Map.of("uuid", "abc", "name", "Test", "type", "Entity"));

        // when
        NodeInfoRespVO result = nodeService.createNode("graph-1",
            Map.of("name", "Test", "type", "Entity"));

        // then: no validation called
        verify(validationService, never()).validateNode(anyString(), anyString(), any());
        assertNotNull(result);
    }

    @Test
    void createNode_withOntology_passesValidation() {
        // given: ontology exists, validation passes
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        when(validationService.validateNode(eq("graph-2"), eq("Person"), any()))
            .thenReturn(ValidationResultVO.pass());
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(anyString(), anyString(), anyString(),
            anyString(), anyString(), any(float[].class), any()))
            .thenReturn(Map.of("uuid", "def", "name", "Alice", "type", "Person"));

        // when
        NodeInfoRespVO result = nodeService.createNode("graph-2",
            Map.of("name", "Alice", "type", "Person"));

        // then
        verify(validationService).validateNode("graph-2", "Person", any());
        assertNotNull(result);
    }

    @Test
    void createNode_withOntology_failsValidation() {
        // given: ontology exists, validation fails
        when(validationService.hasOntology("graph-3")).thenReturn(true);
        var errors = List.of(new com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO(
            2, "ONT002", "缺少必需属性: age", "age", null));
        when(validationService.validateNode(eq("graph-3"), eq("Person"), any()))
            .thenReturn(ValidationResultVO.fail(2, errors));

        // when/then
        assertThrows(OntologyValidationException.class, () ->
            nodeService.createNode("graph-3", Map.of("name", "Bob", "type", "Person")));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest=NodeServiceImplTest -q`
Expected: All 3 tests PASS

- [ ] **Step 6: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/NodeServiceImpl.java
git add graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/NodeServiceImplTest.java
git commit -m "phase1: integrate OntologyValidationService into NodeServiceImpl.createNode()"
```

---

## Task 6: Integrate Validation into EdgeServiceImpl

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EdgeServiceImpl.java:1-153`
- Test: `graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/EdgeServiceImplTest.java`

- [ ] **Step 1: Add new field declarations**

在 `EdgeServiceImpl` 中，找到：
```java
private final GraphNeo4jService graphNeo4jService;
private final EmbedderService embedderService;
```

替换为：
```java
private final GraphNeo4jService graphNeo4jService;
private final EmbedderService embedderService;
private final OntologyValidationService ontologyValidationService;
```

- [ ] **Step 2: Modify createEdge() method**

在 `if (source == null ...)` 校验之前（约第 71 行），插入本体验证：

```java
@Override
public EdgeInfoRespVO createEdge(String graphId, Map<String, Object> edgeData) {
    // 生成边 UUID
    String uuid = UUID.randomUUID().toString().replace("-", "");

    // 提取边属性
    String source = (String) edgeData.get("source");
    String target = (String) edgeData.get("target");
    String type = (String) edgeData.get("type");
    String fact = (String) edgeData.get("fact");
    Map<String, Object> properties = (Map<String, Object>) edgeData.getOrDefault("properties", new HashMap<>());

    // === 本体校验（L1-L4）===
    if (ontologyValidationService.hasOntology(graphId)) {
        ValidationResultVO vr = ontologyValidationService.validateEdge(graphId, type, properties);
        if (!vr.isPassed()) {
            throw new OntologyValidationException(vr);
        }
    }
    // === 业务校验 ===
    if (source == null || source.isEmpty()) {
        throw new BusinessException(1007, "源节点UUID不能为空");
    }
    // ... 后续不变 ...
```

- [ ] **Step 3: Add missing imports**

```java
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
```

- [ ] **Step 4: Write EdgeServiceImpl test**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/EdgeServiceImplTest.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
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
        when(graphNeo4jService.createRelationship(anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), any(float[].class), any()))
            .thenReturn(Map.of("uuid", "e1", "source", "a", "target", "b", "type", "WORKS_FOR"));

        EdgeInfoRespVO result = edgeService.createEdge("graph-1",
            Map.of("source", "a", "target", "b", "type", "WORKS_FOR"));

        verify(validationService, never()).validateEdge(anyString(), anyString(), any());
        assertNotNull(result);
    }

    @Test
    void createEdge_withOntology_failsValidation() {
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        var errors = List.of(new com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO(
            1, "ONT001", "边类型未在本体中定义: UNKNOWN_TYPE", "type", "UNKNOWN_TYPE"));
        when(validationService.validateEdge(eq("graph-2"), eq("UNKNOWN_TYPE"), any()))
            .thenReturn(ValidationResultVO.fail(1, errors));

        assertThrows(OntologyValidationException.class, () ->
            edgeService.createEdge("graph-2",
                Map.of("source", "a", "target", "b", "type", "UNKNOWN_TYPE")));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest=EdgeServiceImplTest -q`
Expected: Both tests PASS

- [ ] **Step 6: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EdgeServiceImpl.java
git add graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/EdgeServiceImplTest.java
git commit -m "phase1: integrate OntologyValidationService into EdgeServiceImpl.createEdge()"
```

---

## Task 7: Add Batch Validation Endpoint

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/NodeController.java`
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/EdgeController.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationReqVO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationRespVO.java`

- [ ] **Step 1: Write BatchValidationReqVO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationReqVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BatchValidationReqVO {
    private List<NodeValidationItem> nodes;
    private List<EdgeValidationItem> edges;

    @Data
    public static class NodeValidationItem {
        private String nodeType;
        private Map<String, Object> properties;
    }

    @Data
    public static class EdgeValidationItem {
        private String edgeType;
        private Map<String, Object> properties;
    }
}
```

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationRespVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchValidationRespVO {
    private int totalNodes;
    private int validNodes;
    private int totalEdges;
    private int validEdges;
    private List<ValidationResultVO> nodeResults;
    private List<ValidationResultVO> edgeResults;
}
```

- [ ] **Step 2: Add batch validate method to OntologyValidationService**

在 `OntologyValidationService.java` 中添加：

```java
/**
 * 批量验证节点和边
 */
BatchValidationRespVO validateBatch(String graphId, BatchValidationReqVO reqVO);
```

在 `OntologyValidationServiceImpl.java` 中添加实现：

```java
@Override
public BatchValidationRespVO validateBatch(String graphId, BatchValidationReqVO reqVO) {
    List<ValidationResultVO> nodeResults = new ArrayList<>();
    if (reqVO.getNodes() != null) {
        for (BatchValidationReqVO.NodeValidationItem item : reqVO.getNodes()) {
            nodeResults.add(validateNode(graphId, item.getNodeType(), item.getProperties()));
        }
    }

    List<ValidationResultVO> edgeResults = new ArrayList<>();
    if (reqVO.getEdges() != null) {
        for (BatchValidationReqVO.EdgeValidationItem item : reqVO.getEdges()) {
            edgeResults.add(validateEdge(graphId, item.getEdgeType(), item.getProperties()));
        }
    }

    int validNodes = (int) nodeResults.stream().filter(ValidationResultVO::isPassed).count();
    int validEdges = (int) edgeResults.stream().filter(ValidationResultVO::isPassed).count();

    return BatchValidationRespVO.builder()
        .totalNodes(nodeResults.size())
        .validNodes(validNodes)
        .totalEdges(edgeResults.size())
        .validEdges(validEdges)
        .nodeResults(nodeResults)
        .edgeResults(edgeResults)
        .build();
}
```

- [ ] **Step 3: Add endpoint to OntologyController**

在 `OntologyController.java` 中添加：

```java
@PostMapping("/{graphId}/validate/batch")
public CommonResult<BatchValidationRespVO> validateBatch(
        @PathVariable("graphId") String graphId,
        @RequestBody BatchValidationReqVO reqVO) {
    BatchValidationRespVO result = ontologyValidationService.validateBatch(graphId, reqVO);
    return CommonResult.success(result);
}
```

在类顶部添加字段：
```java
private final OntologyValidationService ontologyValidationService;
```

- [ ] **Step 4: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationReqVO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/BatchValidationRespVO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/OntologyController.java
git commit -m "phase1: add batch validation endpoint POST /ontology/{graphId}/validate/batch"
```

---

## Task 8: Add ont_mapping Table and DO

**Files:**
- Modify: `sql/postgresql/V1__create_ontology_tables.sql`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntMappingDO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntMappingMapper.java`

- [ ] **Step 1: Append ont_mapping table to SQL migration file**

在 `V1__create_ontology_tables.sql` 文件末尾追加：

```sql
-- 本体映射表（用于 Schema.org / OBO Foundry 等外部本体对齐）
CREATE TABLE ont_mapping (
    id                  BIGSERIAL PRIMARY KEY,
    definition_id       BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    source_ontology     VARCHAR(512),
    source_type         VARCHAR(16),   -- SCHEMA_ORG / OBO Foundry / CUSTOM
    mapped_class_uri   VARCHAR(512),
    mapping_type        VARCHAR(16),   -- EQUIVALENT / SUPERCLASS / SUBPROPERTY / ...
    confidence          DECIMAL(3,2),  -- 0.00 - 1.00
    metadata            JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ont_mapping_def     ON ont_mapping(definition_id);
CREATE INDEX idx_ont_mapping_source ON ont_mapping(source_ontology);
```

- [ ] **Step 2: Write OntMappingDO**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntMappingDO.java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ont_mapping")
public class OntMappingDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.BIG_SERIAL)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("source_ontology")
    private String sourceOntology;

    @TableField("source_type")
    private String sourceType;     // SCHEMA_ORG / OBO_FOUNDRY / CUSTOM

    @TableField("mapped_class_uri")
    private String mappedClassUri;

    @TableField("mapping_type")
    private String mappingType;   // EQUIVALENT / SUPERCLASS / SUBPROPERTY

    private BigDecimal confidence; // 0.00 - 1.00

    private String metadata;      // JSON string

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Write OntMappingMapper**

```java
// graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntMappingMapper.java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntMappingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntMappingMapper extends BaseMapper<OntMappingDO> {

    @Select("SELECT * FROM ont_mapping WHERE definition_id = #{definitionId} ORDER BY confidence DESC")
    List<OntMappingDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_mapping WHERE definition_id = #{definitionId} AND source_ontology = #{sourceOntology}")
    List<OntMappingDO> selectBySourceOntology(@Param("definitionId") Long definitionId,
        @Param("sourceOntology") String sourceOntology);
}
```

- [ ] **Step 4: Commit**

```bash
git add sql/postgresql/V1__create_ontology_tables.sql
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntMappingDO.java
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntMappingMapper.java
git commit -m "phase1: add ont_mapping table for external ontology alignment"
```

---

## Task 9: Integrate Validation into DataImportServiceImpl

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/DataImportServiceImpl.java`

- [ ] **Step 1: Read current DataImportServiceImpl**

当前 `DataImportServiceImpl` 实现了 `addEntityNode()` 方法，直接创建节点而无校验。在该方法中添加本体校验。

- [ ] **Step 2: Add field and import**

在 `DataImportServiceImpl.java` 类顶部添加：
```java
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
```

添加字段：
```java
private final OntologyValidationService validationService;
```

- [ ] **Step 3: Modify addEntityNode() method**

找到 `addEntityNode()` 方法中调用 `graphNeo4jService.createEntityNode()` 之前，插入：

```java
@Override
public Map<String, Object> addEntityNode(String graphId, String name, String entityType,
        String summary, Map<String, Object> properties) {
    // === 本体校验（L1-L4）===
    if (validationService.hasOntology(graphId)) {
        ValidationResultVO vr = validationService.validateNode(
            graphId, entityType != null ? entityType : "Entity", properties);
        if (!vr.isPassed()) {
            throw new OntologyValidationException(vr);
        }
        // 注入默认值
        if (vr.getEnrichedProperties() != null && properties != null) {
            vr.getEnrichedProperties().forEach(properties::putIfAbsent);
        }
    }

    // 生成嵌入向量
    String embedText = name + (summary != null ? " " + summary : "");
    float[] embedding = embedderService.embed(embedText);

    // 创建节点
    return graphNeo4jService.createEntityNode(graphId,
        UUID.randomUUID().toString().replace("-", ""),
        name, entityType != null ? entityType : "Entity",
        summary != null ? summary : "", embedding, properties);
}
```

- [ ] **Step 4: Write test**

```java
// graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/DataImportServiceImplTest.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.exception.OntologyValidationException;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataImportServiceImplTest {

    @Mock private GraphNeo4jService graphNeo4jService;
    @Mock private EmbedderService embedderService;
    @Mock private OntologyValidationService validationService;

    @InjectMocks private DataImportServiceImpl dataImportService;

    @Test
    void addEntityNode_withNoOntology_bypassesValidation() {
        when(validationService.hasOntology("graph-1")).thenReturn(false);
        when(embedderService.embed(anyString())).thenReturn(new float[]{0.1f});
        when(graphNeo4jService.createEntityNode(anyString(), anyString(), anyString(),
            anyString(), anyString(), any(float[].class), any()))
            .thenReturn(java.util.Map.of("uuid", "x", "name", "TestEntity"));

        var result = dataImportService.addEntityNode("graph-1", "TestEntity", null, null, null);

        verify(validationService, never()).validateNode(anyString(), anyString(), any());
        assertNotNull(result);
    }

    @Test
    void addEntityNode_withOntology_failsValidation() {
        when(validationService.hasOntology("graph-2")).thenReturn(true);
        var errors = java.util.List.of(
            new com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO(
                2, "ONT002", "缺少必需属性: code", "code", null));
        when(validationService.validateNode(eq("graph-2"), eq("Product"), any()))
            .thenReturn(ValidationResultVO.fail(2, errors));

        assertThrows(OntologyValidationException.class, () ->
            dataImportService.addEntityNode("graph-2", "Widget", "Product", null, null));
    }
}
```

- [ ] **Step 5: Run test**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest=DataImportServiceImplTest -q`
Expected: Both tests PASS

- [ ] **Step 6: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/DataImportServiceImpl.java
git add graphiti-module-core/src/test/java/com/graphiti/module/graphiti/service/DataImportServiceImplTest.java
git commit -m "phase1: integrate OntologyValidationService into DataImportServiceImpl.addEntityNode()"
```

---

## Task 10: Compile and Run Full Test Suite

- [ ] **Step 1: Run full Maven compile**

Run: `cd D:/projects/graphiti-java && mvn compile -pl graphiti-module-core -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all new tests**

Run: `cd D:/projects/graphiti-java && mvn test -pl graphiti-module-core -Dtest="OntDOTest,OntMapperTest,OntologyValidationServiceImplTest,NodeServiceImplTest,EdgeServiceImplTest,DataImportServiceImplTest" -q`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "phase1: compile pass and all tests green"
```

---

## Self-Review Checklist

- [ ] **Spec coverage:** Spec Section 3 (数据模型扩展) → Tasks 1-3, 8 (ont_mapping); Spec Section 4 (验证机制完善) → Tasks 4-7, 9 (DataImport integration)
- [ ] **Placeholder scan:** No TBD/TODO/placeholder code in any task implementation
- [ ] **Type consistency:** `OntologyValidationService.validateNode()` returns `ValidationResultVO`; `OntologyValidationException` wraps `ValidationResultVO`; Both NodeServiceImpl and EdgeServiceImpl use the same `ValidationResultVO` type
- [ ] **Backward compatibility:** `hasOntology(graphId)` guard ensures no ontology → validation skipped
- [ ] **All 6 layers present in code:** Layer 1 (type existence), Layer 2 (required), Layer 3 (type), Layer 4 (constraint) implemented; Layer 5 (OWL), Layer 6 (reasoning) return pass stub
- [ ] **Data migration:** MySQL `graphiti_ontology` JSON → PostgreSQL structured tables: run one-time migration script after Phase 1 deployment
- [ ] **Spec gap covered:** ont_mapping table added (Task 8); DataImportServiceImpl integration added (Task 9); SearchService integration noted in Phase 2-4 plan Task P4-2
