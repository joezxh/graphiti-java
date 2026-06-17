import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  // 测试目录
  testDir: './tests',

  // 并行执行
  fullyParallel: true,

  // 失败重试次数
  retries: process.env.CI ? 2 : 0,

  // 并行worker数
  workers: process.env.CI ? 1 : undefined,

  // 测试报告
  reporter: [
    ['html', { outputFolder: 'tests/reports', open: 'never' }],
    ['list'],
    ['json', { outputFile: 'tests/reports/results.json' }]
  ],

  // 全局配置
  use: {
    // 基础URL
    baseURL: 'http://localhost:3000',

    // 截图配置
    screenshot: 'only-on-failure',

    // 视频配置
    video: 'retain-on-failure',

    // 追踪配置
    trace: 'retain-on-failure',

    // 超时配置
    actionTimeout: 10000,
    navigationTimeout: 30000,
  },

  // 项目配置
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1280, height: 720 },
      },
    },
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        viewport: { width: 1280, height: 720 },
      },
    },
    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
        viewport: { width: 1280, height: 720 },
      },
    },
    // 移动端测试
    {
      name: 'Mobile Chrome',
      use: {
        ...devices['Pixel 5'],
      },
    },
    {
      name: 'Mobile Safari',
      use: {
        ...devices['iPhone 12'],
      },
    },
  ],

  // 全局超时
  timeout: 60000,

  // 断言超时
  expect: {
    timeout: 10000,
  },

  // 启动web服务器
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
})
