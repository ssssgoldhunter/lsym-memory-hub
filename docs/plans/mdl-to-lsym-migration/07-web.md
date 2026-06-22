# 07 · fund-catering-web 迁移总结（mdl → lsym）

> 迁移顺序：**最后迁移**（HTTP 入口，依赖 base / consume / front / task）｜ [返回总览](./README.md)
>
> **2026-06-21 状态**：本轮只完成对照与决策整理，**尚未迁移 web 代码，未执行编译**。本节结论覆盖下方历史粗统计。

## 0. 最终对照结论（2026-06-21 实测，权威）

### 0.1 对照口径

- mdl：`/Users/limeng/workspaces/IdeaProjects_mdl_dep/mdl/fund-catering-web`
- lsym：`/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-web`
- 排除：`target`、IDE 文件和纯空白差异。
- 当前结果：

| 类型 | 数量 |
|---|---:|
| mdl-only | **56**（55 java + `application-local.yml`） |
| lsym-only | **2 java** |
| 两边共有但内容真实不同 | **44** |
| mdl 自 2026-04-01 起涉及路径 | **81** |

> 旧版的“ADD 40 / differ 43~45”统计已经失效，以本节 `56 / 2 / 44` 为准。

### 0.2 本轮迁移边界

- ✅ 已有前置：base、front、consume、task 的目标功能已迁或正在收口。
- ❌ management、report 本轮没有迁移，因此依赖它们的 web 入口不迁。
- ❌ 提现、实收、通知增强继续采用 lsym 自有实现，不迁 mdl 对应增强。
- ❌ 平台批量实收 + MQ（consume G3）、自动提现（G5）、不明来款（G4）不迁。
- ✅ 本轮 web 聚焦：扣款、02 划付、冻结/解冻查询、平台 MC/MR、自有资金开户补充、批量下载银行凭证。
- 🔴 `application-local.yml` 属于环境密钥配置，禁止复制。
- 🔴 `ScDepositRegReq`、`DepositRegResp` 是 lsym 独有保证金登记契约，必须保留。

## 1. mdl-only 56 项处置

### 1.1 候选迁移：24 java

这些文件与本轮已迁的 base / consume 功能直接配套。明天仍需和对应 controller、converter 一起按方法落地，不能只复制 DTO。

**账户开户补充（2）**
- `ScEditRegistrationReq`
- `ScRegistrationResultReq`

**扣款管理（7）**
- `DeductionTransManageController`
- `ScTransDeductionBatchDetailQueryPageReq`
- `ScTransDeductionBatchDetailReq`
- `ScTransDeductionBatchReq`
- `ScTransDeductionReq`
- `BatchPreDeductionRes`
- `ScTransDeductionBatchDetailRes`

**平台 MC/MR（1）**
- `ScPlatformTransReq`

**02 划付（3）**
- `ScTransTransferTi02DataReq`
- `ScTransTransferTiBatchDetailRes`
- `ScTransTransferTiBatchQueryRes`

**冻结/解冻（2）**
- `ScTransAccountFrozenDetailReq`
- `ScUnFrozenFailRes`

**批量下载银行凭证 A13（9）**
- `BatchDownloadBankReceiptsReq`
- `QueryDownloadStatusReq`
- `BatchBankReceiptsDownloadWebResponse`
- `BatchDownloadBankReceiptsModel`
- `BatchDownloadBankReceiptsWebResponse`
- `DownloadBankReceiptsModel`
- `DownloadBankReceiptsWebResponse`
- `QueryDownloadStatusModel`
- `QueryDownloadStatusWebResponse`

### 1.2 待政策确认：通知手动重发 4 java

技术上它们可对接已迁的 consume `ManualNotifyResendApi` 和 front 消息门面，但和“通知采用 lsym 自有实现”的既定边界可能冲突。明天先确认是否保留管理端手动重发入口，再决定：

- `ManualNotifyResendManageController`
- `ManualNotifyResendRequest`
- `ManualNotifyResendManageService`
- `ManualNotifyResendManageServiceImpl`

### 1.3 明确不迁：28 项（27 java + 1 yml）

**依赖未迁的 management / report / 月度调账 / 门店功能（22 java）**
- `controller/api/store/StoreController`
- `controller/api/store/ZtDataController`
- `TzMonthBatchDateController`
- `ZtBatchDateController`
- `QueryStoreContractReq`
- `QueryStoreInfoReq`
- `ScStoreChannelBindReq`
- `ScStoreFeeRateSyncReq`
- `ScStoreSyncReq`
- `StoreAccountBindReq`
- `TimeRangeQueryReq`
- `BaseBillDetailResponse`
- `LedgerFreezeDebitResponse`
- `PageResultDetailsResp`
- `PageResultResp`
- `SettleInAccResponse`
- `StoreResp`
- `TzMonthPageQueryResponse`
- `TzMonthQueryDetailResponse`
- `StoreContractQueryRes`
- `StoreFeeRateRes`
- `StoreInfoQueryRes`

**实收通知 / 平台通知 / 压测入口（4 java）**
- `AccountTestController`
- `TestBatchNotifyMainController`
- `BatchNotifyRequestRequest`
- `ScPlatformNotifyQueryReq`

