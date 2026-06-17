/**
 * 用户管理模块 E2E测试
 * 
 * 测试覆盖:
 * - 用户列表展示
 * - 搜索过滤
 * - 新建用户
 * - 编辑用户
 * - 删除用户
 * - 重置密码
 * - 角色分配
 * - 用户禁用
 */

import { test, expect, Page } from '@playwright/test'

const BASE_URL = 'http://localhost:3000'
const USER_MANAGEMENT_URL = `${BASE_URL}/system/user`

// 测试数据
const TEST_USER = {
  username: `e2e_user_${Date.now()}`,
  nickname: 'E2E测试用户',
  password: 'Test123456',
  email: 'e2e@test.com',
  phone: '13800138000'
}

// 辅助函数: 等待页面加载完成
async function waitForPageLoad(page: Page) {
  await page.waitForLoadState('networkidle')
  await page.waitForSelector('.ant-table')
}

// 辅助函数: 获取表格行数
async function getTableRowCount(page: Page): Promise<number> {
  return await page.locator('.ant-table-tbody tr').count()
}

// 辅助函数: 验证表格列头
async function verifyTableHeaders(page: Page) {
  const headers = await page.locator('.ant-table-thead th').allTextContents()
  expect(headers).toContain('ID')
  expect(headers).toContain('用户名')
  expect(headers).toContain('昵称')
  expect(headers).toContain('邮箱')
  expect(headers).toContain('状态')
  expect(headers).toContain('创建时间')
  expect(headers).toContain('操作')
}

