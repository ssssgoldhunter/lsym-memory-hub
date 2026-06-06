# 扣款交易（Deduction）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)
- 快速参考：[docs/TRANSACTION_QUICK_REFERENCE.md](../docs/TRANSACTION_QUICK_REFERENCE.md)
- 组件结构：[architecture/TRANS_COMPONENT_STRUCTURE.md](../architecture/TRANS_COMPONENT_STRUCTURE.md)
- 冻结/解冻：[frozen.md](frozen.md)（冻结池机制）

## 源码映射

### 扣款组件 (`flow/component/trans/deduction/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `DeductionTransPack` | `.../flow/component/trans/deduction/DeductionTransPack.java` | 扣款打包：参数校验、Slot 初始化 |
| `DeductionTrans` | `.../flow/component/trans/deduction/DeductionTrans.java` | 扣款核心（NodeSwitchComponent）：余额/冻结校验 → 创建消费流水 → 前置冻结(非冻结模式) → 调用前置交易 → 路由到 After |
| `DeductionTransAfter` | `.../flow/component/trans/deduction/DeductionTransAfter.java` | 非冻结模式后处理：释放冻结 → 账户变动 → 更新状态 |
| `DeductionTransBAfter` | `.../flow/component/trans/deduction/DeductionTransBAfter.java` | 冻结模式后处理：消耗冻结池金额 → 账户变动 → 更新状态 |
| `DeductionFrozenPoolSupport` | `.../flow/component/trans/deduction/DeductionFrozenPoolSupport.java` | 冻结池支持：校验旧冻结记录、消耗冻结池金额 |
| `DeductionTransPack` | `.../flow/component/trans/deduction/DeductionTransPack.java` | 扣款打包组件 |

### 批量扣款预处理 (`flow/component/trans/deduction/batch/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `DeductionBatchPreCreate` | `.../flow/component/trans/deduction/batch/DeductionBatchPreCreate.java` | 批量扣款预处理 |
| `DeductionBatchPreCreatePack` | `.../flow/component/trans/deduction/batch/DeductionBatchPreCreatePack.java` | 批量扣款预处理打包 |

### 关联组件

| 类 | 路径 | 职责 |
|---|---|---|
| `FrozenPoolHelper` | `.../flow/component/trans/frozen/FrozenPoolHelper.java` | 冻结池工具类 |
| `DeductionBatchExecuteServiceImpl` | `.../service/impl/DeductionBatchExecuteServiceImpl.java` | 批量扣款执行服务 |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainDeduction` | deductionTransPack → Checks(8) → **SWITCH**(deductionTrans).TO(deductionTransAfter, deductionTransBAfter) | 扣款交易主链 |
| `chainDeductionPre` | deductionBatchPreCreatePack → Checks(8) → deductionBatchPreCreate | 批量扣款预处理 |

## 两种扣款模式

### 模式对比

| 维度 | 非冻结模式 (`useFrozen=false`) | 冻结模式 (`useFrozen=true`) |
|---|---|---|
| 路由目标 | `deductionTransAfter` | `deductionTransBAfter` |
| 余额校验 | balance - frozenAmt >= transAmt | 校验预冻结记录 + frozenAmt >= transAmt |
| 冻结时机 | 交易前创建冻结 (`createFrozenBeforeTrade`) | 使用已有冻结记录（`DeductionFrozenPoolSupport.validateOldFrozenDetail`） |
| frozenTransNo | 使用 transNo（交易流水号） | 使用 `oldFrozenDetail.getTransNo()`（原冻结流水号） |
| 冻结释放 | `DeductionTransAfter.releaseFrozenBeforeAccountChange` (frozenType=UF) | `DeductionFrozenPoolSupport.consumeOldFrozenAmount` + `DeductionTransBAfter.createFrozenChangeDetail` (frozenType=D) |
| 冻结变动明细 | frozenType=F(冻结) → frozenType=UF(释放) | frozenType=D(扣款消耗) |
| 异步上账 | 支持（收款卡锁失败时写 entry） | 支持（收款卡锁失败时写 entry） |

### 非冻结模式流程

```
DeductionTrans (useFrozen=false):
  ├─ 校验 04 子账户可用余额 (balance - frozenAmt >= transAmt)
  ├─ createMainConsume: 创建 trans_consume_t + trans_consume_sub_rec_t
  ├─ createFrozenBeforeTrade: 创建冻结变动明细 (frozenType=F)
  ├─ createSubConsume: 创建 trans_consume_sub_t + 子 rec
  ├─ 调用前置交易 frontTransConsumeFacadeApi.transConsume()
  └─ 路由到 deductionTransAfter

