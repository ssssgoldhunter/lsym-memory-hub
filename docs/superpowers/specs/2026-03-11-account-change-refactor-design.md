# 账户变动模块重构设计方案

## 一、背景

### 1.1 问题描述

当前系统中，账户变动相关的数据分散在两个数据库：

| 库 | 表 | 说明 |
|---|-----|------|
| base库 | bas_card_sub_account_t | 子账户表 |
| base库 | bas_sub_account_expend_t | 科目账户表(膨胀金) |
| consume库 | trans_acct_change_detail_t | 账户变动明细表(4张) |
| consume库 | trans_acct_frozen_change_detail_t | 冻结明细表 |
| consume库 | trans_acct_change_entry_detail_t | Entry明细表 |

**问题：**
- 子账户更新在 base-service（远程调用）
- 变动明细记录在 consume-service
- 两者在不同数据库，无法保证事务一致性

### 1.2 改造目标

将6张账户变动相关表迁移到 base 库，按交易场景封装统一 API，保证事务一致性。

---

## 二、改造范围

### 2.1 涉及的表清单

#### 2.1.1 需要迁移的表（6张）

| 序号 | 表名 | 说明 | 当前库 | 目标库 |
|------|------|------|--------|--------|
| 1 | trans_acct_change_detail_t | 账户变动明细表 | consume | base |
| 2 | trans_acct_sum_change_detail_t | 账户汇总变动明细表 | consume | base |
| 3 | trans_acct_act_sum_change_detail_t | 账户活动汇总变动明细表 | consume | base |
| 4 | trans_sum_change_detail_t | 总账户变动表 | consume | base |
| 5 | trans_acct_frozen_change_detail_t | 冻结明细表 | consume | base |
| 6 | trans_acct_change_entry_detail_t | Entry明细表 | consume | base |

#### 2.1.2 已有的表（2张）- 无需迁移

| 序号 | 表名 | 说明 | 所在库 |
|------|------|------|--------|
| 1 | bas_card_sub_account_t | 子账户表 | base |
| 2 | bas_sub_account_expend_t | 科目账户表(膨胀金) | base |

#### 2.1.3 涉及的表总数

| 类型 | 数量 | 说明 |
|------|------|------|
| 迁移表 | 6张 | 从consume迁到base |
| 已有表 | 2张 | 已在base，无需迁移 |
| **总计** | **8张** | 全部在base库，统一事务 |

### 2.2 代码迁移清单

#### 2.2.1 Entity 类（6个文件）

```
当前路径: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/.../domain/
迁移后: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/.../domain/

文件清单:
├── TransAcctChangeDetailT.java         # 账户变动明细
├── TransAcctSumChangeDetailT.java       # 账户汇总变动明细
├── TransAcctActSumChangeDetailT.java    # 账户活动汇总变动明细
├── TransSumChangeDetailT.java           # 总账户变动
├── TransAcctFrozenChangeDetailT.java     # 冻结明细
└── TransAcctChangeEntryDetailT.java      # Entry明细
```

#### 2.2.2 Mapper 接口（6个文件）

```
当前路径: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/.../mapper/
迁移后: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/.../mapper/

文件清单:
├── TransAcctChangeDetailTMapper.java
├── TransAcctSumChangeDetailTMapper.java
├── TransAcctActSumChangeDetailTMapper.java
├── TransSumChangeDetailTMapper.java
├── TransAcctFrozenChangeDetailTMapper.java
└── TransAcctChangeEntryDetailTMapper.java
```

#### 2.2.3 Mapper XML（6个文件）

```
当前路径: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/resources/mapper/
迁移后: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/resources/mapper/

文件清单:
├── TransAcctChangeDetailTMapper.xml
├── TransAcctSumChangeDetailTMapper.xml
├── TransAcctActSumChangeDetailTMapper.xml
├── TransSumChangeDetailTMapper.xml
├── TransAcctFrozenChangeDetailTMapper.xml
└── TransAcctChangeEntryDetailTMapper.xml
```

#### 2.2.4 Service 接口（6个文件）

