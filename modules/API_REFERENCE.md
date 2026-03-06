# fund-catering API 接口文档

## 概述

fund-catering 项目提供了完整的餐饮金融服务 API，包括账户管理、交易处理、查询服务等。

---

## 一、基础服务模块 API (base)

### 1.1 账户服务接口 (AccountServiceFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 企业注册 | `scRegistration` | POST | 供应链企业注册 |
| 查询注册结果 | `queryScRegistrationResult` | POST | 查询注册结果 |
| 创建账户 | `createScAccount` | POST | 创建供应链账户 |
| 更新账户 | `updateScAccount` | POST | 更新供应链账户 |
| 绑定银行 | `scBindBank` | POST | 绑定银行账户 |
| 解绑银行 | `scUnBindBank` | POST | 解绑银行账户 |
| 激活银行账户 | `scActiveBankAccount` | POST | 激活银行账户 |
| 查询账户明细列表 | `queryAccountDetailList` | POST | 查询账户明细列表 |
| 查询提现账户 | `scQueryWithdrawAccount` | POST | 查询可提现账户 |
| 查询账户信息 | `queryScAccountInfo` | POST | 查询账户信息 |
| 查询账户列表 | `queryScAccountList` | POST | 查询账户列表 |
| 查询账户详情 | `queryScAccountDetail` | POST | 查询账户详情 |
| 更新账户信息 | `scUpdateAccountInfo` | POST | 更新账户信息 |
| 注销账户 | `scAccountCancellation` | POST | 注销账户 |
| 修改提现规则 | `scChangeWithdrawRule` | POST | 修改提现规则 |
| 修改默认银行 | `scChangeDefaultBank` | POST | 修改默认银行 |
| 删除缓存 | `deleteRedisByUserId` | POST | 删除用户缓存 |
| 更改银行账户 | `scChangeBankAccount` | POST | 更改银行账户 |
| 查询充值账户列表 | `queryRechargeAccountList` | POST | 查询充值账户列表 |
| 激活充值银行账户 | `scActiveRechargeBankAccount` | POST | 激活充值银行账户 |
| 更新充值银行账户 | `scUpdateRechargeBankAccount` | POST | 更新充值银行账户 |
| 白名单服务 | `scWhiteName` | POST | 白名单服务 |

### 1.2 商户服务接口 (BaseMerchantFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询会员卡关系列表 | `queryMemberCardRelationListByReq` | POST | 查询会员卡关系列表 |
| 批量查询商户信息 | `queryMerchantInfoByCodes` | POST | 根据商户代码批量查询 |
| 查询商户信息 | `queryMerchantInfoByCode` | POST | 根据商户代码查询 |
| 根据ID选择 | `selectByMerchantIdCodes` | POST | 根据商户ID代码选择 |
| 检查卡BIN使用 | `checkCardBinUsed` | POST | 检查卡BIN是否被使用 |
| 插入商户信息 | `insertMerchantInfo` | POST | 插入商户信息 |
| 更新商户信息 | `updateMerchantInfoByCode` | POST | 更新商户信息 |
| 查询商户列表 | `queryMerchantList` | POST | 查询商户列表 |

### 1.3 平台服务接口 (BasePlatformFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询结算信息 | `querySettleInfoByOperator` | POST | 根据操作员查询结算信息 |
| 批量查询结算信息 | `querySettleInfosByOperator` | POST | 批量查询结算信息 |
| 查询平台信息缓存 | `queryOrgPlatformInfoCache` | POST | 查询组织平台信息缓存 |
| 获取结算信息 | `getOrgSettleInfo` | POST | 获取组织结算信息 |
| 初始化缓存 | `initCache` | POST | 初始化缓存 |
| 根据商户代码获取结算信息 | `getOrgSettleInfoByMerchantCode` | POST | 根据商户代码获取结算信息 |

### 1.4 卡模板服务接口 (BasCardTemplateFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 根据组织代码查询卡模板 | `queryCardTemplateByOrgCode` | POST | 根据组织代码查询卡模板 |
| 分页查询卡模板 | `queryCardTemplateByPage` | POST | 分页查询卡模板 |
| 根据ID查询卡模板 | `queryCardTemplateById` | POST | 根据ID查询卡模板 |
| 添加卡模板 | `addCardTemplate` | POST | 添加卡模板 |
| 编辑卡模板 | `editCardTemplate` | POST | 编辑卡模板 |
| 审核卡模板 | `auditCardTemplate` | POST | 审核卡模板 |
| 取消卡模板 | `cancelCardTemplate` | POST | 取消卡模板 |
| 查询卡模板 | `queryCardTemplate` | POST | 查询卡模板 |
| 根据IDs查找卡模板 | `findCardTemplateByIds` | POST | 根据IDs查找卡模板 |

---

## 二、消费服务模块 API (consume)

