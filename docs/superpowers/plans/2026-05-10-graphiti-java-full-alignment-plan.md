# ontograph-java 功能对齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过4个阶段使 ontograph-java 具备与 Python 原版 graphiti 相同的核心能力，包括时序事实管理、LLM自动提取、混合检索、嵌入向量、社区发现等。

**Architecture:** 在现有Maven多模块架构基础上，逐步添加新模块和服务，保持向后兼容。

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring AI 1.1.2, Neo4j 5.26, MySQL, Redis

---

## 文件结构映射

```
ontograph-module-core/src/main/java/com/graphiti/module/graphiti/
├── config/
│   ├── GraphNeo4jConfig.java              # 已有 - 扩展向量索引
│   ├── LlmClientConfig.java               # 新增
│   └── EmbedderConfig.java                # 新增
├── controller/admin/
│   ├── GraphitiController.java            # 已有 - 扩展接口
│   ├── SearchController.java              # 已有 - 扩展混合检索
│   ├── MaintenanceController.java         # 新增
│   └── EpisodeController.java             # 已有 - 扩展LLM提取
├── service/
│   ├── LlmClientService.java              # 新增
│   ├── EmbedderService.java               # 新增
│   ├── TemporalService.java               # 新增
│   ├── CommunityService.java              # 新增
│   ├── RerankerService.java               # 新增
│   └── DataQualityService.java            # 新增
├── service/impl/
│   ├── LlmClientServiceImpl.java          # 新增
│   ├── EmbedderServiceImpl.java           # 新增
│   ├── TemporalServiceImpl.java           # 新增
│   ├── CommunityServiceImpl.java          # 新增
│   └── RerankerServiceImpl.java           # 新增
├── dal/dataobject/
│   └── LlmConfigDO.java                   # 新增
├── vo/llm/
│   ├── LlmConfigVO.java                   # 新增
│   ├── ExtractedEntityVO.java             # 新增
│   └── ExtractedRelationVO.java           # 新增
├── vo/temporal/
│   ├── TemporalNodeVO.java                # 新增
│   └── TemporalEdgeVO.java                # 新增
└── resources/prompts/
    ├── extract_entities.txt               # 新增
    ├── extract_relations.txt              # 新增
    └── generate_summary.txt               # 新增
```

---

## 阶段一：P0核心能力 (4-6周)

### Task 1: 项目依赖配置

**Files:**
- Modify: `ontograph-module-core/pom.xml`
- Modify: `ontograph-server/pom.xml`

**说明**: 添加Spring AI、Neo4j向量索引等依赖

**Step 1: 在 ontograph-module-core/pom.xml 中添加Spring AI依赖**

在 `<dependencies>` 节中添加：

```xml
<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Neo4j Vector Index Support -->
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>${neo4j.version}</version>
</dependency>
```

**Step 2: 在 ontograph-server/pom.xml 中添加Spring AI BOM**

在 `<dependencies>` 节中添加：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring.ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

**Step 3: 验证依赖**

```bash
cd d:\projects\ontograph-java && mvn clean install -DskipTests
```

Expected: BUILD SUCCESS

---

### Task 2: 嵌入向量服务 (EmbedderService)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/EmbedderService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EmbedderServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/config/EmbedderConfig.java`

**Step 1: 创建 EmbedderService 接口**

```java
package com.graphiti.module.graphiti.service;

import java.util.List;

/**
 * 嵌入向量服务接口
 */
public interface EmbedderService {

    /**
     * 生成文本嵌入向量
     */
    float[] embed(String text);

    /**
     * 批量生成嵌入向量
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 计算余弦相似度
     */
    double cosineSimilarity(float[] a, float[] b);

    /**
     * 获取向量维度
     */
    int getDimensions();
}
```

**Step 2: 创建 EmbedderServiceImpl 实现**

```java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbedderServiceImpl implements EmbedderService {

    private final EmbeddingClient embeddingClient;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[getDimensions()];
        }
        try {
            var embedding = embeddingClient.embed(text);
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }
            return result;
        } catch (Exception e) {
            log.error("生成嵌入向量失败: {}", e.getMessage());
            return new float[getDimensions()];
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public int getDimensions() {
        return 1536; // OpenAI text-embedding-3-small
    }
}
```

**Step 3: 创建 EmbedderConfig 配置**

