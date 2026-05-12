# 自有资金账户 — 业务功能与开发指南

> 用途：其他项目/渠道接入自有资金账户时参照本文档开发
> 更新日期：2026-05-08
> 状态：入金已完成，转账（front 后续集成）待开发

---

## 一、业务背景

中信银行为平台开立"自有资金登记簿"（`registerAttr=12`），是平台账户下的特殊子账户。自有资金账户绑定了银行卡，具有完整的 `bank_e_account_id` 和 `bank_e_account_code`，系统内建立完整账户结构。

### 核心业务场景

| 场景 | 说明 | 状态 |
|------|------|------|
| **入金（充值）** | 银行推送 `trans_tp=03` 通知，通过银行卡反查内部账号充值到 04 子账户 | ✅ 已完成 |
| **补偿查询** | 补偿查询结果格式与普通充值一致，复用现有 Job | ✅ 已完成 |
| **转账（平台付款）** | 自有资金 → 用户登记簿（bizFunc=2041） | Front 已实现，Consume 待集成 |
| **转账（平台收款）** | 用户登记簿 → 自有资金（bizFunc=2042） | Front 已实现，Consume 待集成 |
| **调账** | 内部调账 | 待确认业务场景 |

---

## 二、开发点清单（按层级）

### 第 1 层：Base — 数据准备

| # | 开发点 | 说明 | 参照 |
|---|--------|------|------|
| 1.1 | 新建 company 记录 | `bank_e_account_id` 和 `bank_e_account_code` 正常赋值（自有资金银行卡），不是 null | — |
| 1.2 | 新建 04 子账户 | 关联上述 company，accountType=04（综合账户） | — |
| 1.3 | 插入配置到 `bas_param_t` | `param_key = "SELF_FUND_ACCOUNT_CONFIG"`，含 cardCode/registerAttr/bizFunc/fundType/dealType/bankEAccountId | 见下方配置结构 |

**配置结构：**

```json
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
```

> 此配置当前仅用于转账场景。入金（充值）不使用此配置，因为自有资金有银行卡，走现有 01/02 逻辑。

### 第 2 层：Common — 常量定义

| # | 开发点 | 说明 |
|---|--------|------|
| 2.1 | `CommonConstants.TRANS_TYPE_SELF_FUND = "03"` | 自有资金入金类型标识（保留用于标识，入金实际走 transType=C） |
| 2.2 | `CommonConstants.TI_ZYKQ_REMAKER` | 自有资金备注参数名 |

### 第 3 层：Web — 通知入口

| # | 开发点 | 说明 |
|---|--------|------|
| 3.1 | `ReverseNoticeController.transNotifyZx()` 支持 03 | trans_tp=03 有 PAY_ACCNO，按 01/02 模式处理（通过 bankEAccountCode 查公司），无需特殊分支 |

> **关键**：无需为 03 写特殊分支。自有资金有银行卡数据，通知路径与 01/02 完全一致。

### 第 4 层：Consume — 业务逻辑

| # | 开发点 | 说明 | 状态 |
|---|--------|------|------|
| 4.1 | `chainRecharge` 兼容性 | 无需改造，自有资金充值走 `transType=C`（普通充值），Pack/Check/Trans/After 全部兼容 | ✅ |
| 4.2 | 转账 LiteFlow 集成 | 在消费流程中判断哪方是自有资金账户，调用 `platformPay` 或 `platformReceive` | ❌ 待开发 |

### 第 5 层：Front — 银行接口抽象（✅ 已实现）

| # | 开发点 | 说明 | 状态 |
|---|--------|------|------|
| 5.1 | `BasTransTransferHandle` 接口新增方法 | `platformPay` / `platformReceive` 两个方法签名 | ✅ |
| 5.2 | `AbstractTransTransferHandle` 默认实现 | 返回 null（未对接的银行无需强制实现） | ✅ |
| 5.3 | `ZxTransTransferHandle` 实现 | platformPay（bizFunc=2041）和 platformReceive（bizFunc=2042）完整实现 | ✅ |
| 5.4 | `PaTransTransferHandle` 实现 | 未实现，继承抽象类返回 null | ⏳ 待 PA 银行需求 |
| 5.5 | `BasTransTransferReq` DTO 扩展 | 新增 bizFunc/fundType/dealType/bankEAccountId 字段 | ✅ |
| 5.6 | `SaasZxInterService` 底层接口 | zxPlatformPay / zxPlatformReceive 方法 | ✅ |
| 5.7 | `SaasZxApi` 路径常量 | PlatformPay = "transfer", PlatformReceive = "transfer" | ✅ |
| 5.8 | `ZxTransferRequest` 字段 | pSelfFlag / pSelfAmt（自有资金交易类型/金额） | ✅ |

