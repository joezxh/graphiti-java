#!/bin/bash

# =====================================================
# 法律知识图谱快速搭建脚本
# =====================================================
# 用途: 一键创建完整的法律知识图谱(徐某骥案)
# 预计时间: 5分钟
# 前置条件: 
#   1. Graphiti-Java服务已启动 (http://localhost:8080)
#   2. 已获取认证Token
# =====================================================

set -e  # 遇到错误立即退出

# 配置
BASE_URL="http://localhost:8080/api/v1"
TOKEN="${GRAPHITI_TOKEN:-your-token-here}"  # 从环境变量获取,或使用默认值
GRAPH_ID="legal-kg"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}法律知识图谱快速搭建脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查服务是否可用
echo -e "${YELLOW}[1/8] 检查服务连接...${NC}"
if curl -s "${BASE_URL}/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 服务连接成功${NC}"
else
    echo -e "${RED}✗ 服务连接失败,请确认Graphiti-Java已启动${NC}"
    exit 1
fi
echo ""

# 步骤1: 创建本体定义
echo -e "${YELLOW}[2/8] 创建本体定义...${NC}"
DEFINITION_RESPONSE=$(curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/definition" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "法律知识图谱本体",
    "namespace": "http://legal-ai.cc/ontology",
    "version": "1.0.0",
    "description": "公司解散纠纷领域本体定义"
  }')

if echo "$DEFINITION_RESPONSE" | grep -q '"code":0'; then
    echo -e "${GREEN}✓ 本体定义创建成功${NC}"
else
    echo -e "${RED}✗ 本体定义创建失败${NC}"
    echo "$DEFINITION_RESPONSE"
    exit 1
fi
echo ""

# 步骤2: 创建法律类
echo -e "${YELLOW}[3/8] 创建法律类...${NC}"

# 创建根类: LegalEntity
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/classes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "LegalEntity",
    "classUri": "http://legal-ai.cc/ontology#LegalEntity",
    "description": "法律领域实体的顶层抽象类"
  }' > /dev/null

echo "  ✓ 创建根类: LegalEntity"

# 创建子类: Party
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/classes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "Party",
    "parentClassId": 1,
    "description": "案件中的当事人,包括自然人、法人和非法人组织",
    "example": "徐某骥(原告)、上海某物业管理有限公司(被告)"
  }' > /dev/null

echo "  ✓ 创建子类: Party(当事人)"

# 创建子类: Court
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/classes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "Court",
    "parentClassId": 1,
    "description": "审判机关",
    "example": "上海市长宁区人民法院"
  }' > /dev/null

echo "  ✓ 创建子类: Court(法院)"

# 创建子类: Case
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/classes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "Case",
    "parentClassId": 1,
    "description": "法律诉讼案件",
    "example": "（2022）沪0105民初21387号公司解散纠纷案"
  }' > /dev/null

echo "  ✓ 创建子类: Case(案件)"

# 创建子类: LegalProvision
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/classes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "LegalProvision",
    "parentClassId": 1,
    "description": "法律法规条文",
    "example": "《公司法》第182条"
  }' > /dev/null

echo "  ✓ 创建子类: LegalProvision(法律条文)"
echo -e "${GREEN}✓ 所有法律类创建完成${NC}"
echo ""

# 步骤3: 创建法律属性
echo -e "${YELLOW}[4/8] 创建法律属性...${NC}"

# Party.partyName
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/properties" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "partyName",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人姓名或名称"
  }' > /dev/null

echo "  ✓ 创建属性: Party.partyName"

# Party.partyType
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/properties" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "partyType",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人类型:自然人/法人/非法人组织"
  }' > /dev/null

echo "  ✓ 创建属性: Party.partyType"

# Party.partyRole
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/properties" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "partyRole",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人在案件中的角色:原告/被告/第三人"
  }' > /dev/null

echo "  ✓ 创建属性: Party.partyRole"

# Case.caseNumber
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/properties" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "localName": "caseNumber",
    "propertyType": "DATATYPE",
    "domainClassId": 30,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "案件编号,格式:(年份)法院简称+案件类型+编号"
  }' > /dev/null

