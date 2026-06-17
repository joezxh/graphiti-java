# Qoder 自动化流程架构图

> **Architecture Diagram: Qoder-Powered Automated Pipeline**

---

## 1. 整体架构

```mermaid
graph TB
    A[需求输入] --> B[Qoder 需求分析]
    B --> C[生成规格文档]
    C --> D[生成实现计划]
    D --> E[生成测试用例]
    E --> F[并行 Agent 开发]
    F --> G[代码审查]
    G --> H[自动化测试]
    H --> I{测试通过?}
    I -->|是| J[构建部署]
    I -->|否| K[问题修复]
    K --> H
    J --> L[监控告警]
    L --> M[团队通知]
```

---

## 2. 详细流程

### 2.1 需求分析阶段

```mermaid
graph LR
    A[用户需求] --> B{需求类型}
    B -->|新功能| C[brainstorming]
    B -->|Bug 修复| D[investigate]
    B -->|优化| E[分析影响范围]
    
    C --> F[技术方案对比]
    D --> F
    E --> F
    
    F --> G[spec Skill]
    G --> H[生成 specs/*.md]
    
    H --> I[writing-plans Skill]
    I --> J[生成 plans/*.md]
    
    J --> K{需要审查?}
    K -->|是| L[CEO/设计/工程审查]
    K -->|否| M[进入开发]
    
    L --> M
```

### 2.2 测试生成阶段

```mermaid
graph TB
    A[规格文档] --> B[test-driven-development]
    A --> C[实现计划]
    
    B --> D[分析 API 定义]
    C --> D
    
    D --> E[提取业务规则]
    E --> F[识别边界条件]
    
    F --> G[生成后端单元测试]
    F --> H[生成前端 E2E 测试]
    F --> I[生成集成测试]
    
    G --> J[RbacServiceTest.java]
    H --> K[rbac.spec.ts]
    I --> L[ApiIntegrationTest.java]
    
    J --> M[运行测试]
    K --> M
    L --> M
    
    M --> N{测试通过?}
    N -->|否| O[记录失败用例]
    N -->|是| P[进入开发阶段]
```

### 2.3 并行开发阶段

```mermaid
graph TB
    A[实现计划] --> B[解析任务清单]
    B --> C[识别依赖关系]
    C --> D[拆分独立任务]
    
    D --> E[Task 1: 数据库层]
    D --> F[Task 2: 业务逻辑]
    D --> G[Task 3: API 层]
    D --> H[Task 4: 前端组件]
    
    E --> I[Agent 1]
    F --> J[Agent 2]
    G --> K[Agent 3]
    H --> L[Agent 4]
    
    I --> M[并行执行]
    J --> M
    K --> M
    L --> M
    
    M --> N[代码合并]
    N --> O[冲突解决]
    O --> P[代码审查]
    P --> Q[提交代码]
```

### 2.4 CI/CD 流水线

```mermaid
graph TB
    A[git push] --> B[云效触发]
    B --> C[阶段 1: 代码质量]
    C --> D[Java 编译检查]
    C --> E[前端 ESLint]
    C --> F[TypeScript 检查]
    
    D --> G[阶段 2: 单元测试]
    E --> G
    F --> G
    
    G --> H[后端单元测试]
    H --> I[生成测试报告]
    
    I --> J[阶段 3: AI 前端测试]
    J --> K[构建前端]
    K --> L[Playwright E2E]
    
    L --> M[阶段 4: AI API 测试]
    M --> N[启动测试数据库]
    N --> O[启动后端服务]
    O --> P[API 自动化测试]
    
    P --> Q[阶段 5: 回归测试]
    Q --> R[核心功能测试]
    
    R --> S{所有测试通过?}
    S -->|是| T[阶段 6: 报告通知]
    S -->|否| U[标记失败]
    
    T --> V[合并测试报告]
    V --> W[钉钉通知]
    W --> X[阶段 7: 部署]
    
    X --> Y[部署前端到 OSS]
    X --> Z[部署后端到 ECS]
    
    Y --> AA[健康检查]
    Z --> AA
    
    AA --> AB{部署成功?}
    AB -->|是| AC[完成]
    AB -->|否| AD[自动回滚]
```

---

## 3. Qoder Skills 工作流

### 3.1 Skills 调用顺序

```mermaid
graph LR
    A[需求输入] --> B(brainstorming)
    B --> C(spec)
    C --> D(writing-plans)
    D --> E(test-driven-development)
    E --> F(subagent-driven-development)
    F --> G(requesting-code-review)
    G --> H(qa)
    H --> I(verification-before-completion)
    I --> J(ship)
```

### 3.2 并行 Skills

