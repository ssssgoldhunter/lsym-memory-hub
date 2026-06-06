# 冻结/解冻交易（Frozen/UnFrozen）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)（4.7 冻结/解冻接口）
- 快速参考：[docs/TRANSACTION_QUICK_REFERENCE.md](../docs/TRANSACTION_QUICK_REFERENCE.md)
- 组件结构：[architecture/TRANS_COMPONENT_STRUCTURE.md](../architecture/TRANS_COMPONENT_STRUCTURE.md)（2.7 冻结交易、2.8 解冻交易）

## 源码映射

### 冻结组件 (`flow/component/trans/frozen/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `FrozenTransPack` | `.../flow/component/trans/frozen/FrozenTransPack.java` | 冻结打包：流水号唯一性校验、Slot 初始化、transType=FR |
| `FrozenTransBefore` | `.../flow/component/trans/frozen/FrozenTransBefore.java` | 冻结前置校验：04 子账户余额是否足够冻结 |
| `FrozenTrans` | `.../flow/component/trans/frozen/FrozenTrans.java` | 冻结核心处理：生成冻结流水 + 创建冻结变动明细 |
| `FrozenPoolHelper` | `.../flow/component/trans/frozen/FrozenPoolHelper.java` | 冻结池工具：加载原冻结记录、校验剩余金额、构建更新请求 |

### 解冻组件 (`flow/component/trans/unfrozen/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `UnFrozenTransPack` | `.../flow/component/trans/unfrozen/UnFrozenTransPack.java` | 解冻打包：流水号唯一性校验、批量解冻 Slot 初始化 |
| `UnFrozenTransBefore` | `.../flow/component/trans/unfrozen/UnFrozenTransBefore.java` | 解冻前置校验 |
| `UnFrozenTrans` | `.../flow/component/trans/unfrozen/UnFrozenTrans.java` | 解冻核心处理：批量解冻、更新原冻结记录状态 |

### Service 层

| 类 | 路径 | 职责 |
|---|---|---|
| `TransFrozenTService` | `.../service/TransFrozenTService.java` | 冻结流水表 CRUD |
| `TransAcctFrozenChangeDetailTService` | `.../service/TransAcctFrozenChangeDetailTService.java` | 冻结变动明细 CRUD |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainFrozen` | frozenTransPack → accountCheck → merchantCheck → companyCheck → bussinessInfoCheck → cardBinInfoCheck → platformInfoCheck → frozenTransBefore → frozenTrans | 冻结交易 |
| `chainUnFrozen` | unFrozenTransPack → accountCheck → merchantCheck → companyCheck → bussinessInfoCheck → cardBinInfoCheck → platformInfoCheck → unFrozenTransBefore → unFrozenTrans | 解冻交易 |

## 流程

### 冻结流程 (chainFrozen)

```
frozenTransPack
  ├─ 流水号唯一性校验 (trans_frozen_t 查重)
  ├─ 设置 frozenTransVo 到 Slot
  └─ 设置 transType = FR, payCardCodes, acctSubTypes

Checks (7个校验组件)
  → frozenTransBefore (04 子账户可冻结余额校验)
    → frozenTrans (核心冻结)
      ├─ 判断冻结标志: transAmt == balance → ALL, else → PART
      ├─ 生成冻结流水号 (frozenId)
      ├─ 创建 trans_frozen_t 记录 (frozenType=F, status=S)
      ├─ 创建 trans_acct_frozen_change_detail_t (frozenType=F)
      └─ 构造返回报文
```

### 解冻流程 (chainUnFrozen)

```
unFrozenTransPack
  ├─ 流水号唯一性校验
  └─ 设置 unFrozenTransVo 到 Slot

Checks (7个校验组件)
  → unFrozenTransBefore
    → unFrozenTrans (批量解冻)
      └─ 循环 unFrozenList:
         ├─ 刷新子账户信息
         ├─ 查找原冻结记录 (orgFrozenDetail)
         ├─ 计算已解冻金额 (freezableAmt = 原已解冻 + 本次解冻)
         ├─ 判断冻结标志 (全部/部分)
         ├─ 创建解冻流水 (frozenType=UF, status=S)
         ├─ 创建解冻变动明细 (frozenType=UF)
         ├─ 更新原冻结记录 (freezableAmt, allowFlag)
         └─ 收集成功/失败结果
