# fund-catering 主结构文档

> 本文档基于 `2026-04-27` 对 `slhy/fund-catering` 的源码扫描整理。
> 当前主扫描范围排除 `fund-catering-data-batch`。

## 1. 范围说明

`fund-catering` 是餐饮资金体系的原单体业务模块集合。当前主结构整理只覆盖以下 7 个核心模块：

| 模块 | Java 文件数 | Controller 标记数 | Service 相关标记数 | 主要职责 |
|------|-------------|-------------------|--------------------|----------|
| `fund-catering-base` | 403 | 22 | 158 | 账户、商户、平台、基础资料、账户安全 |
| `fund-catering-consume` | 528 | 15 | 154 | LiteFlow 交易主流程、消费/充值/提现/转账/退款 |
| `fund-catering-front` | 214 | 12 | 68 | PA/ZX 等前置平台对接、外部交易路由 |
| `fund-catering-management` | 167 | 7 | 69 | 商户、门店、结算机构、配置管理 |
| `fund-catering-report` | 589 | 35 | 223 | 交易、账户、清结算、数据查询报表 |
| `fund-catering-task` | 83 | 0 | 107 | 定时任务、提现状态更新、撤销/回溯、上账 |
| `fund-catering-web` | 248 | 29 | 1 | Web API 聚合、管理查询入口 |

说明：

- `fund-catering-data-batch` 仍存在于源码父 `pom.xml` 中，也保留独立文档 `modules/MODULE_DATA_BATCH.md`。
- 本文档不扫描、不维护 `fund-catering-data-batch` 的模块细节。
- `fund-catering-report` 下存在 `controller/databatch`、`mapper/databatch` 包名，这是报表模块内的数据查询口径，不等同于纳入 `fund-catering-data-batch` 模块。

## 2. Maven 模块结构

```text
slhy/fund-catering/
├── fund-catering-base/
│   ├── fund-catering-base-api/
│   └── fund-catering-base-service/
├── fund-catering-consume/
│   ├── fund-catering-consume-api/
│   ├── fund-catering-consume-common/
│   └── fund-catering-consume-service/
├── fund-catering-front/
│   ├── fund-catering-front-api/
│   ├── fund-catering-front-common/
│   └── fund-catering-front-service/
├── fund-catering-management/
│   ├── fund-catering-management-api/
│   ├── fund-catering-management-common/
│   └── fund-catering-management-service/
├── fund-catering-report/
│   ├── fund-catering-report-api/
│   └── fund-catering-report-service/
├── fund-catering-task/
└── fund-catering-web/
```

## 3. 启动类

| 模块 | 启动类 |
|------|--------|
| `fund-catering-base` | `fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/BaseServiceApplication.java` |
| `fund-catering-consume` | `fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/ConsumeServiceApplication.java` |
| `fund-catering-front` | `fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/FrontServiceApplication.java` |
| `fund-catering-management` | `fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/ManagementServiceApplication.java` |
| `fund-catering-report` | `fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/ReportServiceApplication.java` |
| `fund-catering-task` | `src/main/java/com/chinaums/erp/slhy/catering/task/TaskServiceApplication.java` |
| `fund-catering-web` | `src/main/java/com/chinaums/erp/slhy/catering/web/WebServiceApplication.java` |

## 4. 模块职责

### 4.1 fund-catering-base

基础服务模块，负责账户和基础资料能力。

主要边界：

- 账户、子账户、综合账户、账户安全。
- 商户、机构、平台、合同、银行卡、卡模板等基础资料。
- 对外暴露 base API，供 consume、web、task、front 等模块调用。
- 账户变动相关能力重点关注 `BaseAccountServiceApi` 和 `AccountChangeBatchService`。

### 4.2 fund-catering-consume

交易核心模块，负责 LiteFlow 交易链路。

主要边界：

- 消费、消费授权、预消费、预消费完成、消费关闭。
- 充值、充值退款、提现、转账、消费退款。
- 冻结、解冻、查询链路。
- 交易流水、子流水、异常交易、账户变动明细、平台通知等交易侧数据。

关键配置：

- `fund-catering-consume-service/src/main/resources/liteflow/consume.el.xml`
- `fund-catering-consume-service/src/main/resources/liteflow/check.el.xml`
- `fund-catering-consume-service/src/main/resources/liteflow/query.el.xml`

### 4.3 fund-catering-front

前置服务模块，负责对接外部平台和通道。

主要边界：

- PA/ZX 平台前置交易。
- 前置账户、文件、消息、查询和验证码接口。
- 对内通过 `fund-catering-front-api` 暴露 Feign API。

### 4.4 fund-catering-management

