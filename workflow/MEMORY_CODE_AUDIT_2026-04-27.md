# 记忆体与源码交叉校验报告

日期：2026-04-27

范围：

- 记忆体项目：`D:\workspaces\IdeaProjects_lsym_dep\lsym-memory-hub`
- 源码项目：`D:\workspaces\IdeaProjects_lsym_dep\slhy`
- 重点源码范围：`slhy/fund-catering`

## 1. 扫描结论

本次扫描没有发现 Markdown 内部链接断链。主要问题是：部分文档仍保留旧路径、旧仓库名、已删除目录示意、过期文件数统计，以及历史设计文档中的方案描述与当前源码实现混在一起。

建议将文档分为三类处理：

| 类型 | 处理方式 |
|------|----------|
| 当前入口/索引/规则文档 | 直接修正为当前源码事实 |
| 模块说明文档 | 按源码重新校准文件数、模块结构、关键类 |
| 历史设计/计划/对话日志 | 不直接改结论，只加“历史设计/未完全落地/当前实现参见...”说明 |

## 2. 源码事实基线

### 2.1 slhy 顶层模块

源码根 `slhy/pom.xml` 当前启用模块：

- `common-core`
- `api-modules`
- `starter-modules`
- `db-service`
- `notify-service`
- `portal-service`
- `routing-service`
- `gateway-service`
- `auth-service`
- `system-service`
- `file-service`
- `gen-service`
- `job-service`
- `fund-catering`
- `reconcile-service`

`migration-service` 目录存在，但在 `slhy/pom.xml` 中仍是注释状态。

### 2.2 技术版本

源码确认：

- Java：`17`
- Spring Boot：`3.2.4`
- Spring Cloud：`2023.0.1`
- fund-catering 父模块也声明 Java `17`、Spring Cloud `2023.0.1`

### 2.3 fund-catering 模块文件数

源码扫描结果：

| 模块 | Java 文件数 | pom 数 |
|------|-------------|--------|
| `fund-catering-base` | 403 | 3 |
| `fund-catering-consume` | 528 | 4 |
| `fund-catering-data-batch` | 414 | 4 |
| `fund-catering-front` | 214 | 4 |
| `fund-catering-management` | 167 | 4 |
| `fund-catering-report` | 589 | 3 |
| `fund-catering-task` | 83 | 1 |
| `fund-catering-web` | 248 | 1 |

当前主结构文档已按用户要求排除 `fund-catering-data-batch`，但源码中该模块仍存在，且父 `pom.xml` 仍包含该模块。

## 3. 校验通过项

### 3.1 内部 Markdown 链接

扫描 `lsym-memory-hub` 下所有 Markdown 链接，未发现本地相对链接断链。

### 3.2 LiteFlow 链路

文档中高频交易链路在源码 `fund-catering-consume-service/src/main/resources/liteflow/consume.el.xml` 或 `query.el.xml` 中存在，包括：

- `chainConsume`
- `chainConsumeAuth`
- `chainConsumePre`
- `chainConsumePreFinish`
- `chainConsumeClose`
- `chainConsumeCal`
- `chainConsumeCalOrder`
- `chainRecharge`
- `chainRefundRecharge`
- `chainWithDraw`
- `chainTransfer`
- `chainTransferInner`
- `chainTransferInnerPre`
- `chainTransferTiPre`
- `chainTransferAuth`
- `chainTransferReSendVerification`
- `chainConsumeRefund`
- `chainFrozen`
- `chainUnFrozen`
- `chainWithdrawResultQuery`
- `chainRechargeResultQuery`
- `chainFrozenDetailQuery`
- `chainConsumeResultQuery`
- `chainTransferResultQuery`

### 3.3 账户变动当前实现

源码确认当前真实入口是扩展后的 `BaseAccountServiceApi`，并由 base-service 的 `AccountChangeBatchService`/`AccountChangeBatchServiceImpl` 支撑。

存在的关键方法：

- `batchChangeAccount`
- `batchChangeAccountForRecharge`
- `batchChangeAccountForRefundRecharge`
- `batchChangeAccountForConsume`
- `batchChangeAccountForRefundConsume`

源码中未发现已落地的独立 `AccountChangeApi` 接口。

