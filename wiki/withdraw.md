# 提现交易（WithDraw）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)（4.6 提现交易接口）
- 快速参考：[docs/TRANSACTION_QUICK_REFERENCE.md](../docs/TRANSACTION_QUICK_REFERENCE.md)
- 组件结构：[architecture/TRANS_COMPONENT_STRUCTURE.md](../architecture/TRANS_COMPONENT_STRUCTURE.md)（2.6 提现交易）

## 源码映射

### 提现组件 (`flow/component/trans/withDraw/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `WithDrawTransPack` | `.../flow/component/trans/withDraw/WithDrawTransPack.java` | 提现打包：参数校验、Slot 初始化 |
| `WithDrawTrans` | `.../flow/component/trans/withDraw/WithDrawTrans.java` | 提现核心：余额校验 → 创建消费流水 → 冻结 → 前置交易 → 结果处理 |
| `WithDrawTransAfter` | `.../flow/component/trans/withDraw/WithDrawTransAfter.java` | 提现后处理：更新状态 → 账户变动 → 解冻 → 账户变动明细 |
| `WithDrawRuleCheck` | `.../flow/component/trans/withDraw/WithDrawRuleCheck.java` | 提现规则校验：银行卡配置检查等 |

### Service 层

| 类 | 路径 | 职责 |
|---|---|---|
| `TransConsumeTService` | `.../service/TransConsumeTService.java` | 消费主表 CRUD（提现复用消费表） |
| `TransConsumeSubTService` | `.../service/TransConsumeSubTService.java` | 消费子表 CRUD |
| `TransConsumeSubRecTService` | `.../service/TransConsumeSubRecTService.java` | 消费充值关联表 CRUD |
| `TransAcctFrozenChangeDetailTService` | `.../service/TransAcctFrozenChangeDetailTService.java` | 冻结变动明细 CRUD |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainWithDraw` | withDrawTransPack → accountCheck → merchantCheck → companyCheck → bussinessInfoCheck → cardBinInfoCheck → bankPayInfoCheck → accountBankPayInfoCheck → platformInfoCheck → withDrawRuleCheck → withDrawTrans → withDrawTransAfter | 提现交易 |

## 流程

```
chainWithDraw:
  withDrawTransPack (参数校验、Slot初始化)
    → 8个Check组件
    → withDrawRuleCheck (银行卡配置检查)
    → withDrawTrans (核心提现)
      ├─ 查询子账户 (baseAccountServiceApi.queryOneCardSubAccount)
      ├─ 校验可提现余额 (withdrawBalance - frozenAmt >= transAmt)
      ├─ 流水唯一性校验 (trans_consume_t 查重)
      ├─ createConsume:
      │   ├─ 查找提现银行卡 (bindId → 指定卡, 无 → 默认提现卡)
      │   ├─ 创建 trans_consume_t (transType=WD, status=P)
      │   ├─ 创建 trans_consume_sub_t
      │   └─ 创建 trans_consume_sub_rec_t (含 withdrawFee)
      ├─ handleTransFrozens (frozenType=F, transType=WD) 冻结
      ├─ 调用前置交易 frontTransConsumeFacadeApi.transWithDraw()
      ├─ 成功 → 设置 withdrawalResult 到 Slot
      └─ 失败:
          ├─ BaseASException → 解冻 (frozenType=UF)
          ├─ BaseException → 不解冻 (InnerSqlException)
          └─ Exception → 不解冻
    → withDrawTransAfter
      ├─ 更新交易状态
      ├─ 账户变动 (扣减提现金额)
      ├─ handleTransFrozens (frozenType=UF, transType=WD) 解冻
      └─ 账户变动明细 (4张表)
```

### 银行卡选择逻辑

```
1. 有 bindId → 使用指定银行卡
2. 无 bindId → 查找默认提现卡 (defaultAccountBank="1")
3. 都没有 → 报错 BANKINFO_EMPTY_ERROR / DEFAULT_BANK_INFO_EMPTY
```

### 冻结/解冻机制 (handleTransFrozens)

提现交易使用 `transType=WD` 进行冻结/解冻：

```java
request.setTransType(CommonConstants.TRANS_TYPE_WD);  // transType="WD"
request.setFrozenType(frozenType);                       // frozenType=F(冻结) / UF(解冻)
```

与转账(`transType=T`)的区别：
- 提现冻结的 `transType` 为 `WD`
- 提现只涉及一张卡（付款卡即提现卡）
- 提现无收款卡锁竞争

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_consume_t` | 消费主表（提现复用此表，transType=WD） |
| `trans_consume_sub_t` | 消费子表 |
| `trans_consume_sub_rec_t` | 消费充值关联表（含 withdrawFee 字段） |
| `trans_acct_frozen_change_detail_t` | 账户冻结变动明细表（冻结/解冻记录） |
| `trans_acct_change_detail_t` | 账户变动明细表 |
| `trans_acct_sum_change_detail_t` | 账户汇总变动明细表 |
| `trans_acct_act_sum_change_detail_t` | 账户活动汇总变动明细表 |

**注意**: 提现记录存储在 `trans_consume_t`（消费主表），通过 `trans_type=WD` 区分。`trans_consume_sub_rec_t` 中 `withdraw_fee` 字段记录提现手续费。

## 与其他交易的差异

| 维度 | 提现(WD) | 转账(T) | 消费(C) |
|---|---|---|---|
| 主表 | trans_consume_t | trans_transfer_t | trans_consume_t |
| 冻结 transType | WD | T | (消费内部冻结) |
| 涉及卡数 | 1张(提现卡) | 2张(付款+收款) | 2张(付款+收款) |
| 收款卡锁 | 不需要 | 需要 | 需要 |
| 前置交易 | frontTransConsumeFacadeApi.transWithDraw | frontTransConsumeFacadeApi.transTransfer | frontTransConsumeFacadeApi.transConsume |
| 余额校验字段 | withdrawBalance | balance | balance |
| 银行卡 | 需要提现银行卡信息 | 需要双方银行信息 | 不涉及 |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/scWithdraw` | POST | 提现交易，自动冻结 → 前置交易 → 解冻 → 账户变动 |

## 关联 Wiki
- 冻结/解冻：[frozen.md](frozen.md)（提现冻结/解冻机制）
- 转账：[transfer.md](transfer.md)（转账冻结机制类似）
- 消费交易：[consume.md](consume.md)（消费主表复用）
- 账户变动：[account-change.md](account-change.md)

---

最后更新：2026-06-06
