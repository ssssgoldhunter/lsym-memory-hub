# fund-catering-base 基础服务模块文档

## 模块概述

fund-catering-base 是餐饮资金体系的基础服务模块，提供核心的账户管理、商户管理、平台对接等基础服务。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-base |
| **主要功能** | 账户管理、商户管理、平台对接、数据访问 |
| **代码文件数** | 399 Java 文件 |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-base/
├── fund-catering-base-api/             # API 定义层
│   ├── api/                              # 服务接口定义
│   │   ├── AccountServiceFacadeApi.java         # 账户服务接口
│   │   ├── BaseAccountServiceApi.java           # 基础账户服务接口
│   │   ├── BaseActivityServiceApi.java          # 活动服务接口
│   │   ├── BaseMerchantFacadeApi.java           # 商户服务接口
│   │   ├── BasePlatformFacadeApi.java           # 平台服务接口
│   │   └── ...
│   ├── request/                          # 请求对象
│   │   ├── AccountChangeReq.java               # 账户变动请求
│   │   ├── AccountChangeBatchReq.java           # 账户批量变动请求
│   │   ├── BasCardSubAccountTReq.java           # 子账户请求
│   │   ├── BasCardAccountTReq.java              # 主账户请求
│   │   └── ...
│   └── response/                         # 响应对象
│       ├── BasCardSubAccountTQueryRes.java      # 子账户查询结果
│       ├── BasCardAccountTQueryRes.java         # 主账户查询结果
│       ├── BasMerchantTQueryRes.java            # 商户查询结果
│       └── ...
├── fund-catering-base-common/           # 公共模块（如果有）
└── fund-catering-base-service/          # 服务实现层
    ├── controller/                       # 控制器
    │   ├── AccountServiceFacadeController.java       # 账户服务控制器
    │   ├── BaseAccountFacadeController.java           # 基础账户控制器
    │   ├── BaseMerchantFacadeController.java          # 商户控制器
    │   ├── BasePlatformFacadeController.java          # 平台控制器
    │   └── ...
    ├── domain/                           # 数据对象（实体类）
    │   ├── BasCardAccountT.java                    # 主账户实体
    │   ├── BasCardSubAccountT.java                  # 子账户实体
    │   ├── BasMerchantT.java                       # 商户实体
    │   ├── BasCompanyInfo.java                     # 公司信息实体
    │   ├── BasBusinessInfo.java                    # 业务信息实体
    │   ├── BasBankInfo.java                        # 银行信息实体
    │   ├── BasPlatformInfo.java                    # 平台信息实体
    │   ├── BasActivityT.java                       # 活动实体
    │   ├── BasSubAccountExpendT.java                # 子账户支出实体
    │   ├── Converter.java                          # 对象转换器
    │   └── ...
    ├── mapper/                           # MyBatis 映射器
    │   ├── BasCardAccountTMapper.java               # 主账户Mapper
    │   ├── BasCardSubAccountTMapper.java             # 子账户Mapper
    │   ├── BasMerchantTMapper.java                  # 商户Mapper
    │   └── ...
    ├── service/                          # 服务接口
    │   ├── AccountService.java                     # 账户服务接口
    │   ├── AccountQueryService.java                 # 账户查询服务接口
    │   ├── AccountChangeBatchService.java           # 账户批量变动服务接口
    │   ├── BasCardSubAccountTService.java            # 子账户服务接口
    │   ├── BasMerchantTService.java                 # 商户服务接口
    │   ├── BasCompanyInfoService.java               # 公司信息服务接口
    │   ├── BasBusinessInfoService.java              # 业务信息服务接口
    │   ├── BasBankInfoService.java                  # 银行信息服务接口
    │   ├── BasPlatformInfoService.java              # 平台信息服务接口
    │   ├── BaseActivityService.java                 # 活动服务接口
    │   └── ...
    ├── service/impl/                      # 服务实现
    │   ├── AccountServiceImpl.java                  # 账户服务实现（410KB+）
    │   ├── AccountQueryServiceImpl.java             # 账户查询服务实现
    │   ├── AccountChangeBatchServiceImpl.java       # 账户批量变动服务实现
    │   ├── BasCardSubAccountTServiceImpl.java       # 子账户服务实现
    │   ├── BasMerchantTServiceImpl.java             # 商户服务实现
    │   ├── BasCompanyInfoServiceImpl.java           # 公司信息服务实现
    │   ├── BasBusinessInfoServiceImpl.java          # 业务信息服务实现
    │   └── ...
    ├── utils/                            # 工具类
    │   ├── MacCheckUtils.java                      # MAC校验工具
    │   ├── RedisLockUtils.java                     # Redis分布式锁工具
    │   ├── MdcPasswdUtils.java                     # 密码工具
    │   ├── CreatCheckNo.java                        # 校验码生成工具
    │   └── ...
    ├── constant/                         # 常量定义
    └── resources/
        ├── mapper/                          # MyBatis XML映射文件
        │   ├── BasCardAccountTMapper.xml
        │   ├── BasCardSubAccountTMapper.xml
        │   ├── BasMerchantTMapper.xml
        │   └── ...
        └── templates/                       # 模板文件
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **账户管理** | 主账户、子账户的增删改查，余额管理 |
| **商户管理** | 商户信息管理、门店管理 |
| **公司管理** | 公司信息管理、企业注册 |
| **业务信息** | 业务点信息管理 |
| **银行信息** | 银行信息管理、银行账户管理 |
| **平台对接** | 平台信息管理、平台结算信息 |
| **活动管理** | 营销活动管理 |
| **卡管理** | 卡模板、卡BIN、卡流水管理 |
| **账户变动** | 账户变动明细记录、批量变动处理 |
| **数据查询** | 各类数据查询服务 |

