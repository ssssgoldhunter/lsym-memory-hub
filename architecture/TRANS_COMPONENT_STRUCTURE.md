# fund-catering-consume-service trans 组件项目结构与路径说明

## 一、目录结构概览

```
fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/
├── consume/                          # 消费交易相关组件
│   ├── auth/                         # 消费授权相关
│   ├── calOrder/                     # 消费订单计算相关
│   ├── close/                        # 消费关闭相关
│   ├── consume/                      # 消费核心逻辑
│   ├── frozen/                       # 消费冻结相关
│   └── pre/                          # 消费预处理相关
├── consumeRefund/                    # 消费退款交易相关组件
├── frozen/                           # 冻结交易相关组件
├── recharge/                         # 充值交易相关组件
├── refundRecharge/                   # 充值退款交易相关组件
├── transfer/                         # 转账交易相关组件
│   ├── api/                          # 转账API相关
│   ├── auth/                         # 转账授权相关
│   ├── manage/                       # 转账管理相关
│   ├── ti/                           # 转账TI相关
│   └── verfily/                      # 转账验证相关
├── unfrozen/                         # 解冻交易相关组件
└── withDraw/                         # 提现交易相关组件
```

## 二、各交易类型详细说明

### 2.1 消费交易 (consume)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.consume`

#### 2.1.1 核心消费组件 (consume/)

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `ConsumeTrans01.java` | 01子账户消费交易 | 处理01现金子账户的消费交易，创建消费子表和消费充值关联表 |
| `ConsumeTrans02.java` | 02子账户消费交易 | 处理02膨胀金子账户的消费交易 |
| `ConsumeTrans04.java` | 04子账户消费交易 | 处理04子账户的消费交易 |
| `ConsumeTransPack.java` | 消费交易打包组件 | 消费交易的打包处理 |
| `ConsumeTransAfter.java` | 消费交易后处理 | 消费交易完成后的处理逻辑 |
| `ConsumeTransDeductionSplitPack.java` | 消费扣款拆分打包 | 消费扣款的拆分处理 |
| `ConsumeTransSubTypeRoute.java` | 消费子类型路由 | 根据子账户类型进行路由 |

**涉及的账户变动表**:
- `TransConsumeSubT` (消费子表)
- `TransConsumeSubRecT` (消费充值关联表)
- `TransAcctChangeDetailT` (账户变动明细表)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

#### 2.1.2 消费授权组件 (auth/)

| 文件 | 说明 |
|------|------|
| `ConsumeTransAuthorizationPack.java` | 消费授权打包处理 |
| `ConsumeTransAuthAfter.java` | 消费授权后处理 |
| `ConsumePassWordCheckRoute.java` | 消费密码校验路由 |

#### 2.1.3 消费订单计算组件 (calOrder/)

| 文件 | 说明 |
|------|------|
| `ConsumeTrans01CalOrder.java` | 01子账户订单计算 |
| `ConsumeTrans02CalOrder.java` | 02子账户订单计算 |
| `ConsumeTransCalOrderPack.java` | 订单计算打包处理 |
| `ConsumeTransCalAfter.java` | 订单计算后处理 |
| `ConsumeTransDeductionCalOrderSplitPack.java` | 扣款订单计算拆分打包 |
| `ConsumeTransCalDeductionSplitPack.java` | 计算扣款拆分打包 |

#### 2.1.4 消费预处理组件 (pre/)

| 文件 | 说明 |
|------|------|
| `ConsumeTransPrePack.java` | 消费预处理打包 |
| `ConsumeTransPreAfter.java` | 消费预处理后处理 |
| `ConsumeTransPreDeductionSplitPack.java` | 预处理扣款拆分打包 |
| `ConsumeTransPreFinishPack.java` | 预处理完成打包 |
| `ConsumeTransPreSubTypeRoute.java` | 预处理子类型路由 |
| `ConsumeTransPre04FirstError.java` | 预处理04首次错误处理 |
| `ConsumeTransPre04FirstErrorAfter.java` | 预处理04首次错误后处理 |

#### 2.1.5 消费关闭组件 (close/)