### 第 6 层：Task — 定时任务

| # | 开发点 | 说明 |
|---|--------|------|
| 6.1 | 补偿 Job | **不需要新建**。现有 ZX 补偿机制已覆盖，自有资金补偿查询结果格式与普通一致 |

### 自有资金池账户变动明细类型映射

| 业务交易类型 | 账户变动明细类型 | 方向 |
|--------------|------------------|------|
| `MC` | `MI` | 入金 |
| `MC` | `MO` | 出金 |
| `MR` | `MC` | 出金 |
| `MR` | `MR` | 入金 |

> 排查自有资金池平台收付款时，先确认主交易类型是 `MC` 还是 `MR`，再按上表判断账户变动明细的入金/出金方向。

### 第 7 层：Data-Batch — 批处理

| # | 开发点 | 说明 | 状态 |
|---|--------|------|------|
| 7.1 | `SelfOwnFundController` | 自有资金同步/清分 XXL-Job（handler: selfOwnFundThread） | ✅ |
| 7.2 | `SelfOwnFundServiceImpl` | 批处理逻辑（导入、门店关联、清分明细处理） | ✅ |

---

## 三、Front 后待开发部分

以下为 Front 层已完成接口定义和 ZX 实现，但尚未在业务层（Consume LiteFlow）集成的部分：

### 3.1 转账 LiteFlow 集成（核心待开发）

**现状**：
- Front 层 `platformPay` / `platformReceive` 方法已完整实现
- Consume 层的 `TransferTrans` 组件当前仅调用 `transTransfer`（普通转账 bizFunc=27）
- 没有组件根据 `SELF_FUND_ACCOUNT_CONFIG` 判断是否需要走平台转账

**待开发内容**：

| # | 开发项 | 说明 |
|---|--------|------|
| A | 配置读取组件 | LiteFlow 组件从 `bas_param_t` 读取 `SELF_FUND_ACCOUNT_CONFIG`，判断付款/收款方是否为自有资金账户 |
| B | 路由判断 | 消费流程中：付款卡是自有资金 → 调 `platformPay`；收款卡是自有资金 → 调 `platformReceive` |
| C | Facade API 暴露 | 需确认 `FrontTransTransferFacadeApi` 或 `FrontTransConsumeFacadeApi` 是否需要新增 Feign 接口暴露 platformPay/platformReceive 给 Consume 层调用 |
| D | 参数传递 | 调用方（LiteFlow 组件）从配置读取 bizFunc/fundType/dealType/bankEAccountId，设置到 `BasTransTransferReq` |
| E | 结果处理 | platformPay/platformReceive 的返回结果处理和状态更新 |

### 3.2 PA 银行接入

| # | 开发项 | 说明 |
|---|--------|------|
| F | `PaTransTransferHandle.platformPay` | PA 渠道平台付款实现（当前继承抽象类返回 null） |
| G | `PaTransTransferHandle.platformReceive` | PA 渠道平台收款实现 |

### 3.3 待确认业务项

| # | 待确认项 | 影响 |
|---|---------|------|
| H | 转账（2041/2042）的业务触发时机 | 手工触发 vs 自动触发，决定是否需要新增 LiteFlow 链 |
| I | 调账业务场景和流程 | 决定是否需要新链路 |
| J | 自有资金与消费流程的交互 | 消费扣款时是否涉及自有资金账户参与 |

---

## 四、数据流总结

### 4.1 入金流程（已完成）

