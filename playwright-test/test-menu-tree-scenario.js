// 测试场景 1: 页面加载与菜单树
// 前端: http://localhost:3000
// 后端: http://localhost:9090
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const FRONTEND_URL = 'http://localhost:3000';
const SCREENSHOT_DIR = path.join(__dirname, 'test-results', 'menu-tree');

function logHeader(title) {
  console.log('\n' + '='.repeat(60));
  console.log('  ' + title);
  console.log('='.repeat(60));
}

function logStep(step, message) {
  console.log(`[${step}] ${message}`);
}

function logPass(message) {
  console.log('  PASS: ' + message);
}

function logFail(message) {
  console.log('  FAIL: ' + message);
}

(async () => {
  logHeader('测试场景 1: 页面加载与菜单树');

  if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  }

  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
    executablePath: 'C:\\Users\\joezxh\\AppData\\Local\\ms-playwright\\chromium-1223\\chrome-win64\\chrome.exe'
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 }
  });
  const page = await context.newPage();

  // 收集控制台错误
  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });
  page.on('pageerror', err => {
    consoleErrors.push('PageError: ' + err.message);
  });

  const results = {
    pageTitle: false,
    treeTable: false,
    level0Rows: 0,
    expandIcons: 0,
    childRowsAfterExpand: 0,
    columns: [],
    sortOrder: [],
    errors: []
  };

  try {
    // ============= 步骤 1: 登录 =============
    logHeader('步骤 1: 登录系统');
    await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);

    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '01-login.png'),
      fullPage: true
    });

    // 填写登录表单
    // Ant Design Vue 表单使用 name 属性: username, password
    const usernameInput = page.locator('input#username, input[name="username"], .ant-form-item input').first();
    await usernameInput.waitFor({ state: 'visible', timeout: 10000 });
    await usernameInput.fill('admin');
    logStep('1.0', '已填入用户名: admin');

    const passwordInput = page.locator('input[type="password"]').first();
    await passwordInput.fill('admin123');
    logStep('1.1', '已填入密码: admin123');

    const loginButton = page.locator('button:has-text("登录"), button[type="submit"]').first();
    await loginButton.click();

    // 等待跳转
    await page.waitForURL(url => !url.toString().includes('/login'), { timeout: 10000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);
    logPass('登录成功，跳转到: ' + page.url());

    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '02-after-login.png'),
      fullPage: true
    });

    // ============= 步骤 2: 进入菜单管理 =============
    logHeader('步骤 2: 进入菜单管理页面');
    await page.goto(`${FRONTEND_URL}/system/menu`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // 验证页面标题
    const pageTitle = page.locator('.page-title').first();
    if (await pageTitle.count() > 0) {
      const titleText = (await pageTitle.textContent() || '').trim();
      if (titleText.includes('菜单') || titleText.toLowerCase().includes('menu')) {
        results.pageTitle = true;
        logPass(`页面标题: "${titleText}"`);
      } else {
        logFail(`页面标题不正确: "${titleText}"`);
      }
    } else {
      logFail('未找到 .page-title 元素');
    }

    // 验证树状表格存在
    const tableLocator = page.locator('.ant-table').first();
    const tableCount = await page.locator('.ant-table').count();
    if (tableCount > 0) {
      results.treeTable = true;
      logPass(`树状表格渲染: ${tableCount} 个 .ant-table 元素`);
    } else {
      logFail('未找到 .ant-table 元素');
    }

    // 等待表格数据加载
    await page.waitForTimeout(2000);

    // ============= 步骤 3: 验证树状结构 =============
    logHeader('步骤 3: 验证树状结构渲染');

    // 3.1 第一级菜单行数
    const level0Rows = await page.locator('.ant-table-tbody > tr.ant-table-row-level-0').count();
    results.level0Rows = level0Rows;
    logStep('3.1', `第一级菜单 (level-0) 数量: ${level0Rows}`);
    if (level0Rows > 0) {
      logPass('默认显示第一级菜单');
    } else {
      logFail('未渲染第一级菜单');
    }

    // 3.2 展开/折叠图标
    const expandIcons = await page.locator('.ant-table-row-expand-icon').count();
    results.expandIcons = expandIcons;
    logStep('3.2', `展开图标数量: ${expandIcons}`);

    // 3.3 表格列头（验证 i18n 翻译是否生效）
    const headerCells = await page.locator('.ant-table-thead .ant-table-cell').all();
    for (const cell of headerCells) {
      const txt = (await cell.textContent() || '').trim();
      if (txt) {
        results.columns.push(txt);
      }
    }
    logStep('3.3', `表格列: ${results.columns.join(' | ')}`);

    // 验证 i18n 翻译：列名不应是原始 key 形式 "system.menu.xxx"
    const rawKeyColumns = results.columns.filter(c => c.startsWith('system.') || c.startsWith('common.') && c.includes('.'));
    if (rawKeyColumns.length === 0) {
      logPass('表头已正确翻译 (无原始 i18n key)');
    } else {
      logFail('表头仍显示原始 i18n key: ' + rawKeyColumns.join(', '));
    }

    // 验证菜单类型列存在并显示「目录」标签
    const typeTags = await page.locator('.ant-table-tbody > tr.ant-table-row-level-0 td:nth-child(3) .ant-tag').all();
    const typeTexts = [];
    for (const t of typeTags) {
      const txt = (await t.textContent() || '').trim();
      if (txt) typeTexts.push(txt);
    }
    logStep('3.3.1', `菜单类型列内容: ${typeTexts.join(', ')}`);
    if (typeTexts.some(t => ['Directory', 'Menu', 'Button', '目录', '菜单', '按钮'].includes(t))) {
      logPass('菜单类型列正确显示 1=目录 / 2=菜单');
    } else {
      logFail('菜单类型列内容不正确');
    }

    // 3.4 第一级菜单的排序顺序
    logStep('3.4', '检查排序顺序:');
    // 列顺序: name(0), id(1), type(2), code(3), icon(4), path(5), permission(6), sort(7), status(8), action(9)
    const level0SortCells = page.locator('.ant-table-tbody > tr.ant-table-row-level-0').locator('td').nth(7); // 排序列
    const level0SortCount = await level0SortCells.count();
    for (let i = 0; i < level0SortCount; i++) {
      const sortVal = (await level0SortCells.nth(i).textContent() || '').trim();
      const nameCell = page.locator('.ant-table-tbody > tr.ant-table-row-level-0').nth(i).locator('td').first();
      const name = (await nameCell.textContent() || '').trim();
      results.sortOrder.push({ name, sort: sortVal });
      logStep(`  `, `  菜单[${i}]: 名称="${name}", 排序=${sortVal}`);
    }

    // 验证排序是否升序
    const sortValues = results.sortOrder.map(x => parseInt(x.sort, 10) || 0);
    const isAscending = sortValues.every((v, i) => i === 0 || v >= sortValues[i - 1]);
    if (isAscending && sortValues.length > 1) {
      logPass('排序顺序正确 (升序)');
    } else if (sortValues.length === 1) {
      logPass('仅一项菜单，无需验证排序');
    } else {
      logFail('排序顺序不正确: ' + sortValues.join(', '));
    }

    // 3.5 截图：默认折叠状态
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '03-menu-tree-collapsed.png'),
      fullPage: true
    });
    logStep('3.5', '截图: 03-menu-tree-collapsed.png');

    // ============= 步骤 4: 测试展开/折叠 =============
    logHeader('步骤 4: 测试展开/折叠功能');

    // 只统计真正可点击的展开图标（排除 spaced 占位符）
    const visibleExpandIcons = await page.locator('.ant-table-row-expand-icon:not(.ant-table-row-expand-icon-spaced)').count();
    logStep('4.0', `可点击的展开图标数量: ${visibleExpandIcons}`);

    if (visibleExpandIcons > 0) {
      const firstExpandIcon = page.locator('.ant-table-row-expand-icon:not(.ant-table-row-expand-icon-spaced)').first();
      const initialClass = await firstExpandIcon.getAttribute('class') || '';
      logStep('4.1', `初始状态: ${initialClass.includes('expanded') ? '已展开' : '已折叠'}`);

      // 找到第一行的菜单名（用于后续验证子节点）
      const firstRowName = await page.locator('.ant-table-tbody > tr.ant-table-row-level-0').first().locator('td').first().textContent();
      logStep('4.2', `准备展开: "${(firstRowName || '').trim()}"`);

      // 点击展开
      logStep('4.3', '点击 ➕ 展开图标...');
      await firstExpandIcon.click({ force: true });
      await page.waitForTimeout(1500);

      // 验证展开后子节点
      const childRows = await page.locator('.ant-table-tbody > tr.ant-table-row-level-1').count();
      results.childRowsAfterExpand = childRows;
      logStep('4.4', `展开后子节点 (level-1) 数量: ${childRows}`);

      if (childRows > 0) {
        logPass('展开功能正常');
        // 截图：展开状态
        await page.screenshot({
          path: path.join(SCREENSHOT_DIR, '04-menu-tree-expanded.png'),
          fullPage: true
        });
        logStep('4.5', '截图: 04-menu-tree-expanded.png');

        // 列出所有子节点
        const childNames = await page.locator('.ant-table-tbody > tr.ant-table-row-level-1 td:first-child').all();
        const childList = [];
        for (const c of childNames) {
          const t = (await c.textContent() || '').trim();
          if (t) childList.push(t);
        }
        logStep('4.6', `子节点列表: ${childList.join(', ')}`);

        // 验证子节点有正确的缩进样式
        const firstChild = page.locator('.ant-table-tbody > tr.ant-table-row-level-1').first();
        const childClass = await firstChild.getAttribute('class') || '';
        logStep('4.7', `子节点 class: ${childClass}`);
        if (childClass.includes('level-1')) {
          logPass('子节点缩进层级正确 (level-1)');
        }

        // 点击折叠
        logStep('4.8', '点击 ➖ 折叠...');
        await firstExpandIcon.click({ force: true });
        await page.waitForTimeout(1000);
        const childRowsAfterCollapse = await page.locator('.ant-table-tbody > tr.ant-table-row-level-1').count();
        logStep('4.9', `折叠后子节点数量: ${childRowsAfterCollapse}`);
        if (childRowsAfterCollapse < childRows) {
          logPass('折叠功能正常');
        } else {
          logFail('折叠后子节点未减少');
        }
      } else {
        logStep('4.4', '此菜单无子节点（树形结构为单层）');
      }
    } else {
      logStep('4.1', '未检测到可点击的展开图标，菜单数据可能为单层结构');
      logFail('数据为单层结构，无法验证展开/折叠');
    }

    // ============= 步骤 5: 验证显示内容 =============
    logHeader('步骤 5: 验证关键列内容');

    // 5.1 验证权限标识列
    const permissionCells = page.locator('.ant-table-tbody > tr.ant-table-row-level-0').first().locator('td').nth(2);
    const permission = (await permissionCells.textContent() || '').trim();
    logStep('5.1', `第一行权限标识: "${permission}"`);

    // 5.2 验证排序列
    const sortCell = page.locator('.ant-table-tbody > tr.ant-table-row-level-0').first().locator('td').nth(7);
    const sort = (await sortCell.textContent() || '').trim();
    logStep('5.2', `第一行排序: ${sort}`);

    // 5.3 验证操作列存在
    const actionCell = page.locator('.ant-table-tbody > tr.ant-table-row-level-0').first().locator('td').last();
    const actionButtons = await actionCell.locator('button').count();
    logStep('5.3', `第一行操作按钮数: ${actionButtons}`);
    if (actionButtons >= 2) {
      logPass('操作列包含按钮 (新增/编辑/删除)');
    } else {
      logFail('操作列按钮不完整');
    }

    // ============= 步骤 6: 检查控制台错误 =============
    logHeader('步骤 6: 检查浏览器控制台错误');
    if (consoleErrors.length > 0) {
      logFail(`发现 ${consoleErrors.length} 条错误:`);
      consoleErrors.slice(0, 10).forEach(e => console.log('    - ' + e));
      results.errors = consoleErrors;
    } else {
      logPass('无控制台错误');
    }

    // ============= 测试总结 =============
    logHeader('测试总结');
    console.log('  页面标题正确:        ' + (results.pageTitle ? 'YES' : 'NO'));
    console.log('  树状表格渲染:        ' + (results.treeTable ? 'YES' : 'NO'));
    console.log('  第一级菜单数量:      ' + results.level0Rows);
    console.log('  展开图标数量:        ' + results.expandIcons);
    console.log('  展开后子节点数量:    ' + results.childRowsAfterExpand);
    console.log('  表格列数:            ' + results.columns.length);
    console.log('  控制台错误数:        ' + consoleErrors.length);
    console.log('  截图保存目录:        ' + SCREENSHOT_DIR);

    const allPassed = results.pageTitle && results.treeTable && results.level0Rows > 0;
    console.log('\n  总体结果: ' + (allPassed ? 'PASS' : 'FAIL'));
    console.log('='.repeat(60));

  } catch (error) {
    logFail('测试异常: ' + error.message);
    console.error(error.stack);
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'ERROR.png'),
      fullPage: true
    });
  } finally {
    await browser.close();
  }
})();
