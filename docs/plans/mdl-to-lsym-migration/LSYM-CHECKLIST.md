# lsym 视角迁移/改造清单（按模块）

> 建立：2026-06-17 ｜ 口径：**lsym-grounded**（实测 mdl/lsym 文件树 + 符号 grep 对照，非 mdl 文档转述）
> 工作目录：`_dep`（与 `_uat` 同远端同 commit，结论同等适用）
> **本次范围：base / consume / front / task / report / web（6 模块）—— management 不涉及**（分析仍见 [04-management.md](./04-management.md) 与 [FEATURES-BUGS-PATHS](./FEATURES-BUGS-PATHS.md)，需要时再单开）
> 配套：[README](./README.md)（策略/顺序）、[DIFF-ANALYSIS](./DIFF-ANALYSIS.md)（差异基线 + §3 功能矩阵）、[FEATURES-BUGS-PATHS](./FEATURES-BUGS-PATHS.md)（mdl 功能 + 完整文件清单 附录C）
>
> 状态符号：**❌ 纯缺**（lsym 基本没有，整套新增）｜ **🟠 补全**（lsym 有基础，缺 mdl 新增块，三路合并）｜ **🟡 核对**（lsym 已有，合并 + 保 lsym 独有）
>
> ⚠️ 本文取代 README §5 / DIFF-ANALYSIS §3 里早期「mdl 视角」的判定（A2/A6/A9 等已被实测纠正）。

## 0. 总量（lsym 缺失文件，文件树 mdl-only，仅 java/xml）

| 模块 | 缺失(ADD) | 主要功能 | 状态概览 |
|---|---|---|---|
| base | 6 | AlertMessage、账户/加密 | 1❌ + 2🟠 + 14保 |
| consume | 61 | 扣款/冻结/平台实收/02划付/日终/通知/自动提现 | 重头 |
| front | 13 | 通知重构、平台实收 | 1❌ + 1🟡 |
| task | 5 | 02划付、扣款批量、日终 | 2❌ + 2🟡 + 5保 |
| report | 104 | 日终明细、垫支、月调账、清分 | 重头 |
| web | 41 | 下载凭证、扣款、通知、月调账、02划付 | 入口层 |

> 注：ADD 数为文件树 mdl-only（含少量 4 月前历史分叉如 `WsBankServiceApi`）；权威「自 4 月起」子集以 `git log --since=2026-04-01` 为准（见各模块 MODIFY 行）。

---

## 1. base（ADD 6 ｜ MODIFY ~22 ｜ 保留 14）

### ❌ 新增
- **A5 AlertMessage 告警消息整套（5 java）**：`AlertMessageApi`、`AlertMessageReq`、`AlertMessageController`、`AlertMessageService`、`AlertMessageServiceImpl`。base 层告警能力，供各模块注入。
  > ~~`WsBankServiceApi`~~ 跳过：mdl-only 但范围内零引用，迁过去=死代码。

### 🟠 补全（三路合并，~22 文件，均在 mdl 4 月后范围）
- **账户/企业注册接口优化**：`AccountServiceFacadeApi`、`BasBusinessInfoReq`（去 businessType 必选）、`AccountServiceImpl`、`AccountManageServiceImpl`、`BasCardSubAccountTServiceImpl`（余额查询报错 / 变更提现规则返回参数）。
- **A12 加密改造**：`BasBusinessInfo` + `BasBusinessInfoMapper.xml`——mdl **移除** `bas_business_info` 加密字段（`46e21e48`），合并方向=按 mdl 去加密。
- **8 domain + 8 mapper.xml**：`BasAccountBankInfo`/`BasBusinessInfo`/`BasChannelT`/`BasMemberCardRelationT`/`BasMerchantT`/`BasOperatorT`/`BasOrgT`/`BasPlatformInfo` 及对应 Mapper.xml；+ `generator/CodeGenerator`、`logback-spring.xml`。

### 🟡 保留（lsym 独有，绝不删）
- **DepositReg 保证金全套（11）**：`DepositRegReq/Res`、`BasDepositRegDetail*`（Controller/domain/Query*/Req/Mapper.java+.xml/Service/Impl）。
- ⚠️ **文档原漏列的 3 个 lsym 独有**：`FieldUpdateReq`、`UnifiedAccountDeleteReq`、`UnifiedAccountUpdateReq`（账户管理类，mdl 账户重构时勿碰其引用）。

### 配套
- `application-local.yml`（**含活密钥**：jasypt.password、SM4 lib 路径 `/usr/lib/libsm4evp_x64.so`、企微 webhook key、DB 密码）→ 迁 lsym 用 mdl 值还是换 lsym 值，**待你定**。