---

## 二、核心服务详解

### 2.1 AccountService（账户服务）

**实现类**: `AccountServiceImpl.java`

**主要功能**:
- 企业注册
- 开户
- 账户信息更新
- 银行账户绑定/解绑
- 银行账户激活
- 密码管理
- 提现规则管理
- 白名单管理

**核心方法**:
```java
// 企业注册
DefaultResult<BasOpenAccountRes> scRegistration(BasOpenAccountReq request);

// 创建账户
DefaultResult<BasOpenAccountRes> createScAccount(BasOpenAccountReq request);

// 更新账户
DefaultResult<BasUpdateAccountInfoRes> updateScAccount(BasUpdateAccountInfoReq request);

// 绑定银行账户
DefaultResult<Boolean> scBindBank(BasBindBankReq request);

// 解绑银行账户
DefaultResult<Boolean> scUnBindBank(BasUnbindBankReq request);

// 激活银行账户
DefaultResult<Boolean> scActiveBankAccount(ActiveBankAccountReq request);

// 查询账户信息
DefaultResult<BasAccountInfoQueryRes> queryScAccountInfo(BasAccountInfoQueryReq request);

// 查询账户列表
DefaultResult<List<BasAccountListQueryRes>> queryScAccountList(BasAccountInfoQueryReq request);

// 注销账户
DefaultResult<Boolean> scAccountCancellation(AccountCancellationReq request);

// 修改提现规则
DefaultResult<Boolean> scChangeWithdrawRule(ChangeWithdrawRuleReq request);

// 修改默认银行
DefaultResult<Boolean> scChangeDefaultBank(ChangeDefaultBankReq request);

// 重置密码
DefaultResult<Boolean> scResetPassword(ResetPasswordReq request);

// 白名单服务
DefaultResult<Boolean> scWhiteName(WhiteNameReq request);
```

### 2.2 AccountQueryService（账户查询服务）

**实现类**: `AccountQueryServiceImpl.java`

**主要功能**:
- 账户信息查询
- 账户余额查询
- 账户明细查询
- 账户变动查询

**核心方法**:
```java
// 查询账户详情
DefaultResult<BasAccountDetailQueryRes> queryScAccountDetail(BasAccountInfoQueryReq request);

// 查询账户明细列表
DefaultResult<PagedListResultDto<BasAccountDetailOutQueryRes>> queryAccountDetailList(BasAccountDetailQueryReq request);

// 查询可提现账户
DefaultResult<List<BasAccountListQueryRes>> scQueryWithdrawAccount(BasAccountInfoQueryReq request);

// 查询充值账户列表
DefaultResult<List<BasAccountListQueryRes>> queryRechargeAccountList(BasAccountInfoQueryReq request);
```

### 2.3 AccountChangeBatchService（账户批量变动服务）

**实现类**: `AccountChangeBatchServiceImpl.java`

