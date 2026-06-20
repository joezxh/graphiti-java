---
type: Reference
title: OKF 规范（中文解读）
description: Open Knowledge Format v0.1 草案规范的中文解读：是什么、和 llm-wiki.md 的关系、技术撰稿人怎么用
okf_version: "0.1"
bundle: graphiti-java-wiki
tags: [okf, specification, knowledge-format, reference, zh-CN]
timestamp: 2026-06-20T16:50:00Z
related:
  - /docs/okf-spec.md
  - ./llm-wiki.md
  - ./intro.md
  - /docs/SCHEMA.md
---

`docs/okf-spec.md` 是 **Open Knowledge Format (OKF) v0.1 — Draft 规范**。和 `docs/llm-wiki.md`（模式/idea file）不一样，OKF 是一份**可执行的格式规范**，定义了"知识"该如何被结构化地表示、交换、消费。

下面我从三块讲清楚：①它是什么；②它和 llm-wiki.md 的关系；③作为技术撰稿人怎么用。

---

## 一、它是什么

### 一句话定义

> **OKF 是一种用"目录 + Markdown + YAML frontmatter"来表达知识（knowledge）的开放格式**，面向人和 AI agent 双重消费。

### 设计立场（spec 里反复强调的）

| 特性 | OKF 的取舍 |
|---|---|
| 可读 | `cat` 一个文件就能读，**无需任何工具** |
| 可解析 | agent 不需要专用 SDK |
| 可 diff | git 友好 |
| 可移植 | 不绑定任何存储/查询基础设施 |
| 自描述 | bundle 自己解释自己，**没有中央 schema registry** |
| 最小化强制 | 只强制 `type` 一个字段；其余都是"建议" |

### 核心对象

- **Knowledge Bundle（知识包）**：自包含、可分发的目录树单元，可以是 git repo / tarball / 仓库子目录。
- **Concept（概念）**：bundle 内的一个 `.md` 文件 = 一个知识单元，可以描述"具体资产"（一张表、一个 API），也可以描述"抽象概念"（一个指标、一个流程）。
- **Concept ID**：文件路径去掉 `.md` 后缀，例如 `tables/users.md` → `tables/users`。

### 文件结构（§3）

```
my_bundle/
├── index.md              # 可选：渐进式披露的目录
├── log.md                # 可选：变更历史
├── concept.md
└── subdir/
    ├── index.md
    └── concept.md
```

**保留文件名**：`index.md`、`log.md`，其它任何 `.md` 都是概念文档。

### Concept 的两段式结构（§4）

```markdown
---
type: <Type>                # 唯一强制字段
title: ...
description: ...
resource: <URI>             # 描述的底层资产的规范 URI（可选）
tags: [...]
timestamp: <ISO 8601>
---

# Schema / Examples / Citations
```

**唯一强制字段就是 `type`**（如 `BigQuery Table`、`API Endpoint`、`Metric`、`Playbook`、`Reference`），其余都是可选。

### 三个约定俗成的章节（§4.2）

| 标题 | 用途 |
|---|---|
| `# Schema` | 资产字段/列的结构化描述 |
| `# Examples` | 用法示例 |
| `# Citations` | 外部来源引用，编号列表 |

### 交叉引用（§5）

两种链接形式：
- **绝对（bundle-相对）**：以 `/` 开头，如 `[customers](/tables/customers.md)`——**推荐**，移动文件不破坏。
- **相对**：标准 markdown 相对路径。

链接语义：**只表达"有关系"，关系类型由上下文文字决定**，不是链接本身决定。broken link 不算错误（可能"还没写"）。

### `index.md` / `log.md`（§6/§7）

- `index.md`：无 frontmatter，按"标题分组"列出条目，每条 `* [Title](url) - 一句描述`。**专为渐进式披露设计**——人和 agent 都先看 index 再下钻。
- `log.md`：纯时序的变更日志，按日期倒序，**ISO 8601 日期**（`## 2026-05-22`），开头加粗动词（`**Update**`、`**Creation**`、`**Deprecation**`）。

### 宽容消费模型（§9，重要）

消费者**不能**因为以下原因拒绝 bundle：
- 缺可选字段
- 未知 `type` 值
- 未知 frontmatter 键
- 死链
- 缺 `index.md`

设计意图：**bundle 长大、被重构、被 agent 部分生成时，仍要保持可用。**

### 版本（§11）

- 0.1 当前
- minor 版本 = 向后兼容的新增（新的可选字段、新约定章节）
- major 版本 = 破坏性变更
- bundle 根的 `index.md` 可以在 frontmatter 声明 `okf_version: "0.1"`（这是 index.md **唯一允许写 frontmatter 的位置**）

---

## 二、OKF vs LLM Wiki——两者关系

spec §10 明确说了：**OKF 跟 LLM wiki 这种模式非常接近**，但 OKF 是"被**规定**的"——把必要的规则钉死以保证互操作性，但不强制工具栈。

| 维度 | `llm-wiki.md` | `okf-spec.md` |
|---|---|---|
| 性质 | 模式/idea file | 格式规范 |
| 目的 | 启发你怎么用 LLM 维护 wiki | 规定 bundle 长什么样 |
| 是否强制结构 | 不强制 | 强制（每个概念必须有 frontmatter + `type`） |
| 互操作性 | 你的 LLM 自己懂 | 任何懂 OKF 的 agent 都能消费 |
| 类型字段 | 没规定 | 唯一强制的 `type` |

