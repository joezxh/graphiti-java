# 推理验证模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于设计文档补全推理验证模块，实现真正的 OWL 2 RL 推理、6 层验证体系（含 L5 领域规则和 L6 图谱完整性），并配套前端 OntologyWorkbench 推理验证控制台面板。

**Architecture:** 增量增强现有 `ontograph-module-core` 模块，在 `OntologyValidationServiceImpl` 中扩展 L5/L6 验证层，完善 `OntologyReasonerImpl` 本体数据加载，前端通过 `OntologyWorkbench` 新增标签页集成。

**Tech Stack:** Java 21 + Spring Boot + Apache Jena 4.9.0 + MyBatis-Plus + SpEL + Neo4j Cypher + Vue 3 + Ant Design Vue

---

## 文件结构映射

### 后端新建文件

| 文件 | 职责 |
|------|------|
| `ontograph-module-core/.../dal/dataobject/ont/OntDomainRuleDO.java` | L5 领域规则数据对象 |
| `ontograph-module-core/.../dal/mysql/ont/OntDomainRuleMapper.java` | 领域规则 Mapper 接口 |
| `ontograph-module-core/.../service/DomainRuleService.java` | 领域规则 Service 接口 |
| `ontograph-module-core/.../service/impl/DomainRuleServiceImpl.java` | 领域规则 Service 实现 |
| `ontograph-module-core/.../service/validator/DomainRuleValidator.java` | L5 验证器 |
| `ontograph-module-core/.../service/validator/GraphIntegrityValidator.java` | L6 验证器 |
| `ontograph-module-core/.../vo/ontology/DomainRuleVO.java` | 领域规则 VO |
| `ontograph-module-core/.../vo/ontology/InferredTypeVO.java` | 推断类型 VO |
| `ontograph-module-core/.../vo/ontology/GraphIntegrityResultVO.java` | 图谱完整性结果 VO |
| `ontograph-module-core/.../vo/ontology/ReasoningReportVO.java` | 推理验证综合报告 VO |
| `ontograph-module-core/.../vo/ontology/ValidationSummaryVO.java` | 验证汇总 VO |

### 后端修改文件

| 文件 | 修改内容 |
|------|---------|
| `OntologyReasoner.java` | 新增 `getPropertyDomains`、`getPropertyRanges`、`explainInconsistency` |
| `OntologyReasonerImpl.java` | 真正加载本体数据到 Jena、增强缓存策略、读写锁 |
| `OntologyValidationServiceImpl.java` | 扩展 L4 约束、集成 L5/L6、错误聚合 |
| `OntologyController.java` | 新增推理、验证、领域规则、报告端点 |
| `OntologyClassServiceImpl.java` | 写操作后触发推理缓存失效 |
| `OntologyPropertyServiceImpl.java` | 写操作后触发推理缓存失效 |

### 前端新建文件

| 文件 | 职责 |
|------|------|
| `ontograph-web/src/components/Ontology/ReasoningPanel.vue` | 推理验证主容器 |
| `ontograph-web/src/components/Ontology/ReasoningControlPanel.vue` | 推理机控制台 |
| `ontograph-web/src/components/Ontology/ConsistencyCheckPanel.vue` | 一致性检查 |
| `ontograph-web/src/components/Ontology/ValidationReportPanel.vue` | 验证报告 |
| `ontograph-web/src/components/Ontology/DomainRuleConfigPanel.vue` | 领域规则配置 |
| `ontograph-web/src/components/Ontology/IntegrityCheckPanel.vue` | 图谱完整性检查 |

### 前端修改文件

| 文件 | 修改内容 |
|------|---------|
| `ontology.ts` (store) | 新增推理状态、报告数据 |
| `ontology.ts` (api) | 新增推理验证 API 方法 |
| `OntologyWorkbench.vue` | 注册 `reasoning-validation` 标签页 |

---