> `TestBatchNotifyMainController` 还依赖 management 的 `NpkBusiItemServiceFacadeApi`，不能单独迁入。

**死代码（1 java）**
- `DownloadBankReceiptsResponse`：当前仅自引用，无有效调用方；A13 使用 web response/model 组合，不迁此类。

**环境配置（1）**
- `application-local.yml`：包含环境参数/密钥，不迁。

## 2. 真实 differ 44 项处置

### 2.1 明天优先做方法级合并

| 功能 | 文件 | 决策 |
|---|---|---|
| 开户资料编辑/结果查询 | `AccountController`、`AccountQueryController` | 并 mdl 新入口，保 lsym DepositReg 等既有入口 |
| 扣款 + 平台 MC/MR | `ConsumeTransController` | 并 `scDeduction`、`scPlatformPay`、`scPlatformReceive`、`scPlatformDeduction` |
| 冻结查询 | `FrozenQueryController` | 改接 consume `TransAccountApi.queryTransFrozen/queryTransFrozenDetail` |
| 冻结/解冻交易 | `FrozenTransController`、`ScTransAccountUFrozenRes` | 并元→分转换及 success/fail 返回结构 |
| 02 划付管理 | `TransferTransManageController`、两个批次查询 req | 并新增/分页/详情入口，保 lsym 原有 01 模式逻辑 |
| 批量银行凭证 | `DownloadController` | 并申请批量、查询状态、下载批量文件三个入口 |
| 公共转换 | `BaseConvert` | **只并上述功能所需方法，禁止整文件覆盖** |
| 公共错误返回 | `BaseController` | 评估仅补 `buildErrorResult`，或按 lsym 现有错误风格改 controller |

配套请求/响应需逐字段确认：

- `ScActiveBankAccountReq`
- `ScRegistrationReq`
- `ScBasAccountInfoQueryReq`
- `ScTransAccountFrozenReq`
- `ScUnFrozenReq`
- `ScTransTransferTiBatchDetailQueryPageReq`
- `ScTransTransferTiBatchDetailReq`
- `ScQueryTransDetailResultRes`

### 2.2 明确保留 lsym，不迁 mdl 差异

**提现整簇**
- `WithDrawQueryController`
- `WIthDrawTransController`
- `ScWithdrawReq`
- `ScWithdrawResultRes`
- `WithDrawResultQueryRes`
- `WithDrawConsumeRes`

**实收/通知/本轮排除功能**
- `RechargeQueryController`
- `TestMessageController`
- `ReverseNoticeController`

**配置与环境**
- `pom.xml`：仅在新代码出现真实缺依赖时按需补，不整文件同步。
- `SignAuthInterceptor`
- `WebConfig`
- `bootstrap.yml`
- `logback-spring.xml`

**历史分叉或本轮无关**
- `CreditBillWriteOffController`
- `AccountManageController`
- `BusinessCategorySettleQueryController`
- `controller/manage/cating/StoreController`
- `ScTransferReq`

### 2.3 尚需逐文件复核

mdl 提交 `d33ad54d` 是一次大范围“恢复丢失代码”提交，混有真实修复和历史差异，不能整体套用。明天继续核查：

- `AcctChangeQueryController`
- `ConsumeQueryController`
- `DaySumQueryController`
- `TransferQueryController`
- `TransferTransController`
- `ZxUnidentifiedRemittanceController`
- `SettlementController`

原则：只有能归入本轮已定功能、且 lsym 确实缺失的 method/field 才合并；查询、提现、通知、management/report 相关差异默认保留 lsym。

## 3. 跨模块依赖与风险

### 3.1 consume API 闭环

web 的 A13 批量银行凭证入口依赖 consume 暴露以下能力：

- 申请批量下载
- 查询批量下载状态
- 下载批量凭证

当前 consume 已迁 service/request/response，但对照时发现 `TransConsumeQueryApi` 侧接口可能尚未闭环。明天迁 web 前先静态确认 API/controller 契约，避免 web 先引用不存在的方法。

冻结查询同理依赖 consume G10：

- `TransAccountApi.queryTransFrozen`
- `TransAccountApi.queryTransFrozenDetail`

### 3.2 禁止覆盖 lsym 独有代码

- `ScDepositRegReq`
- `DepositRegResp`
- `BaseConvert` 中 DepositReg 映射
- lsym 既有异步 01 划付、提现、通知和安全拦截逻辑

## 4. 明日执行顺序

1. 复核 §2.3 七个大提交波及文件，完成 44 differ 的最终归类。
2. 静态确认 consume 的冻结查询、扣款、02 划付、A13 API 已闭环。
3. 迁入 §1.1 的 24 个候选文件，通知手动重发 4 项先确认边界。
4. 对 §2.1 controller / converter 做方法级合并，保留 lsym 独有实现。
5. 再做静态引用检查和差异复算。
6. **编译暂不执行，等待用户明确指示。**

## 5. 当前工作区状态

- `fund-catering-web` 当前无新增、修改或删除文件。
- 今天曾用于试对照的复制内容已全部撤销，没有遗留业务代码改动。
- 本次仅更新迁移记录文档。
