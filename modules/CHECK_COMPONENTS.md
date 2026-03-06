# Check 组件详细文档

## 概述

Check 组件是 LiteFlow 流程中的校验组件，负责在交易流程中进行各种业务规则校验。所有 Check 组件位于 `flow/component/check` 目录下。

---

## 一、Check 组件列表

### 1.1 BaseOrderInfoCheck（基础订单信息校验）

**Bean 名称**: `baseOrderInfoCheck`

**功能描述**: 基础订单信息检查组件

**主要逻辑**:
- 实现 LiteFlow 的 NodeComponent 接口
- 接收 BaseOrderInfoCheckVo 请求参数
- 记录开始和结束日志，包含交易号(transNo)
- 设置成功状态码和消息
- **当前实现为空检查**，直接返回成功

**代码位置**:
```
fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/check/BaseOrderInfoCheck.java
```

---

### 1.2 BaseOrderInfoCheckPack（基础订单信息打包）

**Bean 名称**: `baseOrderInfoCheckPack`

**功能描述**: 基础订单信息数据打包组件

**主要逻辑**:
- 将传入的请求信息填充到 TransSlot 中
- 设置交易号(transNo)
- 处理支付卡号和收款卡号列表
- 设置商户信息到 merchantInfos Map
- 设置支持的子账户类型（02-膨胀金子账户，04-现金子账户）

**代码位置**:
```
fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/check/BaseOrderInfoCheckPack.java
```

---

## 二、Check 组件调用链

### 2.1 消费交易校验链

```xml
<chain name="chainConsume">
    THEN(
        consumeTransPack,
        accountCheck,          # 账户校验
        merchantCheck,         # 商户校验
        companyCheck,          # 公司校验
        bankInfoCheck,         # 银行信息校验
        accountBankInfoCheck,  # 账户银行信息校验
        cardBinInfoCheck,      # 卡BIN校验
        bussinessInfoCheck,    # 业务信息校验
        platformInfoCheck,     # 平台信息校验
        consumeTransDeductionSplitPack,
        baseChainConsumeSubTypeRoute
    );
</chain>
```

### 2.2 充值交易校验链

```xml
<chain name="chainRecharge">
    THEN(
        rechargeTransPack,
        accountCheck,          # 账户校验
        merchantCheck,         # 商户校验
        companyCheck,          # 公司校验
        bussinessInfoCheck,    # 业务信息校验
        cardBinInfoCheck,      # 卡BIN校验
        rechargeActivityInfoCheck,  # 充值活动校验
        rechargeTrans,
        rechargeTransAfter
    );
</chain>
```

### 2.3 转账交易校验链

```xml
<chain name="chainTransfer">
    THEN(
        transferTransPack,
        accountCheck,          # 账户校验
        merchantCheck,         # 商户校验
        companyCheck,          # 公司校验
        bussinessInfoCheck,    # 业务信息校验
        cardBinInfoCheck,      # 卡BIN校验
        accountBankInfoCheck,  # 账户银行信息校验
        platformInfoCheck,     # 平台信息校验
        SWITCH(transferTrans).TO(transferTransAfter,transferTransSendVerify)
    );
</chain>
```

### 2.4 提现交易校验链

```xml
<chain name="chainWithDraw">
    THEN(
        withDrawTransPack,
        accountCheck,          # 账户校验
        merchantCheck,         # 商户校验
        companyCheck,          # 公司校验
        bussinessInfoCheck,    # 业务信息校验
        cardBinInfoCheck,      # 卡BIN校验
        bankPayInfoCheck,      # 银行付款信息校验
        accountBankPayInfoCheck,  # 账户银行付款信息校验
        platformInfoCheck,     # 平台信息校验
        withDrawTrans,
        withDrawTransAfter
    );
</chain>
```

---

## 三、通用 Check 组件说明

### 3.1 accountCheck（账户校验）

**功能描述**:
- 校验卡号是否存在
- 校验卡状态是否正常
- 校验子账户是否可用
- 校验账户余额是否充足

**校验规则**:
1. 卡号不能为空
2. 卡号必须存在于系统中
3. 卡状态必须是正常状态（N-正常）
4. 子账户类型必须有效
5. 交易金额不能超过账户余额

### 3.2 merchantCheck（商户校验）

**功能描述**:
- 校验商户号是否存在
- 校验商户状态是否正常
- 校验商户权限

**校验规则**:
1. 商户号不能为空
2. 商户号必须存在于系统中
3. 商户状态必须是正常状态（N-正常）
4. 商户必须有所需的业务权限

### 3.3 companyCheck（公司校验）

**功能描述**:
- 校验公司信息是否存在
- 校验公司状态是否正常
- 校验公司权限

**校验规则**:
1. 公司信息不能为空
2. 公司必须存在于系统中
3. 公司状态必须是正常状态
4. 公司必须有所需的业务权限

### 3.4 bankInfoCheck（银行信息校验）

**功能描述**:
- 校验银行信息是否存在
- 校验银行状态是否正常

**校验规则**:
1. 银行信息不能为空
2. 银行必须存在于系统中
3. 银行状态必须是正常状态

