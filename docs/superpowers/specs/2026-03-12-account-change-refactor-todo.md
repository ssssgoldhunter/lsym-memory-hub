# 账户变动模块重构计划

> **状态**: 待处理（后期重构）
> **创建日期**: 2026-03-12
> **优先级**: 中

---

## 一、背景与目标

### 1.1 问题描述

当前系统中，账户变动相关的数据分散在两个数据库：

| 库 | 表 | 说明 |
|---|-----|------|
| base库 | bas_card_sub_account_t | 子账户表 |
| base库 | bas_sub_account_expend_t | 科目账户表(膨胀金) |
| consume库 | trans_acct_change_detail_t | 账户变动明细表(4张) |
| consume库 | trans_acct_frozen_change_detail_t | 冻结明细表 |
| consume库 | trans_acct_change_entry_detail_t | Entry明细表 |

**问题**：
- 子账户更新在 base-service（远程调用）
- 变动明细记录在 consume-service
- 两者在不同数据库，无法保证事务一致性

### 1.2 改造目标

将6张账户变动相关表迁移到 base 库，按交易场景封装统一 API，保证事务一致性。

---

## 二、涉及的数据表（8张）

### 2.1 现有表（2张）- 已在 base 库

| 表名 | 说明 | 状态 |
|------|------|------|
| bas_card_sub_account_t | 子账户表 | 无需处理 |
| bas_sub_account_expend_t | 科目账户表(膨胀金) | 无需处理 |

### 2.2 需要迁移的表（6张）

| 序号 | 表名 | 说明 | 当前库 | 目标库 |
|------|------|------|--------|--------|
| 1 | trans_acct_change_detail_t | 账户变动明细表 | consume | base |
| 2 | trans_acct_sum_change_detail_t | 账户汇总变动明细表 | consume | base |
| 3 | trans_acct_act_sum_change_detail_t | 账户活动汇总变动明细表 | consume | base |
| 4 | trans_sum_change_detail_t | 总账户变动表 | consume | base |
| 5 | trans_acct_frozen_change_detail_t | 冻结明细表 | consume | base |
| 6 | trans_acct_change_entry_detail_t | Entry明细表 | consume | base |

---

## 三、账户变更操作分类

### 3.1 账户操作（2个）

| 操作 | 方法 | 说明 |
|------|------|------|
| 账户余额更新 | `batchChangeAccount` | 更新子账户余额 |
| 膨胀金更新 | `batchChangeAccountForXxx` | 按场景区分（消费/充值/退款等） |

### 3.2 记录操作（3类，6张表）

| 操作 | 表名 | 说明 |
|------|------|------|
| **变动明细** | trans_acct_change_detail_t | 账户变动明细 |
| | trans_acct_sum_change_detail_t | 账户汇总变动明细 |
| | trans_acct_act_sum_change_detail_t | 账户活动汇总变动明细 |
| | trans_sum_change_detail_t | 总账户变动表 |
| **冻结明细** | trans_acct_frozen_change_detail_t | 冻结/解冻明细（同一张表） |
| **Entry明细** | trans_acct_change_entry_detail_t | 上账Entry明细 |

---

## 四、交易场景与账户变更对照

### 4.1 consume-service 中的 TransAfter 组件

| 序号 | 组件类 | 交易场景 | 账户操作 | 记录操作 |
|------|--------|----------|----------|----------|
| 1 | ConsumeTransAfter | 消费 | batchChangeAccountForConsume | Detail + Frozen + Entry |
| 2 | RechargeTransAfter | 充值 | batchChangeAccountForRecharge | Detail |
| 3 | RefundRechargeTransAfter | 充值退款 | batchChangeAccountForRefundRecharge | Detail |
| 4 | ConsumeTransRefundAfter | 消费退款(01) | batchChangeAccountForRefundConsume | Detail |
| 5 | ConsumeTransRefund04After | 消费退款(04) | batchChangeAccountForRefundConsume | Detail + Frozen |
| 6 | ConsumeTransRefundModel1After | 消费退款(Model1) | batchChangeAccountForRefundConsume | Detail |
| 7 | WithDrawTransAfter | 提现 | batchChangeAccount | Detail + Frozen |
| 8 | TransferTransAfter | 转账 | batchChangeAccount x2 | Detail + Frozen |
| 9 | TransferTransInnerAfter | 转账(内部) | batchChangeAccount | Detail + Frozen |
| 10 | ConsumeTransPreAfter | 预消费 | batchChangeAccount | Frozen |
| 11 | ConsumeTransCloseAfter | 消费关闭 | - | Frozen |
| 12 | FrozenTrans | 冻结 | batchChangeAccount | Frozen |
| 13 | UnFrozenTrans | 解冻 | batchChangeAccount | Frozen |