```
当前路径: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/.../service/
迁移后: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/.../service/

文件清单:
├── TransAcctChangeDetailTService.java
├── TransAcctSumChangeDetailTService.java
├── TransAcctActSumChangeDetailTService.java
├── TransSumChangeDetailTService.java
├── TransAcctFrozenChangeDetailTService.java
└── TransAcctChangeEntryDetailTService.java
```

#### 2.2.5 Service 实现（6个文件）

```
当前路径: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/.../service/impl/
迁移后: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/.../service/impl/

文件清单:
├── TransAcctChangeDetailTServiceImpl.java
├── TransAcctSumChangeDetailTServiceImpl.java
├── TransAcctActSumChangeDetailTServiceImpl.java
├── TransSumChangeDetailTServiceImpl.java
├── TransAcctFrozenChangeDetailTServiceImpl.java
└── TransAcctChangeEntryDetailTServiceImpl.java
```

#### 2.2.6 API 接口（需要新增）

```
路径: slhy/fund-catering/fund-catering-base/fund-catering-base-api/src/main/java/.../api/

新增文件:
└── AccountChangeApi.java    # 统一账户变动API
```

#### 2.2.7 Controller（需要新增/改造）

```
路径: slhy/fund-catering/fund-catering-base/fund-catering-base-service/src/main/java/.../controller/

新增文件:
└── AccountChangeController.java  # 账户变动控制器
```

### 2.3 保持不变

| 模块 | 说明 |
|------|------|
| 交易主表 | trans_consume_t, trans_recharge_t 等保持在 consume 库 |
| 交易流程 | LiteFlow 流程配置不变 |
| 校验逻辑 | Check 组件不变 |
| Trans组件 | Trans/After 组件不变，只改调用方式 |

---

## 三、改造后架构

### 3.1 数据库架构

```
base库 (同一事务)
├── 子账户表 (bas_card_sub_account_t) - 已有
├── 科目账户表 (bas_sub_account_expend_t) - 已有
├── 账户变动明细表 (4张) - 从consume迁入
│   ├── trans_acct_change_detail_t
│   ├── trans_acct_sum_change_detail_t
│   ├── trans_acct_act_sum_change_detail_t
│   └── trans_sum_change_detail_t
├── 冻结明细表 - 从consume迁入
│   └── trans_acct_frozen_change_detail_t
└── Entry明细表 - 从consume迁入
    └── trans_acct_change_entry_detail_t

consume库
└── 交易主表
    ├── trans_consume_t
    ├── trans_recharge_t
    ├── trans_transfer_t
    └── ...
```

### 3.2 调用架构

```
consume-service                              base-service (base库)
    │                                              │
    │  Feign API 调用 (一个业务场景一个API)         │
    │                                              │
    ├────────────────► 消费账户变动API ◄───────────┤ 子账户更新
    │                 (consumeAccountChange)       │ 科目账户更新
    │                                            │ 冻结明细记录
    │                                            │ 4张变动明细记录
    │                                            │ Entry记录
    │                                            │
    ├────────────────► 充值账户变动API ◄───────────┤
    │                 (rechargeAccountChange)      │
    │                                            │
    ├────────────────► 退款账户变动API ◄───────────┤
    │                 (refundAccountChange)       │
    │                                            │
    ... ...                                      │
    │                                            │
    └─ 交易主表 ◄────────────────────────────────┘
```

---

## 四、API 设计

### 4.1 设计原则

参考现有消费改造结构，采用**两次调用**模式：

| 阶段 | 调用 | 操作 |
|------|------|------|
| 第一阶段 | 账户变动API | 更新子账户余额 + 更新膨胀金账户 |
| 第二阶段 | 变动记录API | 记录冻结明细 + 记录4张变动明细 + 记录Entry明细 |

**改造目标**：将两次调用合并为**一个业务场景API**，内部保证事务一致性。

### 4.2 API 列表

按交易场景封装，共 **13 个统一 API**（consume项目11个 + task项目2个）：

#### consume 项目（11个）

