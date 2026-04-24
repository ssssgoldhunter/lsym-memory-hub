# 清结算项目 (fund-catering) 结构

## 模块汇总

| 模块 | 说明 | 关键功能 |
|-----|------|---------|
| fund-catering-base | 账户基础服务 | 开户、冻结/解冻、账户查询 |
| fund-catering-consume | 消费服务 | 消费交易、异常处理 |
| fund-catering-task | 定时任务 | 提现、充值、转账、对账 |
| fund-catering-data-batch | 批处理 | 手工账务、店铺费用、分账台账 |
| fund-catering-front | 前端路由 | 交易路由、PA/ZX通道 |
| fund-catering-management | 管理后台 | 配置、服务商、结算agency |
| fund-catering-report | 报表服务 | 消费/充值/账户报表 |
| fund-catering-web | Web接口 | API接口、查询、管理 |

## 项目路径

```
slhy/fund-catering/
├── fund-catering-base/          # 账户基础服务
├── fund-catering-consume/       # 消费服务
├── fund-catering-task/          # 定时任务服务
├── fund-catering-data-batch/    # 批处理服务
├── fund-catering-front/         # 前端路由服务
├── fund-catering-management/    # 管理后台服务
├── fund-catering-report/        # 报表服务
└── fund-catering-web/           # Web接口服务
```

## 核心业务

### 账户服务 (fund-catering-base)

| 服务 | 说明 |
|-----|------|
| AccountService | 账户管理 |
| AccountQueryService | 账户查询 |
| BasBusinessInfoService | 商户信息 |
| BasBankInfoService | 银行信息 |
| BasContractInfoService | 合同管理 |
| BaseAccountSecurityService | 账户安全 |

### 交易服务 (fund-catering-task)

| 业务类型 | 服务 |
|---------|------|
| **提现** | ZxWithDrawUpdateStatusService, PaWithDrawUpdateStatusService, WithDrawBatchService |
| **充值** | ZxTransNotifyRechargeService, PaTransNotifyRechargeService |
| **转账** | TransferAtService, TransferRecallService |
| **对账** | AccountCheckService, DaySumAmtService, AccountEntryService |
| **账单** | BillJobService, ContractAndBillService |

### 消费服务 (fund-catering-consume)

| Controller | 说明 |
|------------|------|
| TransConsumeController | 消费交易 |
| TransConsumePaController | PA消费 |
| TransConsumeZxController | ZX消费 |
| AbnormalProcessController | 异常处理 |
| CreditInterestDetailController | 利息明细 |

### 批处理服务 (fund-catering-data-batch)

| Controller | 说明 |
|------------|------|
| ManualAccountFacadeController | 手工账务 |
| ShopFeeController | 店铺费用 |
| FzLedgerController | 分账台账 |
| SummaryAccountDataController | 账户汇总数据 |
| SettleCleanController | 结算清分 |

---

## 交易场景与账户变动操作

> 账户变动操作组合说明（基于 liteflow 流程配置 consume.el.xml 扫描）

### 账户变动操作类型

| 操作类型 | 涉及的表 | 说明 |
|---------|----------|------|
| **冻结/解冻** | 子账户表 + 冻结明细表 | 冻结/解冻账户金额 |
| **账户变动** | 子账户表 + 科目账户表 + 4张变动明细表 | 余额变动记录 |
| **Entry上账** | Entry明细表 | 异步上账（仅消费/预消费完成） |

### 交易场景与账户变动操作对应关系

| 交易场景 | 冻结 | 账户变动(4张) | 解冻 | Entry上账 |
|----------|------|---------------|------|-----------|
| 消费 | ✅ | ✅ | ✅ | ✅ |
| 充值 | - | ✅ | - | - |
| 消费退款(04) | ✅ | ✅ | ✅ | - |
| 消费退款(01) | - | ✅ | - | - |
| 充值赠送 | - | ✅ | - | - |
| 转账 | ✅ | ✅ | ✅ | - |
| 提现 | ✅ | ✅ | ✅ | - |
| 预消费 | ✅ | - | - | - |
| 预消费完成 | ✅ | ✅ | ✅ | ✅ |
| 冻结 | ✅ | - | - | - |
| 解冻 | - | - | ✅ | - |

