# fund-catering 框架蓝本

> 本文档记录 `fund-catering` 项目的核心框架设计，作为后续项目的蓝本参考。

---

## 一、项目架构概览

### 1.1 项目模块

```
fund-catering/
├── fund-catering-consume/      # 消费服务（核心业务处理）
├── fund-catering-front/        # 前端路由服务（平台对接）
├── fund-catering-task/         # 定时任务服务（任务调度）
├── fund-catering-base/         # 基础服务
├── fund-catering-report/       # 报表服务
├── fund-catering-management/   # 管理服务
└── fund-catering-web/          # Web 服务
```

### 1.2 核心框架组件

| 项目 | 核心框架 | 主要用途 |
|------|---------|---------|
| **fund-catering-consume** | LiteFlow 流程编排 | 业务处理与查询 |
| **fund-catering-front** | 路由框架 + RocketMQ | 平台路由与消息通知 |
| **fund-catering-task** | XXL-Job | 定时任务调度 |

---

## 二、fund-catering-consume 框架（业务处理核心）

### 2.1 框架特点

- **流程编排**: LiteFlow 实现业务流程可视化、可配置
- **双模式**: 业务处理（Transaction）和业务查询（Query）分离
- **上下文隔离**: TransSlot 和 QuerySlot 分离，共享 BaseSlot

### 2.2 核心设计

#### 2.2.1 业务处理模式（Transaction）

```
Controller → Service（加锁+MAC）→ LiteFlow → TransSlot → 组件 → 写库
```

**特点**:
- 需要分布式锁（Redis）
- 需要 MAC 校验
- 写数据库操作
- 事务保证

#### 2.2.2 业务查询模式（Query）

```
Controller → QueryService → LiteFlow → QuerySlot → 组件 → 读库
```

**特点**:
- 无需分布式锁
- 无需 MAC 校验
- 只读操作
- 简单流程

### 2.3 关键文件

- **流程配置**: `liteflow/consume.el.xml`（处理）、`liteflow/query.el.xml`（查询）
- **上下文**: `TransSlot.java`、`QuerySlot.java`、`BaseSlot.java`
- **组件目录**: `flow/component/trans/`、`flow/component/query/`

### 2.4 适用场景

- 复杂的业务流程编排
- 需要可视化流程配置
- 业务处理与查询分离
- 多步骤、多分支的业务流程

---

## 三、fund-catering-front 框架（路由与消息）

### 3.1 路由框架设计

#### 3.1.1 核心组件

- **Router（路由器）**: 实现 `BeanPostProcessor`，自动扫描注册 Handle
- **Handle（处理器接口）**: 定义业务处理接口
- **Handle 实现**: 具体平台的业务实现

#### 3.1.2 路由流程

```
Service → Router.getHandle() → Handle.process() → 平台接口
```

#### 3.1.3 路由键规则

- **单维度**: `platformCode`（如 `"ZX"`, `"PA"`）
- **双维度**: `platformCode + "_" + accountType`（如 `"ZX_04"`）

### 3.2 RocketMQ 接入

#### 3.2.1 消息发送

- **普通消息**: `sendTopicMessage()`
- **延迟消息**: `sendTopicMessageDelay()`
- **重发消息**: `sendRendTopicMessageDelay()`

#### 3.2.2 消息消费

- **消费者基类**: `RocketMqConsumerListener<T>`
- **消息重试**: Redis 控制重试次数
- **消息历史**: 保存到数据库

### 3.3 关键文件

- **路由服务**: `service/router/*Router.java`
- **Handle 接口**: `handle/*Handle.java`
- **Handle 实现**: `handle/impl/{platform}/*Handle.java`
- **消息服务**: `service/impl/MessageSendServiceImpl.java`
- **消息消费者**: `handle/impl/message/*MessageConsumeHandle.java`

### 3.4 适用场景

- 多平台对接
- 需要动态路由
- 异步消息通知
- 平台实现解耦

---

## 四、fund-catering-task 框架（任务调度）

### 4.1 XXL-Job 接入

#### 4.1.1 任务定义

```java
@XxlJob("jobHandlerName")
public void run() throws Exception {
    String jobParam = XxlJobHelper.getJobParam();
    // 业务逻辑
}
```

#### 4.1.2 工具类

- `XxlJobHelper.getJobParam()` - 获取任务参数
- `XxlJobHelper.log()` - 记录日志
- `XxlJobHelper.handleSuccess()` - 标记成功
- `XxlJobHelper.handleFail()` - 标记失败

### 4.2 任务类型

- **单任务模式**: 一个任务处理一个业务
- **批量任务模式**: 一个任务处理多个配置项
- **分页查询模式**: 分页查询并处理数据

### 4.3 关键文件

- **任务类**: `job/*JobService.java`
- **路由服务**: `service/router/AccountCheckRouter.java`
- **配置类**: `config/ScheduledConfig.java`

### 4.4 适用场景

- 定时任务调度
- 批量数据处理
- 定时对账、汇总等
- 需要任务监控和管理

---

## 五、框架组合使用

### 5.1 典型业务流程

```
外部请求
  ↓
fund-catering-front（路由到对应平台）
  ↓
fund-catering-consume（LiteFlow 流程处理）
  ↓
数据库操作
  ↓
fund-catering-front（发送 RocketMQ 消息）
  ↓
消息消费者（HTTP 回调）
  ↓
fund-catering-task（定时任务处理后续逻辑）
```

