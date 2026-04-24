# 微服务架构模块文档

> 记录从 fund-catering 单体拆分出的新微服务模块
> **更新日期**: 2026-04-18

---

## 一、架构总览

项目正在从 `fund-catering` 单体架构向 **Spring Cloud 微服务架构** 转型。
当前状态：**混合模式**（fund-catering 仍在使用，新功能优先在新微服务中开发）。

### 技术基础

| 组件 | 版本 |
|------|------|
| Spring Boot | 3.2.4 |
| Spring Cloud | 2023.0.1 |
| Spring Alibaba | 2023.0.1.0 |
| Java | 17 |

---

## 二、各微服务详情

### 1. auth-service（认证授权中心）

| 属性 | 值 |
|------|------|
| 主类 | `com.chinaums.slhy.auth.AuthApplication` |
| 核心技术 | Spring Security + Nacos |
| 职责 | 用户认证、授权、Token管理 |

### 2. gateway-service（API网关）

| 属性 | 值 |
|------|------|
| 核心技术 | Spring Cloud Gateway |
| 职责 | 统一入口、负载均衡、请求路由、限流 |
| 扩展 | 验证码服务（Kaptcha）、API文档（Springdoc） |

### 3. system-service（主Web服务入口）

| 属性 | 值 |
|------|------|
| 主类 | `com.chinaums.slhy.system.SystemApplication` |
| 核心技术 | MapStruct + EasyExcel + 美团商企SDK |
| 职责 | 整合 fund-catering API + 系统管理功能 |

#### 重要子模块（NPK）

| 组件 | 路径 | 说明 |
|------|------|------|
| NpkMchntController | `.../catering/controller/NpkMchntController.java` | NPK商户管理 |
| NpkStoreController | `.../catering/controller/NpkStoreController.java` | NPK门店管理 |
| NpkBusiItemController | `.../catering/controller/NpkBusiItemController.java` | NPK业务项目管理 |
| StoreRegisterController | `.../storeRegister/controller/StoreRegisterController.java` | 门店注册 |

#### 其他模块

| 组件 | 说明 |
|------|------|
| ReconcileResultController | 对账结果管理 |
| BaseAccountFacadeController | 基础账户门面（61+方法） |
| TransAccountController | 交易账户管理 |

### 4. db-service（数据库服务）

| 属性 | 值 |
|------|------|
| 核心技术 | ShardingSphere + MySQL + Druid + Spring Data |
| 职责 | 分库分表、数据库统计 |

### 5. routing-service（支付路由）

| 属性 | 值 |
|------|------|
| 子模块数 | 8个 |
| 职责 | 多渠道支付路由分发 |

#### 路由子模块

| 子模块 | 说明 |
|--------|------|
| routing-common | 通用路由能力 |
| routing-alipay | 支付宝渠道 |
| routing-wxpay | 微信支付渠道 |
| routing-unionpay | 银联渠道 |
| routing-umspay | UMS支付渠道 |
| routing-umsbank | UMS银行渠道 |
| routing-portal | 门户路由 |
| routing-signpay | 签约支付 |
| routing-acct | 账户路由 |

### 6. reconcile-service（对账服务）

| 属性 | 值 |
|------|------|
| 核心技术 | FTP/SFTP + 动态数据源 |
| 职责 | 金融对账 |

#### 控制器

| 组件 | 说明 |
|------|------|
| ReconTradeController | 交易对账 |
| ReconFundController | 资金对账 |

### 7. notify-service（通知服务）

| 属性 | 值 |
|------|------|
| 核心技术 | Jakarta Mail + Smack(XMPP) + RocketMQ |
| 职责 | 多通道通知（邮件/即时通讯/消息队列） |

### 8. file-service（文件存储）

| 属性 | 值 |
|------|------|
| 核心技术 | 阿里云OSS |
| 职责 | 文件上传/下载 |

### 9. portal-service（门户服务）

| 属性 | 值 |
|------|------|
| 主类 | `com.chinaums.slhy.portal.PortalApplication` |
| 职责 | Portal门户Web界面 |

### 10. gen-service（代码生成）

| 属性 | 值 |
|------|------|
| 核心技术 | Apache Velocity |
| 职责 | 自动化代码生成 |

### 11. job-service（定时任务）

| 属性 | 值 |
|------|------|
| 核心技术 | Quartz + XXL-JOB |
| 职责 | 分布式定时任务调度 |

### 12. migration-service（数据迁移）

| 属性 | 值 |
|------|------|
| 状态 | **pom.xml中已注释，暂未启用** |
| 核心技术 | ShardingSphere 5.3.2 |
| 职责 | 订单/退款表数据迁移 |

---

## 三、基础设施模块

### common-core

公共核心组件，提供基础能力：

| 组件 | 说明 |
|------|------|
| BaseController | 通用Controller基类 |
| AjaxResult | 统一返回结果 |
| BaseEntity / TenantEntity | 实体基类 |
| PageDomain / TableDataInfo | 分页支持 |
| SecurityContextHolder | 安全上下文 |
| CommonCoreConfiguration | 核心配置 |

### starter-modules

Spring Boot Starter 组件：

| Starter | 说明 |
|---------|------|
| starter-nacos | Nacos服务发现与配置 |
| starter-security | 安全框架 |
| starter-redis | Redis集成 |
| starter-rocketMq | RocketMQ集成 |
| starter-biz | 业务通用组件 |

### api-modules

| 模块 | 说明 |
|------|------|
| api-db | 数据库相关API定义 |
| api-routing | 路由相关API定义 |
| api-system | 系统相关API定义 |

### ui-modules

| 模块 | 说明 |
|------|------|
| cashier | 收银台 |
| mgr-ui | 管理后台UI |
| mgr-ui-v2 | 管理后台UI V2 |
| slhy-dashboard | 仪表盘 |
| slhy-front | 前端 |
| slhy-front-pc | PC前端 |
| slhy-front-pc-v3 | PC前端V3 |