| 文件 | 说明 |
|------|------|
| `ConsumeTransClosePack.java` | 消费关闭打包 |
| `ConsumeTransCloseAfter.java` | 消费关闭后处理 |
| `ConsumeTransCloseRoute.java` | 消费关闭路由 |

#### 2.1.6 消费冻结组件 (frozen/)

| 文件 | 说明 |
|------|------|
| `ConsumeTransFrozen.java` | 消费冻结处理 |
| `ConsumeTransUnFrozen.java` | 消费解冻处理 |

### 2.2 消费退款交易 (consumeRefund)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.consumeRefund`

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `ConsumeTransRefund01.java` | 01子账户消费退款 | 处理01现金子账户的消费退款，更新原消费记录并创建退款记录 |
| `ConsumeTransRefund02.java` | 02子账户消费退款 | 处理02膨胀金子账户的消费退款 |
| `ConsumeTransRefund04.java` | 04子账户消费退款 | 处理04子账户的消费退款 |
| `ConsumeTransRefundAfter.java` | 消费退款后处理 | 消费退款完成后的账户变动明细处理 |
| `ConsumeTransRefundPack.java` | 消费退款打包 | 消费退款的打包处理 |
| `ConsumeTransRefundModel1After.java` | 消费退款模式1后处理 | 消费退款模式1的账户变动明细处理 |
| `ConsumeTransRefundSplitOrderPack.java` | 退款拆分订单打包 | 按订单拆分的退款处理 |
| `ConsumeTransRefundSplitPercentPack.java` | 退款拆分比例打包 | 按比例拆分的退款处理 |
| `ConsumeTransRefundRecharge01.java` | 退款充值01处理 | 退款时对01子账户的充值处理 |
| `ConsumeTransRefund04After.java` | 04退款后处理 | 04子账户退款后的处理 |
| `ConsumeTransRefund04Process.java` | 04退款处理 | 04子账户退款的处理逻辑 |
| `ConsumeTransRefundSplitSwitch.java` | 退款拆分开关 | 退款拆分的路由控制 |

**涉及的账户变动表**:
- `TransConsumeSubT` (消费子表 - 更新退款金额)
- `TransConsumeSubRecT` (消费充值关联表)
- `TransAcctChangeDetailT` (账户变动明细表)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

### 2.3 充值交易 (recharge)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.recharge`

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `RechargeTrans.java` | 充值交易主组件 | 创建充值主表和子表，处理01/02子账户的膨胀金充值 |
| `RechargeTransAfter.java` | 充值交易后处理 | 充值完成后创建账户变动明细记录 |
| `RechargeTransPack.java` | 充值交易打包 | 充值交易的打包处理 |

**涉及的账户变动表**:
- `TransRechargeT` (充值主表)
- `TransRechargeSubT` (充值子表)
- `BasSubAccountExpendT` (膨胀金子账户扩展表)
- `TransAcctChangeDetailT` (账户变动明细表)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

### 2.4 充值退款交易 (refundRecharge)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.refundRecharge`

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `RefundRechargeTrans.java` | 充值退款交易主组件 | 创建充值退款主表和子表 |
| `RefundRechargeTransAfter.java` | 充值退款后处理 | 充值退款完成后创建账户变动明细记录 |
| `RefundRechargeTransPack.java` | 充值退款打包 | 充值退款的打包处理 |

**涉及的账户变动表**:
- `TransRechargeT` (充值主表 - 退款记录)
- `TransRechargeSubT` (充值子表 - 退款记录)
- `TransAcctChangeDetailT` (账户变动明细表)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

### 2.5 转账交易 (transfer)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.transfer`

#### 2.5.1 API转账组件 (api/)

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `TransferTrans.java` | 转账交易主组件 | 创建转账主表和子表，处理转账冻结，调用平台转账接口 |
| `TransferTransAfter.java` | 转账交易后处理 | 转账完成后处理账户变动明细（转出方和转入方） |
| `TransferTransPack.java` | 转账交易打包 | 转账交易的打包处理 |

**涉及的账户变动表**:
- `TransTransferT` (转账主表)
- `TransTransferSubT` (转账子表)
- `TransAcctFrozenChangeDetailT` (账户冻结变动明细表)
- `TransAcctChangeDetailT` (账户变动明细表 - 转出方和转入方)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