| 序号 | 业务场景API | 账户操作(第一阶段) | 记录操作(第二阶段) | 来源 |
|------|------------|-------------------|-------------------|------|
| 1 | consumeAccountChange | batchChangeAccountForConsume | Detail + Frozen + Entry | consume |
| 2 | rechargeAccountChange | batchChangeAccountForRecharge | Detail | consume |
| 3 | refund04AccountChange | batchChangeAccountForRefundConsume | Detail + Frozen | consume |
| 4 | refund01AccountChange | batchChangeAccountForRefundConsume | Detail | consume |
| 5 | rechargeGiftAccountChange | batchChangeAccountForRecharge | Detail | consume |
| 6 | transferAccountChange | batchChangeAccount x2 | Detail + Frozen | consume |
| 7 | withdrawAccountChange | batchChangeAccount | Detail + Frozen | consume |
| 8 | preConsumeAccountChange | batchChangeAccount | Frozen | consume |
| 9 | preConsumeFinishAccountChange | batchChangeAccountForConsume | Detail + Frozen + Entry | consume |
| 10 | frozenAccountChange | batchChangeAccount | Frozen | consume |
| 11 | unfrozenAccountChange | batchChangeAccount | Frozen | consume |

#### task 项目（2个）

| 序号 | 业务场景API | 账户操作(第一阶段) | 记录操作(第二阶段) | 来源 |
|------|------------|-------------------|-------------------|------|
| 12 | withdrawFinishAccountChange | batchChangeAccount | Detail + Frozen | task |
| 13 | entryAccountChange | - | Detail + Entry | task |

### 4.2 API 详细说明

#### 4.2.1 消费账户变动 (consumeAccountChange)

**场景**: 用户进行消费交易

**内部逻辑**:
```java
@Transactional(rollbackFor = Exception.class)
public AccountChangeResponse consumeAccountChange(ConsumeAccountChangeRequest request) {
    // ==================== 第一阶段：账户操作 ====================
    // 1. 调用账户变动服务（更新子账户余额 + 更新膨胀金账户）
    AccountChangeBatchReq accountReq = buildAccountChangeReq(request);
    DefaultResult<AccountChangeForConsumeRes> accountResult =
        accountChangeBatchService.batchChangeAccountForConsume(accountReq);

    // ==================== 第二阶段：记录操作 ====================
    // 2. 记录冻结明细
    frozenDetailService.saveFrozenDetail(request.getFrozenDetails());

    // 3. 记录4张变动明细
    changeDetailService.saveChangeDetails(request.getChangeDetails());

    // 4. 记录Entry明细（如需异步上账）
    if (request.getNeedEntry()) {
        entryDetailService.saveEntryDetail(request.getEntryDetails());
    }

    return buildResponse(accountResult);
}
```

**涉及表**:
- 子账户表 (bas_card_sub_account_t)
- 膨胀金表 (bas_sub_account_expend_t)
- 冻结明细表 (trans_acct_frozen_change_detail_t)
- 4张变动明细表
- Entry明细表 (trans_acct_change_entry_detail_t)

#### 4.2.2 充值账户变动 (rechargeAccountChange)

**场景**: 用户充值

**内部逻辑**:
```java
@Transactional(rollbackFor = Exception.class)
public AccountChangeResponse rechargeAccountChange(RechargeAccountChangeRequest request) {
    // ==================== 第一阶段：账户操作 ====================
    AccountChangeBatchReq accountReq = buildAccountChangeReq(request);
    DefaultResult<AccountChangeForRechargeRes> accountResult =
        accountChangeBatchService.batchChangeAccountForRecharge(accountReq);

    // ==================== 第二阶段：记录操作 ====================
    // 记录4张变动明细
    changeDetailService.saveChangeDetails(request.getChangeDetails());

    return buildResponse(accountResult);
}
```

#### 4.2.3 04退款账户变动 (refund04AccountChange)

**场景**: 04模式消费退款（冻结→变动→解冻）

#### 4.2.4 01退款账户变动 (refund01AccountChange)

**场景**: 01模式消费退款（充值到账）

#### 4.2.5 充值赠送账户变动 (rechargeGiftAccountChange)

**场景**: 充值赠送膨胀金

#### 4.2.6 转账账户变动 (transferAccountChange)

**场景**: 账户间转账（转出+转入）

#### 4.2.7 提现账户变动 (withdrawAccountChange)

**场景**: 提现到银行卡

#### 4.2.8 预消费账户变动 (preConsumeAccountChange)