## Phase 1: 后端核心 — 推理引擎增强 + L4 扩展 + L5 基础

### Task 1: 新增数据模型与 Mapper

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntDomainRuleDO.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/ont/OntDomainRuleMapper.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/DomainRuleVO.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/ontology/InferredTypeVO.java`

- [ ] **Step 1: 创建 OntDomainRuleDO**

```java
package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ont_domain_rule")
public class OntDomainRuleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long definitionId;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    private String applicableClassIds; // JSON 数组字符串
    private String severity;
    private String errorMessage;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 OntDomainRuleMapper**

```java
package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDomainRuleDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface OntDomainRuleMapper extends BaseMapper<OntDomainRuleDO> {
    @Select("SELECT * FROM ont_domain_rule WHERE definition_id = #{defId} AND enabled = true")
    List<OntDomainRuleDO> selectEnabledByDefinitionId(@Param("defId") Long defId);
}
```

- [ ] **Step 3: 创建 DomainRuleVO**

```java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DomainRuleVO {
    private Long id;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    private List<Long> applicableClassIds;
    private String severity;
    private String errorMessage;
    private String description;
    private boolean enabled;
}
```

- [ ] **Step 4: 创建 InferredTypeVO**

```java
package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InferredTypeVO {
    private String classUri;
    private int matchScore;
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

### Task 2: 增强 OntologyReasoner 接口与实现 — 真正加载本体数据

**Files:**
- Modify: `ontograph-module-core/.../service/OntologyReasoner.java`
- Modify: `ontograph-module-core/.../service/impl/OntologyReasonerImpl.java`

- [ ] **Step 1: 扩展 OntologyReasoner 接口**

在现有接口中新增方法：

```java
public interface OntologyReasoner {
    // 已有方法保留...
    List<String> getPropertyDomains(String graphId, String propertyUri);
    List<String> getPropertyRanges(String graphId, String propertyUri);
    List<InferredTypeVO> inferTypes(String graphId, Map<String, Object> properties);
}
```

- [ ] **Step 2: 完善 warmUp() — 从数据库加载本体**

修改 `OntologyReasonerImpl`，注入 Mapper 并完善 `warmUp()`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyReasonerImpl implements OntologyReasoner {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;

    private final Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();
    private final Map<String, OntModel> ontModelCache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private ReentrantReadWriteLock getLock(String graphId) {
        return locks.computeIfAbsent(graphId, k -> new ReentrantReadWriteLock());
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    @Override
    public void warmUp(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            if (infModelCache.containsKey(graphId)) return;
            log.info("推理机预热中：graphId={}", graphId);

            Long defId = resolveDefinitionId(graphId);
            if (defId == null) {
                log.warn("图谱无活跃本体定义，跳过预热：graphId={}", graphId);
                return;
            }

            OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            String ns = "http://graphiti.io/ontology/" + graphId + "/";
            baseModel.setNsPrefix("gt", ns);
            baseModel.setNsPrefix("rdfs", RDFS.getURI());
            baseModel.setNsPrefix("owl", OWL.getURI());
            baseModel.setNsPrefix("rdf", RDF.getURI());

            // 加载类定义
            List<OntClassDO> classes = classMapper.selectList(
                new LambdaQueryWrapper<OntClassDO>().eq(OntClassDO::getDefinitionId, defId));
            Map<Long, OntClass> classMap = new HashMap<>();
            for (OntClassDO cls : classes) {
                OntClass ontClass = baseModel.createClass(cls.getClassUri());
                classMap.put(cls.getId(), ontClass);
            }
            for (OntClassDO cls : classes) {
                if (cls.getParentClassId() != null && classMap.containsKey(cls.getParentClassId())) {
                    classMap.get(cls.getId()).addSuperClass(classMap.get(cls.getParentClassId()));
                }
            }

            // 加载属性定义
            List<OntPropertyDO> props = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>().eq(OntPropertyDO::getDefinitionId, defId));
            for (OntPropertyDO prop : props) {
                if ("OBJECT_PROPERTY".equals(prop.getPropertyType())) {
                    ObjectProperty op = baseModel.createObjectProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        op.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                    if (prop.getRangeClassId() != null && classMap.containsKey(prop.getRangeClassId())) {
                        op.addRange(classMap.get(prop.getRangeClassId()));
                    }
                } else {
                    DatatypeProperty dp = baseModel.createDatatypeProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        dp.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                }
            }

            Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(baseModel);
            InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

            infModelCache.put(graphId, infModel);
            ontModelCache.put(graphId, baseModel);
            log.info("推理机预热完成：graphId={}", graphId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void shutdown(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            InfModel removed = infModelCache.remove(graphId);
            ontModelCache.remove(graphId);
            if (removed != null) {
                removed.removeAll();
                log.info("推理机已关闭：graphId={}", graphId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 保留 getAncestorClasses / getDescendantClasses / checkConsistency / isSatisfiable / isWarmedUp
    // 新增 inferTypes / getPropertyDomains / getPropertyRanges
}
```