### 2.1 交易控制器 (TransConsumeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 提现交易 | `transWithDrawTrans` | POST | 提现交易 |
| 转账交易 | `transTransfer` | POST | 转账交易 |
| 内部转账 | `transTransferInner` | POST | 内部转账 |
| 内部转账预处理 | `transTransferInnerPre` | POST | 内部转账预处理 |
| TI预转账 | `transTransferTiPre` | POST | TI预转账 |
| 转账认证 | `transTransferAuth` | POST | 转账认证 |
| 重发验证码 | `reSendVerification` | POST | 重发验证码 |
| 充值交易 | `rechargeTrans` | POST | 充值交易 |
| 充值退款 | `refundRechargeTrans` | POST | 充值退款 |
| 更新提现交易银行状态 | `updateWithDrawTransBankStatus` | POST | 更新提现交易银行状态 |
| 消费交易 | `transConsume` | POST | 消费交易 |
| 免费消费交易 | `transConsumeFree` | POST | 消费交易（免费） |
| 消费认证 | `transConsumeAuth` | POST | 消费认证 |
| 消费退款 | `transConsumeRefund` | POST | 消费退款 |
| 关闭消费 | `closeConsume` | POST | 关闭消费 |
| 账户冻结 | `transAccountFrozen` | POST | 账户冻结 |
| 账户解冻 | `transAccountUFrozen` | POST | 账户解冻 |
| 免费消费预处理 | `transConsumeFreePre` | POST | 免费消费预处理 |
| 免费消费预处理完成 | `transConsumeFreePreFinish` | POST | 免费消费预处理完成 |
| 消费计算 | `transConsumeCal` | POST | 消费计算 |
| 消费计算订单 | `transConsumeCalOrder` | POST | 消费计算订单 |
| 批量数据消费 | `transConsumeFreeForDataBatch` | POST | 批量数据消费（免费） |
| 批量数据转账 | `transTransferForDataBatch` | POST | 批量数据转账 |

### 2.2 异常处理控制器 (AbnormalProcessController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 插入异常交易明细日志 | `insertAbnomalTransDetailLog` | POST | 插入异常交易明细日志 |
| 插入异常卡余额日志 | `insertAbnomalCardBalanceLog` | POST | 插入异常卡余额日志 |
| 插入交易撤回明细 | `insertTransRecallDetail` | POST | 插入交易撤回明细 |
| 更新交易撤回明细 | `updateTransRecallDetail` | POST | 更新交易撤回明细 |
| 查询撤回明细数量 | `selectRecallDetailCount` | POST | 查询撤回明细数量 |
| 根据交易号更新撤回明细 | `updateTransRecallDetailByTransNo` | POST | 根据交易号更新撤回明细 |
| 批量插入ZX账户变动明细 | `batchInsertZxAcctChangeDetail` | POST | 批量插入ZX账户变动明细 |
| 批量更新异常卡余额日志 | `batchUpdateAbnormalCardBalanceLog` | POST | 批量更新异常卡余额日志 |
| 批量更新异常交易日志 | `batchUpdateAbnormalTransLog` | POST | 批量更新异常交易日志 |
| 查询异常交易明细数量 | `selectAbnomalTransDetailCount` | POST | 查询异常交易明细数量 |
| 查询异常卡余额日志数量 | `selectAbnomalCardBalanceLogCount` | POST | 查询异常卡余额日志数量 |

---

## 三、前置服务模块 API (front)

### 3.1 账户门面控制器 (FrontAccountFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 检查提现 | `checkWithdraw` | POST | 检查提现 |
| 绑定卡片 | `bindCard` | POST | 绑定卡片 |
| 白名单服务 | `whiteName` | POST | 白名单服务 |
| 解绑卡片 | `unbindCard` | POST | 解绑卡片 |
| 开户 | `openAccount` | POST | 开户 |
| 更新账户信息 | `updateAccountInfo` | POST | 更新账户信息 |
| 账户关闭 | `acctClose` | POST | 账户关闭 |

### 3.2 消费交易控制器 (FrontTransConsumeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 转账交易 | `transTransfer` | POST | 转账交易 |
| 转账认证 | `transTransferAuth` | POST | 转账认证 |
| 提现交易 | `transWithDraw` | POST | 提现交易 |
| 消费交易 | `transConsume` | POST | 消费交易 |
| 消费取消 | `transConsumeCancel` | POST | 消费取消 |
| 转账撤回 | `transTransferRecall` | POST | 转账撤回 |

### 3.3 PA平台控制器 (FrontTransPaFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| PA平台交易查询 | `/front/pa/query` | POST | PA平台交易查询 |
| PA平台交易明细 | `/front/pa/detail` | POST | PA平台交易明细 |

### 3.4 ZX平台控制器 (FrontTransZxFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| ZX平台交易查询 | `/front/zx/query` | POST | ZX平台交易查询 |
| ZX平台交易明细 | `/front/zx/detail` | POST | ZX平台交易明细 |