**主要功能**:
- 批量账户变动处理
- 消费账户变动
- 充值账户变动
- 退款账户变动
- 转账账户变动
- 提现账户变动

**核心方法**:
```java
// 消费账户变动
DefaultResult<AccountChangeForConsumeRes> batchChangeAccountForConsume(AccountChangeBatchReq request);

// 充值账户变动
DefaultResult<AccountChangeForRechargeRes> batchChangeAccountForRecharge(AccountChangeBatchReq request);

// 消费退款账户变动
DefaultResult<AccountChangeForRefundConsumeRes> batchChangeAccountForRefundConsume(AccountChangeBatchReq request);

// 转账账户变动
DefaultResult<Boolean> batchChangeAccountForTransfer(AccountChangeBatchReq request);

// 提现账户变动
DefaultResult<Boolean> batchChangeAccountForWithdraw(AccountChangeBatchReq request);
```

**重要特性**:
- **同账户多次变动合并**: 同一个 subAccountId 的多次变动会合并成一次更新
- **MAC 原子一致性**: 使用 CAS 机制保证并发安全
- **批量处理**: 支持批量账户变动处理

### 2.4 BasCardSubAccountTService（子账户服务）

**实现类**: `BasCardSubAccountTServiceImpl.java`

**主要功能**:
- 子账户增删改查
- 子账户余额管理
- 子账户冻结管理

**核心方法**:
```java
// 保存子账户
DefaultResult<Long> save(BasCardSubAccountTReq request);

// 更新子账户
DefaultResult<Boolean> update(BasCardSubAccountTReq request);

// 删除子账户
DefaultResult<Boolean> delete(Long id);

// 根据ID查询
DefaultResult<BasCardSubAccountTQueryRes> queryById(Long id);

// 分页查询
DefaultResult<IPage<BasCardSubAccountTQueryRes>> queryPage(BasCardSubAccountTQueryPageReq request);

// 根据卡号查询子账户列表
List<BasCardSubAccountTQueryRes> queryByCardCode(String cardCode);

// 更新子账户（支持 MAC 校验）
int updateCardSubAccount(BasCardSubAccountTReq request);
```

### 2.5 BasMerchantTService（商户服务）

**实现类**: `BasMerchantTServiceImpl.java`

**主要功能**:
- 商户信息管理
- 商户状态管理
- 商户查询

**核心方法**:
```java
// 插入商户信息
int insert(BasMerchantT basMerchantT);

// 根据商户代码查询
BasMerchantTQueryRes queryByMerchantCode(String merchantCode);

// 批量查询商户信息
List<BasMerchantTQueryRes> queryByMerchantCodes(List<String> merchantCodes);

// 更新商户信息
int updateByMerchantCode(BasMerchantT basMerchantT);
```

### 2.6 BasCompanyInfoService（公司信息服务）

**实现类**: `BasCompanyInfoServiceImpl.java`

**主要功能**:
- 公司信息管理
- 企业注册
- 企业信息查询

### 2.7 BasBusinessInfoService（业务信息服务）

**实现类**: `BasBusinessInfoServiceImpl.java`

**主要功能**:
- 业务点信息管理
- 业务点查询

### 2.8 BasBankInfoService（银行信息服务）

**实现类**: `BasBankInfoServiceImpl.java`

**主要功能**:
- 银行信息管理
- 银行查询

### 2.9 BasePlatformFacadeApi（平台服务接口）

**主要功能**:
- 平台信息查询
- 平台结算信息查询
- 平台配置管理

**核心方法**:
```java
// 根据操作员查询结算信息
DefaultResult<BasPlatformSettleInfoQueryRes> querySettleInfoByOperator(BasOrgPlatformSettleInfoReq request);

// 批量查询结算信息
DefaultResult<List<BasPlatformSettleInfoQueryRes>> querySettleInfosByOperator(BasOrgPlatformSettleInfoReq request);

// 查询组织平台信息缓存
DefaultResult<BasPlatformSettleInfoQueryRes> queryOrgPlatformInfoCache(String orgCode);

// 获取组织结算信息
DefaultResult<BasPlatformSettleInfoQueryRes> getOrgSettleInfo(BasOrgPlatformSettleInfoReq request);

// 根据商户代码获取结算信息
DefaultResult<BasPlatformSettleInfoQueryRes> getOrgSettleInfoByMerchantCode(String merchantCode);
```

