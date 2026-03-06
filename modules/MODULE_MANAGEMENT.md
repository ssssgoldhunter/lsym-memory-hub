# fund-catering-management 管理服务模块文档

## 模块概述

fund-catering-management 是餐饮资金体系的管理服务层，主要负责商户管理、配置管理、结算管理等功能。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-management |
| **主要功能** | 商户管理、配置管理、结算管理 |
| **代码文件数** | 167 Java 文件 |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-management/
├── fund-catering-management-api/         # API 定义层
│   ├── api/                              # 服务接口定义
│   ├── enums/                            # 枚举定义
│   ├── request/                          # 请求对象
│   └── response/                         # 响应对象
├── fund-catering-management-common/      # 公共模块
│   ├── constant/                         # 常量定义
│   ├── enums/                            # 枚举定义
│   └── utils/                            # 工具类
└── fund-catering-management-service/     # 服务实现层
    ├── controller/                       # 控制器
    │   └── XxxServiceFacadeController.java
    ├── domain/                           # 数据对象
    ├── service/                          # 服务接口
    │   └── impl/                         # 服务实现
    └── ManagementServiceApplication.java # 启动类
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **商户管理** | 商户信息管理、门店管理、门店配置 |
| **合同管理** | 商户合同管理、合同模板管理、合同费用管理 |
| **结算管理** | 结算代理管理、结算账户管理、结算汇总 |
| **配置管理** | 系统配置、业务配置 |
| **业务项管理** | NPK业务项管理 |

---

## 二、核心 Controller

### 2.1 StoreServiceFacadeController（商户服务）

| 功能 | 接口 |
|------|------|
| 添加商户 | `addStore` |
| 编辑商户 | `editStore` |
| 查询商户 | `queryStore` |
| 商户列表 | `storeList` |
| 删除商户 | `deleteStore` |

### 2.2 StoreContractFacadeController（合同服务）

| 功能 | 接口 |
|------|------|
| 添加合同 | `addStoreContract` |
| 编辑合同 | `editStoreContract` |
| 查询合同 | `queryStoreContract` |
| 合同列表 | `storeContractList` |
| 获取所有FZ合同 | `getAllFZMpStoreContractInfo` |

### 2.3 ConfigServiceFacadeController（配置服务）

| 功能 | 接口 |
|------|------|
| 重置配置 | `resetConfig` |
| 查询配置 | `queryConfig` |
| 更新配置 | `updateConfig` |

### 2.4 SettlementServiceFacadeController（结算服务）

| 功能 | 接口 |
|------|------|
| 查询清算明细 | `queryCleanSettlementDetail` |
| 查询清算汇总 | `queryCleanSettlementSum` |
| 查询HF清算明细 | `queryHfCleanSettlementDetail` |
| 查询HF清算汇总 | `queryHfCleanSettlementSum` |

### 2.5 MpSettleAgencyServiceFacadeController（结算代理服务）

| 功能 | 接口 |
|------|------|
| 添加结算代理 | `addSettleAgency` |
| 编辑结算代理 | `editSettleAgency` |
| 查询结算代理 | `querySettleAgency` |
| 结算代理列表 | `settleAgencyList` |

### 2.6 SettleAgencyAccountFacadeController（结算账户服务）

| 功能 | 接口 |
|------|------|
| 添加结算账户 | `addSettleAgencyAccount` |
| 编辑结算账户 | `editSettleAgencyAccount` |
| 查询结算账户 | `querySettleAgencyAccount` |

### 2.7 NpkBusiItemServiceFacadeController（业务项服务）

| 功能 | 接口 |
|------|------|
| 添加业务项 | `addBusiItem` |
| 编辑业务项 | `editBusiItem` |
| 查询业务项 | `queryBusiItem` |

---

## 三、核心数据模型

### 3.1 商户相关

#### MpStoreContractQueryRes（商户合同查询结果）
```java
// 商户合同查询结果
- storeContractId: 合同ID
- storeId: 门店ID
- contractNo: 合同编号
- contractName: 合同名称
- startDate: 开始日期
- endDate: 结束日期
- status: 状态
```

#### NpkStoreQueryRes（门店查询结果）
```java
// 门店查询结果
- storeId: 门店ID
- storeName: 门店名称
- storeCode: 门店编码
- orgCode: 机构编码
- status: 状态
```

### 3.2 结算相关

#### MpSettleAgencyQueryRes（结算代理查询结果）
```java
// 结算代理查询结果
- settleAgencyId: 结算代理ID
- settleAgencyName: 结算代理名称
- settleAgencyCode: 结算代理编码
- status: 状态
```

#### SysSettleAgencyAccountQueryResponse（结算账户查询结果）
```java
// 结算账户查询结果
- accountId: 账户ID
- accountName: 账户名称
- accountNo: 账户号码
- bankName: 银行名称
```

### 3.3 配置相关

#### MpMchntConfigQueryRes（商户配置查询结果）
```java
// 商户配置查询结果
- mchntId: 商户ID
- configKey: 配置键
- configValue: 配置值
```

---

## 四、核心常量

### 4.1 StoreContractConstant（合同常量）

```java
public class StoreContractConstant {
    // 合同状态
    public static final String STATUS_DRAFT = "0";      // 草稿
    public static final String STATUS_ACTIVE = "1";     // 生效
    public static final String STATUS_EXPIRED = "2";    // 过期
    public static final String STATUS_TERMINATED = "3"; // 终止

    // 合同类型
    public static final String TYPE_STANDARD = "1";     // 标准合同
    public static final String TYPE_CUSTOM = "2";       // 自定义合同
}
```

