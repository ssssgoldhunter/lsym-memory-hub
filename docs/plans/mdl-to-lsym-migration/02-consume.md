# 02 · fund-catering-consume 迁移总结（mdl → lsym）

> 迁移顺序：**第 2 步（重头）** — 交易核心：扣款/冻结解冻/平台批量实收/划付/通知/日终明细 ｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 54 java/7 xml ｜ 真实 differ 97 ｜ lsym 独有(保留) 0 ｜ 主功能 扣款(A1🟠)/冻结/平台实收(A4🟡)/02划付(A2🟠 有01基础)/日终/自动提现。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 136 |
| mdl 新增（文件树） | 68 |
| lsym 独有 | 2 |
| 内容不同 | 103 |
| **java ADD** | **54**｜**xml ADD** | **7** |

## 2. 涉及功能主题
扣款（冻结/非冻结）批量 + 热点账户异步入账 ｜ 批量冻结/解冻 ｜ 平台批量实收 + MQ 到账通知 ｜ 02 划付组件 ｜ 通知体系（实收/扣款/划付 + 手动重发） ｜ 日终交易明细处理（report→consume 迁移） ｜ 自动提现 ｜ 批量下载银行凭证 ｜ 附言提取

## 3. 新增文件 ADD（54 java + 7 xml，分组）

**扣款/冻结/解冻批量**
- `DeductionBatchExecuteService` / `DeductionBatchExecuteServiceImpl`（**1250 行，lsym 缺失**）
- `TransDeductionBatchBusinessApi` / `Controller` / `Service` / `ServiceImpl`（376 行）
- `TransDeductionBatchDetail` / `Mapper` / `QueryPageReq` / `QueryRes` / `Req` / `Service` / `ServiceImpl`
- `DeductionTransVo`、`DeductionBatchPreCreateVo`、`ConsumeDeductionTransRequest`、`ScDeductionBatchReq`
- `BatchPreCreateRes`、`UnFrozenFailRes`、`FrozenPoolHelper`、`WithDrawRuleCheck`

**日终交易明细处理（report→consume 迁移：实体 + mapper + 处理）**
- `TransDailyBalanceChangeDetailT` / `TransDailyConsumeDetailT` / `TransDailyRechargeDetailT` / `TransDailySummaryT` / `TransDailyTransferDetailT`（各含 `Mapper`，共 5 实体 + 5 mapper）
- `TransBatchStatusT` / `Mapper`（批次状态追踪）
- `TransDailyDetailApi` / `Controller` / `ProcessRequest` / `ProcessResponse` / `ProcessService` / `ProcessServiceImpl`

**通知（手动重发）**
- `ManualNotifyResendApi` / `Controller` / `Req` / `Service` / `ServiceImpl`

**银行凭证批量下载**
- `BankReceiptsService` / `Impl`、`BatchDownloadBankReceiptsOutDto` / `Request`、`QueryDownloadStatusOutDto` / `Request`

**异常测试**
- `TestExceptionApi` / `Controller` / `Service` / `ServiceImpl`

> xml ADD 7 个：日终明细 mapper xml + 扣款/批量相关 mapper xml。

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- **平台批量实收**：`PlatformRechargeBatchServiceImpl`（**mdl 1284 vs lsym 1206，+78 行**）、`PlatformRechargeBatchReq/SubmitRes`、`TransConsumeApi.platformRechargeBatch`
- **实收通知**：`buildActualReceiptNotify` 改查 zx 通知表 + 充值子表；`transJrno/bankProcessingSerialNo` 改用 zx `frscSenum`
- **划付**：`flow/component/trans/transfer` 组件、中信划付逻辑
- **自动提现**：余额提现 zdy_tx / 固定金额 zd_tx、提现逻辑 consumeId
- **扣款既有类**：余额校验/冻结流水号/冻结幂等/冻结池一致性对齐；createFrozenDetail frozenTransNo 自动生成

## 5. 必须保留（lsym 独有）
- 仅 2 个 `.DS_Store` → 清理即可，无功能代码。

## 6. 关键提交
- `8323761d` 平台实收批量业务迁移 + 到账通知
- `eebb52db` / `84817854` buildActualReceiptNotify 改查 zx 通知表 + 充值子表
- `b86e730a` 恢复 createFrozenDetail frozenTransNo 自动生成
- `dfc4fd43` 中信划付功能
- `14c297b6` 日终交易明细处理 report → consume

## 7. 迁移动作清单
- [ ] 新增 54 java + 7 xml（扣款批量整套 / 日终明细实体+处理 / 通知重发 / 银行凭证）
- [ ] 三路合并 PlatformRechargeBatchServiceImpl（+78）及相关 api
- [ ] 三路合并实收通知、划付、自动提现、扣款既有类
- [ ] DB：核实 `trans_platform_recharge_batch_detail`（uk_trans_no）、`trans_deduction_batch_detail`、日终汇总/明细/余额变更表、批次状态表
- [ ] 配套：到账通知 MQ topic、Redis key（`platform_recharge:card_lock:*` 30min、`platform_recharge:done:*` 7d）
- [ ] 编译 consume（api + service）

## 8. 依赖
- 前置：base。后续 front/management/report/task/web 依赖 consume。
