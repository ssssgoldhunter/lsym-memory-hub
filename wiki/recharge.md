# 充值（Recharge）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)
- 账户变动源码映射：[docs/ACCOUNT_CHANGE_SOURCE_MAP.md](../docs/ACCOUNT_CHANGE_SOURCE_MAP.md)
- 技术决策 — MAC 并发修复：[technical-decisions/MAC_CONCURRENCY_FIX.md](../technical-decisions/MAC_CONCURRENCY_FIX.md)

## 源码映射

### consume 模块（核心）

| 类 | 路径 | 职责 |
|---|---|---|
| `RechargeTrans` | `fund-catering-consume/.../flow/component/trans/recharge/RechargeTrans.java` | 充值交易主组件：创建充值主/子流水、流水唯一性校验 |
| `RechargeTransAfter` | `fund-catering-consume/.../flow/component/trans/recharge/RechargeTransAfter.java` | 充值 After 组件：刷新 MAC → 创建提现配置 → 更新账户余额 → 写变动明细 → 更新状态 |
| `TransRechargeTService` | `fund-catering-consume/.../service/TransRechargeTService.java` | 充值主表 CRUD |
| `TransRechargeSubTService` | `fund-catering-consume/.../service/TransRechargeSubTService.java` | 充值子表 CRUD |
| `BatchWithdrawLogService` | `fund-catering-consume/.../service/BatchWithdrawLogService.java` | T+1 提现批次日志服务 |

### base 模块

| 类 | 路径 | 职责 |
|---|---|---|
| `AccountChangeBatchService` | `fund-catering-base/.../service/AccountChangeBatchService.java` | `batchChangeAccountForRecharge` 接口 |
| `AccountChangeBatchServiceImpl` | `fund-catering-base/.../service/impl/AccountChangeBatchServiceImpl.java` | 充值专用批量实现：同一事务更新余额 + 创建膨胀金 |
| `BaseAccountServiceApi` | `fund-catering-base-api/...` | 账户服务 API（Feign 接口） |
| `TransAccountApi` | `fund-catering-consume-api/...` | 账户变动明细 API（Feign 接口） |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainRecharge` | `rechargeTransPack → accountCheck → merchantCheck → companyCheck → bussinessInfoCheck → cardBinInfoCheck → rechargeActivityInfoCheck → rechargeTrans → rechargeTransAfter` | 充值主链路 |

## 流程

### RechargeTrans（创建流水）

```
1. 流水唯一性校验（transNo 不能重复）
2. 生成 rechargeId（3108 + 时间戳 + 序号）
3. 创建充值主流水 trans_recharge_t（status=P）
4. 创建充值子流水 trans_recharge_sub_t
   ├─ 01/02 类型：关联活动信息，acctExpandId=null（后续 After 阶段更新）
   └─ 04 类型：关联银行账户信息
5. 设置 slot 上下文（transId, transNo, transSubNo, transSubId）
```

### RechargeTransAfter（更新账户 + 写明细）

```
1. 刷新账户快照 refreshCardSubAccount（确保最新 MAC）
2. 创建提现配置 createWaitWithDraw
   ├─ cardBin.withdraw=1 且 withdrawDays>0 → 设置 waitReleaseBalance + withdrawDate
   └─ cardBin.withdraw=1 且 withdrawDays<=0 → 设置 withdrawBalance
3. 更新子账户余额 updateSubAccountBalance
   ├─ 构建 AccountChangeBatchReq
   ├─ 01/02 类型：组装膨胀金数据（BasSubAccountExpendTReq）
   ├─ 调用 baseAccountServiceApi.batchChangeAccountForRecharge
   │   └─ 事务内：更新余额 + 创建膨胀金 → 返回 acctExpandId
   └─ 更新充值子流水的 acctExpandId
4. 写入 4 张账户变动明细表
   ├─ trans_acct_change_detail_t（01/02/04）
   ├─ trans_acct_act_sum_change_detail_t（科目汇聚，01/02 按 activityCode 反推期初）
   ├─ trans_acct_sum_change_detail_t（账户汇聚）
   └─ trans_sum_change_detail_t（交易汇聚）
5. T+1 提现：创建 batch_withdraw_log 记录
6. 更新充值主流水状态 → S
```

## 子账户类型与充值规则

| 子账户类型 | 说明 | balance | realBalance | withdrawBalance | waitReleaseBalance | 膨胀金 |
|---|---|---|---|---|---|---|
| 01 | 现金充值 | += amount | += amount | 按 cardBin 配置 | 按 cardBin 配置 | 创建膨胀金 |
| 02 | 膨胀金赠送充值 | += amount | += amount | 按 cardBin 配置 | 按 cardBin 配置 | 创建膨胀金 |
| 04 | 银行子账户充值 | += amount | += amount | 按 cardBin 配置 | 按 cardBin 配置 | 不涉及 |

### 04 账户更新规则

在 `RechargeTransAfter.updateSubAccountBalance` 中：

```
balance += amount       （必加）
realBalance += amount   （必加）

if cardBin.withdraw = 1:
  if withdrawDays <= 0:
    withdrawBalance += amount
    waitReleaseBalance = 0
  else:
    withdrawBalance = 0
    waitReleaseBalance += amount
else:
  withdrawBalance = 0
  waitReleaseBalance = 0
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_recharge_t` | 充值主表，唯一约束 `trans_no`，status: P(处理中)/S(成功)/F(失败) |
| `trans_recharge_sub_t` | 充值子表，`acct_expand_id` 在 After 阶段回填 |
| `bas_card_sub_account_t` | 子账户表（MAC CAS 更新余额） |
| `bas_sub_account_expend_t` | 膨胀金账户扩展表（01/02 充值时创建） |
| `trans_acct_change_detail_t` | 01 账户变动明细 |
| `trans_acct_change_detail_02_t` | 02 账户变动明细 |
| `trans_acct_change_detail_04_t` | 04 账户变动明细 |
| `trans_acct_act_sum_change_detail_t` | 科目汇聚变动明细 |
| `trans_acct_sum_change_detail_t` | 账户汇聚变动明细 |
| `trans_sum_change_detail_t` | 交易汇聚变动明细 |
| `batch_withdraw_log` | T+1 提现批次日志表 |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `BaseAccountServiceApi.batchChangeAccountForRecharge` | RPC | 充值专用批量账户变动（余额 + 膨胀金，同一事务） |
| `BaseAccountServiceApi.sumAvailableAmtByActivityCodesIn` | RPC | 按活动查询膨胀金可用余额（用于科目汇聚期初） |
| `TransAccountApi.batchChangeAccountDetail` | RPC | 批量创建账户变动明细（4 张表） |
| `TransRechargeTService.save` | 内部 | 保存充值主流水 |
| `TransRechargeSubTService.save` | 内部 | 保存充值子流水 |
| `TransRechargeSubTService.updateAcctExpandIdBySubId` | 内部 | 回填 acctExpandId |
| `TransRechargeSubTService.updateWaitWithDrawInfoBySubId` | 内部 | 更新提现配置 |
| `TransRechargeTService.updateRechargeStatusByRechargeId` | 内部 | 更新充值状态 |

## MAC 刷新

- 充值链路有 Redis 锁 + 只有一次账户更新 → 理论上不需要刷新
- 但 `RechargeTransAfter` 仍调用 `slot.refreshCardSubAccount(...)` 确保安全

## 关联 Wiki

- [account-change.md](account-change.md) — 账户变动核心机制（MAC CAS）
- [platform-recharge-batch.md](platform-recharge-batch.md) — 平台批量实收（同使用充值账户变动口径）
