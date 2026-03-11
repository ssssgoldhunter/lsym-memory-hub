# 账户模块 - 预开户与用户注册的区别

**日期**: 2026-03-11

---

## 概念对比

| 特性 | 用户注册 (scRegistration) | 预开户 (createScAccount) |
|------|--------------------------|------------------------|
| **入口方法** | `AccountService.scRegistration()` | `AccountService.createScAccount()` |
| **请求类** | `BasBusinessInfoReq` | `BasCreateAccountReq` |
| **操作对象** | 企业信息表 `bas_company_info` | 账户表 `bas_card_account_t` + `bas_card_sub_account_t` |
| **目的** | 在系统中创建**企业/商户**的基础信息 | 创建**真正的资金账户** |
| **本质** | 登记企业信息（类似填申请表） | 开通资金账户（类似银行开户） |

---

## 业务流程

```
用户注册 (bas_company_info)
       ↓
   企业审核
       ↓
预开户 (bas_card_account_t + bas_card_sub_account_t)
       ↓
   激活账户
       ↓
  正式使用（充值/消费/提现）
```

---

## 请求参数对比

### BasBusinessInfoReq（用户注册）
- `businessCode` - 企业代码
- `businessName` - 企业名称
- `businessType` - 企业类型
- `registerCreditCode` - 统一社会信用代码
- `legalName` - 法人姓名
- `legalCardNo` - 法人证件号
- `registerContractPhone` - 联系人电话
- `merchantCode` - 商户号
- `firstOrgCode` - 一级机构号

### BasCreateAccountReq（预开户）
- `merchantId` - 商户号
- `businessCode` - 企业代码
- `businessId` - 银商企业唯一ID
- `cardBinCode` - 卡BIN代码
- `templateId` - 发卡模板ID
- `effectDate` - 账户生效日期
- `expireDate` - 账户失效日期
- `acctType` - 开户类型
- `certType` - 用户证件类型
- `certNo` - 用户证件号
- `mobile` - 用户手机号

---

## 子账户类型

预开户时会根据卡BIN配置创建对应的子账户：

| 代码 | 名称 | 说明 |
|------|------|------|
| 01 | 资金子账户 | 可提现 |
| 02 | 膨胀金子账户 | 赠送金额，优先消费，不可提现 |
| 03 | 额度子账户 | 额度管理 |
| 04 | 内部户子账户 | 内部账户 |

---

## 源码路径

- **AccountService**: `fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/AccountService.java`
- **AccountServiceImpl**: `fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/impl/AccountServiceImpl.java`

---

## 银行卡白名单（whiteName）

### 功能说明

> **注意**: 目前白名单机制**仅支持中信银行（ZX）**，平安银行（PA）未实现。

| 项目 | 说明 |
|------|------|
| **方法** | `scWhiteName()` |
| **请求类** | `WhiteNameReq` |
| **操作对象** | 银行卡（在银行侧的白名单） |
| **目的** | 将银行卡加入/移出银行的白名单，允许/禁止进行充值等操作 |

### 白名单类型

| opType | 说明 |
|--------|------|
| **1** | 加白（将银行卡加入白名单） |
| **2** | 去白（将银行卡从白名单移除） |

### 请求参数

| 字段 | 说明 |
|------|------|
| merchantId | 商户号 |
| cardCode | 企业账户号 |
| accountNo | 银行卡号 |
| userId | 用户ID |
| opType | 操作类型：1-加白，2-去白 |

### 平台支持

| 平台 | 支持状态 |
|------|----------|
| ZX（中信银行） | ✅ 已实现 |
| PA（平安银行） | ❌ 未实现 |

### 调用链路

```
Base → Front → ZxAccountHandle.whiteName() → 中信银行API
```

### 业务流程

```
用户绑卡 (scBindBank)
       ↓
  调用 whiteName 接口
       ↓
  将银行卡号发送到银行侧加白
       ↓
  银行返回加白结果
       ↓
  记录绑卡日志
```

---

## 充值账户判断逻辑

### 判断是否已开通充值账户

| 场景 | 判断字段 | 说明 |
|------|----------|------|
| **个体工商户** | `corpIdNo`（法人证件号） | 先根据法人信息查询 |
| **普通企业** | `comCode`（企业代码） | 根据企业代码查询 |
| **状态** | `status = 'N'` | 正常状态 |
| **标志** | `rechargeFlag = '1'` | 充值账户标志 |

