# 账户变动源码映射

> 目的：把交易场景、After 组件、账户更新入口、明细写入位置、当前风险边界映射到真实源码，方便排查和后续重构。

## 1. consume 模块主交易场景

| 场景 | 核心类 | 账户更新入口 | 明细/冻结写入 | 说明 |
|------|--------|-------------|--------------|------|
| 消费 | `ConsumeTransAfter` | `baseAccountServiceApi.batchChangeAccountForConsume` | `transAccountApi.batchChangeAccountDetail` + 异步上账 | 收付款卡一起收集；收款卡锁失败时转异步上账 |
| 充值 | `RechargeTransAfter` | `baseAccountServiceApi.batchChangeAccountForRecharge` | 当前类内继续写账户变动明细 | 同时创建膨胀金记录并回填 `acctExpandId` |
| 充值退款 | `RefundRechargeTransAfter` | `baseAccountServiceApi.batchChangeAccountForRefundRecharge` | 当前类内继续写账户变动明细 | 同时扣减膨胀金 |
| 转账 | `TransferTransAfter` | `baseAccountServiceApi.batchChangeAccount` | 当前类内写账户变动明细 + `createFrozenDetail` 解冻 | 付款/收款一起更新，走通用 04 账户路径 |
| 消费退款(主路径) | `ConsumeTransRefundAfter` | `baseAccountServiceApi.batchChangeAccountForRefundConsume` | `writeSummaryChanges(...)` + 04 解冻 | 退款前刷新账户，01/02 膨胀金恢复和账户更新在同一事务接口里 |
| 消费退款(Model1) | `ConsumeTransRefundModel1After` | `baseAccountServiceApi.batchChangeAccountForRefundConsume` | 类内后续明细处理 | 也是退款专用接口，只是模型分支不同 |
| 消费退款04解冻 | `ConsumeTransRefund04After` | 无余额更新 | `transAcctFrozenChangeDetailTService.createFrozenDetail` | 只处理 04 冻结明细回退 |
| 提现提交后处理 | `WithDrawTransAfter` | 无余额更新 | `createFrozenDetail` 解冻或保持处理中状态 | 提现余额真正扣减主要在 task 模块完成 |

## 2. task 模块后处理场景

| 场景 | 核心类 | 账户更新入口 | 明细写入 | 当前判断 |
|------|--------|-------------|----------|---------|
| 异步上账 | `AccountEntryAfterService` | `baseAccountServiceApi.batchChangeAccount` | 当前类内手工创建账户变动明细 | 仍是“两段式”，未统一收口 |
| 提现完成 ZX | `ZxWithDrawUpdateStatusAfterService` | `baseAccountServiceApi.batchChangeAccount` | 当前类内手工创建账户变动明细 | 已改成批量账户接口，但仍未和明细合并成单事务 |
| 提现完成 PA | `PaWithDrawUpdateStatusAfterService` | `baseAccountServiceApi.updateCardSubAccount` | 当前类内手工创建账户变动明细 | 仍是更旧路径，优先重构对象 |
| 转账撤销 | `TransferRecallServiceImpl` | `accountServiceApi.updateCardSubAccount` | 冻结明细通过 `transAccountApi.createFrozenDetail` | 明显旧路径，未走批量账户接口 |
| 交易回溯/撤销 | `TransRecallServiceImpl` | 以冻结明细回退为主 | `transAccountApi.createFrozenDetail` | 偏回溯补偿逻辑，不是统一账户变动模型 |

## 3. 关键源码入口

### consume

- 消费账户更新和批量明细：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consume/consume/ConsumeTransAfter.java`
- 充值：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTransAfter.java`
- 充值退款：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/refundRecharge/RefundRechargeTransAfter.java`
- 转账：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/transfer/api/TransferTransAfter.java`
- 消费退款：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefundAfter.java`
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefundModel1After.java`
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/consumeRefund/ConsumeTransRefund04After.java`
- 提现后处理：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/withDraw/WithDrawTransAfter.java`

### task

- 异步上账：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/AccountEntryAfterService.java`
- 提现完成：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/zx/ZxWithDrawUpdateStatusAfterService.java`
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/pa/PaWithDrawUpdateStatusAfterService.java`
- 撤销/回溯：
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/TransferRecallServiceImpl.java`
  - `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/TransRecallServiceImpl.java`

