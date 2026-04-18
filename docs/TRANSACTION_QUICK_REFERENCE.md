# lsym 六大交易流程快速参考

> 本文档提供lsym项目中六大核心交易流程的快速参考指南

## 📋 流程概览

### 核心交易流程

| 流程 | 流程链 | 接口路径 | 说明 |
|------|--------|----------|------|
| **消费交易** | chainConsume | /scConsumeFree | 膨胀金优先扣款，支持分账 |
| **充值交易** | chainRecharge | /scRecharge | 支持01现金+02膨胀金赠送 |
| **充值退款** | chainRefundRecharge | /scRefundRecharge | 原路退回，膨胀金收回 |
| **提现交易** | chainWithDraw | /scWithdraw | 自动提现+人工审核 |
| **转账交易** | chainTransfer | /scTransfer | 三层锁机制，支持批量 |
| **消费退款** | chainConsumeRefund | /scConsumeRefund | 按比例/按单退款 |

### 扩展交易流程

| 流程 | 流程链 | 说明 |
|------|--------|------|
| **消费授权** | chainConsumeAuth | 需鉴权消费 |
| **预消费** | chainConsumePre | 预消费冻结 |
| **预消费完成** | chainConsumePreFinish | 预消费确认上账 |
| **消费关闭** | chainConsumeClose | 预消费关闭解冻 |
| **消费算价** | chainConsumeCal | 消费计算 |
| **消费算价订单** | chainConsumeCalOrder | 订单级消费计算 |
| **内部转账** | chainTransferInner | 内部转账 |
| **内部转账预处理** | chainTransferInnerPre | 内部转账预处理 |
| **TI转账预处理** | chainTransferTiPre | TI批量转账预处理 |
| **授权转账** | chainTransferAuth | 授权转账 |
| **转账重发验证** | chainTransferReSendVerification | 转账重发验证码 |
| **冻结** | chainFrozen | 冻结操作 |
| **解冻** | chainUnFrozen | 解冻操作 |

### 查询流程

| 流程 | 流程链 | 说明 |
|------|--------|------|
| **提现结果查询** | chainWithdrawResultQuery | 提现结果查询 |
| **充值结果查询** | chainRechargeResultQuery | 充值结果查询 |
| **冻结明细查询** | chainFrozenDetailQuery | 冻结明细查询 |
| **消费结果查询** | chainConsumeResultQuery | 消费结果查询 |
| **转账结果查询** | chainTransferResultQuery | 转账结果查询 |

---

## 1️⃣ 消费交易流程

### 关键特点
- 02膨胀金账户优先扣款
- 支持分账消费（一卡多收款）
- 异步上账机制（收款卡锁失败时）

### 核心组件
```
consumeTransPack → accountCheck → merchantCheck → ...
→ consumeTransDeductionSplitPack → consumeTransSubTypeRoute
→ bashChainConsume → consumeTrans02Cal → consumeTrans01Cal
→ consumeTransFrozen → consumeTrans04/02/01
→ consumeTransAfter → consumeTransUnFrozen
```

### 子账户扣款顺序
1. **02账户**（膨胀金）→ 优先扣款
2. **01账户**（现金）→ 02不足时扣款
3. **04账户**（综合）→ 按配置处理

---

## 2️⃣ 充值交易流程

### 关键特点
- 支持01现金账户充值
- 支持02膨胀金赠送
- 按活动规则赠送膨胀金

### 膨胀金赠送规则
| 充值金额 | 膨胀金比例 | 赠送金额 |
|----------|------------|----------|
| 满100元 | 20% | 20元 |
| 满200元 | 25% | 50元 |
| 满500元 | 30% | 150元 |

### 核心组件
```
RechargeTransPack → 业务校验 → RechargeTrans
→ 更新账户余额(01+02) → RechargeTransAfter
```

---

## 3️⃣ 充值退款流程 ⭐

### 关键特点
- 原路退回充值金额
- 膨胀金按比例收回
- 支持全额/部分退款

### 业务规则
- **退款期限**: 充值后7天内
- **退款次数**: 支持多次部分退款
- **余额检查**: 账户余额必须充足

### 退款计算
```
原充值: 100元 (01:80元, 02:20元)
全额退款100元:
├── 01账户: -80元
└── 02账户: -20元 (收回膨胀金)

部分退款50元(50%):
├── 01账户: -40元 (80×50%)
└── 02账户: -10元 (20×50%)
```

