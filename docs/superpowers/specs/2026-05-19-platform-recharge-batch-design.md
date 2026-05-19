# 平台 03 渠道充值批量高并发上账 — 实现设计

日期：2026-05-19
状态：已确认，待实现
需求文档：`lsym-memory-hub/requirements/2026-05-18-platform-recharge-batch.md`

---

## 一、设计决策

| 项 | 决策 |
|---|---|
| API 位置 | 扩展 `TransConsumeApi`，新增 `platformRechargeBatch` |
| task 实现 | 改造现有 `PlatformRechargeJobService` |
| 异步方式 | 复用 consume `taskExecutor`（execute 模式） |
| 账户处理 | 明细先行 + 一次性 CAS，跟划付同模式 |
| chunk 大小 | 500 笔/事务，平台卡锁覆盖整批 |
| 锁 TTL | 30 分钟，不续期 |
| 事务边界 | 两个事务：事务1充值2表落地 → 事务2账户明细+余额；这是业务要求，事务2失败需完整日志和人工补偿入口 |
| detail 表 | 新增处理状态追踪表，锁卡后同步写入，异步更新状态 |
| 重复处理 | detail 表 unique(transNo) 做入口去重；trans_recharge_t.trans_no 唯一约束继续作为资金流水最终防重 |
| 唯一键冲突处理 | 充值2表入库前批量查重过滤；若仍触发唯一键冲突，当前 chunk 事务1回滚，重新查重、重建本 chunk 后再处理 |
| 补偿机制 | 事务2失败时充值记录已存在但账户明细/余额未完成，必须写失败状态、完整日志，并预留人工补偿/重放入口 |
| 明细表 | 4 张账户变动明细表全写 |
| 金额 | front 返回元，`movePointRight(2)` 转分 |
| 实现方案 | 方案 B：单卡顺序流程 |

---

## 二、涉及模块

- `fund-catering-task`：采集和过滤
- `fund-catering-consume`：批量上账核心逻辑
- `fund-catering-base`：账户查询和更新

---

## 三、task 端改造（PlatformRechargeJobService）

### 3.1 流程

1. 读取 XXL-JOB 参数，生成 `batchNo`
2. 查询 `platformRecharge` 参数配置，遍历每个 operatorCode/platformCode
3. 调用 front 分页查询平台流水（`FrontTransQueryFacadeApi.queryPlatformTransPages`）
4. 过滤 `TRANS_TP = 03`，校验基础字段（transNo 非空、金额 > 0、交易时间非空）
5. Redis done 预过滤已完成流水
6. 组装 `PlatformRechargeBatchReq`，Feign 调用 consume 新接口
7. 记录提交结果，不等待 consume 异步完成

### 3.2 关键变化

- 去掉逐笔调用 `transConsumeApi.rechargeTrans`
- 去掉 `sleep(500)`
- 去掉逐笔查询企业卡
- 去掉写 done 标记（改由 consume 负责）

### 3.3 金额处理

front 返回 `transAmt` 单位为元，转为分：

```java
BigDecimal yuan = new BigDecimal(transAmt);
long cents = yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
```

---

## 四、consume API 层

### 4.1 接口定义

在 `TransConsumeApi` 新增：

```java
@PostMapping(value = PATH_PREFIX + "/platformRechargeBatch")
DefaultResult<PlatformRechargeBatchSubmitRes> platformRechargeBatch(
    @RequestBody PlatformRechargeBatchReq request);
```

### 4.2 请求对象

**PlatformRechargeBatchReq：**

| 字段 | 类型 | 说明 |
|---|---|---|
| batchNo | String | 批次号 |
| operatorCode | String | 运营商编码 |
| orgCode | String | 机构编码 |
| firstOrgCode | String | 一级机构 |
| platformCode | String | 平台编码 |
| transDate | String | 交易日期 |
| platformCardCode | String | 平台卡号 |
| platformMerchantId | String | 平台商户号 |
| terminalCode | String | 终端号 |
| accountType | String | 子账户类型（固定 04） |
| details | List | 明细列表 |

