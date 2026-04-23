# 自有资金账户（Self-Owned Fund Account）设计方案

> 日期：2026-04-23
> 状态：设计中

## 1. 背景与目标

中信银行为平台开立了"自有资金登记簿"（registerAttr=12），这是平台账户下的一个特殊子账户。银行不分配新的 bankEAccountId，所有操作共享平台现有银行账户，通过 `registerAttr` 区分业务类型。

**目标**：支持自有资金账户的入金（充值）、补偿查询、内部调账、转账（平台付款/收款）全流程。

---

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| 自有资金账户 | 平台账户下的特殊子账户，`registerAttr=12`（自有资金登记簿） |
| 系统虚拟账户 | company 表中 `bank_e_account_id=null`、`code=null` 的虚拟账户，映射自有资金卡号 |
| 区分方式 | 不通过 bankEAccountId 区分，通过 `registerAttr` 在银行接口中区分 |
| 金额要求 | 系统虚拟账户余额必须与银行自有资金登记簿余额一致 |

### 2.1 设计原则：平台账户转账抽象

登记簿交易操作在业务层**独立抽象为"平台账户转账"**，不绑定特定银行的区分机制。

**核心理念**：
- 业务层只知道"平台账户转账"（方向、金额、目标账户），不关心底层实现
- 银行差异由 `bas_param_t` 配置参数 + Front Handle 实现层屏蔽
- ZX 用 `registerAttr` 区分，未来其他银行可能用 `subAccountNo` 或其他方式区分
- 新银行接入只需：新增配置 + 实现 Handle 接口，业务层零改动

```
业务层：platformAccountTransfer(方向、金额、目标账户)
  → 读 bas_param_t 配置
  → Router 按 platformCode 路由到 Handle
    → ZX Handle：registerAttr=12 + bizFunc=2041/2042
    → 未来某银行 Handle：subAccountNo + 其他 bizFunc
    → PA Handle：PA 的区分方式
```

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
    "bankEAccountId": "使用平台账户的J编号",
    "bankEMemberCode": null,
    "description": "自有资金虚拟账户映射"
  }
]
```

**用途**：
- 入金 Pack 组件：`trans_tp=03` 时读取 `cardCode` 跳过校验直接充值
- 转账：读取 `payBizFunc`/`recBizFunc` 确定 bizFunc
- 查询：读取 `registerAttr` 确定登记簿类型

**映射关系**：一对一（一个自有资金卡号对应一个系统虚拟账户）

### 3.2 补偿任务配置 — `ZX_SELF_FUND_TRANS_CONFIG`

存储在 `bas_param_t`，`param_key = "ZX_SELF_FUND_TRANS_CONFIG"`。中信渠道自有资金池独享 Job。

```json
[
  {
    "operatorCode": "0010",
    "platformCode": "UMS_PLAT_ZX",
    "orgCode": "1214",
    "firstOrgCode": "1234",
    "bizFunc": "24",
    "registerAttr": "12",
    "transType": "99",
    "queryTimeWindowMinutes": 30
  }
]
```

**用途**：自有资金补偿 Job 直接读取配置拼请求参数。

### 3.3 银行连接参数

复用平台账户的 `BasPlatformSettleInfoQueryRes`（appId/appKey/url/mchntId/mchntMbrId），无需额外配置。

---

## 4. 模块改造设计

### 4.1 Base 层 — 数据准备

**脚本初始化**：
- 新建 company 记录：`bank_e_account_id=null`，`code=null`
- 新建 04 子账户：关联上述 company
- 插入 `SELF_FUND_ACCOUNT_CONFIG` 配置到 `bas_param_t`
- 插入 `ZX_SELF_FUND_TRANS_CONFIG` 配置到 `bas_param_t`

### 4.2 Front 层 — 银行接口抽象

#### 4.2.1 Transfer Handle 改造

**`BasTransTransferHandle` 接口新增 2 个方法：**

```java
/**
 * 平台付款（自有资金 → 用户登记簿）
 * ZX: bizFunc=2041, PA: 待定
 */
<T> T platformPay(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo);

/**
 * 平台收款（用户登记簿 → 自有资金）
 * ZX: bizFunc=2042, PA: 待定
 */
<T> T platformReceive(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo);
```

**`AbstractTransTransferHandle`** 提供默认空实现（返回 null），PA 等暂未对接的银行无需强制实现。

**`ZxTransTransferHandle` 实现：**

`platformPay()`：
- `bizFunc = "2041"`
- `outAcctNo` = 自有资金账户 userId（从配置读 bankEAccountId）
- `inAcctNo` = 收款用户 userId
- reserve 填充：dealType、inAcctNm（SM2加密）、bussId、bussSubId、payDate、payTime、fundTp

`platformReceive()`：
- `bizFunc = "2042"`
- `outAcctNo` = 付款用户 userId
- `inAcctNo` = 自有资金账户 userId（从配置读 bankEAccountId）
- reserve 填充：dealType、outAcctNm（SM2加密）、bussId、bussSubId、payDate、payTime、fundTp

#### 4.2.2 Query Handle 改造

**`BasTransQueryHandle` 接口新增 1 个方法：**

```java
/**
 * 登记簿交易明细查询
 * ZX: bizFunc=24, registerAttr=12（自有资金登记簿）
 */