### 核心组件
```
RefundRechargeTransPack → 业务校验 → 查询原充值
→ 计算退款明细 → RefundRechargeTrans
→ 扣减账户余额 → RefundRechargeTransAfter
```

---

## 4️⃣ 提现交易流程

### 关键特点
- 支持自动提现和人工审核
- 到卡验证（只能提现到本人卡）

### 自动提现条件
| 条件 | 说明 |
|------|------|
| 金额条件 | 提现金额 < 1000元 |
| 信用条件 | 用户信用分 > 700 |
| 用户条件 | 老用户（注册 > 90天） |
| 风控条件 | 风控检查通过 |

### 核心组件
```
WithDrawTransPack → 业务校验 → 余额检查 → 冻结资金
→ 创建提现记录 → 自动提现判断
→ ├─ 满足条件 → 自动审批 → 调用银行接口
└─ 不满足 → 等待人工审核
→ WithDrawTransAfter → 解冻资金
```

---

## 5️⃣ 转账交易流程

### 关键特点
- 支持API转账和内部转账
- 三层锁机制保证并发安全
- 支持批量转账(TI)

### 并发控制设计
| 层级 | 锁对象 | 超时时间 | 作用 |
|------|--------|----------|------|
| 批次级 | batch_no | 10分钟 | 防止重复处理 |
| 付款卡级 | pay_card_code | 30分钟 | 保证卡顺序处理 |
| 收款卡级 | receive_card_code | 10分钟 | 防止并发冲突 |

### 核心组件
```
TransferTransPack → 业务校验 → TransferTransAuth
→ 冻结付款方资金 → 鉴权判断(如需要)
→ 付款方扣款 → 收款方入账 → TransferTransAfter
→ 解冻资金
```

---

## 6️⃣ 消费退款流程

### 关键特点
- 原路退回：01退01，02退02，04退04
- 膨胀金按比例退回02账户
- 支持按比例退款和按单退款

### 退款方式对比
| 退款方式 | 适用场景 | 退款规则 |
|----------|----------|----------|
| **按比例退款** | 统一退款 | 按原消费各账户扣款比例退款 |
| **按单退款** | 多订单部分退款 | 按子订单金额退款 |

### 业务规则
- **退款期限**: 原消费后30天内
- **退款次数**: 支持多次部分退款
- **膨胀金退款**: 按原消费比例退回02账户

### 退款数据流转示例
```
原消费: 100元 (01:70元, 02:25元, 04:5元)
全额退款100元:
├── 01账户: +70元
├── 02账户: +25元 (退回膨胀金)
└── 04账户: +5元

部分退款50元(50%):
├── 01账户: +35元
├── 02账户: +12.5元
└── 04账户: +2.5元
```

### 核心组件
```
consumeTransRefundPack → 业务校验 → 查询原消费
→ consumeTransRefundSplitSwitch (拆分方式路由)
→ 按比例/按单拆分 → bashChainRefundModelSwitch (退款模型)
→ RefundRecharge04/01 → consumeTransRefundAfter
```

---

## 🔧 通用机制

### 分布式锁
- **锁键**: cardCode（卡号）
- **超时**: 5分钟
- **作用**: 防止并发交易冲突

### MAC校验
- **目的**: 确保交易数据完整性
- **工具**: ConsumeMacUtils, RechargeMacUtils, TransferMacUtils

### 幂等性
- **幂等键**: transNo（交易单号）
- **存储**: Redis缓存
- **过期**: 1小时

### LiteFlow组件类型
- **Pack**: 数据打包、参数校验
- **Check**: 业务规则校验
- **Trans**: 核心交易处理
- **After**: 后处理、账户变动明细
- **Route**: 路由、分支逻辑

---

## 📊 性能指标

| 交易类型 | 响应时间(P99) | 吞吐量(TPS) |
|----------|---------------|-------------|
| 消费 | 500ms | 1000 |
| 充值 | 300ms | 500 |
| 提现 | 1000ms | 100 |
| 转账 | 500ms | 300 |
| 消费退款 | 500ms | 200 |

---

## 📖 相关文档

- **完整设计文档**: [供应链交易系统设计文档 v5.5](./SUPPLY_CHAIN_DESIGN_V5.5.md)
- **飞书文档**: https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f
- **项目路径**: `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy`

---

**更新时间**: 2026-04-18
**项目**: lsym-memory