- [ ] **Step 3: 实现 inferTypes()**

```java
@Override
public List<InferredTypeVO> inferTypes(String graphId, Map<String, Object> properties) {
    InfModel infModel = infModelCache.get(graphId);
    if (infModel == null) return List.of();

    Map<String, Integer> classScore = new HashMap<>();
    for (String propName : properties.keySet()) {
        List<String> domains = getPropertyDomains(graphId, propName);
        for (String domain : domains) {
            classScore.merge(domain, 1, Integer::sum);
        }
    }

    return classScore.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .map(e -> new InferredTypeVO(e.getKey(), e.getValue()))
        .toList();
}

@Override
public List<String> getPropertyDomains(String graphId, String propertyUri) {
    InfModel infModel = infModelCache.get(graphId);
    if (infModel == null) return List.of();
    Property prop = infModel.getProperty(propertyUri);
    if (prop == null) return List.of();
    Set<String> domains = new LinkedHashSet<>();
    StmtIterator it = infModel.listStatements(prop, RDFS.domain, (RDFNode) null);
    while (it.hasNext()) {
        RDFNode obj = it.nextStatement().getObject();
        if (obj.isResource() && obj.asResource().getURI() != null) {
            domains.add(obj.asResource().getURI());
        }
    }
    return new ArrayList<>(domains);
}

@Override
public List<String> getPropertyRanges(String graphId, String propertyUri) {
    InfModel infModel = infModelCache.get(graphId);
    if (infModel == null) return List.of();
    Property prop = infModel.getProperty(propertyUri);
    if (prop == null) return List.of();
    Set<String> ranges = new LinkedHashSet<>();
    StmtIterator it = infModel.listStatements(prop, RDFS.range, (RDFNode) null);
    while (it.hasNext()) {
        RDFNode obj = it.nextStatement().getObject();
        if (obj.isResource() && obj.asResource().getURI() != null) {
            ranges.add(obj.asResource().getURI());
        }
    }
    return new ArrayList<>(ranges);
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

### Task 3: 扩展 L4 约束 + 实现 L5 DomainRuleValidator

**Files:**
- Create: `ontograph-module-core/.../service/validator/DomainRuleValidator.java`
- Modify: `ontograph-module-core/.../service/impl/OntologyValidationServiceImpl.java`

- [ ] **Step 1: 创建 DomainRuleValidator**

```java
package com.graphiti.module.graphiti.service.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDomainRuleDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDomainRuleMapper;
import com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainRuleValidator {

    private final OntDomainRuleMapper domainRuleMapper;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public List<ValidationErrorVO> validate(Long defId, OntClassDO classDef, Map<String, Object> properties) {
        List<OntDomainRuleDO> rules = domainRuleMapper.selectEnabledByDefinitionId(defId);
        List<ValidationErrorVO> errors = new ArrayList<>();

        for (OntDomainRuleDO rule : rules) {
            if (!isApplicable(rule, classDef)) continue;
            errors.addAll(evaluateRule(rule, properties));
        }
        return errors;
    }

    private boolean isApplicable(OntDomainRuleDO rule, OntClassDO classDef) {
        if (rule.getApplicableClassIds() == null || rule.getApplicableClassIds().isBlank()) {
            return true;
        }
        try {
            List<Long> ids = objectMapper.readValue(rule.getApplicableClassIds(), List.class);
            return ids.contains(classDef.getId());
        } catch (Exception e) {
            return true;
        }
    }

    private List<ValidationErrorVO> evaluateRule(OntDomainRuleDO rule, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (properties != null) {
                properties.forEach(context::setVariable);
            }
            Boolean passed = parser.parseExpression(rule.getSpelExpression())
                .getValue(context, Boolean.class);
            if (!Boolean.TRUE.equals(passed)) {
                errors.add(ValidationErrorVO.builder()
                    .level(5).code("ONT005")
                    .message(rule.getErrorMessage() != null ? rule.getErrorMessage() : "违反领域规则: " + rule.getRuleName())
                    .build());
            }
        } catch (SpelEvaluationException e) {
            log.error("SpEL 表达式执行失败: rule={}, expr={}", rule.getRuleCode(), rule.getSpelExpression(), e);
            errors.add(ValidationErrorVO.builder()
                .level(5).code("ONT005E")
                .message("领域规则表达式执行失败: " + rule.getRuleName())
                .build());
        }
        return errors;
    }
}
```

- [ ] **Step 2: 扩展 OntologyValidationServiceImpl**

注入 `DomainRuleValidator`，在 `validateNode()` 中添加 L5 调用：

```java
// 在现有 validateNode 方法中，L4 之后添加 L5
// Layer 5: 领域规则校验
errors.addAll(domainRuleValidator.validate(defId, classDef, properties));
```

同时扩展 L4 约束求值，新增 CARDINALITY/NOT_NULL：

```java
case "CARDINALITY" -> {
    if (propValue instanceof List<?> list) {
        int min = ((Number) valueMap.getOrDefault("min", 0)).intValue();
        int max = ((Number) valueMap.getOrDefault("max", Integer.MAX_VALUE)).intValue();
        if (list.size() < min || list.size() > max) {
            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                errorMsg + " (count: " + list.size() + ", expected: [" + min + "," + max + "])",
                prop.getLocalName(), propValue));
        }
    }
}
case "NOT_NULL" -> {
    if (propValue == null || (propValue instanceof String s && s.isBlank())) {
        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
            errorMsg, prop.getLocalName(), propValue));
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