---

## 2. consume（ADD 61 ｜ MODIFY 97）— 迁移重头

### ❌ 新增
- **A1 扣款（冻结/非冻结）批量（16）**：`DeductionBatchExecuteService`+Impl（**1250 行**）、`TransDeductionBatchBusinessService`+Impl（**376 行**）、`TransDeductionBatchDetailService`+Impl、`TransDeductionBatchBusinessController`、`TransDeductionBatchDetail`(domain)、`TransBatchStatusT`、`TransDeductionBatchDetailMapper`(.java/.xml)、`TransBatchStatusTMapper`(.java/.xml)、flow vo（`DeductionBatchPreCreateVo`/`DeductionTransVo`）；consume-api：`TransDeductionBatchBusinessApi`、`ConsumeDeductionTransRequest`、`ScDeductionBatchReq`、`TransDeductionBatchDetailQueryPageReq` 等。（lsym 已有 91 个 deduction 明细 DTO = 基础在）
- **A9 日终明细（21）**：report→consume 迁移的明细处理 + `bizDate`（reportDate→bizDate 表重设计，mdl 76 处 / lsym 0）。
- **A13 批量下载银行凭证（6）**：`BatchDownloadBankReceiptsRequest`、`QueryDownloadStatusRequest` 等。
- **A5 通知重构（5）**：`ManualNotifyResendApi/Req/Controller/Service/Impl`。
- **A6½ 自有资金 MC/MR 平台交易链**：`trans/platform/` 5 类（`AbstractPlatformTransPack`/`PlatformPayTransPack`(MC)/`PlatformReceiveTransPack`(MR)/`PlatformTrans`/`PlatformTransAfter`）+ `SelfFundAccountConfig`(consume-common) + `transPlatformPay/transPlatformReceive`。
- **A3 批量冻结/解冻（2）**：`FrozenPoolHelper` 等。
- 其它 11 个待逐个归类。

### 🟠 补全（三路合并）
- **A4 平台批量实收**：`PlatformRechargeBatchServiceImpl`（+78 行）+ `buildActualReceiptNotify` 改造（改查 zx 通知表）。
- **A8 垫支**：`insertAdvanceSummary` 反写。
- **A11 自动提现**（lsym 37 / mdl 54）：差一部分。
- **A12 加密**（lsym 48 / mdl 108）：配置 + 类型处理器。
- **A14 清分 + 渠道报表**（lsym 98 / mdl 153）：渠道账单查询。
- **A15 附言提取**（lsym 44 / mdl 54）：差一部分。
- **A2 02 划付** flow 组件。

### 🟡 核对 / 保留
- **A6 自有资金账户/池 + 转账入金**：lsym 已有（自有资金 51 > mdl 40，task 已实现转账入金）。**注意配置机制分歧**：lsym 用「自有资金池配置」，mdl 用 `SelfFundAccountConfig` 类——合并时勿覆盖 lsym 实现。
- **A16 不明来款**：lsym 更多（184 vs 189），保 lsym 独有。
- consume 的 lsym 独有基本为 0。

---

## 3. front（ADD 13 ｜ MODIFY 51）

### ❌ 新增
- **A5 通知重构（5）**：`ActualReceiptNotifyDto`、`TransferNotifyDto`、`WithDrawNotifyItemDto`、`FrontTransWSFacadeApi`、`BatchCreateReq`、`FrontTransWsFacadeController`、`HttpActualReceipt/Deduction/TransferMessageConsumeHandle`、`WsChannelConfig`、`WsChannelService`、`WebUtils`。
- **A4（2）**。
- 其它 6 待归类。

### 🟡 核对
- **A6**：`registerAttr`（front lsym 3 = mdl 3，已有，核对）。

---

## 4. task（ADD 5 ｜ MODIFY 25）

### ❌ 新增
- **A9 日终明细（3）**。
- **A1 扣款批量任务（1）**：`DeductionBatchTaskJobService`。
- **A2 02 划付（1）**：`TransferTi02TaskJobService`（**250 行**）。

### 🟡 核对
- **A6 转账入金**：`ZxTransNotifyRechargeServiceImpl`（lsym 已实现自有资金池充值：transTp=03 或 01+ebankId → 异步充值）——核对配置机制。
- **A4 平台批量实收**：`PlatformRechargeJobService`（lsym 有，合并采集过滤）。

### 🟡 保留（lsym 独有，5）
`AccountSumInfo`、`AutoWithdrawRemarkDto`、`FlowTransNoInfo`、`ZxUnidentifiedRemittanceRefundJobService` 等。