管理后台服务模块，负责商户和结算配置管理。

主要边界：

- 商户、门店、合同、费用、结算机构。
- NPK 商户、门店、业务项目管理。
- 管理侧配置能力。

### 4.5 fund-catering-report

报表服务模块，负责查询和报表输出。

主要边界：

- 消费、充值、提现、账户、交易明细报表。
- 清结算、门店、分账、对账类查询。
- `databatch` 包名下的控制器和 Mapper 属于报表服务内的数据查询口径。

### 4.6 fund-catering-task

任务调度模块，负责异步和补偿类任务。

主要边界：

- 提现结果更新和后处理。
- 充值通知处理。
- 转账撤销、交易回溯。
- 对账、日终汇总、Entry 上账、账单任务。

排查 task 模块时优先关注：

- 是否仍直接调用 `updateCardSubAccount`。
- 是否账户更新后手工写入 `createTransAcctChange`。
- 当前类是正常后处理，还是补偿/回溯逻辑。

### 4.7 fund-catering-web

Web 聚合入口模块，负责管理端和查询端 API。

主要边界：

- 管理端查询和操作入口。
- 账户变动查询入口。
- 聚合 consume、base、report、management 等模块能力。

## 5. LiteFlow 链路清单

### 5.1 业务处理链

| 链名 | 说明 |
|------|------|
| `chainConsume` | 消费交易 |
| `chainConsumeAuth` | 消费授权 |
| `chainConsumePre` | 预消费 |
| `chainConsumePreFinish` | 预消费完成 |
| `chainConsumeClose` | 消费关闭 |
| `chainConsumeCal` | 消费算价 |
| `chainConsumeCalOrder` | 订单级消费算价 |
| `chainRecharge` | 充值 |
| `chainRefundRecharge` | 充值退款 |
| `chainWithDraw` | 提现 |
| `chainTransfer` | 转账 |
| `chainTransferInner` | 内部转账 |
| `chainTransferInnerPre` | 内部转账预处理 |
| `chainTransferTiPre` | TI 转账预处理 |
| `chainTransferAuth` | 授权转账 |
| `chainTransferReSendVerification` | 转账重发验证码 |
| `chainConsumeRefund` | 消费退款 |
| `chainFrozen` | 冻结 |
| `chainUnFrozen` | 解冻 |

### 5.2 查询链

| 链名 | 说明 |
|------|------|
| `chainWithdrawResultQuery` | 提现结果查询 |
| `chainRechargeResultQuery` | 充值结果查询 |
| `chainFrozenDetailQuery` | 冻结明细查询 |
| `chainConsumeResultQuery` | 消费结果查询 |
| `chainTransferResultQuery` | 转账结果查询 |
| `chainWithDataStore` | 数据存储 |

## 6. 账户变动和一致性重点

### 6.1 账户变动操作类型

| 操作类型 | 涉及数据 | 说明 |
|----------|----------|------|
| 冻结/解冻 | 子账户、冻结明细 | 冻结或释放账户金额 |
| 账户变动 | 子账户、科目账户、变动明细 | 余额和明细同步变动 |
| Entry 上账 | Entry 明细 | 消费和预消费完成等场景的异步上账 |

### 6.2 关键 API

源码位置：

`fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/AccountChangeBatchService.java`

| 方法 | 主要场景 |
|------|----------|
| `batchChangeAccountForConsume` | 消费 |
| `batchChangeAccountForRecharge` | 充值 |
| `batchChangeAccountForRefundRecharge` | 充值退款 |
| `batchChangeAccountForRefundConsume` | 消费退款 |
| `batchChangeAccount` | 转账、提现等通用账户变动 |

### 6.3 排查重点

- 子账户更新依赖 `MAC/CAS`。
- 多次更新同一账户前要关注账户信息刷新。
- 交易、任务和回溯路径要确认是否走统一账户变动入口。
- 账户余额更新和明细写入是否处在同一事务边界内，是一致性排查重点。

## 7. 相关文档

| 文档 | 说明 |
|------|------|
| `docs/TRANSACTION_QUICK_REFERENCE.md` | 六大交易快速参考 |
| `docs/SUPPLY_CHAIN_DESIGN_V5.5.md` | 业务设计主文档 |
| `docs/ACCOUNT_CHANGE_SOURCE_MAP.md` | 账户变动源码映射 |
| `architecture/FRAMEWORK_STRUCTURE.md` | LiteFlow 框架结构 |
| `architecture/TRANS_COMPONENT_STRUCTURE.md` | Trans 组件结构 |
| `modules/MODULE_DATA_BATCH.md` | data-batch 独立模块文档，不纳入本文主结构 |
