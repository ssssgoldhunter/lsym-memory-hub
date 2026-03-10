# MAC 并发问题修复

## 2025-03-10: Consume项目消费流程MAC刷新修复

### 问题描述
消费流程中冻结操作更新 MAC 后，`cardSubAccountMap` 中的 MAC 未刷新，导致后续操作 CAS 失败。

### 解决方案
1. 冻结解冻回退到之前版本（不刷新）
2. BaseSlot 提供 `refreshCardSubAccount()` 方法
3. 在消费流程中：冻结后、解冻前、交易后刷新账户信息

### 修改的文件
1. **FrozenTrans.java** - 回退冻结后刷新调用
2. **UnFrozenTrans.java** - 回退解冻后刷新调用
3. **ConsumeTransFrozen.java** - 冻结后刷新付款卡账户
4. **ConsumeTransUnFrozen.java** - 解冻前刷新付款卡账户
5. **ConsumeTransAfter.java** - 保留交易后刷新（已有）

### 流程说明
```
消费流程：accountCheck → consumeTransFrozen(冻结+刷新) → consumeTrans04 → consumeTransAfter(交易+刷新) → consumeTransUnFrozen(刷新+解冻)
```

---

## 2025-03-09: Task项目提现流程MAC刷新修复

### 问题描述
Task 项目中提现流程的账户更新操作存在 MAC 过期问题，导致 CAS 失败。

### 根本原因
- `ZxWithDrawUpdateStatusService` 在调用方查询账户信息后传入 AfterService
- 在调用 `batchChangeAccount` 或 `updateCardSubAccount` 时，MAC 可能已过期

### 解决方案
在 AfterService 的 `updateSubAccountBalance()` 方法中，**在调用账户更新 API 之前重新查询账户信息**，获取最新 MAC。

### 修改的文件
1. **ZxWithDrawUpdateStatusAfterService.java**
   - `updateSubAccountBalance()` 方法：在调用 `batchChangeAccount` 前重新查询账户
   - 新增日志：打印获取的最新 MAC

2. **PaWithDrawUpdateStatusAfterService.java**
   - `updateSubAccountBalance()` 方法：在调用 `updateCardSubAccount` 前重新查询账户
   - 使用 `freshSubCardAccount` 替代传入的 `subCardAccount` 参数

### 关键文件路径
- **ZX AfterService**: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/zx/ZxWithDrawUpdateStatusAfterService.java`
- **PA AfterService**: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/pa/PaWithDrawUpdateStatusAfterService.java`

---

## 2025-03-05: 账户更新并发问题修复

### 问题描述
`baseAccountServiceApi.updateCardSubAccount` 方法存在**严重的并发安全问题**，导致"丢失更新"（Lost Update）。

### 根本原因
SQL 更新语句没有并发控制：
```sql
UPDATE bas_card_sub_account_t
SET balance = #{req.balance}, ...
WHERE sub_account_id = #{req.subAccountId}
-- 缺少: AND mac = #{req.mac}  CAS 校验
```

### 解决方案

#### 1. 数据库层面 - CAS 乐观锁
使用 `mac` 字段做 CAS（Compare-And-Set）校验：
```sql
UPDATE bas_card_sub_account_t
SET mac = #{req.newMac}
WHERE sub_account_id = #{req.subAccountId}
  AND mac = #{req.mac}  -- 旧 MAC 校验
```

#### 2. 代码层面修改

**新增字段：**
- `BasCardSubAccountT.newMac` - 用于 SET 的新 MAC
- `BasCardSubAccountTReq.mac` - 用于 WHERE 的旧 MAC

**修改的文件：**
1. `BasCardSubAccountT.java` - 新增 `newMac` 字段
2. `BasCardSubAccountTServiceImpl.java` - 保存旧 MAC 到 newMac，计算新 MAC
3. `BasCardSubAccountTMapper.xml` - WHERE 条件加 `AND mac = #{req.mac}`
4. `AccountChangeBatchServiceImpl.java` - **分组合并同一账户的多次变动**

### 批量接口的并发问题

**场景：**
```java
accountChangeReqList.add(req1);  // subId=123, mac=A, balance=-100
accountChangeReqList.add(req2);  // subId=123, mac=A, balance=-50 (同一账户!)

// 第一次更新成功，mac 变成 B
// 第二次更新失败！WHERE mac=A 但数据库已是 B
```