DeductionTransAfter (useFrozen=false):
  ├─ front 失败 → releaseFrozenBeforeAccountChange(frozenType=UF) → status=F → return
  └─ front 成功:
     ├─ 刷新付款卡子账户
     ├─ releaseFrozenBeforeAccountChange(frozenType=UF, frozenTransNo=transNo)
     ├─ 刷新收款卡子账户
     ├─ 尝试锁收款卡:
     │   ├─ 锁成功 → updateSubAccountBalance (付款+收款 CAS 余额 + 4张变动明细)
     │   └─ 锁失败 → updatePaySubAccountBalanceOnly + asyncWriteRecEntry (待上账)
     └─ updateResult (status=S)
```

### 冻结模式流程

```
DeductionTrans (useFrozen=true):
  ├─ DeductionFrozenPoolSupport.validateOldFrozenDetail:
  │   ├─ 加载原冻结记录 (FrozenPoolHelper.loadAvailableOriginalFrozen)
  │   ├─ 校验剩余冻结金额
  │   └─ 校验账户冻结金额与冻结池记录匹配
  ├─ createMainConsume: 创建 trans_consume_t + trans_consume_sub_rec_t
  │   (不调用 createFrozenBeforeTrade，因为已有冻结)
  ├─ createSubConsume: 创建 trans_consume_sub_t + 子 rec
  ├─ 调用前置交易 frontTransConsumeFacadeApi.transConsume()
  └─ 路由到 deductionTransBAfter

DeductionTransBAfter (useFrozen=true):
  ├─ front 失败 → updateResult (status=F) → return (不解冻)
  └─ front 成功:
     ├─ 刷新付款卡+收款卡子账户
     ├─ DeductionFrozenPoolSupport.consumeOldFrozenAmount:
     │   ├─ 创建扣款冻结记录 (trans_frozen_t, frozenType=D, frozenFlag=ALL/PART)
     │   └─ 更新原冻结记录 (freezableAmt 累加, allowFlag 判断)
     ├─ 尝试锁收款卡:
     │   ├─ 锁成功 → updateSubAccountBalance + createFrozenChangeDetail
     │   └─ 锁失败 → updatePaySubAccountBalanceOnly + createFrozenChangeDetail + asyncWriteRecEntry
     └─ updateResult (status=S)
```

### frozenTransNo 设置差异

| 模式 | frozenTransNo 来源 | 说明 |
|---|---|---|
| 非冻结模式 | `deductionTransVo.getTransNo()` | 当前交易流水号 |
| 冻结模式 | `oldFrozenDetail.getTransNo()` | 原冻结记录的流水号 |

在 `DeductionTransBAfter.createFrozenChangeDetail` 中：
```java
request.setFrozenTransNo(oldFrozenDetail.getTransNo());  // 冻结模式使用原冻结流水号
request.setFrozenType(CommonConstants.FROZEN_TYPE_D);     // frozenType=D (扣款消耗)
```

## 流程图

```
chainDeduction:
  deductionTransPack → Checks(8)
    → SWITCH(deductionTrans)
      ├─ useFrozen=false → deductionTransAfter
      │   (冻结→交易→释放冻结→账户变动)
      └─ useFrozen=true  → deductionTransBAfter
          (校验预冻结→交易→消耗冻结池→账户变动)
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_consume_t` | 消费主表（复用，记录扣款交易） |
| `trans_consume_sub_t` | 消费子表 |
| `trans_consume_sub_rec_t` | 消费充值关联表 |
| `trans_frozen_t` | 冻结流水表（冻结模式记录 frozenType=D） |
| `trans_acct_frozen_change_detail_t` | 账户冻结变动明细表 |
| `trans_acct_change_detail_t` | 账户变动明细表 |
| `trans_acct_sum_change_detail_t` | 账户汇总变动明细表 |
| `trans_acct_act_sum_change_detail_t` | 账户活动汇总变动明细表 |
| `trans_acct_change_entry_detail_t` | 账户变动明细待上账表（异步上账场景） |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| 扣款接口 | POST | 扣款交易，通过 useFrozen 参数区分冻结/非冻结模式 |
| 批量扣款预处理 | POST | 批量扣款预处理 |

## 关联 Wiki
- 冻结/解冻：[frozen.md](frozen.md)（冻结池机制）
- 消费交易：[consume.md](consume.md)（消费流水复用）
- 账户变动：[account-change.md](account-change.md)

---

最后更新：2026-06-06
