# lsym 项目 - AI 工作配置

> 本文件为 Claude Code AI 助手在 lsym 项目中的工作配置和上下文参考

---

## 项目基本信息

| 属性 | 值 |
|------|------|
| **项目名称** | lsym / slhy (餐饮资金体系) |
| **负责人** | 李蒙 (ssssgoldhunter) |
| **主项目路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy` |
| **记忆库路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory-hub` |
| **GitHub 仓库** | https://github.com/ssssgoldhunter/lsym-memory-hub |
| **飞书文档** | https://jvn4jogcy6u.feishu.cn |
| **当前活跃分支** | `lsym_prod` |
| **生产分支** | `lsym_prod` |
| **冷冻分支** | `lsym_20260116_limeng_restruct`（2026-05 冻结） |

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | **3.2.4** | 应用框架 |
| Spring Cloud | **2023.0.1** | 微服务治理 |
| Spring Alibaba | **2023.0.1.0** | 云原生特性 |
| LiteFlow | 最新版 | 流程编排引擎（核心） |
| MyBatis Plus | 最新版 | ORM 框架 |
| ShardingSphere | 5.3.2 | 分库分表 |
| Redis | - | 分布式锁、缓存 |
| RocketMQ | - | 消息队列 |
| Nacos | - | 配置中心、服务注册发现 |
| Druid | 1.2.23 | 数据库连接池 |
| EasyExcel | 3.3.2 | Excel处理 |
| MapStruct | - | 对象映射 |
| Hutool | 5.8.21 | 工具库 |

---

## 项目架构

### 整体架构：单体 + 微服务混合（转型中）

```
slhy/
├── fund-catering/          # 原单体模块（主扫描范围 7 个子模块，不含 data-batch）
├── auth-service/           # 认证授权中心
├── gateway-service/        # API网关
├── system-service/         # 主Web服务（整合fund-catering API + 管理功能）
├── db-service/             # 数据库服务（ShardingSphere分库分表）
├── routing-service/        # 支付路由（8个子模块：alipay/wxpay/unionpay/umspay等）
├── reconcile-service/      # 对账服务
├── notify-service/         # 通知服务（邮件+XMPP+RocketMQ）
├── file-service/           # 文件存储（阿里云OSS）
├── gen-service/            # 代码生成
├── job-service/            # 定时任务（Quartz+XXL-JOB）
├── portal-service/         # Portal门户
├── migration-service/      # 数据迁移（pom.xml中已注释，暂未启用）
├── common-core/            # 公共核心组件
├── starter-modules/        # Spring Boot Starter组件（11个）
├── api-modules/            # API定义（api-db/api-routing/api-system/api-reconcile）
└── ui-modules/             # 前端模块（7个）
```

### fund-catering 子模块（原单体，当前主扫描范围）

| 模块 | 说明 |
|------|------|
| fund-catering-base | 账户基础服务（开户/冻结/解冻/账户查询） |
| fund-catering-consume | 消费服务（LiteFlow核心交易引擎） |
| fund-catering-front | 前置服务（PA/ZX平台对接） |
| fund-catering-task | 定时任务（提现/充值/转账/对账） |
| fund-catering-web | Web接口层 |
| fund-catering-management | 管理后台 |
| fund-catering-report | 报表服务 |

说明：`fund-catering-data-batch` 保留独立模块文档，但不纳入当前 fund-catering 主结构扫描和整理范围。

### 新微服务（详见 [MODULE_MICROSERVICES.md](modules/MODULE_MICROSERVICES.md)）

| 服务 | 职责 | 关键技术 |
|------|------|----------|
| auth-service | 认证授权中心 | Spring Security + Nacos |
| gateway-service | API网关 | Spring Cloud Gateway |
| system-service | 主Web服务入口 | MapStruct + EasyExcel + 美团SDK |
| db-service | 分库分表服务 | ShardingSphere |
| routing-service | 支付路由 | 8个支付渠道模块 |
| reconcile-service | 对账服务 | FTP/SFTP + 动态数据源 |
| notify-service | 通知服务 | Jakarta Mail + Smack + RocketMQ |
| file-service | 文件存储 | 阿里云OSS |
| portal-service | 门户服务 | Portal Web |
| gen-service | 代码生成 | Velocity |
| job-service | 定时任务 | Quartz + XXL-JOB |