```mermaid
graph TB
    A[实现计划] --> B{任务拆分}
    
    B --> C[Agent 1: 后端开发]
    B --> D[Agent 2: 前端开发]
    B --> E[Agent 3: 测试编写]
    
    C --> F(CodeReview)
    D --> F
    E --> F
    
    F --> G{审查通过?}
    G -->|是| H[合并代码]
    G -->|否| I[修复问题]
    I --> F
    
    H --> J(qa)
    J --> K{测试通过?}
    K -->|是| L[完成]
    K -->|否| M[investigate]
    M --> C
```

---

## 4. 数据流

### 4.1 文件产物

```mermaid
graph TB
    A[需求输入] --> B[specs/*.md]
    B --> C[plans/*.md]
    C --> D[测试代码]
    C --> E[实现代码]
    
    D --> F[后端单元测试]
    D --> G[前端 E2E 测试]
    
    E --> H[Entity/DAO]
    E --> I[Service]
    E --> J[Controller]
    E --> K[Vue 组件]
    
    F --> L[测试报告]
    G --> L
    L --> M[HTML 报告]
    L --> N[JSON 数据]
    
    H --> O[数据库迁移]
    I --> P[JAR 包]
    J --> P
    K --> Q[静态资源]
    
    P --> R[部署产物]
    Q --> R
```

### 4.2 配置流

```mermaid
graph LR
    A[.env] --> B[环境变量]
    C[.yunxiao/flow.yml] --> D[CI/CD 配置]
    E[pom.xml] --> F[构建配置]
    G[package.json] --> H[前端配置]
    
    B --> I[运行时配置]
    D --> I
    F --> I
    H --> I
    
    I --> J[应用启动]
```

---

## 5. 监控与反馈

### 5.1 监控指标

```mermaid
graph TB
    A[应用运行] --> B[Prometheus 采集]
    B --> C[指标存储]
    
    C --> D[构建成功率]
    C --> E[测试通过率]
    C --> F[部署成功率]
    C --> G[响应时间]
    C --> H[错误率]
    
    D --> I[Grafana 仪表盘]
    E --> I
    F --> I
    G --> I
    H --> I
    
    I --> J{指标异常?}
    J -->|是| K[触发告警]
    J -->|否| L[正常监控]
    
    K --> M[钉钉通知]
    K --> N[邮件通知]
    K --> O[自动回滚]
```

### 5.2 反馈闭环

```mermaid
graph LR
    A[用户反馈] --> B{问题类型}
    
    B -->|Bug| C[investigate]
    B -->|需求| D[brainstorming]
    B -->|优化| E[分析影响]
    
    C --> F[根因分析]
    D --> F
    E --> F
    
    F --> G[生成修复方案]
    G --> H[执行修复]
    H --> I[验证测试]
    I --> J[部署上线]
    J --> K[用户确认]
    K --> L{问题解决?}
    
    L -->|是| M[关闭问题]
    L -->|否| C
```

---

## 6. 部署架构

### 6.1 环境划分

```mermaid
graph TB
    A[开发环境] -->|git push| B[云效 CI/CD]
    B --> C{分支判断}
    
    C -->|feature/*| D[测试环境]
    C -->|develop| E[预发布环境]
    C -->|main| F[生产环境]
    
    D --> G[自动部署]
    E --> H[手动审批 + 部署]
    F --> I[定时部署 + 审批]
    
    G --> J[测试验证]
    H --> J
    I --> J
    
    J --> K[监控告警]
```

### 6.2 服务部署

```mermaid
graph TB
    A[构建产物] --> B{部署类型}
    
    B -->|前端| C[OSS 静态托管]
    B -->|后端| D[ECS 服务器]
    
    C --> E[CDN 加速]
    E --> F[用户访问]
    
    D --> G[Spring Boot JAR]
    G --> H[systemd 管理]
    H --> I[Nginx 反向代理]
    I --> F
    
    D --> J[数据库连接]
    J --> K[PostgreSQL]
    J --> L[Neo4j]
    
    D --> M[健康检查]
    M --> N[/actuator/health]
    N --> O{健康?}
    O -->|是| P[正常服务]
    O -->|否| Q[自动重启]
```

---

## 7. 故障恢复

### 7.1 自动回滚

```mermaid
graph TB
    A[部署开始] --> B[备份当前版本]
    B --> C[部署新版本]
    C --> D[健康检查]
    
    D --> E{检查通过?}
    E -->|是| F[部署完成]
    E -->|否| G[触发回滚]
    
    G --> H[恢复备份版本]
    H --> I[验证回滚]
    I --> J{回滚成功?}
    
    J -->|是| K[通知团队]
    J -->|否| L[人工介入]
    
    K --> M[记录故障]
    L --> M
```