<T> T queryRegisterDetails(BasRegisterDetailQueryReq request, BasPlatformSettleInfoQueryRes plaformInfo);
```

**`AbstractTransQueryHandle`** 提供默认空实现。

**`ZxTransQueryHandle` 实现：**

`queryRegisterDetails()`：
- `bizFunc = "24"`
- `acctNo` = 自有资金账户编号（SM2加密）
- reserve 填充：TRANS_TYPE、REGISTER_ATTR（从配置读）、TRANS_DATE、PAGE、laasSsn

#### 4.2.3 新增请求 DTO

**`BasRegisterDetailQueryReq`** — 登记簿明细查询请求：
- 继承或参考 `BasTransDetailQueryReq`
- 新增字段：`registerAttr`（登记簿类型）、`transType`（交易类型）

**`BasTransTransferReq` 扩展**：
- 新增字段：`dealType`（交易类型）、`fundTp`（资金类型）、`contractId`（合同编号）

#### 4.2.4 Router 层

无需改动。`BeanPostProcessor` 自动发现 Handle 方法。

### 4.3 Web 层 — 通知入口

**`ReverseNoticeController.transNotifyZx()` 改造：**

现有逻辑：只处理 `trans_tp` 为 `{01, 02}` 的通知。

改造：
- `trans_tp` 过滤集合新增 `03`（自有资金入金）
- 当 `trans_tp = "03"` 时：
  1. 跳过现有公司校验逻辑（不通过 bankEAccountCode 查公司）
  2. 从 `SELF_FUND_ACCOUNT_CONFIG` 读取 `cardCode`
  3. 用 `cardCode` 查找系统虚拟账户的 company
  4. 走正常充值流程：`transConsumeApi.rechargeTrans()`，`accountType=04`
  5. 创建通知记录时 `transType` 标记为 `03`

```java
// 伪代码
if ("03".equals(transTp)) {
    SelfFundAccountConfig config = getSelfFundConfig();
    companyInfo = queryByCardCode(config.getCardCode());
} else {
    // 现有逻辑
    companyInfo = queryByBankEAccountCode(mchntId);
}
```

### 4.4 Consume 层 — 业务逻辑

#### 4.4.1 入金 Pack 组件

**改造点**：`trans_tp=03` 分支处理

- 判断 transType=03 → 标记为自有资金入金
- 跳过标准账户校验（不校验 bankEAccountId、不校验余额上限等）
- 从 `SELF_FUND_ACCOUNT_CONFIG` 获取 `cardCode`
- 用 `cardCode` 构建 `ConsumeRechargeRequest`，`accountType=04`
- 充值金额与银行通知金额一致

#### 4.4.2 现有 IC 充值兼容性

需确认 `chainRecharge` LiteFlow 链路中各组件（Pack/Check/Trans/After）对 `trans_tp=03` 是否有兼容性问题：
- **Pack**：需改造，支持 03 类型数据打包
- **Check**：需评估是否需要跳过某些校验
- **Trans**：充值核心逻辑，accountType=04 应该兼容
- **After**：后处理（通知消息等）应兼容

### 4.5 Task 层 — 定时任务

#### 4.5.1 新建自有资金补偿 Job

**`ZxSelfFundNotifyJobService`**：
- XXL-Job handler：`zxSelfFundNotifyJobService`
- 配置参数：`ZX_SELF_FUND_TRANS_CONFIG`
- 功能：调用 `queryRegisterDetails(bizFunc=24, registerAttr=12)` 查询自有资金登记簿明细
- 获取明细后，与已充值记录比对（通过流水号去重）
- 未充值的明细 → 调用 `transConsumeApi.rechargeTrans()` 充值
- 时间窗口：跟 ZX 现有 Job 类似，整点处理昨天数据，非整点处理近 30 分钟

**`ZxSelfFundNotifyRechargeJobService`**：
- XXL-Job handler：`zxSelfFundNotifyRechargeJobService`
- 配置参数：`ZX_SELF_FUND_TRANS_CONFIG`（同上）
- 功能：从通知记录表中查状态为 INIT 的自有资金通知记录，执行充值

> 这是中信渠道自有资金池独有的 Job，不复用现有补偿机制。

---

## 5. 数据流

### 5.1 入金流程（trans_tp=03）

```
银行推送/Web通知（trans_tp=03）
  → ReverseNoticeController 接收
  → 判断 trans_tp=03
  → 跳过标准校验
  → 从 SELF_FUND_ACCOUNT_CONFIG 读 cardCode
  → 查找系统虚拟账户 company
  → 创建通知记录（transType=03）
  → transConsumeApi.rechargeTrans()
  → chainRecharge LiteFlow
  → 充值到 04 子账户
```

### 5.2 补偿流程

```
ZxSelfFundNotifyJobService（定时触发）
  → 读 ZX_SELF_FUND_TRANS_CONFIG
  → queryRegisterDetails(bizFunc=24, registerAttr=12)
  → 获取自有资金登记簿明细
  → 流水号去重
  → 创建通知记录（INIT）
  → ZxSelfFundNotifyRechargeJobService
  → 查 INIT 状态记录
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
| 1 | base | 脚本初始化 company + 04 账户 + bas_param_t 配置 |
| 2 | front | Handle 接口抽象 + Zx 实现（platformPay/platformReceive/queryRegisterDetails）+ DTO |
| 3 | web | ReverseNoticeController 支持 trans_tp=03 |
| 4 | consume | 入金 Pack 组件支持 03 分支 + 确认 IC 充值兼容性 |
| 5 | task | 新建 ZxSelfFundNotifyJobService + ZxSelfFundNotifyRechargeJobService |

---

## 7. 待确认项

- [ ] `chainRecharge` LiteFlow 链路对 trans_tp=03 的兼容性（Check 组件是否需要跳过部分校验）
- [ ] 自有资金账户调账的具体业务场景和流程细节
- [ ] 转账（2041/2042）的业务触发时机（手工/自动）
- [ ] PA 银行自有资金支持时间节点
- [ ] 自有资金账户的账户变动明细记录策略（是否复用 AccountEntryAfterService）