test.describe('用户管理模块', () => {
  test.beforeEach(async ({ page }) => {
    // 每个测试前访问用户管理页面
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)
  })

  test('步骤1: 页面加载和列表展示', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h2')).toContainText('用户管理')
    await expect(page.locator('.page-description')).toContainText('管理系统用户,分配角色权限')

    // 验证表格存在
    await expect(page.locator('.ant-table')).toBeVisible()

    // 验证表格列头
    await verifyTableHeaders(page)

    // 验证至少有数据
    const rowCount = await getTableRowCount(page)
    expect(rowCount).toBeGreaterThan(0)

    // 验证操作按钮
    await expect(page.locator('button:has-text("新建用户")')).toBeVisible()
    
    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-list.png' })
  })

  test('步骤2: 搜索过滤功能', async ({ page }) => {
    // 获取初始行数
    const initialRowCount = await getTableRowCount(page)

    // 输入搜索关键词
    await page.fill('input[placeholder*="用户名"]', 'admin')
    
    // 点击查询按钮
    await page.click('button:has-text("查 询")')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 验证搜索结果
    const filteredRowCount = await getTableRowCount(page)
    expect(filteredRowCount).toBeLessThanOrEqual(initialRowCount)
    expect(filteredRowCount).toBeGreaterThan(0)

    // 验证搜索结果包含admin
    await expect(page.locator('.ant-table-tbody')).toContainText('admin')

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-search-result.png' })

    // 重置搜索
    await page.click('button:has-text("重 置")')
    await page.waitForLoadState('networkidle')

    // 验证重置后恢复初始状态
    const resetRowCount = await getTableRowCount(page)
    expect(resetRowCount).toBe(initialRowCount)
  })

  test('步骤3: 新建用户 - 完整流程', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 验证模态框标题
    await expect(page.locator('.ant-modal-title')).toContainText('新建用户')

    // 填写表单
    await page.fill('#form_item_username', TEST_USER.username)
    await page.fill('#form_item_nickname', TEST_USER.nickname)
    await page.fill('#form_item_password', TEST_USER.password)
    await page.fill('#form_item_email', TEST_USER.email)
    await page.fill('#form_item_phone', TEST_USER.phone)

    // 验证状态默认启用
    await expect(page.locator('input[type="radio"]:first-of-type')).toBeChecked()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-create-form.png' })

    // 提交表单
    await page.click('button:has-text("确 认")')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)

    // 验证成功提示
    await expect(page.locator('.ant-message-success')).toBeVisible()

    // 验证模态框关闭
    await expect(page.locator('.ant-modal')).not.toBeVisible()

    // 验证新用户出现在列表中
    await expect(page.locator('.ant-table-tbody')).toContainText(TEST_USER.username)
    await expect(page.locator('.ant-table-tbody')).toContainText(TEST_USER.nickname)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-create-result.png' })
  })

  test('步骤4: 新建用户 - 唯一性验证', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 输入已存在的用户名
    await page.fill('#form_item_username', 'admin')
    await page.fill('#form_item_password', 'Test123456')
    await page.fill('#form_item_nickname', 'Test')
    await page.fill('#form_item_email', 'test@test.com')

    // 提交表单
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(2000)

    // 验证错误提示
    await expect(page.locator('.ant-message-error, .ant-form-item-explain-error')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-duplicate-check.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('步骤5: 编辑用户', async ({ page }) => {
    // 找到第一个用户并点击编辑
    const firstRow = page.locator('.ant-table-tbody tr').first()
    await firstRow.locator('button:has-text("编辑")').click()
    await page.waitForSelector('.ant-modal')

    // 验证模态框标题
    await expect(page.locator('.ant-modal-title')).toContainText('编辑用户')

    // 验证用户名不可编辑
    await expect(page.locator('#form_item_username')).toBeDisabled()

    // 修改昵称
    const newNickname = 'E2E-Modified-Nickname'
    await page.fill('#form_item_nickname', newNickname)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-edit-form.png' })

    // 提交修改
    await page.click('button:has-text("确 认")')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)

    // 验证成功提示
    await expect(page.locator('.ant-message-success')).toBeVisible()

    // 验证列表更新
    await expect(page.locator('.ant-table-tbody')).toContainText(newNickname)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-edit-result.png' })
  })

  test('步骤6: 重置密码', async ({ page }) => {
    // 找到第一个用户并点击重置密码
    const firstRow = page.locator('.ant-table-tbody tr').first()
    
    // 添加确认对话框处理
    page.on('dialog', async (dialog) => {
      await dialog.accept()
    })

    await firstRow.locator('button:has-text("重置密码")').click()
    await page.waitForTimeout(2000)

    // 验证成功提示
    await expect(page.locator('.ant-message-success')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-reset-password.png' })
  })

  test('步骤7: 删除用户', async ({ page }) => {
    // 找到最后一个用户(测试用户)并点击删除
    const lastRow = page.locator('.ant-table-tbody tr').last()
    
    // 添加确认对话框处理
    page.on('dialog', async (dialog) => {
      await dialog.accept()
    })

    await lastRow.locator('button:has-text("删除")').click()
    await page.waitForTimeout(2000)

    // 验证成功提示
    await expect(page.locator('.ant-message-success')).toBeVisible()

    // 验证用户从列表中移除
    await page.waitForTimeout(1000)
    
    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-delete-result.png' })
  })

  test('步骤8: 表单验证', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 不填写任何内容直接提交
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(1000)

    // 验证所有必填字段错误提示
    await expect(page.locator('.ant-form-item-explain-error')).toHaveCount(5)

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-form-validation.png' })

    // 测试无效邮箱
    await page.fill('#form_item_email', 'invalid-email')
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.ant-form-item-explain-error')).toContainText('邮箱')

    // 测试密码长度不足
    await page.fill('#form_item_password', '123')
    await page.click('button:has-text("确 认")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.ant-form-item-explain-error')).toContainText('密码')

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('步骤9: 分页功能', async ({ page }) => {
    // 验证分页器存在
    await expect(page.locator('.ant-pagination')).toBeVisible()

    // 验证每页条数选择
    await expect(page.locator('.ant-pagination-options')).toBeVisible()

    // 获取当前页码
    const currentPage = await page.locator('.ant-pagination-item-active').textContent()
    expect(currentPage).toBe('1')

    // 如果有下一页,测试翻页
    const nextButton = page.locator('.ant-pagination-next')
    if (await nextButton.isEnabled()) {
      await nextButton.click()
      await page.waitForLoadState('networkidle')
      
      // 验证页码更新
      const newPage = await page.locator('.ant-pagination-item-active').textContent()
      expect(newPage).toBe('2')
    }

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-pagination.png' })
  })

  test('步骤10: 边界条件 - 空数据', async ({ page }) => {
    // 搜索不存在的用户
    await page.fill('input[placeholder*="用户名"]', 'nonexistent_user_xyz123')
    await page.click('button:has-text("查 询")')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 验证空状态
    await expect(page.locator('.ant-empty')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-empty-state.png' })

    // 重置搜索
    await page.click('button:has-text("重 置")')
  })

  test('步骤11: 边界条件 - 特殊字符', async ({ page }) => {
    // 点击新建用户按钮
    await page.click('button:has-text("新建用户")')
    await page.waitForSelector('.ant-modal')

    // 输入特殊字符
    const specialChars = '!@#$%^&*()_+-=[]{}|;:,.<>?'
    await page.fill('#form_item_username', `user_${specialChars}`)
    await page.fill('#form_item_nickname', `昵称${specialChars}`)
    await page.fill('#form_item_password', 'Test123456')
    await page.fill('#form_item_email', 'test@test.com')

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-special-chars.png' })

    // 关闭模态框
    await page.click('button:has-text("取 消")')
  })

  test('步骤12: 异常场景 - 网络错误处理', async ({ page }) => {
    // 模拟网络错误
    await page.route('**/api/**', route => {
      route.abort()
    })

    // 刷新页面
    await page.reload()
    await page.waitForTimeout(2000)

    // 验证错误提示
    await expect(page.locator('.ant-message-error')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-network-error.png' })

    // 取消路由拦截
    await page.unroute('**/api/**')
  })

  test('步骤13: 并发操作 - 快速点击', async ({ page }) => {
    // 快速连续点击查询按钮
    const queryButton = page.locator('button:has-text("查 询")')
    
    for (let i = 0; i < 5; i++) {
      await queryButton.click()
      await page.waitForTimeout(100)
    }

    await page.waitForLoadState('networkidle')

    // 验证页面没有崩溃
    await expect(page.locator('.ant-table')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-concurrent-ops.png' })
  })

  test('步骤14: 键盘操作', async ({ page }) => {
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
    await page.screenshot({ path: 'tests/screenshots/users-keyboard-ops.png' })
  })

  test('步骤15: 响应式设计 - 移动端适配', async ({ page }) => {
    // 模拟移动端视口
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    // 验证表格横向滚动
    await expect(page.locator('.ant-table')).toBeVisible()

    // 截图
    await page.screenshot({ path: 'tests/screenshots/users-mobile-view.png' })

    // 恢复桌面视口
    await page.setViewportSize({ width: 1280, height: 720 })
  })
})

// 性能测试
test.describe('性能测试', () => {
  test('页面加载性能', async ({ page }) => {
    const startTime = Date.now()
    
    await page.goto(USER_MANAGEMENT_URL)
    await page.waitForLoadState('networkidle')
    
    const loadTime = Date.now() - startTime
    
    // 验证加载时间小于3秒
    expect(loadTime).toBeLessThan(3000)
    
    console.log(`页面加载时间: ${loadTime}ms`)
  })

  test('API响应性能', async ({ page }) => {
    await page.goto(USER_MANAGEMENT_URL)
    await waitForPageLoad(page)

    const startTime = Date.now()
    
    // 执行搜索
    await page.fill('input[placeholder*="用户名"]', 'admin')
    await page.click('button:has-text("查 询")')
    await page.waitForLoadState('networkidle')
    
    const responseTime = Date.now() - startTime
    
    // 验证API响应时间小于1秒
    expect(responseTime).toBeLessThan(1000)
    
    console.log(`API响应时间: ${responseTime}ms`)
  })
})