**PlatformRechargeBatchDetailReq：**

| 字段 | 类型 | 说明 |
|---|---|---|
| transNo | String | 流水号 |
| amount | String | 金额（分） |
| txnTime | String | 交易时间 |
| transDate | String | 交易日期 |
| transTime | String | 交易时间 |
| payAccount | String | 付款账号 |
| payAccountName | String | 付款户名 |
| summaryMsg | String | 摘要 |
| bankEAccountId | String | 银行电子账户 |
| sourceTransType | String | 来源类型（03） |
| rawJson | String | 原始数据（可选） |

### 4.3 返回对象

**PlatformRechargeBatchSubmitRes：**

| 字段 | 类型 | 说明 |
|---|---|---|
| batchNo | String | 批次号 |
| status | String | ACCEPTED/SKIPPED_LOCKED/EMPTY/INVALID/REJECTED |
| message | String | 说明信息 |
| receivedCount | int | 接收总数 |
| submitCount | int | 提交处理数 |
| redisDoneSkippedCount | int | Redis done 跳过数 |
| duplicates | List\<DuplicateDetail\> | 重复流水信息（transNo + 失败原因） |

---

## 五、新增 detail 表

### 5.1 表名

`trans_platform_recharge_batch_detail`

### 5.2 字段

| 字段 | 列名 | 类型 | 说明 |
|---|---|---|---|
| detailId | detail_id | bigint PK | 自增主键 |
| batchNo | batch_no | varchar(64) | 批次号 |
| transNo | trans_no | varchar(64) | 流水号（唯一约束） |
| transAmt | trans_amt | bigint | 金额（分） |
| platformCardCode | platform_card_code | varchar(64) | 平台卡号 |
| status | status | varchar(4) | I(初始化)/P(处理中)/S(成功)/F(失败) |
| remark | remark | varchar(512) | 失败原因 |
| operatorCode | operator_code | varchar(64) | 运营商编码 |
| orgCode | org_code | varchar(64) | 机构编码 |
| platformCode | platform_code | varchar(64) | 平台编码 |
| transDate | trans_date | varchar(16) | 交易日期 |
| txnTime | txn_time | varchar(32) | 交易时间 |
| createTime | create_time | datetime | 创建时间 |
| updateTime | update_time | datetime | 更新时间 |

### 5.3 约束

```sql
UNIQUE KEY uk_trans_no (trans_no)
```

### 5.4 角色和使用时机

**同步阶段（consume 入口，锁卡后）：**
- 遍历每条流水，尝试 insert detail (status=I)
- trans_no 重复（唯一约束冲突）：跳过，收集失败 transNo + 原因
- 同步返回中包含重复流水信息
- 日志打印重复流水

**异步阶段：**
- 对每条 detail (status=I) 逐条更新状态
- 处理前 update status → P
- 成功后 update status → S
- 失败后 update status → F + 记录失败原因到 remark

注意：

- detail 表的 `unique(trans_no)` 是本需求要求的入口去重机制。
- 不能因为 detail 表已去重就取消 `trans_recharge_t.trans_no` 唯一约束。
- 不能只依赖 Redis done 或 detail 表判断资金流水是否已完成；充值表仍要批量查重。
- 异步任务不依赖 `batchNo` 作为唯一执行范围。提交异步任务时应携带本次同步插入成功的 `detailId/transNo` 列表；异步阶段只处理这批 ID 中 `status=I` 的记录。`batchNo` 主要用于日志追踪和人工排查。

---

## 六、consume 服务核心逻辑

### 6.1 类结构

```
PlatformRechargeBatchService (接口)
PlatformRechargeBatchServiceImpl (实现)
```

### 6.2 整体流程

