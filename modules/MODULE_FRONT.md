# fund-catering-front 前置服务模块文档

## 模块概述

fund-catering-front 是餐饮资金体系的前置服务层，主要负责与外部平台（银商PA平台、中信ZX平台）的对接与交互。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-front |
| **主要功能** | 外部平台对接、交易转发、文件处理 |
| **代码文件数** | 214 Java 文件 |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-front/
├── fund-catering-front-api/              # API 定义层
│   ├── api/                              # 服务接口定义
│   ├── request/                          # 请求对象
│   └── response/                         # 响应对象
├── fund-catering-front-common/           # 公共模块
│   ├── annotation/                       # 注解定义
│   ├── config/                           # 配置类
│   ├── constant/                         # 常量定义
│   ├── domain/                           # 数据对象
│   ├── enums/                            # 枚举定义
│   ├── request/                          # 请求对象（对接平台）
│   │   ├── pasass/                       # PA平台请求对象
│   │   └── zxsaas/                       # ZX平台请求对象
│   └── utils/                            # 工具类
└── fund-catering-front-service/          # 服务实现层
    ├── controller/                       # 控制器
    │   ├── sass/                         # SAAS控制器
    │   └── FrontXxxFacadeController.java # 门面控制器
    ├── service/                          # 服务接口
    │   ├── pa/                           # PA平台服务
    │   ├── zx/                           # ZX平台服务
    │   └── impl/                         # 服务实现
    └── FrontServiceApplication.java      # 启动类
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **PA平台对接** | 与银商支付平台对接，处理账户、交易、查询等操作 |
| **ZX平台对接** | 与中信平台对接，处理账户、交易、文件等操作 |
| **交易转发** | 将内部交易请求转发到外部平台 |
| **文件处理** | 处理平台文件的下载、上传 |
| **消息通知** | 处理平台消息通知 |
| **验证码服务** | 处理转账验证码发送和校验 |

---

## 二、PA平台对接

### 2.1 PA平台接口

| 接口 | 功能 | 请求对象 |
|------|------|----------|
| 账户开户 | `PaAcctOpenRequest` | 开通账户 |
| 账户查询 | `PaQueryAcctInfoRequest` | 查询账户信息 |
| 账户列表查询 | `PaQueryAcctInfoListRequest` | 查询账户列表 |
| 绑定卡 | `PaBindCardRequest` | 绑定银行卡 |
| 解绑卡 | `PaUnbindCardRequest` | 解绑银行卡 |
| 绑定卡查询 | `PaQueryBindCardInfoRequest` | 查询绑定卡信息 |
| 更新账户信息 | `PaUpdateAcctInfoRequest` | 更新账户信息 |
| 维护客户信息 | `PaMaintenCustInfoRequest` | 维护客户信息 |
| 子账户转账 | `PaSubAcctTransferRequest` | 子账户转账 |
| 转账 | `PaTransferRequest` | 转账 |
| 小额转账授权 | `PaSmallTransferAuthRequest` | 小额转账授权 |
| 提现 | `PaWithdrawalRequest` | 提现 |
| 提现授权码 | `PaWithdrawAuthCodeRequest` | 提现授权码 |
| 退款 | `PaRefundRequest` | 退款 |
| 交易状态查询 | `PaQueryTransStatusRequest` | 查询交易状态 |
| 交易明细查询 | `PaQueryTransDetailsRequest` | 查询交易明细 |
| 对账文件检查 | `PaCheckFileInfoRequest` | 检查对账文件 |
| 文件下载 | `PaDownLoadRequest` | 下载文件 |

### 2.2 PA平台服务

```java
// PA平台交易查询服务
public interface PaTransQueryService {
    // 查询平台交易流水
    BasTransPageQueryRes<BasPaTransDetailQueryRes> queryPlatformTransPages(
        BasTransDetailPageQueryReq request
    );
}

// PA平台交互服务
public interface SaasPaInterService {
    // 各种PA平台交互方法
}
```

### 2.3 PA Controller

| Controller | 路径 | 功能 |
|------------|------|------|
| `SaasPaController` | `/sass/pa/` | PA平台SAAS接口 |
| `FrontTransPaFacadeController` | `/front/pa/` | PA平台交易门面 |

---

## 三、ZX平台对接

### 3.1 ZX平台接口

