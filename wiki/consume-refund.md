# 消费退款（Consume Refund）

## 关联文档
- 主题页：[topics/consume-refund.md](../topics/consume-refund.md)
- 账户变动源码映射：[docs/ACCOUNT_CHANGE_SOURCE_MAP.md](../docs/ACCOUNT_CHANGE_SOURCE_MAP.md)
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)
- 技术决策 — MAC 并发修复：[technical-decisions/MAC_CONCURRENCY_FIX.md](../technical-decisions/MAC_CONCURRENCY_FIX.md)

## 源码映射

### consume 模块（核心）

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransRefundSplitSwitch` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundSplitSwitch.java` | 退款分流 SWITCH：选择比例退款 or 逐笔退款 |
| `ConsumeTransRefundSplitPercentPack` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundSplitPercentPack.java` | 比例退款分摊：账户类型层/科目层/流水层都按比例 |
| `ConsumeTransRefundSplitOrderPack` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundSplitOrderPack.java` | 逐笔退款分摊：流水层按可退余额贪心分配 |
| `ConsumeTransRefund04` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefund04.java` | 04 退款处理（银行侧退款） |
| `ConsumeTransRefund04Process` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefund04Process.java` | 04 退款后续处理 |
| `ConsumeTransRefund04After` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefund04After.java` | 04 冻结明细回退（只处理冻结，不更新余额） |
| `ConsumeTransRefund01` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefund01.java` | 01 退款处理 |
| `ConsumeTransRefund02` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefund02.java` | 02 退款处理 |
| `ConsumeTransRefundRecharge01` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundRecharge01.java` | Model1 01 退款处理 |
| `ConsumeTransRefundAfter` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundAfter.java` | 消费退款 After（Model04）：批量更新账户 + 膨胀金恢复 + 04 解冻 |
| `ConsumeTransRefundModel1After` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundModel1After.java` | 消费退款 After（Model01）：批量更新账户 + 膨胀金恢复 |

### base 模块

| 类 | 路径 | 职责 |
|---|---|---|
| `AccountChangeBatchService` | `fund-catering-base/.../service/AccountChangeBatchService.java` | `batchChangeAccountForRefundConsume` 接口 |
| `AccountChangeBatchServiceImpl` | `fund-catering-base/.../service/impl/AccountChangeBatchServiceImpl.java` | 消费退款专用：同一事务更新余额 + 恢复膨胀金 |

## LiteFlow 链路

### chainConsumeRefund

```
THEN(
    consumeTransRefundPack,                          // 退款参数封装
    accountCheck,                                    // 账户校验
    merchantCheck,                                   // 商户校验
    companyCheck,                                    // 企业校验
    bankInfoCheck,                                   // 银行信息校验
    accountBankInfoCheck,                            // 银行账户校验
    cardBinInfoCheck,                                // 卡Bin校验
    bussinessInfoCheck,                              // 业务信息校验
    platformInfoCheck,                               // 平台信息校验
    refundRechargeActivityInfoCheck,                 // 活动信息校验
    SWITCH(consumeTransRefundSplitSwitch).TO(         // 退款模式分流
        bashChainConsumeTransRefundSplitPercentPack,  // → 比例退款
        bashChainConsumeTransRefundSplitOrderPack     // → 逐笔退款
    )
);
```

### Model04 退款链（bashChainRefundModel04）

```
THEN(
    consumeTransRefund04,              // 04 银行侧退款
    consumeTransRefund04Process,       // 04 退款后续处理
    consumeTransRefund01,              // 01 退款
    consumeTransRefund02,              // 02 退款
    consumeTransRefundAfter            // After：批量账户变动
);
```

### Model01 退款链（bashChainRefundModel01）

```
THEN(
    consumeTransRefundRecharge01,      // 01 退款（充值退款模式）
    consumeTransRefund01,              // 01 退款
    consumeTransRefund02,              // 02 退款
    consumeTransRefundModel1After      // After：批量账户变动
);
```

## 退款模式

### 分流规则（consumeTransRefundSplitSwitch）

根据退款请求中的标记选择比例退款或逐笔退款，SWITCH 组件根据条件路由到 `bashChainConsumeTransRefundSplitPercentPack` 或 `bashChainConsumeTransRefundSplitOrderPack`。

两者内部又通过 SWITCH 路由到 `bashChainRefundModel04`（含 04 退款）或 `bashChainRefundModel01`（纯 01/02 退款）。

### 比例退款（ConsumeTransRefundSplitPercentPack）

