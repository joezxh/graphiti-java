/**
 * 用户管理模块 - 边界条件和异常场景测试
 * 
 * 测试覆盖:
 * - 空数据场景
 * - 特殊字符处理
 * - 网络错误处理
 * - 并发操作
 * - 键盘操作
 * - 响应式设计
 * - XSS攻击防护
 * - SQL注入防护
 * - 超长数据
 * - 权限验证
 */

import { test, expect, Page } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'
const USER_MANAGEMENT_URL = `${BASE_URL}/system/user`

// 辅助函数
async function waitForPageLoad(page: Page) {
  await page.waitForLoadState('networkidle')
  await page.waitForSelector('.ant-table')
}

test.describe('边界条件测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)
  })

  test('空数据场景 - 搜索不存在的用户', async ({ page }) => {
    // 输入不存在的用户名
    await page.fill('input[placeholder*="用户名"]', 'nonexistent_user_xyz123_abc456')
    await page.click('button:has-text("查 询")')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 验证空状态提示
    await expect(page.locator('.ant-empty')).toBeVisible()
    await expect(page.locator('.ant-empty-description')).toContainText('暂无数据')

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-empty-state.png' })

    // 重置搜索
    await page.click('button:has-text("重 置")')
    await page.waitForLoadState('networkidle')

    // 验证数据恢复
    await expect(page.locator('.ant-table-tbody tr').first()).toBeVisible()
  })

  test('特殊字符处理 - 用户名包含特殊字符', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 测试各种特殊字符
    const specialChars = [
      '!@#$%^&*()_+-=[]{}|;:,.<>?',
      '<script>alert(1)</script>',
      "'; DROP TABLE users; --",
      '用户名包含中文和emoji 🎉',
      'a'.repeat(100), // 超长用户名
    ]

    for (const char of specialChars) {
      await page.fill('#form_item_username', char)
      await page.fill('#form_item_password', 'Test123456')
      await page.fill('#form_item_nickname', 'Test')
      await page.fill('#form_item_email', 'test@test.com')

      // 截图记录
      await page.screenshot({ 
        path: `tests/screenshots/boundary-special-chars-${specialChars.indexOf(char)}.png` 
      })

      // 清理表单
      await page.fill('#form_item_username', '')
    }

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('网络错误处理 - API失败', async ({ page }) => {
    // 模拟API错误
    await page.route('**/api/v1/admin/system/user**', route => {
      route.fulfill({
        status: 500,
        body: 'Internal Server Error'
      })
    })

    // 刷新页面触发错误
    await page.reload()
    await page.waitForTimeout(2000)

    // 验证错误提示
    await expect(page.locator('.ant-message-error')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-api-error.png' })

    // 取消路由拦截
    await page.unroute('**/api/v1/admin/system/user**')
  })

  test('网络超时处理', async ({ page }) => {
    // 模拟网络延迟
    await page.route('**/api/v1/admin/system/user**', async route => {
      await new Promise(resolve => setTimeout(resolve, 10000)) // 10秒延迟
      await route.continue()
    })

    // 刷新页面
    await page.reload()

    // 等待超时
    await page.waitForTimeout(5000)

    // 验证loading状态或超时提示
    const loading = page.locator('.ant-spin')
    const error = page.locator('.ant-message-error')
    
    expect(await loading.isVisible() || await error.isVisible()).toBe(true)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-timeout.png' })

    // 取消路由拦截
    await page.unroute('**/api/v1/admin/system/user**')
  })

  test('并发操作 - 快速连续点击', async ({ page }) => {
    // 快速连续点击查询按钮10次
    const queryButton = page.locator('button:has-text("查 询")')
    
    const promises = []
    for (let i = 0; i < 10; i++) {
      promises.push(queryButton.click())
      await page.waitForTimeout(50)
    }
    
    await Promise.all(promises)
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)

    // 验证页面没有崩溃
    await expect(page.locator('.ant-table')).toBeVisible()
    await expect(page.locator('.ant-table-tbody tr').first()).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-concurrent-clicks.png' })
  })

  test('并发操作 - 同时打开多个模态框', async ({ page }) => {
    // 快速点击新建用户按钮多次
    const createButton = page.locator('button:has-text("新建用户")')
    
    await createButton.click()
    await page.waitForTimeout(100)
    await createButton.click()
    await page.waitForTimeout(100)
    await createButton.click()
    
    await page.waitForTimeout(2000)

    // 验证只有一个模态框
    const modalCount = await page.locator('.ant-modal').count()
    expect(modalCount).toBe(1)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-multiple-modals.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('键盘操作 - Enter键提交', async ({ page }) => {
    // 测试Enter键提交搜索
    await page.fill('input[placeholder*="用户名"]', 'admin')
    await page.press('input[placeholder*="用户名"]', 'Enter')
    await page.waitForLoadState('networkidle')

    // 验证搜索结果
    await expect(page.locator('.ant-table-tbody')).toContainText('admin')

    // 测试Escape键关闭模态框
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')
    await page.press('body', 'Escape')
    await expect(page.locator('.ant-modal')).not.toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-keyboard-ops.png' })
  })

  test('键盘操作 - Tab键导航', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 使用Tab键在表单字段间导航
    await page.keyboard.press('Tab') // 用户名
    await page.keyboard.type('testuser')
    
    await page.keyboard.press('Tab') // 昵称
    await page.keyboard.type('Test User')
    
    await page.keyboard.press('Tab') // 密码
    await page.keyboard.type('Test123456')
    
    await page.keyboard.press('Tab') // 邮箱
    await page.keyboard.type('test@test.com')
    
    await page.keyboard.press('Tab') // 手机
    await page.keyboard.type('13800138000')

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-tab-navigation.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('超长数据 - 字段最大长度验证', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 输入超长数据
    const longUsername = 'a'.repeat(100)
    const longNickname = 'b'.repeat(200)
    const longEmail = 'c'.repeat(100) + '@test.com'

    await page.fill('#form_item_username', longUsername)
    await page.fill('#form_item_nickname', longNickname)
    await page.fill('#form_item_password', 'Test123456')
    await page.fill('#form_item_email', longEmail)
    await page.fill('#form_item_phone', '1'.repeat(20))

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-long-data.png' })

    // 提交表单,验证是否有长度限制
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(2000)

    // 如果成功,说明后端有长度限制
    // 如果失败,说明前端需要添加长度限制
  })

  test('XSS攻击防护', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 输入XSS攻击代码
    const xssPayloads = [
      '<script>alert("XSS")</script>',
      '<img src=x onerror=alert(1)>',
      'javascript:alert(1)',
      '"><script>alert(1)</script>',
    ]

    for (const payload of xssPayloads) {
      await page.fill('#form_item_username', payload)
      await page.fill('#form_item_password', 'Test123456')
      await page.fill('#form_item_nickname', 'Test')
      await page.fill('#form_item_email', 'test@test.com')

      // 截图
      await page.screenshot({ 
        path: `tests/screenshots/boundary-xss-${xssPayloads.indexOf(payload)}.png` 
      })

      // 清理表单
      await page.fill('#form_item_username', '')
    }

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('SQL注入防护', async ({ page }) => {
    // 测试SQL注入
    const sqlPayloads = [
      "' OR '1'='1",
      "'; DROP TABLE users; --",
      "' UNION SELECT * FROM users --",
      "1; DROP TABLE users",
    ]

    for (const payload of sqlPayloads) {
      await page.fill('input[placeholder*="用户名"]', payload)
      await page.click('button:has-text("查 询")')
      await page.waitForLoadState('networkidle')
      await page.waitForTimeout(1000)

      // 验证没有返回异常数据
      await expect(page.locator('.ant-table-tbody')).not.toContainText(payload)

      // 截图
      await page.screenshot({ 
        path: `tests/screenshots/boundary-sql-injection-${sqlPayloads.indexOf(payload)}.png` 
      })

      // 重置搜索
      await page.click('button:has-text("重 置")')
    }
  })

  test('权限验证 - 未登录用户', async ({ page }) => {
    // 清除所有cookies
    await page.context().clearCookies()

    // 访问用户管理页面
    await page.goto(USER_MANAGEMENT_URL)
    await page.waitForTimeout(2000)

    // 验证跳转到登录页或显示无权限提示
    const isLoginPage = await page.locator('input[type="password"]').isVisible()
    const isUnauthorized = await page.locator('.ant-result-error, .ant-alert-error').isVisible()

    expect(isLoginPage || isUnauthorized).toBe(true)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-unauthorized.png' })
  })

  test('响应式设计 - 移动端适配', async ({ page }) => {
    // 模拟移动端视口
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    // 验证表格存在
    await expect(page.locator('.ant-table')).toBeVisible()

    // 验证表格可能隐藏了某些列
    const visibleColumns = await page.locator('.ant-table-thead th:visible').count()
    console.log(`移动端可见列数: ${visibleColumns}`)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-mobile-view.png' })

    // 测试平板视口
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.reload()
    await waitForPageLoad(page)
    await page.screenshot({ path: 'tests/screenshots/boundary-tablet-view.png' })

    // 恢复桌面视口
    await page.setViewportSize({ width: 1280, height: 720 })
  })

  test('浏览器兼容性 - 旧版浏览器', async ({ page }) => {
    // 模拟旧版Chrome
    await page.setExtraHTTPHeaders({
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36'
    })

    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    // 验证基本功能可用
    await expect(page.locator('.ant-table')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-old-browser.png' })
  })

  test('极端数据 - 空字符串和null值', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 测试空字符串
    await page.fill('#form_item_username', '')
    await page.fill('#form_item_nickname', '')
    await page.fill('#form_item_password', '')
    await page.fill('#form_item_email', '')
    await page.fill('#form_item_phone', '')

    // 提交表单
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(1000)

    // 验证表单验证错误
    const errorCount = await page.locator('.ant-form-item-explain-error').count()
    expect(errorCount).toBeGreaterThan(0)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-empty-fields.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('极端数据 - 负数和零', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 填写正常数据
    await page.fill('#form_item_username', 'testuser')
    await page.fill('#form_item_password', 'Test123456')
    await page.fill('#form_item_nickname', 'Test')
    await page.fill('#form_item_email', 'test@test.com')
    await page.fill('#form_item_phone', '0') // 零

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-zero-values.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('日期和时间 - 时区处理', async ({ page }) => {
    // 验证创建时间格式
    const firstRow = page.locator('.ant-table-tbody tr').first()
    const createdAt = await firstRow.locator('td').nth(6).textContent()

    // 验证日期格式
    expect(createdAt).toMatch(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/boundary-date-format.png' })
  })

  test('文件上传 - 头像上传(如果支持)', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 检查是否有头像上传功能
    const uploadButton = page.locator('input[type="file"], .ant-upload')
    if (await uploadButton.isVisible()) {
      // 创建一个测试图片
      const testImagePath = 'tests/fixtures/test-avatar.png'
      
      // 上传图片
      await uploadButton.setInputFiles(testImagePath)
      await page.waitForTimeout(2000)

      // 截图
      await page.screenshot({ path: 'tests/screenshots/boundary-avatar-upload.png' })
    }

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })
})

// 性能测试
test.describe('性能测试', () => {
  test('页面加载性能 - 首屏时间', async ({ page }) => {
    const startTime = Date.now()
    
    await page.goto(USER_MANAGEMENT_URL)
    await page.waitForLoadState('domcontentloaded')
    
    const firstPaintTime = Date.now() - startTime
    
    // 验证首屏时间小于2秒
    expect(firstPaintTime).toBeLessThan(2000)
    
    console.log(`首屏时间: ${firstPaintTime}ms`)
  })

  test('页面加载性能 - 完全加载时间', async ({ page }) => {
    const startTime = Date.now()
    
    await page.goto(USER_MANAGEMENT_URL)
    await page.waitForLoadState('networkidle')
    
    const fullLoadTime = Date.now() - startTime
    
    // 验证完全加载时间小于3秒
    expect(fullLoadTime).toBeLessThan(3000)
    
    console.log(`完全加载时间: ${fullLoadTime}ms`)
  })

  test('API响应性能 - 搜索接口', async ({ page }) => {
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    const startTime = Date.now()
    
    // 执行搜索
    await page.fill('input[placeholder*="用户名"]', 'admin')
    await page.click('button:has-text("查 询")')
    await page.waitForLoadState('networkidle')
    
    const searchTime = Date.now() - startTime
    
    // 验证API响应时间小于1秒
    expect(searchTime).toBeLessThan(1000)
    
    console.log(`搜索API响应时间: ${searchTime}ms`)
  })

  test('内存使用 - 大量数据', async ({ page }) => {
    // 如果有大量数据,测试内存使用
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    // 滚动表格加载更多数据(如果支持虚拟滚动)
    for (let i = 0; i < 10; i++) {
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
      await page.waitForTimeout(500)
    }

    // 获取内存使用情况(如果浏览器支持)
    const memoryUsage = await page.evaluate(() => {
      if ('memory' in performance) {
        const mem = (performance as any).memory
        return {
          usedJSHeapSize: mem.usedJSHeapSize,
          totalJSHeapSize: mem.totalJSHeapSize,
        }
      }
      return null
    })

    if (memoryUsage) {
      console.log('内存使用:', memoryUsage)
    }

    // 截图
    await page.screenshot({ path: 'tests/screenshots/performance-memory.png' })
  })
})