### 基础设施模块

| 模块 | 说明 |
|------|------|
| common-core | 公共组件（BaseController/AjaxResult/PageDomain等） |
| starter-modules | Spring Boot Starter（nacos/security/redis/rocketMq/biz/mybatisplus/datascope/log/sensitive/shardingjdbc/xxljob） |
| api-modules | API定义（api-db/api-routing/api-system/api-reconcile） |
| ui-modules | 前端（cashier/mgr-ui/mgr-ui-v2/dashboard/front/front-pc/front-pc-v3） |

---

## 核心文档索引

### 必读文档（按优先级）

| 优先级 | 文档 | 路径 | 说明 |
|--------|------|------|------|
| 1 | 快速参考 | `docs/TRANSACTION_QUICK_REFERENCE.md` | 六大交易+预消费+冻结流程快速查询 |
| 1 | 完整设计文档 | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` | 最权威的设计文档 |
| 2 | 账户变动源码映射 | `docs/ACCOUNT_CHANGE_SOURCE_MAP.md` | 交易场景→源码→账户变动入口映射 |
| 2 | 框架结构 | `architecture/FRAMEWORK_STRUCTURE.md` | TransSlot/QuerySlot 详解 |
| 2 | 组件结构 | `architecture/TRANS_COMPONENT_STRUCTURE.md` | Trans 组件结构详解 |
| 3 | 框架蓝图 | `architecture/FRAMEWORK_BLUEPRINT.md` | 新项目参考 |
| 3 | 文档管理规则 | `workflow/DOCUMENT_MANAGEMENT_RULES.md` | 文档存储规范 |

### 模块文档

| 模块 | 文档 | 路径 | 说明 |
|------|------|------|------|
| 主结构 | fund-catering模块 | `modules/MODULE_FUND_CATERING.md` | 完整模块结构与账户变动 |
| 微服务 | 微服务架构 | `modules/MODULE_MICROSERVICES.md` | 12+新微服务模块文档 |
| 基础服务 | 基础服务模块 | `modules/MODULE_BASE.md` | 账户/商户/平台对接 |
| 前置服务 | 前置服务模块 | `modules/MODULE_FRONT.md` | PA/ZX平台对接 |
| 管理服务 | 管理服务模块 | `modules/MODULE_MANAGEMENT.md` | 商户，配置，结算管理 |
| 任务调度 | 任务调度模块 | `modules/MODULE_TASK.md` | XXL-Job定时任务 |
| 报表服务 | 报表模块 | `modules/MODULE_REPORT.md` | 报表生成和查询 |
| 校验组件 | Check组件 | `modules/CHECK_COMPONENTS.md` | Check组件详解 |
| API文档 | API接口文档 | `modules/API_REFERENCE.md` | 完整API接口清单 |
| 数据库 | 数据库表结构 | `modules/DATABASE_SCHEMA.md` | 核心表结构说明 |
| 账户开户 | 开户模块 | `modules/ACCOUNT_OPENING.md` | 预开户与用户注册区别 |

### 技术决策与计划

| 文档 | 路径 | 说明 |
|------|------|------|
| MAC并发修复 | `technical-decisions/MAC_CONCURRENCY_FIX.md` | MAC刷新+CAS并发保护 |
| 批量转账实现 | `technical-decisions/BATCH_TRANSFER_IMPLEMENTATION.md` | 批量上账业务 |
| MAC CAS设计 | `docs/plans/2026-03-10-mac-cas-design.md` | CAS乐观锁方案 |
| 账户变动重构设计 | `docs/superpowers/specs/2026-03-11-account-change-refactor-design.md` | 6张表迁移方案（restruct冷冻） |
| 账户变动重构TODO | `docs/superpowers/specs/2026-03-12-account-change-refactor-todo.md` | 重构待办（restruct冷冻） |
| task模块账户变动统一 | `docs/plans/2026-03-17-task-account-change-unification-plan.md` | task模块改造方案（restruct冷冻） |

---

## 核心交易流程（LiteFlow链）

| 流程 | 流程链 | 说明 |
|------|--------|------|
| **消费** | chainConsume | 02膨胀金优先扣款，支持分账 |
| **消费授权** | chainConsumeAuth | 需鉴权消费 |
| **预消费** | chainConsumePre | 预消费冻结 |
| **预消费完成** | chainConsumePreFinish | 预消费确认 |
| **消费关闭** | chainConsumeClose | 预消费关闭解冻 |
| **消费算价** | chainConsumeCal | 消费计算 |
| **消费算价订单** | chainConsumeCalOrder | 订单级消费计算 |
| **充值** | chainRecharge | 支持01现金+02膨胀金赠送 |
| **充值退款** | chainRefundRecharge | 原路退回，膨胀金收回 |
| **提现** | chainWithDraw | 自动提现+人工审核 |
| **转账** | chainTransfer | 三层锁机制，支持批量 |
| **转账(内部)** | chainTransferInner | 内部转账 |
| **转账(授权)** | chainTransferAuth | 授权转账 |
| **消费退款** | chainConsumeRefund | 按比例/按单退款 |
| **冻结** | chainFrozen | 冻结操作 |
| **解冻** | chainUnFrozen | 解冻操作 |

### 子账户类型

| 代码 | 名称 | 说明 |
|------|------|------|
| 01 | 现金账户 | 可提现 |
| 02 | 膨胀金账户 | 赠送金额，优先消费，不可提现 |
| 04 | 综合账户 | 综合子账户 |

---

## LiteFlow 组件类型

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

## 核心源码路径

| 类型 | 路径 |
|------|------|
| 消费服务 | `slhy/fund-catering/fund-catering-consume` |
| LiteFlow 配置 | `fund-catering-consume/fund-catering-consume-service/src/main/resources/liteflow/` |
| Trans 组件 | `flow/component/trans/` |
| Query 组件 | `flow/component/query/` |
| 账户变动批量服务 | `fund-catering-base/.../service/AccountChangeBatchService.java` |
| 账户变动查询 | `fund-catering-web/.../query/AcctChangeQueryController.java` |
| NPK模块 | `system-service/.../catering/controller/Npk*Controller.java` |
| 支付路由 | `routing-service/routing-{alipay|wxpay|unionpay|umspay|...}/` |

---

## 工作规范

### AI 编码准则

来源：`https://github.com/forrestchang/andrej-karpathy-skills/blob/main/CLAUDE.md`

