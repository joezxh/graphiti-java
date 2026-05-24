# =====================================================
# 法律知识图谱快速搭建脚本 (PowerShell版本)
# =====================================================
# 用途: 一键创建完整的法律知识图谱(徐某骥案)
# 预计时间: 5分钟
# 前置条件: 
#   1. Graphiti-Java服务已启动 (http://localhost:8080)
#   2. 已获取认证Token
# =====================================================

$ErrorActionPreference = "Stop"

# 配置
$BASE_URL = "http://localhost:8080/api/v1"
$TOKEN = if ($env:GRAPHITI_TOKEN) { $env:GRAPHITI_TOKEN } else { "your-token-here" }
$GRAPH_ID = "legal-kg"

Write-Host "========================================" -ForegroundColor Green
Write-Host "法律知识图谱快速搭建脚本 (PowerShell)" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 步骤1: 检查服务连接
Write-Host "[1/8] 检查服务连接..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/health" -Method Get -ErrorAction Stop
    Write-Host "✓ 服务连接成功" -ForegroundColor Green
} catch {
    Write-Host "✗ 服务连接失败,请确认Graphiti-Java已启动" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 步骤2: 创建本体定义
Write-Host "[2/8] 创建本体定义..." -ForegroundColor Yellow
$definitionBody = @{
    name = "法律知识图谱本体"
    namespace = "http://legal-ai.cc/ontology"
    version = "1.0.0"
    description = "公司解散纠纷领域本体定义"
} | ConvertTo-Json

$definitionResponse = Invoke-RestMethod -Uri "$BASE_URL/ontology/$GRAPH_ID/definition" `
    -Method Post `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
        "Content-Type" = "application/json"
    } `
    -Body $definitionBody

if ($definitionResponse.code -eq 0) {
    Write-Host "✓ 本体定义创建成功" -ForegroundColor Green
} else {
    Write-Host "✗ 本体定义创建失败" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 步骤3: 创建法律类
Write-Host "[3/8] 创建法律类..." -ForegroundColor Yellow

$classes = @(
    @{
        localName = "LegalEntity"
        classUri = "http://legal-ai.cc/ontology#LegalEntity"
        description = "法律领域实体的顶层抽象类"
    },
    @{
        localName = "Party"
        parentClassId = 1
        description = "案件中的当事人,包括自然人、法人和非法人组织"
        example = "徐某骥(原告)、上海某物业管理有限公司(被告)"
    },
    @{
        localName = "Court"
        parentClassId = 1
        description = "审判机关"
        example = "上海市长宁区人民法院"
    },
    @{
        localName = "Case"
        parentClassId = 1
        description = "法律诉讼案件"
        example = "（2022）沪0105民初21387号公司解散纠纷案"
    },
    @{
        localName = "LegalProvision"
        parentClassId = 1
        description = "法律法规条文"
        example = "《公司法》第182条"
    }
)

foreach ($class in $classes) {
    $body = $class | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$BASE_URL/ontology/$GRAPH_ID/classes" `
        -Method Post `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $body | Out-Null
    Write-Host "  ✓ 创建类: $($class.localName)"
}

Write-Host "✓ 所有法律类创建完成" -ForegroundColor Green
Write-Host ""

# 步骤4: 创建法律属性
Write-Host "[4/8] 创建法律属性..." -ForegroundColor Yellow

$properties = @(
    @{
        localName = "partyName"
        propertyType = "DATATYPE"
        domainClassId = 10
        rangeDataType = "string"
        isRequired = $true
        description = "当事人姓名或名称"
    },
    @{
        localName = "partyType"
        propertyType = "DATATYPE"
        domainClassId = 10
        rangeDataType = "string"
        isRequired = $true
        description = "当事人类型:自然人/法人/非法人组织"
    },
    @{
        localName = "partyRole"
        propertyType = "DATATYPE"
        domainClassId = 10
        rangeDataType = "string"
        isRequired = $true
        description = "当事人在案件中的角色:原告/被告/第三人"
    },
    @{
        localName = "caseNumber"
        propertyType = "DATATYPE"
        domainClassId = 30
        rangeDataType = "string"
        isRequired = $true
        description = "案件编号,格式:(年份)法院简称+案件类型+编号"
    }
)

foreach ($prop in $properties) {
    $body = $prop | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$BASE_URL/ontology/$GRAPH_ID/properties" `
        -Method Post `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $body | Out-Null
    Write-Host "  ✓ 创建属性: $($prop.localName)"
}

Write-Host "✓ 所有法律属性创建完成" -ForegroundColor Green
Write-Host ""

# 步骤5: 创建法律实体
Write-Host "[5/8] 创建法律实体..." -ForegroundColor Yellow

$nodes = @(
    @{
        name = "徐某骥"
        type = "Party"
        partyName = "徐某骥"
        partyType = "自然人"
        partyRole = "原告"
        summary = "公司解散纠纷案原告,持有公司10%股权"
        valid_at = 1668470400000
    },
    @{
        name = "上海某物业管理有限公司"
        type = "Party"
        partyName = "上海某物业管理有限公司"
        partyType = "法人"
        partyRole = "被告"
        summary = "公司解散纠纷案被告"
        valid_at = 1668470400000
    },
    @{
        name = "公司解散纠纷案"
        type = "Case"
        caseNumber = "（2022）沪0105民初21387号"
        caseType = "民事案件"
        summary = "徐某骥诉上海某物业管理有限公司公司解散纠纷案"
        valid_at = 1668470400000
    },
    @{
        name = "上海市长宁区人民法院"
        type = "Court"
        courtName = "上海市长宁区人民法院"
        courtLevel = "基层人民法院"
        summary = "本案一审法院"
        valid_at = 1668470400000
    },
    @{
        name = "公司法第182条"
        type = "LegalProvision"
        lawName = "中华人民共和国公司法"
        articleNumber = "第182条"
        summary = "公司经营管理发生严重困难,继续存续会使股东利益受到重大损失"
        valid_at = 1668470400000
    }
)

foreach ($node in $nodes) {
    $body = $node | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$BASE_URL/graph/$GRAPH_ID/nodes" `
        -Method Post `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $body | Out-Null
    Write-Host "  ✓ 创建实体: $($node.name)"
}

Write-Host "✓ 所有法律实体创建完成" -ForegroundColor Green
Write-Host ""

# 步骤6: 创建法律关系
Write-Host "[6/8] 创建法律关系..." -ForegroundColor Yellow

$edges = @(
    @{
        sourceUuid = "party-001"
        targetUuid = "case-001"
        type = "CASE_PARTY"
        fact = "徐某骥作为原告提起公司解散纠纷诉讼"
        role = "原告"
        valid_at = 1668470400000
        invalid_at = $null
    },
    @{
        sourceUuid = "case-001"
        targetUuid = "court-002"
        type = "CASE_COURT"
        fact = "上海市长宁区人民法院审理此案"
        courtRole = "一审法院"
        valid_at = 1668470400000
        invalid_at = $null
    },
    @{
        sourceUuid = "case-001"
        targetUuid = "law-001"
        type = "CASE_LEGAL_BASIS"
        fact = "案件适用《中华人民共和国公司法》第182条"
        applicableProvision = "《公司法》第182条"
        valid_at = 1668470400000
        invalid_at = $null
    }
)

foreach ($edge in $edges) {
    $body = $edge | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$BASE_URL/graph/$GRAPH_ID/edges" `
        -Method Post `
        -Headers @{
            "Authorization" = "Bearer $TOKEN"
            "Content-Type" = "application/json"
        } `
        -Body $body | Out-Null
    Write-Host "  ✓ 创建关系: $($edge.type)"
}

