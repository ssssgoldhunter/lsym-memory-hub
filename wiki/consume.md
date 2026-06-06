# 消费交易（Consume）

## 关联文档
- 设计文档：[docs/SUPPLY_CHAIN_DESIGN_V5.5.md](../docs/SUPPLY_CHAIN_DESIGN_V5.5.md)
- 快速参考：[docs/TRANSACTION_QUICK_REFERENCE.md](../docs/TRANSACTION_QUICK_REFERENCE.md)
- 流程图：[business-flows/CONSUME_FLOW_DIAGRAMS.md](../business-flows/CONSUME_FLOW_DIAGRAMS.md)
- 组件结构：[architecture/TRANS_COMPONENT_STRUCTURE.md](../architecture/TRANS_COMPONENT_STRUCTURE.md)

## 源码映射

### 核心消费组件 (`flow/component/trans/consume/consume/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransPack` | `.../flow/component/trans/consume/consume/ConsumeTransPack.java` | 消费交易打包：参数校验、Slot 初始化 |
| `ConsumeTransDeductionSplitPack` | `.../flow/component/trans/consume/consume/ConsumeTransDeductionSplitPack.java` | 消费扣款拆分打包：多收款卡场景拆分 |
| `ConsumeTransSubTypeRoute` | `.../flow/component/trans/consume/consume/ConsumeTransSubTypeRoute.java` | 消费子类型路由：直接消费/需鉴权消费路由 |
| `ConsumeTrans01` | `.../flow/component/trans/consume/consume/ConsumeTrans01.java` | 01 现金子账户消费交易：创建消费子表和充值关联表 |
| `ConsumeTrans02` | `.../flow/component/trans/consume/consume/ConsumeTrans02.java` | 02 膨胀金子账户消费交易 |
| `ConsumeTrans04` | `.../flow/component/trans/consume/consume/ConsumeTrans04.java` | 04 综合子账户消费交易 |
| `ConsumeTrans01Cal` | `.../flow/component/trans/consume/consume/ConsumeTrans01Cal.java` | 01 账户算价：现金账户消费金额计算 |
| `ConsumeTrans02Cal` | `.../flow/component/trans/consume/consume/ConsumeTrans02Cal.java` | 02 账户算价：膨胀金账户消费金额计算 |
| `ConsumeTransAfter` | `.../flow/component/trans/consume/consume/ConsumeTransAfter.java` | 消费后处理：账户变动明细(4张表)、余额更新 |
| `ConsumeTrans04FirstError` | `.../flow/component/trans/consume/consume/ConsumeTrans04FirstError.java` | 04 交易首次失败处理 |
| `ConsumeTrans04FirstErrorAfter` | `.../flow/component/trans/consume/consume/ConsumeTrans04FirstErrorAfter.java` | 04 交易首次失败后处理 |

### 消费冻结组件 (`flow/component/trans/consume/frozen/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransFrozen` | `.../flow/component/trans/consume/frozen/ConsumeTransFrozen.java` | 消费冻结：交易前冻结资金 |
| `ConsumeTransUnFrozen` | `.../flow/component/trans/consume/frozen/ConsumeTransUnFrozen.java` | 消费解冻：交易完成后释放冻结资金 |

### 消费授权组件 (`flow/component/trans/consume/auth/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransAuthorizationPack` | `.../flow/component/trans/consume/auth/ConsumeTransAuthorizationPack.java` | 授权消费打包 |
| `ConsumeTransAuthAfter` | `.../flow/component/trans/consume/auth/ConsumeTransAuthAfter.java` | 授权消费后处理 |
| `ConsumePassWordCheckRoute` | `.../flow/component/trans/consume/auth/ConsumePassWordCheckRoute.java` | 密码校验路由：通过走消费链、失败走鉴权后处理 |

