# Qoder 自动化流程快速入门

> **Getting Started with Qoder-Powered Automated Pipeline**

**文档版本**: v1.0.0  
**适用项目**: OntoGraph (graphiti-java)  
**更新时间**: 2026-06-16

---

## 目录

- [1. 环境准备](#1-环境准备)
- [2. 快速开始](#2-快速开始)
- [3. 核心工作流](#3-核心工作流)
- [4. 常用命令](#4-常用命令)
- [5. 故障排查](#5-故障排查)
- [6. 最佳实践](#6-最佳实践)

---

## 1. 环境准备

### 1.1 必需工具

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| Java | 17+ | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端开发 |
| Python | 3.8+ | 自动化脚本 |
| Git | 2.30+ | 版本控制 |
| Qoder | 最新版 | AI 辅助开发 |

### 1.2 安装验证

```bash
# 检查所有工具
java -version
mvn -version
node -v
python --version
git --version
```

### 1.3 Qoder 配置

确保 Qoder 已安装并配置好以下 Skills：

- ✅ `brainstorming` - 需求分析
- ✅ `spec` - 规格生成
- ✅ `writing-plans` - 实现计划
- ✅ `test-driven-development` - 测试生成
- ✅ `subagent-driven-development` - 并行开发
- ✅ `qa` / `qa_only` - 自动化测试
- ✅ `requesting-code-review` - 代码审查

---

## 2. 快速开始

### 2.1 运行完整流程

**Windows (PowerShell)**:
```powershell
.\scripts\run-automated-pipeline.ps1
```

**Linux/macOS (Bash)**:
```bash
bash scripts/run-automated-pipeline.sh
```

### 2.2 运行单个阶段

```bash
# 仅验证需求规格
python scripts/validate-specs.py \
  --specs-dir docs/superpowers/specs \
  --output reports/spec-validation.json

# 仅运行后端测试
cd ontograph-backend && mvn test

# 仅运行前端测试
cd ontograph-frontend && npx playwright test
```

---

## 3. 核心工作流

### 3.1 工作流 1: 从需求到实现

```
用户需求 → Qoder 分析 → 生成规格 → 生成计划 → 并行开发 → 测试验证 → 部署
```

**示例**:

```bash
# Step 1: 用户在 Qoder 中描述需求
"我需要添加一个数据导出功能，支持 Excel 和 PDF 格式"

# Step 2: Qoder 自动触发 brainstorming
# 输出: 技术方案对比、风险分析

# Step 3: Qoder 生成规格文档
# 文件: docs/superpowers/specs/2026-06-16-data-export-design.md

# Step 4: Qoder 生成实现计划
# 文件: docs/superpowers/plans/2026-06-16-data-export-plan.md

# Step 5: 启动并行开发
skill: "subagent-driven-development"

# Step 6: 自动测试验证
skill: "qa"

# Step 7: 部署到测试环境
bash scripts/auto-deploy.sh
```

### 3.2 工作流 2: 测试驱动开发

```
规格文档 → 生成测试 → 编写代码 → 运行测试 → 修复问题 → 提交代码
```

**示例**:

```bash
# Step 1: 读取规格文档
# 文件: docs/superpowers/specs/2026-06-16-rbac-design.md

# Step 2: 生成测试用例
skill: "test-driven-development"

# 生成文件:
# - ontograph-backend/src/test/java/.../RbacServiceTest.java
# - ontograph-frontend/tests/rbac.spec.ts

# Step 3: 运行测试 (会失败，因为还没实现)
cd ontograph-backend && mvn test

# Step 4: 实现功能
skill: "subagent-driven-development"

# Step 5: 再次运行测试 (应该通过)
cd ontograph-backend && mvn test

# Step 6: 提交代码
git add .
git commit -m "feat: 实现 RBAC 权限管理模块"
git push origin feature/rbac
```

### 3.3 工作流 3: CI/CD 集成

```
代码推送 → 云效触发 → 自动构建 → 自动测试 → 自动部署 → 通知团队
```

**云效流水线配置**:

参考 `.yunxiao/flow.yml`，包含 7 个阶段：
1. 代码质量检查
2. 单元测试
3. AI 前端测试
4. AI API 测试
5. AI 回归测试
6. 测试报告与通知
7. 部署到测试环境

---

## 4. 常用命令

### 4.1 需求分析

```bash
# 在 Qoder 对话中使用
skill: "brainstorming"  # 分析需求
skill: "spec"           # 生成规格
skill: "writing-plans"  # 生成计划
```

### 4.2 测试相关

```bash
# 生成测试用例
skill: "test-driven-development"

# 运行后端测试
cd ontograph-backend && mvn test

# 运行前端测试
cd ontograph-frontend && npx playwright test

# QA 自动化测试
skill: "qa"
skill: "qa_only"
```

### 4.3 开发相关

```bash
# 并行开发
skill: "subagent-driven-development"

# 代码审查
skill: "requesting-code-review"
skill: "chinese-code-review"

# 验证完成
skill: "verification-before-completion"
```

### 4.4 构建部署

```bash
# 运行完整流程
bash scripts/run-automated-pipeline.sh

# 仅构建
mvn clean package -DskipTests
cd ontograph-frontend && npm run build

# 部署
bash scripts/auto-deploy.sh
```

### 4.5 文档验证

```bash
# 验证规格文档
python scripts/validate-specs.py \
  --specs-dir docs/superpowers/specs \
  --output reports/spec-validation.json

# 查看报告
cat reports/spec-validation.json
```

---

## 5. 故障排查

### 5.1 常见问题

#### 问题 1: Java 编译失败

**症状**: `mvn compile` 报错

**解决**:
```bash
# 检查 Java 版本
java -version

# 应该是 Java 17+
# 如果不是，安装正确版本

# 清理并重新编译
mvn clean compile
```

#### 问题 2: Node.js 依赖安装失败

**症状**: `npm ci` 报错

**解决**:
```bash
# 清理缓存
npm cache clean --force

# 删除 node_modules 和 lock 文件
rm -rf node_modules package-lock.json

# 重新安装
npm ci
```

#### 问题 3: Playwright 测试失败

**症状**: `npx playwright test` 报错

**解决**:
```bash
# 安装浏览器
npx playwright install

# 检查后端服务是否运行
curl http://localhost:8080/actuator/health

# 如果未运行，启动后端
cd ontograph-backend && mvn spring-boot:run
```

#### 问题 4: 云效流水线失败

**症状**: CI/CD 构建失败

**解决**:
```bash
# 检查 .yunxiao/flow.yml 配置
# 确保环境变量已配置:
# - JAVA_VERSION
# - NODE_VERSION
# - DB_HOST
# - DB_USER
# - DINGTALK_WEBHOOK

# 本地模拟运行
bash scripts/run-automated-pipeline.sh
```

### 5.2 日志查看

```bash
# 后端日志
tail -f ontograph-backend/logs/application.log

# 前端构建日志
cd ontograph-frontend && npm run build

# 测试报告
open reports/test-report.html
```

---

## 6. 最佳实践

### 6.1 需求阶段

✅ **推荐做法**:
- 使用自然语言清晰描述需求
- 提供具体的用户场景和验收标准
- 识别约束条件和技术风险
- 利用 Qoder 的 `brainstorming` 探索多种方案

❌ **避免做法**:
- 需求描述模糊不清
- 缺少关键业务规则
- 未定义边界条件
- 直接跳过分析进入编码

### 6.2 测试阶段

✅ **推荐做法**:
- 测试先行 (TDD)
- 覆盖正常路径和异常路径
- 使用 Mock 隔离外部依赖
- 自动化测试脚本，避免手动测试

❌ **避免做法**:
- 跳过测试直接编码
- 测试用例不完整
- 测试依赖外部环境
- 手动执行重复测试

### 6.3 开发阶段

✅ **推荐做法**:
- 小步快跑，频繁提交
- 使用并行 Agent 加速开发
- 代码审查后再合并
- 及时更新文档

❌ **避免做法**:
- 大批量代码一次性提交
- 跳过代码审查
- 文档与代码不同步
- 长时间不提交代码

### 6.4 CI/CD 阶段

✅ **推荐做法**:
- 自动化所有检查
- 快速失败 (Fail Fast)
- 可回滚的部署策略
- 实时监控和告警

❌ **避免做法**:
- 手动部署生产环境
- 忽略测试失败
- 缺少监控告警
- 部署后不验证

### 6.5 团队协作

✅ **推荐做法**:
- 统一使用 Qoder Skills
- 遵循项目代码规范
- 定期回顾和改进流程
- 分享最佳实践

❌ **避免做法**:
- 各自使用不同工具
- 忽略代码规范
- 不回顾不改进
- 不分享经验

---

## 7. 进阶使用

### 7.1 自定义 Skills

创建自定义 Skill 文件：`.qoder/skills/my-custom-skill.md`

```markdown
---
name: my-custom-skill
description: 我的自定义技能
---

# 自定义技能描述

## 触发条件
- 用户执行特定命令
- 满足特定条件

## 执行流程
1. 步骤 1
2. 步骤 2
3. 步骤 3

## 输出产物
- 生成的文件
- 执行的结果
```

### 7.2 自定义 Agent

配置并行 Agent：

```bash
# 在 Qoder 中使用
skill: "subagent-driven-development"

# Agent 会自动:
# 1. 读取实现计划
# 2. 拆分独立任务
# 3. 并行执行
# 4. 合并代码
```

### 7.3 集成外部工具

#### SonarQube 代码质量

```bash
# 安装 SonarScanner
# 配置 sonar-project.properties
sonar-scanner
```

#### Prometheus 监控

```yaml
# 配置 Prometheus
scrape_configs:
  - job_name: 'ontograph'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

#### Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "OntoGraph CI/CD",
    "panels": [
      {
        "title": "构建成功率",
        "type": "gauge"
      },
      {
        "title": "测试通过率",
        "type": "graph"
      }
    ]
  }
}
```

---

## 8. 参考资源

### 8.1 文档

- [Qoder 自动化流程详细文档](qoder-automated-pipeline.md)
- [Graphiti 测试文档](graphiti-test.md)
- [云效流水线配置](../.yunxiao/flow.yml)

### 8.2 脚本

- [自动化流程脚本 (Bash)](../scripts/run-automated-pipeline.sh)
- [自动化流程脚本 (PowerShell)](../scripts/run-automated-pipeline.ps1)
- [规格验证脚本](../scripts/validate-specs.py)
- [测试报告合并脚本](../scripts/merge-test-reports.py)

### 8.3 示例

- [规格文档示例](../docs/superpowers/specs/)
- [实现计划示例](../docs/superpowers/plans/)
- [测试用例示例](../ontograph-backend/src/test/)

---

## 9. 反馈与支持

- **问题反馈**: 提交 GitHub Issues
- **功能建议**: 提交 Pull Requests
- **文档改进**: 编辑本文档并提交 PR

---

**最后更新**: 2026-06-16  
**维护者**: OntoGraph 团队
