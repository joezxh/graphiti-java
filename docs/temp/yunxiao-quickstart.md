# 云效 AI 自动化测试快速入门指南

## 🎯 目标

在 30 分钟内完成阿里云云效平台的自动化 AI 测试流水线配置,实现代码提交后的自动测试。

---

## 📋 前置条件

- ✅ 阿里云账号已开通云效服务
- ✅ 代码已托管到云效代码库 (或已关联 GitHub/GitLab)
- ✅ 项目依赖已配置 (JDK 17, Node.js 18, Maven, PostgreSQL/MySQL, Neo4j)
- ✅ 测试环境已准备 (数据库、Neo4j 图数据库)

---

## 🚀 快速开始 (5 步完成)

### 步骤 1: 在云效创建流水线

1. 登录 [阿里云云效](https://flow.aliyun.com/)
2. 进入你的项目 → **流水线** → **新建流水线**
3. 选择 **自定义流水线**
4. 流水线名称: `OntoGraph AI 自动化测试`
5. 点击 **创建**

### 步骤 2: 导入流水线配置

**方式 A: 使用 YAML 配置 (推荐)**

1. 在流水线编辑页面,点击右上角 **YAML 模式**
2. 复制 `.yunxiao/flow.yml` 文件内容
3. 粘贴到 YAML 编辑器
4. 点击 **保存**

**方式 B: 手动配置各阶段**

按照 `.yunxiao/flow.yml` 中的配置,手动添加各个阶段和任务。

### 步骤 3: 配置环境变量

在流水线 → **变量管理** 中添加以下变量:

```bash
# 数据库配置
DB_HOST=your-db-host.rds.aliyuncs.com
DB_USER=ontograph
DB_PASSWORD=your-password

# 测试账号
TEST_USERNAME=admin
TEST_PASSWORD=admin123

# 服务地址
FRONTEND_URL=http://localhost:5173
BACKEND_URL=http://localhost:8080

# 通知配置 (可选)
DINGTALK_WEBHOOK=https://oapi.dingtalk.com/robot/send?access_token=xxx

# OSS 部署配置 (可选)
OSS_ACCESS_KEY_ID=xxx
OSS_ACCESS_KEY_SECRET=xxx
OSS_BUCKET=your-bucket
```

**注意**: 密码等敏感信息请设置为 **保密变量**。

### 步骤 4: 配置触发规则

在流水线 → **触发规则** 中配置:

```yaml
# Push 触发
- 分支: main, develop, feature/**
- 路径: ontograph-backend/**, ontograph-frontend/**

# 合并请求触发
- 目标分支: main, develop

# 定时触发 (每日回归)
- Cron: 0 2 * * * (每天凌晨 2 点)
- 分支: main
```

### 步骤 5: 测试流水线

1. 提交一个测试代码变更:
   ```bash
   git add .
   git commit -m "test: 触发 AI 自动化测试"
   git push origin main
   ```

2. 在云效查看流水线执行:
   - 进入 **流水线** → 点击运行中的流水线
   - 查看各阶段执行日志
   - 下载测试报告

---

## 📊 查看测试结果

### 方式 1: 云效控制台

1. 进入流水线执行记录
2. 点击 **测试报告** 标签
3. 查看单元测试报告、AI 测试报告

### 方式 2: 下载产物

1. 进入流水线执行记录
2. 点击 **产物** 标签
3. 下载 `ai-test-report.html`
4. 本地浏览器打开查看

### 方式 3: 钉钉通知

如果配置了钉钉 Webhook,测试完成后会自动发送通知:

```
🤖 AI 自动化测试完成

提交信息: test: 触发 AI 自动化测试
分支: main
提交者: 张三

测试结果:
| 阶段 | 状态 |
|------|------|
| 代码质量 | ✅ 通过 |
| 单元测试 | ✅ 通过 |
| AI 前端测试 | ✅ 通过 |
| AI API 测试 | ✅ 通过 |
| AI 回归测试 | ✅ 通过 |

📊 查看完整测试报告: [链接]
```

---

## 🔧 自定义测试套件

### 添加新的测试用例

编辑 `scripts/ai-frontend-test.sh`,添加测试函数:

```bash
# 新增测试套件
test_your_feature() {
    echo ""
    echo "🎯 ========== 你的功能测试 =========="
    
    # TC-YOUR-001: 测试用例 1
    run_test_case "TC-YOUR-001" "$BASE_URL/your-page" '
        // Playwright 测试代码
        const element = await page.$(".your-selector");
        if (!element) {
            throw new Error("元素不存在");
        }
    '
}
```

### 配置测试并行执行

在 `.yunxiao/flow.yml` 中:

```yaml
# 并行执行多个测试套件
- name: "AI 前端测试 - 并行"
  jobs:
    - name: "仪表盘测试"
      steps: [...]
    
    - name: "图谱管理测试"
      steps: [...]
    
    - name: "数据导入测试"
      steps: [...]
```

---

## 🎓 进阶使用

### 1. AI 智能诊断

当测试失败时,AI 会自动分析问题并生成修复建议:

```json
{
  "name": "TC-GRAPH-002",
  "status": "failed",
  "errors": ["新建按钮不存在"],
  "ai_suggestions": [
    "检查按钮文本是否为 '新建图谱',可能是国际化键值未翻译",
    "验证按钮是否在对话框内,需要使用 page.waitForSelector",
    "查看截图: reports/ai-tests/screenshots/TC-GRAPH-002-error.png"
  ]
}
```

### 2. 变更感知测试

根据代码变更智能选择测试套件:

```yaml
# 仅测试受影响的模块
test_strategy:
  frontend_changes:
    - dashboard
    - graph-management
  backend_changes:
    - api-auth
    - api-graph
  database_changes:
    - full-regression
```

### 3. 定时回归测试

每天凌晨执行完整回归测试:

```yaml
# 在触发规则中配置
schedule:
  - cron: "0 2 * * *"
    branches: [main]
    suites: [full-regression]
```

### 4. 性能基准测试

监控关键操作的响应时间:

```bash
# 在测试脚本中添加性能检查
run_test_case "TC-PERF-001" "$BASE_URL/dashboard" '
    const startTime = Date.now();
    await page.goto("$BASE_URL/dashboard");
    await page.waitForLoadState("networkidle");
    const loadTime = Date.now() - startTime;
    
    if (loadTime > 3000) {
        throw new Error(`页面加载过慢: ${loadTime}ms (标准: <3000ms)`);
    }
'
```

---

## 🚨 常见问题

### Q1: 流水线执行失败,提示 "JDK 版本不匹配"

**解决方案**:
```yaml
# 在流水线配置中明确指定 JDK 版本
jdk_version: "17"
maven_version: "3.8.6"
```

### Q2: 前端测试报错 "页面加载超时"

**解决方案**:
```bash
# 增加等待时间
await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });

# 检查前端服务是否正常启动
curl -I http://localhost:5173
```

### Q3: 数据库连接失败

**解决方案**:
```yaml
# 检查环境变量是否正确配置
DB_HOST: 确保使用内网地址
DB_USER: 确保有创建数据库权限
DB_PASSWORD: 检查密码是否正确

# 在流水线中添加数据库连接测试
- step: run@shell
  command: |
    psql -h $DB_HOST -U $DB_USER -c "SELECT 1;"
```

### Q4: 钉钉通知发送失败

**解决方案**:
```bash
# 检查 Webhook 地址是否正确
# 在钉钉群设置 → 智能群助手 → 自定义 Webhook
# 确保复制完整的 URL (包含 access_token)

# 测试 Webhook
curl -X POST "$DINGTALK_WEBHOOK" \
  -H "Content-Type: application/json" \
  -d '{"msgtype":"text","text":{"content":"测试通知"}}'
```

---

## 📈 优化建议

### 1. 加速流水线执行

```yaml
# 启用缓存
caches:
  - ~/.m2/repository          # Maven 依赖缓存
  - ontograph-frontend/node_modules  # NPM 依赖缓存
  - ~/.cache/pip              # Python 包缓存

# 并行执行任务
parallel:
  max_concurrent: 5
```

### 2. 减少测试失败率

```bash
# 添加重试机制
run_test_case "TC-XXX" "$URL" '...' --retries 2

# 增加页面等待
await page.waitForTimeout(2000);

# 使用稳定的选择器
await page.$('[data-testid="submit-btn"]');  # 推荐
await page.$('.el-button');                   # 不推荐 (可能变化)
```

### 3. 提高测试覆盖率

```bash
# 定期审查 graphiti-test.md 中的测试用例
# 确保所有关键功能都有对应的自动化测试

# 使用代码覆盖率工具
mvn test jacoco:report
npx nyc npm test
```

---

## 📚 参考文档

- [完整方案文档](./yunxiao-ai-test-cicd.md)
- [graphiti-test.md 测试提示词](./graphiti-test.md)
- [云效 Flow 官方文档](https://help.aliyun.com/product/116588.html)
- [Playwright 文档](https://playwright.dev/docs/intro)

---

## 🎉 完成检查清单

- [ ] 云效流水线已创建
- [ ] YAML 配置已导入或手动配置完成
- [ ] 环境变量已配置
- [ ] 触发规则已设置
- [ ] 测试提交并成功执行
- [ ] 测试报告可查看
- [ ] 钉钉通知已配置 (可选)
- [ ] 定时回归已配置 (可选)

**恭喜! 🎊 你现在拥有了自动化 AI 测试流水线!**

每次代码提交都会自动触发:
- ✅ 代码质量检查
- ✅ 单元测试
- ✅ AI 前端 UI 测试
- ✅ AI 后端 API 测试
- ✅ AI 回归测试
- ✅ 测试报告生成
- ✅ 钉钉通知发送

**下一步**:
1. 根据项目需求自定义测试用例
2. 优化测试执行速度
3. 集成更多 AI 能力 (智能修复、代码审查等)

有任何问题,欢迎查阅完整文档或联系技术支持! 🚀
