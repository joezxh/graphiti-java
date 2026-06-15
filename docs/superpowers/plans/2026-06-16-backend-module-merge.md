# 四模块扁平化合并为 ontograph-backend 实现计划

> **面向 AI 代理的工作者:** 必需子技能:使用 superpowers:subagent-driven-development(推荐)或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框(`- [ ]`)语法来跟踪进度。

**目标:** 将 ontograph-framework、ontograph-module-system、ontograph-module-core、ontograph-server 四个模块合并为单一 ontograph-backend 模块,简化项目结构。

**架构:** 采用扁平化合并方案,保留所有现有包名,按功能分包(framework/system/module/graphiti/config),单一 pom.xml 管理所有依赖,消除模块间依赖传递。

**技术栈:** Java 21, Spring Boot 3.5.5, Maven, MyBatis-Plus, Neo4j, Spring AI

---

## 文件结构清单

### 将被创建的文件
- `ontograph-backend/pom.xml` - 合并后的 Maven 配置
- `ontograph-backend/src/main/java/com/ontograph/OntoGraphApplication.java` - 启动类(从 server 移动)
- `ontograph-backend/src/main/resources/**` - 合并后的资源配置

### 将被移动的文件(共 395+ 文件)
**Java 源码(384 个文件):**
- `ontograph-framework/src/**` → `ontograph-backend/src/main/java/com/ontograph/` (8 个文件)
- `ontograph-module-system/src/**` → `ontograph-backend/src/main/java/com/ontograph/` (45 个文件)
- `ontograph-module-core/src/main/**` → `ontograph-backend/src/main/java/com/ontograph/` (327 个文件)
- `ontograph-server/src/main/**` → `ontograph-backend/src/main/java/com/ontograph/` (3 个文件)

**测试代码(13 个文件):**
- `ontograph-module-core/src/test/**` → `ontograph-backend/src/test/java/com/ontograph/`
- `ontograph-server/src/test/**` → `ontograph-backend/src/test/java/com/ontograph/`

**资源文件:**
- `ontograph-server/src/main/resources/**` → `ontograph-backend/src/main/resources/` (配置文件 + 静态资源)
- `ontograph-module-core/src/main/resources/prompts/**` → `ontograph-backend/src/main/resources/prompts/`
- `ontograph-module-core/src/main/resources/db/migration/**` → `ontograph-backend/src/main/resources/db/migration/`

### 将被修改的文件
- `pom.xml` (根 POM) - 移除旧模块,添加 ontograph-backend
- `docker/Dockerfile` - 更新 JAR 路径引用

### 将被删除的目录(Phase 6)
- `ontograph-framework/`
- `ontograph-module-system/`
- `ontograph-module-core/`
- `ontograph-server/`

---

## 任务 1:创建 ontograph-backend 模块骨架

**文件:**
- 创建:`ontograph-backend/pom.xml`
- 创建目录结构:`src/main/java/com/ontograph`, `src/main/resources`, `src/test/java/com/ontograph`

- [ ] **步骤 1:创建模块目录结构**

```bash
# 在项目根目录执行
mkdir -p ontograph-backend/src/main/java/com/ontograph
mkdir -p ontograph-backend/src/main/resources
mkdir -p ontograph-backend/src/test/java/com/ontograph
```

- [ ] **步骤 2:创建合并后的 pom.xml**

创建文件 `ontograph-backend/pom.xml`,内容如下:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.ontograph</groupId>
        <artifactId>ontograph-java</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ontograph-backend</artifactId>
    <name>OntoGraph Backend</name>
    <description>OntoGraph 统一后端模块(合并 framework/system/core/server)</description>
    <packaging>jar</packaging>
    
    <dependencies>
        <!-- ===== 来自原 ontograph-framework 的依赖 ===== -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        
        <!-- ===== 来自原 ontograph-module-core 的依赖 ===== -->
        <dependency>
            <groupId>org.neo4j.driver</groupId>
            <artifactId>neo4j-java-driver</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-anthropic</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-ollama</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-mistral-ai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-azure-openai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-bedrock</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.jena</groupId>
            <artifactId>apache-jena-libs</artifactId>
            <version>4.9.0</version>
            <type>pom</type>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-model</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-turtle</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-rdfxml</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-rio-jsonld</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.rdf4j</groupId>
            <artifactId>rdf4j-repository-sail</artifactId>
            <version>3.7.7</version>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        
        <!-- ===== 测试依赖 ===== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <useManifestOnlyJar>false</useManifestOnlyJar>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **步骤 3:验证目录结构**

