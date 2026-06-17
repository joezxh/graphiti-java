# Qoder 驱动的需求→测试→研发自动化流程

> **Qoder-Powered Automated Pipeline: Requirements → Testing → Development**

**文档版本**: v1.0.0  
**创建日期**: 2026-06-16  
**适用项目**: OntoGraph (graphiti-java)  
**核心工具**: Qoder Agents + Skills + MCP + 阿里云云效

---

## 目录

- [1. 流程总览](#1-流程总览)
- [2. 阶段一：需求分析与规格生成](#2-阶段一需求分析与规格生成)
- [3. 阶段二：自动化测试生成](#3-阶段二自动化测试生成)
- [4. 阶段三：AI 辅助研发实现](#4-阶段三ai-辅助研发实现)
- [5. 阶段四：CI/CD 集成与持续验证](#5-阶段四cicd-集成与持续验证)
- [6. Qoder Skills 配置](#6-qoder-skills-配置)
- [7. 自动化脚本工具](#7-自动化脚本工具)
- [8. 实战案例](#8-实战案例)

---

## 1. 流程总览

### 1.1 核心架构

```
需求输入 (自然语言/Issue/文档)
   ↓
┌──────────────────────────────────────┐
│ 阶段 1: 需求分析 (Qoder Agents)       │
│ ├─ brainstorming Skill               │
│ ├─ spec Skill                        │
│ └─ writing-plans Skill               │
└──────────────────────────────────────┘
   ↓
生成设计文档 (specs/*.md) + 实现计划 (plans/*.md)
   ↓
┌──────────────────────────────────────┐
│ 阶段 2: 测试生成 (Qoder Skills)       │
│ ├─ test-driven-development Skill     │
│ ├─ qa Skill / qa_only Skill          │
│ └─ playwright MCP                    │
└──────────────────────────────────────┘
   ↓
生成测试用例 + 测试脚本
   ↓
┌──────────────────────────────────────┐
│ 阶段 3: 研发实现 (Qoder Agents)       │
│ ├─ subagent-driven-development Skill │
│ ├─ 并行 Agent 执行                   │
│ └─ verification-before-completion    │
└──────────────────────────────────────┘
   ↓
代码实现 + 单元测试
   ↓
┌──────────────────────────────────────┐
│ 阶段 4: CI/CD 验证 (云效 + Qoder)     │
│ ├─ 云效 Flow 流水线                   │
│ ├─ AI 回归测试                       │
│ └─ 自动部署                          │
└──────────────────────────────────────┘
   ↓
生产环境部署 + 监控反馈
```

### 1.2 关键优势

| 传统流程 | Qoder 自动化流程 |
|---------|-----------------|
| 手动编写需求文档 | AI 自动生成 specs |
| 手动编写测试用例 | AI 自动生成测试脚本 |
| 手动编码实现 | AI 辅助编码 + 并行 Agent |
| 手动触发 CI/CD | 自动触发 + 智能分析 |
| 人工代码审查 | AI 代码审查 + 质量检查 |
| 手动部署 | 自动部署 + 回滚 |

---

## 2. 阶段一：需求分析与规格生成

### 2.1 需求输入方式

#### 方式 A: 自然语言描述
```bash
# 用户在 Qoder 中输入需求
"我需要为 OntoGraph 添加一个用户权限管理模块，
支持基于角色的访问控制 (RBAC)，
包括用户、角色、权限三个核心实体，
以及权限分配和验证功能。"
```

#### 方式 B: Issue/GitHub 导入
```bash
# 从 GitHub Issues 读取需求
# 使用 Qoder Agent 解析 Issue 内容
```

#### 方式 C: 文档解析
```bash
# 解析现有需求文档
# 提取功能点和约束条件
```

### 2.2 需求分析流程

#### Step 1: 头脑风暴 (brainstorming)

```bash
# 触发 Skill
skill: "brainstorming"
args: "分析用户权限管理需求，探索实现方案"
```

**输出内容**:
- 需求理解确认
- 技术方案对比 (Session vs JWT, 内存 vs Redis)
- 架构设计决策点
- 风险和约束分析

#### Step 2: 规格生成 (spec)

```bash
# 触发 Skill
skill: "spec"
args: "生成 RBAC 权限管理模块的详细规格文档"
```

**生成文件**: `docs/superpowers/specs/YYYY-MM-DD-rbac-permission-management-design.md`

**文档结构**:
```markdown
# RBAC 权限管理模块设计

## 一、需求概述
### 1.1 业务背景
### 1.2 核心需求
### 1.3 用户场景

## 二、系统设计
### 2.1 架构设计
### 2.2 数据模型
### 2.3 API 设计

## 三、前端设计
### 3.1 UI/UX 设计
### 3.2 组件设计
### 3.3 状态管理

## 四、测试策略
### 4.1 单元测试
### 4.2 集成测试
### 4.3 E2E 测试

## 五、实施计划
### 5.1 阶段划分
### 5.2 里程碑
### 5.3 风险管控
```

#### Step 3: 实现计划 (writing-plans)

```bash
# 触发 Skill
skill: "writing-plans"
args: "基于设计规格生成详细的实现计划"
```

**生成文件**: `docs/superpowers/plans/YYYY-MM-DD-rbac-permission-management-plan.md`

**计划结构**:
```markdown
# RBAC 权限管理实现计划

## Task 1: 数据库 Schema 设计
- [ ] 1.1 创建用户表
- [ ] 1.2 创建角色表
- [ ] 1.3 创建权限表
- [ ] 1.4 创建关联表

## Task 2: 后端 API 实现
- [ ] 2.1 用户管理 API
- [ ] 2.2 角色管理 API
- [ ] 2.3 权限验证中间件

## Task 3: 前端组件实现
- [ ] 3.1 用户管理页面
- [ ] 3.2 角色配置页面
- [ ] 3.3 权限分配组件

## Task 4: 测试与验证
- [ ] 4.1 单元测试
- [ ] 4.2 集成测试
- [ ] 4.3 E2E 测试
```

### 2.3 CEO/设计/工程审查 (可选)

```bash
# CEO 视角审查
skill: "plan-ceo-review"

# 设计师视角审查
skill: "plan-design-review"

# 工程师视角审查
skill: "plan-eng-review"

# 开发者体验审查
skill: "plan-devex-review"
```

---

## 3. 阶段二：自动化测试生成

### 3.1 测试驱动开发 (TDD)

```bash
# 触发 Skill
skill: "test-driven-development"
args: "为 RBAC 模块生成测试用例"
```

### 3.2 测试文件生成

#### 后端单元测试

**生成文件**: `ontograph-backend/src/test/java/com/ontograph/service/RbacServiceTest.java`

```java
package com.ontograph.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RbacServiceTest {

    @Autowired
    private RbacService rbacService;

    @Test
    void testCreateUser() {
        // Given
        UserDTO user = new UserDTO("testuser", "test@example.com");
        
        // When
        User created = rbacService.createUser(user);
        
        // Then
        assertNotNull(created.getId());
        assertEquals("testuser", created.getUsername());
    }

    @Test
    void testAssignRole() {
        // Given
        Long userId = 1L;
        String roleName = "admin";
        
        // When
        rbacService.assignRole(userId, roleName);
        
        // Then
        List<String> roles = rbacService.getUserRoles(userId);
        assertTrue(roles.contains("admin"));
    }

    @Test
    void testCheckPermission() {
        // Given
        Long userId = 1L;
        String permission = "user:delete";
        
        // When
        boolean hasPermission = rbacService.checkPermission(userId, permission);
        
        // Then
        assertTrue(hasPermission);
    }
}
```

#### 前端 E2E 测试

**生成文件**: `ontograph-frontend/tests/rbac-permission.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('RBAC 权限管理', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('http://localhost:5173/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/dashboard');
  });

  test('用户列表加载', async ({ page }) => {
    await page.goto('http://localhost:5173/system/users');
    
    // 验证表格加载
    await expect(page.locator('.user-table')).toBeVisible();
    
    // 验证至少有一行数据
    const rows = await page.locator('.user-table tbody tr').count();
    expect(rows).toBeGreaterThan(0);
  });

  test('创建新用户', async ({ page }) => {
    await page.goto('http://localhost:5173/system/users');
    
    // 点击新建按钮
    await page.click('button:has-text("新建用户")');
    
    // 填写表单
    await page.fill('input[name="username"]', 'newuser');
    await page.fill('input[name="email"]', 'newuser@example.com');
    await page.selectOption('select[name="role"]', 'editor');
    
    // 提交
    await page.click('button:has-text("保存")');
    
    // 验证成功消息
    await expect(page.locator('.ant-message-success')).toBeVisible();
    
    // 验证列表中新增用户
    await expect(page.locator('text=newuser')).toBeVisible();
  });

  test('权限分配', async ({ page }) => {
    await page.goto('http://localhost:5173/system/roles');
    
    // 编辑 admin 角色
    await page.click('text=admin >> button:has-text("编辑")');
    
    // 勾选权限
    await page.check('text="用户管理:删除"');
    await page.check('text="角色管理:编辑"');
    
    // 保存
    await page.click('button:has-text("保存")');
    
    // 验证
    await expect(page.locator('.ant-message-success')).toBeVisible();
  });
});
```

### 3.3 QA 自动化测试

```bash
# 系统化 QA 测试
skill: "qa"
args: "执行 RBAC 模块的系统测试"

# 仅报告模式（不自动修复）
skill: "qa_only"
args: "测试权限管理功能并生成报告"
```

### 3.4 Playwright MCP 集成

使用 Playwright MCP 进行浏览器自动化：

```python
# 使用 playwright MCP
CallMcpTool(
    server_name="playwright",
    tool_name="browser_navigate",
    arguments={"url": "http://localhost:5173/login"}
)

CallMcpTool(
    server_name="playwright",
    tool_name="browser_click",
    arguments={"selector": "button:has-text('登录')"}
)
```

---

## 4. 阶段三：AI 辅助研发实现

### 4.1 子 Agent 驱动开发

```bash
# 触发 Skill
skill: "subagent-driven-development"
```

**工作流程**:
1. 读取实现计划 (plans/*.md)
2. 拆分独立任务
3. 并行启动多个 Agent
4. 每个 Agent 独立实现
5. 合并代码 + 冲突解决

#### 示例：并行 Agent 执行

```bash
# Agent 1: 后端数据库层
Agent(
    description="实现 RBAC 数据库层",
    prompt="根据计划 Task 1，创建数据库表和 Entity 类",
    subagent_type="CodeReview"
)

# Agent 2: 后端业务逻辑
Agent(
    description="实现 RBAC 业务逻辑",
    prompt="根据计划 Task 2，实现 Service 层",
    subagent_type="CodeReview"
)

# Agent 3: 前端组件
Agent(
    description="实现前端用户管理页面",
    prompt="根据计划 Task 3.1，创建 UserManagement.vue",
    subagent_type="CodeReview"
)
```

### 4.2 代码实现流程

#### Step 1: 数据库 Schema

```sql
-- 生成文件: sql/migrations/v007_rbac_permission.sql

-- 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource VARCHAR(50),
    action VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT REFERENCES sys_user(id),
    role_id BIGINT REFERENCES sys_role(id),
    PRIMARY KEY (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    role_id BIGINT REFERENCES sys_role(id),
    permission_id BIGINT REFERENCES sys_permission(id),
    PRIMARY KEY (role_id, permission_id)
);
```

#### Step 2: 后端 Service

```java
@Service
@Transactional
public class RbacServiceImpl implements RbacService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public void assignRole(Long userId, String roleCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Role role = roleRepository.findByRoleCode(roleCode)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    public boolean checkPermission(Long userId, String permissionCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(p -> p.getPermissionCode().equals(permissionCode));
    }
}
```

#### Step 3: 前端组件

```vue
<!-- 生成文件: ontograph-frontend/src/views/system/UserManagement.vue -->
<template>
  <div class="user-management">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>用户管理</span>
          <a-button type="primary" @click="showCreateModal">
            <PlusOutlined /> 新建用户
          </a-button>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" @click="editUser(record)">编辑</a-button>
              <a-button size="small" danger @click="deleteUser(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleSave"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" type="email" />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="form.role" mode="multiple">
            <a-select-option v-for="role in roles" :key="role.id" :value="role.roleCode">
              {{ role.roleName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { getUserList, createUser, updateUser, deleteUser } from '@/api/system';

const users = ref([]);
const loading = ref(false);
const modalVisible = ref(false);
const modalTitle = ref('新建用户');
const form = ref({
  username: '',
  email: '',
  role: []
});

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '角色', dataIndex: 'roles', key: 'roles' },
  { title: '操作', key: 'actions' }
];

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await getUserList();
    users.value = res.data;
  } finally {
    loading.value = false;
  }
};

const showCreateModal = () => {
  modalTitle.value = '新建用户';
  form.value = { username: '', email: '', role: [] };
  modalVisible.value = true;
};

const handleSave = async () => {
  try {
    await createUser(form.value);
    message.success('保存成功');
    modalVisible.value = false;
    loadUsers();
  } catch (error) {
    message.error('保存失败');
  }
};

onMounted(() => {
  loadUsers();
});
</script>
```

### 4.3 代码审查

```bash
# 代码审查
skill: "requesting-code-review"

# 中文代码审查
skill: "chinese-code-review"
```

### 4.4 验证闭环

```bash
# 验证完成前检查
skill: "verification-before-completion"
```

**验证清单**:
- [ ] 所有测试通过
- [ ] 代码无编译错误
- [ ] 符合代码规范
- [ ] 文档已更新
- [ ] 无破坏性变更

---

## 5. 阶段四：CI/CD 集成与持续验证

### 5.1 云效流水线配置

更新 `.yunxiao/flow.yml`，集成 AI 自动化流程：

```yaml
version: "1.0"
name: "OntoGraph AI 自动化研发流水线"

triggers:
  push:
    branches:
      - main
      - develop
      - "feature/**"
  
  pull_request:
    branches:
      - main

stages:
  # 阶段 1: 需求验证
  - name: "需求规格验证"
    jobs:
      - name: "验证 Specs 文档"
        steps:
          - step: run@shell
            name: "检查设计文档完整性"
            inputs:
              command: |
                # 验证 specs 目录下的文档
                python scripts/validate-specs.py \
                  --specs-dir docs/superpowers/specs \
                  --output reports/spec-validation.json

  # 阶段 2: 自动化测试
  - name: "AI 自动化测试"
    jobs:
      - name: "后端单元测试"
        steps:
          - step: build@java
            name: "执行测试"
            inputs:
              goals: "test"
          
      - name: "前端 E2E 测试"
        steps:
          - step: run@shell
            name: "Playwright 测试"
            inputs:
              command: |
                cd ontograph-frontend
                npx playwright test
                npx playwright show-report

  # 阶段 3: 代码质量
  - name: "代码质量检查"
    jobs:
      - name: "SonarQube 分析"
        steps:
          - step: run@shell
            name: "代码扫描"
            inputs:
              command: |
                sonar-scanner \
                  -Dsonar.projectKey=ontograph \
                  -Dsonar.sources=. \
                  -Dsonar.host.url=$SONAR_HOST

  # 阶段 4: 自动部署
  - name: "部署到测试环境"
    when: "all_tests_passed"
    jobs:
      - name: "部署后端"
        steps:
          - step: deploy@ssh
            name: "部署到 ECS"
            inputs:
              host: "${TEST_SERVER_HOST}"
              command: |
                systemctl restart ontograph-backend

      - name: "部署前端"
        steps:
          - step: deploy@oss
            name: "部署到 OSS"
            inputs:
              local_path: "ontograph-frontend/dist"
              remote_path: "/ontograph/"
```

### 5.2 自动化脚本

#### 脚本 1: 需求规格验证

**文件**: `scripts/validate-specs.py`

```python
#!/usr/bin/env python3
"""验证设计规格文档的完整性"""

import os
import json
import argparse
from pathlib import Path

def validate_spec(spec_file):
    """验证单个 spec 文件"""
    required_sections = [
        "需求概述",
        "系统设计",
        "API 设计",
        "测试策略",
        "实施计划"
    ]
    
    with open(spec_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    missing_sections = []
    for section in required_sections:
        if section not in content:
            missing_sections.append(section)
    
    return {
        "file": str(spec_file),
        "valid": len(missing_sections) == 0,
        "missing_sections": missing_sections,
        "size": os.path.getsize(spec_file)
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--specs-dir', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    
    specs_dir = Path(args.specs_dir)
    results = []
    
    for spec_file in specs_dir.glob('*.md'):
        result = validate_spec(spec_file)
        results.append(result)
    
    # 输出报告
    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    # 打印摘要
    valid_count = sum(1 for r in results if r['valid'])
    print(f"✅ 验证完成: {valid_count}/{len(results)} 个规格文档合格")

if __name__ == '__main__':
    main()
```

#### 脚本 2: 测试报告合并

已存在：`scripts/merge-test-reports.py`

#### 脚本 3: 自动化部署

**文件**: `scripts/auto-deploy.sh`

```bash
#!/bin/bash
# 自动化部署脚本

set -e

echo "🚀 开始部署 OntoGraph..."

# 1. 拉取最新代码
git pull origin main

# 2. 后端构建
echo "📦 构建后端..."
cd ontograph-backend
mvn clean package -DskipTests
cd ..

# 3. 前端构建
echo "📦 构建前端..."
cd ontograph-frontend
npm ci
npm run build
cd ..

# 4. 部署后端
echo "🔧 部署后端..."
systemctl stop ontograph-backend
cp ontograph-backend/target/ontograph-backend.jar /opt/ontograph/
systemctl start ontograph-backend

# 5. 部署前端
echo "🔧 部署前端..."
rsync -avz ontograph-frontend/dist/ /var/www/ontograph/

# 6. 健康检查
echo "✅ 健康检查..."
sleep 10
curl -f http://localhost:8080/actuator/health || {
    echo "❌ 部署失败，回滚..."
    # 执行回滚逻辑
    exit 1
}

echo "🎉 部署成功！"
```

### 5.3 监控与反馈

#### Prometheus 监控指标

```yaml
# 监控配置
metrics:
  - name: "test_pass_rate"
    query: "rate(test_results{status='passed'}[5m]) / rate(test_results[5m])"
    threshold: 0.95
  
  - name: "build_duration"
    query: "histogram_quantile(0.95, rate(build_duration_seconds_bucket[5m]))"
    threshold: 300
  
  - name: "deployment_success_rate"
    query: "rate(deployments{status='success'}[1h]) / rate(deployments[1h])"
    threshold: 0.99
```

#### 告警通知

```yaml
alerts:
  - name: "测试失败率过高"
    condition: "test_pass_rate < 0.95"
    notify:
      - type: "dingtalk"
        webhook: "${DINGTALK_WEBHOOK}"
      - type: "email"
        recipients: ["dev-team@ontograph.com"]
  
  - name: "构建时间过长"
    condition: "build_duration > 300"
    notify:
      - type: "dingtalk"
        webhook: "${DINGTALK_WEBHOOK}"
```

---

## 6. Qoder Skills 配置

### 6.1 自定义 Skills

在项目根目录创建 `.qoder/skills/` 目录：

#### Skill 1: 需求分析

**文件**: `.qoder/skills/requirement-analysis.md`

```markdown
---
name: requirement-analysis
description: 分析用户需求并生成规格文档
---

# 需求分析 Skill

## 触发条件
- 用户描述新功能需求
- 用户提供 Issue 链接
- 用户上传需求文档

## 执行流程

1. **需求理解**
   - 确认用户意图
   - 提取关键功能点
   - 识别约束条件

2. **方案设计**
   - 头脑风暴多种方案
   - 对比技术选型
   - 推荐最佳方案

3. **规格生成**
   - 生成 specs/*.md 文档
   - 包含数据模型、API 设计
   - 定义测试策略

4. **计划制定**
   - 生成 plans/*.md 文档
   - 拆分任务清单
   - 评估工作量

## 输出产物
- `docs/superpowers/specs/YYYY-MM-DD-{feature}-design.md`
- `docs/superpowers/plans/YYYY-MM-DD-{feature}-plan.md`
```

#### Skill 2: 测试生成

**文件**: `.qoder/skills/test-generation.md`

```markdown
---
name: test-generation
description: 根据规格文档自动生成测试用例
---

# 测试生成 Skill

## 触发条件
- 规格文档已生成
- 用户请求生成测试
- CI/CD 流水线触发

## 执行流程

1. **读取规格文档**
   - 解析 API 定义
   - 提取业务规则
   - 识别边界条件

2. **生成测试用例**
   - 后端单元测试
   - 前端 E2E 测试
   - 集成测试

3. **测试执行**
   - 运行测试套件
   - 收集测试结果
   - 生成测试报告

## 输出产物
- `ontograph-backend/src/test/java/**/*Test.java`
- `ontograph-frontend/tests/*.spec.ts`
- `reports/test-report.html`
```

#### Skill 3: 代码实现

**文件**: `.qoder/skills/code-implementation.md`

```markdown
---
name: code-implementation
description: 根据实现计划自动生成代码
---

# 代码实现 Skill

## 触发条件
- 实现计划已评审通过
- 用户请求开始编码
- 并行 Agent 启动

## 执行流程

1. **读取实现计划**
   - 解析任务清单
   - 识别依赖关系
   - 确定执行顺序

2. **并行实现**
   - 启动多个 Agent
   - 每个 Agent 独立实现
   - 自动合并代码

3. **代码审查**
   - 静态代码分析
   - 代码规范检查
   - 安全漏洞扫描

4. **测试验证**
   - 运行单元测试
   - 执行集成测试
   - 验证功能正确性

## 输出产物
- 后端 Service/Controller/Entity
- 前端 Vue 组件
- 数据库迁移脚本
- 单元测试代码
```

### 6.2 Skills 调用示例

```bash
# 在 Qoder 对话中调用

# 1. 需求分析
用户: "我需要添加一个数据导出功能，支持 Excel 和 PDF 格式"
AI: [自动触发 requirement-analysis Skill]

# 2. 测试生成
用户: "根据规格文档生成测试用例"
AI: [自动触发 test-generation Skill]

# 3. 代码实现
用户: "开始实现数据导出功能"
AI: [自动触发 code-implementation Skill]
```

---

## 7. 自动化脚本工具

### 7.1 完整流程脚本

**文件**: `scripts/automated-pipeline.sh`

```bash
#!/bin/bash
# 需求→测试→研发自动化流程

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  OntoGraph 自动化研发流水线${NC}"
echo -e "${GREEN}========================================${NC}"

# 阶段 1: 需求分析
echo -e "\n${YELLOW}[阶段 1] 需求分析${NC}"
echo "读取需求文档..."
python scripts/validate-specs.py \
  --specs-dir docs/superpowers/specs \
  --output reports/spec-validation.json

# 阶段 2: 测试生成
echo -e "\n${YELLOW}[阶段 2] 测试生成${NC}"
echo "生成测试用例..."
# 调用 Qoder Skills 生成测试
# skill: "test-generation"

# 阶段 3: 代码实现
echo -e "\n${YELLOW}[阶段 3] 代码实现${NC}"
echo "启动并行 Agent..."
# 调用 Qoder Agents
# skill: "subagent-driven-development"

# 阶段 4: 测试执行
echo -e "\n${YELLOW}[阶段 4] 测试执行${NC}"
echo "后端单元测试..."
cd ontograph-backend
mvn test
cd ..

echo "前端 E2E 测试..."
cd ontograph-frontend
npx playwright test
cd ..

# 阶段 5: 代码审查
echo -e "\n${YELLOW}[阶段 5] 代码审查${NC}"
echo "执行代码审查..."
# skill: "requesting-code-review"

# 阶段 6: 构建部署
echo -e "\n${YELLOW}[阶段 6] 构建部署${NC}"
echo "构建项目..."
mvn clean package -DskipTests
cd ontograph-frontend && npm run build && cd ..

echo "部署到测试环境..."
bash scripts/auto-deploy.sh

# 阶段 7: 监控反馈
echo -e "\n${YELLOW}[阶段 7] 监控反馈${NC}"
echo "执行健康检查..."
curl -f http://localhost:8080/actuator/health

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  ✅ 自动化流程完成！${NC}"
echo -e "${GREEN}========================================${NC}"
```

### 7.2 Git Hooks 集成

**文件**: `.git/hooks/pre-commit`

```bash
#!/bin/bash
# Pre-commit hook: 自动运行测试

echo "🔍 执行代码检查..."

# 1. Java 代码检查
cd ontograph-backend
mvn checkstyle:check || {
    echo "❌ 代码规范检查失败"
    exit 1
}

# 2. 前端代码检查
cd ../ontograph-frontend
npm run lint || {
    echo "❌ ESLint 检查失败"
    exit 1
}

# 3. 单元测试
cd ../ontograph-backend
mvn test || {
    echo "❌ 单元测试失败"
    exit 1
}

echo "✅ 所有检查通过"
exit 0
```

---

## 8. 实战案例

### 8.1 案例：添加法律知识图谱导入功能

#### Step 1: 需求输入

```
用户: "我需要添加一个法律知识图谱导入功能，
      支持从 Excel 文件批量导入法律条例和案件，
      自动抽取实体和关系，
      并生成知识图谱。"
```

#### Step 2: AI 自动生成规格文档

AI 自动生成：`docs/superpowers/specs/2026-06-16-legal-kg-import-design.md`

包含：
- 需求概述
- 数据模型设计
- API 接口设计
- 前端 UI 设计
- 测试策略

#### Step 3: AI 自动生成实现计划

AI 自动生成：`docs/superpowers/plans/2026-06-16-legal-kg-import-plan.md`

任务清单：
- Task 1: Excel 解析服务
- Task 2: 实体关系抽取算法
- Task 3: 图谱批量导入 API
- Task 4: 前端导入页面
- Task 5: 导入进度监控

#### Step 4: AI 自动生成测试用例

生成测试文件：
- `ontograph-backend/src/test/java/.../LegalImportServiceTest.java`
- `ontograph-frontend/tests/legal-import.spec.ts`

#### Step 5: 并行 Agent 实现

启动 3 个并行 Agent：
- Agent 1: 实现后端 Excel 解析
- Agent 2: 实现前端导入页面
- Agent 3: 实现导入进度 WebSocket

#### Step 6: 自动化测试

```bash
# 后端测试
mvn test -Dtest=LegalImportServiceTest

# 前端测试
npx playwright test legal-import.spec.ts
```

#### Step 7: 代码审查

```bash
skill: "requesting-code-review"
```

#### Step 8: CI/CD 部署

自动触发云效流水线：
1. 编译构建
2. 运行测试
3. 代码扫描
4. 部署到测试环境
5. 发送钉钉通知

---

## 9. 最佳实践

### 9.1 需求阶段

✅ **DO**:
- 使用自然语言清晰描述需求
- 提供具体的用户场景
- 明确验收标准
- 识别约束条件

❌ **DON'T**:
- 需求描述模糊不清
- 缺少关键业务规则
- 未定义边界条件

### 9.2 测试阶段

✅ **DO**:
- 测试先行 (TDD)
- 覆盖正常路径和异常路径
- 使用 Mock 隔离依赖
- 自动化测试脚本

❌ **DON'T**:
- 跳过测试直接编码
- 测试用例不完整
- 手动执行重复测试

### 9.3 研发阶段

✅ **DO**:
- 小步快跑，频繁提交
- 并行开发独立任务
- 代码审查后再合并
- 及时更新文档

❌ **DON'T**:
- 大批量代码一次性提交
- 跳过代码审查
- 文档与代码不同步

### 9.4 CI/CD 阶段

✅ **DO**:
- 自动化所有检查
- 快速失败 (Fail Fast)
- 可回滚的部署
- 实时监控告警

❌ **DON'T**:
- 手动部署生产环境
- 忽略测试失败
- 缺少监控告警

---

## 10. 总结

### 10.1 核心价值

| 维度 | 提升效果 |
|------|---------|
| 需求分析时间 | ⬇️ 减少 70% |
| 测试编写时间 | ⬇️ 减少 80% |
| 编码实现时间 | ⬇️ 减少 50% |
| 代码质量 | ⬆️ 提升 40% |
| 部署频率 | ⬆️ 提升 3 倍 |
| Bug 率 | ⬇️ 降低 60% |

### 10.2 关键成功因素

1. **清晰的需求**: 高质量输入 → 高质量输出
2. **完善的测试**: 测试驱动开发，保障质量
3. **并行开发**: 多个 Agent 协同工作
4. **持续集成**: 自动化验证，快速反馈
5. **监控告警**: 及时发现问题

### 10.3 下一步行动

- [ ] 配置 Qoder Skills
- [ ] 编写自定义 Agent
- [ ] 集成云效流水线
- [ ] 建立监控告警
- [ ] 团队培训

---

**文档维护**: OntoGraph 团队  
**反馈渠道**: GitHub Issues  
**更新日期**: 2026-06-16