## 4. task 模块待重点重构点

### 高优先级

- `PaWithDrawUpdateStatusAfterService`
  - 仍直接调用 `updateCardSubAccount`
  - 账户更新和明细写入是分离的
  - 更容易出现“余额更新成功，明细失败”或反向不一致

- `TransferRecallServiceImpl`
  - 仍直接调用 `updateCardSubAccount`
  - 没有统一走批量账户变动接口
  - 说明撤销链路还没有纳入新账户变更抽象

### 中优先级

- `ZxWithDrawUpdateStatusAfterService`
  - 已切到 `batchChangeAccount`
  - 但账户余额更新和明细写入仍是两段式

- `AccountEntryAfterService`
  - 异步上账也还是“先 base 更新余额，再 consume 记明细”
  - 与设计中的统一事务方向不一致

### 特殊说明

- `TransRecallServiceImpl`
  - 主要做回溯补偿和冻结明细回退
  - 不能简单按普通交易 After 组件方式改
  - 后续如果收口，需要先明确“回溯补偿”和“正常交易账户变更”是否共用统一模型

## 5. 2026-04 原子一致性改造进展

> 以下为 2026-03 至 2026-04 期间的重大改造更新

### 5.1 AccountChangeBatchService 已落地

`fund-catering-base` 中新增 `AccountChangeBatchService`，提供按交易场景封装的统一账户批量变动接口：

- `batchChangeAccountForConsume` — 消费
- `batchChangeAccountForRecharge` — 充值
- `batchChangeAccountForRefundRecharge` — 充值退款
- `batchChangeAccountForRefundConsume` — 消费退款
- `batchChangeAccount` — 通用（转账/提现等）

源码：`fund-catering/fund-catering-base/fund-catering-base-service/.../service/AccountChangeBatchService.java`

### 5.2 六大交易原子一致性更新

截至 2026-04，以下交易已全部改造为原子一致性更新账户余额：
- 消费 / 消费退款
- 充值 / 充值退款
- 转账 / 提现

对应 git 提交：`cf34c8d9a1` 等系列提交（`lsym_20260116_limeng_restruct` 分支）

### 5.3 交易回溯改造

- 交易回溯和转账回溯逻辑已改造（`2b84b40f5a`）
- 修复了转账回溯账户更新缺失 MAC 值导致 CAS 校验失败的问题（`c9ca4269bf`）

## 6. 当前总体判断（更新于 2026-04）

- `consume` 主交易路径已经大量接入分场景账户批量接口
- `task` 模块仍混合存在：
  - 新路径：`batchChangeAccount`
  - 旧路径：`updateCardSubAccount`
  - 手工明细路径：`createTransAcctChange` / `createFrozenDetail`
- 因此，账户变动重构当前最现实的主战场不是 `consume` 主流程，而是 `task` 后处理和撤销链路

## 7. 银行明确失败 + 解冻时的状态落库规则

- 规则：
  - 如果渠道/银行返回的是“明确失败”，并且当前组件在异常分支里执行了解冻，那么主流水和子流水失败状态必须在当前组件内落库。
  - 不能只依赖 `*After` 组件更新失败状态，因为一旦前置组件抛异常，LiteFlow 后续节点不会继续执行。
  - 如果当前策略是“不解冻，交给回溯处理”，则不要在这里直接把主流水置失败。

- 2026-03-17 已落地修正：
  - `TransferTrans`
    - 在明确失败且带返回模型时，先保存 `slot.setTransTransferResult(...)`
    - 在异常分支内回写主/子转账流水失败状态
    - 然后执行解冻
  - `TransferTransAuth`
    - 授权转账明确失败时同样先保存返回结果
    - 在 `BaseException` 分支内回写主/子转账流水失败状态
    - 然后执行解冻

- 2026-03-17 排查结论：
  - `WithDrawTrans`
    - 明确失败不会在前置节点抛异常中断 after，仍由 `WithDrawTransAfter` 统一更新状态和处理解冻，当前结构可接受。
  - `ConsumeTrans04`
    - 明确失败时会中断后续流程，但当前场景不属于“已解冻仍未落失败状态”的规则范围。
  - `ConsumeTransRefund04Process`
    - 明确失败时会中断后续流程，但当前场景不属于“已解冻仍未落失败状态”的规则范围。
