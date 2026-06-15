# 法律知识图谱搭建脚本

## 📁 脚本说明

本目录包含用于快速搭建和导入法律知识图谱的自动化脚本。

### 可用脚本

| 脚本 | 适用平台 | 说明 |
|------|---------|------|
| `setup-legal-kg.sh` | Linux/macOS | Bash自动搭建脚本 |
| `setup-legal-kg.ps1` | Windows | PowerShell自动搭建脚本 |
| `import-legal-data.sh` | Linux/macOS | Bash批量导入脚本 |
| `import-legal-data.ps1` | Windows | PowerShell批量导入脚本 |

### 数据文件

| 文件 | 说明 |
|------|------|
| `data/legal-kg-sample-data.json` | 徐某骥案完整示例数据(9节点+8关系) |

---

## 🚀 快速开始

### 前置条件

1. **OntoGraph服务已启动**
   ```bash
   cd ontograph-java
   mvn spring-boot:run
   ```

2. **获取认证Token**
   - 从系统管理员获取
   - 或查看API文档获取Token

### 使用方法

#### Linux/macOS (Bash)

```bash
# 方式1: 设置环境变量
export GRAPHITI_TOKEN="your-token-here"
chmod +x scripts/setup-legal-kg.sh
./scripts/setup-legal-kg.sh

# 方式2: 直接传入Token
GRAPHITI_TOKEN="your-token-here" ./scripts/setup-legal-kg.sh
```

#### Windows (PowerShell)

```powershell
# 方式1: 设置环境变量
$env:GRAPHITI_TOKEN = "your-token-here"
.\scripts\setup-legal-kg.ps1

# 方式2: 直接运行(使用默认Token)
.\scripts\setup-legal-kg.ps1
```

---

## 📊 脚本执行内容

脚本会自动完成以下步骤:

1. ✅ 检查服务连接
2. ✅ 创建本体定义
3. ✅ 创建5个法律类
   - LegalEntity (根类)
   - Party (当事人)
   - Court (法院)
   - Case (案件)
   - LegalProvision (法律条文)
4. ✅ 创建4个法律属性
   - partyName (当事人姓名)
   - partyType (当事人类型)
   - partyRole (当事人角色)
   - caseNumber (案件编号)
5. ✅ 创建法律实体 (徐某骥案)
   - 徐某骥 (原告)
   - 上海某物业管理有限公司 (被告)
   - 公司解散纠纷案
   - 上海市长宁区人民法院
   - 公司法第182条
6. ✅ 创建3个法律关系
   - CASE_PARTY (当事人关系)
   - CASE_COURT (法院关系)
   - CASE_LEGAL_BASIS (法律依据关系)
7. ✅ 验证知识图谱数据

**预计执行时间**: 5分钟

---

## 🎯 执行结果

脚本执行成功后,会创建完整的法律知识图谱:

```
========================================
✓ 法律知识图谱搭建完成!
========================================

📊 知识图谱统计:
  - 法律类: 5个 (LegalEntity, Party, Court, Case, LegalProvision)
  - 法律属性: 4个 (partyName, partyType, partyRole, caseNumber)
  - 法律实体: 5个 (徐某骥, 上海某物业管理有限公司, 公司解散纠纷案, 上海市长宁区人民法院, 公司法第182条)
  - 法律关系: 3个 (CASE_PARTY, CASE_COURT, CASE_LEGAL_BASIS)

🎯 下一步:
  1. 查看完整培训文档: docs/training/ontology-training-guide.md
  2. 查看快速入门指南: docs/training/legal-kg-quickstart.md
  3. 执行AI法律分析: 参考快速入门指南第5章

🔗 API地址: http://localhost:8080/api/v1
```

---

## 🔧 自定义配置

### 修改API地址

如果服务不在默认地址,可以修改脚本中的`BASE_URL`:

```bash
# Bash
BASE_URL="http://your-server:8080/api/v1"

# PowerShell
$BASE_URL = "http://your-server:8080/api/v1"
```