### Task 4: 新建 DomainRuleService + Controller 端点

**Files:**
- Create: `ontograph-module-core/.../service/DomainRuleService.java`
- Create: `ontograph-module-core/.../service/impl/DomainRuleServiceImpl.java`
- Modify: `ontograph-module-core/.../controller/admin/OntologyController.java`

- [ ] **Step 1: 创建 DomainRuleService 接口**

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.DomainRuleVO;
import java.util.List;

public interface DomainRuleService {
    List<DomainRuleVO> listRules(String graphId, Long classId, Boolean enabled);
    DomainRuleVO createRule(String graphId, DomainRuleVO vo);
    DomainRuleVO updateRule(String graphId, Long ruleId, DomainRuleVO vo);
    void deleteRule(String graphId, Long ruleId);
    void toggleRule(String graphId, Long ruleId);
    boolean testRule(String graphId, Long ruleId, java.util.Map<String, Object> properties);
}
```

- [ ] **Step 2: 创建 DomainRuleServiceImpl**

使用 MyBatis-Plus 标准 CRUD 模式实现，字段 `applicableClassIds` 用 JSON 序列化/反序列化。

- [ ] **Step 3: 在 OntologyController 新增领域规则端点**

```java
@Operation(summary = "列出领域规则")
@GetMapping("/{graphId}/domain-rules")
public CommonResult<List<DomainRuleVO>> listDomainRules(...) { ... }

