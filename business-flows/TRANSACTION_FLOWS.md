# lsym 业务流程详解

## 概述

lsym 项目的核心业务是餐饮资金交易处理，通过 LiteFlow 流程编排引擎实现业务逻辑的可视化和可维护性。

## 核心流程链

| 流程链名称 | 说明 | Controller |
|------------|------|------------|
| chainConsume | 消费交易流程 | TransConsumeController |
| chainConsumeAuth | 授权消费流程 | TransConsumeController |
| chainConsumeRefund | 消费退款流程 | TransConsumeController |
| chainRecharge | 充值交易流程 | TransRechargeController |
| chainRefundRecharge | 充值退款流程 | TransRechargeController |
| chainTransfer | 转账交易流程 | TransTransferController |
| chainTransferAuth | 授权转账流程 | TransTransferController |
| chainWithDraw | 提现交易流程 | TransWithDrawController |
| chainFrozen | 冻结交易流程 | - |
| chainUnFrozen | 解冻交易流程 | - |
| chainConsumePre | 预消费流程 | TransConsumeController |
| chainConsumeCal | 订单金额计算流程 | TransConsumeController |

## 交易类型

### 子账户类型

| 代码 | 名称 | 说明 |
|------|------|------|
| 01 | 现金账户 | 可提现 |
| 02 | 膨胀金账户 | 赠送金额，优先消费，不可提现 |
| 04 | 综合账户 | 综合子账户 |

## 流程组件类型

| 组件类型 | 命名规范 | 职责 | 示例 |
|----------|----------|------|------|
| Pack | {业务}TransPack | 数据打包、参数校验、初始化 | ConsumeTransPack |
| Check | {校验项}Check | 业务规则校验 | accountCheck, merchantCheck |
| Trans | {业务}Trans{类型} | 核心交易处理、写库 | ConsumeTrans01, RechargeTrans |
| After | {业务}TransAfter | 后处理、账户变动明细 | ConsumeTransAfter |
| Route | {业务}Route | 路由、分支逻辑 | consumeTransSubTypeRoute |

## 通用流程

所有交易流程都遵循以下通用链路结构：

```
Pack → Check → Trans → After
  │        │       │       │
  │        │       │       └── 账户变动明细
  │        │       └── 核心交易处理
  │        └── 业务规则校验
  └── 数据打包+参数校验
```

## 快速参考

### 消费交易
- **入口**: `/scConsume`, `/scConsumeFree`, `/scConsumeAuth`
- **特点**: 支持02膨胀金优先扣款，支持分账消费
- **异步上账**: 收款卡锁获取失败时异步处理

### 充值交易
- **入口**: `/scRecharge`, `/scRecharge02`
- **特点**: 支持01现金账户充值，02膨胀金赠送
- **膨胀金**: 按活动规则赠送，消费时优先扣减

### 充值退款
- **入口**: `/scRefundRecharge`
- **特点**: 原路退回，膨胀金按比例收回
- **期限**: 7天内可退款

### 提现交易
- **入口**: `/scWithdraw`
- **特点**: 支持自动提现和人工审核
- **自动条件**: 金额<1000元，信用分>700，老用户

### 转账交易
- **入口**: `/scTransfer`, `/scTransOnAccount`, `/scTransOnAccountAuth`
- **特点**: 支持批量转账(TI)，三层锁机制
- **锁机制**: 批次锁(10分钟) → 付款卡锁(30分钟) → 收款卡锁(10分钟)

### 消费退款
- **入口**: `/scConsumeRefund`
- **特点**: 支持按比例退款和按单退款
- **期限**: 30天内可退款
- **膨胀金**: 按原消费比例退回02账户

## 飞书文档

最新完整设计文档：https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f