---

## 四、Web 模块 API (web)

### 4.1 账户控制器 (AccountController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 企业注册 | `scRegistration` | POST /api/ums/catering/base/account/scRegistration | 供应链企业注册 |
| 创建账户 | `createScAccount` | POST /api/ums/catering/base/account/createScAccount | 创建供应链账户 |
| 更新账户 | `updateScAccount` | POST /api/ums/catering/base/account/updateScAccount | 更新供应链账户 |
| 激活银行账户 | `scActiveBankAccount` | POST /api/ums/catering/base/account/scActiveBankAccount | 激活银行账户 |
| 绑定银行 | `scBindBank` | POST /api/ums/catering/base/account/scBindBank | 绑定银行账户 |
| 解绑银行 | `scUnBindBank` | POST /api/ums/catering/base/account/scUnBindBank | 解绑银行账户 |
| 更新账户信息 | `scUpdateAccountInfo` | POST /api/ums/catering/base/account/scUpdateAccountInfo | 更新账户信息 |
| 注销账户 | `scAccountCancellation` | POST /api/ums/catering/base/account/scAccountCancellation | 注销账户 |

### 4.2 账户查询控制器 (AccountQueryController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询注册结果 | `queryScRegistrationResult` | POST /api/ums/catering/query/queryScRegistrationResult | 查询注册结果 |
| 查询创建账户结果 | `queryCreateScAccountResultEx` | POST /api/ums/catering/query/queryCreateScAccountResultEx | 查询创建账户结果 |
| 查询账户列表 | `queryScAccountList` | POST /api/ums/catering/query/queryScAccountList | 查询账户列表 |
| 查询账户详情 | `queryScAccountDetail` | POST /api/ums/catering/query/queryScAccountDetail | 查询账户详情 |
| 查询账户明细列表 | `queryAccountDetailList` | POST /api/ums/catering/query/queryAccountDetailList | 查询账户明细列表 |
| 查询账户信息 | `queryScAccountInfo` | POST /api/ums/catering/query/queryScAccountInfo | 查询账户信息 |
| 查询提现账户 | `scQueryWithdrawAccount` | POST /api/ums/catering/query/scQueryWithdrawAccount | 查询可提现账户 |
| 查询充值账户列表 | `queryRechargeAccountList` | POST /api/ums/catering/query/queryRechargeAccountList | 查询充值账户列表 |

### 4.3 充值交易控制器 (RechargeTransController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 充值交易 | `rechargeTrans` | POST /api/ums/catering/trans/rechargeTrans | 充值交易 |

### 4.4 消费查询控制器 (ConsumeQueryController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询消费结果 | `/api/ums/catering/query/consumeResult` | POST | 查询消费结果 |

### 4.5 提现查询控制器 (WithDrawQueryController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询提现结果 | `/api/ums/catering/query/withdrawResult` | POST | 查询提现结果 |

### 4.6 转账查询控制器 (TransferQueryController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询转账结果 | `/api/ums/catering/query/transferResult` | POST | 查询转账结果 |

---

## 五、管理模块 API (management)

### 5.1 配置服务控制器 (ConfigServiceFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 重置配置 | `resetConfig` | POST | 重置配置 |

### 5.2 门店合同控制器 (StoreContractFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取所有FZ商户合同信息 | `getAllFZMpStoreContractInfo` | POST | 获取所有FZ商户合同信息 |
| 根据IDs获取FZ商户合同信息 | `getAllFZMpStoreContractInfoByIds` | POST | 根据IDs获取FZ商户合同信息 |

### 5.3 结算服务控制器 (SettlementServiceFacadeController)

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询清算明细 | `queryCleanSettlementDetail` | POST | 查询清算明细 |
| 查询清算汇总 | `queryCleanSettlementSum` | POST | 查询清算汇总 |

---

## 六、通用响应格式

### 6.1 成功响应

```json
{
  "code": "0000",
  "msg": "成功",
  "model": { ... }
}
```

### 6.2 失败响应

```json
{
  "code": "1001",
  "msg": "账户不存在",
  "model": null
}
```

---

## 七、错误码列表

| 错误码 | 错误描述 |
|--------|----------|
| 0000 | 成功 |
| 1001 | 参数错误 |
| 1002 | 账户不存在 |
| 1003 | 账户状态异常 |
| 1004 | 余额不足 |
| 1005 | 商户不存在 |
| 1006 | 商户状态异常 |
| 1007 | 银行信息不存在 |
| 1008 | 卡BIN无效 |
| 1009 | 平台信息不存在 |
| 1010 | 业务信息不存在 |
| 2001 | 活动不存在 |
| 2002 | 活动已过期 |
| 2003 | 活动未开始 |
| 2004 | 活动预算不足 |
| 2005 | 用户次数超限 |
| 9999 | 系统异常 |

---

**文档生成时间**: 2026-03-06
**API版本**: 1.0
