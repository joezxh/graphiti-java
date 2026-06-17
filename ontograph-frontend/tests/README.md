# 用户管理模块 E2E测试执行指南

## 📋 测试文件说明

### 测试文件

1. **user-management.spec.ts** - 核心功能测试
   - 页面加载和列表展示
   - 搜索过滤功能
   - 新建用户完整流程
   - 编辑用户
   - 删除用户
   - 重置密码
   - 表单验证
   - 分页功能
   - 键盘操作
   - 响应式设计

2. **user-management-boundary.spec.ts** - 边界条件和异常场景测试
   - 空数据场景
   - 特殊字符处理
   - 网络错误处理
   - 并发操作
   - XSS攻击防护
   - SQL注入防护
   - 超长数据验证
   - 权限验证
   - 响应式设计
   - 浏览器兼容性
   - 性能测试

### 配置文件

- **playwright.config.ts** - Playwright配置
  - 多浏览器支持(Chrome, Firefox, WebKit)
  - 移动端支持
  - 自动截图和视频录制
  - HTML测试报告

## 🚀 安装依赖

```bash
# 进入前端目录
cd ontograph-frontend

# 安装Playwright
pnpm add -D @playwright/test

# 安装浏览器驱动
pnpm exec playwright install

# 安装类型定义(如果需要)
pnpm add -D @types/node
```

## 🧪 执行测试

### 1. 执行所有测试

```bash
# 使用Playwright默认配置
pnpm exec playwright test

# 指定项目(浏览器)
pnpm exec playwright test --project=chromium
```

### 2. 执行特定测试文件

```bash
# 核心功能测试
pnpm exec playwright test tests/user-management.spec.ts

# 边界条件测试
pnpm exec playwright test tests/user-management-boundary.spec.ts
```

### 3. 执行特定测试用例

```bash
# 通过测试名称筛选
pnpm exec playwright test -g "新建用户"

# 执行步骤3
pnpm exec playwright test -g "步骤3"
```

### 4. 调试模式

```bash
# 启用调试模式
pnpm exec playwright test --debug

# 单步执行
pnpm exec playwright test --debug --project=chromium
```

### 5. 生成测试报告

```bash
# 执行测试并生成报告
pnpm exec playwright test --reporter=html

# 打开报告
pnpm exec playwright show-report tests/reports
```

## 📊 测试报告

### HTML报告

测试完成后,报告会自动生成在 `tests/reports/` 目录:

```bash
# 查看报告
pnpm exec playwright show-report
```

报告包含:
- 测试概览(通过/失败/跳过)
- 每个测试的详细信息
- 失败测试的截图和视频
- 性能指标
- 浏览器兼容性

### JSON报告

```bash
# 生成JSON报告
pnpm exec playwright test --reporter=json

# 查看结果
cat tests/reports/results.json
```

## 📸 截图管理

### 自动截图

测试会自动截图保存在 `tests/screenshots/`:

- `users-list.png` - 用户列表
- `users-search-result.png` - 搜索结果
- `users-create-form.png` - 新建用户表单
- `users-create-result.png` - 创建结果
- `users-edit-form.png` - 编辑表单
- `users-delete-result.png` - 删除结果
- `boundary-*.png` - 边界条件截图

### 手动截图

```typescript
// 在测试中添加截图
await page.screenshot({ path: 'tests/screenshots/my-screenshot.png' })
```

## 🎯 测试场景

### 场景1: 完整用户流程

```bash
# 执行完整流程测试
pnpm exec playwright test tests/user-management.spec.ts
```

测试内容:
1. 访问用户管理页面
2. 搜索用户
3. 新建用户
4. 编辑用户
5. 删除用户
6. 重置密码

### 场景2: 边界条件验证

```bash
# 执行边界条件测试
pnpm exec playwright test tests/user-management-boundary.spec.ts
```

测试内容:
1. 空数据处理
2. 特殊字符输入
3. 网络错误
4. 并发操作
5. XSS/SQL注入防护

### 场景3: 性能测试

```bash
# 执行性能测试
pnpm exec playwright test -g "性能测试"
```

测试内容:
1. 页面加载时间
2. API响应时间
3. 内存使用

## 🔧 高级配置

### 自定义浏览器

```bash
# 使用Chrome
pnpm exec playwright test --browser=chromium

# 使用Firefox
pnpm exec playwright test --browser=firefox

# 使用WebKit
pnpm exec playwright test --browser=webkit
```

### 移动端测试

```bash
# iPhone测试
pnpm exec playwright test --project="Mobile Safari"

# Android测试
pnpm exec playwright test --project="Mobile Chrome"
```

