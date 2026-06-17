# 06 · fund-catering-task 迁移总结（mdl → lsym）

> 迁移顺序：**第 6 步**（定时任务，依赖 consume-service）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 5 java/0 xml ｜ 真实 differ 25 ｜ lsym 独有(保留) 5 = AccountSumInfo/AutoWithdrawRemarkDto/FlowTransNoInfo/ZxUnidentifiedRemittanceRefundJobService ｜ 主功能 02划付任务/扣款批量任务/Zx提现通知。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 23 |
| mdl 新增（文件树） | 7 |
| lsym 独有 | 5 |
| 内容不同 | 25 |
| **java ADD** | **5**（xml 0） |

## 2. 涉及功能主题
- 02 模式划付任务（TransferTi02TaskJobService）
- 扣款批量任务（DeductionBatchTaskJobService）
- 日终交易明细处理任务（report→consume 迁移配套）
- Zx 提现状态更新通知（仅成功/失败才通知）
- 平台批量实收采集/过滤任务

## 3. 新增文件 ADD（5 java）
- `TransferTi02TaskJobService` — **02 模式划付定时任务（mdl 新增 250 行，lsym 缺失）**
- `DeductionBatchTaskJobService` — 扣款批量任务调度
- `TransDailyDetailJobService` / `TransDailyDetailService` / `TransDailyDetailServiceImpl` — 日终明细处理任务（配合 consume 日终明细迁移）

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- `PlatformRechargeJobService`（平台批量实收采集过滤改造）
- `TransferRecallService`（划付回溯）
- Zx 提现状态更新通知逻辑
- 各类 job 调度配置（cron/参数）

## 5. 必须保留（lsym 独有，不得删）
- `AccountSumInfo`、`AutoWithdrawRemarkDto`、`FlowTransNoInfo`（domain）
- `DepositRegJobService`（保证金登记任务，配合 base DepositReg）
- `ZxUnidentifiedRemittanceRefundJobService`（zx 不明来款退款任务）

## 6. 关键提交
- `b02323d3` Zx 提现状态更新通知：仅成功/失败才发通知
- `8323761d` 平台批量实收 + 到账通知设计文档
- `dfc4fd43` 中信划付功能提交（task 侧任务）

## 7. 迁移动作清单
- [ ] 新增 5 个 java（02 划付任务、扣款批量任务、日终明细任务）
- [ ] 三路合并 PlatformRechargeJobService / TransferRecallService / Zx 通知
- [ ] 配套：日终明细任务调度参数（bizDate/reportDate 口径）
- [ ] **保留** DepositRegJobService 等 5 个 lsym 独有文件
- [ ] 编译 task（依赖 consume-service）

## 8. 依赖
- 前置：base、consume-service。