### 4.2 CfgConstant（配置常量）

```java
public class CfgConstant {
    // 配置类型
    public static final String TYPE_SYSTEM = "1";       // 系统配置
    public static final String TYPE_BUSINESS = "2";     // 业务配置

    // 配置状态
    public static final String STATUS_ENABLE = "1";     // 启用
    public static final String STATUS_DISABLE = "0";    // 禁用
}
```

---

## 五、枚举定义

### 5.1 DeleteState（删除状态）

```java
public enum DeleteState {
    NORMAL(0, "正常"),
    DELETED(1, "已删除");

    private final int code;
    private final String desc;
}
```

### 5.2 EnableState（启用状态）

```java
public enum EnableState {
    DISABLE(0, "禁用"),
    ENABLE(1, "启用");

    private final int code;
    private final String desc;
}
```

### 5.3 CatingErrorCodeEnum（错误码枚举）

```java
public enum CatingErrorCodeEnum {
    SUCCESS("0000", "成功"),
    PARAM_ERROR("1001", "参数错误"),
    DATA_NOT_FOUND("1002", "数据不存在"),
    DATA_EXISTS("1003", "数据已存在"),
    // ... 更多错误码
}
```

---

## 六、工具类

### 6.1 RedisIdUtil（Redis ID生成工具）

```java
public class RedisIdUtil {
    // 生成唯一ID
    public static String generateId(String prefix);

    // 生成批量ID
    public static List<String> generateIds(String prefix, int count);
}
```

---

## 七、API接口列表

### 7.1 StoreServiceFacadeApi

```java
public interface StoreServiceFacadeApi {
    // 添加商户
    DefaultResult addStore(StoreListApiRequest request);

    // 编辑商户
    DefaultResult editStore(StoreListApiRequest request);

    // 查询商户
    DefaultResult<NpkStoreQueryRes> queryStore(String storeId);

    // 商户列表
    DefaultResult<List<NpkStoreQueryRes>> storeList(StoreListApiRequest request);
}
```

### 7.2 StoreContractServiceFacadeApi

```java
public interface StoreContractServiceFacadeApi {
    // 添加合同
    DefaultResult addStoreContract(MpStoreContractInfoRes request);

    // 编辑合同
    DefaultResult editStoreContract(MpStoreContractInfoRes request);

    // 查询合同
    DefaultResult<MpStoreContractQueryRes> queryStoreContract(String contractId);

    // 获取所有FZ合同
    DefaultResult<List<MpStoreContractQueryRes>> getAllFZMpStoreContractInfo();
}
```

### 7.3 ConfigServiceFcadeApi

```java
public interface ConfigServiceFcadeApi {
    // 重置配置
    DefaultResult resetConfig(String configKey);

    // 查询配置
    DefaultResult queryConfig(String configKey);

    // 更新配置
    DefaultResult updateConfig(String configKey, String configValue);
}
```

### 7.4 SettlementServiceFacadeApi

```java
public interface SettlementServiceFacadeApi {
    // 查询清算明细
    DefaultResult<BaseCleanSettlementDetailResponse> queryCleanSettlementDetail(
        BaseCleanSettlementDetailRequest request
    );

    // 查询清算汇总
    DefaultResult<BaseCleanSettlementSumResponse> queryCleanSettlementSum(
        BaseCleanSettlementSumRequest request
    );
}
```

### 7.5 MpSettleAgencyFacadeApi

```java
public interface MpSettleAgencyFacadeApi {
    // 添加结算代理
    DefaultResult addSettleAgency(MpSettleAgencyQueryRes request);

    // 编辑结算代理
    DefaultResult editSettleAgency(MpSettleAgencyQueryRes request);

    // 查询结算代理
    DefaultResult<MpSettleAgencyQueryRes> querySettleAgency(String agencyId);
}
```

### 7.6 SettleAgencyAccountFacadeApi

```java
public interface SettleAgencyAccountFacadeApi {
    // 添加结算账户
    DefaultResult addSettleAgencyAccount(SysSettleAgencyAccountQueryResponse request);

    // 编辑结算账户
    DefaultResult editSettleAgencyAccount(SysSettleAgencyAccountQueryResponse request);

    // 查询结算账户
    DefaultResult<SysSettleAgencyAccountQueryResponse> querySettleAgencyAccount(String accountId);
}
```

### 7.7 NpkBusiItemServiceFacadeApi

```java
public interface NpkBusiItemServiceFacadeApi {
    // 添加业务项
    DefaultResult addBusiItem(NpkBusiItemQueryRes request);

    // 编辑业务项
    DefaultResult editBusiItem(NpkBusiItemQueryRes request);

    // 查询业务项
    DefaultResult<NpkBusiItemQueryRes> queryBusiItem(String itemId);
}
```

---

## 八、响应对象

### 8.1 BaseStoreApiResponse（商户API响应）

```java
public class BaseStoreApiResponse extends DefaultResult {
    // 商户API响应基类
    // 包含响应码、响应消息、响应数据
}
```

### 8.2 MerchantBaseResponse（商户基础响应）

```java
public class MerchantBaseResponse extends DefaultResult {
    // 商户基础响应
    // 包含响应码、响应消息、商户数据
}
```

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
