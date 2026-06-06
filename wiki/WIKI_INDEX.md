# Wiki — 文档→代码→内容映射索引

> 本目录作为文档与代码的桥梁，每篇 wiki 页面映射一个业务域/功能模块，
> 关联其设计文档、需求文档、源码路径、数据库表、API接口和配置项。

---

## 快速导航

### 按业务域

| 业务域 | Wiki 页 | 核心源码模块 | 设计文档 |
|---|---|---|---|
| 消费交易 | [consume.md](consume.md) | `fund-catering-consume` | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` |
| 消费退款 | [consume-refund.md](consume-refund.md) | `fund-catering-consume` | `topics/consume-refund.md` |
| 充值 | [recharge.md](recharge.md) | `fund-catering-consume` | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` |
| 平台批量实收 | [platform-recharge-batch.md](platform-recharge-batch.md) | `fund-catering-consume` + `fund-catering-task` | `docs/specs/2026-05-19-platform-recharge-batch-design.md` |
| 提现 | [withdraw.md](withdraw.md) | `fund-catering-consume` | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` |
| 转账(T/TI) | [transfer.md](transfer.md) | `fund-catering-consume` | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` |
| 扣款(冻结/非冻结) | [deduction.md](deduction.md) | `fund-catering-consume` | — |
| 冻结/解冻 | [frozen.md](frozen.md) | `fund-catering-consume` | — |
| 账户变动 | [account-change.md](account-change.md) | `fund-catering-base` | `topics/account-change.md` |
| 自有资金池 | [self-fund.md](self-fund.md) | `fund-catering-base` | `topics/self-fund-account.md` |
| 账户开户 | [account-opening.md](account-opening.md) | `fund-catering-base` | `modules/ACCOUNT_OPENING.md` |
| 批量转账上账 | [batch-transfer.md](batch-transfer.md) | `fund-catering-consume` | `technical-decisions/BATCH_TRANSFER_IMPLEMENTATION.md` |
| 回溯(消费/转账) | [recall.md](recall.md) | `fund-catering-task` | — |

### 按模块

| 模块 | Wiki 页 | 模块文档 |
|---|---|---|
| fund-catering-base | [account-change.md](account-change.md) | `modules/MODULE_BASE.md` |
| fund-catering-consume | [consume.md](consume.md) 等 | `modules/MODULE_FUND_CATERING.md` |
| fund-catering-front | [front.md](front.md) | `modules/MODULE_FRONT.md` |
| fund-catering-task | [task.md](task.md) | `modules/MODULE_TASK.md` |
| fund-catering-report | [report.md](report.md) | `modules/MODULE_REPORT.md` |
| fund-catering-data-batch | [data-batch.md](data-batch.md) | `modules/MODULE_DATA_BATCH.md` |
| fund-catering-management | [management.md](management.md) | `modules/MODULE_MANAGEMENT.md` |

---

## Wiki 页面模板

每个 wiki 页应包含以下结构：

```markdown
# {业务域/模块名}

## 关联文档
- 设计文档：链接
- 需求文档：链接
- 技术决策：链接

## 源码映射
| 类 | 路径 | 职责 |
|---|---|---|

## LiteFlow 链路（如适用）
| 链 | 组件顺序 | 说明 |
|---|---|---|

## 数据库表
| 表名 | 说明 |
|---|---|

## API 接口
| 接口 | 方法 | 说明 |
|---|---|---|

## Redis Key
| Key | TTL | 用途 |
|---|---|---|

## 关联 Bug / 排查记录
- 链接

## 跨项目迁移记录（如适用）
- 源项目 → 目标项目，commit，分支
```

---

## 项目间映射

| lsym (slhy) 源码 | mdl 源码 | 记忆体项目 | 说明 |
|---|---|---|---|
| `slhy/fund-catering-consume` | `mdl/fund-catering-consume` | `lsym-memory-hub` | 消费服务，结构一致 |
| `slhy/fund-catering-base` | `mdl/fund-catering-base` | `lsym-memory-hub` | 基础服务，结构一致 |
| `slhy/fund-catering-task` | `mdl/fund-catering-task` | `lsym-memory-hub` | 定时任务，结构一致 |
| `slhy/common-core` | `mdl/common-core` | `lsym-memory-hub` | 公共组件，结构一致 |

> **规则**：设计文档、需求文档、wiki 均保存在 `lsym-memory-hub` 记忆体项目中。
> mdl 项目的 Claude memory (`~/.claude/projects/.../memory/`) 仅保存 mdl 特有的运行时笔记。

---

## 与其他目录的关系

```
lsym-memory-hub/
├── wiki/                    ← 【本目录】文档→代码映射索引，每个业务域一个页面
├── docs/                    ← 设计文档、规范、参考手册
│   ├── specs/               ← 功能设计规格
│   └── plans/               ← 实现计划
├── requirements/            ← 需求文档
├── architecture/            ← 架构文档
├── topics/                  ← 问题域主题页（当前口径+源码入口）
├── technical-decisions/     ← 技术决策记录
├── modules/                 ← 模块结构文档
├── bugs/                    ← Bug 排查记录
├── business-flows/          ← 业务流程图
├── workflow/                ← 工作流和项目管理
├── skills/                  ← 技能文档
├── knowledge-base/          ← 知识库
└── api-docs/                ← API 文档
```

| 需求 | 去哪里 |
|---|---|
| 查业务流程涉及哪些类、表、接口 | `wiki/` |
| 查设计决策和方案 | `docs/specs/` |
| 查模块整体结构 | `modules/` |
| 查某个问题的当前口径 | `topics/` |
| 查 Bug 排查过程 | `bugs/` |
| 查交易流程快速参考 | `docs/TRANSACTION_QUICK_REFERENCE.md` |
| 查账户变动源码入口 | `docs/ACCOUNT_CHANGE_SOURCE_MAP.md` |
```
