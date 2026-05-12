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
- 支持按比例退款和按单退款（当前“按单/逐笔”主要体现在流水层逐笔贪心）
- 退款请求必须以 `subTransList` 明确传入收款卡退款金额；同一 `receiveCardCode` 多条明细需要先合并
- 同一 `receiveCardCode` 业务上唯一绑定商户，不会出现同卡多个 `receiveMerchantId`

### 退款方式对比
| 退款方式 | 组件名 | 分摊深度 | 流水层分配 | 说明 |
|----------|--------|----------|-----------|------|
| **比例退款** | SplitPercentPack | 三层 | 比例计算+补充分配合并 | 按原消费各账户/科目/流水比例退款 |
| **订单退款/逐笔退款** | SplitOrderPack | 三层 | 贪心逐笔分配 | 账户类型层和科目层仍按比例+兜底，流水层逐笔扣减 |

### 退款分摊结构（三层）

```
用户传入 subTransList
│
├─ 先按 receiveCardCode 合并 receiveAmount
│   ├─ 同卡多条明细必须合并，避免重复使用整卡可退余额
│   └─ 合并后的 subTransList 会作为退款主记录落库口径
│
├─ 第一层：账户类型层分摊
│   ├─ 分母：当前 receiveCardCode 下 01 + 02 + 04 的原消费金额合计
│   ├─ 排序：从 Slot 读取 consumeSubAccountSort，倒序
│   ├─ 前N个按比例：refundTotalAmt × accountConsumeAmt / totalConsumeAmt (HALF_UP)
│   ├─ 最后一个兜底剩余金额
│   ├─ 兜底补充分配：剩余金额向有容量的账户类型补分配
│   └─ 兜底后必须重新回写 cancel01Amt/cancel02Amt/cancel04Amt，再进入子明细落地
│
├─ 第二层：科目层分摊 (01/02，04跳过)
│   ├─ 排序：按 activityTypeSort 倒序
│   ├─ 前面科目按比例：accountRefundAmt × activityOrgAmt / totalOrgConsumeAmt (HALF_UP)
│   ├─ 最后一个科目兜底剩余金额
│   └─ 兜底补充分配：剩余金额向有容量的科目补分配
│
└─ 第三层：流水层分摊 (01/02按科目，04直接贪心)
    ├─ 比例退款(SplitPercentPack)：按比例+HALF_UP，补充分配+同consumeSubId合并
    └─ 订单退款(SplitOrderPack)：贪心逐笔 min(leftCancelAmount, recordRefundable)
```

### 业务规则
- **退款期限**: 原消费后30天内
- **退款次数**: 支持多次部分退款
- **膨胀金退款**: 按原消费比例退回02账户
- **收款卡口径**: 退款金额由用户传入的 `subTransList.receiveAmount` 决定，不按整单重新计算多收款卡退款比例
- **单卡可退校验**: 每张 `receiveCardCode` 独立校验本卡 `01+02+04` 可退余额，独立判断是否全退
- **04账户**: 比例模式和逐笔模式都支持 04；04可走原路退回，也可按配置走 04转01
- **极值兜底**: 账户类型层、科目层、流水层都有兜底，兜底金额必须参与最终退款子单金额落地

### 退款数据流转示例
```
原消费: 100元 (01:70元, 02:25元, 04:5元)

全额退款100元 (isLastRefundFlag=true):
├── 01账户: +70元 (直接退全部可退余额)
├── 02账户: +25元 (退回膨胀金)
└── 04账户: +5元

部分退款50元 (比例退款):
├── 第一层(账户类型): 01→35, 02→12, 04→3 (比例+兜底)
├── 第二层(科目): 按科目比例分配到各科目 + 兜底补充分配
└── 第三层(流水): 按流水比例分配 + 补充分配+同consumeSubId合并

部分退款50元 (订单退款):
├── 第一层(账户类型): 01→35, 02→12, 04→3 (比例+兜底)
├── 第二层(科目): 按科目比例分配到各科目 + 兜底补充分配
└── 第三层(流水): 逐笔贪心分配 min(left, recordRefundable)
```

### 核心组件
```
consumeTransRefundPack → 业务校验 → 查询原消费
→ consumeTransRefundSplitSwitch (拆分方式路由)
→ ├─ SplitPercentPack: 账户类型层(比例) → 科目层(比例) → 流水层(比例+补充分配+合并)
→ └─ SplitOrderPack:   账户类型层(比例) → 科目层(比例) → 流水层(贪心逐笔)
→ 04退款模型路由
→ ├─ bashChainRefundModel04: consumeTransRefund04 → consumeTransRefund04Process → consumeTransRefund01 → consumeTransRefund02 → consumeTransRefundAfter
→ └─ bashChainRefundModel01: consumeTransRefundRecharge01 → consumeTransRefund01 → consumeTransRefund02 → consumeTransRefundModel1After
```

### 2026-05-12 退款分摊修复记忆
- 问题原因：极值金额下，比例分摊后触发兜底，但兜底只更新 `accountRefundAmtMap`，未重新回写 `cancel01Amt/cancel02Amt/cancel04Amt`，导致计算金额与最终子明细落地金额不一致。
- 同卡多明细问题：如果同一 `receiveCardCode` 被上游拆成多条退款明细，逐条分摊会重复基于整张卡的可退余额计算，可能重复触发兜底并造成账户类型金额偏移。
- 修复口径：SplitPercentPack 和 SplitOrderPack 都先按 `receiveCardCode` 合并 `subTransList`，再按当前收款卡内部 `01+02+04` 原消费金额计算分摊；兜底后必须回写最终 `cancelXXAmt`。
- 测试重点：同卡多明细、极小金额、01/02/04混合、部分退款/全退、04原路退回、04转01。

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

**更新时间**: 2026-05-07
**项目**: lsym-memory
