# AI Provider Contract Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Anthropic and Mistral selectable, working providers with regression coverage, then synchronize provider documentation and record the Jackson 3 migration work.

**Architecture:** Provider adapters keep the existing `LlmClientService` and `EmbedderService` contracts. Spring AI model auto-configuration supplies provider models, while `graphiti.ai` property conditions ensure exactly one application adapter is selected per capability.

**Tech Stack:** Spring Boot 4.1, Spring AI 2.0, JUnit 6, AssertJ.

---

### Task 1: Specify provider selection with failing tests

**Files:**
- Create: `ontograph-backend/src/test/java/com/ontograph/module/graphiti/config/AiProviderContractTest.java`
- Modify: `ontograph-backend/src/main/java/com/ontograph/OntoGraphApplication.java`

- [x] **Step 1: Add context tests that set `graphiti.ai.llm-provider=anthropic` and `mistral`, and assert their selected `LlmClientService#getProvider()` values.**
- [x] **Step 2: Run `rtk mvn -q -Dtest=AiProviderContractTest test`; verify it fails because Anthropic and Mistral auto-configuration are excluded.**
- [x] **Step 3: Remove the Anthropic and Mistral exclusions from `OntoGraphApplication`.**
- [x] **Step 4: Rerun the focused test and keep provider credentials as test-only placeholders so no request leaves the JVM.**

### Task 2: Add Mistral service adapters

**Files:**
- Create: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/MistralLlmClientServiceImpl.java`
- Create: `ontograph-backend/src/main/java/com/ontograph/module/graphiti/service/impl/ai/MistralEmbedderServiceImpl.java`
- Modify: `ontograph-backend/src/test/java/com/ontograph/module/graphiti/config/AiProviderContractTest.java`

- [x] **Step 1: Add a failing assertion that `embedding-provider=mistral` exposes `EmbedderService#getProvider()` as `mistral`.**
- [x] **Step 2: Implement `MistralLlmClientServiceImpl` with `MistralAiChatModel`, the existing chat methods, and `@ConditionalOnProperty(... llm-provider=mistral)`.**
- [x] **Step 3: Implement `MistralEmbedderServiceImpl` with `MistralAiEmbeddingModel`, the existing embedding methods, and `@ConditionalOnProperty(... embedding-provider=mistral)`.**
- [x] **Step 4: Run `rtk mvn -q -Dtest=AiProviderContractTest test`, then `rtk mvn -q test`.**

### Task 3: Synchronize configurations and documentation

**Files:**
- Modify: `ontograph-backend/src/main/resources/application-dev.yml`
- Modify: `README.md`
- Modify: `README_CN.md`
- Modify: `docker/docker-compose.yml`
- Create: `docs/superpowers/plans/2026-07-22-jackson3-migration.md`

- [x] **Step 1: Change Spring AI OpenAI model settings from deprecated `.options.model` keys to `spring.ai.openai.chat.model` and `spring.ai.openai.embedding.model`.**
- [x] **Step 2: Document the actual Anthropic/Mistral support and remove stale Azure starter claims.**
- [x] **Step 3: Record a Jackson 3-only follow-up plan covering import migration, removal of `spring-boot-jackson2`, and REST/JWT/JSONB/OpenAPI contract tests.**
- [x] **Step 4: Run `rtk mvn -q test package` and `rtk git diff --check`.**
