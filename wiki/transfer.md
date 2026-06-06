# 转账交易（Transfer）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)（4.5 转账交易接口）
- 快速参考：[docs/TRANSACTION_QUICK_REFERENCE.md](../docs/TRANSACTION_QUICK_REFERENCE.md)
- 组件结构：[architecture/TRANS_COMPONENT_STRUCTURE.md](../architecture/TRANS_COMPONENT_STRUCTURE.md)（2.5 转账交易）

## 源码映射

### API 转账组件 (`flow/component/trans/transfer/api/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `TransferTransPack` | `.../flow/component/trans/transfer/api/TransferTransPack.java` | 转账打包：参数校验、Slot 初始化 |
| `TransferTrans` | `.../flow/component/trans/transfer/api/TransferTrans.java` | 转账核心（NodeSwitchComponent）：余额校验 → 创建转账流水 → 冻结 → 前置交易 → 路由 |
| `TransferTransAfter` | `.../flow/component/trans/transfer/api/TransferTransAfter.java` | 转账后处理：更新状态 → 账户变动 → 解冻 → 账户变动明细(4张表) |

### 授权转账组件 (`flow/component/trans/transfer/auth/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `TransferTransAuthPack` | `.../flow/component/trans/transfer/auth/TransferTransAuthPack.java` | 授权转账打包 |
| `TransferTransAuth` | `.../flow/component/trans/transfer/auth/TransferTransAuth.java` | 授权转账处理 |

### 内部转账组件 (`flow/component/trans/transfer/manage/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `TransferTransInnerPack` | `.../flow/component/trans/transfer/manage/TransferTransInnerPack.java` | 内部转账打包 |
| `TransferTransInner` | `.../flow/component/trans/transfer/manage/TransferTransInner.java` | 内部转账处理 |
| `TransferTransInnerAfter` | `.../flow/component/trans/transfer/manage/TransferTransInnerAfter.java` | 内部转账后处理 |
| `TransferTransInnerPreAfter` | `.../flow/component/trans/transfer/manage/TransferTransInnerPreAfter.java` | 内部转账预处理后处理 |

### TI 转账组件 (`flow/component/trans/transfer/ti/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `TransferTransTiPack` | `.../flow/component/trans/transfer/ti/TransferTransTiPack.java` | TI 转账打包 |
| `TransferTransTi` | `.../flow/component/trans/transfer/ti/TransferTransTi.java` | TI 转账处理：只创建划付记录，不涉及冻结解冻 |
| `TransferTransTiPreAfter` | `.../flow/component/trans/transfer/ti/TransferTransTiPreAfter.java` | TI 转账预处理后处理 |

### 转账验证组件 (`flow/component/trans/transfer/verfily/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `TransferTransSendVerify` | `.../flow/component/trans/transfer/verfily/TransferTransSendVerify.java` | 发送转账验证码 |
| `TransferTransReSendVerifyPack` | `.../flow/component/trans/transfer/verfily/TransferTransReSendVerifyPack.java` | 重新发送验证码打包 |

### Service 层

| 类 | 路径 | 职责 |
|---|---|---|
| `TransTransferTService` | `.../service/TransTransferTService.java` | 转账主表 CRUD |
| `TransTransferSubTService` | `.../service/TransTransferSubTService.java` | 转账子表 CRUD |
| `TransAcctFrozenChangeDetailTService` | `.../service/TransAcctFrozenChangeDetailTService.java` | 冻结变动明细 CRUD |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainTransfer` | transferTransPack → Checks(7) → **SWITCH**(transferTrans).TO(transferTransAfter, transferTransSendVerify) | 普通转账(T) |
| `chainTransferInner` | transferTransInnerPack → Checks(6) → transferTransInner → transferTransInnerAfter | 内部转账 |
| `chainTransferInnerPre` | transferTransInnerPack → Checks(5) → transferTransInner → transferTransInnerPreAfter | 内部转账预处理 |
| `chainTransferTiPre` | transferTransTiPack → Checks(5) → transferTransTi → transferTransTiPreAfter | TI 划付预处理 |
| `chainTransferAuth` | transferTransAuthPack → Checks(8) → transferTransAuth → transferTransAfter | 授权转账(AT) |
| `chainTransferReSendVerification` | transferTransReSendVerifyPack → Checks(6) → transferTransSendVerify | 转账重发验证码 |

## 转账类型

| transType | 名称 | 说明 | 冻结 | 前置交易 |
|---|---|---|---|---|
| T | 普通转账 | 标准转账，冻结 → 前置交易 → 解冻 → 账户变动 | 是(F/UF) | 是 |
| TI | 划付上账 | 批量转账预处理，**只创建划付记录，不涉及冻结解冻** | 否 | 否 |
| AT | 授权转账 | 需授权转账，走授权链路后复用 transferTransAfter | 是(F/UF) | 是 |

## 流程

### 普通转账流程 (chainTransfer, transType=T)

```
transferTransPack → Checks(7)
  → SWITCH(transferTrans):
    ├─ 无需验证码 (verifityType 为空):
    │   ├─ 余额校验 (balance - frozenAmt >= transAmt)
    │   ├─ 流水唯一性校验
    │   ├─ createTransfer: 创建 trans_transfer_t + trans_transfer_sub_t
    │   ├─ handleTransFrozens (frozenType=F, transType=T)
    │   ├─ 调用前置交易 frontTransConsumeFacadeApi.transTransfer()
    │   ├─ 失败:
    │   │   ├─ updateFailureResult (status=F)
    │   │   └─ handleTransFrozens (frozenType=UF) 解冻
    │   └─ 成功 → transferTransAfter
    │       ├─ updateResult (status=S/F)
    │       ├─ updateSubAccountBalance (付款-收款 CAS, 4张变动明细)
    │       └─ handleTransFrozens (frozenType=UF) 解冻
    └─ 需要验证码 (verifityType 不为空):
        └─ transferTransSendVerify (发送验证码)