### 数据关系

```
企业信息表 (bas_company_info)
    │
    ├── comCode = "UMS00000001"     (企业代码)
    ├── businessCode = "123456789"   (业务代码)
    ├── cardCode = "31000010001"    (消费账户卡号)
    ├── rechargeCardCode = "990001" (充值账户卡号，关联到另一个企业)
    └── rechargeFlag = "1"          (是否开通充值账户)
```

### 企业账户与充值账户的关系

- 每个企业可以拥有**两个账户**：1个消费账户 + 1个充值账户
- `rechargeCardCode` 指向充值账户的卡号
- 它们是**不同的卡号**，对应不同的银行账户
- 充值账户用**法人证件号**判断是否已开通

---

## 独立充值账户机制

### 充值账户是独立账户（D）

开通充值账户时，会**独立创建一个新账户**，不是复用第一个消费账户：

```java
// 第一次开户且需要充值账户时
rechargeCreateAccountReq.setUserId("RE" + dto.getCertNo());  // RE开头，独立的userId
rechargeCreateAccountReq.setRechargeFlag("1");  // 是充值账户
```

### 账户结构

```
企业信息表 (bas_company_info)
    │
    ├── 账户A (cardCode: A, userId: A001, rechargeFlag: 0) - 消费账户
    ├── 账户B (cardCode: B, userId: B001, rechargeFlag: 0, rechargeCardCode: D) - 消费账户
    ├── 账户C (cardCode: C, userId: C001, rechargeFlag: 0, rechargeCardCode: D) - 消费账户
    │
    └── 账户D (cardCode: D, userId: RE证件号, rechargeFlag: 1) - 独立充值账户
```

### 账户类型说明

| 账户 | 类型 | userId | 说明 |
|------|------|--------|------|
| A | 消费 | 普通ID | 第一张卡，创建充值账户D |
| B | 消费 | 普通ID | 复用D的充值配置 |
| C | 消费 | 普通ID | 复用D的充值配置 |
| D | 充值 | RE+证件号 | 独立账户，只接收充值 |

### 业务含义

- **消费账户**：用于消费、提现
- **充值账户（D）**：独立创建，以"RE"开头，只接收充值资金，不用于消费
- **多消费账户共享**：A/B/C都使用同一个充值账户D

---

## 中信入金通知获取内部账号

### 核心逻辑

```
中信银行入金通知
       ↓
推送的银行卡号（用户充值的卡）
       ↓
查询默认充值配置：default_recharge_flag = 1
       ↓
找到内部账户（D账户）
       ↓
充值上账到D账户的04子账户
```

### SQL 查询

```sql
SELECT * FROM bas_company_info bas
WHERE EXISTS (
    SELECT 1 FROM bas_bank_info bi
    WHERE bi.status = 'N'
      AND bi.default_recharge_flag = 1      -- 默认充值标志
      AND bi.com_code = bas.com_code
      AND bi.account_no_enc = #{银行卡号}    -- 银行卡号（加密）
)
AND bas.platform_com_code = #{平台商户号}
AND bas.status = 'N'
ORDER BY bas.create_time ASC;               -- 取第一条
```

### 查询条件

| 字段 | 说明 |
|------|------|
| `default_recharge_flag = 1` | **默认充值标志**（绑卡加白成功时设置） |
| `account_no_enc` | 银行卡号（加密存储） |
| `platform_com_code` | 平台商户号 |
| `status = 'N'` | 正常状态 |

### 关键点

1. **入金通知是根据绑卡时的 `default_recharge_flag` 标志来判断**哪个账户接收充值
2. 加白成功 → `default_recharge_flag = 1`
3. 无论用户用哪张卡充值，最终都上到**默认充值账户（D账户）**的04子账户

### 多卡充值场景

| 场景 | 结果 |
|------|------|
| 用户绑卡A + 绑卡B + 绑卡C | 3张卡共用同一个充值账户D |
| 用户用卡B充值 | 充值到**卡D**的04子账户 |
| 用户用卡C充值 | 同上 |

---

## 总结

1. **同一个法人**：用法人证件号（corpIdNo）判断是否已开通充值账户
2. **充值账户是独立的**：以"RE"开头创建，与消费账户分离
3. **多消费账户共享**：A/B/C账户共用同一个充值账户D
4. **入金上账**：无论用哪张卡充值，最终都上到D账户的04子账户