```
submit() — 同步入口
  ├─ 参数校验 → INVALID
  ├─ 抢平台卡 Redis 锁 (SET NX EX 30min)
  │   └─ 抢不到 → SKIPPED_LOCKED
  ├─ 锁卡后同步写入 detail 表 (status=I)
  │   └─ 唯一约束冲突的记录收集到 duplicates 列表
  ├─ taskExecutor.execute() 提交异步任务
  │   └─ 提交失败 → 释放锁 → REJECTED
  └─ 返回 ACCEPTED + duplicates

processAsync() — 异步处理（锁保护下）
  ├─ 根据本次提交成功的 detailId/transNo 列表查询 status=I 的记录
  ├─ 按 chunk(500笔) 分批处理
  │   每个 chunk:
  │   ├─ 逐条 update detail status → P
  │   ├─ 事务1: 批量 insert trans_recharge_t + trans_recharge_sub_t
  │   │   ├─ 入库前批量查询 trans_recharge_t 已存在 transNo，过滤重复
  │   │   ├─ 过滤后的流水才允许插入充值2表
  │   │   └─ 若仍发生唯一键冲突，事务1回滚，重新查重、重建本 chunk 再处理
  │   ├─ 事务2: 基于落地成功的记录
  │   │   ├─ 按 txnTime→transNo 排序
  │   │   ├─ 读取平台卡当前余额
  │   │   ├─ 内存计算余额链
  │   │   ├─ 写入 4 张账户变动明细表
  │   │   ├─ 按 RechargeTransAfter 口径一次性 CAS 更新平台卡 04 余额
  │   │   └─ 成功后写 Redis done (TTL 7天)
  │   ├─ 每条记录成功 → update detail status → S
  │   └─ 每条记录失败 → update detail status → F + remark
  └─ finally 释放平台卡锁
```

### 6.3 事务1和事务2的边界说明

本设计按业务要求保留两个事务：

- 事务1：充值主表 `trans_recharge_t` 和充值子表 `trans_recharge_sub_t` 落地。
- 事务2：4 张账户变动明细表写入，并一次性更新平台卡 04 账户余额。

风险和约束：

- 事务1成功、事务2失败时，会形成“充值流水已存在，但账户明细/余额未完成”的中间状态。
- 这种状态不能静默吞掉，必须将对应 detail 更新为 `F`，`remark` 写明事务2失败原因。
- 必须打印完整错误日志，包含 `batchNo`、`detailId`、`transNo`、`platformCardCode`、`chunkIndex`、事务阶段、异常堆栈。
- 必须预留人工补偿或重放入口：以 detail 表中 `status=F` 且 remark 标记为事务2失败的记录为依据，重新执行事务2。
- 补偿执行时不能重新插入充值2表，只能基于已存在充值流水重新生成账户变动明细并更新账户余额。

### 6.4 trans_recharge_t 查重和唯一键冲突处理

虽然 detail 表有 `unique(trans_no)`，事务1前仍必须批量查询 `trans_recharge_t`：

1. 查询当前 chunk 的 `transNo` 是否已存在于 `trans_recharge_t`。
2. 已存在的流水更新 detail 为跳过/失败状态，并记录原因：`充值流水已存在`。
3. 不存在的流水进入充值2表插入。
4. 如果插入时仍出现 `trans_no` 唯一键冲突，说明存在并发或历史脏数据：
   - 当前 chunk 的事务1必须回滚。
   - 重新查询 `trans_recharge_t`。
   - 过滤已存在流水。
   - 使用剩余流水重建 chunk。
   - 重新执行事务1。

禁止把数据库唯一键冲突当成“部分成功过滤器”继续往下跑。

---

## 七、余额链计算

在平台卡锁内，读取 04 子账户当前余额作为期初。

### 7.1 排序规则

参与上账的流水按 `txnTime` 升序，再按 `transNo` 升序。`txnTime` 为空的流水判定为非法，不进入上账。

### 7.2 计算规则

```
初始: balance=B0, realBalance=R0

第1笔 A(100分):
  orgAmt=B0, balance=B0+100
  orgRealAmt=R0, realBalance=R0+100

第2笔 B(200分):
  orgAmt=B0+100, balance=B0+300
  orgRealAmt=R0+100, realBalance=R0+300
```

### 7.3 chunk 间衔接

第 N 个 chunk 的期初 = 第 N-1 个 chunk 更新后的余额。锁内顺序执行，天然连续。

