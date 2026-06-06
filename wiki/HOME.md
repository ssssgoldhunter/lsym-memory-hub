# lsym Wiki — 文档与代码映射索引

> 本目录维护**文档 → 代码**的映射关系，方便快速定位功能实现。

---

## 一、代码模块 → 源码路径

| 代码模块 | 源码路径 | 职责 |
|----------|----------|------|
| common-core | `slhy/common-core/` | 公共核心：工具类、常量、DTO、枚举、基础配置 |
| api-modules | `slhy/api-modules/` | API 接口定义层：对外暴露的接口和 DTO |
| ui-modules | `slhy/ui-modules/` | 前端 UI 模块 |
| fund-catering | `slhy/fund-catering/` | 资金体系核心（7子模块） |
| ├─ base | `slhy/fund-catering/fund-catering-base/` | 基础服务：账户、余额、基础组件 |
| ├─ consume | `slhy/fund-catering/fund-catering-consume/` | 消费/消费退款交易 |
| ├─ front | `slhy/fund-catering/fund-catering-front/` | 前置服务：路由、前置校验 |
| ├─ management | `slhy/fund-catering/fund-catering-management/` | 管理服务：充值、提现、转账、后台管理 |
| ├─ report | `slhy/fund-catering/fund-catering-report/` | 报表服务 |
| ├─ task | `slhy/fund-catering/fund-catering-task/` | 任务调度：批处理、定时任务 |
| └─ web | `slhy/fund-catering/fund-catering-web/` | Web 层：Controller、启动入口 |
| starter-modules | `slhy/starter-modules/` | 启动模块 |
| auth-service | `slhy/auth-service/` | 认证鉴权服务 |
| gateway-service | `slhy/gateway-service/` | 网关服务 |
| routing-service | `slhy/routing-service/` | 路由服务（9子模块） |
| reconcile-service | `slhy/reconcile-service/` | 对账服务 |
| job-service | `slhy/job-service/` | 分布式任务调度 |
| db-service | `slhy/db-service/` | 数据库服务 |
| file-service | `slhy/file-service/` | 文件服务 |
| gen-service | `slhy/gen-service/` | 代码生成服务 |
| notify-service | `slhy/notify-service/` | 通知服务 |
| portal-service | `slhy/portal-service/` | 门户服务 |
| system-service | `slhy/system-service/` | 系统管理服务 |
| migration-service | `slhy/migration-service/` | 数据迁移服务 |
| deploy | `slhy/deploy/` | 部署配置 |
| script | `slhy/script/` | 脚本工具 |
| tools | `slhy/tools/` | 开发工具 |
| km | `slhy/km/` | 知识管理 |

---

## 二、文档 → 代码映射

### 核心设计文档 (`docs/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 供应链设计 V5.5 | `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` | fund-catering 全模块 |
| 六大交易快速参考 | `docs/TRANSACTION_QUICK_REFERENCE.md` | fund-catering-consume, fund-catering-management |
| 账户变动源码映射 | `docs/ACCOUNT_CHANGE_SOURCE_MAP.md` | fund-catering-base |

### 规格说明 (`docs/superpowers/specs/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 账户变动重构设计 | `docs/superpowers/specs/2026-03-11-account-change-refactor-design.md` | fund-catering-base |
| 账户变动重构 TODO | `docs/superpowers/specs/2026-03-12-account-change-refactor-todo.md` | fund-catering-base |
| 特殊账户充值设计 | `docs/superpowers/specs/2026-04-22-special-account-recharge-design.md` | fund-catering-management |
| 自有资金池设计 | `docs/superpowers/specs/2026-04-23-self-fund-account-design.md` | fund-catering-management |
| 平台充值批处理设计 | `docs/superpowers/specs/2026-05-19-platform-recharge-batch-design.md` | fund-catering-task, fund-catering-management |

### 计划文档 (`docs/superpowers/plans/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 自有资金池计划 | `docs/superpowers/plans/2026-04-23-self-fund-account-plan.md` | fund-catering-management |
| 平台充值批处理计划 | `docs/superpowers/plans/2026-05-19-platform-recharge-batch.md` | fund-catering-task, fund-catering-management |

### 架构文档 (`architecture/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 框架蓝图 | `architecture/FRAMEWORK_BLUEPRINT.md` | common-core (框架基础) |
| 框架结构 TransSlot/QuerySlot | `architecture/FRAMEWORK_STRUCTURE.md` | common-core, fund-catering-base |
| Trans 组件结构 | `architecture/TRANS_COMPONENT_STRUCTURE.md` | fund-catering-base |

### 模块文档 (`modules/`)