这些准则用于减少 AI 编码时的常见问题，并与本项目规则合并执行。若与用户明确要求冲突，以用户要求为准。

#### 1. 编码前先想清楚

- 不要假设需求，不要隐藏困惑，要主动说明权衡。
- 实现前明确假设；不确定时先问。
- 如果存在多种解释，先列出来，不要静默选择。
- 如果有更简单方案，要说出来；需要时可以提出反对意见。
- 如果需求不清楚，先停下，说明困惑点，再提问。

#### 2. 简单优先

- 只写解决当前问题所需的最少代码。
- 不增加用户没有要求的功能。
- 不为单次使用的代码增加抽象。
- 不增加未要求的灵活性、配置项或扩展点。
- 不为不可能发生的场景增加复杂错误处理。
- 如果实现明显可以更短更直接，应主动简化。

#### 3. 外科手术式修改

- 只修改完成任务必须修改的内容。
- 不顺手改进相邻代码、注释或格式。
- 不重构无关代码。
- 匹配现有代码风格，即使个人偏好不同。
- 发现无关死代码或坏味道时，只说明，不擅自删除。
- 只清理由本次修改造成的未使用导入、变量或函数。
- 每一行变更都应能追溯到用户需求。

#### 4. 目标驱动执行

- 把任务转成可验证目标，并循环直到验证完成。
- Bug 修复应先复现，再修复，再验证。
- 重构要确认修改前后行为不变。
- 多步骤任务需要给出简短计划，并说明每步如何验证。
- 成功标准要清晰，避免只用"让它能工作"这类模糊目标。

### 文档存储规则

