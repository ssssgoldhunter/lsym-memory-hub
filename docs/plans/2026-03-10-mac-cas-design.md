# MAC CAS 并发保护设计方案

**日期**: 2026-03-10
**状态**: 已实施完成

## 背景

账户更新操作存在并发安全问题，可能导致"丢失更新"（Lost Update）。需要为账户更新添加原子一致性保护。

## 问题分析

### 原有问题
- 账户更新 SQL 缺少并发控制
- 同一账户并发更新时，后更新的会覆盖先更新的数据

### 解决方案
使用 **MAC 字段作为 CAS（Compare-And-Set）乐观锁**：
- 每次更新时，MAC 作为 WHERE 条件
- 更新同时生成新的 MAC（newMac）
- 保证每次更新都是基于最新的 MAC

```sql
-- 原有 SQL（无并发控制）
UPDATE bas_card_sub_account_t
SET balance = #{req.balance}
WHERE sub_account_id = #{req.subAccountId}

-- 修改后（CAS 校验）
UPDATE bas_card_sub_account_t
SET balance = #{req.balance}, mac = #{req.newMac}
WHERE sub_account_id = #{req.subAccountId}
  AND mac = #{req.mac}
```

## 场景分析

### 判断原则

| 条件 | 是否需要刷新 MAC |
|------|-----------------|
| 没有 Redis 锁 | ✅ 需要 |
| 有 Redis 锁 + 多次账户更新 | ✅ 需要 |
| 有 Redis 锁 + 只有一次账户更新 | ❌ 不需要 |

### 场景汇总

#### Consume 模块

| 场景 | Redis 锁 | 账户更新次数 | 需要刷新 MAC | 状态 |
|------|---------|-------------|-------------|------|
| 消费 | ✅ 有 | 3次 | ✅ 需要 | ✅ 已处理 |
| 消费退款 | ✅ 有 | 3次 | ✅ 需要 | ✅ 已处理 |
| 充值 | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| 充值退款 | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| 转账 | ✅ 有 | 2次 | ✅ 需要 | ✅ 已处理 |
| 内部转账 | ✅ 有 | 2次 | ❌ 不需要 | ✅ 正确 |
| 转账授权 | ✅ 有 | 2次 | ✅ 需要 | ✅ 已处理 |

#### Task 模块

| 场景 | Redis 锁 | 账户更新次数 | 需要刷新 MAC | 状态 |
|------|---------|-------------|-------------|------|
| 提现 ZX | ✅ 有 | 1次 | ✅ 需要 | ✅ 已处理 |
| 提现 PA | ✅ 有 | 1次 | ✅ 需要 | ✅ 已处理 |
| AT转账 | ✅ 有 | 2次 | ✅ 需要 | ✅ 已处理 |

## 实施内容

### 1. 数据库修改

- `BasCardSubAccountT` 新增 `newMac` 字段
- `BasCardSubAccountTReq` 新增 `mac` 字段
- `BasCardSubAccountTMapper.xml` WHERE 条件加入 `AND mac = #{req.mac}`

### 2. 代码修改

- `BasCardSubAccountTServiceImpl` - 计算新 MAC
- `AccountChangeBatchServiceImpl` - 分组合并同一账户的多次变动

### 3. MAC 刷新

在以下场景添加 `refreshCardSubAccount()` 调用：

| 组件 | 文件 |
|------|------|
| ConsumeTransAfter | 消费交易后 |
| ConsumeTransUnFrozen | 解冻前 |
| ConsumeTransRefundAfter | 退款后 |
| TransferTransAfter | 转账后 |
| ZxWithDrawUpdateStatusAfterService | 提现后 |
| PaWithDrawUpdateStatusAfterService | 提现后 |

## 结论

**所有交易场景已正确覆盖，MAC CAS 并发保护实施完成。**

## 参考文档

- technical-decisions/MAC_CONCURRENCY_FIX.md
- skills/LITEFLOW_SKILLS.md
