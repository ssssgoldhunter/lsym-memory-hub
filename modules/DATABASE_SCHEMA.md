# 数据库表结构文档

## 概述

本文档描述 fund-catering 项目的核心数据库表结构。

---

## 一、核心账户相关表

### 1.1 BasCardAccountT（主账户表）

**表名**: `bas_card_account_t`

**表说明**: 卡主账户表，存储卡片账户的基本信息

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| account_id | BIGINT | 卡主账户ID | 自增主键 |
| card_code | VARCHAR(32) | 卡号 | 编码规则：7位卡BIN+2位面额码+1位是否有密码+8位顺序号+1位校验位 |
| platform_code | VARCHAR(10) | 平台编码 | - |
| card_batch_no | VARCHAR(32) | 发卡批次号 | - |
| first_org_code | VARCHAR(4) | 一级机构号 | 4位数字 |
| second_org_code | VARCHAR(4) | 二级机构号 | - |
| merchant_code | VARCHAR(16) | 商户号 | 310+4位机构号+补0+7位门店号 |
| status | VARCHAR(2) | 卡状态 | N-正常 O-待激活 CE-作废 AR-退卡 L-锁定 F-冻结 R-挂失 E-过期 |
| open_date | DATE | 开卡日期 | - |
| active_date | DATE | 激活日期 | - |
| exp_date | DATE | 到期日期 | - |
| if_recharge | VARCHAR(1) | 是否可以充值 | 1-是 0-否 |
| denomination | BIGINT | 面值 | 单位：分 |
| passwd_free | VARCHAR(1) | 免密开关 | A-开启 D-关闭 |
| if_have_passwd | VARCHAR(1) | 是否有密码 | 1-是 0-否 |
| passwd | VARCHAR(128) | 密码 | 不可逆密文存储 |
| mac | VARCHAR(64) | MAC值 | 用于数据校验 |

### 1.2 BasCardSubAccountT（子账户表）

**表名**: `bas_card_sub_account_t`

**表说明**: 卡子账户表，存储卡片的子账户信息

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| sub_account_id | BIGINT | 子账户ID | 自增主键 |
| account_id | BIGINT | 主账户ID | - |
| card_code | VARCHAR(32) | 卡号 | - |
| sub_account_type | VARCHAR(2) | 子账户类型 | 01-现金子账户 02-膨胀金子账户 |
| balance | BIGINT | 余额 | 单位：分 |
| real_balance | BIGINT | 本金余额 | 单位：分 |
| frozen_amt | BIGINT | 冻结金额 | 单位：分 |
| withdraw_balance | BIGINT | 可提现金额 | 单位：分 |
| wait_release_balance | BIGINT | 待释放可提现金额 | 单位：分 |
| last_change_time | TIMESTAMP | 余额最后变更时间 | - |
| mac | VARCHAR(64) | MAC值（旧值） | 用于CAS校验 |
| newMac | - | 新MAC值 | 非数据库字段，用于CAS更新 |

---

## 二、商户相关表

### 2.1 BasMerchantT（商户表）

**表名**: `bas_merchant_t`

**表说明**: 商户表，存储商户的基本信息

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| id | BIGINT | 商户ID | 自增主键 |
| merchant_code | VARCHAR(16) | 商户号 | 310+4位机构号+补0+7位门店号 |
| merchant_name | VARCHAR(128) | 商户名称 | - |
| merchant_describe | VARCHAR(512) | 商户描述 | - |
| org_code | VARCHAR(4) | 机构号 | 二级发卡机构 |
| shop_id | VARCHAR(7) | 业务方门店ID | 7位数字 |
| status | VARCHAR(2) | 商户状态 | O-待生效 N-正常 P-禁用 D-失效 |
| audit_status | VARCHAR(2) | 审核状态 | O-待审核 P-通过 R-拒绝 |

---

## 三、会员相关表

### 3.1 BasMemberCardRelationT（会员关系表）

**表名**: `bas_member_card_relation_t`

**表说明**: 会员关系表，存储会员与卡片的关联关系

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| member_id | BIGINT | 会员ID | 自增主键 |
| member_type | VARCHAR(2) | 会员类型 | 0-个人 1-家庭 |
| user_id | VARCHAR(32) | 第三方用户ID | - |
| merchant_code | VARCHAR(16) | 商户号 | - |
| status | VARCHAR(2) | 会员状态 | N-正常 L-锁定 D-失效 E-过期 |
| acct_id | BIGINT | 会员关联的主账户ID | - |
| card_code | VARCHAR(32) | 会员关联的卡号 | - |
| channel_type | VARCHAR(16) | 渠道类型 | MWallet-麦钱包 MCash-现金卡 |
| wx_card_id | VARCHAR(64) | 微信卡包CardID | - |
| wx_card_code | VARCHAR(64) | 微信卡包CardCode | - |
| if_bind | VARCHAR(1) | 是否已绑定个人信息 | 0-否 1-是 |
| phone | VARCHAR(11) | 手机号 | - |