**场景**: 预授权消费（仅冻结）

#### 4.2.9 预消费完成账户变动 (preConsumeFinishAccountChange)

**场景**: 预授权消费完成

#### 4.2.10 冻结账户变动 (frozenAccountChange)

**场景**: 单独冻结操作

#### 4.2.11 解冻账户变动 (unfrozenAccountChange)

**场景**: 单独解冻操作

### 4.3 事务保证

每个 API 内部使用 `@Transactional(rollbackFor = Exception.class)` 注解，保证以下操作在同一事务中：

- 子账户余额更新 (bas_card_sub_account_t)
- 科目账户(膨胀金)更新 (bas_sub_account_expend_t)
- 冻结明细记录 (trans_acct_frozen_change_detail_t)
- 4张变动明细记录 (trans_acct_change_detail_t 等)
- Entry明细记录 (trans_acct_change_entry_detail_t)

### 4.4 错误处理

```java
public enum AccountChangeErrorCode {
    SUCCESS("0000", "成功"),
    ACCOUNT_NOT_FOUND("1001", "账户不存在"),
    BALANCE_NOT_ENOUGH("1002", "余额不足"),
    FROZEN_FAILED("1003", "冻结失败"),
    UNFROZEN_FAILED("1004", "解冻失败"),
    SAVE_DETAIL_FAILED("1005", "保存变动明细失败"),
    SAVE_ENTRY_FAILED("1006", "保存Entry失败"),
    TRANS_EXPIRED("1007", "交易已过期"),
    SYSTEM_ERROR("9999", "系统异常");

    private String code;
    private String message;
}
```

---

## 五、实施步骤

### 5.1 第一阶段：数据库迁移

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 备份数据 | 备份 consume 库中的6张表 |
| 2 | 创建表结构 | 在 base 库创建6张表（结构一致） |
| 3 | 数据迁移 | 使用 ETL 工具迁移数据 |
| 4 | 数据校验 | 验证数据一致性和完整性 |
| 5 | 双写验证 | 新旧库同时写入，验证一致性 |

**迁移SQL示例**:
```sql
-- 在 base 库执行
CREATE TABLE trans_acct_change_detail_t LIKE consume_db.trans_acct_change_detail_t;
INSERT INTO base_db.trans_acct_change_detail_t SELECT * FROM consume_db.trans_acct_change_detail_t;
```

### 5.2 第二阶段：代码迁移

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 迁移 Entity | 复制6个 Entity 类到 base-service |
| 2 | 迁移 Mapper | 复制6个 Mapper 接口到 base-service |
| 3 | 迁移 Mapper XML | 复制6个 XML 到 base-service |
| 4 | 迁移 Service | 复制6个 Service 接口和实现到 base-service |
| 5 | 编译验证 | 确保代码编译通过 |
| 6 | 本地测试 | 单服务功能测试 |

### 5.3 第三阶段：API 封装

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 定义 API 接口 | 在 base-api 中定义 AccountChangeApi |
| 2 | 实现 Controller | 在 base-service 中实现 AccountChangeController |
| 3 | 封装交易场景 | 实现11个交易场景的 API |
| 4 | 添加事务注解 | 确保事务边界正确 |
| 5 | 单元测试 | 编写单元测试用例 |

### 5.4 第四阶段：调用改造

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 添加 Feign 依赖 | 在 consume-service 中添加 base-api 依赖 |
| 2 | 配置 Feign 客户端 | 配置 base-service 的服务发现 |
| 3 | 改造 TransAfter | 将本地调用改为 Feign API 调用 |
| 4 | 移除本地事务 | 移除原有的 @Transactional 注解 |
| 5 | 集成测试 | 端到端测试 |

**调用示例**:
```java
// consume-service 中的调用方式
@Autowired
private AccountChangeApi accountChangeApi;

public void processConsume(ConsumeTransVo vo) {
    // 构造请求
    ConsumeAccountChangeRequest request = new ConsumeAccountChangeRequest();
    request.setTransNo(vo.getTransNo());
    request.setCardCode(vo.getPayCardCode());
    // ... 其他参数

    // 调用 base-service 的 API
    AccountChangeResponse response = accountChangeApi.consumeAccountChange(request);

    if (!response.getSuccess()) {
        throw new BusinessException(response.getCode(), response.getMessage());
    }
}
```

