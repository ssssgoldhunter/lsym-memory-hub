# 批量上账业务实现说明

## 概述

本文档描述了批量上账业务系统的完整实现，包括接口c（触发上账任务）和接口d（高级查询接口）的实现细节。

## 系统架构

### 核心组件

1. **TransTransferTiBatchBusinessServiceImpl**: 批量上账业务服务实现类
2. **TransTransferTiBatchController**: Spring API控制器
3. **BatchDetailSummary**: 批次明细汇总信息类
4. **TransTransferTiBatchDetailAdvancedQueryReq**: 高级查询请求类

### 线程池配置

- 使用Spring的`@Async("taskExecutor")`注解进行异步处理
- 线程池配置参考`ScheduledConfig`中的`taskExecutor`
- 支持单日300万条数据的并发处理

## 接口实现

### 接口c: 触发上账任务 (triggerBatchTask)

#### 业务流程

1. **Redis锁处理**
   - 获取批次级别的Redis锁，超时时间10分钟
   - 防止同一批次被重复处理

2. **批次任务校验**
   - 获取并校验批次任务状态
   - 状态必须为"I"（初始化），否则返回对应错误信息

3. **数据一致性校验**
   - 校验明细数据总量和总金额与批次任务信息是否一致
   - 校验所有明细状态是否为"I"

4. **状态更新**
   - 更新批次状态为"P"（进行中）

5. **多线程并发处理**
   - 按付款卡号分组处理
   - 每个付款卡使用独立的Redis锁（30分钟超时）
   - 收款卡上账和付款卡下账分别处理

6. **完成处理**
   - 更新批次状态为"S"（成功）
   - 释放所有Redis锁

#### 并发处理策略

```
批次级别 (Redis锁: 10分钟)
├── 付款卡1 (Redis锁: 30分钟)
│   ├── 收款卡A (Redis锁: 10分钟) - 上账处理
│   ├── 收款卡B (Redis锁: 10分钟) - 上账处理
│   └── 付款卡1 - 下账处理
├── 付款卡2 (Redis锁: 30分钟)
│   ├── 收款卡C (Redis锁: 10分钟) - 上账处理
│   └── 付款卡2 - 下账处理
└── ...
```

#### 关键特性

- **分层锁机制**: 批次锁 → 付款卡锁 → 收款卡锁
- **异常处理**: 完善的异常处理和状态回滚
- **资源管理**: 自动释放Redis锁，防止死锁
- **状态跟踪**: 详细的处理状态记录和更新

### 接口d: 高级查询接口 (queryBatchDetailAdvanced)

#### 查询条件

- **batchNo**: 批次号（必填）
- **transNo**: 交易流水号（可选）
- **receiveCardCode**: 收款卡号（可选）
- **payCardCode**: 付款卡号（可选）
- **分页参数**: current, pageSize

#### 实现特点

- 支持多条件组合查询
- 基于现有分页查询服务扩展
- 完整的参数校验和异常处理

## 技术实现

### Redis锁服务

```java
@Autowired
private RedisLockService redisLockService;

// 获取锁
boolean lockAcquired = redisLockService.tryLock(lockKey, lockValue, timeout, timeUnit);

// 释放锁
redisLockService.releaseLock(lockKey, lockValue);
```

### 异步处理

```java
@Async("taskExecutor")
public void processBatchDataAsync(List<ScTransferTiReq> batchList, TransTransferTiBatchQueryRes batchRes) {
    // 异步处理逻辑
}
```

### 并发控制

```java
CountDownLatch latch = new CountDownLatch(totalCount);
CompletableFuture.runAsync(() -> {
    try {
        // 处理逻辑
    } finally {
        latch.countDown();
    }
}, taskExecutor);
latch.await(); // 等待所有任务完成
```

## 错误处理

### 错误码定义

- `BATCH_STATUS_INVALID_ERR`: 批次状态不正确
- `BATCH_DATA_MISMATCH_ERR`: 批次数据不匹配
- `BATCH_NOT_FOUND_ERR`: 批次任务不存在
- `SYSTEM_BUSY_ERR`: 系统繁忙

### 异常处理策略

1. **业务异常**: 抛出BaseException，包含具体错误信息
2. **系统异常**: 包装为BaseException，记录详细日志
3. **状态回滚**: 异常发生时自动更新批次状态为失败
4. **资源清理**: finally块中确保Redis锁被释放

## 性能优化

### 分批处理

- 每批1000条数据进行处理
- 避免单次处理数据量过大

### 并发优化

- 付款卡级别并发处理
- 收款卡级别并发处理
- 使用CountDownLatch控制并发数量

### 数据库优化

- 分页查询避免一次性加载大量数据
- 批量更新减少数据库交互次数

## 监控和日志

### 日志记录

- 详细的处理过程日志
- 异常情况的完整记录
- 性能指标监控

### 状态跟踪

- 批次处理状态实时更新
- 明细处理状态详细记录
- 处理结果和失败原因记录

## 部署和配置

### 依赖服务

- Redis服务（分布式锁）
- 数据库服务（批次和明细数据）
- 上账/下账业务服务

### 配置参数

- Redis锁超时时间
- 线程池大小
- 分页查询大小

## 扩展性

### 水平扩展

- 支持多实例部署
- Redis锁确保任务不重复执行

### 业务扩展

- 支持不同类型的上账业务
- 可配置的处理策略
- 插件化的业务逻辑

## 总结

本实现提供了完整的批量上账业务解决方案，具备以下特点：

1. **高并发**: 支持大规模数据的并发处理
2. **高可靠**: 完善的异常处理和状态管理
3. **高扩展**: 模块化设计，易于扩展和维护
4. **高性能**: 优化的并发策略和资源管理
5. **易监控**: 详细的日志记录和状态跟踪

该实现能够满足单日300万条数据的处理需求，为批量上账业务提供了稳定可靠的技术支撑。