#### 2.5.2 转账授权组件 (auth/)

| 文件 | 说明 |
|------|------|
| `TransferTransAuth.java` | 转账授权处理 |
| `TransferTransAuthPack.java` | 转账授权打包 |

#### 2.5.3 转账管理组件 (manage/)

| 文件 | 说明 |
|------|------|
| `TransferTransInner.java` | 内部转账处理 |
| `TransferTransInnerAfter.java` | 内部转账后处理 |
| `TransferTransInnerPack.java` | 内部转账打包 |
| `TransferTransInnerPreAfter.java` | 内部转账预处理后处理 |

#### 2.5.4 转账TI组件 (ti/)

| 文件 | 说明 |
|------|------|
| `TransferTransTi.java` | TI转账处理 |
| `TransferTransTiPack.java` | TI转账打包 |
| `TransferTransTiPreAfter.java` | TI转账预处理后处理 |

#### 2.5.5 转账验证组件 (verfily/)

| 文件 | 说明 |
|------|------|
| `TransferTransSendVerify.java` | 发送转账验证码 |
| `TransferTransReSendVerifyPack.java` | 重新发送验证码打包 |

### 2.6 提现交易 (withDraw)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.withDraw`

| 文件 | 说明 | 主要功能 |
|------|------|----------|
| `WithDrawTrans.java` | 提现交易主组件 | 创建消费主表、子表和充值关联表，处理提现冻结，调用平台提现接口 |
| `WithDrawTransAfter.java` | 提现交易后处理 | 提现完成后处理账户变动明细和解冻 |
| `WithDrawTransPack.java` | 提现交易打包 | 提现交易的打包处理 |

**涉及的账户变动表**:
- `TransConsumeT` (消费主表 - 提现记录)
- `TransConsumeSubT` (消费子表 - 提现记录)
- `TransConsumeSubRecT` (消费充值关联表 - 提现记录)
- `TransAcctFrozenChangeDetailT` (账户冻结变动明细表)
- `TransAcctChangeDetailT` (账户变动明细表)
- `TransAcctSumChangeDetailT` (账户汇总变动明细表)
- `TransAcctActSumChangeDetailT` (账户活动汇总变动明细表)

### 2.7 冻结交易 (frozen)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.frozen`

| 文件 | 说明 |
|------|------|
| `FrozenTrans.java` | 冻结交易处理 |
| `FrozenTransBefore.java` | 冻结交易前置处理 |
| `FrozenTransPack.java` | 冻结交易打包 |

### 2.8 解冻交易 (unfrozen)

