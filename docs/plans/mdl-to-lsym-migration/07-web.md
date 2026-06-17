# 07 · fund-catering-web 迁移总结（mdl → lsym）

> 迁移顺序：**第 7 步（最后）**（HTTP 入口，依赖以上全部）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 40 java/0 xml ｜ 真实 differ 43 ｜ lsym 独有(保留) 2 ｜ 主功能 划付/扣款 controller/test 重发/月度调账入口/批量下载凭证。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 79 |
| mdl 新增（文件树） | 45 |
| lsym 独有 | 2 |
| 内容不同 | 45 |
| **java ADD** | **40**（xml 0） |

## 2. 涉及功能主题
划付管理 controller ｜ 扣款管理 controller(DeductionTransManage) ｜ test 重发接口(实收通知批量/按 transNo 重发) ｜ 月度调账入口(TzMonth/ZtBatchDate) ｜ 批量下载银行凭证 web ｜ 门店查询/同步接口(ScStore*) ｜ 通知重发(ManualNotifyResend) ｜ 批量通知(BatchNotify)

## 3. 新增文件 ADD（40 java，分组）

**划付 / 扣款管理**
- `DeductionTransManageController`、`ScTransTransferTi02DataReq`、`ScTransTransferTiBatchDetailRes` / `QueryRes`、`ScTransDeductionBatchReq` / `DetailReq` / `DetailQueryPageReq` / `DetailRes`、`ScTransDeductionReq`、`ScTransAccountFrozenDetailReq`、`ScUnFrozenFailRes`、`BatchPreDeductionRes`

**test 重发 / 通知**
- `TestBatchNotifyMainController`（含 `testBatchSendActualReceiptNotify` / `testSendActualReceiptNotifyByTransNo`）
- `ManualNotifyResendRequest`、`BatchNotifyRequestRequest`

**月度调账**
- `TzMonthBatchDateController`、`ZtBatchDateController`、`TimeRangeQueryReq`

**银行凭证批量下载**
- `BatchBankReceiptsDownloadWebResponse`、`BatchDownloadBankReceiptsModel` / `Req` / `WebResponse`、`DownloadBankReceiptsModel` / `Response` / `WebResponse`、`QueryDownloadStatusModel` / `Req` / `WebResponse`

**门店查询 / 同步**
- `QueryStoreContractReq` / `QueryStoreInfoReq`、`ScStoreSyncReq` / `ScStoreFeeRateSyncReq` / `ScStoreChannelBindReq`、`StoreInfoQueryRes` / `StoreContractQueryRes` / `StoreFeeRateRes` / `StoreAccountBindReq`

**其他**
- `AccountTestController`、`ScPlatformTransReq`、`ScRegistrationResultReq`

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- 划付/扣款待处理数据提示逻辑
- 各接口入参/返回字段对齐（如 scQueryTransDetail transTime→umsTxnTime）
- 中信划付管理 controller

## 5. 必须保留（lsym 独有，不得删）
- `ScDepositRegReq`、`DepositRegResp`（保证金登记 web 层，配合 base/task DepositReg）

## 6. 关键提交
- `dfc4fd43` 中信划付功能提交（web 侧）
- `8474d09a` web 端 testBatchSendActualReceiptNotify 批量重发实收消息
- `5b18268e` web 端 testSendActualReceiptNotifyByTransNo 接口
- `60f2b101` 门店并发错误提示

## 7. 迁移动作清单
- [ ] 新增 40 java（划付/扣款管理 / test 重发 / 月度调账入口 / 银行凭证下载 / 门店查询）
- [ ] 三路合并接口字段对齐 / 划付扣款提示逻辑
- [ ] **保留** ScDepositRegReq / DepositRegResp
- [ ] 编译 web（依赖 base + consume + management + report + task）

## 8. 依赖
- 前置：base、consume、management、report、task（最后迁）。
