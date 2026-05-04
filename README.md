# lsym-memory-hub

> lsym 项目记忆体仓库 - 用于保存 AI 助手在 lsym 项目工作过程中的记忆、知识和文档

---

## 目录结构

```
lsym-memory-hub/
├── CLAUDE.md                    # AI 工作配置（必读）
├── README.md                    # 本文件
├── docs/                        # 核心设计文档
│   ├── SUPPLY_CHAIN_DESIGN_V5.5.md       # 完整设计文档（最权威）
│   ├── TRANSACTION_QUICK_REFERENCE.md    # 六大交易快速参考
│   └── ACCOUNT_CHANGE_SOURCE_MAP.md      # 账户变动源码映射
├── architecture/                # 架构设计文档
│   ├── FRAMEWORK_BLUEPRINT.md            # 框架蓝图（新项目参考）
│   ├── FRAMEWORK_STRUCTURE.md            # 框架结构（TransSlot/QuerySlot 详解）
│   └── TRANS_COMPONENT_STRUCTURE.md      # Trans 组件结构详解
├── modules/                     # 模块文档
│   ├── MODULE_FUND_CATERING.md           # fund-catering 主结构文档
│   ├── MODULE_BASE.md                    # 基础服务模块
│   ├── MODULE_FRONT.md                  # 前置服务模块
│   ├── MODULE_MANAGEMENT.md              # 管理服务模块
│   ├── MODULE_TASK.md                    # 任务调度模块
│   ├── MODULE_REPORT.md                  # 报表模块
│   ├── MODULE_MICROSERVICES.md           # 微服务架构模块
│   ├── ACCOUNT_OPENING.md                # 开户模块
│   ├── CHECK_COMPONENTS.md               # Check 组件详解
│   ├── API_REFERENCE.md                  # API 接口文档
│   └── DATABASE_SCHEMA.md                # 数据库表结构
├── technical-decisions/         # 技术决策记录
│   ├── BATCH_TRANSFER_IMPLEMENTATION.md  # 批量转账实现
│   └── MAC_CONCURRENCY_FIX.md            # MAC并发修复
├── business-flows/              # 业务流程文档
│   └── CONSUME_FLOW_DIAGRAMS.md          # 消费流程图
├── skills/                      # AI 技能和技术指南
│   └── LITEFLOW_SKILLS.md                # LiteFlow 开发技能
├── workflow/                    # 工作流程与规范
│   ├── DOCUMENT_MANAGEMENT_RULES.md      # 文档管理规则
│   └── PROJECT_MEMORY.md                # 项目记忆管理
└── conversation-logs/           # 会话日志
    └── YYYY-MM-DD.md
```

---

## 项目信息

| 属性 | 值 |
|------|------|
| **项目名称** | lsym (餐饮资金体系) |
| **负责人** | 李蒙 (ssssgoldhunter) |
| **主项目路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy` |
| **记忆库路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory-hub` |
| **当前活跃分支** | `lsym_prod` |
| **冷冻分支** | `lsym_20260116_limeng_restruct` |
| **GitHub** | https://github.com/ssssgoldhunter/lsym-memory-hub |
| **飞书文档** | https://jvn4jogcy6u.feishu.cn |

**详细配置**: 请查看 [`CLAUDE.md`](./CLAUDE.md)

---

## 核心文档速览

### 六大交易流程

| 流程 | 特点 | 快速查看 |
|------|------|----------|
| 消费 | 02膨胀金优先扣款，支持分账 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |
| 充值 | 支持01现金+02膨胀金赠送 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |
| 充值退款 | 原路退回，膨胀金收回 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |
| 提现 | 自动提现+人工审核 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |
| 转账 | 三层锁机制，支持批量 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |
| 消费退款 | 按比例/按单退款 | [快速参考 →](./docs/TRANSACTION_QUICK_REFERENCE.md) |

### 技术架构

- **流程编排**: LiteFlow
- **组件类型**: Pack → Check → Trans → After
- **上下文**: TransSlot（交易）、QuerySlot（查询）
- **详情**: [框架结构 →](./architecture/FRAMEWORK_STRUCTURE.md)

### fund-catering 主扫描范围

当前主结构文档聚焦 `slhy/fund-catering` 下 7 个核心模块：

- `fund-catering-base`
- `fund-catering-consume`
- `fund-catering-front`
- `fund-catering-management`
- `fund-catering-report`
- `fund-catering-task`
- `fund-catering-web`

`fund-catering-data-batch` 保留独立文档 `modules/MODULE_DATA_BATCH.md`，但不纳入当前 fund-catering 主结构整理范围。

---

## 更新记录

| 日期 | 更新内容 |
|------|----------|
| 2026-05-04 | 以 lsym_prod 为主线对齐全部文档：路径修正(Mac)、分支状态、开发重点 |
| 2026-04-27 | 根据当前源码重整 fund-catering 主结构文档，主扫描范围排除 data-batch |
| 2026-03-06 | 模块文档补充：新增 9 个模块文档 |
| 2026-03-04 | 文档整理：删除 6 个重复文件，创建 CLAUDE.md |
| 2026-03-03 | 文档迁移：从 slhy/md 迁移到记忆库 |
| 2026-03-02 | 初始化记忆体仓库，补充交易流程详解 v5.5 |

---

## 联系方式

- **GitHub**: https://github.com/ssssgoldhunter
