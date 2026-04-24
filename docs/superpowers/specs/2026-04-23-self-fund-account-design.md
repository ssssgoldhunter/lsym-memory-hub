# 自有资金账户（Self-Owned Fund Account）设计方案

> 日期：2026-04-24（v2 业务调整）
> 状态：设计已更新

## 1. 背景与目标

中信银行为平台开立了"自有资金登记簿"（registerAttr=12），这是平台账户下的一个特殊子账户。自有资金账户绑定了银行卡，具有完整的 `bank_e_account_id` 和 `bank_e_account_code`，系统内会为该银行卡建立完整的账户结构。

**目标**：支持自有资金账户的入金（充值）、内部调账、转账（平台付款/收款）全流程。

---

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| 自有资金账户 | 平台账户下的特殊子账户，`registerAttr=12`（自有资金登记簿） |
| 银行卡绑定 | 自有资金账户有完整银行卡数据，`bank_e_account_id` 和 `bank_e_account_code` 正常存在 |
| 入金方式 | 银行通知 `trans_tp=03`，有银行卡号（PAY_ACCNO），按 01/02 模式处理；补偿查询结果格式与普通一致 |
| 金额要求 | 系统虚拟账户余额必须与银行自有资金登记簿余额一致 |

### 2.1 关键业务确认

1. **自有资金账户有银行卡** — `company` 表中 `bank_e_account_id` 和 `bank_e_account_code` 不为 null
2. **通知入金走 01/02 模式** — 银行通知数据 `trans_tp=03`，有 `PAY_ACCNO`（银行卡号），按 01/02 模式处理，通过银行卡反查内部账号
3. **不需要额外补偿 Job** — 自有资金补偿查询结果格式与普通充值一致（trans_tp=01），现有补偿机制已覆盖
4. **bankEAccountId/Code 在充值流程中不关键** — 核心充值靠 `cardCode` + `subAccountId` 驱动；`bankEAccountId` 仅在 04 子账户子记录中做记录存储

### 2.2 设计原则：平台账户转账抽象

登记簿转账操作在业务层**独立抽象为"平台账户转账"**，不绑定特定银行的区分机制。

- 业务层只知道"平台账户转账"（方向、金额、目标账户），不关心底层实现
- 银行差异由 `bas_param_t` 配置参数 + Front Handle 实现层屏蔽
- 新银行接入只需：新增配置 + 实现 Handle 接口，业务层零改动

---

## 3. 配置设计

### 3.1 账户映射配置 — `SELF_FUND_ACCOUNT_CONFIG`

存储在 `bas_param_t`，`param_key = "SELF_FUND_ACCOUNT_CONFIG"`。

```json
[
  {
    "cardCode": "系统卡号",
    "registerAttr": "12",
    "payBizFunc": "2041",
    "recBizFunc": "2042",
    "bankEAccountId": "自有资金银行账户J编号",
    "payFundType": "001002",
    "recFundType": "001002",
    "payDealType": "01",
    "recDealType": "01"
  }
]
```

**用途**：
- 转账（LiteFlow）：在消费流程中判断哪方是自有资金账户，使用对应方向参数
  - 付款卡是自有资金 → 读取 `payBizFunc`/`payFundType`/`payDealType` → 调 `platformPay`
  - 收款卡是自有资金 → 读取 `recBizFunc`/`recFundType`/`recDealType` → 调 `platformReceive`

> 入金（充值）不再使用此配置，因为自有资金账户有银行卡，通知和补偿走现有 01/02 逻辑。

---

## 4. 模块改造设计

### 4.1 Base 层 — 数据准备

**脚本初始化**：
- 新建 company 记录：`bank_e_account_id` 和 `bank_e_account_code` 正常赋值（自有资金银行卡）
- 新建 04 子账户：关联上述 company
- 插入 `SELF_FUND_ACCOUNT_CONFIG` 配置到 `bas_param_t`

### 4.2 Front 层 — 银行接口抽象

#### 4.2.1 Transfer Handle 改造

**`BasTransTransferHandle` 接口新增 2 个方法：**

- `platformPay(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo)` — 平台付款
- `platformReceive(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo)` — 平台收款

**Zx 实现**：
- `platformPay()`：bizFunc=2041, outAcctNo=自有资金 bankEAccountId, inAcctNo=目标用户
- `platformReceive()`：bizFunc=2042, outAcctNo=用户, inAcctNo=自有资金 bankEAccountId

### 4.3 Web 层 — 通知入口

**`ReverseNoticeController.transNotifyZx()` 无需特殊改造**。

自有资金账户有银行卡数据，通知入金数据中 `trans_tp=03`、`PAY_ACCNO` 有值，按 01/02 模式处理，走和普通充值完全一样的路径：
- `buildTPNotifyZxDto`：通过 `bankEAccountCode`（来自 `zxMchntId`）查公司
- `executeRechargeProcess`：通过 `platformComCode` + `accountNoEnc`（加密银行卡号）查公司并充值