**解决方案：按 subAccountId 分组合并**
```java
// 合并前: [{subId=123, balance=-100}, {subId=123, balance=-50}]
// 合并后: {subId=123, balance=-150}  // 一次更新成功
```

---

## 交易场景 MAC CAS 覆盖分析

### 判断原则

| 条件 | 是否需要刷新 MAC |
|------|-----------------|
| **有 Redis 锁 + 只有一次账户更新** | ❌ 不需要 |
| **有 Redis 锁 + 有多次账户更新** | ✅ 需要（每次更新前刷新） |
| **没有 Redis 锁** | ✅ 需要（每次更新前刷新） |

### 消费流程的3次账户更新

| 步骤 | 组件 | 更新内容 | MAC刷新 |
|------|------|---------|--------|
| **第1次** | consumeTransFrozen | 冻结金额更新 | ❌ 不需要（冻结时已有最新MAC） |
| **第2次** | consumeTransAfter | 账户余额更新 | ✅ 需要 |
| **第3次** | consumeTransUnFrozen | 冻结金额更新 | ✅ 需要 |

### Consume 模块场景汇总

| 场景 | 流程 | Redis 锁 | 账户更新次数 | 需要刷新 MAC | 状态 |
|------|------|---------|-------------|-------------|------|
| **消费** | chainConsume | ✅ 有 | 3次（冻结→交易→解冻） | ✅ 需要 | ✅ 已处理 |
| **消费退款** | chainConsumeRefund | ✅ 有 | 3次（04/01/02账户） | ✅ 需要 | ✅ 已处理 |
| **充值** | chainRecharge | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| **充值退款** | chainRefundRecharge | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| **转账** | chainTransfer | ✅ 有 | 2次（付款+收款） | ✅ 需要 | ✅ 已处理 |
| **内部转账** | chainTransferInner | ✅ 有 | 2次（付款+收款） | ❌ 不需要 | ✅ 正确 |
| **预内部转账** | chainTransferInnerPre | - | 0次 | - | - |
| **TI预转账** | chainTransferTiPre | - | 0次 | - | - |
| **转账授权** | chainTransferAuth | ✅ 有 | 2次（付款+收款） | ✅ 需要 | ✅ 已处理 |

### Task 模块场景汇总

| 场景 | Redis 锁 | 账户更新次数 | 需要刷新 MAC | 状态 |
|------|---------|-------------|-------------|------|
| **提现 ZX** | ✅ 有 | 1次 | ✅ 需要 | ✅ 已处理 |
| **提现 PA** | ✅ 有 | 1次 | ✅ 需要 | ✅ 已处理 |
| **充值通知 ZX** | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| **充值通知 PA** | ✅ 有 | 1次 | ❌ 不需要 | ✅ 正确 |
| **AT转账** | ✅ 有 | 2次（付款+收款） | ✅ 需要 | ✅ 已处理 |

### 已添加 MAC 刷新的组件

| 组件 | 文件路径 |
|------|---------|
| ConsumeTransAfter | `fund-catering-consume/flow/component/trans/consume/consume/ConsumeTransAfter.java` |
| ConsumeTransUnFrozen | `fund-catering-consume/flow/component/trans/consume/frozen/ConsumeTransUnFrozen.java` |
| ConsumeTransRefundAfter | `fund-catering-consume/flow/component/trans/consumeRefund/ConsumeTransRefundAfter.java` |
| ConsumeTransRefundModel1After | `fund-catering-consume/flow/component/trans/consumeRefund/ConsumeTransRefundModel1After.java` |
| TransferTransAfter | `fund-catering-consume/flow/component/trans/transfer/api/TransferTransAfter.java` |
| ZxWithDrawUpdateStatusAfterService | `fund-catering-task/service/impl/zx/ZxWithDrawUpdateStatusAfterService.java` |
| PaWithDrawUpdateStatusAfterService | `fund-catering-task/service/impl/pa/PaWithDrawUpdateStatusAfterService.java` |

---

## 关键文件路径

- **Service**: `slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/impl/BasCardSubAccountTServiceImpl.java`
- **Mapper**: `slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/mapper/BasCardSubAccountTMapper.xml`
- **Domain**: `slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/domain/BasCardSubAccountT.java`
- **批量Service**: `slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/impl/AccountChangeBatchServiceImpl.java`
- **刷新方法**: `slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/slot/BaseSlot.java` (refreshCardSubAccount)