### 消费预处理组件 (`flow/component/trans/consume/pre/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransPrePack` | `.../flow/component/trans/consume/pre/ConsumeTransPrePack.java` | 预消费打包 |
| `ConsumeTransPreAfter` | `.../flow/component/trans/consume/pre/ConsumeTransPreAfter.java` | 预消费冻结后处理 |
| `ConsumeTransPreDeductionSplitPack` | `.../flow/component/trans/consume/pre/ConsumeTransPreDeductionSplitPack.java` | 预处理扣款拆分打包 |
| `ConsumeTransPreSubTypeRoute` | `.../flow/component/trans/consume/pre/ConsumeTransPreSubTypeRoute.java` | 预处理子类型路由 |
| `ConsumeTransPreFinishPack` | `.../flow/component/trans/consume/pre/ConsumeTransPreFinishPack.java` | 预消费完成打包 |
| `ConsumeTransPre04FirstError` | `.../flow/component/trans/consume/pre/ConsumeTransPre04FirstError.java` | 预处理 04 首次错误处理 |
| `ConsumeTransPre04FirstErrorAfter` | `.../flow/component/trans/consume/pre/ConsumeTransPre04FirstErrorAfter.java` | 预处理 04 首次错误后处理 |

### 消费关闭组件 (`flow/component/trans/consume/close/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransClosePack` | `.../flow/component/trans/consume/close/ConsumeTransClosePack.java` | 消费关闭打包 |
| `ConsumeTransCloseAfter` | `.../flow/component/trans/consume/close/ConsumeTransCloseAfter.java` | 消费关闭后处理 |
| `ConsumeTransCloseRoute` | `.../flow/component/trans/consume/close/ConsumeTransCloseRoute.java` | 消费关闭路由：预消费关闭(解冻)/直接关闭 |

### 消费订单计算组件 (`flow/component/trans/consume/calOrder/`)

| 类 | 路径 | 职责 |
|---|---|---|
| `ConsumeTransCalOrderPack` | `.../flow/component/trans/consume/calOrder/ConsumeTransCalOrderPack.java` | 订单计算打包 |
| `ConsumeTrans01CalOrder` | `.../flow/component/trans/consume/calOrder/ConsumeTrans01CalOrder.java` | 01 子账户订单级计算 |
| `ConsumeTrans02CalOrder` | `.../flow/component/trans/consume/calOrder/ConsumeTrans02CalOrder.java` | 02 子账户订单级计算 |
| `ConsumeTransDeductionCalOrderSplitPack` | `.../flow/component/trans/consume/calOrder/ConsumeTransDeductionCalOrderSplitPack.java` | 扣款订单计算拆分打包 |
| `ConsumeTransCalDeductionSplitPack` | `.../flow/component/trans/consume/calOrder/ConsumeTransCalDeductionSplitPack.java` | 计算扣款拆分打包 |
| `ConsumeTransCalAfter` | `.../flow/component/trans/consume/calOrder/ConsumeTransCalAfter.java` | 订单计算后处理 |

### Service 层

| 类 | 路径 | 职责 |
|---|---|---|
| `TransConsumeServiceImpl` | `.../service/impl/TransConsumeServiceImpl.java` | 消费交易核心服务：分布式锁、MAC 生成、LiteFlow 执行、幂等性检查 |
| `TransConsumeTService` | `.../service/TransConsumeTService.java` | 消费主表 CRUD |
| `TransConsumeSubTService` | `.../service/TransConsumeSubTService.java` | 消费子表 CRUD |
| `TransConsumeSubRecTService` | `.../service/TransConsumeSubRecTService.java` | 消费充值关联表 CRUD |

## LiteFlow 链路

