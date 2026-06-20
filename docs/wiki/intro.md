---
type: Reference
title: LLM Wiki 模式（中文解读）
description: 从 graphiti-java 视角介绍 LLM Wiki 模式：摄入 / 查询 / 巡检 三操作与三种使用路径
okf_version: "0.1"
bundle: graphiti-java-wiki
tags: [llm-wiki, knowledge-base, pattern, methodology]
timestamp: 2026-06-20T16:50:00Z
related:
  - ./llm-wiki.md
  - ./llm-wiki-cn.md
  - /docs/SCHEMA.md
---

# LLM Wiki 模式（中文解读）

`docs/wiki/llm-wiki.md` 是 Anthropic 发布的一份**模式文档（pattern / idea file）**，不是技术规范或 API 文档。它描述的是"用 LLM 维护一个持续积累的个人知识库"这一方法论。

下面从两个层面回答：①这是什么；②作为技术撰稿人如何用起来。

## 一、它是什么

### 核心主张：跟 RAG 不一样

| 维度 | 传统 RAG（NotebookLM、ChatGPT 文件上传等） | LLM Wiki 模式 |
|---|---|---|
| 处理时机 | 查询时临时检索+生成 | 摄入时即写入 wiki |
| 知识形态 | 每次从原文重新拼装 | 已编译、已交叉引用、已标注矛盾 |
| 跨文档综合 | 每次都重新做 | 已沉淀在页面里 |
| 是否有"积累" | 没有 | 有，复利增长 |

一句话：**把 wiki 本身当成一个被 LLM 持续维护的、可复利的产物，而不是查询时的临时产物。**

### 三层架构

```
┌─────────────────────────────────────┐
│  Schema（CLAUDE.md / AGENTS.md）     │  ← 你和 LLM 共同演进，约定 wiki 怎么组织
├─────────────────────────────────────┤
│  Wiki（Markdown 页面集合）            │  ← LLM 全权写入，你只读
├─────────────────────────────────────┤
│  Raw sources（原始资料，只读）        │  ← 你挑选，LLM 读取但不修改
└─────────────────────────────────────┘
```

### 三个核心操作

- **Ingest（摄入）**：扔一个新源料进来 → LLM 读 → 写摘要页 → 更新实体/概念页 → 维护交叉引用 → 追加日志。一次摄入常会触动 10–15 个 wiki 页面。
- **Query（查询）**：LLM 先读 `index.md` 找相关页，再钻进去综合，回答可以是 Markdown、对比表、Marp 幻灯片、图表。**关键洞察：好的回答本身就该回填成新 wiki 页**，让探索成果也成为知识库的一部分。
- **Lint（巡检）**：定期让 LLM 健康检查——发现矛盾页面、过时主张、孤儿页、缺失交叉引用、可补充的资料缺口。

### 两个导航文件

- `index.md`：内容向的目录（按类别列页+一行摘要+元数据），每次摄入都更新。LLM 回答前先读它找页。小规模（~100 源，几百页）够用，不一定要向量检索。
- `log.md`：时序向的追加日志（`## [2026-04-02] ingest | Article Title` 这种格式），`grep` 友好。

### 为什么可行

维基的真正成本是维护（更新交叉引用、标注矛盾、保持一致），不是阅读。人类维护会厌倦，LLM 不会。这就是这个模式要解决的问题。

---

## 二、作为技术撰稿人怎么用

结合 graphiti-java 这个仓库的实际场景（已有 `docs/`、`docs/superpowers/plans/`、`docs/superpowers/specs/`），给你三条路径，按投入度递增：

### 路径 A：把现有 `docs/` 当 raw sources，让 LLM 生成一个二级 wiki

最小启动：
1. 新建 `docs/wiki/` 作为 wiki 层
2. 把 `docs/manual/`（虽然 git 里显示被删除，但概念上）以及 specs、plans 视作 raw sources
3. 写一个 `docs/wiki/SCHEMA.md`（或 `.cursor/rules/llm-wiki.md`）告诉 Cursor：
   - 摄入一篇 spec/plan 时，要建哪些页（概念页、决策页、模块页、ADR 页）
   - `index.md` 怎么分类
   - `log.md` 的前缀格式
4. 下一次"喂料"时，让 Cursor 走一遍 ingest 流程并把结果写进 wiki

### 路径 B：把"好答案"也回填

在 Cursor 里跟 LLM 讨论架构、做对比分析时，把那些高质量对话产物（对比表、设计权衡图、API 选型结论）直接写成 `docs/wiki/discoveries/*.md`。这些跟你读的 spec 一样有价值，不该消失在聊天记录里——这就是文档里说的 *"good answers can be filed back into the wiki as new pages"*。

### 路径 C：把 wiki 当 IDE，LLM 当程序员

按文档原意，走 Obsidian + LLM Agent 的范式：
- `docs/wiki/` 用 Obsidian 打开（左侧面板浏览，右侧和 Cursor 对话）
- 用 Obsidian Graph View 看哪些概念成了 hub、哪些是 orphan
- 用 Dataview 插件读 YAML frontmatter（让 LLM 写页面时加上 tags、source_count、last_updated）
- Marp 插件做分享幻灯片
- 资料图床放 `docs/wiki/raw/assets/`，给 LLM 看图

### 工具选型

文档提到一个本地搜索工具 [qmd](https://github.com/tobi/qmd)（BM25 + 向量 + LLM 重排，纯本地）。当 wiki 长到 `index.md` 不够用时再上，**不要预先建设**——这跟文档的"模块化、按需取舍"原则一致。

---

## 三、一句话总结 + 第一步

> **这是一份"思想文件"（idea file），它的唯一职责是把这个模式传达给 LLM，让你和 LLM 一起把它实例化成贴合你领域的版本。**

建议第一步：复制 `docs/wiki/llm-wiki.md` 到 Cursor 的上下文，让 Cursor 读完之后，一起起草一份 `docs/wiki/SCHEMA.md`，约定好 graphiti-java 这个项目的 wiki 结构（按模块？按功能域？按 spec 关联？）和摄入流程。然后试 ingest 一篇 spec 看效果。

如果你想，我可以直接帮你起草这份 `SCHEMA.md`——需要先告诉我两件事：你想用这个 wiki 覆盖什么范围（仅 docs/superpowers/？还是连代码文档一起？），以及你倾向用 Obsidian 浏览还是纯编辑器内阅读。