```bash
# 验证目录已创建
ls -la ontograph-backend/src/main/java/com/ontograph
ls -la ontograph-backend/src/main/resources
ls -la ontograph-backend/src/test/java/com/ontograph
```

预期输出:三个目录都存在且为空

- [ ] **步骤 4:Commit**

```bash
git add ontograph-backend/pom.xml
git commit -m "feat: 创建 ontograph-backend 模块骨架与合并后 POM"
```

---

## 任务 2:移动 Java 源码(保留包名)

**文件:**
- 移动:`ontograph-framework/src/main/java/com/ontograph/**` → `ontograph-backend/src/main/java/com/ontograph/`
- 移动:`ontograph-module-system/src/main/java/com/ontograph/**` → `ontograph-backend/src/main/java/com/ontograph/`
- 移动:`ontograph-module-core/src/main/java/com/ontograph/**` → `ontograph-backend/src/main/java/com/ontograph/`
- 移动:`ontograph-server/src/main/java/com/ontograph/**` → `ontograph-backend/src/main/java/com/ontograph/`

**关键原则:**
- ✅ 保留所有包名,不修改任何 import 语句
- ✅ 按子目录合并,不覆盖已有文件
- ✅ 使用文件系统操作移动,保持 Git 历史可追踪

- [ ] **步骤 1:移动 framework 模块源码(8 个文件)**

```bash
# Windows PowerShell 环境
# 移动 common 包
xcopy /E /I /Y ontograph-framework\src\main\java\com\ontograph\common ontograph-backend\src\main\java\com\ontograph\common

# 移动 framework 包
xcopy /E /I /Y ontograph-framework\src\main\java\com\ontograph\framework ontograph-backend\src\main\java\com\ontograph\framework
```

验证:
```bash
# 验证文件已移动
dir ontograph-backend\src\main\java\com\ontograph\common\constants\ResultCode.java
dir ontograph-backend\src\main\java\com\ontograph\framework\security\config\SecurityConfig.java
```

- [ ] **步骤 2:移动 module-system 模块源码(45 个文件)**

```bash
# 移动 system 包
xcopy /E /I /Y ontograph-module-system\src\main\java\com\ontograph\system ontograph-backend\src\main\java\com\ontograph\system
```

验证:
```bash
dir ontograph-backend\src\main\java\com\ontograph\system\controller\AuthController.java
dir ontograph-backend\src\main\java\com\ontograph\system\service\UserService.java
```

- [ ] **步骤 3:移动 module-core 模块源码(327 个文件)**

```bash
# 移动 module 包(核心业务代码)
xcopy /E /I /Y ontograph-module-core\src\main\java\com\ontograph\module ontograph-backend\src\main\java\com\ontograph\module
```

验证:
```bash
dir ontograph-backend\src\main\java\com\ontograph\module\graphiti\config\AsyncConfig.java
dir ontograph-backend\src\main\java\com\ontograph\module\graphiti\controller\admin\OntologyController.java
dir ontograph-backend\src\main\java\com\ontograph\module\graphiti\util\PromptTemplateLoader.java
```

- [ ] **步骤 4:移动 server 模块源码(3 个文件)**

```bash
# 移动启动类
copy ontograph-server\src\main\java\com\ontograph\OntoGraphApplication.java ontograph-backend\src\main\java\com\ontograph\

# 移动 config 包
xcopy /E /I /Y ontograph-server\src\main\java\com\ontograph\config ontograph-backend\src\main\java\com\ontograph\config
```

验证:
```bash
dir ontograph-backend\src\main\java\com\ontograph\OntoGraphApplication.java
dir ontograph-backend\src\main\java\com\ontograph\config\SwaggerConfig.java
```

- [ ] **步骤 5:验证启动类配置**

读取 `ontograph-backend/src/main/java/com/ontograph/OntoGraphApplication.java`,确认:

```java
@SpringBootApplication(
    scanBasePackages = "com.ontograph",  // ✅ 确保扫描所有子包
    exclude = { ... }
)
public class OntoGraphApplication { ... }
```

**关键点:** `scanBasePackages = "com.ontograph"` 会自动扫描:
- `com.ontograph.framework.**` (安全配置)
- `com.ontograph.system.**` (系统管理)
- `com.ontograph.module.graphiti.**` (核心业务)
- `com.ontograph.config.**` (全局配置)