| 链 | 组件顺序 | 说明 |
|---|---|---|
| `chainConsume` | consumeTransPack → Checks(8) → consumeTransDeductionSplitPack → baseChainConsumeSubTypeRoute | 标准消费交易 |
| `chainConsumeAuth` | consumeTransAuthorizationPack → Checks(8) → consumeTransDeductionSplitPack → baseChainConsumeSubTypeRoute | 授权消费交易 |
| `bashChainConsume` | consumeTrans02Cal → consumeTrans01Cal → consumeTransFrozen → consumeTrans04 → consumeTrans04FirstError → consumeTrans04FirstErrorAfter → consumeTrans02 → consumeTrans01 → consumeTransAfter → consumeTransUnFrozen | 消费核心子链 |
| `baseChainConsumeSubTypeRoute` | SWITCH(consumeTransSubTypeRoute).TO(bashChainConsume, baseChainConsumeSubTypeU) | 子类型路由(直接消费/需鉴权) |
| `baseChainConsumeSubTypeU` | SWITCH(consumePassWordCheckRoute).TO(bashChainConsume, consumeTransAuthAfter) | 密码校验路由 |
| `chainConsumePre` | consumeTransPrePack → Checks(6) → consumeTransPreDeductionSplitPack → baseChainConsumePreSubTypeRoute | 预消费 |
| `chainConsumePreFinish` | consumeTransPreFinishPack → Checks(8) → consumeTransPreDeductionSplitPack → baseChainConsumePreSubTypeRoute | 预消费完成 |
| `chainConsumeClose` | consumeTransClosePack → Checks(4) → SWITCH(consumeTransCloseRoute).TO(bashChainConsumePreClose, consumeTransCloseAfter) | 预消费关闭 |
| `chainConsumeCal` | consumeTransPack → Checks(8) → consumeTransCalDeductionSplitPack → consumeTrans02Cal → consumeTrans01Cal → consumeTransCalAfter | 消费算价 |
| `chainConsumeCalOrder` | consumeTransCalOrderPack → Checks(8) → consumeTransDeductionCalOrderSplitPack → bashChainConsumeCalOrder | 订单级消费算价 |

### 子账户扣款顺序
1. **02 账户**（膨胀金）→ 优先扣款（consumeTrans02Cal 先算价）
2. **01 账户**（现金）→ 02 不足时扣款（consumeTrans01Cal）
3. **04 账户**（综合）→ 按配置处理（consumeTrans04，失败走 consumeTrans04FirstError）

## 流程

```
chainConsume 主流程:
  consumeTransPack(参数校验+Slot初始化)
    → 8个Check组件(账户/商户/公司/银行/卡BIN/业务/平台)
    → consumeTransDeductionSplitPack(扣款拆分)
    → consumeTransSubTypeRoute(路由)
      ├─ 直接消费 → bashChainConsume
      └─ 需鉴权 → baseChainConsumeSubTypeU
         ├─ 密码正确 → bashChainConsume
         └─ 密码错误 → consumeTransAuthAfter(失败)

bashChainConsume 核心子链:
  consumeTrans02Cal(膨胀金算价)
  → consumeTrans01Cal(现金算价)
  → consumeTransFrozen(冻结资金)
  → consumeTrans04(04账户交易)
  → consumeTrans04FirstError(04失败处理)
  → consumeTrans02(02账户交易)
  → consumeTrans01(01账户交易)
  → consumeTransAfter(账户变动明细,4张表)
  → consumeTransUnFrozen(解冻资金)
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_consume_t` | 消费主表（唯一约束 trans_no） |
| `trans_consume_sub_t` | 消费子表（按子账户类型拆分） |
| `trans_consume_sub_rec_t` | 消费充值关联表（收款卡维度） |
| `trans_acct_change_detail_t` | 账户变动明细表 |
| `trans_acct_sum_change_detail_t` | 账户汇总变动明细表 |
| `trans_acct_act_sum_change_detail_t` | 账户活动汇总变动明细表 |
| `trans_acct_frozen_change_detail_t` | 账户冻结变动明细表 |

## 子账户类型

| 代码 | 名称 | 说明 | 消费顺序 |
|---|---|---|---|
| 01 | 现金账户 | 可提现 | 第2优先 |
| 02 | 膨胀金账户 | 赠送金额，优先消费，不可提现 | 第1优先 |
| 04 | 综合账户 | 综合子账户 | 按配置处理 |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/scConsumeFree` | POST | 标准消费(膨胀金优先扣款，支持分账) |
| `/scConsumeAuth` | POST | 授权消费(需鉴权) |
| `/scConsumePre` | POST | 预消费(冻结) |
| `/scConsumePreFinish` | POST | 预消费完成(上账) |
| `/scConsumeClose` | POST | 预消费关闭(解冻) |
| `/scConsumeCal` | POST | 消费算价 |
| `/scConsumeCalOrder` | POST | 订单级消费算价 |

## 关联 Wiki
- 消费退款：[consume-refund.md](consume-refund.md)
- 冻结/解冻：[frozen.md](frozen.md)
- 扣款：[deduction.md](deduction.md)
- 账户变动：[account-change.md](account-change.md)

---

最后更新：2026-06-06