### 5.2 框架选择指南

| 需求 | 推荐框架 | 说明 |
|------|---------|------|
| **复杂业务流程** | LiteFlow | 流程可视化、可配置 |
| **多平台对接** | 路由框架 | 动态路由、平台解耦 |
| **异步消息** | RocketMQ | 消息队列、可靠传输 |
| **定时任务** | XXL-Job | 任务调度、监控管理 |
| **业务处理** | LiteFlow + TransSlot | 写操作、事务保证 |
| **业务查询** | LiteFlow + QuerySlot | 读操作、简单流程 |

---

## 六、框架设计原则

### 6.1 单一职责

- **consume**: 专注于业务处理
- **front**: 专注于平台对接和路由
- **task**: 专注于定时任务

### 6.2 开闭原则

- **路由框架**: 新增平台只需实现 Handle 接口
- **LiteFlow**: 新增流程只需配置 XML
- **XXL-Job**: 新增任务只需添加 Job 类

### 6.3 依赖倒置

- **Router**: 依赖 Handle 接口，不依赖具体实现
- **Service**: 依赖 Router 接口，不依赖具体平台

### 6.4 接口隔离

- **Handle 接口**: 每个业务有独立的 Handle 接口
- **Slot 分离**: TransSlot 和 QuerySlot 分离

---

## 七、最佳实践

### 7.1 LiteFlow 使用

1. **流程设计**: 先设计流程，再实现组件
2. **组件粒度**: 组件职责单一，粒度适中
3. **上下文传递**: 通过 Slot 传递数据，避免全局变量
4. **异常处理**: 在组件中处理异常，不要抛出到流程外

### 7.2 路由框架使用

1. **Handle 命名**: 遵循 `{平台}{业务}Handle` 规范
2. **路由键设计**: 考虑单维度和双维度路由
3. **平台扩展**: 新增平台时实现所有相关 Handle
4. **抽象类**: 使用 AbstractHandle 减少重复代码

### 7.3 RocketMQ 使用

1. **消息格式**: 统一消息格式，包含 traceId
2. **消息历史**: 所有消息保存历史记录
3. **重试机制**: 实现完善的重试机制
4. **异步发送**: 使用异步发送提高性能

### 7.4 XXL-Job 使用

1. **任务命名**: JobHandler 名称清晰明确
2. **参数格式**: 使用 JSON 格式传递参数
3. **日志记录**: 使用 XxlJobHelper.log() 记录关键日志
4. **异常处理**: 捕获异常并标记任务失败

---

## 八、扩展指南

### 8.1 新增业务处理流程

1. 在 `flow/component/trans/{业务}/` 下创建组件
2. 在 `consume.el.xml` 中定义流程链
3. 在 `FlowChainEnums` 中添加枚举
4. 在 Service 中调用流程

### 8.2 新增平台路由

1. 在 `handle/impl/{平台}/` 下实现 Handle
2. Router 自动扫描注册
3. 在 Service 中使用 Router 获取 Handle

### 8.3 新增定时任务

1. 在 `job/` 下创建 JobService 类
2. 使用 `@XxlJob` 注解定义任务
3. 在 XXL-Job 管理界面配置任务

### 8.4 新增消息类型

1. 在 `MessageTypeTopicEnum` 中添加枚举
2. 创建对应的消费者 Handle
3. 实现 `RocketMqConsumerListener` 接口

---

## 九、技术栈总结

### 9.1 核心技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 17 | 开发语言 |
| **Spring Boot** | 2.x | 应用框架 |
| **LiteFlow** | 最新版 | 流程编排 |
| **RocketMQ** | 通过 starter | 消息队列 |
| **XXL-Job** | 通过 starter | 任务调度 |
| **MyBatis Plus** | 最新版 | ORM 框架 |
| **Redis** | - | 缓存、分布式锁 |
| **Nacos** | - | 配置中心 |

### 9.2 Starter 依赖

- `starter-rocketMq` - RocketMQ 启动器
- `starter-xxljob` - XXL-Job 启动器
- `starter-mybatisplus` - MyBatis Plus 启动器
- `starter-redis` - Redis 启动器
- `starter-nacos` - Nacos 配置中心启动器

---

## 十、项目参考

### 10.1 文档位置

- **consume 项目总结**: `fund-catering-consume/PROJECT_SUMMARY.md`
- **consume 框架结构**: `fund-catering-consume/FRAMEWORK_STRUCTURE.md`
- **front 项目总结**: `fund-catering-front/PROJECT_SUMMARY.md`
- **task 项目总结**: `fund-catering-task/PROJECT_SUMMARY.md`

### 10.2 关键代码位置

- **LiteFlow 流程**: `fund-catering-consume-service/src/main/resources/liteflow/`
- **路由框架**: `fund-catering-front-service/src/main/java/.../router/`
- **Handle 实现**: `fund-catering-front-service/src/main/java/.../handle/`
- **XXL-Job 任务**: `fund-catering-task/src/main/java/.../job/`

---

**文档生成时间**: 2026-01-28  
**框架版本**: fund-catering 1.0-SNAPSHOT  
**适用项目**: 所有基于 fund-catering 框架的新项目