### 4.4 Consume 层 — 业务逻辑

#### 4.4.1 入金兼容性

`chainRecharge` LiteFlow 链路无需特殊改造。自有资金充值走 `transType=C`（普通充值），不使用 `transType=03`。

- **Pack**：无需改造
- **Check**：无需改造
- **Trans**：充值核心逻辑，accountType=04 兼容
- **After**：后处理兼容

### 4.5 Task 层 — 定时任务

**不需要新建自有资金补偿 Job**。

自有资金账户的补偿查询结果格式与普通充值一致（`trans_tp=01`，有银行卡号），现有 ZX 补偿机制已完全覆盖。

---

## 5. 数据流

### 5.1 入金流程（与普通充值一致）

```
银行推送通知（trans_tp=03, PAY_ACCNO=自有资金银行卡号）
  → ReverseNoticeController 接收
  → 通过 bankEAccountCode 查公司（与 01/02 一致）
  → 创建通知记录
  → executeRechargeProcess
  → 通过 platformComCode + accountNoEnc 查公司
  → transConsumeApi.rechargeTrans()
  → chainRecharge LiteFlow（transType=C）
  → 充值到 04 子账户
```

### 5.2 补偿流程（复用现有）

```
ZxTransNotifyRechargeJob（现有Job）
  → 查 INIT 状态通知记录
  → rechargeTrans() 充值
  → 更新状态 SUCCESS/FAIL
```

### 5.3 转账流程（后续）

```
平台付款（自有资金 → 用户）:
  → platformPay(bizFunc=2041)
  → outAcctNo=自有资金 bankEAccountId
  → inAcctNo=用户 userId

平台收款（用户 → 自有资金）:
  → platformReceive(bizFunc=2042)
  → outAcctNo=用户 userId
  → inAcctNo=自有资金 bankEAccountId
```

---

## 6. 实施顺序

| 阶段 | 模块 | 内容 |
|------|------|------|
| 1 | base | 脚本初始化 company + 04 账户 + SELF_FUND_ACCOUNT_CONFIG 配置 |
| 2 | front | Handle 接口抽象 + Zx 实现（platformPay/platformReceive）+ DTO 扩展 |
| 3 | common | CommonConstants 新增 TRANS_TYPE_SELF_FUND="03"（保留用于标识） |

---

## 7. 待确认项

- [ ] 自有资金账户调账的具体业务场景和流程细节
- [ ] 转账（2041/2042）的业务触发时机（手工/自动）
- [ ] PA 银行自有资金支持时间节点
- [ ] 自有资金账户的账户变动明细记录策略（是否复用 AccountEntryAfterService）

---

## 8. v1 → v2 变更说明

### 8.1 变更原因

自有资金账户实际绑定了银行卡，具有完整银行账户数据，通知和补偿查询结果格式与普通充值一致。

### 8.2 移除项

| 移除内容 | 原因 |
|---------|------|
| `ZX_SELF_FUND_TRANS_CONFIG` 配置 | 不需要单独的补偿配置，复用现有 Job |
| `ZxSelfFundNotifyRechargeService/Impl` | 不需要独立补偿逻辑 |
| `ZxSelfFundNotifyJobService` | 不需要新 Job |
| `ZxSelfFundNotifyRechargeJobService` | 不需要新 Job |
| `FrontTransQueryFacadeApi.queryRegisterTransPages` | 不需要新查询接口 |
| `BasPlatformAccountDetailQueryReq` DTO | 不需要新查询 DTO |
| Front 层 `queryRegisterTransPages` Handle 实现 | 不需要新查询实现 |
| ReverseNoticeController 03 特殊分支 | 03 有银行卡，走和 01/02 一样的路径 |
| RechargeTrans/RechargeTransAfter 03 isAccess | 不再需要 transType=03，走普通 C 类型 |
| `PARAM_SELF_FUND_ACCOUNT_CONFIG` 常量 | 不再用于入金，front 转账用配置名直接引用即可 |
| `SELF_FUND_ACCOUNT_CONFIG` SQL（入金相关） | 入金不再使用此配置 |

### 8.3 保留项

| 保留内容 | 原因 |
|---------|------|
| Front `platformPay`/`platformReceive` 抽象 | 转账场景仍需 |
| `SELF_FUND_ACCOUNT_CONFIG` 配置（转账用途） | 转账时读取方向参数 |
| `TRANS_TYPE_SELF_FUND = "03"` 常量 | 保留标识，当前入金不使用 |
| `BasTransTransferReq` 扩展字段 | 转账 DTO 需要 bizFunc/fundType/dealType/bankEAccountId |

---

## 9. 不在本期范围

- 调账流程（待确认业务场景）
- 转账（2041/2042）LiteFlow集成（front接口先行，业务流程后续）
- PA银行自有资金支持
