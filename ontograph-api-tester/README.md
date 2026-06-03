# OntoGraph API Tester

独立自动化API测试工具，用于全面验证OntoGraph后端所有RESTful接口。支持OpenAPI自动发现、JWT认证、端到端CRUD流程测试、三端报告输出（控制台/HTML/Markdown）。

## 功能特性

| 特性 | 说明 |
|------|------|
| **接口自动发现** | 解析OpenAPI v3规范，自动生成端点清单 |
| **手动端点注册** | 预定义28+控制器、100+端点的完整测试用例 |
| **JWT认证** | 完整模拟登录/Token刷新/登出流程 |
| **环境适配** | 通过环境变量或配置文件动态指定目标服务 |
| **端到端CRUD** | 按SETUP→CREATE→READ→UPDATE→DELETE阶段顺序执行 |
| **自动数据清理** | 测试后自动删除创建的测试数据 |
| **重试与连接池** | Apache HttpClient 5连接池 + 可配置重试策略 |
| **JSON Path断言** | 支持字段值/状态码/响应时间/Header等多种断言 |
| **彩色控制台** | 实时进度条 + ANSI颜色高亮 + 失败追踪 |
| **HTML报告** | 深色主题美观报告，支持直接在浏览器打开 |
| **Markdown报告** | 适合CI/CD集成的文本报告格式 |
| **完全隔离** | 独立Maven模块，与项目JUnit测试互不干扰 |

## 快速开始

### 前提条件

- Java 21+
- Maven 3.8+
- OntoGraph后端服务运行中

### 编译

```bash
cd ontograph-api-tester
mvn clean compile
```

### 运行

**方式1：通过Maven运行**

```bash
mvn compile exec:java \
  -Dexec.mainClass="com.ontograph.tester.OntographApiTester"
```

**方式2：打包后运行**

```bash
mvn clean package -DskipTests
java -jar target/ontograph-api-tester-1.0.0-SNAPSHOT.jar
```

### 环境变量配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `TEST_SERVER_URL` | `http://localhost:8080` | 目标服务地址 |
| `TEST_AUTH_USERNAME` | `admin` | 登录用户名 |
| `TEST_AUTH_PASSWORD` | `admin123` | 登录密码 |
| `TEST_HTTP_TIMEOUT_MS` | `30000` | HTTP超时（毫秒） |
| `TEST_CONCURRENCY` | `0` | 并发数（0=串行） |
| `TEST_AUTO_CLEANUP` | `true` | 是否自动清理测试数据 |
| `TEST_REPORT_DIR` | `./test-reports` | 报告输出目录 |
| `TEST_REPORT_MD` | `true` | 生成Markdown报告 |
| `TEST_REPORT_HTML` | `true` | 生成HTML报告 |
| `TEST_MODULES` | 全部启用 | 启用的模块（逗号分隔） |

**示例：**

```bash
# 测试远程环境
TEST_SERVER_URL=http://staging.ontograph.com \
TEST_AUTH_USERNAME=admin \
TEST_AUTH_PASSWORD=secret \
TEST_MODULES=auth,graph,node,search \
java -jar ontograph-api-tester-1.0.0-SNAPSHOT.jar

# 仅测试搜索模块
TEST_MODULES=search java -jar ontograph-api-tester-1.0.0-SNAPSHOT.jar
```

### 配置文件

也可通过 `api-tester.yml` 配置文件进行详细配置。复制 `src/main/resources/api-tester.yml` 到运行目录后修改。

## 测试模块

| 模块 | 端点数量 | 测试内容 |
|------|---------|---------|
| 认证 (auth) | 3 | 登录/获取信息/登出 |
| 图谱管理 (graph) | 11 | CRUD/统计/社区/克隆 |
| 节点管理 (node) | 7 | CRUD/边关联/剧集关联 |
| 边管理 (edge) | 6 | CRUD/节点间关系 |
| 剧集管理 (episode) | 4 | CRUD/详情/提及 |
| 搜索 (search) | 6 | 全局/图谱内/混合/语义/BFS |
| 搜索管道 (searchPipeline) | 2 | 管道执行/重排 |
| 数据导入 (dataImport) | 6 | 单条/批量/消息/三元组/抽取 |
| 本体管理 (ontology) | 5 | 定义/类/属性/一致性 |
| 提示管理 (prompt) | 2 | 模板列表/类型 |
| 法律图谱 (legal) | - | 导入/导出 |
| 业务信息 (businessInfo) | - | 本体生成/草稿/优化 |

## 报告示例

### 控制台输出