### 2.10 BaseActivityService（活动服务）

**实现类**: `BasActivityTServiceImpl.java`

**主要功能**:
- 活动信息管理
- 活动查询
- 活动预算管理

---

## 三、核心数据模型（Domain）

### 3.1 BasCardAccountT（主账户实体）

```java
@Data
@TableName("bas_card_account_t")
public class BasCardAccountT {
    @TableId(value = "account_id", type = IdType.AUTO)
    private Long accountId;           // 主账户ID

    private String cardCode;           // 卡号
    private String platformCode;       // 平台编码
    private String cardBatchNo;        // 发卡批次号
    private String firstOrgCode;       // 一级机构号
    private String secondOrgCode;      // 二级机构号
    private String merchantCode;       // 商户号
    private String status;             // 卡状态：N-正常 O-待激活 CE-作废 AR-退卡 L-锁定 F-冻结 R-挂失 E-过期
    private Date openDate;             // 开卡日期
    private Date activeDate;           // 激活日期
    private Date expDate;              // 到期日期
    private String ifRecharge;         // 是否可以充值：1-是 0-否
    private Long denomination;         // 面值（分）
    private String passwdFree;         // 免密开关：A-开启 D-关闭
    private String ifHavePasswd;       // 是否有密码：1-是 0-否
    private String passwd;             // 密码（密文）
    private String mac;                // MAC值
}
```

### 3.2 BasCardSubAccountT（子账户实体）

```java
@Data
@TableName("bas_card_sub_account_t")
public class BasCardSubAccountT {
    @TableId(value = "sub_account_id", type = IdType.AUTO)
    private Long subAccountId;         // 子账户ID

    private Long accountId;            // 主账户ID
    private String cardCode;           // 卡号
    private String subAccountType;     // 子账户类型：01-现金 02-膨胀金
    private Long balance;              // 余额（分）
    private Long realBalance;          // 本金余额（分）
    private Long frozenAmt;            // 冻结金额（分）
    private Long withdrawBalance;      // 可提现金额（分）
    private Long waitReleaseBalance;   // 待释放可提现金额（分）
    private Date lastChangeTime;       // 余额最后变更时间
    private String mac;                // MAC值（旧值，用于CAS校验）

    // 非数据库字段
    @TableField(exist = false)
    private String newMac;             // 新MAC值（用于CAS更新）
}
```

### 3.3 BasMerchantT（商户实体）

```java
@Data
@TableName("bas_merchant_t")
public class BasMerchantT {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;                   // 商户ID

    private String merchantCode;        // 商户号
    private String merchantName;        // 商户名称
    private String merchantDescribe;    // 商户描述
    private String orgCode;             // 机构号
    private String shopId;             // 业务方门店ID
    private String status;             // 商户状态：O-待生效 N-正常 P-禁用 D-失效
    private String auditStatus;        // 审核状态：O-待审核 P-通过 R-拒绝
}
```

### 3.4 BasCompanyInfo（公司信息实体）

```java
@Data
@TableName("bas_company_info_t")
public class BasCompanyInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;             // 第三方用户ID
    private String companyName;        // 公司名称
    private String companyType;        // 公司类型
    private String orgCode;            // 机构号
    private String creditCode;         // 统一社会信用代码
    private String legalPerson;        // 法人代表
    private String businessLicense;    // 营业执照
    private String bankEAccountCode;   // 银行电子账户代码
    // ... 更多字段
}
```

### 3.5 BasBusinessInfo（业务信息实体）

```java
@Data
@TableName("bas_business_info_t")
public class BasBusinessInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessId;         // 业务点ID
    private String businessName;       // 业务点名称
    private String orgCode;            // 机构号
    private String secondOrgCode;      // 二级机构号
    private String firstOrgCode;       // 一级机构号
    private String operatorCode;       // 运营商号
    private String merchantCode;       // 商户号
    // ... 更多字段
}
```

### 3.6 BasActivityT（活动实体）