**路径**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans.unfrozen`

| 文件 | 说明 |
|------|------|
| `UnFrozenTrans.java` | 解冻交易处理 |
| `UnFrozenTransBefore.java` | 解冻交易前置处理 |
| `UnFrozenTransPack.java` | 解冻交易打包 |

## 三、账户变动明细表结构说明

### 3.1 主要账户变动明细表

#### 3.1.1 TransAcctChangeDetailT (账户变动明细表)
- **表名**: `trans_acct_change_detail_t`
- **用途**: 记录账户金额变动明细，支持一笔交易扣减多笔膨胀金的情况
- **主要字段**:
  - `change_detail_id`: 变动明细ID
  - `account_id`: 主账户ID
  - `card_code`: 卡号
  - `sub_account_type`: 子账户类型 (01-现金, 02-膨胀金)
  - `sub_account_id`: 子账户ID
  - `acct_expand_id`: 膨胀金子账户扩展ID
  - `trans_time`: 交易时间
  - `org_amt`: 交易前金额
  - `trans_amt`: 交易金额
  - `balance`: 交易后金额
  - `org_real_amt`: 交易前本金余额
  - `real_balance`: 交易后本金余额
  - `trans_type`: 交易类型
  - `trans_no`: 交易单号

#### 3.1.2 TransAcctSumChangeDetailT (账户汇总变动明细表)
- **表名**: `trans_acct_sum_change_detail_t`
- **用途**: 账户汇总级别的变动明细记录

#### 3.1.3 TransAcctActSumChangeDetailT (账户活动汇总变动明细表)
- **表名**: `trans_acct_act_sum_change_detail_t`
- **用途**: 账户活动汇总级别的变动明细记录

#### 3.1.4 TransAcctFrozenChangeDetailT (账户冻结变动明细表)
- **表名**: `trans_acct_frozen_change_detail_t`
- **用途**: 记录账户冻结和解冻的变动明细
- **主要使用场景**: 转账、提现等需要冻结资金的交易

#### 3.1.5 TransAcctChangeEntryDetailT (账户变动明细待上账表)
- **表名**: `trans_acct_change_entry_detail_t`
- **用途**: 账户余额变动明细待上账表

### 3.2 各交易类型对账户变动明细表的操作

| 交易类型 | 涉及的账户变动明细表 | 操作说明 |
|---------|-------------------|---------|
| **消费** | TransAcctChangeDetailT<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT | 扣减账户余额，记录变动明细 |
| **消费退款** | TransAcctChangeDetailT<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT | 增加账户余额，记录退款变动明细 |
| **充值** | TransAcctChangeDetailT<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT<br>BasSubAccountExpendT | 增加账户余额，创建膨胀金扩展记录，记录变动明细 |
| **充值退款** | TransAcctChangeDetailT<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT | 扣减账户余额，记录退款变动明细 |
| **转账** | TransAcctFrozenChangeDetailT<br>TransAcctChangeDetailT (转出方)<br>TransAcctChangeDetailT (转入方)<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT | 转出方扣减，转入方增加，记录冻结和解冻明细 |
| **提现** | TransAcctFrozenChangeDetailT<br>TransAcctChangeDetailT<br>TransAcctSumChangeDetailT<br>TransAcctActSumChangeDetailT | 扣减账户余额，记录冻结和解冻明细 |

## 四、代码组织特点

### 4.1 命名规范
- 主交易组件: `{交易类型}Trans.java` (如 `ConsumeTrans01.java`, `RechargeTrans.java`)
- 后处理组件: `{交易类型}TransAfter.java` (如 `ConsumeTransAfter.java`, `RechargeTransAfter.java`)
- 打包组件: `{交易类型}TransPack.java` (如 `ConsumeTransPack.java`, `RechargeTransPack.java`)

### 4.2 处理流程
1. **主交易组件**: 创建主表和子表记录
2. **后处理组件**: 处理账户变动明细表，更新账户余额
3. **打包组件**: 对交易进行打包处理

### 4.3 账户变动明细处理位置
- 大部分账户变动明细的处理在 `*TransAfter.java` 文件中
- 使用 `TransAcctChangeDetailTService` 服务进行账户变动明细的创建
- 同时会创建汇总级别的变动明细记录

## 五、重构建议

基于当前代码结构，重构账户变动与账户变动明细表时需要考虑：

1. **统一账户变动明细处理逻辑**: 将分散在各 `*TransAfter.java` 中的账户变动明细处理逻辑提取为统一的服务
2. **账户变动明细表结构优化**: 考虑是否需要合并或拆分现有的多张账户变动明细表
3. **事务一致性**: 确保主表、子表和账户变动明细表的事务一致性
4. **性能优化**: 考虑批量插入账户变动明细记录，减少数据库交互次数

## 六、相关服务类

### 6.1 账户变动明细服务
- `TransAcctChangeDetailTService`: 账户变动明细表服务
- `TransAcctSumChangeDetailTService`: 账户汇总变动明细表服务
- `TransAcctActSumChangeDetailTService`: 账户活动汇总变动明细表服务
- `TransAcctFrozenChangeDetailTService`: 账户冻结变动明细表服务

### 6.2 交易主表服务
- `TransConsumeTService`: 消费主表服务
- `TransRechargeTService`: 充值主表服务
- `TransTransferTService`: 转账主表服务

### 6.3 交易子表服务
- `TransConsumeSubTService`: 消费子表服务
- `TransRechargeSubTService`: 充值子表服务
- `TransTransferSubTService`: 转账子表服务
- `TransConsumeSubRecTService`: 消费充值关联表服务

---

**文档生成时间**: 2025-01-27  
**扫描范围**: `com.chinaums.erp.slhy.catering.consume.flow.component.trans` 目录下所有代码

