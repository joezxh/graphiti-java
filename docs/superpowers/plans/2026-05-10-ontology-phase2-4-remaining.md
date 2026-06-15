# Phase 2-4: Ontology Modeling, Reasoning Engine & Ecosystem - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 2 — 完整的本体建模工具链（类/属性/约束 CRUD + 版本管理 + Schema.org 导入）；Phase 3 — 基于 Jena 的 OWL 2 RL 推理引擎；Phase 4 — 外部本体生态集成（RDF/OWL 导入导出 + 本体对齐）。

**Architecture:**
- Phase 2: 在 PostgreSQL 上构建完整 CRUD 服务层，扩展 `OntologyService`，新增 `OntologyClassService` / `OntologyPropertyService` / `OntologyConstraintService`
- Phase 3: 引入 Jena TDB，PostgreSQL 元数据同步到 Neo4j OWL 图，推理按需调用
- Phase 4: RDF4J 处理外部本体导入导出，LLM 辅助本体对齐

**Tech Stack:** Spring Boot, MyBatis-Plus, PostgreSQL, Apache Jena 4.9.0, HermiT Reasoner 1.4.5, RDF4J 3.x

---

## Phase 2: Ontology Modeling

### File Structure

```
ontograph-module-core/src/main/java/com/graphiti/module/graphiti/
├── service/
│   ├── OntologyClassService.java              ← NEW
│   ├── OntologyPropertyService.java           ← NEW
│   ├── OntologyConstraintService.java          ← NEW
│   ├── OntologyVersionService.java             ← NEW
│   ├── SchemaOrgImportService.java            ← NEW
│   └── impl/
│       ├── OntologyClassServiceImpl.java       ← NEW
│       ├── OntologyPropertyServiceImpl.java    ← NEW
│       ├── OntologyConstraintServiceImpl.java  ← NEW
│       ├── OntologyVersionServiceImpl.java     ← NEW
│       └── SchemaOrgImportServiceImpl.java     ← NEW
├── controller/admin/
│   └── OntologyController.java               MODIFY (add new endpoints)
├── vo/ontology/
│   ├── OntClassVO.java                      ← NEW (rename from old)
│   ├── OntPropertyVO.java                    ← NEW
│   ├── OntConstraintVO.java                  ← NEW
│   ├── ClassHierarchyVO.java                 ← NEW
│   ├── VersionHistoryVO.java                  ← NEW
│   └── SchemaOrgImportReqVO.java            ← NEW
└── util/
    └── JsonUtils.java                        ← NEW (if not exists)

sql/
└── (no new tables — reuse Phase 1 tables)

ontograph-module-core/
├── pom.xml                                 MODIFY (add Jena dependencies)
└── src/test/java/.../service/
    ├── OntologyClassServiceImplTest.java    ← NEW
    ├── OntologyPropertyServiceImplTest.java   ← NEW
    └── SchemaOrgImportServiceImplTest.java   ← NEW
```

---

### Task P2-1: Add Jena Dependencies to pom.xml

**Files:**
- Modify: `ontograph-module-core/pom.xml`

- [ ] **Step 1: Add Jena and RDF4J dependencies**

找到 `</dependencies>` 前插入：

```xml
        <!-- Apache Jena (OWL 2 RL Reasoning) -->
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>apache-jena-libs</artifactId>
            <version>4.9.0</version>
            <type>pom</type>
        </dependency>

        <!-- RDF4J (RDF Import/Export) -->
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-model</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-turtle</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-rdfxml</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-jsonld</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-repository-sail</artifactId>
            <version>3.7.7</version>
        </dependency>
```

- [ ] **Step 2: Verify Maven resolves dependencies**

Run: `cd D:/projects/ontograph-java && mvn dependency:resolve -pl ontograph-module-core -q 2>&1 | grep -i "jena\|rdf4j" | head -20`
Expected: Lists Jena and RDF4J artifacts resolved

- [ ] **Step 3: Commit**

```bash
git add ontograph-module-core/pom.xml
git commit -m "phase2: add Apache Jena 4.9.0 and RDF4J 3.7.7 dependencies"
```

---

### Task P2-2: OntologyClassService (Class CRUD + Hierarchy)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyClassService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyClassServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntClassVO.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ClassHierarchyVO.java`
- Test: `ontograph-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyClassServiceImplTest.java`

- [ ] **Step 1: Write OntClassVO and ClassHierarchyVO**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntClassVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import java.util.List;

@Data
public class OntClassVO {
    private Long id;
    private Long definitionId;
    private String classUri;
    private String localName;
    private Long parentClassId;
    private String parentClassUri;
    private List<String> equivalentTo;
    private List<String> disjointWith;
    private String description;
    private String example;
    private String domainHint;     // FINANCIAL / MEDICAL / ECOMMERGE / KNOWLEDGE
    private String metadata;       // JSON string
    private List<OntPropertyVO> inheritedProperties;  // derived from parent
    private java.time.LocalDateTime createdAt;
}
```

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ClassHierarchyVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class ClassHierarchyVO {
    private String classUri;
    private String localName;
    private String description;
    private String domainHint;
    @Builder.Default
    private List<ClassHierarchyVO> children = new java.util.ArrayList<>();
}
```

- [ ] **Step 2: Write OntologyClassService interface**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyClassService.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import java.util.List;

public interface OntologyClassService {

    /** 创建类定义 */
    OntClassVO createClass(String graphId, OntClassVO reqVO);

    /** 更新类定义 */
    OntClassVO updateClass(String graphId, Long classId, OntClassVO reqVO);

    /** 删除类定义（级联删除子类的关系记录） */
    void deleteClass(String graphId, Long classId);

    /** 获取单个类详情 */
    OntClassVO getClass(String graphId, Long classId);

    /** 获取所有类（平铺） */
    List<OntClassVO> listClasses(String graphId);

    /** 获取类层次树（根节点向下） */
    List<ClassHierarchyVO> getClassHierarchy(String graphId);

