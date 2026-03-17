# task 模块账户变动统一改造方案

> 日期：2026-03-17  
> 目标：为 `fund-catering-task` 提供后续可执行的账户变动统一改造方案。  
> 当前明确排除：
> - `PaWithDrawUpdateStatusAfterService`：本次用不到，先记录代做
> - `TransferRecallServiceImpl`：其他老师在处理，本次不动

## 1. 背景

`consume` 主交易路径已经大量接入 `BaseAccountServiceApi` 的分场景批量接口，但 `task` 模块仍存在多种旧路径：

- 直接调用 `updateCardSubAccount`
- 先调 `base` 更新余额，再调 `consume` 手工写明细
- 单独写冻结/解冻明细

这会导致：

- 账户变动模型不统一
- 事务边界不一致
- 问题排查成本高
- MAC/CAS 风险在旧路径更容易暴露

## 2. 本次建议范围

### 范围内

- `AccountEntryAfterService`
- `ZxWithDrawUpdateStatusAfterService`
- `TransRecallServiceImpl` 的“可梳理部分”，先做设计，不直接承诺与普通交易统一

### 范围外

- `PaWithDrawUpdateStatusAfterService`
  - 原因：当前业务不用
  - 状态：记录为后续代做

- `TransferRecallServiceImpl`
  - 原因：已有其他老师处理
  - 状态：本方案不介入实现

## 3. 改造目标

### 目标一

尽量消除 `task` 模块中直接调用 `updateCardSubAccount` 的路径。

### 目标二

把“账户更新”和“账户明细写入”的职责边界明确下来，至少做到：

- 先统一识别哪些类仍是两段式
- 哪些类可以迁到统一账户变动 API
- 哪些类因补偿/回溯特性不能直接套用普通交易模型

### 目标三

为后续“6 张表迁移到 base 库 + 统一事务”保留清晰迁移路径。

## 4. 分类策略

### A 类：普通后处理，适合优先统一

- `AccountEntryAfterService`
- `ZxWithDrawUpdateStatusAfterService`

这类逻辑特征：

- 有明确交易结果
- 账户余额变动规则稳定
- 明细写入结构固定
- 不属于复杂回溯补偿

### B 类：补偿/回溯逻辑，不能直接硬套

- `TransRecallServiceImpl`

这类逻辑特征：

- 不是正常正向交易流
- 经常夹带状态修正、冻结回退、失败记录补写
- 更适合先抽“补偿动作模型”，再考虑是否纳入统一账户变动接口

### C 类：暂缓

- `PaWithDrawUpdateStatusAfterService`
- `TransferRecallServiceImpl`

## 5. 具体建议

### 5.1 AccountEntryAfterService

当前状态：

- 已使用 `batchChangeAccount`
- 但账户余额更新后，仍在当前类手工创建账户变动明细

建议：

1. 先抽取“异步上账账户变动命令对象”
2. 把“04 账户入账 + Detail + Entry”封装成独立场景接口
3. 后续在统一账户变动 API 具备后，优先迁入

短期可接受方案：

- 保持两段式
- 但把账户更新参数构建、明细构建、错误处理拆开，减少重复代码

### 5.2 ZxWithDrawUpdateStatusAfterService

当前状态：

- 已使用 `batchChangeAccount`
- 仍手工写账户变动明细

建议：

1. 抽取“提现完成账户变动模板”
2. 将账户更新、冻结回退、明细构建整理成统一内部方法
3. 后续与 `withdrawFinishAccountChange` 这类设计接口对齐

### 5.3 TransRecallServiceImpl

当前状态：

- 核心动作偏向“冻结回退/失败补偿/状态修正”
- 已存在 `createFrozenDetail`
- 不适合直接照搬普通交易 `After` 组件的收口方式

建议：

1. 先把它拆成几个动作维度：
   - 状态修正
   - 余额回退
   - 冻结/解冻明细补偿
   - 失败记录补写
2. 判断其中哪些动作能复用统一账户变动服务
3. 其余保留为补偿专用能力

## 6. 推荐实施顺序

1. `ZxWithDrawUpdateStatusAfterService`
   - 成本低
   - 风险边界清楚
   - 容易形成模板

2. `AccountEntryAfterService`
   - 结构清晰
   - 便于抽“异步上账”场景模型

3. `TransRecallServiceImpl`
   - 先设计后实现
   - 不建议直接进入大改

## 7. 暂不处理项记录

### 代做

- `PaWithDrawUpdateStatusAfterService`
  - 仍直接调用 `updateCardSubAccount`
  - 后续需要参照 `ZX` 路径统一

### 他人处理中

- `TransferRecallServiceImpl`
  - 当前不介入实现
  - 后续只在需要对齐整体账户变动框架时再复核

## 8. 最终建议

当前最值得做的不是一次性把 `task` 全部推平，而是：

- 先把“普通后处理”从旧路径中剥离出来
- 再把“补偿/回溯”单独建模
- 最后再和 `base` 侧统一账户变动 API 对齐

这样风险最低，也最符合现在代码的真实状态。
