# 回溯（Recall）

## 关联文档
- 技术决策 — MAC 并发修复：[technical-decisions/MAC_CONCURRENCY_FIX.md](../technical-decisions/MAC_CONCURRENCY_FIX.md)
- 账户变动源码映射：[docs/ACCOUNT_CHANGE_SOURCE_MAP.md](../docs/ACCOUNT_CHANGE_SOURCE_MAP.md)

## 源码映射

### task 模块（核心）

| 类 | 路径 | 职责 |
|---|---|---|
| `TransRecallServiceImpl` | `fund-catering-task/.../service/impl/TransRecallServiceImpl.java` | 消费回溯服务：交易状态判定、解冻、04 退款 |
| `TransferRecallServiceImpl` | `fund-catering-task/.../service/impl/TransferRecallServiceImpl.java` | 转账回溯服务：余额校验、中信退款、解冻 |
| `TransRecallJobService` | `fund-catering-task/.../job/TransRecallJobService.java` | 消费回溯定时任务（`@XxlJob("transRecallJobService")`） |
| `TransferRecallJobService` | `fund-catering-task/.../job/TransferRecallJobService.java` | 转账回溯定时任务（`@XxlJob("transferRecallJobService")`） |
| `TransRecallService` | `fund-catering-task/.../service/TransRecallService.java` | 消费回溯接口 |
| `TransferRecallService` | `fund-catering-task/.../service/TransferRecallService.java` | 转账回溯接口 |

## 两种回溯类型

### 消费回溯（TransRecall）

**触发条件**：消费交易（`TRANS_TYPE_P` / `TRANS_TYPE_F`）长时间处于"处理中"状态。

**Job 流程**（`TransRecallJobService`）：
```
1. 按机构参数遍历 → 查询 status=P 的消费记录
2. 逐笔查询中信交易状态（04 账户）
3. 判断 04 账户成功数量：
   ├─ 全部成功 → processAll04Success → transRecallService.transRecall
   ├─ 部分成功 → processPartial04Failed → transRecallService.transRecall04
   └─ 全部失败 → 不做处理
4. 无 04 账户 → processNo04Account → transRecallService.transRecall
5. Redis 分布式锁（cardCode 维度），带重试
```

**Service 核心方法**（`TransRecallServiceImpl`）：

| 方法 | 职责 |
|---|---|
| `transRecall` | 根据 6 种 TransactionType 枚举分流处理 |
| `transRecall04` | 04 退款 + 01/02/04 解冻，更新交易状态为失败 |
| `recallConsumeSubRecNew` | 核心回溯方法：04 中信退款 + 子记录状态更新 + 解冻 |
| `unfreezeFrozenAmount` | 独立解冻方法，按账户类型创建解冻明细 |
| `updateTransStatusSUAndUnfreeze` | 更新交易状态为成功 + 各类子账户解冻 |
| `handle04MismatchCase` | 04 不匹配时创建异步上账 |
| `handle04WithoutChangeDetail` | 04 无账户变动明细时记录异常 |
| `createFailureRecords` | 创建失败记录（主/子流水 + 回溯申请详情） |
| `asyncUpdateSubAccountBalance` | 创建异步上账记录 |

**TransactionType 6 种判定**：

| 类型 | 04 账户 | 账户变动明细 | 匹配 | 处理 |
|---|---|---|---|---|
| `HAS_04_WITH_CHANGE_DETAIL_MATCH` | 有 | 有 | 完全匹配 | 更新成功 + 解冻 |
| `HAS_04_WITH_CHANGE_DETAIL_MISMATCH` | 有 | 有 | 不匹配 | 异步上账 + 更新成功 + 解冻 |
| `HAS_04_WITHOUT_CHANGE_DETAIL` | 有 | 无 | — | 查新增交易 → 记录异常/失败 |
| `NO_04_WITH_CHANGE_DETAIL_MATCH` | 无 | 有 | 完全匹配 | 更新成功 + 解冻 |
| `NO_04_WITH_CHANGE_DETAIL_MISMATCH` | 无 | 有 | 不匹配 | 记录异常/失败 |
| `NO_04_WITHOUT_CHANGE_DETAIL` | 无 | 无 | — | 记录异常/失败 |

### 转账回溯（TransferRecall）

**触发条件**：转账交易（`TRANS_TYPE_T`）长时间处于"处理中"状态。

**Job 流程**（`TransferRecallJobService`）：
```
1. 按机构参数遍历 → 查询 status=P 的转账记录
2. 查询中信交易状态
   ├─ 处理中/受理成功 → 跳过
   ├─ none（查不到记录） → handleZxStatusNone → 直接解冻 + 交易失败
   └─ 其他 → 执行回溯
3. Redis 分布式锁（payCardCode 维度，30 秒超时）
4. 调用 transferRecallService.transferRecall
```

**Service 核心方法**（`TransferRecallServiceImpl`）：

| 方法 | 职责 |
|---|---|
| `transferRecall` | 转账回溯主入口 |
| `handleNoTransactionDetail` | 无交易明细 → 校验余额匹配 → 退款/解冻 |
| `handleTransactionDetailExists` | 有交易明细 → 标记成功 + 解冻 |
| `handleBalanceMatch` | 余额匹配 → 中信退款 + 收款卡扣减 + 付款卡增加 + 解冻 |
| `handleBalanceMismatch` | 余额不匹配 → 标记失败 |
| `executeZxRecall` | 执行中信退款（调用 `transTransferRecall`） |
| `updateReceiveCardAccount` | 更新收款卡余额（减交易金额） |
| `updatePayCardAccount` | 更新付款卡余额（加交易金额） |
| `unfreezeAccount` | 解冻付款卡冻结金额 |
| `handleZxStatusNone` | 中信查不到记录 → 直接解冻 + 交易失败 |
| `buildSubAccountUpdateReq` | 构建子账户更新请求（含 MAC CAS） |

