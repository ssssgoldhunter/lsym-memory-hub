# 05 · fund-catering-report 迁移总结（mdl → lsym）

> 迁移顺序：**第 5 步**（报表，依赖 base + consume）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 87 java/12 xml ｜ 真实 differ 134 ｜ lsym 独有(保留) 21 = ReportTrans*/TNegativeHuafuDetail/SettleActualPayDetail/AmountUtils ｜ 主功能 日终明细报表/清分推送/渠道报表/垫支汇总。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 110 |
| mdl 新增（文件树） | 109 |
| lsym 独有 | 44 |
| 内容不同 | 133 |
| **java ADD** | **91**｜**xml ADD** | **13** |

## 2. 涉及功能主题
日终交易明细报表（TransDaily* 查询侧） ｜ 清分结果推送 ｜ 渠道账单报表(ChannelBillReport) ｜ 垫支明细汇总(GdAccountSummarySettleExc) + advance 反写 ｜ 系统看板(SystemDashboard) ｜ 分区清理(PartitionCleanup) ｜ 调账手续费(AdjustFee) ｜ 长期记录 ｜ 账户业务码配置(GdAccountBusicodeConfig)

## 3. 新增文件 ADD（91 java + 13 xml，分组）

**日终交易明细查询侧（注意与 consume 侧实体去重对齐）**
- `TransDailyBalanceChangeDetailT` / `Mapper` / `TService` / `Impl` + `QueryPageReq` / `Req` / `Res`
- `TransDailyConsumeDetailT` / `Mapper` / `Service` / `Impl` + `QueryPageReq` / `Req` / `Res`
- `TransDailyRechargeDetailT` / `Mapper` / `Service` / `Impl` + `QueryPageReq` / `Req` / `Res`
- `TransDailySummaryT` / `Mapper` / `Service` / `Impl` + `QueryPageReq` / `Req` / `Res`
- `TransDailyTransferDetailT` / `Mapper` / `Service` / `Impl` + `QueryPageReq` / `Req` / `Res`
- `TransDailyDetailApi` / `Controller`、`TransDailyAdvancePayDetailRes`、`TransBatchStatusT` / `Mapper` / `Service` / `Impl`

**垫支汇总 / 结算异常**
- `GdAccountSummarySettleExc` / `Api` / `Controller` / `ListReq` / `ListRes` / `ListPageRes` / `ExportRes` / `FileRes` / `Mapper` / `Service` / `Impl`
- `HuafuTotalSummaryRes`、`SummaryKey`

**渠道账单 / 看板 / 调账 / 清理**
- `ChannelBillReportApi` / `Controller` / `Mapper` / `Service` / `Impl`、`ChannelBillDataStatusRes`
- `SystemDashboardApi` / `Controller` / `Mapper` / `Service` / `Impl`、`DashboardQueryConditionReq`
- `AdjustFeeDetailApi` / `Controller` / `Res` / `AdjustFeeListReq` / `AdjustFeeMapper`、`GdMonthAdjustFeeRes` / `GdMonthAdjustFeeDetailRes`
- `PartitionCleanupApi` / `Controller` / `Mapper` / `Service` / `Impl`

**账户业务码 / 长期记录**
- `GdAccountBusicodeConfig` / `Api` / `Controller` / `Mapper` / `Service` / `Impl`
- `GdLongTermRecord` / `Mapper`、`LongReceiveRecordService` / `Impl`

**其他**：`ReportTransTransferTiBatchDetailQueryRes`、`DateUtils`

> xml ADD 13：日终明细 mapper xml（BalanceChange/Consume/Recharge/Summary/Transfer）+ 垫支/渠道/看板/清理 mapper xml。

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- `insertAdvanceSummary()`：从 report 库查 advance 汇总 → 反写 consume 库
- prod report 数据源切换
- 日终汇总表字段映射、字典/支付方式中文转换、交易类型补「T 转账」

## 5. 必须保留（lsym 独有，不得删）
- `ReportTransAcctChangeEntryDetailTApi` / `Controller` / `ListReq` / `ListRes` / `Res`
- `ReportTransTransferTiBatchApi` / `Controller` / `ListReq` / `DetailQueryRes` / `ListRes`
- `TNegativeHuafuDetailApi` / `Controller` / `ListReq` / `Res`、`NegativeHuafuDetail`(domain)、`TNegativeHuafuDetailMapper`(.java/.xml)、`TNegativeHuafuDetailService` / `Impl`（负数花付明细，关联 data-batch）
- `SettleActualPayDetailUpdateStatusReq`
- `AmountUtils`
- 其余 only-in-lsym 多为 `.DS_Store` → 清理即可。

## 6. 关键提交
- `852574fc` insertAdvanceSummary advance 汇总反写
- `14c297b6` 日终交易明细 report → consume 迁移
- `6b2f4126` 新增 distributor_account_status
- `0c7fe579` / `6b2f4126` report 相关 merge

## 7. 迁移动作清单
- [ ] 新增 91 java + 13 xml（日终明细查询侧 / 垫支汇总 / 渠道账单 / 看板 / 清理 / 调账）
- [ ] **去重对齐**：日终明细实体与 consume 侧（report 查询侧 vs consume 处理侧）避免重复定义
- [ ] 三路合并 insertAdvanceSummary / 数据源切换 / 字段映射
- [ ] DB：日终汇总/明细/余额变更表、垫支汇总表
- [ ] **保留** ReportTrans* / TNegativeHuafuDetail / SettleActualPayDetail / AmountUtils
- [ ] 清理 lsym `.DS_Store`
- [ ] 编译 report（依赖 base + consume）

## 8. 依赖
- 前置：base、consume。