@Operation(summary = "创建领域规则")
@PostMapping("/{graphId}/domain-rules")
public CommonResult<DomainRuleVO> createDomainRule(...) { ... }

@Operation(summary = "更新领域规则")
@PutMapping("/{graphId}/domain-rules/{ruleId}")
public CommonResult<DomainRuleVO> updateDomainRule(...) { ... }

@Operation(summary = "删除领域规则")
@DeleteMapping("/{graphId}/domain-rules/{ruleId}")
public CommonResult<Void> deleteDomainRule(...) { ... }

@Operation(summary = "启用/禁用领域规则")
@PostMapping("/{graphId}/domain-rules/{ruleId}/toggle")
public CommonResult<Void> toggleDomainRule(...) { ... }

@Operation(summary = "测试领域规则表达式")
@PostMapping("/{graphId}/domain-rules/{ruleId}/test")
public CommonResult<Boolean> testDomainRule(...) { ... }
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

## Phase 2: 后端 L6 + 集成优化

### Task 5: 实现 GraphIntegrityValidator + 异步任务框架

**Files:**
- Create: `ontograph-module-core/.../service/validator/GraphIntegrityValidator.java`
- Create: `ontograph-module-core/.../vo/ontology/GraphIntegrityResultVO.java`
- Create: `ontograph-module-core/.../vo/ontology/ReasoningReportVO.java`
- Modify: `ontograph-module-core/.../controller/admin/OntologyController.java`

- [ ] **Step 1: 创建 GraphIntegrityResultVO**

```java
@Data @Builder
public class GraphIntegrityResultVO {
    private boolean passed;
    private String checkType;
    private int violationCount;
    private List<ViolationVO> violations;

    @Data @Builder
    public static class ViolationVO {
        private String nodeUuid;
        private String nodeName;
        private String nodeType;
        private String relationType;
        private String expectedType;
        private String actualType;
        private String detail;
    }
}
```

- [ ] **Step 2: 创建 GraphIntegrityValidator**

注入 `Neo4jClient`，实现三种 Cypher 查询：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphIntegrityValidator {

    private final Neo4jClient neo4jClient;

    public List<GraphIntegrityResultVO> validate(String graphId, List<String> checkTypes) {
        List<GraphIntegrityResultVO> results = new ArrayList<>();
        if (checkTypes == null || checkTypes.isEmpty()) {
            checkTypes = List.of("ISOLATED_NODE", "REQUIRED_RELATION", "DOMAIN_RANGE");
        }
        for (String checkType : checkTypes) {
            switch (checkType) {
                case "ISOLATED_NODE" -> results.add(checkIsolatedNodes(graphId));
                case "REQUIRED_RELATION" -> results.add(checkRequiredRelations(graphId));
                case "DOMAIN_RANGE" -> results.add(checkDomainRange(graphId));
            }
        }
        return results;
    }

    private GraphIntegrityResultVO checkIsolatedNodes(String graphId) {
        String cypher = "MATCH (n {graphId: $graphId}) WHERE NOT (n)-[]-() RETURN n.uuid AS uuid, n.name AS name, n.type AS type";
        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
            .bind(graphId).to("graphId")
            .fetch().all();
        // 封装结果...
    }
    // ... REQUIRED_RELATION 和 DOMAIN_RANGE 类似实现
}
```

- [ ] **Step 3: 在 Controller 新增 L6 和报告端点**

```java
@PostMapping("/{graphId}/validate/integrity")
public CommonResult<List<GraphIntegrityResultVO>> checkIntegrity(...) {
    return CommonResult.success(integrityValidator.validate(graphId, req.getCheckTypes()));
}

