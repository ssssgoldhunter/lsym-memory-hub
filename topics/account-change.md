# Account Change

> Status: current, verified-against-source
> Last updated: 2026-05-12

This page is the current entry point for 账户变动、MAC/CAS and consistency checks in `lsym`.

## Current Rules

- 子账户余额更新依赖 `MAC/CAS`，多次更新同一账户前要关注账户刷新。
- 交易主流程优先看 `BaseAccountServiceApi` 的分场景批量账户变动接口。
- `consume` 主交易路径已经大量接入分场景批量接口。
- `task` 模块仍可能混合存在 `batchChangeAccount`、`updateCardSubAccount` 和手工明细写入。
- 排查账户不平时，先确认余额更新和明细写入是否在同一事务边界内。

## Main Account APIs

| 方法 | 主要场景 |
|------|----------|
| `batchChangeAccountForConsume` | 消费 |
| `batchChangeAccountForRecharge` | 充值 |
| `batchChangeAccountForRefundRecharge` | 充值退款 |
| `batchChangeAccountForRefundConsume` | 消费退款 |
| `batchChangeAccount` | 转账、提现等通用账户变动 |
| `updateCardSubAccount` | 旧路径，排查时默认重点关注 |

## Triage Checklist

1. 是否同一张卡/同一子账户被多次更新。
2. 更新前是否调用 `refreshCardSubAccount(...)` 或等价刷新。
3. 更新 SQL 是否走到 `and mac = #{req.mac}`。
4. 是否有同一 `subAccountId` 的多次变动需要合并。
5. 是否因为 task/回溯旧路径直接更新导致 MAC 过期。

## Source Pointers

- Account source map:
  - `../docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
- Base account batch service:
  - `../slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/AccountChangeBatchService.java`
- Consume base slot refresh:
  - `../slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/BaseSlot.java`

## Related Docs

- `../docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
- `../technical-decisions/MAC_CONCURRENCY_FIX.md`
- `../docs/plans/2026-03-10-mac-cas-design.md`
- `../docs/plans/2026-03-17-task-account-change-unification-plan.md`
- `../workflow/PROJECT_MEMORY.md`
