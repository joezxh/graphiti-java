# Spring Boot 4.1 and Spring AI 2.0 Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the backend to Spring Boot 4.1.0 and Spring AI 2.0.0 while preserving its REST, security, graph, storage, and AI-provider behavior.

**Architecture:** The Maven BOMs become the source of truth for Spring dependencies. Third-party starters are upgraded only after their Boot 4 compatibility is proven by compilation and targeted context tests. Spring AI provider wiring is migrated behind the existing `LlmClientService` and `EmbedderService` interfaces, so controllers and graph services retain their contracts.

**Tech Stack:** Java 21, Maven, Spring Boot 4.1.0, Spring AI 2.0.0, Spring Security 7, Neo4j, PostgreSQL, Redis, Vue build verification.

---

### Task 1: Establish a reproducible upgrade baseline

**Files:**
- Modify: `ontograph-backend/pom.xml`
- Modify: `ontograph-backend/src/test/java/com/ontograph/PasswordTest.java`
- Create: `ontograph-backend/src/test/java/com/ontograph/UpgradeSmokeTest.java`

- [ ] **Step 1: Add a context smoke test that asserts the application class is loadable**

```java
package com.ontograph;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UpgradeSmokeTest {
    @Test
    void applicationContextLoads() {
    }
}
```

- [ ] **Step 2: Run the smoke test before changing dependency versions**

Run: `rtk mvn -q -Dtest=UpgradeSmokeTest test`  
Expected: PASS; if external services make it fail, record the first failure and add test properties that disable only the failing external integration.

- [ ] **Step 3: Verify the full baseline**

Run: `rtk mvn -q test` in `ontograph-backend` and `rtk pnpm build` in `ontograph-frontend`  
Expected: Maven tests and the frontend type-check/build complete without compilation errors.

- [ ] **Step 4: Commit the test baseline**

```bash
rtk git add ontograph-backend/src/test/java/com/ontograph/UpgradeSmokeTest.java
rtk git commit -m "test: add backend upgrade smoke baseline"
```

### Task 2: Move the build to the Spring 4 / AI 2 BOMs

**Files:**
- Modify: `ontograph-backend/pom.xml`

- [ ] **Step 1: Change the platform properties**

```xml
<spring.boot.version>4.1.0</spring.boot.version>
<spring.ai.version>2.0.0</spring.ai.version>
```

- [ ] **Step 2: Remove duplicate MyBatis starter declarations and preserve one Boot-compatible starter**

Keep exactly one `mybatis-plus-spring-boot3-starter` declaration; remove the duplicate `mybatis-plus-boot-starter` declaration so Maven has one source for the starter.

- [ ] **Step 3: Compile to expose migration failures**

Run: `rtk mvn -q -DskipTests compile`  
Expected: FAIL initially with missing Spring AI, third-party starter, or Jackson API symbols; capture the complete compiler output.

- [ ] **Step 4: Update all direct third-party version properties to the newest stable versions that resolve with Boot 4.1**

Run after each property update: `rtk mvn -q -DskipTests compile`  
Expected: dependency resolution completes; do not override Spring Framework, Spring Security, Jackson, or Micrometer versions managed by the Boot BOM.

- [ ] **Step 5: Commit the BOM migration once compilation succeeds**

```bash
rtk git add ontograph-backend/pom.xml
rtk git commit -m "build: upgrade to spring boot 4 and spring ai 2"
```

### Task 3: Migrate application and provider auto-configuration