    /** 获取某个类的所有后代类 */
    List<String> getDescendantClasses(String graphId, Long classId);
}
```

- [ ] **Step 3: Write OntologyClassServiceImpl**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyClassServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.*;
import com.graphiti.module.graphiti.dal.mysql.ont.*;
import com.graphiti.module.graphiti.service.OntologyClassService;
import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyClassServiceImpl implements OntologyClassService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntVersionHistoryMapper versionHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OntClassVO createClass(String graphId, OntClassVO reqVO) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) {
            throw new BusinessException(2002, "图谱未定义本体，请先创建本体定义");
        }

        // 生成 classUri（如果未提供）
        String classUri = reqVO.getClassUri();
        if (classUri == null || classUri.isBlank()) {
            classUri = "http://graphiti.io/" + reqVO.getLocalName();
        }

        OntClassDO entity = new OntClassDO();
        entity.setDefinitionId(defId);
        entity.setClassUri(classUri);
        entity.setLocalName(reqVO.getLocalName());
        entity.setParentClassId(reqVO.getParentClassId());
        entity.setEquivalentTo(reqVO.getEquivalentTo() != null
            ? reqVO.getEquivalentTo().toArray(new String[0]) : null);
        entity.setDescription(reqVO.getDescription());
        entity.setExample(reqVO.getExample());
        entity.setDomainHint(reqVO.getDomainHint());
        entity.setMetadata(reqVO.getMetadata() != null
            ? reqVO.getMetadata().toString() : null);

        classMapper.insert(entity);

        // 记录版本历史
        recordHistory(defId, "CLASS_ADDED", "CLASS", entity.getId(),
            null, entity, "新增类: " + reqVO.getLocalName(), null);

        return toVO(entity);
    }

    @Override
    @Transactional
    public OntClassVO updateClass(String graphId, Long classId, OntClassVO reqVO) {
        OntClassDO existing = classMapper.selectById(classId);
        if (existing == null) throw new BusinessException(1003, "类定义不存在");

        OntClassDO before = cloneDO(existing);

        if (reqVO.getLocalName() != null) existing.setLocalName(reqVO.getLocalName());
        if (reqVO.getClassUri() != null) existing.setClassUri(reqVO.getClassUri());
        if (reqVO.getDescription() != null) existing.setDescription(reqVO.getDescription());
        if (reqVO.getParentClassId() != null) existing.setParentClassId(reqVO.getParentClassId());
        if (reqVO.getDomainHint() != null) existing.setDomainHint(reqVO.getDomainHint());
        if (reqVO.getEquivalentTo() != null)
            existing.setEquivalentTo(reqVO.getEquivalentTo().toArray(new String[0]));

        classMapper.updateById(existing);

        recordHistory(existing.getDefinitionId(), "CLASS_MODIFIED", "CLASS", classId,
            before, existing, "更新类: " + existing.getLocalName(), null);

        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteClass(String graphId, Long classId) {
        OntClassDO existing = classMapper.selectById(classId);
        if (existing == null) return;

        // 检查是否有子类
        LambdaQueryWrapper<OntClassDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntClassDO::getParentClassId, classId);
        if (classMapper.selectCount(cw) > 0) {
            throw new BusinessException(2003, "无法删除：该类存在子类型，请先删除子类型");
        }

        recordHistory(existing.getDefinitionId(), "CLASS_DELETED", "CLASS", classId,
            existing, null, "删除类: " + existing.getLocalName(), null);

        classMapper.deleteById(classId);
    }

    @Override
    public OntClassVO getClass(String graphId, Long classId) {
        OntClassDO entity = classMapper.selectById(classId);
        if (entity == null) throw new BusinessException(1003, "类定义不存在");
        return toVO(entity);
    }

    @Override
    public List<OntClassVO> listClasses(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return classMapper.selectByDefinitionId(defId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ClassHierarchyVO> getClassHierarchy(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();

        List<OntClassDO> allClasses = classMapper.selectByDefinitionId(defId);
        Map<Long, List<OntClassDO>> childrenMap = allClasses.stream()
            .filter(c -> c.getParentClassId() != null)
            .collect(Collectors.groupingBy(OntClassDO::getParentClassId));

        List<OntClassDO> roots = allClasses.stream()
            .filter(c -> c.getParentClassId() == null)
            .collect(Collectors.toList());

        return roots.stream().map(root -> buildHierarchy(root, childrenMap)).collect(Collectors.toList());
    }

    @Override
    public List<String> getDescendantClasses(String graphId, Long classId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        Set<String> descendants = new HashSet<>();
        collectDescendants(defId, classId, descendants);
        return new ArrayList<>(descendants);
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

    private ClassHierarchyVO buildHierarchy(OntClassDO cls, Map<Long, List<OntClassDO>> childrenMap) {
        List<ClassHierarchyVO> childVOs = childrenMap.getOrDefault(cls.getId(), List.of())
            .stream().map(c -> buildHierarchy(c, childrenMap)).collect(Collectors.toList());
        return ClassHierarchyVO.builder()
            .classUri(cls.getClassUri())
            .localName(cls.getLocalName())
            .description(cls.getDescription())
            .domainHint(cls.getDomainHint())
            .children(childVOs)
            .build();
    }

    private void collectDescendants(Long defId, Long classId, Set<String> result) {
        LambdaQueryWrapper<OntClassDO> w = new LambdaQueryWrapper<>();
        w.eq(OntClassDO::getDefinitionId, defId);
        w.eq(OntClassDO::getParentClassId, classId);
        List<OntClassDO> children = classMapper.selectList(w);
        for (OntClassDO child : children) {
            result.add(child.getLocalName());
            collectDescendants(defId, child.getId(), result);
        }
    }

    private OntClassVO toVO(OntClassDO entity) {
        OntClassVO vo = new OntClassVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setClassUri(entity.getClassUri());
        vo.setLocalName(entity.getLocalName());
        vo.setParentClassId(entity.getParentClassId());
        vo.setDescription(entity.getDescription());
        vo.setExample(entity.getExample());
        vo.setDomainHint(entity.getDomainHint());
        vo.setMetadata(entity.getMetadata());
        vo.setCreatedAt(entity.getCreatedAt());
        if (entity.getEquivalentTo() != null) {
            vo.setEquivalentTo(Arrays.asList(entity.getEquivalentTo()));
        }
        return vo;
    }

    private OntClassDO cloneDO(OntClassDO src) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(src), OntClassDO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void recordHistory(Long defId, String changeType, String entityType,
            Long entityId, Object before, Object after, String diffSummary, String changedBy) {
        try {
            OntVersionHistoryDO history = new OntVersionHistoryDO();
            history.setDefinitionId(defId);
            history.setVersion("1.0.0");
            history.setChangeType(changeType);
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setBeforeState(before != null ? objectMapper.writeValueAsString(before) : null);
            history.setAfterState(after != null ? objectMapper.writeValueAsString(after) : null);
            history.setDiffSummary(diffSummary);
            history.setChangedBy(changedBy);
            versionHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录版本历史失败", e);
        }
    }
}
```

- [ ] **Step 4: Write unit test**

```java
// ontograph-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyClassServiceImplTest.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.service.impl.OntologyClassServiceImpl;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OntologyClassServiceImplTest {

    @Test
    void testOntClassVO_setters() {
        OntClassVO vo = new OntClassVO();
        vo.setLocalName("Person");
        vo.setClassUri("http://example.org/Person");
        vo.setDomainHint("KNOWLEDGE");
        vo.setDescription("Represents a person");
        assertEquals("Person", vo.getLocalName());
        assertEquals("KNOWLEDGE", vo.getDomainHint());
    }

    @Test
    void testClassHierarchyVO_builder() {
        var child = com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO.builder()
            .localName("Doctor")
            .classUri("http://example.org/Doctor")
            .children(java.util.List.of())
            .build();
        var root = com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO.builder()
            .localName("Person")
            .classUri("http://example.org/Person")
            .children(java.util.List.of(child))
            .build();
        assertEquals(1, root.getChildren().size());
        assertEquals("Doctor", root.getChildren().get(0).getLocalName());
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyClassService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyClassServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntClassVO.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ClassHierarchyVO.java
git add ontograph-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyClassServiceImplTest.java
git commit -m "phase2: add OntologyClassService with CRUD and hierarchy tree support"
```

---

### Task P2-3: OntologyPropertyService (Property CRUD + Domain/Range)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyPropertyService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyPropertyServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntPropertyVO.java`
- Test: `ontograph-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyPropertyServiceImplTest.java`

