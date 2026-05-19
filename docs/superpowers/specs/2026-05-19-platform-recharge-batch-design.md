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
| 事务边界 | 两个事务：事务1充值2表落地 → 事务2明细+余额 |
| detail 表 | 新增处理状态追踪表，锁卡后同步写入，异步更新状态 |
| 重复处理 | detail 表 unique(transNo) 过滤，同步返回失败流水信息 |
| 补偿机制 | 事务2失败打完整日志 + 人工介入 |
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
  ├─ 查询 detail 表中 status=I 的记录
  ├─ 按 chunk(500笔) 分批处理
  │   每个 chunk:
  │   ├─ 逐条 update detail status → P
  │   ├─ 事务1: 批量 insert trans_recharge_t + trans_recharge_sub_t
  │   │   └─ DB unique constraint 过滤重复，只处理落地成功的
  │   ├─ 事务2: 基于落地成功的记录
  │   │   ├─ 按 txnTime→transNo 排序
  │   │   ├─ 读取平台卡当前余额
  │   │   ├─ 内存计算余额链
  │   │   ├─ 写入 4 张账户变动明细表
  │   │   ├─ 一次性 CAS 更新平台卡 04 余额
  │   │   └─ 成功后写 Redis done (TTL 7天)
  │   ├─ 每条记录成功 → update detail status → S
  │   └─ 每条记录失败 → update detail status → F + remark
  └─ finally 释放平台卡锁
```

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

具体字段以 `RechargeTransAfter` 现有充值口径为准。

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

详细日志模板见需求文档第 11 节。

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
- 事务2失败：充值记录已存在，明细缺失，打日志人工介入
- Redis done 写入失败不影响 DB 已成功事实
