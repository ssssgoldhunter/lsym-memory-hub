# 账户变动（Account Change）

## 关联文档
- 主题页：[topics/account-change.md](../topics/account-change.md)
- 源码映射：[docs/ACCOUNT_CHANGE_SOURCE_MAP.md](../docs/ACCOUNT_CHANGE_SOURCE_MAP.md)
- 技术决策 — MAC 并发修复：[technical-decisions/MAC_CONCURRENCY_FIX.md](../technical-decisions/MAC_CONCURRENCY_FIX.md)
- 冻结重构分支：`lsym_20260116_limeng_restruct`

## 源码映射

### base 模块（核心）

| 类 | 路径 | 职责 |
|---|---|---|
| `AccountServiceImpl` | `fund-catering-base/.../service/impl/AccountServiceImpl.java` | 账户开户/查询/更新基础服务，持有 Redis 锁和 MAC 校验工具 |
| `BasCardSubAccountTServiceImpl` | `fund-catering-base/.../service/impl/BasCardSubAccountTServiceImpl.java` | 子账户余额更新核心：`updateCardSubAccount`，内含 MAC CAS 校验逻辑 |
| `AccountChangeBatchService` | `fund-catering-base/.../service/AccountChangeBatchService.java` | 批量账户变动接口定义 |
| `AccountChangeBatchServiceImpl` | `fund-catering-base/.../service/impl/AccountChangeBatchServiceImpl.java` | 批量账户变动实现，按 `subAccountId` 分组合并以避免 CAS 失败 |
| `MacCheckUtils` | `fund-catering-base/.../utils/MacCheckUtils.java` | MAC 生成与校验工具 |
| `RedisLockUtils` | `fund-catering-base/.../utils/RedisLockUtils.java` | Redis 分布式锁工具 |

### consume 模块（交易 After 组件调用入口）

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransAfter` | `fund-catering-consume/.../flow/component/trans/consume/consume/ConsumeTransAfter.java` | 消费 → `batchChangeAccountForConsume` |
| `RechargeTransAfter` | `fund-catering-consume/.../flow/component/trans/recharge/RechargeTransAfter.java` | 充值 → `batchChangeAccountForRecharge` |
| `RefundRechargeTransAfter` | `fund-catering-consume/.../flow/component/trans/refundRecharge/RefundRechargeTransAfter.java` | 充值退款 → `batchChangeAccountForRefundRecharge` |
| `ConsumeTransRefundAfter` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundAfter.java` | 消费退款 → `batchChangeAccountForRefundConsume` |
| `ConsumeTransRefundModel1After` | `fund-catering-consume/.../flow/component/trans/consumeRefund/ConsumeTransRefundModel1After.java` | 消费退款 Model1 → `batchChangeAccountForRefundConsume` |
| `TransferTransAfter` | `fund-catering-consume/.../flow/component/trans/transfer/api/TransferTransAfter.java` | 转账 → `batchChangeAccount`（通用） |
| `WithDrawTransAfter` | `fund-catering-consume/.../flow/component/trans/withDraw/WithDrawTransAfter.java` | 提现 → `createFrozenDetail` |
| `BaseSlot` | `fund-catering-consume/.../flow/slot/BaseSlot.java` | LiteFlow Slot 基类，提供 `refreshCardSubAccount` 刷新 MAC |

### task 模块（后处理）

| 类 | 路径 | 职责 |
|---|---|---|
| `AccountEntryAfterService` | `fund-catering-task/.../service/impl/AccountEntryAfterService.java` | 异步上账，仍使用两段式（先更新余额再写明细） |
| `ZxWithDrawUpdateStatusAfterService` | `fund-catering-task/.../service/impl/zx/ZxWithDrawUpdateStatusAfterService.java` | 中信提现完成 → `batchChangeAccount` |
| `PaWithDrawUpdateStatusAfterService` | `fund-catering-task/.../service/impl/pa/PaWithDrawUpdateStatusAfterService.java` | 平安提现完成 → `updateCardSubAccount`（旧路径） |

## 核心机制 — MAC + CAS 乐观锁

### 子账户字段

`bas_card_sub_account_t` 表中参与余额更新的关键字段：

| 字段 | 说明 |
|---|---|
| `balance` | 账户余额 |
| `real_balance` | 真实余额 |
| `withdraw_balance` | 可提现余额 |
| `wait_release_balance` | 待释放余额 |
| `frozen_amt` | 冻结金额 |
| `mac` | MAC 校验值（用于 WHERE 条件 CAS 比较） |
| `new_mac` | 新 MAC 值（用于 SET 更新） |

### MAC CAS 流程

