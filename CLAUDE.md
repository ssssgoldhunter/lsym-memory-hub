# lsym 项目 - AI 工作配置

> 本文件为 Claude Code AI 助手在 lsym 项目中的工作配置和上下文参考

---

## 📋 项目基本信息

| 属性 | 值 |
|------|------|
| **项目名称** | lsym (餐饮资金体系) |
| **负责人** | 李蒙 (ssssgoldhunter) |
| **主项目路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy` |
| **记忆库路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory` |
| **GitHub 仓库** | https://github.com/ssssgoldhunter/lsym-memory-hub |
| **飞书文档** | https://jvn4jogcy6u.feishu.cn |

---

## 🔧 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 2.x | 应用框架 |
| LiteFlow | 最新版 | 流程编排引擎（核心） |
| MyBatis Plus | 最新版 | ORM 框架 |
| Redis | - | 分布式锁、缓存 |
| Nacos | - | 配置中心、服务注册发现 |

---

## 📚 核心文档索引

### 必读文档（按优先级）

| 优先级 | 文档 | 路径 | 说明 |
|--------|------|------|------|
| ⭐⭐⭐ | 快速参考 | `docs/TRANSACTION_QUICK_REFERENCE.md` | 六大交易流程快速查询 |
| ⭐⭐⭐ | 完整设计文档 | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` | 最权威的设计文档 |
| ⭐⭐ | 框架结构 | `architecture/FRAMEWORK_STRUCTURE.md` | TransSlot/QuerySlot 详解 |
| ⭐ | 框架蓝图 | `architecture/FRAMEWORK_BLUEPRINT.md` | 新项目参考 |
| ⭐ | 文档管理规则 | `workflow/DOCUMENT_MANAGEMENT_RULES.md` | 文档存储规范 |

---

## 🎯 六大核心交易流程

| 流程 | 流程链 | 接口 | 特点 |
|------|--------|------|------|
| **消费** | chainConsume | /scConsumeFree | 02膨胀金优先扣款，支持分账 |
| **充值** | chainRecharge | /scRecharge | 支持01现金+02膨胀金赠送 |
| **充值退款** | chainRefundRecharge | /scRefundRecharge | 原路退回，膨胀金收回 |
| **提现** | chainWithDraw | /scWithdraw | 自动提现+人工审核 |
| **转账** | chainTransfer | /scTransfer | 三层锁机制，支持批量 |
| **消费退款** | chainConsumeRefund | /scConsumeRefund | 按比例/按单退款 |

### 子账户类型

| 代码 | 名称 | 说明 |
|------|------|------|
| 01 | 现金账户 | 可提现 |
| 02 | 膨胀金账户 | 赠送金额，优先消费，不可提现 |
| 04 | 综合账户 | 综合子账户 |

---

## 🧠 LiteFlow 组件类型

| 组件类型 | 命名规范 | 职责 |
|----------|----------|------|
| **Pack** | {业务}TransPack | 数据打包、参数校验 |
| **Check** | {校验项}Check | 业务规则校验 |
| **Trans** | {业务}Trans{类型} | 核心交易处理、写库 |
| **After** | {业务}TransAfter | 后处理、账户变动明细 |
| **Route** | {业务}Route | 路由、分支逻辑 |

### 通用链路结构

```
Pack → Check → Trans → After
```

---

## 📁 核心源码路径

| 类型 | 路径 |
|------|------|
| 消费服务 | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume` |
| LiteFlow 配置 | `fund-catering-consume-service/src/main/resources/liteflow/` |
| Trans 组件 | `flow/component/trans/` |
| Query 组件 | `flow/component/query/` |

---

## 📝 工作规范

### 文档存储规则

> **默认规则**：所有 md 文档和记忆体内容都放在 `lsym-memory/` 项目中

| 类型 | 存储位置 |
|------|----------|
| md 文档 | `lsym-memory/` |
| 记忆体内容 | `lsym-memory/` |
| 项目文档 | `lsym-memory/docs/` |
| 技术文档 | `lsym-memory/architecture/` |
| 工作流程 | `lsym-memory/workflow/` |

### 代码与文档分离

| 类型 | 存储位置 |
|------|----------|
| 源代码 | `slhy/` 项目目录 |
| 配置文件 | `slhy/` 项目目录 |

### 对话日志管理

> 每次会话结束后，在 `conversation-logs/` 目录下创建当日的对话日志

| 类型 | 存储位置 |
|------|----------|
| 对话日志 | `lsym-memory/conversation-logs/YYYY-MM-DD.md` |
| 上下文记录 | 包含会话摘要、关键决策、文件变更 |

**目的**: 防止对话过长，保持上下文连续性 |

---

## 🔐 安全机制

| 机制 | 说明 |
|------|------|
| **MAC 校验** | 确保交易数据完整性，防篡改 |
| **分布式锁** | Redis 锁，基于 cardCode，5分钟超时 |
| **幂等性** | 基于 transNo，Redis 缓存，1小时过期 |

---

## 📊 性能指标

| 交易类型 | 响应时间(P99) | 吞吐量(TPS) |
|----------|---------------|-------------|
| 消费 | 500ms | 1000 |
| 充值 | 300ms | 500 |
| 提现 | 1000ms | 100 |
| 转账 | 500ms | 300 |
| 消费退款 | 500ms | 200 |

---

**更新日期**: 2026-03-03
**维护者**: ssssgoldhunter