```
━━━ 认证与准备 (3用例) ━━━
[PASS] POST /api/v1/auth/login 245ms
[FAIL] GET  /api/v1/auth/info 120ms
  原因: JSON路径 $.code 期望 EQUALS 200，实际 401
[SKIP] POST /api/v1/auth/logout 50ms

━━━ 创建资源 (5用例) ━━━
...
════════════════════════════════════════════════════
                    测试报告摘要
════════════════════════════════════════════════════

  通过率: 87.50%
  总耗时: 12.34s
  失败用例详情 (3个):
    1. [auth] 获取用户信息
       原因: Token过期
```

### HTML报告

深色主题，包含：
- 顶部导航栏（概览/模块/详情/失败锚点跳转）
- Hero区：目标服务 + 通过率
- 统计卡片：总用例/通过/失败/成功率/耗时
- 模块网格：每个模块的通过率、失败数、平均响应时间
- 详情表格：按阶段分组，显示每个用例的状态/方法/路径/耗时
- 失败卡片：HTTP状态/堆栈/失败断言详情

## 架构设计

```
OntographApiTester (Main)
    │
    ├── ConfigLoader          加载 api-tester.yml + 环境变量覆盖
    │
    ├── ApiHttpClient         Apache HttpClient 5 连接池
    │       ├── 连接池管理 (50连接, 每路由10)
    │       ├── 重试策略 (3次, 1s间隔)
    │       └── SSL跳过验证
    │
    ├── JwtAuthManager        JWT Token生命周期
    │       ├── login()       POST /api/v1/auth/login
    │       ├── ensureToken() 自动刷新
    │       └── logout()      POST /api/v1/auth/logout
    │
    ├── OpenApiDiscoveryService  解析 /v3/api-docs
    │
    ├── ManualEndpointRegistry  手动注册端点 & 用例
    │
    ├── AssertionEngine        JSON Path断言引擎
    │       ├── STATUS_CODE
    │       ├── JSON_PATH / NOT_NULL / EXISTS
    │       ├── RESPONSE_TIME
    │       └── BODY_CONTAINS
    │
    ├── TestRunner            核心编排器
    │       ├── 前置检查 (服务可达)
    │       ├── 认证阶段
    │       ├── 发现阶段
    │       └── 按阶段执行 (SETUP→CREATE→READ→UPDATE→DELETE→CLEANUP)
    │
    ├── TestDataCleanupService  逆序删除创建的测试数据
    │
    └── Reporters             多端报告输出
            ├── ConsoleReporter  ANSI彩色控制台
            ├── MarkdownReporter  .md 文件
            └── HtmlReporter     .html 文件 (深色主题)
```

## 测试报告输出

```
test-reports/
├── api-test-report.md    # Markdown格式（适合CI/CD）
└── api-test-report.html  # HTML格式（适合人工审阅）
```

## 扩展开发

### 添加新的手动测试用例

在 `ManualEndpointRegistry` 中注册：

```java
registry.put("myModule", List.of(
    createCase("my_test", "我的测试", TestCase.TestPhase.READ,
        TestEndpoint.HttpMethod.GET, "/api/v1/my/resource", "myModule",
        tc -> {
            tc.getAssertions().add(TestCase.Assertion.builder()
                .type(TestCase.Assertion.ASSERTION_TYPE.STATUS_CODE)
                .expectedValue(200)
                .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                .build());
            tc.getAssertions().add(TestCase.Assertion.builder()
                .type(TestCase.Assertion.ASSERTION_TYPE.JSON_PATH)
                .jsonPath("$.data.id")
                .operator(TestCase.Assertion.ComparisonOperator.NOT_EQUALS)
                .expectedValue(null)
                .build());
        },
        Collections.emptyMap()
    )
));
```

### 添加自定义报告格式

实现 `TestReporter` 接口：

```java
public class JsonReporter implements TestReporter {
    @Override
    public void onTestRunComplete(TestReport report) {
        // 输出JSON格式报告
    }
}
```

## 依赖说明

本模块使用完全独立的依赖树，零污染主项目：

- `org.apache.httpcomponents.client5:httpclient5` - HTTP客户端
- `com.fasterxml.jackson.*` - JSON序列化
- `com.jayway.jsonpath:json-path` - JSON Path断言
- `org.springdoc:springdoc-openapi-core` - OpenAPI解析
- `org.projectlombok:lombok` - 代码生成
- `ch.qos.logback:logback-classic` - 日志
- `org.yaml:snakeyaml` - YAML配置

**不依赖**：Spring Boot, Spring MVC, MyBatis, Neo4j Driver 等主项目组件。
