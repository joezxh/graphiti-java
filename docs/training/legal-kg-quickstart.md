# 法律知识图谱快速入门指南

> **适用对象**: 法律知识图谱开发新手  
> **预计时间**: 30分钟  
> **前置知识**: 基础编程能力,了解JSON格式  
> **主线案例**: 徐某骥与上海某物业管理有限公司公司解散纠纷案

---

## 📋 目录

1. [环境准备](#1-环境准备)
2. [创建法律本体](#2-创建法律本体)
3. [导入法律数据](#3-导入法律数据)
4. [查询法律知识图谱](#4-查询法律知识图谱)
5. [AI法律分析](#5-ai法律分析)
6. [下一步学习](#6-下一步学习)

---

## 1. 环境准备

### 1.1 系统要求

- Java 21+
- PostgreSQL 14+
- Neo4j 5.x
- Spring Boot 3.x

### 1.2 启动服务

```bash
# 启动Graphiti-Java服务
cd graphiti-java
mvn spring-boot:run

# 服务启动后访问
# API地址: http://localhost:8080/api/v1
```

---

## 2. 创建法律本体

### 2.1 创建本体定义

```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/definition' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "法律知识图谱本体",
    "namespace": "http://legal-ai.cc/ontology",
    "version": "1.0.0",
    "description": "公司解散纠纷领域本体定义"
  }'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "graphId": "legal-kg",
    "name": "法律知识图谱本体",
    "status": "ACTIVE"
  }
}
```

### 2.2 创建法律类

```bash
# 1. 创建根类: LegalEntity
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "LegalEntity",
    "classUri": "http://legal-ai.cc/ontology#LegalEntity",
    "description": "法律领域实体的顶层抽象类"
  }'

# 2. 创建子类: Party(当事人)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Party",
    "parentClassId": 1,
    "description": "案件中的当事人,包括自然人、法人和非法人组织",
    "example": "徐某骥(原告)、上海某物业管理有限公司(被告)"
  }'

# 3. 创建子类: Court(法院)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Court",
    "parentClassId": 1,
    "description": "审判机关",
    "example": "上海市长宁区人民法院"
  }'

# 4. 创建子类: Case(案件)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Case",
    "parentClassId": 1,
    "description": "法律诉讼案件",
    "example": "（2022）沪0105民初21387号公司解散纠纷案"
  }'

# 5. 创建子类: LegalProvision(法律条文)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "LegalProvision",
    "parentClassId": 1,
    "description": "法律法规条文",
    "example": "《公司法》第182条"
  }'
```

### 2.3 创建法律属性

```bash
# Party.partyName (当事人姓名)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "partyName",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人姓名或名称"
  }'

# Party.partyType (当事人类型)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "partyType",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人类型:自然人/法人/非法人组织"
  }'

# Party.partyRole (当事人角色)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "partyRole",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人在案件中的角色:原告/被告/第三人"
  }'

# Case.caseNumber (案件编号)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "caseNumber",
    "propertyType": "DATATYPE",
    "domainClassId": 30,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "案件编号,格式:(年份)法院简称+案件类型+编号"
  }'
```

### 2.4 创建法律约束

```bash
# 当事人类型枚举约束
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/constraints' \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId": 102,
    "constraintType": "ENUM",
    "value": "{\"allowed_values\": [\"自然人\", \"法人\", \"非法人组织\"]}",
    "errorMessage": "当事人类型必须是:自然人、法人或非法人组织",
    "severity": "ERROR"
  }'

# 当事人角色枚举约束
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/constraints' \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId": 103,
    "constraintType": "ENUM",
    "value": "{\"allowed_values\": [\"原告\", \"被告\", \"第三人\", \"上诉人\", \"被上诉人\"]}",
    "errorMessage": "当事人角色必须是:原告、被告、第三人、上诉人或被上诉人",
    "severity": "ERROR"
  }'

# 案件编号格式约束
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/constraints' \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId": 201,
    "constraintType": "PATTERN",
    "value": "{\"pattern\": \"^（\\\\d{4}）[\\\\u4e00-\\\\u9fa5]{2,6}\\\\u6c11[\\\\u521d\\\\u7ec8]{1}\\\\d{3,8}号$\"}",
    "errorMessage": "案件编号格式错误,应为:(年份)法院简称+案件类型+编号",
    "severity": "ERROR"
  }'
```

---

## 3. 导入法律数据

### 3.1 创建法律实体

```bash
# 创建当事人: 徐某骥
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "徐某骥",
    "type": "Party",
    "partyName": "徐某骥",
    "partyType": "自然人",
    "partyRole": "原告",
    "summary": "公司解散纠纷案原告,持有公司10%股权",
    "valid_at": 1668470400000
  }'

# 创建当事人: 上海某物业管理有限公司
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "上海某物业管理有限公司",
    "type": "Party",
    "partyName": "上海某物业管理有限公司",
    "partyType": "法人",
    "partyRole": "被告",
    "summary": "公司解散纠纷案被告",
    "valid_at": 1668470400000
  }'

# 创建案件: 公司解散纠纷案
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "公司解散纠纷案",
    "type": "Case",
    "caseNumber": "（2022）沪0105民初21387号",
    "caseType": "民事案件",
    "summary": "徐某骥诉上海某物业管理有限公司公司解散纠纷案",
    "valid_at": 1668470400000
  }'

# 创建法院: 上海市长宁区人民法院
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "上海市长宁区人民法院",
    "type": "Court",
    "courtName": "上海市长宁区人民法院",
    "courtLevel": "基层人民法院",
    "summary": "本案一审法院",
    "valid_at": 1668470400000
  }'

# 创建法律条文: 公司法第182条
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "公司法第182条",
    "type": "LegalProvision",
    "lawName": "中华人民共和国公司法",
    "articleNumber": "第182条",
    "summary": "公司经营管理发生严重困难,继续存续会使股东利益受到重大损失",
    "valid_at": 1668470400000
  }'
```

### 3.2 创建法律关系

```bash
# 徐某骥 CASE_PARTY 公司解散纠纷案
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/edges' \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUuid": "party-001",
    "targetUuid": "case-001",
    "type": "CASE_PARTY",
    "fact": "徐某骥作为原告提起公司解散纠纷诉讼",
    "role": "原告",
    "valid_at": 1668470400000,
    "invalid_at": null
  }'

# 公司解散纠纷案 CASE_COURT 上海市长宁区人民法院
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/edges' \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUuid": "case-001",
    "targetUuid": "court-002",
    "type": "CASE_COURT",
    "fact": "上海市长宁区人民法院审理此案",
    "courtRole": "一审法院",
    "valid_at": 1668470400000,
    "invalid_at": null
  }'

# 公司解散纠纷案 CASE_LEGAL_BASIS 公司法第182条
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/edges' \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUuid": "case-001",
    "targetUuid": "law-001",
    "type": "CASE_LEGAL_BASIS",
    "fact": "案件适用《中华人民共和国公司法》第182条",
    "applicableProvision": "《公司法》第182条",
    "valid_at": 1668470400000,
    "invalid_at": null
  }'
```

---

## 4. 查询法律知识图谱

### 4.1 查询案件当事人

```bash
# 查询公司解散纠纷案的所有当事人
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/nodes?relatedTo=case-001&relationType=CASE_PARTY'
```

**响应示例**:
```json
{
  "code": 0,
  "data": {
    "nodes": [
      {
        "uuid": "party-001",
        "name": "徐某骥",
        "type": "Party",
        "partyRole": "原告",
        "partyType": "自然人"
      },
      {
        "uuid": "party-002",
        "name": "上海某物业管理有限公司",
        "type": "Party",
        "partyRole": "被告",
        "partyType": "法人"
      }
    ]
  }
}
```

### 4.2 查询案件适用法律

```bash
# 查询案件适用的法律条文
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/nodes?relatedTo=case-001&relationType=CASE_LEGAL_BASIS'
```

### 4.3 时序查询

```bash
# 查询2022年11月15日(立案时)的案件状态
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/temporal/query' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "queryTime": "2022-11-15T00:00:00Z",
    "centerNode": "case-001",
    "maxDepth": 2
  }'
```

---

## 5. AI法律分析

### 5.1 提取上下文

```bash
# 查询与徐某骥相关的所有实体
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/nodes?relatedTo=party-001'

# 查询徐某骥所属的社区
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/communities?memberUuid=party-001'
```

### 5.2 组装AI上下文

```python
# 上下文组装示例
context = {
    "case_info": {
        "name": "徐某骥与上海某物业管理有限公司公司解散纠纷案",
        "caseNumber": "（2022）沪0105民初21387号",
        "type": "公司解散纠纷"
    },
    "related_entities": [
        {"name": "徐某骥", "type": "Party", "partyRole": "原告"},
        {"name": "上海某物业管理有限公司", "type": "Party", "partyRole": "被告"},
        {"name": "上海市长宁区人民法院", "type": "Court"},
        {"name": "《公司法》第182条", "type": "LegalProvision"}
    ],
    "timeline": [
        {"date": "2022-11-15", "event": "徐某骥提起公司解散纠纷诉讼"},
        {"date": "2023-10-24", "event": "一审法院判决"},
        {"date": "2023-11-10", "event": "徐某骥提起上诉"},
        {"date": "2024-03-15", "event": "二审法院判决"}
    ]
}
```

### 5.3 调用LLM分析

```python
# 构建LLM提示词
prompt = f"""
你是一位资深公司法律师,请根据以下上下文信息,为徐某骥提供法律意见:

## 案件信息
{context['case_info']}

## 相关实体
{context['related_entities']}

## 时间线
{context['timeline']}

请分析:
1. 徐某骥的诉求是否合理?
2. 是否符合《公司法》第182条的解散条件?
3. 诉讼策略建议
"""

# 调用LLM
response = llm.generate(prompt, max_tokens=2000)
print(response)
```

---

## 6. 下一步学习

### 6.1 深入学习路径

1. **完整培训文档**: 阅读 `docs/training/ontology-training-guide.md`
   - 第二章: 本体论基本概念
   - 第三章: 本体核心要素详解
   - 第四章: 知识图谱核心概念
   - 第十章: 上下文工程应用

2. **API参考文档**: 查看 `docs/api/` 目录
   - 完整API列表
   - 请求/响应格式
   - 错误码说明

3. **示例代码**: 参考 `graphiti-module-core/src/test/`
   - 单元测试示例
   - 集成测试示例

### 6.2 实践项目

**建议实践项目**:

1. **公司解散纠纷知识图谱** (1周)
   - 导入10个真实案例
   - 构建完整的法律关系网络
   - 实现类案检索功能

2. **劳动合同法律知识图谱** (1周)
   - 导入劳动合同法相关法条
   - 导入劳动争议案例
   - 实现法律推荐功能

3. **法律知识图谱AI助手** (2周)
   - 集成LLM进行法律分析
   - 实现智能问答
   - 生成法律意见书

### 6.3 常见问题

**Q1: 如何批量导入法律数据?**
```bash
# 使用批量导入API
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/import' \
  -H 'Content-Type: application/json' \
  -d '@legal-data.json'
```

**Q2: 如何验证数据是否符合本体?**
```bash
# 使用本体验证API
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/validate/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "nodes": [...],
    "edges": [...]
  }'
```

**Q3: 如何执行社区检测?**
```bash
# 触发社区检测
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/communities/detect'
```

---

## 📚 参考资源

- **完整培训文档**: `docs/training/ontology-training-guide.md`
- **知识图谱关系文档**: `docs/knowledge-graph-relationships.md`
- **API文档**: `docs/api/`
- **数据库设计**: `docs/manual/数据库设计/`
- **部署指南**: `docs/manual/部署运维/`

---

**祝你学习愉快!** 🎉

如有问题,请参考完整培训文档或联系开发团队。
