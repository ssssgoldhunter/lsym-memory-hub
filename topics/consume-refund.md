# Consume Refund

> Status: current, verified-against-source
> Last updated: 2026-05-12

This page is the current entry point for 消费退款 logic in `lsym`.

## Current Business Rules

- 消费退款支持比例退款和订单/逐笔退款两个模式。
- 退款请求以用户传入的 `subTransList.receiveAmount` 为准，不按整张原订单重新计算多收款卡退款比例。
- 同一 `receiveCardCode` 多条退款明细需要先合并；业务规则是同一收款卡唯一绑定商户，不会出现同卡多个 `receiveMerchantId`。
- 合并后的 `subTransList` 是退款主记录落库口径。
- 每张 `receiveCardCode` 独立校验本卡可退余额，分母是当前收款卡下 `01 + 02 + 04` 的原消费金额合计。
- `01`、`02`、`04` 都支持退款；`04` 既支持原路退回，也支持按配置转 `01`。
- 极值金额下必须依赖兜底补充分配，兜底后的金额必须回写最终 `cancel01Amt`、`cancel02Amt`、`cancel04Amt`。

## Split Modes

| 模式 | 组件 | 分摊口径 | 兜底 |
|------|------|----------|------|
| 比例退款 | `ConsumeTransRefundSplitPercentPack` | 账户类型层、科目层、流水层都按比例计算 | 账户类型层、科目层、流水层都需要补充分配 |
| 订单/逐笔退款 | `ConsumeTransRefundSplitOrderPack` | 账户类型层、科目层按比例；流水层逐笔贪心 | 账户类型层、科目层有兜底，流水层按可退余额逐笔消耗 |

## Three-Level Allocation

```text
用户传入 subTransList
  -> 按 receiveCardCode 合并 receiveAmount
  -> 账户类型层：当前收款卡内 01/02/04 原消费金额占比分摊
  -> 科目层：01/02 按 activityType 分摊，04 跳过
  -> 流水层：比例模式按比例+补充分配；逐笔模式按可退余额贪心分配
  -> 兜底结果回写 cancelXXAmt 并参与子单落地
```

## Key Fix Memory

- 2026-05-12 修复过“极值操作没有兜底导致比例退款分摊失败”的问题。
- 触发兜底时日志需要带 `trans_no`，方便从交易流水定位。
- 如果只更新临时 `accountRefundAmtMap`，但没有重新回写 `cancelXXAmt`，最终退款子单金额仍会落旧值。

## Source Pointers

- Split percent:
  - `../slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefundSplitPercentPack.java`
- Split order:
  - `../slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefundSplitOrderPack.java`
- Refund after:
  - `../slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefundAfter.java`
- 04 refund after:
  - `../slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefund04After.java`

## Related Docs

- `../docs/TRANSACTION_QUICK_REFERENCE.md`
- `../docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
- `../workflow/PROJECT_MEMORY.md`