| 层级 | 分摊方式 | 兜底 |
|---|---|---|
| 账户类型层 | 01/02/04 按原消费金额占比分摊 | 需要补充分配 |
| 科目层 | 01/02 按 activityType 分摊，04 跳过 | 需要补充分配 |
| 流水层 | 按比例 + 补充分配 | 需要补充分配 |

### 逐笔退款（ConsumeTransRefundSplitOrderPack）

| 层级 | 分摊方式 | 兜底 |
|---|---|---|
| 账户类型层 | 01/02/04 按原消费金额占比分摊 | 有兜底 |
| 科目层 | 01/02 按 activityType 分摊，04 跳过 | 有兜底 |
| 流水层 | 按可退余额逐笔贪心消耗 | 无兜底（受可退余额约束） |

## 三级分摊流程

```text
用户传入 subTransList
  → 按 receiveCardCode 合并 receiveAmount（同卡合并）
  → 账户类型层：当前收款卡内 01/02/04 原消费金额占比分摊
  → 科目层：01/02 按 activityType 分摊，04 跳过
  → 流水层：比例模式按比例+补充分配；逐笔模式按可退余额贪心分配
  → 兜底结果回写 cancelXXAmt 并参与子单落地
```

## 关键业务规则

### 同卡合并

- 同一 `receiveCardCode` 多条退款明细先合并
- 业务规则：同一收款卡唯一绑定商户，不会出现同卡多个 `receiveMerchantId`

### 退款金额校验

- 每张 `receiveCardCode` 独立校验本卡可退余额
- 分母：当前收款卡下 01 + 02 + 04 的原消费金额合计
- 01、02、04 都支持退款
- 04 既支持原路退回，也支持按配置转 01

### cancelXXAmt 回写

- 兜底后的金额**必须**回写最终 `cancel01Amt`、`cancel02Amt`、`cancel04Amt`
- 如果只更新临时 `accountRefundAmtMap` 但不回写 `cancelXXAmt`，最终退款子单金额仍会落旧值

### 04 解冻

- `ConsumeTransRefundAfter` 中通过 `createFrozenDetail` 执行 04 解冻
- `frozenType = "UF"`（解冻）
- 不设置 `frozenTransNo`，依赖自动生成

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_consume_t` | 消费主表（退款更新状态） |
| `trans_consume_sub_t` | 消费子表（退款子单） |
| `trans_consume_sub_rec_t` | 消费子记录表（退款明细） |
| `bas_card_sub_account_t` | 子账户表（MAC CAS 更新余额） |
| `bas_sub_account_expend_t` | 膨胀金账户扩展表（恢复膨胀金） |
| `trans_acct_change_detail_t` | 01 账户变动明细 |
| `trans_acct_change_detail_02_t` | 02 账户变动明细 |
| `trans_acct_change_detail_04_t` | 04 账户变动明细 |
| `trans_acct_frozen_change_detail_t` | 冻结/解冻变动明细（04 退款解冻） |
| `trans_acct_act_sum_change_detail_t` | 科目汇聚变动明细 |
| `trans_acct_sum_change_detail_t` | 账户汇聚变动明细 |
| `trans_sum_change_detail_t` | 交易汇聚变动明细 |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `BaseAccountServiceApi.batchChangeAccountForRefundConsume` | RPC | 消费退款专用批量账户变动（余额 + 膨胀金恢复） |
| `TransAccountApi.batchChangeAccountDetail` | RPC | 批量创建账户变动明细（4 张表） |
| `TransAcctFrozenChangeDetailTService.createFrozenDetail` | RPC | 04 解冻（创建 UF 类型冻结明细） |

## MAC 刷新

- 消费退款链路有 Redis 锁，但有多次账户更新（04/01/02）
- `ConsumeTransRefundAfter` 在退款前刷新账户：`refreshCardSubAccount`
- `ConsumeTransRefundModel1After` 同样刷新

## Key Fix Memory

- 2026-05-12 修复过"极值操作没有兜底导致比例退款分摊失败"的问题
- 触发兜底时日志需要带 `trans_no`，方便从交易流水定位
- 如果只更新临时 `accountRefundAmtMap` 但没有重新回写 `cancelXXAmt`，最终退款子单金额仍会落旧值

## 关联 Wiki

- [account-change.md](account-change.md) — 账户变动核心机制（MAC CAS）
- [recall.md](recall.md) — 回溯（消费回溯也涉及 04 退款 + 解冻）
- [recharge.md](recharge.md) — 充值（原充值流程的逆向操作）