### LiteFlow 流程配置

- 配置文件: `fund-catering-consume-service/src/main/resources/liteflow/consume.el.xml`
- 组件目录: `flow/component/trans/`

### 待迁移到 base 的 6 张表

> 改造目标：将以下 6 张表从 consume 库迁移到 base 库，实现统一事务管理

| 序号 | 表名 | 说明 |
|------|------|------|
| 1 | trans_acct_change_detail_t | 账户金额变动明细表 |
| 2 | trans_acct_sum_change_detail_t | 账户汇总变动明细表 |
| 3 | trans_acct_act_sum_change_detail_t | 账户活动汇总变动明细表 |
| 4 | trans_sum_change_detail_t | 总账户变动表 |
| 5 | trans_acct_frozen_change_detail_t | 冻结明细表 |
| 6 | trans_acct_change_entry_detail_t | 变更Entry明细表 |

### API 封装现状（AccountChangeBatchService）

> 已落地实现：fund-catering-base 中 `AccountChangeBatchService` 提供统一账户批量变动接口

**源码位置**: `fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/AccountChangeBatchService.java`

| 序号 | 方法 | 交易场景 | 说明 |
|------|------|----------|------|
| 1 | `batchChangeAccountForConsume` | 消费 | 冻结+账户变动+解冻+Entry上账 |
| 2 | `batchChangeAccountForRecharge` | 充值 | 账户变动 |
| 3 | `batchChangeAccountForRefundRecharge` | 充值退款 | 账户变动 |
| 4 | `batchChangeAccountForRefundConsume` | 消费退款 | 冻结+账户变动+解冻 |
| 5 | `batchChangeAccount` | 通用 | 转账/提现等通用账户变动 |

**核心改造**：六大交易（消费/充值/充值退款/消费退款/转账/提现）已全部使用 **原子一致性** 更新账户余额。

### 账户变动查询（AcctChangeQueryController）

**源码位置**: `fund-catering/fund-catering-web/.../query/AcctChangeQueryController.java`

| 接口 | 说明 |
|------|------|
| `scTransAccChange` | 余额变动记录查询 |
| `scTransAcctSumChange` | 子账户变动明细（热库） |
| `scTransAcctActSumChange` | 子账户科目变动明细（热库） |
| `scTransAcctSumChange02` | 02子账户变动明细（热库） |
| `scTransAcctActSumChange02` | 02科目变动明细（热库） |
| `scTransSumChange` | 总账户变动表查询（热库） |
| `scTransSumChangeDetail` | 总账户变动明细（按交易号+商户号） |
| `sumMonthWithDrawDetail` | 提现月度汇总查询 |

---

## 完整 LiteFlow 链路清单

> 基于 consume.el.xml / check.el.xml 当前配置

### 业务处理链（Trans）

| 链名 | 说明 |
|------|------|
| chainConsume | 消费交易 |
| chainConsumeAuth | 消费授权（需鉴权） |
| chainConsumePre | 预消费 |
| chainConsumePreFinish | 预消费完成 |
| chainConsumeClose | 消费关闭（预消费解冻） |
| chainConsumeCal | 消费算价 |
| chainConsumeCalOrder | 订单级消费算价 |
| chainRecharge | 充值 |
| chainRefundRecharge | 充值退款 |
| chainWithDraw | 提现 |
| chainTransfer | 转账（API） |
| chainTransferInner | 内部转账 |
| chainTransferInnerPre | 内部转账预处理 |
| chainTransferTiPre | TI转账预处理 |
| chainTransferAuth | 授权转账 |
| chainTransferReSendVerification | 转账重发验证码 |
| chainConsumeRefund | 消费退款 |
| chainFrozen | 冻结 |
| chainUnFrozen | 解冻 |

### 查询链（Query）

| 链名 | 说明 |
|------|------|
| chainWithdrawResultQuery | 提现结果查询 |
| chainRechargeResultQuery | 充值结果查询 |
| chainFrozenDetailQuery | 冻结明细查询 |
| chainConsumeResultQuery | 消费结果查询 |
| chainTransferResultQuery | 转账结果查询 |
| chainWithDataStore | 数据存储 |