```java
package com.graphiti.module.graphiti.config;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbedderConfig {

    @Bean
    public org.springframework.ai.embedding.EmbeddingClient embeddingClient() {
        // Spring Boot auto-configuration will create this from properties
        return null;
    }
}
```

**Step 4: 在 application.yml 中添加嵌入配置**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      embedding:
        options:
          model: text-embedding-3-small
```

**Step 5: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/EmbedderService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EmbedderServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/config/EmbedderConfig.java
git add ontograph-server/src/main/resources/application.yml
git commit -m "feat: add embedder service with Spring AI integration"
```

---

### Task 3: LLM客户端服务 (LlmClientService)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/LlmClientService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/LlmClientServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/llm/ExtractedEntityVO.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/llm/ExtractedRelationVO.java`

**Step 1: 创建 ExtractedEntityVO**

```java
package com.graphiti.module.graphiti.vo.llm;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class ExtractedEntityVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String type;
    private String summary;
    private Map<String, Object> attributes;
}
```

**Step 2: 创建 ExtractedRelationVO**

```java
package com.graphiti.module.graphiti.vo.llm;

import lombok.Data;
import java.io.Serializable;

@Data
public class ExtractedRelationVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sourceEntityName;
    private String targetEntityName;
    private String relationType;
    private String fact;
}
```

**Step 3: 创建 LlmClientService 接口**

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.llm.ExtractedEntityVO;
import com.graphiti.module.graphiti.vo.llm.ExtractedRelationVO;

import java.util.List;

/**
 * LLM客户端服务接口
 */
public interface LlmClientService {

    /**
     * 从文本中提取实体
     */
    List<ExtractedEntityVO> extractEntities(String text, List<String> entityTypes);

    /**
     * 从文本中提取关系
     */
    List<ExtractedRelationVO> extractRelations(String text, List<ExtractedEntityVO> entities);

    /**
     * 生成内容摘要
     */
    String generateSummary(String content);

    /**
     * 生成社区摘要
     */
    String generateCommunitySummary(List<String> nodeSummaries);
}
```

**Step 4: 创建 Prompt 模板文件**

`ontograph-module-core/src/main/resources/prompts/extract_entities.txt`:

```
从以下文本中提取实体。每个实体应包含名称、类型和摘要。

可用实体类型: {entityTypes}

文本内容:
{text}

请以JSON格式返回，结构如下:
[
  {
    "name": "实体名称",
    "type": "实体类型",
    "summary": "实体摘要",
    "attributes": {}
  }
]
```

`ontograph-module-core/src/main/resources/prompts/extract_relations.txt`:

```
从以下文本中提取实体之间的关系。

已知实体:
{entities}

文本内容:
{text}