**结论：OKF 可以看成"把 LLM wiki 这种好实践正式化的格式"**。你的 wiki 既可以纯按 `llm-wiki.md` 的模式自由生长，也可以声明成 OKF bundle 让别的 agent 也能读。

---

## 三、作为技术撰稿人怎么用

针对 graphiti-java 这个项目，下面给你三条可执行的路径：

### 路径 A：把现有 `docs/` 升级为 OKF bundle（最低成本）

只需做一件事：**给 `docs/` 下的每个 `.md` 加上 YAML frontmatter**，至少包含 `type` 字段。

具体做法：
1. 先为这个项目约定一组 `type`（举例）：
   - `Module` — 描述一个模块
   - `API` — 描述一个 API 接口
   - `Concept` — 抽象概念（如社区发现算法、向量嵌入）
   - `Spec` — 来自 `docs/superpowers/specs/` 的设计文档
   - `Plan` — 来自 `docs/superpowers/plans/` 的实施计划
   - `Reference` — 参考资料 / 外部链接集合
2. 一次性给所有文档加 frontmatter（可以用脚本批量），不必改 body。
3. 在 `docs/index.md` 顶部加 frontmatter 声明版本：
   ```markdown
   ---
   okf_version: "0.1"
   ---
   ```
4. 把 `docs/wiki/frontend/index.md` 改成"按 type 分组的目录页"（OKF index 的推荐形式）。

完成后，**任何遵循 OKF 的 agent 都能消费这个仓库**——不只是 Cursor。

### 路径 B：把 OKF 用作新文档的写作规范

往后再写新文档时（比如新的 spec、新的 plan），按 OKF 模板写：

```markdown
---
type: Spec
title: 社区发现算法 Phase 2 设计
description: 通用社区发现的第二轮设计与算法选型
tags: [community-detection, algorithm]
resource: docs/superpowers/specs/2026-05-20-community-generic-design.md
timestamp: 2026-06-20T10:00:00Z
---

# Context
... （用 OKF 推荐的"结构性 markdown"）

# Schema
... （如有）

# Citations
[1] [...]
```

并且：**用绝对链接 `/concepts/xxx.md`** 引用其它概念，便于以后移动文件不破坏链接。

### 路径 C：搭建自动化流水线（中等投入）

OKF 本身就鼓励"机器生成 + 人类审阅"。结合 graphiti-java 已有 `docs/superpowers/specs/` 和 `docs/superpowers/plans/` 这两堆文档，可以：

1. **写一个 ingest 脚本**：每次新增 spec/plan，自动：
   - 从 frontmatter 提取 `type`、`tags`、`resource`
   - 在对应子目录的 `index.md` 追加条目
   - 在根 `log.md` 追加 `## <日期>` 记录
2. **写一个 lint 脚本**（OKF §9 conformance）：
   - 检查每个 `.md` 是否有 frontmatter
   - 检查 `type` 是否非空
   - 检查死链
   - 检查保留文件名是否被误用作概念文档
3. **把 lint 接到 CI**：让 PR 阶段就保证 OKF 合规。

这一套 `2026-06-18-docs-pipeline.md` 计划（git 里看到了它还没合并）看起来正好对应这个方向——你可以用它对齐。

### 路径 D：对外发布时声明 bundle 身份

如果哪天你要把 `docs/` 单独打包或克隆给别人用：

```bash
git clone --depth 1 --branch docs-bundle https://... docs-bundle/
tar czf graphiti-knowledge.tar.gz docs-bundle/
```

接收方只需要知道"这是 OKF 0.1 bundle"就能用任何 OKF 工具消费。spec §3 明确说 bundle 可以是 git repo、tarball、或更大仓库的子目录。

---

## 四、给你一个立即能用的 checklist

如果你决定要走 OKF 路线，下面是第一步该做的事，按顺序：

1. **翻一遍现有 `docs/`**，数一下文档数量和分布。
2. **为这个项目定义 5–10 个 `type` 值**，写在 `docs/SCHEMA.md` 或 `.cursor/rules/okf.md`（这相当于 OKF 之外的本地约定）。
3. **挑一个最小的子集试水**（比如 `docs/wiki/frontend/`），先把这部分文档加上 frontmatter + 改造 index.md。
4. **跑一遍手动 lint**（用 spec §9 的 3 条规则）。
5. 验证 OK 后，再决定是否铺到整个 `docs/`。

---

## 五、回答你的两个隐含问题

**"这是什么？"** —— 一份可执行的格式规范（不是 idea file），定义了一种叫 OKF 的"知识包"格式：目录 + Markdown + 强制 `type` 字段的 frontmatter + 可选 index/log。

**"技术撰稿人怎么用？"** —— 三种力度任选：
- 轻：把现有文档加上 frontmatter 升级为 OKF bundle，对外可分发
- 中：以后所有新文档按 OKF 模板写，并自动维护 index/log
- 重：加 CI lint，让 OKF 合规成为仓库的硬约束

如果你想，我可以接着做两件事：
1. 帮你审查 `docs/wiki/frontend/` 现有的 `index.md` / 状态文档，看它们离 OKF 合规差多远；
2. 或者起草一份 `docs/SCHEMA.md`，给 graphiti-java 这个项目定义一组 `type` 值和 frontmatter 约定。

要我做哪个？