---

## 5. report（ADD 104 ｜ MODIFY 134）— 迁移重头

### ❌ 新增
- **A9 日终明细（43）**：日终处理报表（report→consume 迁移后 report 侧的报表/汇总）。
- **A8 垫支（12）**：`GdAccountSummarySettleExc` 整套（Api/ListReq/ListRes/PageRes/ExportRes/FileRes/Controller/domain/Mapper.java+.xml/Service+Impl）、`HuafuTotalSummaryRes`、`SummaryKey`。
- **A7 月度调账（8）**：`GdMonthAdjustFeeRes`/`GdMonthAdjustFeeDetailRes`、`AdjustFeeDetailApi`/`AdjustFeeListReq`/`AdjustFeeDetailRes`、`AdjustFeeMapper`(.java/.xml)、`AdjustFeeDetailController`。
- **A14 清分 + 渠道报表（7）**：清分结果推送 + 渠道账单报表。
- 其它 34 待归类。

### 🟡 核对 / 保留
- **A16 不明来款**：lsym 184 ≈ mdl 189，核对合并。
- **保留（21）**：`ReportTransAcctChangeEntryDetailT*`、`ReportTransTransferTiBatch*`、`TNegativeHuafuDetail*`、`SettleActualPayDetail*`、`AmountUtils`。

---

## 6. web（ADD 41 ｜ MODIFY 43）

### ❌ 新增
- **A13 批量下载凭证（10）**。
- **A1 扣款（4）**：`DeductionTransManageController`、`ScTransDeduction{Batch,BatchDetail,...}Req` 等。
- **A5 通知（4）**：`TestBatchNotifyMainController`（test 重发）、`ManualNotifyResendRequest`、`BatchNotifyRequestRequest`、`ScPlatformNotifyQueryReq`。
- **A3 冻结（2）**、**A7 月调账（2）**（`TzMonthBatchDateController`、`ZtBatchDateController`、`TimeRangeQueryReq`）、**A2 02划付（1）**（`ScTransTransferTi02DataReq` 等）、**A10 门店（1）**。
- 其它 17 待归类。

### 🟠 补全
- **A14 清分 / 渠道报表** web 入口。

---

## 附 A：16 功能 × 落点模块 + lsym 状态（速查，本次范围 6 模块）

| # | 功能 | base | consume | front | report | task | web | lsym |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| A1 | 扣款批量 | | ● | | | ● | ● | 🟠 |
| A2 | 02划付/中信 | | ◐ | | | ● | ● | 🟠 |
| A3 | 批量冻结/解冻 | | ● | | | | ● | ❌ |
| A4 | 平台批量实收+MQ | | ◐ | ◐ | | ◐ | | 🟡 |
| A5 | 通知体系重构 | ● | ● | ● | | | ● | ❌ |
| A6 | 自有资金账户 | | ●(MR) | ◐ | | ◐ | | 🟡+❌ |
| A7 | 月度调账 | | ◐ | | ● | | ● | ❌ |
| A8 | 垫支 advance | | ◐ | | ● | | | ❌ |
| A9 | 日终明细 | | ● | | ● | ● | | 🟠 |
| A10 | 门店同步/创建 | | | | | | ● | ❌ |
| A11 | 自动提现 | | ◐ | | | | | 🟠 |
| A12 | 加密 Jasypt/SM4 | ◐ | ◐ | | | | | 🟠 |
| A13 | 批量下载凭证 | | ● | | | | ● | ❌ |
| A14 | 清分+渠道报表 | | ◐ | | ● | | ◐ | 🟠 |
| A15 | 附言提取 | | ◐ | | | | | 🟠 |
| A16 | 不明来款 | | ◐ | | ◐ | ◐ | | 🟡 |

> ●=新增  ◐=补全/核对(MODIFY) ｜ A7/A8 的 management 管理端不在本次范围。

## 附 B：建议迁移顺序（6 模块）

1. **base**（无前置；接口契约基础）→ AlertMessage + 账户/加密合掉，解锁下游。
2. **consume**（重头，依赖 base）→ A1/A9/A6平台链 整套新增 + A4/A8/A11/A12/A14/A15 合并。
3. **front**（依赖 consume-api）→ A5 通知整套。
4. **report**（重头）→ A9/A8/A7/A14。
5. **task** → A1/A2/A9 任务 + A4/A6 核对。
6. **web**（最后，依赖以上全部）→ 各功能 controller/test 入口。

> 每步：建分支 → ADD 新增 → 三路合并 MODIFY → 保 lsym 独有 → `mvn -pl <module> -am compile` 全绿 → 冒烟 → 合入。