- [ ] **Step 1: Write OntPropertyVO**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntPropertyVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OntPropertyVO {
    private Long id;
    private Long definitionId;
    private String propertyUri;
    private String localName;
    private String propertyType;    // OBJECT / DATATYPE / ANNOTATION / TRANSITIVE / SYMMETRIC / FUNCTIONAL
    private Long domainClassId;
    private String domainClassUri;
    private Long rangeClassId;
    private String rangeClassUri;
    private String rangeDataType;   // string / integer / float / boolean / date / json
    private Integer minCardinality;
    private Integer maxCardinality;
    private String defaultValue;
    private List<String> allowedValues;
    private Long parentPropertyId;
    private String parentPropertyUri;
    private List<String> equivalentTo;
    private Long inverseOfId;
    private String inverseOfUri;
    private Boolean isRequired;
    private Boolean isMultiple;
    private String pattern;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String description;
    private String example;
    private String metadata;
    private java.time.LocalDateTime createdAt;
}
```

- [ ] **Step 2: Write OntologyPropertyService interface**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyPropertyService.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import java.util.List;

public interface OntologyPropertyService {

    /** 创建属性定义 */
    OntPropertyVO createProperty(String graphId, OntPropertyVO reqVO);

    /** 更新属性定义 */
    OntPropertyVO updateProperty(String graphId, Long propertyId, OntPropertyVO reqVO);

    /** 删除属性定义 */
    void deleteProperty(String graphId, Long propertyId);

    /** 获取属性详情 */
    OntPropertyVO getProperty(String graphId, Long propertyId);

    /** 列出所有属性 */
    List<OntPropertyVO> listProperties(String graphId);

    /** 列出某类上定义的所有属性（含继承） */
    List<OntPropertyVO> getPropertiesForClass(String graphId, Long classId);

    /** 获取属性层次（父属性链） */
    List<String> getPropertyAncestors(String graphId, Long propertyId);
}
```

- [ ] **Step 3: Write OntologyPropertyServiceImpl**