```java
@Data
@TableName("bas_activity_t")
public class BaseActivity {
    @TableId(value = "activity_id", type = IdType.AUTO)
    private Long activityId;           // 活动ID

    private String activityCode;        // 活动编码
    private String activityCategory;    // 活动种类：EX-膨胀金活动 PA-支付立减活动
    private Long activityTypeId;        // 活动类型ID
    private String activityName;        // 活动名称
    private String activityDesc;        // 活动规则描述
    private Long activityBudgetAmt;    // 活动预算（分）
    private Date startDate;            // 活动起始时间
    private Date endDate;              // 活动截止时间
    private Integer userNumsLimit;     // 用户上限
    private Integer userTimesLimit;    // 单用户最大参加次数
    private Integer accessUserNums;     // 已参加活动用户数量
    private Long accessAmt;            // 已使用活动预算（分）
    private String activityStatus;     // 活动状态：O-待生效 N-正常 P-禁用 C-作废 E-结束
    private String subAccountType;     // 子账户类型：01-现金 02-膨胀金
}
```

### 3.7 BasSubAccountExpendT（子账户支出实体）

```java
@Data
@TableName("bas_sub_account_expend_t")
public class BasSubAccountExpendT {
    @TableId(type = IdType.AUTO)
    private Long acctExpandId;         // 膨胀金子账户扩展ID

    private String cardCode;           // 卡号
    private String subAccountType;     // 子账户类型
    private Long subAccountId;         // 子账户ID
    private String activityCode;        // 活动编码
    private String rechargeTransNo;    // 充值交易号
    private Long expendAmt;            // 已支出金额（分）
    private Long remainAmt;            // 剩余金额（分）
    private String status;             // 状态：1-有效 0-失效
}
```

---

## 四、核心工具类

### 4.1 MacCheckUtils（MAC校验工具）

```java
public class MacCheckUtils {
    // 生成MAC
    public static String generateMac(Object obj);

    // 验证MAC
    public static boolean verifyMac(Object obj, String mac);

    // 生成新MAC
    public static String getMac();
}
```

### 4.2 RedisLockUtils（Redis分布式锁工具）

```java
public class RedisLockUtils {
    // 获取锁
    public boolean lockKey(String key, int expireSeconds);

    // 获取锁（不等待）
    public boolean lockKeyNoWait(String key, int expireSeconds);

    // 释放锁
    public void unlockKey(String key);
}
```

### 4.3 CreatCheckNo（校验码生成工具）

```java
public class CreatCheckNo {
    // 生成校验码
    public static String createCheckNo(String prefix);
}
```

### 4.4 MdcPasswdUtils（密码工具）

```java
public class MdcPasswdUtils {
    // 加密密码
    public static String encryptPassword(String password);

    // 验证密码
    public static boolean verifyPassword(String password, String encryptedPassword);
}
```

### 4.5 Converter（对象转换器）

```java
@Component
public class Converter {
    // 对象转换
    public <T> T reqToDto(Object source, Class<T> targetClass);

    // 批量转换
    public <T> List<T> reqToDtoList(List<?> sourceList, Class<T> targetClass);
}
```

---

## 五、API接口列表

### 5.1 AccountServiceFacadeApi