### 修改Graph ID

如果要创建不同的知识图谱实例,修改`GRAPH_ID`:

```bash
# Bash
GRAPH_ID="legal-kg-prod"

# PowerShell
$GRAPH_ID = "legal-kg-prod"
```

---

## ⚠️ 注意事项

1. **权限要求**: 需要有效的认证Token
2. **服务可用性**: 确保OntoGraph服务已启动
3. **幂等性**: 脚本可以重复执行,但会产生重复数据
4. **错误处理**: 脚本遇到错误会立即停止并显示错误信息

---

## 🐛 故障排查

### 问题1: 服务连接失败

```
✗ 服务连接失败,请确认OntoGraph已启动
```

**解决方案**:
```bash
# 检查服务状态
curl http://localhost:8080/api/v1/health

# 启动服务
mvn spring-boot:run
```

### 问题2: 认证失败

```
✗ 本体定义创建失败
```

**解决方案**:
- 确认Token正确
- 检查Token是否过期
- 确认有创建本体的权限

### 问题3: PowerShell执行策略限制

```
无法加载文件 setup-legal-kg.ps1，因为在此系统上禁止运行脚本。
```

**解决方案**:
```powershell
# 临时允许执行脚本
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## 📚 相关文档

- **完整培训文档**: `docs/training/ontology-training-guide.md`
- **快速入门指南**: `docs/training/legal-kg-quickstart.md`
- **API文档**: `docs/api/`
- **项目README**: `README.md`

---

## 📦 批量导入数据

### 使用示例数据

项目提供了完整的徐某骥案示例数据,包含9个实体节点和8个关系边。

#### Linux/macOS

```bash
# 导入示例数据
chmod +x scripts/import-legal-data.sh
./scripts/import-legal-data.sh data/legal-kg-sample-data.json
```

#### Windows (PowerShell)

```powershell
# 导入示例数据
.\scripts\import-legal-data.ps1 data\legal-kg-sample-data.json
```

### 创建自己的数据文件

参考 `data/legal-kg-sample-data.json` 的格式创建自己的数据文件:

```json
{
  "graphId": "legal-kg",
  "nodes": [
    {
      "name": "实体名称",
      "type": "实体类型",
      "属性1": "值1",
      "属性2": "值2",
      "valid_at": 时间戳
    }
  ],
  "edges": [
    {
      "sourceName": "源实体名称",
      "targetName": "目标实体名称",
      "type": "关系类型",
      "fact": "关系描述",
      "valid_at": 时间戳
    }
  ]
}
```

### 批量导入多个案例

创建包含多个案例的JSON文件:

```json
{
  "graphId": "legal-kg",
  "nodes": [
    // 案例1: 徐某骥案
    {"name": "徐某骥", "type": "Party", ...},
    {"name": "公司解散纠纷案", "type": "Case", ...},
    
    // 案例2: 李某案
    {"name": "李某", "type": "Party", ...},
    {"name": "劳动争议案", "type": "Case", ...}
  ],
  "edges": [
    // 案例1关系
    {"sourceName": "徐某骥", "targetName": "公司解散纠纷案", ...},
    
    // 案例2关系
    {"sourceName": "李某", "targetName": "劳动争议案", ...}
  ]
}
```

然后执行导入:

```bash
./scripts/import-legal-data.sh data/multi-cases-data.json
```

---

## 💡 高级用法

### 批量导入更多法律数据

脚本执行后,可以使用批量导入API导入更多案例:

```bash
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/import' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '@legal-data-batch.json'
```

### 执行社区检测

```bash
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/communities/detect' \
  -H 'Authorization: Bearer <token>'
```

### 时序查询

```bash
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/temporal/query' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "queryTime": "2022-11-15T00:00:00Z",
    "centerNode": "case-001",
    "maxDepth": 2
  }'
```

---

**祝你使用愉快!** 🎉