### 5.5 第五阶段：上线验证

| 序号 | 步骤 | 说明 |
|------|------|------|
| 1 | 双写验证 | 新旧系统同时运行，对比结果 |
| 2 | 数据对账 | 核对新旧库数据一致性 |
| 3 | 灰度上线 | 按比例切换到新系统 |
| 4 | 监控验证 | 观察业务指标和错误率 |
| 5 | 旧代码清理 | 移除 consume-service 中的旧代码 |
| 6 | 旧表清理 | 确认无误后删除 consume 库中的旧表 |

---

## 六、风险控制

### 6.1 数据迁移风险

| 风险 | 方案 | 回滚 |
|------|------|------|
| 数据丢失 | 迁移前全量备份 | 恢复备份 |
| 数据不一致 | 迁移后校验 count 和 sum | 重新迁移 |
| 迁移失败 | 保留原表，失败可回退 | 切回原表 |

### 6.2 服务调用风险

| 风险 | 方案 | 回滚 |
|------|------|------|
| 超时 | 配置 Feign 超时时间（默认30s） | 增大超时配置 |
| 降级 | 预留本地调用降级开关 | 降级到本地调用 |
| 网络抖动 | 配置重试机制 | 增加重试次数 |

### 6.3 事务一致性风险

| 风险 | 方案 | 监控 |
|------|------|------|
| 事务失败 | API 级别事务保证 | 事务日志 |
| 数据不一致 | 定期对账任务 | 对账报表 |
| 死锁 | 优化索引，合理安排SQL顺序 | 慢SQL告警 |

---

## 七、监控指标

### 7.1 业务指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| API 调用量 | 各场景 API 调用次数 | - |
| API 成功率 | 各场景 API 成功率 | < 99.9% |
| API 响应时间 | P99 响应时间 | > 5s |
| 事务失败数 | 事务回滚次数 | > 0 |

### 7.2 数据库指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| 连接数 | 数据库连接池使用率 | > 80% |
| 慢SQL | 执行时间 > 1s 的 SQL | > 10条/分钟 |
| 死锁 | 数据库死锁次数 | > 0 |

---

## 八、改造收益

1. **事务一致性**: 账户变动操作在同一数据库，保证 ACID
2. **简化调用**: 消费端只需调用一个 API
3. **降低复杂度**: 移除分布式事务依赖
4. **便于维护**: 账户相关逻辑集中在 base-service
5. **性能提升**: 减少网络调用延迟

---

## 九、回滚方案

### 9.1 快速回滚

1. 关闭新 API 调用开关
2. 恢复 consume-service 调用本地代码
3. 恢复 consume 库的数据写入

### 9.2 数据回滚

1. 恢复 consume 库中的6张表数据
2. 停止 base 库的写入
3. 恢复原调用链路

---

## 十、待确认事项

1. 数据库迁移的具体时间窗口
2. 双写验证周期（建议1周）
3. 旧代码清理计划（建议上线1个月后）
4. 是否需要灰度策略（按商户/按交易量）

---

## 附录

### A. 现有代码中的调用位置

| 交易场景 | 调用类 | 方法 |
|---------|-------|------|
| 消费 | ConsumeTransAfter.java | batchChangeAccountDetail() |
| 充值 | RechargeTransAfter.java | batchChangeAccountDetail() |
| 退款 | ConsumeTransRefundAfter.java | batchChangeAccountDetail() |
| 转账 | TransferTransAfter.java | batchChangeAccountDetail() |
| 提现 | WithDrawTransAfter.java | createFrozenDetail() |
| 冻结 | FrozenTrans.java | createFrozenDetail() |
| 解冻 | UnFrozenTrans.java | createFrozenDetail() |

### B. 相关配置文件

| 配置项 | 说明 |
|-------|------|
| feign.client.base.url | base-service 服务地址 |
| base.service.timeout | API 调用超时时间 |
| base.service.retry | 重试次数 |

---

**文档信息**
- 创建日期：2026-03-11
- 作者：Claude Code
- 版本：v1.0