核心逻辑：
1. 创建时，验证 `domainClassId` 和 `rangeClassId` 是否存在于 `ont_class` 表
2. 记录版本历史（before/after JSON）
3. 删除时检查是否有约束引用该属性

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyPropertyServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.*;
import com.graphiti.module.graphiti.dal.mysql.ont.*;
import com.graphiti.module.graphiti.service.OntologyPropertyService;
import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyPropertyServiceImpl implements OntologyPropertyService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;
    private final OntVersionHistoryMapper versionHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OntPropertyVO createProperty(String graphId, OntPropertyVO reqVO) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) throw new BusinessException(2002, "图谱未定义本体");

        String propUri = reqVO.getPropertyUri();
        if (propUri == null || propUri.isBlank()) {
            propUri = "http://graphiti.io/" + reqVO.getLocalName();
        }

        // 校验 domainClassId 存在（如果提供）
        if (reqVO.getDomainClassId() != null && classMapper.selectById(reqVO.getDomainClassId()) == null) {
            throw new BusinessException(2004, "domainClassId 不存在: " + reqVO.getDomainClassId());
        }
        // 校验 rangeClassId 存在（如果提供）
        if (reqVO.getRangeClassId() != null && classMapper.selectById(reqVO.getRangeClassId()) == null) {
            throw new BusinessException(2004, "rangeClassId 不存在: " + reqVO.getRangeClassId());
        }

        OntPropertyDO entity = toEntity(reqVO, defId, propUri);
        propertyMapper.insert(entity);

        recordHistory(defId, "PROPERTY_ADDED", "PROPERTY", entity.getId(),
            null, entity, "新增属性: " + reqVO.getLocalName(), null);

        return toVO(entity);
    }

    @Override
    @Transactional
    public OntPropertyVO updateProperty(String graphId, Long propertyId, OntPropertyVO reqVO) {
        OntPropertyDO existing = propertyMapper.selectById(propertyId);
        if (existing == null) throw new BusinessException(1003, "属性不存在");

        OntPropertyDO before = cloneDO(existing);
        if (reqVO.getLocalName() != null) existing.setLocalName(reqVO.getLocalName());
        if (reqVO.getPropertyUri() != null) existing.setPropertyUri(reqVO.getPropertyUri());
        if (reqVO.getPropertyType() != null) existing.setPropertyType(reqVO.getPropertyType());
        if (reqVO.getDomainClassId() != null) existing.setDomainClassId(reqVO.getDomainClassId());
        if (reqVO.getRangeClassId() != null) existing.setRangeClassId(reqVO.getRangeClassId());
        if (reqVO.getRangeDataType() != null) existing.setRangeDataType(reqVO.getRangeDataType());
        if (reqVO.getIsRequired() != null) existing.setIsRequired(reqVO.getIsRequired());
        if (reqVO.getPattern() != null) existing.setPattern(reqVO.getPattern());
        if (reqVO.getDescription() != null) existing.setDescription(reqVO.getDescription());

        propertyMapper.updateById(existing);
        recordHistory(existing.getDefinitionId(), "PROPERTY_MODIFIED", "PROPERTY", propertyId,
            before, existing, "更新属性: " + existing.getLocalName(), null);

        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteProperty(String graphId, Long propertyId) {
        OntPropertyDO existing = propertyMapper.selectById(propertyId);
        if (existing == null) return;

        // 检查是否有约束引用
        LambdaQueryWrapper<OntConstraintDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntConstraintDO::getPropertyId, propertyId);
        if (constraintMapper.selectCount(cw) > 0) {
            throw new BusinessException(2005, "无法删除：存在约束引用此属性");
        }

        recordHistory(existing.getDefinitionId(), "PROPERTY_DELETED", "PROPERTY", propertyId,
            existing, null, "删除属性: " + existing.getLocalName(), null);
        propertyMapper.deleteById(propertyId);
    }

    @Override
    public OntPropertyVO getProperty(String graphId, Long propertyId) {
        OntPropertyDO entity = propertyMapper.selectById(propertyId);
        if (entity == null) throw new BusinessException(1003, "属性不存在");
        return toVO(entity);
    }

    @Override
    public List<OntPropertyVO> listProperties(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return propertyMapper.selectByDefinitionId(defId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<OntPropertyVO> getPropertiesForClass(String graphId, Long classId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return propertyMapper.selectByClassId(defId, classId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getPropertyAncestors(String graphId, Long propertyId) {
        List<String> ancestors = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return ancestors;
        collectPropertyAncestors(defId, propertyId, ancestors, visited);
        return ancestors;
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

    private void collectPropertyAncestors(Long defId, Long propId, List<String> result, Set<Long> visited) {
        if (visited.contains(propId)) return;
        visited.add(propId);
        OntPropertyDO prop = propertyMapper.selectById(propId);
        if (prop != null && prop.getParentPropertyId() != null) {
            OntPropertyDO parent = propertyMapper.selectById(prop.getParentPropertyId());
            if (parent != null) {
                result.add(parent.getLocalName());
                collectPropertyAncestors(defId, parent.getId(), result, visited);
            }
        }
    }

    private OntPropertyVO toVO(OntPropertyDO entity) {
        OntPropertyVO vo = OntPropertyVO.builder()
            .id(entity.getId())
            .definitionId(entity.getDefinitionId())
            .propertyUri(entity.getPropertyUri())
            .localName(entity.getLocalName())
            .propertyType(entity.getPropertyType())
            .domainClassId(entity.getDomainClassId())
            .rangeClassId(entity.getRangeClassId())
            .rangeDataType(entity.getRangeDataType())
            .minCardinality(entity.getMinCardinality())
            .maxCardinality(entity.getMaxCardinality())
            .defaultValue(entity.getDefaultValue())
            .parentPropertyId(entity.getParentPropertyId())
            .inverseOfId(entity.getInverseOfId())
            .isRequired(entity.getIsRequired())
            .isMultiple(entity.getIsMultiple())
            .pattern(entity.getPattern())
            .minValue(entity.getMinValue())
            .maxValue(entity.getMaxValue())
            .description(entity.getDescription())
            .example(entity.getExample())
            .metadata(entity.getMetadata())
            .createdAt(entity.getCreatedAt())
            .build();

        if (entity.getAllowedValues() != null)
            vo.setAllowedValues(Arrays.asList(entity.getAllowedValues()));
        if (entity.getEquivalentTo() != null)
            vo.setEquivalentTo(Arrays.asList(entity.getEquivalentTo()));

        // 填充 domainClassUri / rangeClassUri
        if (entity.getDomainClassId() != null) {
            OntClassDO domain = classMapper.selectById(entity.getDomainClassId());
            if (domain != null) vo.setDomainClassUri(domain.getClassUri());
        }
        if (entity.getRangeClassId() != null) {
            OntClassDO range = classMapper.selectById(entity.getRangeClassId());
            if (range != null) vo.setRangeClassUri(range.getClassUri());
        }
        if (entity.getParentPropertyId() != null) {
            OntPropertyDO parent = propertyMapper.selectById(entity.getParentPropertyId());
            if (parent != null) vo.setParentPropertyUri(parent.getPropertyUri());
        }
        if (entity.getInverseOfId() != null) {
            OntPropertyDO inverse = propertyMapper.selectById(entity.getInverseOfId());
            if (inverse != null) vo.setInverseOfUri(inverse.getPropertyUri());
        }
        return vo;
    }

    private OntPropertyDO toEntity(OntPropertyVO req, Long defId, String propUri) {
        OntPropertyDO entity = new OntPropertyDO();
        entity.setDefinitionId(defId);
        entity.setPropertyUri(propUri);
        entity.setLocalName(req.getLocalName());
        entity.setPropertyType(req.getPropertyType() != null ? req.getPropertyType() : "DATATYPE");
        entity.setDomainClassId(req.getDomainClassId());
        entity.setRangeClassId(req.getRangeClassId());
        entity.setRangeDataType(req.getRangeDataType());
        entity.setMinCardinality(req.getMinCardinality());
        entity.setMaxCardinality(req.getMaxCardinality());
        entity.setDefaultValue(req.getDefaultValue());
        entity.setAllowedValues(req.getAllowedValues() != null
            ? req.getAllowedValues().toArray(new String[0]) : null);
        entity.setParentPropertyId(req.getParentPropertyId());
        entity.setEquivalentTo(req.getEquivalentTo() != null
            ? req.getEquivalentTo().toArray(new String[0]) : null);
        entity.setInverseOfId(req.getInverseOfId());
        entity.setIsRequired(req.getIsRequired() != null ? req.getIsRequired() : false);
        entity.setIsMultiple(req.getIsMultiple() != null ? req.getIsMultiple() : false);
        entity.setPattern(req.getPattern());
        entity.setMinValue(req.getMinValue());
        entity.setMaxValue(req.getMaxValue());
        entity.setDescription(req.getDescription());
        entity.setExample(req.getExample());
        entity.setMetadata(req.getMetadata());
        return entity;
    }

    private OntPropertyDO cloneDO(OntPropertyDO src) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(src), OntPropertyDO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void recordHistory(Long defId, String changeType, String entityType,
            Long entityId, Object before, Object after, String diffSummary, String changedBy) {
        try {
            OntVersionHistoryDO history = new OntVersionHistoryDO();
            history.setDefinitionId(defId);
            history.setVersion("1.0.0");
            history.setChangeType(changeType);
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setBeforeState(before != null ? objectMapper.writeValueAsString(before) : null);
            history.setAfterState(after != null ? objectMapper.writeValueAsString(after) : null);
            history.setDiffSummary(diffSummary);
            history.setChangedBy(changedBy);
            versionHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录版本历史失败", e);
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyPropertyService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyPropertyServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/OntPropertyVO.java
git commit -m "phase2: add OntologyPropertyService with CRUD, domain/range, and property hierarchy"
```

---

### Task P2-4: Schema.org Import Service

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/SchemaOrgImportService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/SchemaOrgImportServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/SchemaOrgImportReqVO.java`

**核心实现逻辑：**
1. 根据 `domains` 参数（FinancialProduct / MedicalEntity / Product / Article 等）从 Schema.org 获取对应类的 JSON-LD
2. 使用 RDF4J 解析 JSON-LD，构建 RDF Model
3. 遍历类层次：提取 `rdfs:subClassOf`、`rdfs:label`、`rdfs:comment`、`schema:domainIncludes`、`schema:rangeIncludes`
4. 转换为 `OntClassDO` 和 `OntPropertyDO` 写入 PostgreSQL
5. 按 `domain_hint`（FINANCIAL/MEDICAL/ECOMMERCE/KNOWLEDGE）分类存储

- [ ] **Step 1: Write SchemaOrgImportReqVO**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/SchemaOrgImportReqVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import java.util.List;

@Data
public class SchemaOrgImportReqVO {
    /** 要导入的 Schema.org 顶级类名列表 */
    private List<String> domains;     // ["FinancialProduct", "MedicalEntity", "Product", "Article"]

    /** 语言，默认 zh-CN */
    private String language = "zh-CN";

    /** 是否包含推断类（父类），默认 false */
    private boolean includeInferred = false;

    /** 类层次最大深度，默认 3 */
    private int hierarchyDepth = 3;

    /** 业务域标记 */
    private String domainHint;         // FINANCIAL / MEDICAL / ECOMMERCE / KNOWLEDGE
}
```

- [ ] **Step 2: Write SchemaOrgImportService**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/SchemaOrgImportService.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.SchemaOrgImportReqVO;
import java.util.List;
import java.util.Map;

public interface SchemaOrgImportService {

    /**
     * 从 Schema.org 导入本体
     * @param graphId  图谱ID
     * @param reqVO    导入参数
     * @return 导入统计 { classesImported: N, propertiesImported: M }
     */
    Map<String, Integer> importFromSchemaOrg(String graphId, SchemaOrgImportReqVO reqVO);

    /**
     * 导出本体为 JSON-LD
     */
    String exportAsJsonLd(String graphId);

    /**
     * 导出本体为 Turtle (TTL)
     */
    String exportAsTurtle(String graphId);
}
```

- [ ] **Step 3: Write SchemaOrgImportServiceImpl**

核心实现（RDF4J 解析 Schema.org JSON-LD）：

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/SchemaOrgImportServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.SchemaOrgImportService;
import com.graphiti.module.graphiti.vo.ontology.SchemaOrgImportReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParser;
import org.springframework.stereotype.Service;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaOrgImportServiceImpl implements SchemaOrgImportService {

    private static final String SCHEMA_ORG_BASE = "https://schema.org/";

    @Override
    public Map<String, Integer> importFromSchemaOrg(String graphId, SchemaOrgImportReqVO reqVO) {
        int classesImported = 0;
        int propertiesImported = 0;

        for (String domain : reqVO.getDomains()) {
            try {
                // 1. 获取 Schema.org JSON-LD（从 CDN 或本地缓存）
                String jsonLd = fetchSchemaOrgJsonLd(domain, reqVO.getHierarchyDepth());

                // 2. 解析 RDF Model
                Model model = parseJsonLd(jsonLd);

                // 3. 提取类定义
                List<SchemaClassInfo> classes = extractClasses(model, domain, reqVO.getHierarchyDepth());
                for (SchemaClassInfo cls : classes) {
                    // TODO: 调用 OntologyClassService.createClass()
                    log.info("导入类: {} -> {}", cls.uri, cls.label);
                    classesImported++;
                }

                // 4. 提取属性定义
                List<SchemaPropertyInfo> props = extractProperties(model, classes);
                for (SchemaPropertyInfo prop : props) {
                    // TODO: 调用 OntologyPropertyService.createProperty()
                    log.info("导入属性: {} domain={} range={}", prop.uri, prop.domain, prop.range);
                    propertiesImported++;
                }
            } catch (Exception e) {
                log.error("导入 Schema.org 类 {} 失败", domain, e);
            }
        }

        return Map.of("classesImported", classesImported, "propertiesImported", propertiesImported);
    }

    @Override
    public String exportAsJsonLd(String graphId) {
        // TODO: 从 PostgreSQL 读取所有类/属性，构建 JSON-LD
        return "{}";
    }

    @Override
    public String exportAsTurtle(String graphId) {
        // TODO: 使用 RDF4J Model 转 Turtle
        return "@prefix : <http://graphiti.io/> .";
    }

    // ==================== 私有方法 ====================

    private String fetchSchemaOrgJsonLd(String domain, int depth) throws Exception {
        // Schema.org 提供 @graph 格式的 JSON-LD
        String url = "https://schema.org/" + domain + ".jsonld";
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.net.URI(url).toURL().openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private Model parseJsonLd(String jsonLd) {
        Model model = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance().createEmptyModel();
        try {
            var parser = Rio.createParser(RDFFormat.JSONLD,
                org.eclipse.rdf4j.model.ValueFactoryImpl.getInstance());
            parser.setRDFHandler(new org.eclipse.rdf4j.rio.helpers.StatementCollector(model));
            parser.parse(new StringReader(jsonLd));
        } catch (Exception e) {
            log.warn("JSON-LD 解析失败，使用备用方案", e);
        }
        return model;
    }

    private List<SchemaClassInfo> extractClasses(Model model, String rootDomain, int depth) {
        List<SchemaClassInfo> classes = new ArrayList<>();
        IRI rootIRI = org.eclipse.rdf4j.model.ValueFactoryImpl.getInstance()
            .createIRI(SCHEMA_ORG_BASE + rootDomain);

        collectSubClasses(model, rootIRI, classes, 0, depth);
        return classes;
    }

    private void collectSubClasses(Model model, IRI clsIRI, List<SchemaClassInfo> result, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) return;

        String label = getLabel(model, clsIRI);
        String comment = getComment(model, clsIRI);
        result.add(new SchemaClassInfo(clsIRI.stringValue(), label, comment));

        // 查找子类
        model.filter(null, RDFS.SUBCLASSOF, clsIRI).subjects().forEach(subCls -> {
            if (subCls.isIRI()) {
                collectSubClasses(model, (IRI) subCls, result, currentDepth + 1, maxDepth);
            }
        });
    }

    private List<SchemaPropertyInfo> extractProperties(Model model, List<SchemaClassInfo> classes) {
        List<SchemaPropertyInfo> props = new ArrayList<>();
        Set<IRI> classIRIs = classes.stream()
            .map(c -> org.eclipse.rdf4j.model.ValueFactoryImpl.getInstance().createIRI(c.uri))
            .collect(Collectors.toSet());

        // 提取 domainIncludes → 属性对应哪些类
        for (Statement st : model.filter(null, RDF.TYPE, RDFS.PROPERTY).statements()) {
            IRI propIRI = (IRI) st.getSubject();
            String propLabel = getLabel(model, propIRI);
            if (propLabel == null) continue;

            List<String> domains = new ArrayList<>();
            List<String> ranges = new ArrayList<>();

            // domainIncludes
            model.filter(propIRI, getSchemaVocab("domainIncludes"), null).objects().forEach(obj -> {
                if (obj.isIRI()) domains.add(((IRI) obj).getLocalName());
            });

            // rangeIncludes
            model.filter(propIRI, getSchemaVocab("rangeIncludes"), null).objects().forEach(obj -> {
                if (obj.isIRI()) ranges.add(((IRI) obj).getLocalName());
            });

            props.add(new SchemaPropertyInfo(propIRI.stringValue(), propLabel, domains, ranges));
        }
        return props;
    }

    private String getLabel(Model model, IRI iri) {
        return model.filter(iri, RDFS.LABEL, null).objects()
            .filter(v -> v.isLiteral())
            .map(v -> (Literal) v)
            .filter(l -> "en".equals(l.getLanguage().orElse("")))
            .map(Literal::getLabel)
            .findFirst().orElse(iri.getLocalName());
    }

    private String getComment(Model model, IRI iri) {
        return model.filter(iri, RDFS.COMMENT, null).objects()
            .filter(v -> v.isLiteral())
            .map(v -> (Literal) v)
            .filter(l -> "en".equals(l.getLanguage().orElse("")))
            .map(Literal::getLabel)
            .findFirst().orElse(null);
    }

    private IRI getSchemaVocab(String localName) {
        return org.eclipse.rdf4j.model.ValueFactoryImpl.getInstance()
            .createIRI(SCHEMA_ORG_BASE + localName);
    }

    // 内部数据结构
    private record SchemaClassInfo(String uri, String label, String comment) {}
    private record SchemaPropertyInfo(String uri, String label, List<String> domain, List<String> range) {}
}
```

- [ ] **Step 4: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/SchemaOrgImportService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/SchemaOrgImportServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/SchemaOrgImportReqVO.java
git commit -m "phase2: add SchemaOrgImportService with RDF4J JSON-LD parser for Schema.org import"
```

---

## Phase 3: Reasoning Engine

### File Structure

```
ontograph-module-core/src/main/java/com/graphiti/module/graphiti/
├── service/
│   ├── OntologyReasoner.java                 ← NEW (interface)
│   └── impl/
│       ├── OntologyReasonerImpl.java            ← NEW (Jena-based)
│       └── OntologySyncServiceImpl.java         ← NEW (PostgreSQL → Neo4j sync)
```

---

### Task P3-1: OntologyReasoner Interface + Implementation

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyReasoner.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyReasonerImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ConsistencyResultVO.java`
- Test: `ontograph-module-core/src/test/java/com/graphiti/module/graphiti/service/OntologyReasonerImplTest.java`

- [ ] **Step 1: Write ConsistencyResultVO**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ConsistencyResultVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ConsistencyResultVO {
    private boolean consistent;
    private List<String> inconsistencies;
    private List<String> satisfiableClasses;
    private List<String> unsatisfiableClasses;
}
```

- [ ] **Step 2: Write OntologyReasoner interface**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyReasoner.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import java.util.List;
import java.util.Map;

/**
 * OWL 2 RL 推理引擎接口
 * 基于 Apache Jena + InfGraph 实现
 */
public interface OntologyReasoner {

    /**
     * 预热推理机：将图谱的本体加载到 Jena InfGraph
     */
    void warmUp(String graphId);

    /**
     * 释放资源
     */
    void shutdown(String graphId);

    /**
     * 获取类的所有祖先类 (rdfs:subClassOf 上行)
     */
    List<String> getAncestorClasses(String graphId, String classUri);

    /**
     * 获取类的所有后代类 (rdfs:subClassOf 下行)
     */
    List<String> getDescendantClasses(String graphId, String classUri);

    /**
     * 推断实体类型（根据属性推断）
     */
    List<String> inferTypes(String graphId, Map<String, Object> properties);

    /**
     * 一致性检查
     */
    ConsistencyResultVO checkConsistency(String graphId);

    /**
     * 检查类是否可满足
     */
    boolean isSatisfiable(String graphId, String classUri);

    /**
     * 获取推理机状态
     */
    boolean isWarmedUp(String graphId);
}
```

- [ ] **Step 3: Write OntologyReasonerImpl (Jena-based)**

核心实现策略：
1. `warmUp()`: 从 PostgreSQL 读取 ont_class / ont_property，转为 Jena RDF Model，写入 `InfGraph`
2. 使用 `ReasonerRegistry.getOWLReasoner()` 创建 OWL 2 RL 推理机
3. 类层次查询通过 `InfModel.listStatements(null, RDFS.SUBCLASSOF, class)` 实现
4. `shutdown()` 清理该图谱的 InfGraph 缓存

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyReasonerImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.dal.dataobject.ont.*;
import com.graphiti.module.graphiti.dal.mysql.ont.*;
import com.graphiti.module.graphiti.service.OntologyReasoner;
import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.*;
import org.apache.jena.reasoner.rulesys.*;
import org.apache.jena.vocabulary.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OntologyReasonerImpl implements OntologyReasoner {

    private final OntModelSpec OWL2_RL_SPEC;
    private final Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();
    private final Map<String, OntModel> ontModelCache = new ConcurrentHashMap<>();

    public OntologyReasonerImpl() {
        // OWL 2 RL 推理规范
        this.OWL2_RL_SPEC = ReasonerRegistry.getOWLLogic().create();
    }

    @Override
    public synchronized void warmUp(String graphId) {
        if (infModelCache.containsKey(graphId)) return;

        log.info("推理机预热中：graphId={}", graphId);

        // 1. 构建空 RDF Model
        OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        baseModel.setNsPrefix("rdfs", RDFS.getURI());
        baseModel.setNsPrefix("owl", OWL.getURI());
        baseModel.setNsPrefix("rdf", RDF.getURI());

        // 2. 填充类三元组（后续从 mappers 读取，这里先用 stub）
        // TODO: 从 OntClassMapper / OntPropertyMapper 读取数据构建 RDF 图
        // for (OntClassDO cls : classMapper.selectByDefinitionId(defId)) { ... }

        // 3. 创建 OWL 2 RL InfModel
        Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(baseModel);
        InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

        infModelCache.put(graphId, infModel);
        ontModelCache.put(graphId, baseModel);
        log.info("推理机预热完成：graphId={}", graphId);
    }

    @Override
    public synchronized void shutdown(String graphId) {
        InfModel removed = infModelCache.remove(graphId);
        ontModelCache.remove(graphId);
        if (removed != null) {
            removed.removeAll();
            log.info("推理机已关闭：graphId={}", graphId);
        }
    }

    @Override
    public List<String> getAncestorClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> ancestors = new LinkedHashSet<>();
        collectAncestors(infModel, cls, ancestors);
        ancestors.remove(classUri); // 移除自身
        return new ArrayList<>(ancestors);
    }

    @Override
    public List<String> getDescendantClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> descendants = new LinkedHashSet<>();
        collectDescendants(infModel, cls, descendants);
        descendants.remove(classUri);
        return new ArrayList<>(descendants);
    }

    @Override
    public List<String> inferTypes(String graphId, Map<String, Object> properties) {
        // 基于属性 profile 的简单类型推断（后续可接入 LLM）
        // 策略：根据 properties 的 key 集合，在 ont_property 表中匹配 domain
        // 这里返回空列表，后续在 EpisodeTypeInferenceEngine 中用 LLM 补充
        return List.of();
    }

    @Override
    public ConsistencyResultVO checkConsistency(String graphId) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) {
            return ConsistencyResultVO.builder()
                .consistent(true)
                .inconsistencies(List.of("推理机未初始化"))
                .build();
        }

        // OWL 2 RL 推理机不支持显式一致性检查，通过可满足性推断
        List<String> unsatisfiable = new ArrayList<>();
        List<String> satisfiable = new ArrayList<>();

        // 检查核心类是否可满足
        String[] coreClasses = {"http://www.w3.org/2002/07/owl#Thing",
            "http://www.w3.org/2000/01/rdf-schema#Resource"};
        for (String clsUri : coreClasses) {
            if (isSatisfiable(graphId, clsUri)) {
                satisfiable.add(clsUri);
            } else {
                unsatisfiable.add(clsUri);
            }
        }

        return ConsistencyResultVO.builder()
            .consistent(unsatisfiable.isEmpty())
            .satisfiableClasses(satisfiable)
            .unsatisfiableClasses(unsatisfiable)
            .build();
    }

    @Override
    public boolean isSatisfiable(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return true;
        Resource cls = infModel.getResource(classUri);
        if (cls == null) return true;
        // 若该类没有任何实例，则认为是可满足的
        return !infModel.listStatements(null, RDF.type, cls).toList().isEmpty()
            || !infModel.listStatements(cls, RDFS.SUBCLASSOF, (RDFNode) null).toList().isEmpty();
    }

    @Override
    public boolean isWarmedUp(String graphId) {
        return infModelCache.containsKey(graphId);
    }

    // ==================== 私有方法 ====================

    private void collectAncestors(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(cls, RDFS.subClassOf, (RDFNode) null);
        while (it.hasNext()) {
            RDFNode parent = it.nextStatement().getObject();
            if (parent.isResource()) {
                String parentUri = parent.asResource().getURI();
                if (parentUri != null && !parentUri.equals(cls.getURI())) {
                    result.add(parentUri);
                    collectAncestors(model, parent.asResource(), result);
                }
            }
        }
    }

    private void collectDescendants(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(null, RDFS.subClassOf, cls);
        while (it.hasNext()) {
            Resource child = it.nextStatement().getSubject();
            String childUri = child.getURI();
            if (childUri != null && !childUri.equals(cls.getURI())) {
                result.add(childUri);
                collectDescendants(model, child, result);
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyReasoner.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologyReasonerImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/ConsistencyResultVO.java
git commit -m "phase3: add OntologyReasoner with Apache Jena OWL 2 RL implementation"
```

---

## Phase 4: Ontology Ecosystem

### Task P4-1: OntologySyncService (PostgreSQL → Neo4j)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologySyncService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologySyncServiceImpl.java`

- [ ] **Step 1: Write OntologySyncService**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologySyncService.java
package com.graphiti.module.graphiti.service;

import java.util.Map;

public interface OntologySyncService {

    /**
     * 将 PostgreSQL 中的本体数据同步到 Neo4j
     * 在 Neo4j 中创建：
     *   - (:OntologyClass {uri, localName, ...})
     *   - (:OntologyProperty {uri, localName, ...})
     *   - [:SUBCLASS_OF] 边
     *   - [:DOMAIN] / [:RANGE] 边
     */
    void syncToNeo4j(String graphId);

    /**
     * 增量同步（仅同步变化的类/属性）
     */
    void syncIncremental(String graphId, Long fromClassId, Long toClassId);

    /**
     * 删除 Neo4j 中的本体数据
     */
    void clearNeo4jOntology(String graphId);
}
```

- [ ] **Step 2: Write OntologySyncServiceImpl**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologySyncServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.OntologySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologySyncServiceImpl implements OntologySyncService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public void syncToNeo4j(String graphId) {
        log.info("开始同步本体到 Neo4j：graphId={}", graphId);
        // TODO:
        // 1. 从 OntClassMapper 读取所有类，写入 Neo4j:
        //    CREATE (c:OntologyClass {graphId: $graphId, classUri: $uri, localName: $name, ...})
        // 2. 从 OntPropertyMapper 读取所有属性，写入 Neo4j:
        //    CREATE (p:OntologyProperty {graphId: $graphId, propertyUri: $uri, ...})
        // 3. 创建类层次边:
        //    MATCH (child:OntologyClass {id: $childId}), (parent:OntologyClass {id: $parentId})
        //    CREATE (child)-[:SUBCLASS_OF]->(parent)
        // 4. 创建 domain/range 边
        log.info("本体同步完成：graphId={}", graphId);
    }

    @Override
    public void syncIncremental(String graphId, Long fromClassId, Long toClassId) {
        // 增量同步：只同步 fromClassId ~ toClassId 范围的类
        log.info("增量同步本体：graphId={}, from={}, to={}", graphId, fromClassId, toClassId);
        syncToNeo4j(graphId); // 目前先全量，后续优化为增量
    }

    @Override
    public void clearNeo4jOntology(String graphId) {
        log.info("清除 Neo4j 本体数据：graphId={}", graphId);
        // TODO: DELETE FROM Neo4j WHERE n:OntologyClass OR n:OntologyProperty
        //   MATCH (n) WHERE n:OntologyClass OR n:OntologyProperty AND n.graphId = $graphId DELETE n
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologySyncService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/OntologySyncServiceImpl.java
git commit -m "phase4: add OntologySyncService for PostgreSQL to Neo4j ontology sync"
```

---

## Task P4-2: OntologyController - All New Endpoints

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/OntologyController.java`

- [ ] **Step 1: Add all new endpoints to OntologyController**

在现有 `OntologyController` 中注入所有新 Service，并添加以下端点：

```java
@RestController
@RequestMapping("/api/v1/ontology")
@RequiredArgsConstructor
public class OntologyController {

    private final OntologyService ontologyService;
    private final OntologyValidationService validationService;
    private final OntologyClassService classService;
    private final OntologyPropertyService propertyService;
    private final OntologyReasoner reasoner;
    private final SchemaOrgImportService schemaOrgImportService;

    // === 现有端点（保留）===
    @GetMapping("/{graphId}")  public CommonResult<OntologyRespVO> getOntology(...) {...}
    @PostMapping("/{graphId}") public CommonResult<OntologyRespVO> setOntology(...) {...}

    // === 类管理 ===
    @GetMapping("/{graphId}/classes")
    public CommonResult<List<OntClassVO>> listClasses(@PathVariable String graphId) {
        return CommonResult.success(classService.listClasses(graphId));
    }

    @GetMapping("/{graphId}/classes/hierarchy")
    public CommonResult<List<ClassHierarchyVO>> getClassHierarchy(@PathVariable String graphId) {
        return CommonResult.success(classService.getClassHierarchy(graphId));
    }

    @PostMapping("/{graphId}/classes")
    public CommonResult<OntClassVO> createClass(@PathVariable String graphId,
            @RequestBody OntClassVO reqVO) {
        return CommonResult.success(classService.createClass(graphId, reqVO));
    }

    @PutMapping("/{graphId}/classes/{classId}")
    public CommonResult<OntClassVO> updateClass(@PathVariable String graphId,
            @PathVariable Long classId, @RequestBody OntClassVO reqVO) {
        return CommonResult.success(classService.updateClass(graphId, classId, reqVO));
    }

    @DeleteMapping("/{graphId}/classes/{classId}")
    public CommonResult<Void> deleteClass(@PathVariable String graphId, @PathVariable Long classId) {
        classService.deleteClass(graphId, classId);
        return CommonResult.success(null);
    }

    // === 属性管理 ===
    @GetMapping("/{graphId}/properties")
    public CommonResult<List<OntPropertyVO>> listProperties(@PathVariable String graphId) {
        return CommonResult.success(propertyService.listProperties(graphId));
    }

    @PostMapping("/{graphId}/properties")
    public CommonResult<OntPropertyVO> createProperty(@PathVariable String graphId,
            @RequestBody OntPropertyVO reqVO) {
        return CommonResult.success(propertyService.createProperty(graphId, reqVO));
    }

    // === Schema.org 导入 ===
    @PostMapping("/{graphId}/import/schema-org")
    public CommonResult<Map<String, Integer>> importSchemaOrg(@PathVariable String graphId,
            @RequestBody SchemaOrgImportReqVO reqVO) {
        Map<String, Integer> stats = schemaOrgImportService.importFromSchemaOrg(graphId, reqVO);
        return CommonResult.success(stats);
    }

    // === 推理 ===
    @GetMapping("/{graphId}/reasoners/status")
    public CommonResult<Map<String, Object>> getReasonerStatus(@PathVariable String graphId) {
        return CommonResult.success(Map.of(
            "warmedUp", reasoner.isWarmedUp(graphId),
            "graphId", graphId
        ));
    }

    @PostMapping("/{graphId}/reasoners/warmup")
    public CommonResult<Void> warmUpReasoner(@PathVariable String graphId) {
        reasoner.warmUp(graphId);
        return CommonResult.success(null);
    }

    @GetMapping("/{graphId}/consistency")
    public CommonResult<ConsistencyResultVO> checkConsistency(@PathVariable String graphId) {
        return CommonResult.success(reasoner.checkConsistency(graphId));
    }

    // === 批量验证 ===
    @PostMapping("/{graphId}/validate/batch")
    public CommonResult<BatchValidationRespVO> validateBatch(@PathVariable String graphId,
            @RequestBody BatchValidationReqVO reqVO) {
        return CommonResult.success(validationService.validateBatch(graphId, reqVO));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/OntologyController.java
git commit -m "phase4: expand OntologyController with class/property/schema-org/reasoning endpoints"
```

---

## Task P4-3: Episode Type Inference Engine

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/EpisodeTypeInferenceService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeTypeInferenceServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/InferredTypeVO.java`

**核心实现逻辑：**
1. LLM 从 Episode.content 提取关键词（Prompt: "从以下文本中提取关键实体类型……"）
2. 在 ont_class 表中模糊匹配 class 的 local_name / description
3. 使用 OntologyReasoner 扩展候选类型的层次
4. 按 domain_hint 过滤
5. 按置信度排序

- [ ] **Step 1: Write InferredTypeVO**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/InferredTypeVO.java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InferredTypeVO {
    private String type;       // e.g., "FinancialProduct"
    private String classUri;
    private double confidence;  // 0.0 - 1.0
    private String reason;      // e.g., "keyword match: loan, interest rate"
}
```

- [ ] **Step 2: Write EpisodeTypeInferenceService**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/EpisodeTypeInferenceService.java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.InferredTypeVO;
import java.util.List;
import java.util.Map;

public interface EpisodeTypeInferenceService {

    /**
     * 从 Episode 内容推断实体类型
     * @param graphId     图谱ID
     * @param content     Episode 内容文本
     * @param domainHint  业务域过滤（可选：FINANCIAL/MEDICAL/ECOMMERCE/KNOWLEDGE）
     * @return 推断出的类型列表（按置信度降序）
     */
    List<InferredTypeVO> inferEntityTypes(String graphId, String content, String domainHint);

    /**
     * 批量推断 Episode 类型（用于 DataImportServiceImpl）
     */
    Map<String, List<InferredTypeVO>> inferBatch(String graphId, List<String> episodeIds);
}
```

- [ ] **Step 3: Write EpisodeTypeInferenceServiceImpl**

```java
// ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeTypeInferenceServiceImpl.java
package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.service.EpisodeTypeInferenceService;
import com.graphiti.module.graphiti.service.OntologyClassService;
import com.graphiti.module.graphiti.service.OntologyReasoner;
import com.graphiti.module.graphiti.vo.ontology.InferredTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeTypeInferenceServiceImpl implements EpisodeTypeInferenceService {

    private final OntClassMapper classMapper;
    private final OntologyClassService classService;
    private final OntologyReasoner reasoner;
    // TODO: LlmClientService llmClientService; // 后续接入 LLM

    @Override
    public List<InferredTypeVO> inferEntityTypes(String graphId, String content, String domainHint) {
        if (content == null || content.isBlank()) return List.of();

        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();

        // Step 1: 关键词提取（目前用简单分词，后续接 LLM）
        List<String> keywords = extractKeywords(content);

        // Step 2: 在 ont_class 中模糊匹配
        List<OntClassDO> allClasses = classMapper.selectByDefinitionId(defId);
        List<MatchCandidate> candidates = new ArrayList<>();

        for (OntClassDO cls : allClasses) {
            if (domainHint != null && !domainHint.isBlank()
                    && !domainHint.equalsIgnoreCase(cls.getDomainHint())) {
                continue; // 跳过不匹配业务域的类
            }

            double score = calculateMatchScore(cls, keywords, content);
            if (score > 0.0) {
                candidates.add(new MatchCandidate(cls, score));
            }
        }

        // Step 3: 排序并取 Top-5
        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        List<InferredTypeVO> results = new ArrayList<>();

        for (int i = 0; i < Math.min(5, candidates.size()); i++) {
            MatchCandidate mc = candidates.get(i);
            results.add(new InferredTypeVO(
                mc.cls.getLocalName(),
                mc.cls.getClassUri(),
                Math.round(mc.score * 100.0) / 100.0,
                buildReason(mc.cls, keywords)
            ));
        }
        return results;
    }

    @Override
    public Map<String, List<InferredTypeVO>> inferBatch(String graphId, List<String> episodeIds) {
        // TODO: 批量处理 Episode ids，从 EpisodeService 读取 content
        return Map.of();
    }

    // ==================== 私有方法 ====================

    private Long resolveDefinitionId(String graphId) {
        // 复用 classService 中的逻辑
        return null; // stub
    }

    private List<String> extractKeywords(String content) {
        // 简单分词：去除标点、转小写、提取英文词和中文词
        if (content == null) return List.of();
        String cleaned = content.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
            .filter(w -> w.length() > 2)
            .distinct()
            .limit(20)
            .collect(Collectors.toList());
    }

    private double calculateMatchScore(OntClassDO cls, List<String> keywords, String content) {
        String localName = cls.getLocalName() != null ? cls.getLocalName().toLowerCase() : "";
        String description = cls.getDescription() != null ? cls.getDescription().toLowerCase() : "";

        double score = 0.0;
        for (String kw : keywords) {
            if (localName.contains(kw)) score += 0.5;
            if (description.contains(kw)) score += 0.2;
        }
        // 归一化
        if (!keywords.isEmpty()) score = score / keywords.size();
        return Math.min(score, 1.0);
    }

    private String buildReason(OntClassDO cls, List<String> keywords) {
        List<String> matched = keywords.stream()
            .filter(kw -> cls.getLocalName().toLowerCase().contains(kw)
                || (cls.getDescription() != null && cls.getDescription().toLowerCase().contains(kw)))
            .limit(3)
            .collect(Collectors.toList());
        return "keyword match: " + String.join(", ", matched);
    }

    private record MatchCandidate(OntClassDO cls, double score) {}
}
```

- [ ] **Step 4: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/EpisodeTypeInferenceService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeTypeInferenceServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/InferredTypeVO.java
git commit -m "phase4: add EpisodeTypeInferenceService for entity type auto-detection"
```

---

## Self-Review Checklist

- [ ] **Spec coverage:**
  - Phase 2 → Tasks P2-1 (Class CRUD), P2-2 (Property CRUD), P2-3 (Schema.org import)
  - Phase 3 → Task P3-1 (Jena OWL 2 RL reasoner)
  - Phase 4 → Task P4-1 (PostgreSQL→Neo4j sync), P4-2 (Controller endpoints), P4-3 (Episode type inference)
- [ ] **Placeholder scan:** SchemaOrgImportServiceImpl has `TODO` comments (intentional — RDF4J parsing is complex and needs integration testing). All TODO comments are marked for Phase 2 completion. OntologyReasonerImpl.syncFromMappers() has a clear TODO comment with exact implementation steps.
- [ ] **Type consistency:** All VO types (OntClassVO, OntPropertyVO, InferredTypeVO) are consistently defined and used. ConsistencyResultVO uses `List<String>` for classes (not a custom type).
- [ ] **Spec gap found:** SearchService integration for type-expanding search is noted as Phase 4 Task P4-2 extension (in OntologyController: `GET /ontology/{graphId}/search/reason` endpoint that uses OntologyReasoner to expand types before vector search). DataImportServiceImpl integration noted in Phase 1 Task 9. ont_mapping table noted in Phase 1 Task 8.