请以JSON格式返回，结构如下:
[
  {
    "sourceEntityName": "源实体名称",
    "targetEntityName": "目标实体名称",
    "relationType": "关系类型",
    "fact": "事实描述"
  }
]
```

**Step 5: 创建 LlmClientServiceImpl 实现**

```java
package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.vo.llm.ExtractedEntityVO;
import com.graphiti.module.graphiti.vo.llm.ExtractedRelationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientServiceImpl implements LlmClientService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<ExtractedEntityVO> extractEntities(String text, List<String> entityTypes) {
        try {
            String prompt = loadPrompt("prompts/extract_entities.txt")
                    .replace("{entityTypes}", String.join(", ", entityTypes))
                    .replace("{text}", text);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return objectMapper.readValue(response, new TypeReference<List<ExtractedEntityVO>>() {});
        } catch (Exception e) {
            log.error("实体提取失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ExtractedRelationVO> extractRelations(String text, List<ExtractedEntityVO> entities) {
        try {
            String prompt = loadPrompt("prompts/extract_relations.txt")
                    .replace("{entities}", objectMapper.writeValueAsString(entities))
                    .replace("{text}", text);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return objectMapper.readValue(response, new TypeReference<List<ExtractedRelationVO>>() {});
        } catch (Exception e) {
            log.error("关系提取失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String generateSummary(String content) {
        try {
            String prompt = "请为以下内容生成简洁的摘要（不超过100字）:\n\n" + content;
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("摘要生成失败: {}", e.getMessage());
            return content.substring(0, Math.min(content.length(), 100));
        }
    }

    @Override
    public String generateCommunitySummary(List<String> nodeSummaries) {
        try {
            String prompt = "请为以下社区成员生成社区摘要:\n\n" + String.join("\n", nodeSummaries);
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("社区摘要生成失败: {}", e.getMessage());
            return String.join(", ", nodeSummaries).substring(0, Math.min(200, String.join(", ", nodeSummaries).length()));
        }
    }

    private String loadPrompt(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
```

**Step 6: 在 application.yml 中添加LLM配置**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.2
```

**Step 7: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/LlmClientService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/LlmClientServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/llm/
git add ontograph-module-core/src/main/resources/prompts/
git commit -m "feat: add LLM client service with entity/relation extraction"
```

---

### Task 4: 时序事实管理 (TemporalService)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/TemporalService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/TemporalServiceImpl.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/temporal/TemporalNodeVO.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/temporal/TemporalEdgeVO.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java`

**Step 1: 创建 TemporalNodeVO 和 TemporalEdgeVO**

```java
package com.graphiti.module.graphiti.vo.temporal;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TemporalNodeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String name;
    private String type;
    private String summary;
    private LocalDateTime validAt;
    private LocalDateTime invalidAt;
    private Map<String, Object> properties;
}
```

```java
package com.graphiti.module.graphiti.vo.temporal;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TemporalEdgeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String sourceUuid;
    private String targetUuid;
    private String type;
    private String fact;
    private LocalDateTime validAt;
    private LocalDateTime invalidAt;
    private Map<String, Object> properties;
}
```

**Step 2: 创建 TemporalService 接口**

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.temporal.TemporalEdgeVO;
import com.graphiti.module.graphiti.vo.temporal.TemporalNodeVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 时序事实管理服务接口
 */
public interface TemporalService {

    /**
     * 标记与指定实体相关的旧事实为失效
     */
    void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt);

    /**
     * 查询当前有效的节点（默认当前时间）
     */
    List<TemporalNodeVO> getValidNodes(String graphId);

    /**
     * 查询指定时间有效的节点
     */
    List<TemporalNodeVO> getValidNodesAt(String graphId, LocalDateTime referenceTime);

    /**
     * 查询指定时间有效的边
     */
    List<TemporalEdgeVO> getValidEdgesAt(String graphId, LocalDateTime referenceTime);

    /**
     * 获取实体的事实版本链
     */
    List<TemporalNodeVO> getFactVersions(String graphId, String entityName);
}
```

**Step 3: 创建 TemporalServiceImpl 实现**

```java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.TemporalService;
import com.graphiti.module.graphiti.vo.temporal.TemporalEdgeVO;
import com.graphiti.module.graphiti.vo.temporal.TemporalNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemporalServiceImpl implements TemporalService {

    private final Driver neo4jDriver;

    @Override
    public void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WHERE n.name IN $entityNames AND (n.invalid_at IS NULL OR n.invalid_at > $invalid_at) " +
            "SET n.invalid_at = $invalid_at";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters(
                "group_id", graphId,
                "entityNames", entityNames,
                "invalid_at", invalidAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ));
            log.info("已标记 {} 个实体的旧事实为失效", entityNames.size());
        }
    }

    @Override
    public List<TemporalNodeVO> getValidNodes(String graphId) {
        return getValidNodesAt(graphId, LocalDateTime.now());
    }

    @Override
    public List<TemporalNodeVO> getValidNodesAt(String graphId, LocalDateTime referenceTime) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WHERE n.valid_at <= $reference_time " +
            "  AND (n.invalid_at IS NULL OR n.invalid_at > $reference_time) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, " +
            "       n.summary as summary, n.valid_at as valid_at, n.invalid_at as invalid_at, " +
            "       properties(n) as props " +
            "ORDER BY n.valid_at DESC";

        long refTime = referenceTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<TemporalNodeVO> nodes = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "reference_time", refTime
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalNodeVO node = new TemporalNodeVO();
                node.setUuid(record.get("uuid").asString());
                node.setName(record.get("name").asString());
                node.setType(record.get("type").asString());
                node.setSummary(record.get("summary").asString(null));
                node.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    node.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                node.setProperties(record.get("props").asMap());
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public List<TemporalEdgeVO> getValidEdgesAt(String graphId, LocalDateTime referenceTime) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO]->(b:Entity {group_id: $group_id}) " +
            "WHERE r.valid_at <= $reference_time " +
            "  AND (r.invalid_at IS NULL OR r.invalid_at > $reference_time) " +
            "RETURN r.uuid as uuid, a.uuid as source_uuid, b.uuid as target_uuid, " +
            "       r.type as type, r.fact as fact, r.valid_at as valid_at, r.invalid_at as invalid_at " +
            "ORDER BY r.valid_at DESC";

        long refTime = referenceTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<TemporalEdgeVO> edges = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "reference_time", refTime
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalEdgeVO edge = new TemporalEdgeVO();
                edge.setUuid(record.get("uuid").asString());
                edge.setSourceUuid(record.get("source_uuid").asString());
                edge.setTargetUuid(record.get("target_uuid").asString());
                edge.setType(record.get("type").asString());
                edge.setFact(record.get("fact").asString(null));
                edge.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    edge.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                edges.add(edge);
            }
        }
        return edges;
    }

    @Override
    public List<TemporalNodeVO> getFactVersions(String graphId, String entityName) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id, name: $name}) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, " +
            "       n.summary as summary, n.valid_at as valid_at, n.invalid_at as invalid_at " +
            "ORDER BY n.valid_at DESC";

        List<TemporalNodeVO> versions = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "name", entityName
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalNodeVO node = new TemporalNodeVO();
                node.setUuid(record.get("uuid").asString());
                node.setName(record.get("name").asString());
                node.setType(record.get("type").asString());
                node.setSummary(record.get("summary").asString(null));
                node.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    node.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                versions.add(node);
            }
        }
        return versions;
    }

    private LocalDateTime millisToLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        );
    }
}
```

**Step 4: 修改 GraphNeo4jService 添加时序支持**

在 `createEntityNode` 方法中添加时序字段：

```java
public Map<String, Object> createEntityNode(String graphId, String uuid, String name, String type, 
        Map<String, Object> properties, String summary, LocalDateTime validAt) {
    String cypher = "CREATE (n:Entity {group_id: $group_id, uuid: $uuid, name: $name, type: $type, " +
                    "summary: $summary, valid_at: $valid_at}) SET n += $props RETURN n";
    
    Map<String, Object> params = new HashMap<>();
    params.put("group_id", graphId);
    params.put("uuid", uuid);
    params.put("name", name);
    params.put("type", type);
    params.put("summary", summary != null ? summary : "");
    params.put("valid_at", validAt != null ? 
        validAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 
        System.currentTimeMillis());
    params.put("props", properties != null ? properties : new HashMap<>());
    
    // ... 原有代码
}
```

**Step 5: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/TemporalService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/TemporalServiceImpl.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/vo/temporal/
git commit -m "feat: add temporal fact management service"
```

