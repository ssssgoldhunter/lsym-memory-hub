# 平台批量实收（Platform Recharge Batch）

## 关联文档
- 设计文档：[docs/specs/2026-05-19-platform-recharge-batch-design.md](../docs/specs/2026-05-19-platform-recharge-batch-design.md)
- 需求文档：[requirements/2026-05-18-platform-recharge-batch.md](../requirements/2026-05-18-platform-recharge-batch.md)

## 源码映射

### consume 模块（核心）

| 类 | 路径 | 职责 |
|---|---|---|
| `PlatformRechargeBatchServiceImpl` | `fund-catering-consume/.../service/impl/PlatformRechargeBatchServiceImpl.java` | 核心实现(1302行)：submit/processAsync/executeTx1/executeTx2/compensate |
| `PlatformRechargeBatchService` | `fund-catering-consume/.../service/PlatformRechargeBatchService.java` | 接口定义 |
| `TransPlatformRechargeBatchDetail` | `fund-catering-consume/.../domain/TransPlatformRechargeBatchDetail.java` | detail 表实体，status: I/P/S/F |
| `TransPlatformRechargeBatchDetailMapper` | `fund-catering-consume/.../mapper/TransPlatformRechargeBatchDetailMapper.java` | Mapper |
| `TransPlatformRechargeBatchDetailServiceImpl` | `fund-catering-consume/.../service/impl/TransPlatformRechargeBatchDetailServiceImpl.java` | detail CRUD |
| `Tx1Result` | `fund-catering-consume/.../service/Tx1Result.java` | 事务1结果对象 |
| `RedisLockUtils` | `fund-catering-consume/.../utils/RedisLockUtils.java` | Redis 锁工具 |
| `TransConsumeController.platformRechargeBatch` | `fund-catering-consume/.../controller/TransConsumeController.java:244` | API 入口 |

### API 层

| 类 | 路径 | 职责 |
|---|---|---|
| `PlatformRechargeBatchReq` | `fund-catering-consume-api/.../request/PlatformRechargeBatchReq.java` | 批量请求 |
| `PlatformRechargeBatchDetailReq` | `fund-catering-consume-api/.../request/PlatformRechargeBatchDetailReq.java` | 明细请求 |
| `PlatformRechargeBatchSubmitRes` | `fund-catering-consume-api/.../response/PlatformRechargeBatchSubmitRes.java` | 返回(status + duplicates) |
| `DuplicateDetail` | `fund-catering-consume-api/.../response/DuplicateDetail.java` | 重复流水信息 |
| `TransConsumeApi.platformRechargeBatch` | `fund-catering-consume-api/.../api/TransConsumeApi.java:118` | Feign 接口 |

### task 模块

| 类 | 路径 | 职责 |
|---|---|---|
| `PlatformRechargeJobService` | `fund-catering-task/.../job/zx/PlatformRechargeJobService.java` | 采集+过滤+分发(改造后) |

## 流程

```
submit() — 同步入口
  ├─ 参数校验 → INVALID
  ├─ 抢平台卡 Redis 锁 (SET NX EX 30min)
  │   └─ 抢不到 → SKIPPED_LOCKED
  ├─ 锁卡后同步写入 detail 表 (status=I)，唯一约束冲突收集到 duplicates
  ├─ taskExecutor.execute() 提交异步任务
  └─ 返回 ACCEPTED + duplicates

processAsync() — 异步处理（锁保护下）
  ├─ 查询 status=I 的 detail 记录
  ├─ 按 chunk(500笔) 分批
  │   ├─ 逐条 update detail status → P
  │   ├─ 事务1: 批量查重 + insert trans_recharge_t + trans_recharge_sub_t
  │   │   └─ 唯一键冲突 → 回滚 → 重新查重 → 重建 chunk
  │   ├─ 事务2: 余额链计算 + 4张账户变动明细表 + CAS 更新余额
  │   │   └─ 失败 → detail status=F + 完整日志 + 人工补偿入口
  │   ├─ 成功 → detail status → S + Redis done (TTL 7天)
  │   └─ 到账通知(MQ异步)
  └─ finally 释放平台卡锁

compensate() — 补偿入口
  └─ 基于已存在充值流水重新执行事务2
```

## 数据库表

| 表名 | 说明 |
|---|---|
| `trans_platform_recharge_batch_detail` | 批次明细追踪表，唯一约束 `uk_trans_no(trans_no)` |
| `trans_recharge_t` | 充值主表（唯一约束 `trans_no`） |
| `trans_recharge_sub_t` | 充值子表 |
| 4张账户变动明细表 | 详见 account-change.md |

## API 接口

| 接口 | 方法 | 说明 |
|---|---|---|
| `/platformRechargeBatch` | POST | 批量提交入口，返回 ACCEPTED/SKIPPED_LOCKED/EMPTY/INVALID/REJECTED |

## Redis Key

| Key | 格式 | TTL | 用途 |
|---|---|---|---|
| 平台卡锁 | `platform_recharge:card_lock:{platformCardCode}` | 30min | consume 入口抢锁 |
| 已完成流水 | `platform_recharge:done:{operatorCode}:{platformCode}:{transNo}` | 7天 | task 预过滤 + consume 写入 |

## 余额链计算

排序：`txnTime ASC → transNo ASC`

```
初始: balance=B0, realBalance=R0

第1笔 A(100分):
  orgAmt=B0, balance=B0+100
  orgRealAmt=R0, realBalance=R0+100

第2笔 B(200分):
  orgAmt=B0+100, balance=B0+300
  orgRealAmt=R0+100, realBalance=R0+300
```

04 账户更新对齐 `RechargeTransAfter` 口径：
- `balance += amount`，`realBalance += amount`（必加）
- `withdrawBalance/waitReleaseBalance` 按 cardBin 提现配置处理

## 关联 Bug / 排查记录
- [bugs/2026-05-26-platform-recharge-card-lock-async-release.md](../bugs/2026-05-26-platform-recharge-card-lock-async-release.md)

## 跨项目迁移记录
- **来源**: lsym (slhy) 项目设计 → mdl 项目实现
- **commit**: `f02ecfcd` — `1.平台实收批量业务迁移+到账通知(MQ异步)`
- **分支**: `20260603_limeng`（mdl 项目）
- **时间**: 2026-06-05
- **差异**: 额外增加了 MQ 异步到账通知（`sendActualReceiptNotify`）