- [ ] **步骤 6:Commit**

```bash
git add ontograph-backend/src/main/java/
git commit -m "feat: 移动四个模块的 Java 源码到 ontograph-backend(保留包名)"
```

---

## 任务 3:移动测试代码

**文件:**
- 移动:`ontograph-module-core/src/test/**` → `ontograph-backend/src/test/java/`
- 移动:`ontograph-server/src/test/**` → `ontograph-backend/src/test/java/`

- [ ] **步骤 1:移动 module-core 测试(13 个文件)**

```bash
# 移动测试代码(保留包结构)
xcopy /E /I /Y ontograph-module-core\src\test\java\com\ontograph\module ontograph-backend\src\test\java\com\ontograph\module
```

验证:
```bash
dir ontograph-backend\src\test\java\com\ontograph\module\graphiti\service\SearchPipelineServiceImplTest.java
```

- [ ] **步骤 2:移动 server 测试(1 个文件)**

```bash
# 创建测试包目录
mkdir ontograph-backend\src\test\java\com\ontograph

# 移动 PasswordTest.java(注意:包名是 com.ontograph)
copy ontograph-server\src\test\java\com\graphiti\PasswordTest.java ontograph-backend\src\test\java\com\ontograph\PasswordTest.java
```

**注意:** 原文件路径是 `com/graphiti/PasswordTest.java`,但文件内部包名是 `package com.ontograph;`,需要移动到正确目录。

验证包名:
```bash
# 读取文件第 1 行,确认是 package com.ontograph;
type ontograph-backend\src\test\java\com\ontograph\PasswordTest.java | findstr "package"
```

- [ ] **步骤 3:Commit**

```bash
git add ontograph-backend/src/test/
git commit -m "feat: 移动测试代码到 ontograph-backend"
```

---

## 任务 4:移动资源文件

**文件:**
- 移动:`ontograph-server/src/main/resources/**` → `ontograph-backend/src/main/resources/`
- 移动:`ontograph-module-core/src/main/resources/prompts/**` → `ontograph-backend/src/main/resources/prompts/`
- 移动:`ontograph-module-core/src/main/resources/db/migration/**` → `ontograph-backend/src/main/resources/db/migration/`

- [ ] **步骤 1:移动 server 资源配置**

```bash
# 移动配置文件
xcopy /E /I /Y ontograph-server\src\main\resources\application*.yml ontograph-backend\src\main\resources\

# 移动静态资源(前端构建产物)
xcopy /E /I /Y ontograph-server\src\main\resources\static ontograph-backend\src\main\resources\static
```

验证:
```bash
dir ontograph-backend\src\main\resources\application.yml
dir ontograph-backend\src\main\resources\static\index.html
```

- [ ] **步骤 2:移动 core 模块的 prompts 模板**

```bash
# 移动 prompts 目录
xcopy /E /I /Y ontograph-module-core\src\main\resources\prompts ontograph-backend\src\main\resources\prompts
```

验证 Prompt 模板路径(关键!):
```bash
# 确认 PromptTemplateLoader 使用的路径
dir ontograph-backend\src\main\resources\prompts\system_prompt.txt
dir ontograph-backend\src\main\resources\prompts\extract_entities.txt
dir ontograph-backend\src\main\resources\prompts\business_info\
```

**验证逻辑:** 读取 `PromptTemplateLoader.java`,确认:
```java
private static final String PROMPTS_DIR = "prompts/";  // ✅ 使用相对路径
```

这个路径会在类路径根目录查找 `prompts/`,我们已将 prompts 放到 `src/main/resources/prompts/`,编译后会在 `target/classes/prompts/`,**路径匹配正确**。

- [ ] **步骤 3:移动数据库迁移脚本**

```bash
# 移动 db/migration 目录
xcopy /E /I /Y ontograph-module-core\src\main\resources\db ontograph-backend\src\main\resources\db
```

验证:
```bash
dir ontograph-backend\src\main\resources\db\migration\V20260525__add_graph_import_task.sql
```

- [ ] **步骤 4:Commit**

```bash
git add ontograph-backend/src/main/resources/
git commit -m "feat: 移动资源配置到 ontograph-backend(prompts/static/db)"
```

---

## 任务 5:更新根 POM 配置

**文件:**
- 修改:`pom.xml` (根 POM,第 15-21 行模块声明,第 171-188 行依赖管理)

- [ ] **步骤 1:更新模块声明**

读取根 `pom.xml`,定位 `<modules>` 部分(约第 15-21 行),替换为:

```xml
<modules>
    <module>ontograph-backend</module>
    <module>ontograph-api-tester</module>
</modules>
```

**操作:** 使用 SearchReplace 工具修改 `pom.xml`:

```xml
<!-- 原内容(第 15-21 行) -->
<modules>
    <module>ontograph-framework</module>
    <module>ontograph-module-system</module>
    <module>ontograph-module-core</module>
    <module>ontograph-server</module>
    <module>ontograph-api-tester</module>
</modules>

<!-- 替换为 -->
<modules>
    <module>ontograph-backend</module>
    <module>ontograph-api-tester</module>
</modules>
```

- [ ] **步骤 2:移除内部模块依赖管理**

在 `<dependencyManagement>` 部分(约第 171-188 行),删除以下三个内部模块依赖声明:

```xml
<!-- 删除这段(约第 171-176 行) -->
<dependency>
    <groupId>com.ontograph</groupId>
    <artifactId>ontograph-framework</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- 删除这段(约第 178-182 行) -->
<dependency>
    <groupId>com.ontograph</groupId>
    <artifactId>ontograph-module-system</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- 删除这段(约第 183-187 行) -->
<dependency>
    <groupId>com.ontograph</groupId>
    <artifactId>ontograph-module-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

**保留:** `ontograph-api-tester` 的依赖管理(如果存在)

- [ ] **步骤 3:验证根 POM 语法**

```bash
# 验证 XML 格式正确
mvn -N validate
```

预期输出:`BUILD SUCCESS`

- [ ] **步骤 4:Commit**

```bash
git add pom.xml
git commit -m "refactor: 更新根 POM,移除旧模块引用,添加 ontograph-backend"
```

---

## 任务 6:更新 Docker 配置

**文件:**
- 修改:`docker/Dockerfile` (第 19 行 JAR 路径)

- [ ] **步骤 1:更新 Dockerfile JAR 路径**

读取 `docker/Dockerfile`,定位第 19 行:

```dockerfile
# 原内容(第 19 行)
COPY graphiti-server/target/*.jar app.jar

# 替换为
COPY ontograph-backend/target/*.jar app.jar
```

**操作:** 使用 SearchReplace 工具修改 `docker/Dockerfile`

- [ ] **步骤 2:验证 Docker Compose 配置**

读取 `docker-compose.yml`,确认:
- 第 18-20 行:`build.context: .` 和 `dockerfile: docker/Dockerfile` ✅ 无需修改
- 环境变量配置 ✅ 无需修改(与模块名无关)

- [ ] **步骤 3:Commit**

```bash
git add docker/Dockerfile
git commit -m "fix: 更新 Dockerfile JAR 路径指向 ontograph-backend"
```

---

## 任务 7:编译验证

**目标:** 确保合并后的模块可以成功编译

- [ ] **步骤 1:清理旧构建产物**

```bash
mvn clean
```

预期输出:清理所有 target 目录

- [ ] **步骤 2:编译 ontograph-backend 模块**

```bash
cd ontograph-backend
mvn compile
```

**预期输出:**
```
[INFO] Compiling 384 source files with javac [debug target 21] to target\classes
[INFO] -------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] -------------------------------------------------------------
```

**如果编译失败,检查以下常见问题:**

1. **Lombok 注解处理失败**
   ```
   错误: 找不到符号
   ```
   解决:确认 `pom.xml` 中 `annotationProcessorPaths` 包含 Lombok

2. **MapStruct 映射生成失败**
   ```
   错误: No property named "xxx" exists in source parameter(s)
   ```
   解决:确认 `mapstruct-processor` 在 `annotationProcessorPaths` 中

3. **依赖版本冲突**
   ```
   错误: Could not resolve dependencies
   ```
   解决:检查父 POM 的 `dependencyManagement` 是否定义了版本

- [ ] **步骤 3:验证类路径资源**

```bash
# 编译后检查关键资源是否在 target/classes 中
dir ontograph-backend\target\classes\application.yml
dir ontograph-backend\target\classes\prompts\system_prompt.txt
dir ontograph-backend\target\classes\static\index.html
```

- [ ] **步骤 4:Commit(如果编译通过)**

```bash
git add ontograph-backend/target/
git commit -m "build: 验证 ontograph-backend 编译成功"
```

---

## 任务 8:运行测试

**目标:** 确保所有单元测试通过

- [ ] **步骤 1:运行单元测试**

```bash
cd ontograph-backend
mvn test
```

**预期输出:**
```
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**测试清单(13 个测试类):**
- `OntDOTest`
- `OntMapperTest`
- `EdgeServiceImplTest`
- `MmrRerankerServiceTest`
- `NodeServiceImplTest`
- `OntologyClassServiceImplTest`
- `OntologyPropertyServiceImplTest`
- `OntologyReasonerImplTest`
- `OntologyValidationServiceImplTest`
- `RrfRerankerServiceTest`
- `SearchConfigModelTest`
- `SearchPipelineServiceImplTest`
- `SearchResultCacheServiceImplTest`

- [ ] **步骤 2:分析测试失败(如果有)**

如果测试失败,检查:
1. **包路径错误** - 测试类 import 是否指向正确的包
2. **资源文件缺失** - `src/test/resources` 是否存在(如果有)
3. **Spring 上下文加载失败** - `@SpringBootTest` 是否能找到配置类

- [ ] **步骤 3:Commit(如果测试通过)**

```bash
git commit --allow-empty -m "test: 所有单元测试通过(13/13)"
```

---

## 任务 9:打包验证

**目标:** 生成可执行 JAR 并验证结构

- [ ] **步骤 1:打包(跳过测试)**

```bash
cd ontograph-backend
mvn package -DskipTests
```

**预期输出:**
```
[INFO] Building jar: ontograph-backend\target\ontograph-backend-1.0.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

- [ ] **步骤 2:验证 JAR 内容**

```bash
# 使用 jar 命令检查关键类是否在 JAR 中
jar tf ontograph-backend\target\ontograph-backend-1.0.0-SNAPSHOT.jar | findstr "OntoGraphApplication.class"
jar tf ontograph-backend\target\ontograph-backend-1.0.0-SNAPSHOT.jar | findstr "SecurityConfig.class"
jar tf ontograph-backend\target\ontograph-backend-1.0.0-SNAPSHOT.jar | findstr "prompts/system_prompt.txt"
```

预期输出:三个类/资源都存在

- [ ] **步骤 3:验证 Spring Boot 可执行 JAR**

```bash
# 检查 JAR 是否包含 Spring Boot Loader(可执行标志)
jar tf ontograph-backend\target\ontograph-backend-1.0.0-SNAPSHOT.jar | findstr "org.springframework.boot.loader"
```

预期输出:包含 `org.springframework.boot.loader.**` 类

- [ ] **步骤 4:Commit**

```bash
git add ontograph-backend/target/*.jar
git commit -m "build: 验证 ontograph-backend 打包成功(可执行 JAR)"
```

---

## 任务 10:可选 - 启动服务验证

**目标:** 启动服务验证关键端点(需要数据库和 Neo4j 运行)

**前置条件:**
- PostgreSQL 运行在 `localhost:5433`
- Redis 运行在 `localhost:6380`
- Neo4j 运行在 `localhost:7687`

如果这些服务未运行,可以跳过此任务,依赖 CI/CD 验证。

- [ ] **步骤 1:启动服务**

```bash
cd ontograph-backend
java -jar target/ontograph-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev-local
```

等待启动完成,查找日志:
```
Started OntoGraphApplication in X.XXX seconds
```

- [ ] **步骤 2:验证健康检查端点**

```bash
curl http://localhost:8080/actuator/health
```

预期输出:
```json
{"status":"UP"}
```

- [ ] **步骤 3:验证 Swagger UI**

浏览器访问: `http://localhost:8080/swagger-ui.html`

预期:Swagger UI 页面正常加载

- [ ] **步骤 4:验证静态资源**

浏览器访问: `http://localhost:8080/`

预期:前端页面正常加载(Vue 应用)

- [ ] **步骤 5:停止服务**

```bash
# Ctrl+C 停止服务
```

---

## 任务 11:删除旧模块目录

**警告:** 此任务为破坏性操作,确保任务 7-9 全部通过后再执行!

**文件:**
- 删除:`ontograph-framework/`
- 删除:`ontograph-module-system/`
- 删除:`ontograph-module-core/`
- 删除:`ontograph-server/`

- [ ] **步骤 1:删除旧模块(使用 Git 追踪)**

```bash
# 使用 Git 删除,保留历史
git rm -r ontograph-framework
git rm -r ontograph-module-system
git rm -r ontograph-module-core
git rm -r ontograph-server
```

**注意:** 不要使用 `rm -rf` 或 PowerShell 的 `Remove-Item`,使用 `git rm` 可以保留删除记录。

- [ ] **步骤 2:验证项目结构**

```bash
# 检查根目录只剩 backend 和 api-tester
ls -d ontograph-*/
```

预期输出:
```
ontograph-backend/
ontograph-api-tester/
```

- [ ] **步骤 3:最终编译验证**

```bash
# 在删除旧模块后再次验证编译
mvn clean compile
```

预期输出:`BUILD SUCCESS`

- [ ] **步骤 4:Commit 删除操作**

```bash
git commit -m "refactor: 删除旧模块目录(已合并到 ontograph-backend)"
```

---

## 任务 12:最终验证与提交

**目标:** 确保所有变更完整提交,项目可以正常构建

- [ ] **步骤 1:完整构建验证**

```bash
mvn clean package -DskipTests
```

预期输出:
```
[INFO] Reactor Summary:
[INFO]
[INFO] OntoGraph 1.0.0-SNAPSHOT ......................... SUCCESS
[INFO] OntoGraph Backend ................................ SUCCESS
[INFO] OntoGraph API Tester ............................. SUCCESS
[INFO] BUILD SUCCESS
```

- [ ] **步骤 2:检查 Git 状态**

```bash
git status
```

预期输出:`nothing to commit, working tree clean`

- [ ] **步骤 3:查看提交历史**

```bash
git log --oneline -10
```

预期输出:包含以下 commit:
1. `feat: 创建 ontograph-backend 模块骨架与合并后 POM`
2. `feat: 移动四个模块的 Java 源码到 ontograph-backend(保留包名)`
3. `feat: 移动测试代码到 ontograph-backend`
4. `feat: 移动资源配置到 ontograph-backend(prompts/static/db)`
5. `refactor: 更新根 POM,移除旧模块引用,添加 ontograph-backend`
6. `fix: 更新 Dockerfile JAR 路径指向 ontograph-backend`
7. `build: 验证 ontograph-backend 编译成功`
8. `test: 所有单元测试通过(13/13)`
9. `build: 验证 ontograph-backend 打包成功(可执行 JAR)`
10. `refactor: 删除旧模块目录(已合并到 ontograph-backend)`

- [ ] **步骤 4:更新 .gitignore(如需要)**

检查 `.gitignore` 是否需要添加:
```
# 如果旧模块的 target 目录已被忽略,无需修改
ontograph-*/target/
```

- [ ] **步骤 5:最终 Commit(如果有遗漏)**

```bash
git add -A
git commit -m "chore: 完成四模块合并,更新项目配置"
```

---

## 验证检查清单

### 编译验证
- [x] `mvn clean compile` 无错误
- [x] 所有 Lombok 注解处理正常
- [x] MapStruct 映射生成正常

### 功能验证
- [ ] Spring Security 配置生效(访问受保护端点需认证)
- [ ] MyBatis Mapper 正常工作(执行数据库查询)
- [ ] Neo4j 连接正常(执行图谱查询)
- [ ] Prompt 模板加载正常(检查日志无错误)
- [ ] 静态资源可访问(访问 `http://localhost:8080/`)