## 4. 发现的问题

### 4.1 当前入口文档仍列出不存在目录

`README.md` 仍展示以下目录：

- `api-docs/`
- `knowledge-base/`
- `common-issues/`

当前仓库实际顶层目录只有：

- `architecture`
- `business-flows`
- `conversation-logs`
- `docs`
- `modules`
- `skills`
- `technical-decisions`
- `workflow`

建议：修正 `README.md` 的目录结构示意。

### 4.2 文档管理规则仍列出已删除或不存在目录

`workflow/DOCUMENT_MANAGEMENT_RULES.md` 仍展示：

- `docs/MIGRATION_LOG.md`
- `business-flows/TRANSACTION_FLOWS.md`
- `project-overview/PROJECT_INFO.md`
- `workflow/USER_PREFERENCES.md`
- `knowledge-base/`

这些内容在当前仓库中不存在，且部分在历史日志中明确是已删除/去重内容。

建议：更新目录结构示意，避免后续按过期目录写文档。

### 4.3 模块文档文件数过期

当前文档与源码扫描结果不一致：

| 文档位置 | 现文档 | 源码事实 |
|----------|--------|----------|
| `modules/MODULE_BASE.md` | 399 Java 文件 | 403 |
| `modules/MODULE_TASK.md` | 217 Java 文件 | 83 |
| `modules/MODULE_REPORT.md` | 50+ Java 文件 | 589 |
| `modules/MODULE_DATA_BATCH.md` | 100+ Java 文件 | 414 |
| `CLAUDE.md` 模块索引 | task 217、report 50+ | task 83、report 589 |

建议：以源码扫描结果更新模块文档和 `CLAUDE.md` 模块索引。

### 4.4 旧路径仍存在于非入口文档

以下当前/参考文档仍包含旧 Mac 路径：

- `docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
- `docs/SUPPLY_CHAIN_DESIGN_V5.5.md`
- `docs/TRANSACTION_QUICK_REFERENCE.md`
- `skills/LITEFLOW_SKILLS.md`

历史对话日志 `conversation-logs/2026-03-04.md` 也包含旧路径和旧仓库名，这属于历史记录，不建议直接改原文。

建议：

- 当前参考文档中的旧路径替换为相对路径或 Windows 当前路径。
- 历史对话日志保留原文。

### 4.5 历史设计文档存在未落地方案描述

以下设计/待办文档仍大量出现 `AccountChangeApi`：

- `docs/superpowers/specs/2026-03-11-account-change-refactor-design.md`
- `docs/superpowers/specs/2026-03-12-account-change-refactor-todo.md`

源码事实是：当前落地的是扩展 `BaseAccountServiceApi`，未发现独立 `AccountChangeApi`。

建议：不重写历史设计正文，只在文档开头增加状态说明：

> 当前实现已转为扩展 `BaseAccountServiceApi`，独立 `AccountChangeApi` 属于历史设计方案，未在当前源码中完整落地。

### 4.6 `requirements/` 目录不存在

用户提到“需求文档”时，当前仓库没有 `requirements/` 目录。实际需求/计划类文档分散在：

- `docs/plans/`
- `docs/superpowers/specs/`
- `docs/superpowers/plans/`

建议：如果后续要统一“需求文档”，可新增 `requirements/` 或保留现状但在 README 中明确“需求/计划文档位置”。

## 5. 建议修复顺序

建议按风险从低到高处理：

1. 更新入口和规则文档的目录结构示意。
2. 更新模块文档文件数和模块索引。
3. 将当前参考文档中的绝对 Mac 路径改为相对路径。
4. 给历史设计文档增加状态说明，不改历史正文。
5. 决定是否新增 `requirements/` 目录，或继续使用现有 `docs/plans` 和 `docs/superpowers/specs` 结构。

## 6. 不建议直接修改的内容

- `conversation-logs/*`：属于历史会话记录，不应为了当前事实改写原文。
- 历史设计文档中的方案主体：可加状态说明，但不建议直接删除旧方案内容。
- `MODULE_DATA_BATCH.md`：模块仍在源码和父 POM 中存在，只是不纳入当前 fund-catering 主结构扫描范围。
