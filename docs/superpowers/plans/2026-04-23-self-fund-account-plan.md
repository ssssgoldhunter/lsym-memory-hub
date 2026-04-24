# 自有资金账户（Self-Owned Fund Account）实施计划

> 日期：2026-04-24
> 状态：已按 v2 业务调整更新
> 对应设计：`docs/superpowers/specs/2026-04-23-self-fund-account-design.md`

## 1. 当前结论

自有资金账户 v2 的核心调整是：自有资金账户实际绑定银行卡，通知和补偿数据格式与普通充值一致，因此入金不再走独立链路。

当前实施口径：

- 入金通知：银行推送 `trans_tp=03`，但按普通 01/02 模式处理，通过银行卡反查内部账号。
- 充值 LiteFlow：继续走 `chainRecharge`，业务 `transType=C`，不使用 `transType=03` 驱动 LiteFlow。
- 补偿任务：复用现有 `ZxTransNotifyRechargeJob` / `ZxTransNotifyRechargeServiceImpl`，不新增自有资金补偿 Job。
- 转账能力：保留 Front 层 `platformPay` / `platformReceive` 抽象，用于后续平台付款/平台收款场景。
- 配置：`SELF_FUND_ACCOUNT_CONFIG` 仅用于转账方向参数，不用于入金和补偿。

## 2. 已完成范围

### 2.1 Common

- `CommonConstants.TRANS_TYPE_SELF_FUND = "03"` 已保留作为银行通知类型标识。
- 该常量不应加入 `RechargeTrans` / `RechargeTransAfter` 的 `isAccess()` 判断。

### 2.2 Web 通知入口

- `ReverseNoticeController.transNotifyZx()` 接收中信通知。
- `buildTPNotifyZxDto()` 通过 `zxMchntId` / `bankEAccountCode` 查询平台公司信息。
- `executeRechargeProcess()` 通过 `platformComCode + accountNoEnc` 查询公司，组装 `ConsumeRechargeRequest`。
- 自有资金入金请求设置 `accountType=04`，并通过 `cardCode` 驱动后续充值。

注意：允许 `trans_tp=03` 进入通知处理是必要的，但不应新增读取 `SELF_FUND_ACCOUNT_CONFIG` 的特殊分支。

### 2.3 Consume 充值链路

`chainRecharge` 保持普通充值链路：

```text
rechargeTransPack
  -> accountCheck
  -> merchantCheck
  -> companyCheck
  -> bussinessInfoCheck
  -> cardBinInfoCheck
  -> rechargeActivityInfoCheck
  -> rechargeTrans
  -> rechargeTransAfter
```

实现约束：

- `RechargeTrans.isAccess()` 只接受 `TRANS_TYPE_C` / `TRANS_TYPE_IC`。
- `RechargeTransAfter.isAccess()` 只接受 `TRANS_TYPE_C` / `TRANS_TYPE_IC`。
- 04 子账户充值时记录付款账户、付款户名、开户行、`bankEAccountId` 等子流水信息。
- 账户余额更新继续由 `baseAccountServiceApi.batchChangeAccountForRecharge` 完成。

### 2.4 Front 转账抽象

已保留 v2 需要的转账抽象：

- `BasTransTransferReq` 扩展 `bizFunc` / `fundType` / `dealType` / `bankEAccountId`。
- `BasTransTransferHandle` 新增 `platformPay()` / `platformReceive()`。
- `ZxTransTransferHandle` 实现中信 `platformPay()` / `platformReceive()`。
- `SaasZxInterService` 新增 `zxPlatformPay()` / `zxPlatformReceive()`。
- `SaasZxApi.Saas_Api.PlatformPay` / `PlatformReceive` 复用 `transfer` 接口路径。

## 3. 明确不再实施的 v1 内容

以下内容已被 v2 设计移除，不应再作为实施任务：

- 不新增 `ZX_SELF_FUND_TRANS_CONFIG` 补偿配置。
- 不新增 `ZxSelfFundNotifyRechargeService/Impl`。
- 不新增 `ZxSelfFundNotifyJobService`。
- 不新增 `ZxSelfFundNotifyRechargeJobService`。
- 不新增 `FrontTransQueryFacadeApi.queryRegisterTransPages`。
- 不新增 `BasPlatformAccountDetailQueryReq`。
- 不新增 Front 层 `queryRegisterTransPages` Handle 实现。
- 不在 `ReverseNoticeController` 中新增读取 `SELF_FUND_ACCOUNT_CONFIG` 的 03 特殊入金分支。
- 不在 `RechargeTrans` / `RechargeTransAfter` 中加入 `TRANS_TYPE_SELF_FUND` 的 `isAccess()` 分支。
- 不为入金新增 `SELF_FUND_ACCOUNT_CONFIG` SQL。

## 4. 待确认/后续范围

- 自有资金调账业务场景仍待确认。
- 2041/2042 平台付款/平台收款的业务触发时机仍待确认。
- 转账 LiteFlow 集成本期不做，当前只保留 Front 银行接口能力。
- PA 银行自有资金支持时间待确认。
- 自有资金账户变动明细策略如需单独口径，需要另行确认；当前入金复用普通充值账户变动明细。

## 5. 验收检查

代码检查时按以下清单确认：

- `slhy` 中不存在 `ZxSelfFundNotify*` Job/Service 实现。
- `RechargeTrans.isAccess()` 和 `RechargeTransAfter.isAccess()` 不接受 `TRANS_TYPE_SELF_FUND`。
- `ReverseNoticeController` 不读取 `SELF_FUND_ACCOUNT_CONFIG` 处理入金。
- `chainRecharge` 未新增自有资金专用节点。
- Front 层存在 `platformPay` / `platformReceive` 抽象和 Zx 实现。
- 若仍存在 `PARAM_ZX_SELF_FUND_TRANS_CONFIG` 之类未使用常量，应作为旧计划残留清理。