| 接口 | 功能 | 请求对象 |
|------|------|----------|
| 账户开户 | `ZxAcctOpenRequest` | 开通账户 |
| 账户关闭 | `ZxAcctCloseRequest` | 关闭账户 |
| 账户查询 | `ZxQueryAcctInfoRequest` | 查询账户信息 |
| 绑定卡查询 | `ZxQueryBindCardInfoRequest` | 查询绑定卡信息 |
| 更新账户信息 | `ZxUpdateAcctInfoRequest` | 更新账户信息 |
| 转账 | `ZxTransferRequest` | 转账 |
| 提现 | `ZxWithdrawalRequest` | 提现 |
| 交易状态查询 | `ZxQueryTransStatusRequest` | 查询交易状态 |
| 交易明细查询 | `ZxQueryTransDetailRequest` | 查询交易明细 |
| 对账文件检查 | `ZxQueryCheckFileInfoRequest` | 检查对账文件 |
| 文件上传 | `ZxUploadRequest` | 上传文件 |
| 文件下载 | `ZxDownLoadRequest` | 下载文件 |
| 特殊账户充值 | `ZxScSpecialRechargeRequest` | 特殊账户充值 |
| 特殊账户 | `ZxScSpecialAccountsRequest` | 特殊账户处理 |

### 3.2 ZX平台服务

```java
// ZX平台交易服务
public interface ZxTransService {
    // ZX平台交易处理
}

// ZX平台交易查询服务
public interface ZxTransQueryService {
    // ZX平台交易查询
}
```

### 3.3 ZX Controller

| Controller | 路径 | 功能 |
|------------|------|------|
| `SaasZxController` | `/sass/zx/` | ZX平台SAAS接口 |
| `FrontTransZxFacadeController` | `/front/zx/` | ZX平台交易门面 |

---

## 四、其他核心功能

### 4.1 文件处理服务

```java
public interface FileProcessService {
    // 文件上传
    // 文件下载
    // 文件处理
}
```

### 4.2 消息服务

```java
public interface MessageSendService {
    // 消息发送
}
```

### 4.3 验证服务

```java
public interface TransVerificationService {
    // 验证码发送
    // 验证码校验
}
```

---

## 五、核心 Controller 列表

| Controller | 功能描述 |
|------------|----------|
| `FrontAccountFacadeController` | 账户门面控制器 |
| `FrontTransConsumeController` | 消费交易控制器 |
| `FrontTransPaFacadeController` | PA平台交易门面控制器 |
| `FrontTransZxFacadeController` | ZX平台交易门面控制器 |
| `FrontTransQueryFacadeController` | 交易查询门面控制器 |
| `FrontFileProcessFacadeController` | 文件处理门面控制器 |
| `FrontMessageFacadeController` | 消息门面控制器 |
| `FrontTransVerificationController` | 交易验证控制器 |
| `FrontLsymFacadeController` | LSYS门面控制器 |
| `FrontTestFacadeController` | 测试门面控制器 |
| `SaasPaController` | PA平台SAAS控制器 |
| `SaasZxController` | ZX平台SAAS控制器 |

---

## 六、核心常量与枚举

### 6.1 常量

```java
// FrontConstants.java
public class FrontConstants {
    // 平台相关常量
    // 交易类型常量
    // 状态常量
}
```

### 6.2 枚举

| 枚举类 | 说明 |
|--------|------|
| `MessageTypeTopicEnum` | 消息类型主题枚举 |
| `PlatformQueryTypeEnum` | 平台查询类型枚举 |
| `PaTransHandleEnum` | PA交易处理枚举 |

---

## 七、注解

### 7.1 @EnableUmsReserve

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableUmsReserve {
    // 启用UMS保留注解
}
```

---

## 八、配置

### 8.1 SAAS配置

```java
@Configuration
public class SAASConfig {
    // SAAS平台配置
}
```

---

## 九、流程说明

### 9.1 PA平台交易流程

```
内部请求 → Front Controller → PaTransService → SAAS PA Service → PA平台
```

### 9.2 ZX平台交易流程

```
内部请求 → Front Controller → ZxTransService → SAAS ZX Service → ZX平台
```

---

## 十、技术特点

1. **平台适配**: 通过统一的接口适配不同外部平台
2. **请求封装**: 将外部平台请求封装为内部对象
3. **响应转换**: 将外部平台响应转换为内部对象
4. **异常处理**: 统一的异常处理机制
5. **日志记录**: 完整的请求响应日志

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