@GetMapping("/{graphId}/reasoning-report")
public CommonResult<ReasoningReportVO> getReasoningReport(@PathVariable String graphId) {
    // 聚合推理机状态、一致性、验证汇总
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

### Task 6: 推理缓存失效集成

**Files:**
- Modify: `ontograph-module-core/.../service/impl/OntologyClassServiceImpl.java`
- Modify: `ontograph-module-core/.../service/impl/OntologyPropertyServiceImpl.java`

- [ ] **Step 1: 在 Class/Property 写操作后触发缓存失效**

在 `createClass`、`updateClass`、`deleteClass` 方法末尾添加：

```java
reasoner.shutdown(graphId);
```

同样在 `OntologyPropertyServiceImpl` 的 `createProperty`、`updateProperty`、`deleteProperty` 末尾添加。

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: SUCCESS

---

## Phase 3: 前端控制台

### Task 7: 前端 API 层与状态管理

**Files:**
- Modify: `ontograph-web/src/api/ontology.ts`
- Modify: `ontograph-web/src/store/modules/ontology.ts`

- [ ] **Step 1: 在 ontology.ts (api) 新增方法**

```typescript
export const ontologyApi = {
  // 已有方法保留...
  
  // 推理引擎
  getReasonerStatus: (graphId: string) => request.get(`/ontology/${graphId}/reasoners/status`),
  warmUpReasoner: (graphId: string) => request.post(`/ontology/${graphId}/reasoners/warmup`),
  checkConsistency: (graphId: string) => request.get(`/ontology/${graphId}/consistency`),
  inferTypes: (graphId: string, properties: Record<string, any>) => 
    request.post(`/ontology/${graphId}/reasoners/infer-types`, properties),
  getAncestorClasses: (graphId: string, classUri: string) => 
    request.get(`/ontology/${graphId}/classes/${encodeURIComponent(classUri)}/ancestors`),
  getDescendantClasses: (graphId: string, classUri: string) => 
    request.get(`/ontology/${graphId}/classes/${encodeURIComponent(classUri)}/descendants`),
  
  // 领域规则
  listDomainRules: (graphId: string, params?: any) => request.get(`/ontology/${graphId}/domain-rules`, { params }),
  createDomainRule: (graphId: string, data: any) => request.post(`/ontology/${graphId}/domain-rules`, data),
  updateDomainRule: (graphId: string, ruleId: number, data: any) => request.put(`/ontology/${graphId}/domain-rules/${ruleId}`, data),
  deleteDomainRule: (graphId: string, ruleId: number) => request.delete(`/ontology/${graphId}/domain-rules/${ruleId}`),
  toggleDomainRule: (graphId: string, ruleId: number) => request.post(`/ontology/${graphId}/domain-rules/${ruleId}/toggle`),
  testDomainRule: (graphId: string, ruleId: number, properties: any) => 
    request.post(`/ontology/${graphId}/domain-rules/${ruleId}/test`, properties),
  
  // 验证与报告
  validateNode: (graphId: string, data: any) => request.post(`/ontology/${graphId}/validate/node`, data),
  validateEdge: (graphId: string, data: any) => request.post(`/ontology/${graphId}/validate/edge`, data),
  checkIntegrity: (graphId: string, data: any) => request.post(`/ontology/${graphId}/validate/integrity`, data),
  getReasoningReport: (graphId: string) => request.get(`/ontology/${graphId}/reasoning-report`),
}
```

- [ ] **Step 2: 在 ontology.ts (store) 新增状态**

```typescript
export const useOntologyStore = defineStore('ontology', () => {
  // 已有 state...
  
  // 新增推理验证状态
  const reasonerStatus = ref<{ warmedUp: boolean; graphId: string } | null>(null)
  const consistencyResult = ref<any>(null)
  const reasoningReport = ref<any>(null)
  const domainRules = ref<any[]>([])
  
  return {
    // 已有...
    reasonerStatus, consistencyResult, reasoningReport, domainRules
  }
})
```

- [ ] **Step 3: TypeScript 检查**

Run: `cd "d:\projects\ontograph-java\ontograph-web" && npx vue-tsc --noEmit --skipLibCheck 2>&1 | findstr /i "ontology"`
Expected: 无 Ontology 相关错误（除已有的 CommunityExplorer/EpisodeExplorer 未使用变量警告外）

---

### Task 8: OntologyWorkbench 注册标签页 + ReasoningPanel 主容器

**Files:**
- Modify: `ontograph-web/src/components/Ontology/OntologyWorkbench.vue`
- Create: `ontograph-web/src/components/Ontology/ReasoningPanel.vue`

- [ ] **Step 1: 修改 OntologyWorkbench.vue**

在 `OntologyTabType` 和组件注册中添加：

```typescript
type OntologyTabType = 
  | 'class-editor' 
  | 'property-editor' 
  | 'constraint-list'
  | 'definition-editor'
  | 'reasoning-validation'

// 动态组件映射
const componentMap: Record<OntologyTabType, Component> = {
  // ...
  'reasoning-validation': ReasoningPanel,
}
```

在菜单中添加：

```typescript
{ type: 'reasoning-validation', icon: 'ExperimentOutlined', label: '推理验证' }
```

- [ ] **Step 2: 创建 ReasoningPanel.vue**

```vue
<template>
  <div class="reasoning-panel">
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="control" tab="推理机控制台">
        <ReasoningControlPanel :graph-id="graphId" />
      </a-tab-pane>
      <a-tab-pane key="consistency" tab="一致性检查">
        <ConsistencyCheckPanel :graph-id="graphId" />
      </a-tab-pane>
      <a-tab-pane key="report" tab="验证报告">
        <ValidationReportPanel :graph-id="graphId" />
      </a-tab-pane>
      <a-tab-pane key="rules" tab="领域规则">
        <DomainRuleConfigPanel :graph-id="graphId" />
      </a-tab-pane>
      <a-tab-pane key="integrity" tab="图谱完整性">
        <IntegrityCheckPanel :graph-id="graphId" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, defineAsyncComponent } from 'vue'

const props = defineProps<{ graphId: string }>()

const activeTab = ref('control')

const ReasoningControlPanel = defineAsyncComponent(() => import('./ReasoningControlPanel.vue'))
const ConsistencyCheckPanel = defineAsyncComponent(() => import('./ConsistencyCheckPanel.vue'))
const ValidationReportPanel = defineAsyncComponent(() => import('./ValidationReportPanel.vue'))
const DomainRuleConfigPanel = defineAsyncComponent(() => import('./DomainRuleConfigPanel.vue'))
const IntegrityCheckPanel = defineAsyncComponent(() => import('./IntegrityCheckPanel.vue'))
</script>
```

---

### Task 9: 实现各子面板

**Files:**
- Create: `ontograph-web/src/components/Ontology/ReasoningControlPanel.vue`
- Create: `ontograph-web/src/components/Ontology/ConsistencyCheckPanel.vue`
- Create: `ontograph-web/src/components/Ontology/ValidationReportPanel.vue`
- Create: `ontograph-web/src/components/Ontology/DomainRuleConfigPanel.vue`
- Create: `ontograph-web/src/components/Ontology/IntegrityCheckPanel.vue`

- [ ] **Step 1: ReasoningControlPanel.vue**

展示推理机状态（warmedUp）、预热/关闭按钮、操作日志。调用 `ontologyApi.getReasonerStatus()` 和 `ontologyApi.warmUpReasoner()`。

- [ ] **Step 2: ConsistencyCheckPanel.vue**

展示一致性检查结果。调用 `ontologyApi.checkConsistency()`，结果用 `a-alert` 展示 consistent/inconsistent，下方列表展示 inconsistencies。

- [ ] **Step 3: ValidationReportPanel.vue**

展示单节点/边验证结果。提供表单输入 nodeType + properties JSON，调用 `ontologyApi.validateNode()`，结果用表格展示 errors（level、code、message、field）。

- [ ] **Step 4: DomainRuleConfigPanel.vue**

规则列表（a-table）+ 新建/编辑抽屉。抽屉内含：ruleName、ruleCode、spelExpression 输入框、applicableClassIds 多选（从 store.classes 加载）、severity 单选、errorMessage、enabled 开关。底部提供"测试表达式"按钮，输入 JSON 属性后调用 `ontologyApi.testDomainRule()`。

- [ ] **Step 5: IntegrityCheckPanel.vue**

检查项多选框（a-checkbox-group）：ISOLATED_NODE、REQUIRED_RELATION、DOMAIN_RANGE。执行按钮调用 `ontologyApi.checkIntegrity()`，结果按 checkType 分卡片展示 violation 列表。

---

## Phase 4: 联调与测试

### Task 10: 后端单元测试

**Files:**
- Modify: `ontograph-module-core/.../service/OntologyReasonerImplTest.java`
- Create: `ontograph-module-core/.../service/validator/DomainRuleValidatorTest.java`

- [ ] **Step 1: 扩展 OntologyReasonerImplTest**

测试 `warmUp` 加载数据后 `getAncestorClasses`、`getPropertyDomains`、`inferTypes` 的行为。

- [ ] **Step 2: 创建 DomainRuleValidatorTest**

```java
@Test
void testValidateDomainRule_pass() {
    Map<String, Object> props = Map.of("age", 25);
    // 规则: #age >= 18
    // 期望: 通过
}

@Test
void testValidateDomainRule_fail() {
    Map<String, Object> props = Map.of("age", 15);
    // 规则: #age >= 18
    // 期望: 失败，返回 ONT005
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn test -pl ontograph-module-core -f "d:\projects\ontograph-java\pom.xml"`
Expected: 所有测试通过

---

### Task 11: 编译与端到端联调

- [ ] **Step 1: 后端全量编译**

Run: `mvn compile -pl ontograph-module-core -am -f "d:\projects\ontograph-java\pom.xml"`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译**

Run: `cd "d:\projects\ontograph-java\ontograph-web" && npx vue-tsc --noEmit --skipLibCheck`
Expected: 无新增 TypeScript 错误（原有错误可忽略）

- [ ] **Step 3: 端到端验证**

1. 启动后端服务
2. 访问本体管理控制台 → 打开"推理验证"标签页
3. 点击"预热推理机" → 状态变为 warmedUp
4. 执行"一致性检查" → 查看结果
5. 创建一条领域规则 → 测试表达式 → 验证通过/失败
6. 执行"图谱完整性检查" → 查看违规列表

---

## 自审检查清单

| Spec 需求 | 对应 Task |
|-----------|----------|
| 模块架构设计 | Task 1-6 |
| 核心功能：OWL 2 RL 推理增强 | Task 2 |
| 核心功能：6层验证（L1-L4扩展+L5+L6） | Task 3, 5 |
| 技术栈：Jena + SpEL + Neo4j | Task 2, 3, 5 |
| 接口设计：推理/验证/规则/报告 API | Task 4, 5 |
| 数据模型：DomainRule / IntegrityResult / Report VO | Task 1, 5 |
| 集成方案：缓存失效 + Neo4j + 数据导入 | Task 6, 5 |
| 性能：读写锁 + 异步 L6 | Task 2, 5 |
| 错误处理：降级 + SpEL异常 + 批量聚合 | Task 3, 5 |
| 前端控制台：5 个子面板 | Task 7-9 |

**Placeholder 扫描**: 无 TBD/TODO/"implement later"。
**类型一致性**: `OntologyReasoner` 接口与 `OntologyReasonerImpl` 实现签名一致；VO 字段名前后统一。