### 7.4 一次性余额更新

本批成功流水总额 `successAmount`，平台卡 04 账户：
- balance += successAmount
- realBalance += successAmount
- withdrawBalance / waitReleaseBalance 按 `RechargeTransAfter` 现有 04 充值口径处理

`RechargeTransAfter` 当前 04 账户更新口径：

- `balance += amount`
- `realBalance += amount`
- 如果 `cardBin.withdraw == 1` 且 `withdrawDays <= 0`：`withdrawBalance += amount`，`waitReleaseBalance += 0`
- 如果 `cardBin.withdraw == 1` 且 `withdrawDays > 0`：`waitReleaseBalance += amount`，`withdrawBalance += 0`
- 如果不支持提现：`withdrawBalance += 0`，`waitReleaseBalance += 0`
- `frozenAmt += 0`

批量实现必须先查询平台卡对应 `cardBin` 提现配置，并按上述规则一次性汇总更新。

---

## 八、新增对象清单

### 8.1 API 层

- `PlatformRechargeBatchReq`
- `PlatformRechargeBatchDetailReq`
- `PlatformRechargeBatchSubmitRes`
- `DuplicateDetail`（重复流水信息）
- `TransConsumeApi` 新增方法
- consume Controller 新增端点

### 8.2 服务层

- `PlatformRechargeBatchService`（接口）
- `PlatformRechargeBatchServiceImpl`（实现）

### 8.3 数据层

- `TransPlatformRechargeBatchDetail`（实体）
- 对应 Mapper 和 Service

### 8.4 修改对象

- `PlatformRechargeJobService`（task 端改造）

---

## 九、Redis Key 规范

| Key | 格式 | TTL | 用途 |
|---|---|---|---|
| 平台卡锁 | `platform_recharge:card_lock:{platformCardCode}` | 30 分钟 | consume 入口抢锁 |
| 已完成流水 | `platform_recharge:done:{operatorCode}:{platformCode}:{transNo}` | 7 天 | task 预过滤 + consume 写入 |

---

## 十、日志要求

所有日志必须包含 `batchNo`、`operatorCode`、`platformCode`、`platformCardCode` 等可追踪字段。

异常日志必须打完整堆栈：
```java
log.error("... error", e);
```

禁止只打 `log.error("error: {}", e.getMessage())`。

### 10.1 submit 日志

```text
platformRechargeBatch:submit:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, receivedCount={}
platformRechargeBatch:submit:validateFailed batchNo={}, reason={}
platformRechargeBatch:lock:start batchNo={}, lockKey={}, platformCardCode={}
platformRechargeBatch:lock:success batchNo={}, lockKey={}, ttlSeconds={}
platformRechargeBatch:lock:skipped batchNo={}, lockKey={}, platformCardCode={}, message=card locked
platformRechargeBatch:detailInsert:start batchNo={}, receivedCount={}
platformRechargeBatch:detailInsert:itemDuplicate batchNo={}, transNo={}, reason=detail trans_no duplicate
platformRechargeBatch:detailInsert:success batchNo={}, insertedCount={}, duplicateCount={}, costMs={}
platformRechargeBatch:asyncSubmit:success batchNo={}, insertedCount={}
platformRechargeBatch:asyncSubmit:failed batchNo={}, error={}
platformRechargeBatch:submit:end batchNo={}, status={}, message={}, costMs={}
```

### 10.2 异步处理日志