### 4.2 task-service 中的账户变更

| 序号 | 组件类 | 场景 | 账户操作 | 记录操作 |
|------|--------|------|----------|----------|
| 14 | PaWithDrawUpdateStatusAfterService | 提现完成(PA) | batchChangeAccount | Detail + Frozen |
| 15 | ZxWithDrawUpdateStatusAfterService | 提现完成(ZX) | batchChangeAccount | Detail + Frozen |
| 16 | AccountEntryAfterService | 上账 | - | Detail + Entry |
| 17 | TransferRecallServiceImpl | 转账撤销 | batchChangeAccount x2 | Detail + Frozen |
| 18 | TransRecallServiceImpl | 交易撤销 | batchChangeAccount | Detail + Frozen |

---

## 五、场景组合分类

| 场景组合 | 包含操作 | 示例 |
|----------|----------|------|
| 账户+变动明细+冻结+Entry | 4类全有 | 消费、预消费完成 |
| 账户+变动明细+冻结 | Detail + Frozen | 转账、提现、消费退款04 |
| 账户+变动明细 | Detail only | 充值、充值退款、消费退款01 |
| 账户+冻结 | Frozen only | 预消费、冻结、解冻 |
| 变动明细+Entry | Detail + Entry | 上账 |

---

## 六、代码迁移清单

### 6.1 Entity（6个）

```
当前路径: slhy/fund-catering/fund-catering-consume/.../domain/
迁移后: slhy/fund-catering/fund-catering-base/.../domain/

├── TransAcctChangeDetailT.java
├── TransAcctSumChangeDetailT.java
├── TransAcctActSumChangeDetailT.java
├── TransSumChangeDetailT.java
├── TransAcctFrozenChangeDetailT.java
└── TransAcctChangeEntryDetailT.java
```

### 6.2 Mapper（6个）

```
当前路径: slhy/fund-catering/fund-catering-consume/.../mapper/
迁移后: slhy/fund-catering/fund-catering-base/.../mapper/

├── TransAcctChangeDetailTMapper.java
├── TransAcctSumChangeDetailTMapper.java
├── TransAcctActSumChangeDetailTMapper.java
├── TransSumChangeDetailTMapper.java
├── TransAcctFrozenChangeDetailTMapper.java
└── TransAcctChangeEntryDetailTMapper.java
```

### 6.3 Service（12个 - 接口+实现）

```
当前路径: slhy/fund-catering/fund-catering-consume/.../service/
迁移后: slhy/fund-catering/fund-catering-base/.../service/

├── TransAcctChangeDetailTService.java + impl/
├── TransAcctSumChangeDetailTService.java + impl/
├── TransAcctActSumChangeDetailTService.java + impl/
├── TransSumChangeDetailTService.java + impl/
├── TransAcctFrozenChangeDetailTService.java + impl/
└── TransAcctChangeEntryDetailTService.java + impl/
```

### 6.4 需要新增的 API

```
slhy/fund-catering/fund-catering-base/.../api/
└── AccountChangeApi.java  # 统一账户变动API
```

### 6.5 需要新增的 Controller

```
slhy/fund-catering/fund-catering-base/.../controller/
└── AccountChangeController.java  # 账户变动控制器
```

---

## 七、API 设计

### 7.1 设计原则

按业务场景封装，一个 API 包含：
1. 账户操作（更新子账户 + 更新膨胀金）
2. 记录操作（变动明细 + 冻结明细 + Entry明细）

内部使用 `@Transactional` 保证事务一致性。

### 7.2 API 列表