```
1. 查询当前子账户 → 获取旧 MAC
2. 计算新余额（原值 + 变动值）
3. 重新计算 MAC → 存入 newMac
4. SQL: UPDATE bas_card_sub_account_t SET mac = #{newMac}, balance = ... WHERE sub_account_id = #{id} AND mac = #{oldMac}
5. 影响行数 = 0 → CAS 失败，抛异常
```

### 并发保护 — Redis 分布式锁

- 锁 Key：以 `cardCode` 为维度
- 锁超时：30 秒（按场景可配置更长）
- 获取策略：部分场景支持重试（如回溯 `tryLockWithRetry(cardCode, 30, 2)`）

### 批量接口按 subAccountId 分组合并

```java
// 同一 subAccountId 的多次变动合并为一次，避免第二次 CAS 失败
// 合并前: [{subId=123, balance=+100}, {subId=123, balance=+50}]
// 合并后: {subId=123, balance=+150}
```

### MAC 刷新判断规则

| 条件 | 是否需要刷新 MAC |
|---|---|
| 有 Redis 锁 + 只有一次账户更新 | 不需要 |
| 有 Redis 锁 + 有多次账户更新 | 需要（每次更新前刷新） |
| 没有 Redis 锁 | 需要（每次更新前刷新） |

## 批量账户变动接口（AccountChangeBatchService）

| 方法 | 适用场景 | 膨胀金处理 |
|---|---|---|
| `batchChangeAccountForConsume` | 消费 | 扣减/创建膨胀金 |
| `batchChangeAccountForRecharge` | 充值 | 创建膨胀金，返回 `acctExpandId` |
| `batchChangeAccountForRefundRecharge` | 充值退款 | 扣减膨胀金 |
| `batchChangeAccountForRefundConsume` | 消费退款 | 恢复膨胀金 |
| `batchChangeAccount` | 通用（转账/提现等） | 不涉及膨胀金 |

## 数据库表

### 账户变动明细表（4 张）

| 表名 | 子账户类型 | 说明 |
|---|---|---|
| `trans_acct_change_detail_t` | 01 | 现金账户变动明细 |
| `trans_acct_change_detail_02_t` | 02 | 膨胀金赠送账户变动明细 |
| `trans_acct_change_detail_04_t` | 04 | 银行子账户变动明细 |
| `trans_acct_frozen_change_detail_t` | frozen | 冻结/解冻变动明细 |

### 子账户表

| 表名 | 说明 |
|---|---|
| `bas_card_sub_account_t` | 子账户主表，MAC 字段用于 CAS |
| `bas_sub_account_expend_t` | 膨胀金账户扩展表 |

## 余额链计算

排序：`txnTime ASC → transNo ASC`

```
初始: balance=B0, realBalance=R0

第1笔 A(+100分):
  orgAmt=B0, balance=B0+100
  orgRealAmt=R0, realBalance=R0+100

第2笔 B(+200分):
  orgAmt=B0+100, balance=B0+300
  orgRealAmt=R0+100, realBalance=R0+300
```

## 更新模式总结

### consume 主交易路径（已改造为批量接口）

各交易 After 组件调用对应的 `batchChangeAccountForXxx` 方法，余额更新和膨胀金操作在同一事务内完成。

### task 后处理路径（混合状态）

| 场景 | 当前路径 | 优先级 |
|---|---|---|
| `ZxWithDrawUpdateStatusAfterService` | `batchChangeAccount` | 中（两段式，已切到批量接口） |
| `AccountEntryAfterService` | `batchChangeAccount` | 中（两段式） |
| `PaWithDrawUpdateStatusAfterService` | `updateCardSubAccount` | 高（旧路径） |
| `TransferRecallServiceImpl` | `updateCardSubAccount` | 高（旧路径） |
| `TransRecallServiceImpl` | `createFrozenDetail` | 特殊（回溯补偿逻辑） |

## 排查清单

1. 是否同一张卡/同一子账户被多次更新
2. 更新前是否调用 `refreshCardSubAccount(...)` 或等价刷新
3. 更新 SQL 是否走到 `and mac = #{req.mac}`
4. 是否有同一 `subAccountId` 的多次变动需要合并
5. 是否因为 task/回溯旧路径直接更新导致 MAC 过期

## 关联 Wiki

- [recharge.md](recharge.md) — 充值流程（调用 `batchChangeAccountForRecharge`）
- [consume-refund.md](consume-refund.md) — 消费退款流程（调用 `batchChangeAccountForRefundConsume`）
- [recall.md](recall.md) — 回溯流程（冻结明细回退）
- [platform-recharge-batch.md](platform-recharge-batch.md) — 平台批量实收（同一余额链计算口径）