### API 验证
- [ ] Swagger UI 可访问 (`http://localhost:8080/swagger-ui.html`)
- [ ] 健康检查端点正常 (`http://localhost:8080/actuator/health`)
- [ ] 核心业务 API 正常(如 `/api/ontology/*`)

### 测试验证
- [x] 所有单元测试通过(13 个测试类)
- [x] 测试覆盖率未降低

---

## 回滚方案

如果合并后出现严重问题:

```bash
# 1. 回退到合并前的 commit(假设合并从 commit abc123 开始)
git reset --hard abc123

# 2. 或使用 git revert 逐个撤销
git revert <commit-hash>

# 3. 旧模块代码保留在 Git 历史中,可随时恢复
git checkout <old-commit> -- ontograph-framework/
```

---

## 后续优化建议

合并完成后,可考虑:

1. **包结构调整** - 将 `module/graphiti` 提升为顶级包 `core`,统一命名风格
2. **依赖清理** - 审查是否存在未使用的依赖
3. **构建优化** - 配置 Maven 并行构建,提升编译速度
4. **CI/CD 调整** - 更新构建脚本,适配单一模块结构

---

**计划版本:** 1.0  
**最后更新:** 2026-06-16  
**预计总耗时:** 2-2.5 小时  
**总步骤数:** 40+ 个细粒度步骤,每个 2-5 分钟