**Files:**
- Modify: `ontograph-backend/src/main/java/com/ontograph/OntoGraphApplication.java`
- Modify: `ontograph-backend/src/main/resources/application.yml`
- Modify: `ontograph-backend/src/main/resources/application-dev.yml`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/config/GraphitiAiProperties.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/OpenAiLlmClientServiceImpl.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/OpenAiEmbedderServiceImpl.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/QwenLlmClientServiceImpl.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/QwenEmbedderServiceImpl.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/AnthropicLlmClientServiceImpl.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/OllamaLlmClientServiceImpl.java`

- [ ] **Step 1: Write a failing provider-isolation context test**

Create a `@SpringBootTest` using only OpenAI-compatible properties and assert the `LlmClientService` and `EmbedderService` beans are available without Azure or Mistral credentials.

- [ ] **Step 2: Run the provider-isolation test**

Run: `rtk mvn -q -Dtest=AiProviderContextTest test`  
Expected: FAIL because the 1.1 auto-configuration exclusions and 2.0 configuration binding do not match.

- [ ] **Step 3: Replace direct exclusions of provider auto-configuration classes with configuration that activates only configured providers**

Do not retain imports for Azure or Mistral auto-configuration classes. Keep OpenAI-compatible Qwen, Anthropic, and Ollama behind conditional provider selection in `GraphitiAiProperties`.

- [ ] **Step 4: Migrate Spring AI option keys and model calls to the 2.0 APIs**

Use immutable options builders and the new configuration-property names; retain the existing `LlmClientService` method signatures and error messages at the service boundary.

- [ ] **Step 5: Run provider context tests**

Run: `rtk mvn -q -Dtest=AiProviderContextTest test`  
Expected: PASS without any real provider credential or network call.

- [ ] **Step 6: Commit the provider migration**

```bash
rtk git add ontograph-backend/src/main/java/com/ontograph ontograph-backend/src/main/resources
rtk git commit -m "fix(ai): migrate provider configuration to spring ai 2"
```

### Task 4: Repair framework integration regressions

**Files:**
- Modify: `ontograph-backend/src/main/java/com/ontograph/config/SwaggerConfig.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/framework/security/config/SecurityConfig.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/framework/security/jwt/JwtAuthenticationFilter.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/typehandler/PgJsonbTypeHandler.java`
- Test: `ontograph-backend/src/test/java/com/ontograph/UpgradeSmokeTest.java`

- [ ] **Step 1: Write failing MVC tests for unauthenticated OpenAPI access and authenticated API access**

Use `MockMvc` to assert `GET /v3/api-docs` returns 200 and an authenticated protected endpoint does not return 401.

- [ ] **Step 2: Run the MVC tests**

Run: `rtk mvn -q -Dtest=SecurityAndOpenApiIntegrationTest test`  
Expected: FAIL before Spring Security 7 and springdoc compatibility fixes are applied.

- [ ] **Step 3: Update Security, springdoc, and Jackson-dependent configuration using Boot 4 APIs**

Preserve the public endpoint allow-list, JWT filter order, and `CommonResult` JSON response shape. Update JSONB serialization only where the upgraded Jackson API requires it.

- [ ] **Step 4: Run the MVC tests and affected unit tests**

Run: `rtk mvn -q -Dtest=SecurityAndOpenApiIntegrationTest,PasswordTest,OntDOTest test`  
Expected: PASS.

- [ ] **Step 5: Commit the framework compatibility repairs**

```bash
rtk git add ontograph-backend/src/main/java ontograph-backend/src/test/java
rtk git commit -m "fix: adapt framework integrations for boot 4"
```

### Task 5: Verify the complete upgraded system

**Files:**
- Modify: `README.md`
- Modify: `README_CN.md`

- [ ] **Step 1: Update documented runtime versions**

Change the backend technology-stack entries to Spring Boot 4.1.0, Spring AI 2.0.0, and the final resolved direct dependency versions.

- [ ] **Step 2: Run all backend tests and package**

Run: `rtk mvn -q test package` in `ontograph-backend`  
Expected: PASS and a runnable JAR under `target/`.

- [ ] **Step 3: Inspect dependency convergence**

Run: `rtk mvn -q dependency:tree -Dverbose` in `ontograph-backend`  
Expected: one Spring Boot 4.1.0 platform line, one Spring AI 2.0.0 platform line, and no dependency resolution errors.

- [ ] **Step 4: Build the unchanged frontend against the upgraded backend contract**

Run: `rtk pnpm build` in `ontograph-frontend`  
Expected: PASS.

- [ ] **Step 5: Commit verification and documentation**

```bash
rtk git add README.md README_CN.md
rtk git commit -m "docs: record spring boot 4 platform versions"
```