### 3.5 accountBankInfoCheck（账户银行信息校验）

**功能描述**:
- 校验账户银行信息是否存在
- 校验账户银行信息是否有效

**校验规则**:
1. 账户银行信息不能为空
2. 账户银行信息必须存在于系统中
3. 账户银行信息必须有效

### 3.6 cardBinInfoCheck（卡BIN校验）

**功能描述**:
- 校验卡BIN是否有效
- 校验卡BIN所属银行

**校验规则**:
1. 卡BIN不能为空
2. 卡BIN必须存在于系统中
3. 卡BIN必须是有效的

### 3.7 bussinessInfoCheck（业务信息校验）

**功能描述**:
- 校验业务信息是否存在
- 校验业务信息是否有效

**校验规则**:
1. 业务信息不能为空
2. 业务信息必须存在于系统中
3. 业务信息必须有效

### 3.8 platformInfoCheck（平台信息校验）

**功能描述**:
- 校验平台信息是否存在
- 校验平台状态是否正常

**校验规则**:
1. 平台信息不能为空
2. 平台必须存在于系统中
3. 平台状态必须是正常状态

---

## 四、专用 Check 组件

### 4.1 rechargeActivityInfoCheck（充值活动校验）

**功能描述**:
- 校验充值活动是否存在
- 校验充值活动是否有效
- 校验充值活动是否在有效期内

**校验规则**:
1. 活动编码不能为空
2. 活动必须存在于系统中
3. 活动状态必须是正常状态（N-正常）
4. 当前时间必须在活动有效期内
5. 活动预算是否充足
6. 用户参加次数是否超限

### 4.2 bankPayInfoCheck（银行付款信息校验）

**功能描述**:
- 校验银行付款信息是否存在
- 校验银行付款信息是否有效

**校验规则**:
1. 银行付款信息不能为空
2. 银行付款信息必须存在于系统中
3. 银行付款信息必须有效

### 4.3 accountBankPayInfoCheck（账户银行付款信息校验）

**功能描述**:
- 校验账户银行付款信息是否存在
- 校验账户银行付款信息是否有效

**校验规则**:
1. 账户银行付款信息不能为空
2. 账户银行付款信息必须存在于系统中
3. 账户银行付款信息必须有效

### 4.4 refundRechargeActivityInfoCheck（充值退款活动校验）

**功能描述**:
- 校验充值退款活动是否存在
- 校验充值退款活动是否有效

**校验规则**:
1. 活动信息不能为空
2. 活动必须存在于系统中
3. 活动必须是退款类型的活动

---

## 五、Check 组件错误码

### 5.1 通用错误码

| 错误码 | 错误描述 | 说明 |
|--------|----------|------|
| 1001 | 账户不存在 | 卡号在系统中不存在 |
| 1002 | 账户状态异常 | 卡状态不是正常状态 |
| 1003 | 余额不足 | 账户余额不足以完成交易 |
| 1004 | 商户不存在 | 商户号在系统中不存在 |
| 1005 | 商户状态异常 | 商户状态不是正常状态 |
| 1006 | 公司不存在 | 公司信息不存在 |
| 1007 | 银行信息不存在 | 银行信息不存在 |
| 1008 | 卡BIN无效 | 卡BIN不存在或无效 |
| 1009 | 平台信息不存在 | 平台信息不存在 |
| 1010 | 业务信息不存在 | 业务信息不存在 |

### 5.2 充值相关错误码

| 错误码 | 错误描述 | 说明 |
|--------|----------|------|
| 2001 | 活动不存在 | 充值活动不存在 |
| 2002 | 活动已过期 | 充值活动已过期 |
| 2003 | 活动未开始 | 充值活动未开始 |
| 2004 | 活动预算不足 | 活动预算已用完 |
| 2005 | 用户次数超限 | 用户参加次数已达上限 |

---

## 六、Check 组件开发规范

### 6.1 命名规范

- 组件类名: `{校验项}Check`
- Bean 名称: `{校验项}Check`（首字母小写）

### 6.2 实现规范

```java
@Component("{checkName}")
@Slf4j
public class XxxCheck extends NodeComponent {

    @Override
    public void process() throws Exception {
        TransSlot slot = this.getFirstContextBean();

        // 1. 获取校验参数
        // 2. 执行校验逻辑
        // 3. 校验失败抛出异常
        // 4. 校验成功正常返回
    }

    @Override
    public boolean isAccess() {
        // 判断是否需要执行此组件
        return true;
    }
}
```

### 6.3 异常处理规范

```java
// 校验失败使用 BaseASException
throw new BaseASException(BaseErrorCodeEnum.ACCOUNT_NOT_EXIST.code(),
                         BaseErrorCodeEnum.ACCOUNT_NOT_EXIST.message());

// 或使用 BaseException
throw new BaseException(BaseErrorCodeEnum.ACCOUNT_NOT_EXIST.code(),
                       BaseErrorCodeEnum.ACCOUNT_NOT_EXIST.message());
```

---

**文档生成时间**: 2026-03-06
**扫描范围**: fund-catering-consume 模块 check 组件