---

### Task 5: 混合检索系统 (SearchService增强)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/RerankerService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/RerankerServiceImpl.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/SearchService.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/SearchServiceImpl.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/SearchController.java`

**Step 1: 创建 RerankerService 接口和实现**

```java
package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 重排序服务接口
 */
public interface RerankerService {

    /**
     * RRF (Reciprocal Rank Fusion) 重排序
     */
    List<Map<String, Object>> rrfRerank(List<List<Map<String, Object>>> resultLists, int k);

    /**
     * MMR (Maximal Marginal Relevance) 重排序
     */
    List<Map<String, Object>> mmrRerank(List<Map<String, Object>> results, float[] queryEmbedding,
                                        double lambda, EmbedderService embedderService);
}
```

```java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.RerankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RerankerServiceImpl implements RerankerService {

    @Override
    public List<Map<String, Object>> rrfRerank(List<List<Map<String, Object>>> resultLists, int k) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Map<String, Object>> items = new HashMap<>();

        for (List<Map<String, Object>> list : resultLists) {
            for (int i = 0; i < list.size(); i++) {
                String uuid = (String) list.get(i).get("uuid");
                double score = 1.0 / (k + i + 1);
                scores.merge(uuid, score, Double::sum);
                items.putIfAbsent(uuid, list.get(i));
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>(items.get(e.getKey()));
                    item.put("score", e.getValue());
                    return item;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> mmrRerank(List<Map<String, Object>> results, float[] queryEmbedding,
                                                double lambda, EmbedderService embedderService) {
        List<Map<String, Object>> selected = new ArrayList<>();
        Set<String> selectedUuids = new HashSet<>();

        while (selected.size() < results.size()) {
            Map<String, Object> bestItem = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Map<String, Object> item : results) {
                String uuid = (String) item.get("uuid");
                if (selectedUuids.contains(uuid)) continue;

                double relevance = item.containsKey("similarity") ? 
                    ((Number) item.get("similarity")).doubleValue() : 0.5;

                double maxSim = 0;
                for (Map<String, Object> sel : selected) {
                    // 简化：使用名称相似度
                    double sim = calculateNameSimilarity(
                        (String) item.get("name"), (String) sel.get("name"));
                    maxSim = Math.max(maxSim, sim);
                }

                double mmrScore = lambda * relevance - (1 - lambda) * maxSim;
                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestItem = item;
                }
            }

            if (bestItem == null) break;
            selected.add(bestItem);
            selectedUuids.add((String) bestItem.get("uuid"));
        }

        return selected;
    }

    private double calculateNameSimilarity(String a, String b) {
        if (a == null || b == null) return 0;
        // 简化实现：Jaccard相似度
        Set<String> setA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\s+")));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}
```

