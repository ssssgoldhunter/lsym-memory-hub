# lsym 项目精简记忆

> 用于 AI 助手在当前和后续工作中快速恢复项目上下文，只保留高频规则和导航信息。

## 1. 项目定位

- 主代码仓：`/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy`
- 记忆/文档仓：`/Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory-hub`
- 业务领域：`lsym` 餐饮资金体系
- 主工作范围：`slhy/fund-catering`

## 2. 技术主线

- 语言和框架：`Java 17`、`Spring Boot 2.x`
- 流程编排：`LiteFlow`
- 数据访问：`MyBatis Plus`
- 基础设施：`Redis`、`Nacos`

## 3. 默认阅读顺序

1. `lsym-memory-hub/docs/TRANSACTION_QUICK_REFERENCE.md`
2. `lsym-memory-hub/docs/SUPPLY_CHAIN_DESIGN_V5.5.md`
3. `lsym-memory-hub/architecture/FRAMEWORK_STRUCTURE.md`
4. `lsym-memory-hub/modules/MODULE_FUND_CATERING.md`
5. 对应源码目录 `slhy/fund-catering/...`

## 4. 核心业务记忆

- 六大交易：消费、充值、充值退款、提现、转账、消费退款
- 子账户类型：
  - `01` 现金账户
  - `02` 膨胀金账户
  - `04` 综合账户
- 消费默认重点：
  - `02` 优先扣款
  - 支持分账
  - 存在异步上账场景

## 5. 架构记忆

- 写流程看 `TransSlot`
- 查流程看 `QuerySlot`
- LiteFlow 组件顺序：`Pack -> Check -> Trans -> After -> Route`
- 核心链路配置：
  - `fund-catering-consume/.../resources/liteflow/consume.el.xml`
  - `fund-catering-consume/.../resources/liteflow/query.el.xml`

## 6. 模块分工

- `fund-catering-base`：账户、商户、平台、基础能力
- `fund-catering-consume`：交易主流程、LiteFlow 组件
- `fund-catering-task`：定时任务、提现后处理、撤销、上账
- `fund-catering-data-batch`：批处理
- `fund-catering-front`：前置路由和外部通道
- `fund-catering-management`：后台管理
- `fund-catering-report`：报表
- `fund-catering-web`：Web API 聚合出口

## 7. 并发和一致性重点

- 子账户更新依赖 `MAC/CAS`
- 多次账户更新场景必须关注 MAC 刷新
- 已有刷新入口：`BaseSlot.refreshCardSubAccount(...)`
- 排查顺序优先看：
  1. 是否加锁
  2. 是否多次更新同一账户
  3. 更新前是否刷新账户信息
  4. 是否走了正确的账户变动 API

## 8. 当前账户变动现状

- 真实入口是 `BaseAccountServiceApi` 这组接口：
  - `batchChangeAccount`
  - `batchChangeAccountForRecharge`
  - `batchChangeAccountForRefundRecharge`
  - `batchChangeAccountForConsume`
  - `batchChangeAccountForRefundConsume`
- 设计中的统一 `AccountChangeApi` 目前还未完整落地
- “6 张明细/冻结/Entry 表迁移到 base 库并统一事务” 仍属于进行中的重构方向

## 9. 文档与记忆存放规则

- 默认情况下：
  - `md` 文档、分析结论、项目记忆放 `lsym-memory-hub`
  - 源码、配置、资源文件放 `slhy`
- 只有用户明确要求时，才把文档放进代码仓

## 10. 会话工作习惯

- 先读 memory，再对照源码
- 回答业务/架构问题时，优先给出“文档位置 + 源码位置”
- 涉及消费、转账、提现、退款时，优先检查账户更新链路和 MAC 问题
- 需要沉淀长期规则时，优先更新 `lsym-memory-hub`

## 11. 排查检查清单

### A. 交易链路排查

1. 先确认交易类型：
   - 消费
   - 充值
   - 充值退款
   - 转账
   - 提现
   - 消费退款
2. 定位对应 LiteFlow 链：
   - `consume.el.xml`
   - `query.el.xml`
3. 找到对应 `After` 组件或 `task` 后处理类
4. 确认账户更新走的是哪条入口：
   - `batchChangeAccount`
   - `batchChangeAccountForRecharge`
   - `batchChangeAccountForRefundRecharge`
   - `batchChangeAccountForConsume`
   - `batchChangeAccountForRefundConsume`
   - `updateCardSubAccount`

### B. MAC/CAS 排查

1. 是否同一张卡/同一子账户被多次更新
2. 更新前是否调用了 `refreshCardSubAccount(...)`
3. 更新 SQL 是否走到 `and mac = #{req.mac}`
4. 是否有同一 `subAccountId` 的多次变动需要合并
5. 是否因为旧路径直接更新导致 MAC 过期

### C. 事务一致性排查

1. 账户余额更新和明细写入是否在一个服务内完成
2. 是否存在“先调 base，再调 consume 写明细”的两段式流程
3. 是否存在“账户成功、明细失败”或“明细成功、账户失败”的窗口
4. 是否涉及冻结/解冻明细单独写入

### D. task 模块专项排查

1. 优先看提现完成、异步上账、撤销/回溯
2. 如果类里仍调用 `updateCardSubAccount`，默认视为旧路径
3. 如果类里账户更新后再手工写 `createTransAcctChange`，默认视为未统一收口
4. 需要判断该类是普通交易后处理，还是补偿/回溯逻辑，避免误用统一模型

### E. 文档同步

1. 如果发现长期有效的新规则，更新 `workflow/PROJECT_MEMORY.md`
2. 如果发现源码映射变化，更新 `docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
3. 如果只是一次性结论，不必污染长期记忆

### F. 渠道失败与冻结处理规则

1. 如果渠道返回“无明确结果”，且流程选择不解冻、交给回溯处理，则不要在当前节点把主流水直接置失败。
2. 如果渠道返回“明确失败结果”，且当前节点已经执行解冻，那么主流水和对应子流水必须在当前节点或当前异常分支内同步回写失败，不能依赖 LiteFlow 的 after 节点。
3. 判断是否需要补失败状态，先看异常后是否仍会进入 after；如果抛异常会中断 LiteFlow，则 after 中的失败回写不能作为唯一落库点。
4. 2026-03-17 已确认并修正的实例：
   - `TransferTrans`
   - `TransferTransAuth`