> **默认规则**：所有 md 文档和记忆体内容都放在 `lsym-memory-hub/` 项目中

| 类型 | 存储位置 |
|------|----------|
| md 文档 | `lsym-memory-hub/` |
| 记忆体内容 | `lsym-memory-hub/` |
| 项目文档 | `lsym-memory-hub/docs/` |
| 技术文档 | `lsym-memory-hub/architecture/` |
| 工作流程 | `lsym-memory-hub/workflow/` |

### 代码与文档分离

| 类型 | 存储位置 |
|------|----------|
| 源代码 | `slhy/` 项目目录 |
| 配置文件 | `slhy/` 项目目录 |

### 对话日志管理

> 每次会话结束后，在 `conversation-logs/` 目录下创建当日的对话日志

| 类型 | 存储位置 |
|------|----------|
| 对话日志 | `lsym-memory-hub/conversation-logs/YYYY-MM-DD.md` |
| 上下文记录 | 包含会话摘要、关键决策、文件变更 |

---

## 安全机制

| 机制 | 说明 |
|------|------|
| **MAC + CAS** | MAC字段作为CAS乐观锁，确保并发更新安全 |
| **分布式锁** | Redis 锁，基于 cardCode，5分钟超时 |
| **幂等性** | 基于 transNo，Redis 缓存，1小时过期 |

---

## 当前开发重点（截至2026-05，以 lsym_prod 为主线）

1. **入账识别异常告警机制** — 入账解析异常时发送898未配告警，优化任务查询和告警内容拼接
2. **提现结果查询接口优化** — 未查到记录时返回优化
3. **授信合同功能** — 权限统一、下载优化、批量异常处理、导入增加备注字段
4. **配置调整与优雅停机** — 优雅处理时间配置、bootstrap.properties 调整

### 冷冻分支工作记录（lsym_20260116_limeng_restruct）

以下重构工作已暂停，待后续恢复：

1. 账户变动原子一致性改造 — 六大交易全部使用原子一致性更新账户余额
2. AccountChangeBatchService 扩展 — 新增统一账户批量变动服务（672行新增）
3. 六大交易 After 组件重写 — Consume/Refund/Recharge/Transfer/WithDraw After 全部重写
4. 交易回溯 & 转账回溯改造 — TransRecallServiceImpl/TransferRecallServiceImpl 大幅扩展
5. 冻结流水号唯一校验
6. 消费退款汇总口径修正

> restruct 分支共 98 文件变更（+8615/-4583），合并时需注意与 prod 独有功能（入账告警、授信合同等）的冲突。

---

## 待办事项

### 账户变动模块重构（后期恢复）

| 属性 | 值 |
|------|-----|
| **状态**: 冷冻（随 restruct 分支暂停） |
| **创建日期**: 2026-03-12 |
| **优先级**: 中 |

**2026-03-12 头脑风暴讨论确认**：

1. **API 设计**：采用方案A（按场景细分 13 个 API）
2. **接口改动**：扩展现有 BaseAccountServiceApi
3. **report-service**：DTS 同步，无需迁移

**待办文档**: `docs/superpowers/specs/2026-03-12-account-change-refactor-todo.md`

---

## AI 自定义指令

### 任务结束流程

> **约束项**: 在每次任务结束后，自动 push 到 GitHub

**执行流程**:
1. 任务完成后，自动执行 push
2. 更新 `conversation-logs/YYYY-MM-DD.md`（如有必要）
3. 提交并推送到 GitHub

### 自动 Push 规则

> **重要**: lsym-memory-hub 每次会话结束或必要时，**自动 push 到远程仓库**，保证在线 memory 实时同步

**执行流程**:
1. 有新提交时，自动执行 `git push origin main`
2. 无需每次询问用户
3. 确保记忆库与 GitHub 保持同步

### 对话日志更新时机

| 场景 | 是否更新日志 |
|------|-------------|
| 完成一个功能开发 | 建议 |
| 修复一个 Bug | 建议 |
| 文档整理/优化 | 建议 |
| 简单代码查看 | 可选 |
| 快速问题咨询 | 可选 |

---

**更新日期**: 2026-05-04
**维护者**: ssssgoldhunter