**Step 2: 增强 SearchService 接口**

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.search.SearchConfigVO;
import com.graphiti.module.graphiti.vo.search.SearchResultsVO;

import java.util.List;
import java.util.Map;

public interface SearchService {

    // 已有方法...

    /**
     * 混合检索 (语义 + 全文 + 图遍历)
     */
    SearchResultsVO hybridSearch(String graphId, String query, SearchConfigVO config);

    /**
     * 语义搜索 (向量相似度)
     */
    List<Map<String, Object>> semanticSearch(String graphId, String query, int limit);

    /**
     * BFS图遍历搜索
     */
    List<Map<String, Object>> bfsSearch(String graphId, String startNodeUuid, int depth, int limit);

    /**
     * 全文搜索 (BM25)
     */
    List<Map<String, Object>> fullTextSearch(String graphId, String query, int limit);
}
```

**Step 3: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/RerankerService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/RerankerServiceImpl.java
git commit -m "feat: add reranker service with RRF and MMR"
```

---

### Task 6: 社区发现服务 (CommunityService)

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/CommunityService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java`

**Step 1: 创建 CommunityService 接口和实现**

```java
package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 社区发现服务接口
 */
public interface CommunityService {

    /**
     * 构建社区
     */
    Map<String, Object> buildCommunities(String graphId);

    /**
     * 获取社区列表
     */
    List<Map<String, Object>> listCommunities(String graphId);

    /**
     * 搜索社区
     */
    List<Map<String, Object>> searchCommunities(String graphId, String query);

    /**
     * 删除所有社区
     */
    void removeCommunities(String graphId);
}
```

```java
package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.CommunityService;
import com.graphiti.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final Driver neo4jDriver;
    private final LlmClientService llmClientService;

    @Override
    public Map<String, Object> buildCommunities(String graphId) {
        // 1. 清除现有社区
        removeCommunities(graphId);

        // 2. 使用简单的标签传播算法进行社区发现
        String cypher =
            "MATCH (n:Entity {group_id: $group_id})-[r:RELATES_TO]->(m:Entity {group_id: $group_id}) " +
            "WITH n, m " +
            "WHERE n.type = m.type OR EXISTS { " +
            "  MATCH (n)-[:RELATES_TO]->(x:Entity {group_id: $group_id})<-[:RELATES_TO]-(m) " +
            "} " +
            "WITH n, collect(m) as neighbors " +
            "WHERE size(neighbors) >= 2 " +
            "RETURN n.uuid as center_uuid, n.name as center_name, n.type as center_type, " +
            "       [neighbor in neighbors | neighbor.uuid] as member_uuids, " +
            "       [neighbor in neighbors | neighbor.name] as member_names " +
            "LIMIT 50";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("group_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("centerUuid", record.get("center_uuid").asString());
                community.put("centerName", record.get("center_name").asString());
                community.put("memberUuids", record.get("member_uuids").asList());
                community.put("memberNames", record.get("member_names").asList());
                communities.add(community);
            }
        }

        // 3. 为每个社区生成摘要并创建社区节点
        int communityCount = 0;
        for (Map<String, Object> community : communities) {
            List<String> memberNames = (List<String>) community.get("memberNames");
            String summary = llmClientService.generateCommunitySummary(memberNames);

            String communityUuid = UUID.randomUUID().toString().replace("-", "");

            // 创建社区节点
            String createCypher =
                "CREATE (c:Community {group_id: $group_id, uuid: $uuid, name: $name, " +
                "summary: $summary, member_count: $member_count}) " +
                "WITH c " +
                "UNWIND $member_uuids as memberUuid " +
                "MATCH (m:Entity {group_id: $group_id, uuid: memberUuid}) " +
                "CREATE (m)-[:HAS_COMMUNITY]->(c)";

            try (Session session = neo4jDriver.session()) {
                session.run(createCypher, Values.parameters(
                    "group_id", graphId,
                    "uuid", communityUuid,
                    "name", "Community-" + community.get("centerName"),
                    "summary", summary,
                    "member_count", memberNames.size(),
                    "member_uuids", community.get("memberUuids")
                ));
                communityCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("communityCount", communityCount);
        result.put("message", "社区构建完成");
        return result;
    }

    @Override
    public List<Map<String, Object>> listCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count " +
            "ORDER BY c.member_count DESC";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("group_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public List<Map<String, Object>> searchCommunities(String graphId, String query) {
        // 简化为全文搜索社区名称和摘要
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "WHERE c.name CONTAINS $query OR c.summary CONTAINS $query " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count " +
            "ORDER BY c.member_count DESC " +
            "LIMIT 10";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "query", query
            ));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public void removeCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "DETACH DELETE c";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("group_id", graphId));
            log.info("已清除图谱 {} 的所有社区", graphId);
        }
    }
}
```

**Step 2: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/CommunityService.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java
git commit -m "feat: add community detection service"
```