| 序号 | API 名称 | 场景 |
|------|----------|------|
| 1 | consumeAccountChange | 消费 |
| 2 | rechargeAccountChange | 充值 |
| 3 | refundRechargeAccountChange | 充值退款 |
| 4 | refundConsume01AccountChange | 消费退款(01) |
| 5 | refundConsume04AccountChange | 消费退款(04) |
| 6 | transferAccountChange | 转账 |
| 7 | withdrawAccountChange | 提现 |
| 8 | preConsumeAccountChange | 预消费 |
| 9 | preConsumeFinishAccountChange | 预消费完成 |
| 10 | frozenAccountChange | 冻结 |
| 11 | unfrozenAccountChange | 解冻 |
| 12 | withdrawFinishAccountChange | 提现完成 |
| 13 | entryAccountChange | 上账 |

---

## 八、调用链路变化

### 8.1 当前调用链路

```
TransAfter (consume-service)
    │
    ├──► base-service (Feign)  ──► 子账户更新 (bas_card_sub_account_t)
    │                           ──► 膨胀金更新 (bas_sub_account_expend_t)
    │
    └──► consume-service 本地 ──► 变动明细 (trans_acct_change_detail_t 等6张)
```

**问题**：跨库调用，无法保证事务一致性

### 8.2 改造后调用链路

```
TransAfter (consume-service)
    │
    └──► base-service (Feign)  ──► 统一 AccountChangeApi
                                    │
                                    ├── 子账户更新 (bas_card_sub_account_t)
                                    ├── 膨胀金更新 (bas_sub_account_expend_t)
                                    ├── 变动明细 (trans_acct_change_detail_t 等6张)
                                    │
                                    └── @Transactional 保证事务一致性
```

---

## 九、实施步骤

### 9.1 第一阶段：数据库迁移

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 备份数据 | 备份 consume 库中的6张表 |
| 2 | 创建表结构 | 在 base 库创建6张表（结构一致） |
| 3 | 数据迁移 | 使用 ETL 工具迁移数据 |
| 4 | 数据校验 | 验证数据一致性和完整性 |
| 5 | 双写验证 | 新旧库同时写入，验证一致性 |

### 9.2 第二阶段：代码迁移

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 迁移 Entity | 复制6个 Entity 类到 base-service |
| 2 | 迁移 Mapper | 复制6个 Mapper 接口到 base-service |
| 3 | 迁移 Service | 复制6个 Service 接口和实现到 base-service |
| 4 | 编译验证 | 确保代码编译通过 |

### 9.3 第三阶段：API 封装

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 定义 API 接口 | 在 base-api 中定义 AccountChangeApi |
| 2 | 实现 Controller | 在 base-service 中实现 AccountChangeController |
| 3 | 封装交易场景 | 实现13个交易场景的 API |
| 4 | 添加事务注解 | 确保事务边界正确 |

### 9.4 第四阶段：调用改造

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 添加 Feign 依赖 | 在 consume-service 中添加 base-api 依赖 |
| 2 | 配置 Feign 客户端 | 配置 base-service 的服务发现 |
| 3 | 改造 TransAfter | 将本地调用改为 Feign API 调用 |
| 4 | 移除本地事务 | 移除原有的 @Transactional 注解 |

### 9.5 第五阶段：上线验证

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 双写验证 | 新旧系统同时运行，对比结果 |
| 2 | 数据对账 | 核对新旧库数据一致性 |
| 3 | 灰度上线 | 按比例切换到新系统 |
| 4 | 旧代码清理 | 移除 consume-service 中的旧代码 |

---

## 十、风险与回滚

### 10.1 风险控制

| 风险 | 方案 |
|------|------|
| 数据丢失 | 迁移前全量备份 |
| 迁移失败 | 保留原表，失败可回退 |
| 服务调用失败 | 配置 Feign 超时和降级 |
| 事务不一致 | API 级别事务保证 |

### 10.2 回滚方案

1. 关闭新 API 调用开关
2. 恢复 consume-service 调用本地代码
3. 恢复 consume 库的数据写入

---

## 十一、待确认事项

1. 数据库迁移的具体时间窗口
2. 双写验证周期（建议1周）
3. 旧代码清理计划（建议上线1个月后）
4. 是否需要灰度策略（按商户/按交易量）

---

## 相关文档

- 设计文档：`docs/superpowers/specs/2026-03-11-account-change-refactor-design.md`

---

**维护者**: Claude Code
**更新日期**: 2026-03-12