---

## 四、活动相关表

### 4.1 BasActivityT（活动表）

**表名**: `bas_activity_t`

**表说明**: 活动表，存储营销活动的基本信息

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| activity_id | BIGINT | 活动ID | 自增主键 |
| activity_code | VARCHAR(32) | 活动编码 | - |
| activity_category | VARCHAR(4) | 活动种类 | EX-膨胀金活动 PA-支付立减活动 |
| activity_type_id | BIGINT | 活动类型ID | - |
| activity_name | VARCHAR(128) | 活动名称 | - |
| activity_desc | VARCHAR(512) | 活动规则描述 | - |
| activity_budget_amt | BIGINT | 活动预算 | 单位：分 |
| start_date | TIMESTAMP | 活动起始时间 | - |
| end_date | TIMESTAMP | 活动截止时间 | - |
| user_nums_limit | INT | 用户上限 | - |
| user_times_limit | INT | 单用户最大参加次数 | - |
| access_user_nums | INT | 已参加活动用户数量 | - |
| access_amt | BIGINT | 已使用活动预算 | 单位：分 |
| activity_status | VARCHAR(2) | 活动状态 | O-待生效 N-正常 P-禁用 C-作废 E-结束 |
| sub_account_type | VARCHAR(2) | 子账户类型 | 01-现金子账户 02-膨胀金子账户 |

---

## 五、交易流水表

### 5.1 BasCardActiveFlowT（卡片激活流水表）

**表名**: `bas_card_active_flow_t`

**表说明**: 卡片激活流水表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| flow_id | BIGINT | 流水ID | 自增主键 |
| card_code | VARCHAR(32) | 卡号 | - |
| active_date | DATE | 激活日期 | - |
| active_amt | BIGINT | 激活金额 | 单位：分 |
| merchant_code | VARCHAR(16) | 商户号 | - |
| active_type | VARCHAR(4) | 激活类型 | - |
| trans_no | VARCHAR(32) | 交易号 | - |
| pay_type | VARCHAR(4) | 支付方式 | - |
| pay_amt | BIGINT | 支付金额 | 单位：分 |

---

## 六、其他重要表

### 6.1 BasAccountBankInfoT（账户银行信息表）

**表名**: `bas_account_bank_info_t`

**表说明**: 账户银行信息表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| id | BIGINT | 主键ID | 自增主键 |
| account_id | BIGINT | 主账户ID | - |
| card_code | VARCHAR(32) | 卡号 | - |
| bank_code | VARCHAR(16) | 银行编码 | - |
| bank_name | VARCHAR(128) | 银行名称 | - |
| bank_account_no | VARCHAR(64) | 银行账号 | - |
| bank_account_name | VARCHAR(128) | 银行账户名 | - |
| is_default | VARCHAR(1) | 是否默认 | 1-是 0-否 |

### 6.2 BasBankInfoT（银行信息表）

**表名**: `bas_bank_info_t`

**表说明**: 银行信息表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| bank_code | VARCHAR(16) | 银行编码 | 主键 |
| bank_name | VARCHAR(128) | 银行名称 | - |
| bank_short_name | VARCHAR(32) | 银行简称 | - |

### 6.3 BasOrgT（机构表）

**表名**: `bas_org_t`

**表说明**: 机构表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| org_code | VARCHAR(4) | 机构号 | 主键 |
| org_name | VARCHAR(128) | 机构名称 | - |
| org_level | VARCHAR(2) | 机构级别 | 1-一级 2-二级 |
| parent_org_code | VARCHAR(4) | 上级机构号 | - |

### 6.4 BasOperatorT（运营商表）

**表名**: `bas_operator_t`

**表说明**: 运营商表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| operator_code | VARCHAR(8) | 运营商号 | 主键 |
| operator_name | VARCHAR(128) | 运营商名称 | - |
| org_code | VARCHAR(4) | 机构号 | - |

### 6.5 BasParamT（参数表）

**表名**: `bas_param_t`

**表说明**: 参数表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| param_id | BIGINT | 参数ID | 主键 |
| param_key | VARCHAR(64) | 参数键 | - |
| param_value | VARCHAR(512) | 参数值 | - |
| param_desc | VARCHAR(256) | 参数描述 | - |

### 6.6 BasBankAccountChange（银行账户变更表）

**表名**: `bas_bank_account_change_t`

**表说明**: 银行账户变更表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| change_id | BIGINT | 变更ID | 主键 |
| account_id | BIGINT | 账户ID | - |
| card_code | VARCHAR(32) | 卡号 | - |
| change_type | VARCHAR(4) | 变更类型 | 1-新增 2-修改 3-删除 |
| old_bank_account | VARCHAR(64) | 原银行账号 | - |
| new_bank_account | VARCHAR(64) | 新银行账号 | - |
| change_time | TIMESTAMP | 变更时间 | - |

### 6.7 BasCardBinT（卡BIN表）