echo "  ✓ 创建属性: Case.caseNumber"
echo -e "${GREEN}✓ 所有法律属性创建完成${NC}"
echo ""

# 步骤4: 创建法律约束
echo -e "${YELLOW}[5/8] 创建法律约束...${NC}"

# 当事人类型枚举约束
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/constraints" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": 102,
    "constraintType": "ENUM",
    "value": "{\"allowed_values\": [\"自然人\", \"法人\", \"非法人组织\"]}",
    "errorMessage": "当事人类型必须是:自然人、法人或非法人组织",
    "severity": "ERROR"
  }' > /dev/null

echo "  ✓ 创建约束: 当事人类型枚举"

# 当事人角色枚举约束
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/constraints" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": 103,
    "constraintType": "ENUM",
    "value": "{\"allowed_values\": [\"原告\", \"被告\", \"第三人\", \"上诉人\", \"被上诉人\"]}",
    "errorMessage": "当事人角色必须是:原告、被告、第三人、上诉人或被上诉人",
    "severity": "ERROR"
  }' > /dev/null

echo "  ✓ 创建约束: 当事人角色枚举"

# 案件编号格式约束
curl -s -X POST "${BASE_URL}/ontology/${GRAPH_ID}/constraints" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "propertyId": 201,
    "constraintType": "PATTERN",
    "value": "{\"pattern\": \"^（\\\\d{4}）[\\\\u4e00-\\\\u9fa5]{2,6}\\\\u6c11[\\\\u521d\\\\u7ec8]{1}\\\\d{3,8}号$\"}",
    "errorMessage": "案件编号格式错误,应为:(年份)法院简称+案件类型+编号",
    "severity": "ERROR"
  }' > /dev/null

echo "  ✓ 创建约束: 案件编号格式"
echo -e "${GREEN}✓ 所有法律约束创建完成${NC}"
echo ""

# 步骤5: 创建法律实体
echo -e "${YELLOW}[6/8] 创建法律实体...${NC}"

# 创建当事人: 徐某骥
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/nodes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "徐某骥",
    "type": "Party",
    "partyName": "徐某骥",
    "partyType": "自然人",
    "partyRole": "原告",
    "summary": "公司解散纠纷案原告,持有公司10%股权",
    "valid_at": 1668470400000
  }' > /dev/null

echo "  ✓ 创建实体: 徐某骥(原告)"

# 创建当事人: 上海某物业管理有限公司
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/nodes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "上海某物业管理有限公司",
    "type": "Party",
    "partyName": "上海某物业管理有限公司",
    "partyType": "法人",
    "partyRole": "被告",
    "summary": "公司解散纠纷案被告",
    "valid_at": 1668470400000
  }' > /dev/null

echo "  ✓ 创建实体: 上海某物业管理有限公司(被告)"

# 创建案件: 公司解散纠纷案
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/nodes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "公司解散纠纷案",
    "type": "Case",
    "caseNumber": "（2022）沪0105民初21387号",
    "caseType": "民事案件",
    "summary": "徐某骥诉上海某物业管理有限公司公司解散纠纷案",
    "valid_at": 1668470400000
  }' > /dev/null

echo "  ✓ 创建实体: 公司解散纠纷案"

# 创建法院: 上海市长宁区人民法院
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/nodes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "上海市长宁区人民法院",
    "type": "Court",
    "courtName": "上海市长宁区人民法院",
    "courtLevel": "基层人民法院",
    "summary": "本案一审法院",
    "valid_at": 1668470400000
  }' > /dev/null

echo "  ✓ 创建实体: 上海市长宁区人民法院"

# 创建法律条文: 公司法第182条
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/nodes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "公司法第182条",
    "type": "LegalProvision",
    "lawName": "中华人民共和国公司法",
    "articleNumber": "第182条",
    "summary": "公司经营管理发生严重困难,继续存续会使股东利益受到重大损失",
    "valid_at": 1668470400000
  }' > /dev/null

echo "  ✓ 创建实体: 公司法第182条"
echo -e "${GREEN}✓ 所有法律实体创建完成${NC}"
echo ""