```

### TI 划付流程 (chainTransferTiPre, transType=TI)

```
transferTransTiPack → Checks(5)
  → transferTransTi:
    ├─ 余额校验 (balance - frozenAmt >= transAmt)
    ├─ 流水唯一性校验
    ├─ createTransfer: 创建 trans_transfer_t + trans_transfer_sub_t
    │   (只创建划付记录，不涉及冻结解冻，不调用前置交易)
    └─ 设置 Slot 信息
  → transferTransTiPreAfter
```

**重要**: TI 划付不涉及冻结解冻，只创建划付记录。后续由批量任务处理实际转账。

### 转账冻结/解冻 (handleTransFrozens)

在 `TransferTrans` 和 `TransferTransAfter` 中，冻结/解冻使用：
```java
request.setTransType(CommonConstants.TRANS_TYPE_T);  // transType="T"
request.setFrozenType(frozenType);                     // frozenType=F(冻结) / UF(解冻)
```

## 并发控制设计

| 层级 | 锁对象 | 超时时间 | 作用 |
|---|---|---|---|
| 批次级 | batch_no | 10分钟 | 防止重复处理 |
| 付款卡级 | pay_card_code | 30分钟 | 保证卡顺序处理 |
| 收款卡级 | receive_card_code | 10分钟 | 防止并发冲突 |

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_transfer_t` | 转账主表（唯一约束 trans_no） |
| `trans_transfer_sub_t` | 转账子表（按子账户类型拆分） |
| `trans_acct_frozen_change_detail_t` | 账户冻结变动明细表（冻结/解冻记录） |
| `trans_acct_change_detail_t` | 账户变动明细表（转出方和转入方各一条） |
| `trans_acct_sum_change_detail_t` | 账户汇总变动明细表 |
| `trans_acct_act_sum_change_detail_t` | 账户活动汇总变动明细表 |

### trans_transfer_t 关键字段

| 字段 | 说明 |
|---|---|
| transfer_id | 转账流水号（主键，ID_3108 + 时间戳 + 序列） |
| trans_no | 交易流水号 |
| trans_type | 交易类型（T/TI/AT） |
| trans_bus_type | 业务类型 |
| transfers_type | 转账类型 |
| pay_card_code | 付款卡号 |
| receive_card_code | 收款卡号 |
| trans_amt | 转账金额 |
| status | 状态（P/S/F/V） |
| mac | MAC 值（幂等性校验） |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/scTransfer` | POST | 普通转账，三层锁机制，支持冻结 |
| 内部转账 | POST | 内部转账 |
| TI 预处理 | POST | TI 划付预处理（不涉及冻结） |
| 授权转账 | POST | 授权转账 |
| 重发验证码 | POST | 转账重发验证码 |

## 关联 Wiki
- 冻结/解冻：[frozen.md](frozen.md)（转账冻结/解冻机制）
- 扣款：[deduction.md](deduction.md)（扣款复用冻结池）
- 提现：[withdraw.md](withdraw.md)（提现冻结机制类似）
- 账户变动：[account-change.md](account-change.md)

---

最后更新：2026-06-06