### 并行执行

```bash
# 指定worker数量
pnpm exec playwright test --workers=4

# 禁用并行
pnpm exec playwright test --workers=1
```

## 📝 CI/CD集成

### GitHub Actions

创建 `.github/workflows/playwright.yml`:

```yaml
name: Playwright Tests
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
jobs:
  test:
    timeout-minutes: 60
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-node@v3
      with:
        node-version: 18
    - name: Install dependencies
      run: |
        cd ontograph-frontend
        pnpm install
        pnpm exec playwright install --with-deps
    - name: Start backend
      run: |
        cd ontograph-backend
        # 启动后端服务
    - name: Start frontend
      run: |
        cd ontograph-frontend
        pnpm dev &
    - name: Run Playwright tests
      run: |
        cd ontograph-frontend
        pnpm exec playwright test
    - uses: actions/upload-artifact@v3
      if: always()
      with:
        name: playwright-report
        path: ontograph-frontend/tests/reports/
        retention-days: 30
```

### Docker

```dockerfile
FROM mcr.microsoft.com/playwright:v1.40.0-focal

WORKDIR /app
COPY . .

RUN npm install
RUN npx playwright install --with-deps

CMD ["npx", "playwright", "test"]
```

## 🐛 调试技巧

### 1. 使用Playwright Inspector

```bash
# 启用Inspector
PWDEBUG=1 pnpm exec playwright test
```

### 2. 截图对比

```typescript
// 保存基准截图
await page.screenshot({ path: 'tests/screenshots/baseline.png' })

// 对比截图
await expect(page).toHaveScreenshot('baseline.png')
```

### 3. 录制测试

```bash
# 使用Codegen录制测试
pnpm exec playwright codegen http://localhost:3000/system/user
```

### 4. 追踪调试

```bash
# 启用追踪
pnpm exec playwright test --trace on

# 查看追踪
pnpm exec playwright show-trace trace.zip
```

## 📊 测试结果分析

### 成功标准

- ✅ 所有核心功能测试通过
- ✅ 边界条件测试通过
- ✅ 性能指标达标
- ✅ 无严重安全漏洞

### 失败处理

1. **查看失败原因**
   ```bash
   pnpm exec playwright test --reporter=list
   ```

2. **查看截图**
   ```bash
   ls tests/screenshots/*-failure.png
   ```

3. **查看视频**
   ```bash
   ls tests/videos/*.webm
   ```

## 🎓 最佳实践

### 1. 测试数据管理

```typescript
// 使用唯一的时间戳
const TEST_USER = {
  username: `e2e_user_${Date.now()}`,
  // ...
}
```

### 2. 测试隔离

```typescript
// 每个测试前清理数据
test.beforeEach(async ({ page }) => {
  await cleanupTestData()
})
```

### 3. 断言最佳实践

```typescript
// 使用语义化断言
await expect(page.locator('.ant-table')).toBeVisible()
await expect(page.locator('button')).toContainText('提交')
```

### 4. 等待策略

```typescript
// 等待网络空闲
await page.waitForLoadState('networkidle')

// 等待元素可见
await page.waitForSelector('.ant-modal', { state: 'visible' })

// 等待元素消失
await page.waitForSelector('.ant-spin', { state: 'hidden' })
```

## 🔐 安全测试

### XSS测试

```typescript
test('XSS防护', async ({ page }) => {
  await page.fill('#username', '<script>alert(1)</script>')
  // 验证脚本未执行
})
```

### SQL注入测试

```typescript
test('SQL注入防护', async ({ page }) => {
  await page.fill('#username', "' OR '1'='1")
  // 验证未返回异常数据
})
```

## 📚 参考资源

- [Playwright官方文档](https://playwright.dev/)
- [Playwright API参考](https://playwright.dev/docs/api/class-page)
- [测试最佳实践](https://playwright.dev/docs/test-assertions)
- [CI/CD集成](https://playwright.dev/docs/ci)

## 🆘 常见问题

### Q: 测试失败:找不到元素

A: 
- 检查选择器是否正确
- 添加等待时间
- 使用`page.waitForSelector()`

### Q: 测试超时

A:
- 增加超时配置
- 检查网络状态
- 使用`--timeout`参数

### Q: 浏览器启动失败

A:
```bash
# 重新安装浏览器
pnpm exec playwright install
```

## 📞 联系支持

如有问题,请查看:
- 测试报告: `tests/reports/`
- 截图: `tests/screenshots/`
- 日志: 控制台输出