**表名**: `bas_card_bin_t`

**表说明**: 卡BIN表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| card_bin | VARCHAR(7) | 卡BIN | 主键 |
| bank_code | VARCHAR(16) | 银行编码 | - |
| card_type | VARCHAR(4) | 卡片类型 | - |
| card_name | VARCHAR(64) | 卡片名称 | - |

### 6.8 BasCardTemplateT（卡模板表）

**表名**: `bas_card_template_t`

**表说明**: 卡模板表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| template_id | BIGINT | 模板ID | 主键 |
| template_code | VARCHAR(16) | 模板编码 | - |
| template_name | VARCHAR(64) | 模板名称 | - |
| card_face | VARCHAR(128) | 卡面 | - |
| status | VARCHAR(2) | 状态 | O-待审核 N-正常 P-禁用 |

### 6.9 BasChannelT（渠道表）

**表名**: `bas_channel_t`

**表说明**: 渠道表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| channel_code | VARCHAR(8) | 渠道编码 | 主键 |
| channel_name | VARCHAR(64) | 渠道名称 | - |
| channel_type | VARCHAR(4) | 渠道类型 | - |

### 6.10 BasCardCancelFlowT（卡注销流水表）

**表名**: `bas_card_cancel_flow_t`

**表说明**: 卡注销流水表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| flow_id | BIGINT | 流水ID | 主键 |
| card_code | VARCHAR(32) | 卡号 | - |
| cancel_date | DATE | 注销日期 | - |
| cancel_type | VARCHAR(4) | 注销类型 | - |
| cancel_reason | VARCHAR(256) | 注销原因 | - |
| trans_no | VARCHAR(32) | 交易号 | - |

---

## 七、账户变动明细表

### 7.1 TransAcctChangeDetailT（账户变动明细表）

**表名**: `trans_acct_change_detail_t`

**表说明**: 账户变动明细表

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| change_detail_id | VARCHAR(32) | 变动明细ID | 主键 |
| account_id | BIGINT | 主账户ID | - |
| card_code | VARCHAR(32) | 卡号 | - |
| sub_account_type | VARCHAR(2) | 子账户类型 | 01-现金 02-膨胀金 |
| sub_account_id | BIGINT | 子账户ID | - |
| acct_expand_id | BIGINT | 膨胀金子账户扩展ID | - |
| trans_time | TIMESTAMP | 交易时间 | - |
| org_amt | BIGINT | 交易前金额 | 单位：分 |
| trans_amt | BIGINT | 交易金额 | 单位：分 |
| balance | BIGINT | 交易后金额 | 单位：分 |
| org_real_amt | BIGINT | 交易前本金余额 | 单位：分 |
| real_balance | BIGINT | 交易后本金余额 | 单位：分 |
| trans_type | VARCHAR(4) | 交易类型 | - |
| trans_no | VARCHAR(32) | 交易单号 | - |

### 7.2 TransAcctSumChangeDetailT（账户汇总变动明细表）

**表名**: `trans_acct_sum_change_detail_t`

**表说明**: 账户汇总级别的变动明细记录

### 7.3 TransAcctActSumChangeDetailT（账户活动汇总变动明细表）

**表名**: `trans_acct_act_sum_change_detail_t`

**表说明**: 账户活动汇总级别的变动明细记录

### 7.4 TransAcctFrozenChangeDetailT（账户冻结变动明细表）

**表名**: `trans_acct_frozen_change_detail_t`

**表说明**: 账户冻结和解冻的变动明细

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| frozen_id | VARCHAR(32) | 冻结ID | 主键 |
| card_code | VARCHAR(32) | 卡号 | - |
| sub_account_type | VARCHAR(2) | 子账户类型 | - |
| frozen_amt | BIGINT | 冻结金额 | 单位：分 |
| trans_no | VARCHAR(32) | 交易单号 | - |
| frozen_type | VARCHAR(4) | 冻结类型 | 1-冻结 2-解冻 |

---

## 八、关键业务特点

### 8.1 MAC机制

关键表（如 `BasCardSubAccountT`）包含 MAC 字段，用于实现乐观锁机制，防止并发更新问题。

**CAS更新逻辑**:
```sql
UPDATE bas_card_sub_account_t
SET balance = #{newBalance},
    mac = #{newMac}
WHERE sub_account_id = #{subAccountId}
  AND mac = #{oldMac}
```

### 8.2 多账户类型

支持现金子账户（01）和膨胀金子账户（02）：
- **01 现金子账户**: 用户充值金额，可提现
- **02 膨胀金子账户**: 赠送金额，优先消费，不可提现

### 8.3 三级机构架构

- **一级机构**: 顶级机构
- **二级机构**: 发卡机构
- **商户**: 具体门店

### 8.4 多渠道支持

- **MWallet**: 麦钱包
- **MCash**: 现金卡

---

**文档生成时间**: 2026-03-06
**数据库版本**: 1.0