---

### Task 7: 增强现有Controller接口

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphitiController.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/SearchController.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/EpisodeController.java`

**Step 1: 在 GraphitiController 中添加社区构建和数据导出接口**

```java
// 添加注入
private final CommunityService communityService;

// 添加接口
@Operation(summary = "构建社区", description = "对指定图谱执行社区发现算法")
@PostMapping("/{graphId}/communities/build")
public CommonResult<Map<String, Object>> buildCommunities(
        @PathVariable("graphId") String graphId) {
    return CommonResult.success(communityService.buildCommunities(graphId));
}

@Operation(summary = "获取社区列表", description = "获取指定图谱的社区列表")
@GetMapping("/{graphId}/communities")
public CommonResult<List<Map<String, Object>>> listCommunities(
        @PathVariable("graphId") String graphId) {
    return CommonResult.success(communityService.listCommunities(graphId));
}

@Operation(summary = "克隆图谱", description = "克隆指定图谱")
@PostMapping("/{graphId}/clone")
public CommonResult<GraphInfoRespVO> cloneGraph(
        @PathVariable("graphId") String graphId) {
    return CommonResult.success(graphitiService.cloneGraph(graphId));
}

@Operation(summary = "导出图谱", description = "导出指定图谱数据")
@GetMapping("/{graphId}/export")
public CommonResult<Map<String, Object>> exportGraph(
        @PathVariable("graphId") String graphId) {
    return CommonResult.success(graphitiService.exportGraph(graphId));
}

@Operation(summary = "历史状态查询", description = "查询指定时间点的图谱状态")
@GetMapping("/{graphId}/history")
public CommonResult<Map<String, Object>> getHistory(
        @PathVariable("graphId") String graphId,
        @RequestParam("time") @Parameter(description = "查询时间") String time) {
    LocalDateTime referenceTime = LocalDateTime.parse(time);
    Map<String, Object> result = new HashMap<>();
    result.put("nodes", temporalService.getValidNodesAt(graphId, referenceTime));
    result.put("edges", temporalService.getValidEdgesAt(graphId, referenceTime));
    return CommonResult.success(result);
}
```

**Step 2: 在 SearchController 中添加混合检索接口**

```java
// 添加注入
private final RerankerService rerankerService;
private final EmbedderService embedderService;

// 添加接口
@Operation(summary = "混合检索", description = "执行语义+全文+图遍历的混合检索")
@PostMapping("/hybrid")
public CommonResult<SearchResultsVO> hybridSearch(
        @Valid @RequestBody HybridSearchReqVO reqVO) {
    return CommonResult.success(searchService.hybridSearch(
        reqVO.getGraphId(), reqVO.getQuery(), reqVO.getConfig()));
}

@Operation(summary = "语义搜索", description = "基于向量相似度的语义搜索")
@PostMapping("/semantic")
public CommonResult<List<Map<String, Object>>> semanticSearch(
        @Valid @RequestBody SemanticSearchReqVO reqVO) {
    return CommonResult.success(searchService.semanticSearch(
        reqVO.getGraphId(), reqVO.getQuery(), reqVO.getLimit()));
}