### 7.2 故障诊断

```mermaid
graph LR
    A[故障发生] --> B[investigate Skill]
    B --> C[收集日志]
    C --> D[分析堆栈]
    D --> E[定位根因]
    
    E --> F{问题类型}
    F -->|代码 Bug| G[生成修复]
    F -->|配置错误| H[修正配置]
    F -->|资源不足| I[扩容资源]
    F -->|依赖故障| J[修复依赖]
    
    G --> K[验证修复]
    H --> K
    I --> K
    J --> K
    
    K --> L[部署修复]
    L --> M[监控验证]
    M --> N[故障关闭]
```

---

## 8. 团队协作

### 8.1 角色分工

```mermaid
graph TB
    A[产品经理] -->|需求输入| B(Qoder 需求分析)
    B --> C[生成规格文档]
    
    C --> D[技术负责人]
    D -->|审查| E{规格通过?}
    
    E -->|是| F[开发工程师]
    E -->|否| G[修改规格]
    G --> D
    
    F -->|并行开发| H[实现代码]
    H --> I[测试工程师]
    I -->|自动化测试| J{测试通过?}
    
    J -->|是| K[运维工程师]
    J -->|否| F
    
    K -->|部署上线| L[监控告警]
    L --> M[产品经理]
```

### 8.2 协作流程

```mermaid
graph LR
    A[晨会] --> B[分配任务]
    B --> C[独立开发]
    C --> D[代码审查]
    D --> E[合并代码]
    E --> F[自动化测试]
    F --> G[部署测试环境]
    G --> H[集成测试]
    H --> I[验收]
    I --> J[部署生产]
    J --> K[回顾会议]
    K --> A
```

---

## 9. 性能优化

### 9.1 构建优化

```mermaid
graph TB
    A[构建触发] --> B{缓存检查}
    B -->|有缓存| C[增量构建]
    B -->|无缓存| D[全量构建]
    
    C --> E[编译变更代码]
    D --> F[编译全部代码]
    
    E --> G[运行测试]
    F --> G
    
    G --> H{测试并行化}
    H -->|是| I[多线程测试]
    H -->|否| J[单线程测试]
    
    I --> K[生成报告]
    J --> K
    
    K --> L[构建完成]
```

### 9.2 测试优化

```mermaid
graph LR
    A[测试套件] --> B{测试分类}
    
    B -->|单元测试| C[快速测试 < 1min]
    B -->|集成测试| D[中速测试 < 5min]
    B -->|E2E 测试| E[慢速测试 < 15min]
    
    C --> F[每次提交运行]
    D --> G[每天运行]
    E --> H[每周运行]
    
    F --> I[快速反馈]
    G --> J[深度验证]
    H --> K[全面保障]
```

---

## 10. 安全合规

### 10.1 安全检查

```mermaid
graph TB
    A[代码提交] --> B[静态代码分析]
    B --> C[SonarQube 扫描]
    B --> D[安全漏洞扫描]
    
    C --> E{代码质量达标?}
    D --> F{无安全漏洞?}
    
    E -->|是| G[继续流水线]
    E -->|否| H[阻断流水线]
    
    F -->|是| G
    F -->|否| H
    
    H --> I[生成安全报告]
    I --> J[通知开发团队]
    J --> K[修复安全问题]
    K --> A
```

### 10.2 合规检查

```mermaid
graph LR
    A[代码变更] --> B{变更类型}
    
    B -->|数据库| C[数据迁移审查]
    B -->|API| D[API 兼容性审查]
    B -->|配置| E[配置安全审查]
    B -->|依赖| F[依赖许可证审查]
    
    C --> G[合规验证]
    D --> G
    E --> G
    F --> G
    
    G --> H{合规通过?}
    H -->|是| I[继续部署]
    H -->|否| J[阻断部署]
```

---

## 总结

以上架构图展示了 Qoder 自动化流程的完整体系，包括：

1. **整体架构**: 从需求到部署的端到端流程
2. **详细流程**: 各阶段的详细步骤和决策点
3. **Skills 工作流**: Qoder Skills 的调用顺序和并行策略
4. **数据流**: 文件产物和配置流转
5. **监控反馈**: 实时监控和故障恢复
6. **部署架构**: 多环境部署和服务管理
7. **故障恢复**: 自动回滚和故障诊断
8. **团队协作**: 角色分工和协作流程
9. **性能优化**: 构建和测试优化策略
10. **安全合规**: 安全检查和合规验证

这些架构图可以帮助团队理解自动化流程的全貌，识别优化点，并持续改进研发效率。

---

**文档版本**: v1.0.0  
**创建日期**: 2026-06-16  
**维护者**: OntoGraph 团队