```

## frozenTransNo 生成逻辑

冻结流水号 `frozenId` 生成规则：
```
frozenId = ID_3108 + DateFomatUtils(YYYYMMDDHHMMSS) + ConsumeOrderUtil.getGlobalUniqueSequenceNumber(remaining_length)
```
- 前缀: `3108` + 14位时间戳
- 后缀: 雪花算法补齐至 32 位
- 示例: `310820260603143025xxxx...` (32位)

## 冻结类型枚举

| frozen_type | 含义 | 使用场景 |
|---|---|---|
| F | 冻结 | 独立冻结交易、消费冻结、转账冻结、提现冻结 |
| UF | 释放(解冻) | 独立解冻交易、消费解冻、转账解冻、提现解冻 |
| D | 扣款消耗冻结 | 冻结模式扣款时消耗预冻结金额 |

## 冻结标志枚举

| frozen_flag | 含义 |
|---|---|
| ALL | 全额冻结/解冻 |
| PART | 部分冻结/解冻 |

## 允许标志枚举

| allow_flag | 含义 |
|---|---|
| Y | 仍允许继续解冻（有剩余冻结金额） |
| N | 不允许继续解冻（已全部解冻/消耗） |

## 冻结状态枚举

| status | 含义 |
|---|---|
| S | 成功 |

## FrozenPoolHelper 工具方法

| 方法 | 说明 |
|---|---|
| `loadAvailableOriginalFrozen(orgTransNo, cardCode, subAccountType)` | 加载可用原冻结记录（status=S, frozenType=F, allowFlag=Y） |
| `validateRemainingAmount(originalFrozen, transAmt)` | 校验剩余冻结金额是否足够 |
| `getRemainFrozenAmt(originalFrozen)` | 计算剩余冻结金额 = frozenAmt - freezableAmt |
| `getAfterUsedAmt(originalFrozen, transAmt)` | 计算使用后已消耗金额 = 当前 freezableAmt + transAmt |
| `resolveAllowFlag(originalFrozen, afterUsedAmt)` | 判断是否仍允许解冻 |
| `buildOriginalFrozenUpdateReq(originalFrozen, cardCode, afterUsedAmt)` | 构建原冻结记录更新请求（带乐观锁：expectedStatus, expectedAllowFlag, expectedFreezableAmt） |

## DeductionFrozenPoolSupport (冻结池扣款支持)

| 方法 | 说明 |
|---|---|
| `validateOldFrozenDetail(deductionTransVo, paySubCardAccount)` | 校验旧冻结记录有效性和金额匹配 |
| `consumeOldFrozenAmount(slot, paySubCardAccount, oldFrozenDetail)` | 消耗冻结池金额：新建扣款冻结记录 + 更新原冻结记录 |

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_frozen_t` | 冻结流水表：记录冻结/解冻/扣款消耗的流水 |
| `trans_acct_frozen_change_detail_t` | 账户冻结变动明细表：记录每次冻结变动的明细 |

### trans_frozen_t 关键字段

| 字段 | 说明 |
|---|---|
| frozen_id | 冻结流水号（主键，ID_3108 + 时间戳 + 序列） |
| trans_no | 交易流水号 |
| org_trans_no | 原冻结流水号（解冻/扣款消耗时指向原冻结记录） |
| batch_no | 批次号（批量解冻时使用） |
| card_code | 卡号 |
| sub_account_type | 子账户类型（04） |
| frozen_type | 冻结类型（F/UF/D） |
| frozen_amt | 冻结金额 |
| freezable_amt | 已消耗金额（冻结时为0，解冻/扣款时累加） |
| freezed_amt | 原冻结金额（解冻/扣款时记录原冻结总额） |
| balance | 当前余额 |
| frozen_flag | 冻结标志（ALL/PART） |
| allow_flag | 允许标志（Y/N） |
| status | 状态（S） |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| 冻结接口 | POST | 冻结操作，冻结指定卡号的04子账户资金 |
| 解冻接口 | POST | 解冻操作，支持批量解冻，每条解冻记录需指定 orgTransNo |

## 重要 Bug 记录

### frozen_trans_no 为 null 修复
- **问题**: 冻结交易时 `frozen_trans_no`（`trans_acct_frozen_change_detail_t.frozen_trans_no`）字段未设置，导致扣款模式（冻结模式）下无法关联原冻结记录
- **修复**: 确保 `handleTransFrozens` 方法中 `request.setFrozenTransNo()` 正确设置原冻结流水号
- **相关 commit**: `9692c6e1` — fix: 扣款业务冻结解冻流水号修复及日志补全
- **影响范围**: `FrozenTrans.handleTransFrozens`, `UnFrozenTrans.handleTransFrozens`, `DeductionTransBAfter.createFrozenChangeDetail`

## 关联 Wiki
- 消费交易：[consume.md](consume.md)（消费冻结/解冻组件）
- 扣款：[deduction.md](deduction.md)（冻结模式扣款使用冻结池）
- 转账：[transfer.md](transfer.md)（转账冻结/解冻）
- 提现：[withdraw.md](withdraw.md)（提现冻结/解冻）

---

最后更新：2026-06-06