```text
platformRechargeBatch:process:start batchNo={}, platformCardCode={}, detailCount={}
platformRechargeBatch:chunk:start batchNo={}, chunkIndex={}, chunkSize={}, firstTransNo={}, lastTransNo={}
platformRechargeBatch:chunk:statusP:start batchNo={}, chunkIndex={}, count={}
platformRechargeBatch:chunk:statusP:success batchNo={}, chunkIndex={}, count={}
platformRechargeBatch:chunk:rechargeExistsFilter:start batchNo={}, chunkIndex={}, count={}
platformRechargeBatch:chunk:rechargeExistsFilter:success batchNo={}, chunkIndex={}, existsCount={}, pendingCount={}
platformRechargeBatch:chunk:tx1:start batchNo={}, chunkIndex={}, rechargeCount={}, rechargeSubCount={}
platformRechargeBatch:chunk:tx1:success batchNo={}, chunkIndex={}, insertedCount={}, costMs={}
platformRechargeBatch:chunk:tx1:uniqueConflict batchNo={}, chunkIndex={}, error={}, action=rollback_reload_rebuild
platformRechargeBatch:chunk:tx1:rollback batchNo={}, chunkIndex={}, reason={}, error={}
platformRechargeBatch:chunk:accountSnapshot batchNo={}, chunkIndex={}, balance={}, realBalance={}, withdrawBalance={}, waitReleaseBalance={}, totalRealBalance={}
platformRechargeBatch:chunk:buildChain:item batchNo={}, chunkIndex={}, index={}, transNo={}, amount={}, orgAmt={}, balance={}, orgRealAmt={}, realBalance={}
platformRechargeBatch:chunk:buildChain:success batchNo={}, chunkIndex={}, count={}, totalAmount={}
platformRechargeBatch:chunk:tx2:start batchNo={}, chunkIndex={}, accountDetailCount={}, totalAmount={}
platformRechargeBatch:chunk:tx2:success batchNo={}, chunkIndex={}, accountDetailCount={}, accountUpdateAmount={}, costMs={}
platformRechargeBatch:chunk:tx2:failed batchNo={}, chunkIndex={}, reason={}, needManualCompensate=true
platformRechargeBatch:markDone:start batchNo={}, chunkIndex={}, count={}, ttlDays=7
platformRechargeBatch:markDone:success batchNo={}, chunkIndex={}, count={}, costMs={}
platformRechargeBatch:chunk:end batchNo={}, chunkIndex={}, successCount={}, failedCount={}, skippedCount={}, costMs={}
platformRechargeBatch:process:end batchNo={}, status={}, successCount={}, failedCount={}, skippedCount={}, totalAmount={}, costMs={}
platformRechargeBatch:unlock:success batchNo={}, lockKey={}
platformRechargeBatch:unlock:failed batchNo={}, lockKey={}, error={}
```

### 10.3 补偿日志

```text
platformRechargeBatch:compensate:start detailId={}, transNo={}, platformCardCode={}, reason={}
platformRechargeBatch:compensate:rechargeExists detailId={}, transNo={}, exists={}
platformRechargeBatch:compensate:tx2:start detailId={}, transNo={}
platformRechargeBatch:compensate:tx2:success detailId={}, transNo={}
platformRechargeBatch:compensate:failed detailId={}, transNo={}, error={}
```

---

## 十一、验收标准

### 功能验收

- task 采集 03 渠道充值流水，Redis done 预过滤
- consume 抢不到锁返回 SKIPPED_LOCKED
- detail 表记录每条流水处理状态
- 重复 transNo 不参与上账，同步返回失败信息
- 充值2表落地成功后，4 张明细表写入
- 平台卡 04 余额按成功总金额增加
- Redis done 写入，TTL 7 天
- 04 账户更新必须对齐 `RechargeTransAfter`：`balance/realBalance` 必加，`withdrawBalance/waitReleaseBalance` 按 cardBin 提现配置处理

### 连续性验收

初始余额 10000，三笔 A(100) B(200) C(300)：

```
A orgAmt=10000 balance=10100
B orgAmt=10100 balance=10300
C orgAmt=10300 balance=10600
```

B 被过滤时：

```
A orgAmt=10000 balance=10100
C orgAmt=10100 balance=10400
```

### 异常验收

- 事务1失败：chunk 内充值2表回滚
- 事务1唯一键冲突：chunk 回滚，重新查询 `trans_recharge_t`，过滤后重建 chunk 再处理
- 事务2失败：充值记录已存在，账户明细/余额未完成，detail 标记失败，打完整日志，并可通过补偿入口重放事务2
- Redis done 写入失败不影响 DB 已成功事实