@Operation(summary = "BFS搜索", description = "从指定节点开始BFS图遍历搜索")
@PostMapping("/bfs")
public CommonResult<List<Map<String, Object>>> bfsSearch(
        @Valid @RequestBody BfsSearchReqVO reqVO) {
    return CommonResult.success(searchService.bfsSearch(
        reqVO.getGraphId(), reqVO.getStartNodeUuid(), 
        reqVO.getDepth(), reqVO.getLimit()));
}
```

**Step 3: Commit**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphitiController.java
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/SearchController.java
git commit -m "feat: add community, clone, export, history and hybrid search APIs"
```

---

### Task 8: 前端API更新

**Files:**
- Modify: `ontograph-web/src/api/graph.ts`
- Modify: `ontograph-web/src/api/search.ts`

**Step 1: 在 graph.ts 中添加新接口**

```typescript
// 社区相关
export const graphApi = {
  // 已有方法...

  // 社区
  async buildCommunity(graphId: string): Promise<{ communityCount: number; message: string }> {
    return request.post(`/graph/${graphId}/communities/build`)
  },

  async getCommunities(graphId: string): Promise<any[]> {
    return request.get(`/graph/${graphId}/communities`)
  },

  // 克隆
  async clone(graphId: string): Promise<Graph> {
    return request.post(`/graph/${graphId}/clone`)
  },

  // 导出
  async exportData(graphId: string): Promise<any> {
    return request.get(`/graph/${graphId}/export`)
  },

  // 历史状态
  async getHistory(graphId: string, time: string): Promise<{ nodes: any[]; edges: any[] }> {
    return request.get(`/graph/${graphId}/history?time=${time}`)
  }
}
```

**Step 2: Commit**

```bash
git add ontograph-web/src/api/graph.ts
git commit -m "feat: update frontend APIs for new backend features"
```

---

## 阶段二：P1重要功能 (4-5周)

### Task 9: 数据质量保障

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/DataQualityService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/DataQualityServiceImpl.java`

**说明**: 实现节点去重、边去重、实体解析

### Task 10: Saga管理

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/SagaService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/SagaServiceImpl.java`

**说明**: 实现SagaNode、NEXT_EPISODE边、Episode时序链

### Task 11: 多数据库驱动抽象

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphDriverService.java`
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/Neo4jDriverAdapter.java`

**说明**: 设计GraphDriver接口，抽象Neo4j操作

---

## 阶段三：P2功能完善 (3-4周)

### Task 12: 本体系统增强

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/OntologyService.java`

**说明**: 增强本体定义，支持字段定义和验证

### Task 13: 边类型多样化

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java`

**说明**: 添加EpisodicEdge、CommunityEdge等边类型

### Task 14: 图谱克隆/导出

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphitiService.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/GraphitiServiceImpl.java`

**说明**: 实现cloneGraph()和exportGraph()

---

## 阶段四：P3增强功能 (2-3周)

### Task 15: 可观测性

**Files:**
- Create: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/config/TelemetryConfig.java`

**说明**: OpenTelemetry集成

### Task 16: 性能优化

**Files:**
- Modify: 各Service实现类

**说明**: 并行处理、批量操作优化

---

## 测试策略

### 单元测试
- 每个Service接口对应一个测试类
- 使用Mockito模拟Neo4j和LLM调用

### 集成测试
- 使用Testcontainers启动Neo4j
- 测试完整的Episode添加→提取→检索流程

### API测试
- 使用RestAssured测试Controller接口

---

## 部署配置

### application.yml 更新

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.2
      embedding:
        options:
          model: text-embedding-3-small

graphiti:
  search:
    default-limit: 10
    rrf-k: 60
    mmr-lambda: 0.5
  temporal:
    auto-invalidate: true
  community:
    min-members: 2
    max-communities: 50
```

---

## 验证清单

- [ ] 时序管理: 添加Episode后旧事实自动失效
- [ ] LLM提取: 文本导入后自动提取实体和关系
- [ ] 混合检索: 语义+全文+BFS搜索返回正确结果
- [ ] 嵌入向量: 节点/边嵌入生成成功
- [ ] 社区发现: 社区构建成功，LLM生成摘要
- [ ] 数据质量: 重复节点/边被正确去重
- [ ] API完整: 所有新增接口通过Swagger测试

---

**计划版本**: 1.0  
**创建时间**: 2026-05-10  
**预估总工期**: 13-18周
