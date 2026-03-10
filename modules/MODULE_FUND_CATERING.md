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