| 文档 | 路径 | 对应源码 |
|------|------|----------|
| fund-catering 主结构 | `modules/MODULE_FUND_CATERING.md` | `slhy/fund-catering/` |
| 基础服务模块 | `modules/MODULE_BASE.md` | `slhy/fund-catering/fund-catering-base/` |
| 前置服务模块 | `modules/MODULE_FRONT.md` | `slhy/fund-catering/fund-catering-front/` |
| 管理服务模块 | `modules/MODULE_MANAGEMENT.md` | `slhy/fund-catering/fund-catering-management/` |
| 任务调度模块 | `modules/MODULE_TASK.md` | `slhy/fund-catering/fund-catering-task/` |
| 报表模块 | `modules/MODULE_REPORT.md` | `slhy/fund-catering/fund-catering-report/` |
| 数据批处理模块 | `modules/MODULE_DATA_BATCH.md` | `slhy/fund-catering/fund-catering-data-batch/` |
| 微服务架构模块 | `modules/MODULE_MICROSERVICES.md` | auth-service, gateway-service, routing-service 等 |
| 开户模块 | `modules/ACCOUNT_OPENING.md` | fund-catering-management |
| Check 组件 | `modules/CHECK_COMPONENTS.md` | fund-catering-base |
| API 接口文档 | `modules/API_REFERENCE.md` | api-modules |
| 数据库表结构 | `modules/DATABASE_SCHEMA.md` | db-service |

### 技术决策 (`technical-decisions/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 批量转账实现 | `technical-decisions/BATCH_TRANSFER_IMPLEMENTATION.md` | fund-catering-management |
| MAC 并发修复 | `technical-decisions/MAC_CONCURRENCY_FIX.md` | fund-catering-base |

### 业务流程 (`business-flows/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 消费流程图 | `business-flows/CONSUME_FLOW_DIAGRAMS.md` | fund-catering-consume |

### 主题页 (`topics/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 消费退款 | `topics/consume-refund.md` | fund-catering-consume |
| 账户变动 | `topics/account-change.md` | fund-catering-base |
| 自有资金池 | `topics/self-fund-account.md` | fund-catering-management |

### Bug 记录 (`bugs/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 平台充值卡锁异步释放 | `bugs/2026-05-26-platform-recharge-card-lock-async-release.md` | fund-catering-task |

### 需求 (`requirements/`)

| 文档 | 路径 | 对应代码模块 |
|------|------|-------------|
| 平台充值批处理需求 | `requirements/2026-05-18-platform-recharge-batch.md` | fund-catering-task, fund-catering-management |

---

## 三、六大交易 → 代码入口

| 交易类型 | 核心包路径 | LiteFlow Chain |
|----------|-----------|----------------|
| 消费 | `fund-catering-consume` → consume 相关 | 消费链路 |
| 充值 | `fund-catering-management` → recharge 相关 | 充值链路 |
| 充值退款 | `fund-catering-management` → recharge refund 相关 | 充值退款链路 |
| 提现 | `fund-catering-management` → withdraw 相关 | 提现链路 |
| 转账 | `fund-catering-management` → transfer 相关 | 转账链路 |
| 消费退款 | `fund-catering-consume` → consume refund 相关 | 消费退款链路 |

---

## 四、反向索引：代码路径 → 文档

```
slhy/fund-catering/
├── fund-catering-base/        → MODULE_BASE.md, FRAMEWORK_STRUCTURE.md, account-change.md, CHECK_COMPONENTS.md
├── fund-catering-consume/     → MODULE_FUND_CATERING.md, CONSUME_FLOW_DIAGRAMS.md, consume-refund.md
├── fund-catering-front/       → MODULE_FRONT.md
├── fund-catering-management/  → MODULE_MANAGEMENT.md, self-fund-account.md, special-account-recharge-design.md
├── fund-catering-report/      → MODULE_REPORT.md
├── fund-catering-task/        → MODULE_TASK.md, platform-recharge-batch-design.md
├── fund-catering-web/         → MODULE_FUND_CATERING.md
└── fund-catering-data-batch/  → MODULE_DATA_BATCH.md

slhy/common-core/              → FRAMEWORK_BLUEPRINT.md, LITEFLOW_SKILLS.md
slhy/api-modules/              → API_REFERENCE.md
slhy/routing-service/          → MODULE_MICROSERVICES.md
slhy/reconcile-service/        → MODULE_MICROSERVICES.md
```

---

## 五、维护说明

- 新增文档时，同步更新本文件对应章节
- 新增代码模块时，在「一、代码模块」中添加条目
- 文档 → 代码的映射关系变更时，更新「二、文档 → 代码映射」和「四、反向索引」

最后更新：2026-06-06
