# lsym 架构设计

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        API层                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  消费API  │  │  充值API  │  │  转账API  │  │  提现API  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                      流程编排层 (LiteFlow)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │chainConsume│ │chainRecharge│ │chainTransfer│ │chainWithDraw│  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                        组件层                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Pack组件 → Check组件 → Trans组件 → After组件       │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                        服务层                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │账户服务  │  │商户服务  │  │平台服务  │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                        数据层                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ MySQL    │  │  Redis   │  │  Nacos   │                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

## LiteFlow 流程编排

### 流程链配置

流程链通过 XML 配置文件定义，主要配置文件：
- `consume.el.xml` - 业务处理流程配置
- `query.el.xml` - 业务查询流程配置

### 组件目录结构

```
flow/component/
├── trans/                    # 交易组件
│   ├── consume/              # 消费交易组件
│   ├── recharge/             # 充值交易组件
│   ├── refundRecharge/       # 充值退款组件
│   ├── transfer/             # 转账交易组件
│   └── withDraw/             # 提现交易组件
└── query/                    # 查询组件
```

## 上下文传递设计

### Slot 继承关系

```
BaseSlot (基础上下文)
    ├── TransSlot (交易流程上下文) - extends BaseSlot
    └── QuerySlot (查询流程上下文) - extends BaseSlot
```

### BaseSlot 缓存内容

- `payCardCodes` - 付款卡列表
- `recCardCodes` - 收款卡列表
- `compayInfoMaps` - 公司信息映射
- `basBankInfoMap` - 银行信息映射
- `cardSubAccountMap` - 子账户映射
- `businessInfos` - 业务信息映射
- `merchantInfos` - 商户信息映射

## 安全机制

### MAC 校验
- **目的**: 确保交易数据完整性，防篡改
- **工具类**: ConsumeMacUtils, RechargeMacUtils, TransferMacUtils
- **流程**: 交易前生成MAC → 流程中校验MAC

### 分布式锁
- **实现**: Redis 分布式锁
- **锁键**: cardCode（卡号）
- **超时**: 5分钟
- **目的**: 防止并发交易冲突

### 幂等性保证
- **幂等键**: transNo（交易单号）
- **存储方式**: Redis缓存
- **过期时间**: 1小时
- **目的**: 防止重复扣款/充值

## 数据模型

### 核心交易表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| trans_consume_t | 消费交易主表 | trans_no, card_code, amount, status |
| trans_consume_sub_t | 消费交易子表 | sub_trans_no, trans_no, sub_account_type |
| trans_recharge_t | 充值交易主表 | trans_no, card_code, amount, status |
| trans_recharge_sub_t | 充值交易子表 | sub_trans_no, recharge_type, amount |
| trans_transfer_t | 转账交易主表 | trans_no, pay_card_code, rec_card_code, amount |
| trans_transfer_sub_t | 转账交易子表 | sub_trans_no, trans_no, sub_account_type |
| trans_withdraw_t | 提现交易主表 | trans_no, card_code, amount, status |

### 账户变动明细表

| 表名 | 说明 |
|------|------|
| trans_acct_change_detail_t | 账户变动明细表 |
| trans_acct_sum_change_detail_t | 账户汇总变动明细表 |
| trans_acct_act_sum_change_detail_t | 活动汇总变动明细表 |
| trans_acct_frozen_change_detail_t | 冻结变动明细表 |
| trans_acct_change_entry_detail_t | 待上账明细表 |

## 部署架构

| 项目 | 值 |
|------|------|
| 服务端口 | 8092 |
| 服务名称 | fund-catering-consume |
| 配置中心 | Nacos |
| 注册中心 | Nacos |