## 关键规则

### 所有回溯解冻操作

- `frozenType = "UF"`（解冻）
- **不设置** `frozenTransNo`，依赖 `createFrozenDetail` 内部自动生成
- 解冻金额来自 `frozenAmtMap`，key 为 `transNo + accountType`

### 转账回溯的账户更新

`TransferRecallServiceImpl` 使用旧路径 `updateCardSubAccount`，包含 MAC CAS 校验：
- 付款卡：`balance += transAmt`，`realBalance += transAmt`
- 收款卡：`balance -= transAmt`，`realBalance -= transAmt`
- 待释放余额/可提现余额按 cardBin 提现配置处理

### 消费回溯的 04 退款

- 中信成功的 04 交易需要调用 `transConsumeCancel` 进行银行侧退款
- 收款卡和付款卡角色互换（原收款方作为退款付款方）
- 回溯交易号 = 原 `transNo + "transRecall"` 后缀

## 流程图

### 消费回溯流程

```
TransRecallJobService.run()
  ├─ 查询系统参数（机构号列表）
  ├─ 遍历机构 → 查询 status=P 的消费记录
  └─ 逐笔处理 processSingleTransaction
       ├─ 查询子流水（01/02/04）
       ├─ 查询中信交易状态（04 类型）
       ├─ 确认无已存在的 UF 解冻记录
       ├─ 统计 04 成功数量
       ├─ Redis 锁（cardCode）
       └─ 执行回溯
            ├─ 插入 trans_recall_apply_detail_t（status=P）
            ├─ 根据 04 结果分流
            └─ 更新回溯申请状态
```

### 转账回溯流程

```
TransferRecallJobService.run()
  ├─ 查询系统参数（机构号列表）
  ├─ 遍历机构 → 查询 status=P 的转账记录
  └─ 逐笔处理
       ├─ 查询企业信息
       ├─ 确认无已存在的 UF 解冻记录
       ├─ Redis 锁（payCardCode, 30s）
       ├─ 查询中信交易状态
       │   ├─ 处理中 → 跳过
       │   ├─ none → handleZxStatusNone（直接解冻）
       │   └─ 其他 → 调用 transferRecallService
       │        ├─ 有明细 → 标记成功 + 解冻
       │        └─ 无明细 → 校验余额
       │             ├─ 匹配 → 中信退款 + 更新双方余额 + 解冻
       │             └─ 不匹配 → 标记失败
       └─ 更新回溯申请状态
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_recall_apply_detail_t` | 回溯申请详情表，status: P(处理中)/S(成功)/F(失败) |
| `trans_consume_t` | 消费主表（回溯更新 status 字段） |
| `trans_consume_sub_rec_t` | 消费子记录表（回溯更新 recStatus） |
| `trans_transfer_t` | 转账主表（回溯更新 status 字段） |
| `trans_acct_frozen_change_detail_t` | 冻结/解冻变动明细（回溯写入 UF 类型） |
| `trans_acct_change_entry_detail_t` | 异步上账明细表（04 不匹配时写入） |
| `bas_card_sub_account_t` | 子账户表（转账回溯直接更新余额） |

## Redis Key

| Key | 格式 | TTL | 用途 |
|---|---|---|---|
| 消费回溯锁 | `{cardCode}` | 30s | `RedisLockUtils.lockKey`，按付款卡维度加锁 |
| 转账回溯锁 | `{payCardCode}` | 30s | `RedisLockUtils.lockKey`，按付款卡维度加锁 |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `TransRecallService.transRecall` | 内部调用 | 消费回溯（6 种 TransactionType 分流） |
| `TransRecallService.transRecall04` | 内部调用 | 消费回溯 04 退款+解冻 |
| `TransferRecallService.transferRecall` | 内部调用 | 转账回溯主入口 |
| `AbnormalProcessApi.insertTransRecallDetail` | RPC | 插入回溯申请详情 |
| `AbnormalProcessApi.updateTransRecallDetailByTransNo` | RPC | 更新回溯状态（按 transNo） |
| `AbnormalProcessApi.updateTransRecallDetail` | RPC | 更新回溯状态（按 applyDetailId） |
| `FrontTransConsumeFacadeApi.transConsumeCancel` | RPC | 04 消费退款（中信） |
| `FrontTransConsumeFacadeApi.transTransferRecall` | RPC | 转账退款（中信） |
| `FrontTransQueryFacadeApi.queryTransStatusQuery` | RPC | 查询中信交易状态 |
| `TransAccountApi.createFrozenDetail` | RPC | 创建冻结/解冻明细 |
| `TransAccountApi.updateConsumeStatusByConsumeId` | RPC | 更新消费状态 |
| `TransAccountApi.updateTransferStatusByTransferId` | RPC | 更新转账状态 |

## XxlJob 任务参数

```json
{
  "operatorCodes": "ORG001,ORG002",  // 指定机构号，逗号分隔
  "days": 0,                         // 查询天数范围，默认 0（当天）
  "min": 10                          // 查询最近 N 分钟，默认 10
}
```

## 关联 Wiki

- [account-change.md](account-change.md) — 账户变动（MAC CAS、子账户更新）
- [platform-recharge-batch.md](platform-recharge-batch.md) — 平台批量实收（同使用 Redis 锁 + 解冻明细）