# 步骤6: 创建法律关系
echo -e "${YELLOW}[7/8] 创建法律关系...${NC}"

# 徐某骥 CASE_PARTY 公司解散纠纷案
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/edges" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceUuid": "party-001",
    "targetUuid": "case-001",
    "type": "CASE_PARTY",
    "fact": "徐某骥作为原告提起公司解散纠纷诉讼",
    "role": "原告",
    "valid_at": 1668470400000,
    "invalid_at": null
  }' > /dev/null

echo "  ✓ 创建关系: 徐某骥 → CASE_PARTY → 公司解散纠纷案"

# 公司解散纠纷案 CASE_COURT 上海市长宁区人民法院
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/edges" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceUuid": "case-001",
    "targetUuid": "court-002",
    "type": "CASE_COURT",
    "fact": "上海市长宁区人民法院审理此案",
    "courtRole": "一审法院",
    "valid_at": 1668470400000,
    "invalid_at": null
  }' > /dev/null

echo "  ✓ 创建关系: 公司解散纠纷案 → CASE_COURT → 上海市长宁区人民法院"

# 公司解散纠纷案 CASE_LEGAL_BASIS 公司法第182条
curl -s -X POST "${BASE_URL}/graph/${GRAPH_ID}/edges" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceUuid": "case-001",
    "targetUuid": "law-001",
    "type": "CASE_LEGAL_BASIS",
    "fact": "案件适用《中华人民共和国公司法》第182条",
    "applicableProvision": "《公司法》第182条",
    "valid_at": 1668470400000,
    "invalid_at": null
  }' > /dev/null

echo "  ✓ 创建关系: 公司解散纠纷案 → CASE_LEGAL_BASIS → 公司法第182条"
echo -e "${GREEN}✓ 所有法律关系创建完成${NC}"
echo ""

# 步骤7: 验证数据
echo -e "${YELLOW}[8/8] 验证知识图谱...${NC}"

# 查询案件当事人
PARTIES_RESPONSE=$(curl -s -X GET "${BASE_URL}/graph/${GRAPH_ID}/nodes?relatedTo=case-001&relationType=CASE_PARTY" \
  -H "Authorization: Bearer ${TOKEN}")

PARTIES_COUNT=$(echo "$PARTIES_RESPONSE" | grep -o '"uuid"' | wc -l)
echo "  ✓ 案件当事人数量: ${PARTIES_COUNT}"

# 查询案件适用法律
LAWS_RESPONSE=$(curl -s -X GET "${BASE_URL}/graph/${GRAPH_ID}/nodes?relatedTo=case-001&relationType=CASE_LEGAL_BASIS" \
  -H "Authorization: Bearer ${TOKEN}")

LAWS_COUNT=$(echo "$LAWS_RESPONSE" | grep -o '"uuid"' | wc -l)
echo "  ✓ 案件适用法律数量: ${LAWS_COUNT}"

echo -e "${GREEN}✓ 知识图谱验证完成${NC}"
echo ""

# 完成
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ 法律知识图谱搭建完成!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "📊 知识图谱统计:"
echo "  - 法律类: 5个 (LegalEntity, Party, Court, Case, LegalProvision)"
echo "  - 法律属性: 4个 (partyName, partyType, partyRole, caseNumber)"
echo "  - 法律约束: 3个 (当事人类型, 当事人角色, 案件编号格式)"
echo "  - 法律实体: 5个 (徐某骥, 上海某物业管理有限公司, 公司解散纠纷案, 上海市长宁区人民法院, 公司法第182条)"
echo "  - 法律关系: 3个 (CASE_PARTY, CASE_COURT, CASE_LEGAL_BASIS)"
echo ""
echo "🎯 下一步:"
echo "  1. 查看完整培训文档: docs/training/ontology-training-guide.md"
echo "  2. 查看快速入门指南: docs/training/legal-kg-quickstart.md"
echo "  3. 执行AI法律分析: 参考快速入门指南第5章"
echo ""
echo "🔗 API地址: ${BASE_URL}"
echo ""
