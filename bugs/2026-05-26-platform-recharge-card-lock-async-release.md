# 平台批量实收卡锁跨线程释放待处理

更新时间：2026-05-26

状态：待处理

## 现象

平台 03 渠道充值批量实收上线后出现：

- 第一批 `2881` 条实际处理成功。
- `trans_platform_recharge_batch_detail` 全部成功。
- 充值表已落库。
- 平台账户金额已变动。
- 日志打印了 `释放卡锁成功`。
- 第二批剩余 `1854` 条提交到 consume 后返回 `SKIPPED_LOCKED`，未写入批量明细表。

示例日志：

```text
平台充值批量上账：异步处理汇总 batchNo=PRB20260526060623000002, 总明细=2881, 成功=2881, 失败=0, 跳过=0
平台充值批量上账：释放卡锁成功 batchNo=PRB20260526060623000002, lockKey=platform_recharge:card_lock:0000005001000000011
平台充值批量上账：卡锁被占用，跳过本次处理 batchNo=PRB20260526062220000002, lockKey=platform_recharge:card_lock:0000005001000000011
```

## 根因判断

当前 `PlatformRechargeBatchServiceImpl.submit()` 在 HTTP 线程中抢平台卡锁，但实际批量上账在异步线程中执行并释放锁。

`RedisLockUtils.unlockKey()` 最终调用 `RedissonDistributedLocker.unlock()`：

```java
if (lock.isLocked() && lock.isHeldByCurrentThread()) {
    lock.unlock();
}
```

Redisson 锁与持有线程绑定。HTTP 线程加锁、异步线程释放时，`isHeldByCurrentThread()` 为 `false`，实际不会 unlock。

同时 `RedisLockUtils.unlockKey()` 固定返回 `true`，导致业务日志打印“释放卡锁成功”，但 Redis 锁实际可能仍在，只能等 TTL 自动过期。

## 当前临时处理

- 如果第一批业务数据已经成功落库且账户金额已变动，可等待 `platform_recharge:card_lock:{platformCardCode}` TTL 自动过期后重跑任务。
- 已成功流水会被 Redis done 或充值表 `trans_no` 去重过滤，不会重复上账。
- 若 Redis TTL 为 `-1`，必须确认没有异步任务仍在执行后，才能人工删除卡锁。

## 推荐修复

优先方案：

- 平台卡锁不要使用线程绑定的 Redisson `RLock` 跨线程释放。
- 改为 Redis token 锁：
  - 加锁：`SET key token NX EX ttl`
  - 释放：Lua 校验 token 后删除
  - token 随异步任务上下文传递

备选方案：

- 在该异步场景使用 `forceUnlock`，但必须严格保证不会误删其他批次新抢到的锁。
- 或调整流程为异步线程内抢锁和释放锁，避免跨线程持有。

## 后续验证

修复后验证：

1. 第一批完成后 Redis 卡锁立即消失。
2. 日志中的“释放卡锁成功”必须代表实际删除成功。
3. 连续触发第二批时，如果第一批已完成，第二批能正常进入 consume。
4. 如果第一批未完成，第二批仍返回 `SKIPPED_LOCKED`。