Write-Host "✓ 所有法律关系创建完成" -ForegroundColor Green
Write-Host ""

# 步骤7: 验证数据
Write-Host "[7/8] 验证知识图谱..." -ForegroundColor Yellow

# 查询案件当事人
$partiesResponse = Invoke-RestMethod -Uri "$BASE_URL/graph/$GRAPH_ID/nodes?relatedTo=case-001&relationType=CASE_PARTY" `
    -Method Get `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
    }

$partiesCount = $partiesResponse.data.nodes.Count
Write-Host "  ✓ 案件当事人数量: $partiesCount"

# 查询案件适用法律
$lawsResponse = Invoke-RestMethod -Uri "$BASE_URL/graph/$GRAPH_ID/nodes?relatedTo=case-001&relationType=CASE_LEGAL_BASIS" `
    -Method Get `
    -Headers @{
        "Authorization" = "Bearer $TOKEN"
    }

$lawsCount = $lawsResponse.data.nodes.Count
Write-Host "  ✓ 案件适用法律数量: $lawsCount"

Write-Host "✓ 知识图谱验证完成" -ForegroundColor Green
Write-Host ""

# 完成
Write-Host "========================================" -ForegroundColor Green
Write-Host "✓ 法律知识图谱搭建完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📊 知识图谱统计:" -ForegroundColor Cyan
Write-Host "  - 法律类: 5个 (LegalEntity, Party, Court, Case, LegalProvision)"
Write-Host "  - 法律属性: 4个 (partyName, partyType, partyRole, caseNumber)"
Write-Host "  - 法律实体: 5个 (徐某骥, 上海某物业管理有限公司, 公司解散纠纷案, 上海市长宁区人民法院, 公司法第182条)"
Write-Host "  - 法律关系: 3个 (CASE_PARTY, CASE_COURT, CASE_LEGAL_BASIS)"
Write-Host ""
Write-Host "🎯 下一步:" -ForegroundColor Cyan
Write-Host "  1. 查看完整培训文档: docs/training/ontology-training-guide.md"
Write-Host "  2. 查看快速入门指南: docs/training/legal-kg-quickstart.md"
Write-Host "  3. 执行AI法律分析: 参考快速入门指南第5章"
Write-Host ""
Write-Host "🔗 API地址: $BASE_URL" -ForegroundColor Cyan
Write-Host ""