```java
public interface AccountServiceFacadeApi {
    // 企业注册
    DefaultResult<BasOpenAccountRes> scRegistration(BasOpenAccountReq request);

    // 创建账户
    DefaultResult<BasOpenAccountRes> createScAccount(BasOpenAccountReq request);

    // 更新账户
    DefaultResult<BasUpdateAccountInfoRes> updateScAccount(BasUpdateAccountInfoReq request);

    // 绑定银行账户
    DefaultResult<Boolean> scBindBank(BasBindBankReq request);

    // 解绑银行账户
    DefaultResult<Boolean> scUnBindBank(BasUnbindBankReq request);

    // 激活银行账户
    DefaultResult<Boolean> scActiveBankAccount(ActiveBankAccountReq request);

    // 查询账户信息
    DefaultResult<BasAccountInfoQueryRes> queryScAccountInfo(BasAccountInfoQueryReq request);

    // 查询账户列表
    DefaultResult<List<BasAccountListQueryRes>> queryScAccountList(BasAccountInfoQueryReq request);

    // 查询账户详情
    DefaultResult<BasAccountDetailQueryRes> queryScAccountDetail(BasAccountInfoQueryReq request);

    // 查询账户明细列表
    DefaultResult<PagedListResultDto<BasAccountDetailOutQueryRes>> queryAccountDetailList(BasAccountDetailQueryReq request);

    // 查询可提现账户
    DefaultResult<List<BasAccountListQueryRes>> scQueryWithdrawAccount(BasAccountInfoQueryReq request);

    // 更新账户信息
    DefaultResult<BasUpdateAccountInfoRes> scUpdateAccountInfo(BasUpdateAccountInfoReq request);

    // 注销账户
    DefaultResult<Boolean> scAccountCancellation(AccountCancellationReq request);

    // 修改提现规则
    DefaultResult<Boolean> scChangeWithdrawRule(ChangeWithdrawRuleReq request);

    // 修改默认银行
    DefaultResult<Boolean> scChangeDefaultBank(ChangeDefaultBankReq request);

    // 重置密码
    DefaultResult<Boolean> scResetPassword(ResetPasswordReq request);

    // 删除缓存
    DefaultResult<Boolean> deleteRedisByUserId(String userId);

    // 更改银行账户
    DefaultResult<Boolean> scChangeBankAccount(ChangeBankAccountReq request);

    // 查询充值账户列表
    DefaultResult<List<BasAccountListQueryRes>> queryRechargeAccountList(BasAccountInfoQueryReq request);

    // 激活充值银行账户
    DefaultResult<Boolean> scActiveRechargeBankAccount(ActiveRechargeBankAccountReq request);

    // 更新充值银行账户
    DefaultResult<Boolean> scUpdateRechargeBankAccount(UpdateRechargeBankAccountReq request);

    // 白名单服务
    DefaultResult<Boolean> scWhiteName(WhiteNameReq request);
}
```

### 5.2 BaseAccountServiceApi

```java
public interface BaseAccountServiceApi {
    // 账户变动服务接口
    AccountChangeBatchService getAccountChangeBatchService();

    // 账户查询服务接口
    AccountQueryService getAccountQueryService();

    // 子账户服务接口
    BasCardSubAccountTService getBasCardSubAccountTService();

    // 主账户服务接口
    BasCardAccountTService getBasCardAccountTService();
}
```

### 5.3 BasePlatformFacadeApi

```java
public interface BasePlatformFacadeApi {
    // 根据操作员查询结算信息
    DefaultResult<BasPlatformSettleInfoQueryRes> querySettleInfoByOperator(BasOrgPlatformSettleInfoReq request);

    // 批量查询结算信息
    DefaultResult<List<BasPlatformSettleInfoQueryRes>> querySettleInfosByOperator(BasOrgPlatformSettleInfoReq request);

    // 查询组织平台信息缓存
    DefaultResult<BasPlatformSettleInfoQueryRes> queryOrgPlatformInfoCache(String orgCode);

    // 获取组织结算信息
    DefaultResult<BasPlatformSettleInfoQueryRes> getOrgSettleInfo(BasOrgPlatformSettleInfoReq request);

    // 初始化缓存
    DefaultResult<Boolean> initCache();

    // 根据商户代码获取结算信息
    DefaultResult<BasPlatformSettleInfoQueryRes> getOrgSettleInfoByMerchantCode(String merchantCode);
}
```

---

## 六、技术特点

### 6.1 MAC机制

关键实体（如 `BasCardSubAccountT`）包含 MAC 字段，用于实现乐观锁机制，防止并发更新问题。

**CAS更新逻辑**:
```sql
UPDATE bas_card_sub_account_t
SET balance = #{newBalance},
    mac = #{newMac}
WHERE sub_account_id = #{subAccountId}
  AND mac = #{oldMac}
```

### 6.2 批量处理优化

`AccountChangeBatchServiceImpl` 实现了同账户多次变动的合并处理：
- 同一个 `subAccountId` 的多次变动会合并成一次更新
- 减少数据库交互次数
- 提高批量处理性能

### 6.3 分布式锁

使用 `RedisLockUtils` 实现分布式锁：
- 基于 `cardCode` 加锁
- 防止并发操作冲突
- 5分钟超时自动释放

### 6.4 对象转换

使用 `Converter` 工具类进行对象转换：
- 支持单个对象转换
- 支持批量对象转换
- 自动复制同名字段

---

## 七、服务调用关系

```
其他模块
    ↓
API 接口层 (AccountServiceFacadeApi)
    ↓
Service 接口层 (AccountService)
    ↓
Service 实现层 (AccountServiceImpl)
    ↓
Mapper 层 (MyBatis)
    ↓
数据库
```

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