```
银行推送通知（trans_tp=03, PAY_ACCNO=自有资金银行卡号）
  → ReverseNoticeController 接收
  → 通过 bankEAccountCode 查公司（与 01/02 一致）
  → 创建通知记录
  → executeRechargeProcess
  → transConsumeApi.rechargeTrans()
  → chainRecharge LiteFlow（transType=C）
  → 充值到 04 子账户
```

### 4.2 补偿流程（复用现有）

```
ZxTransNotifyRechargeJob（现有Job）
  → 查 INIT 状态通知记录
  → rechargeTrans() 充值
  → 更新状态 SUCCESS/FAIL
```

### 4.3 转账流程（Front 已实现，Consume 待集成）

```
平台付款（自有资金 → 用户）:
  → [待开发] LiteFlow 组件判断自有资金
  → 读取 SELF_FUND_ACCOUNT_CONFIG
  → 设置 bizFunc=2041, fundType, dealType, bankEAccountId
  → [待开发] Feign 调用 platformPay
  → Front ZxTransTransferHandle.platformPay()
  → SaasZxInterService.zxPlatformPay()
  → 银行接口

平台收款（用户 → 自有资金）:
  → [待开发] LiteFlow 组件判断自有资金
  → 读取 SELF_FUND_ACCOUNT_CONFIG
  → 设置 bizFunc=2042, fundType, dealType, bankEAccountId
  → [待开发] Feign 调用 platformReceive
  → Front ZxTransTransferHandle.platformReceive()
  → SaasZxInterService.zxPlatformReceive()
  → 银行接口
```

---

## 五、新渠道接入清单

当新银行渠道（如 PA、其他银行）需要支持自有资金账户时，按以下清单开发：

### 必须完成

- [ ] **Base**：新建 company + 04 子账户 + bas_param_t 配置
- [ ] **Common**：确认 TRANS_TYPE_SELF_FUND 常量是否需要
- [ ] **Web**：通知入口支持该渠道的入金通知格式
- [ ] **Consume**：确认 chainRecharge 兼容性

### 按需完成

- [ ] **Front Handle**：实现 `XxTransTransferHandle.platformPay()` 和 `platformReceive()`
- [ ] **Front 底层接口**：实现 `SaasXxInterService.xxPlatformPay()` 和 `xxPlatformReceive()`
- [ ] **Front DTO**：确认 `BasTransTransferReq` 扩展字段是否满足新渠道需求
- [ ] **Consume LiteFlow**：集成平台转账路由判断

### 不需要做

- ~~新建补偿 Job~~ — 复用现有
- ~~新建查询接口~~ — 复用现有
- ~~通知入口特殊分支~~ — 有银行卡走普通路径
- ~~修改 isAccess 门控~~ — 走 transType=C

---

## 六、源码索引

| 模块 | 文件 | 说明 |
|------|------|------|
| common-core | `CommonConstants.TRANS_TYPE_SELF_FUND = "03"` | 自有资金入金类型常量 |
| common-core | `CommonConstants.TI_ZYKQ_REMAKER` | 自有资金备注参数 |
| front-service | `BasTransTransferHandle.platformPay/platformReceive` | 接口定义 |
| front-service | `AbstractTransTransferHandle` | 抽象基类（默认 null） |
| front-service | `ZxTransTransferHandle.platformPay()` | ZX 平台付款实现（完整） |
| front-service | `ZxTransTransferHandle.platformReceive()` | ZX 平台收款实现（完整） |
| front-service | `PaTransTransferHandle` | PA 渠道未实现（继承默认 null） |
| front-service | `SaasZxInterService.zxPlatformPay/zxPlatformReceive` | ZX 底层银行接口调用 |
| front-api | `SaasZxApi.PlatformPay/PlatformReceive = "transfer"` | API 路径常量 |
| front-api | `BasTransTransferReq.bizFunc/fundType/dealType/bankEAccountId` | 转账 DTO 扩展字段 |
| front-common | `ZxTransferRequest.pSelfFlag/pSelfAmt` | 自有资金交易类型/金额 |
| web | `ReverseNoticeController.transNotifyZx()` | 支持 03 类型通知入金 |
| data-batch | `SelfOwnFundController/SelfOwnFundServiceImpl` | 自有资金同步/清分 |
| management | `NpkStoreService` | 门店自有资金协议查询 |
