# 06 · fund-catering-task 迁移总结（mdl → lsym）

> 迁移顺序：**第 6 步**（定时任务，依赖 consume-service）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：早期口径「ADD 5 / differ 25 / 独有 5」已被下方 **§0 最终对账结论** 校正(ADD 实测 6)并固化逐项决策,以 §0 为准。

## 0. 最终对账结论（2026-06-21 实测 + 逐项决策,权威）

> ⚠️ **约束(用户定):提现 / 实收 / 通知 → lsym 用自己的**(不迁 mdl)。
> 口径:mdl `/IdeaProjects_mdl_dep/mdl/fund-catering-task` × lsym `_dep`,`find+comm` 排 target。

### 库存校正
- **ADD(mdl-only)= 6 java**(原记 5,多 `WithDrawNotifyBuilder`);**lsym 独有 = 5**;共有 84、differ 25。

### ADD 决策
- ✅ **T1 `DeductionBatchTaskJobService`**(扣款批量任务,绑 consume ADD①)
- ✅ **T2 `TransferTi02TaskJobService`**(02 划付任务,绑 consume G1)
- ✅ **T3 `TransDailyDetailJobService`/`TransDailyDetailService`/`Impl`**(日终任务,绑 consume ADD③)
- 🔴 **T4 `WithDrawNotifyBuilder`**(提现通知构建器)——**不迁**(提现用lsym + 依赖 `WithDrawNotifyItemDto` lsym front 没有)
- ⏳ T1/T2/T3 **依赖 consume ADD①/G1/ADD③**,须 consume 迁完再迁(否则编译不过)

### differ 决策(25 项,**全用 lsym**)
- 约束定(提现/实收/通知):D4 Zx提现/D5 Pa提现/D6 Zx充值/D12 Pa提现后/D15 Zx提现后/D18 划付告警(企微)/D20 对账(企微)/D21 不明来款/D23 Pa充值/D24 Zx提现job
- 实测 lsym 已有/更全/mdl 没真改:D1 自动提现(lsym 更全)、D2 日终汇总(lsym 独立 FLWD/AccountSumInfo)、D3 logback、D7 pom、D8 划付AT(通知)、D9 账单(mdl仅merge)、D10 账户分录锁(lsym 已挪@Async)、D11 卡余额(mdl没动)、D13 合同账单(mdl没动)、D14 划付回溯(mdl没真改)、D16 TaskConstants(部分,见下)、D17 bootstrap、D19 账户分录job(lsym已优化)、D22 对账路由(import差)、D25 日终job(import差)

### 额外(3 处,执行时补)
1. **D16 `TaskConstants` 加 2 锁 key**:`DEDUCTION_BATCH_TASK_LOCK_KEY`(T1)、`TRANSFER_TI_02_TASK_LOCK_KEY`(T2)
2. **common-core `CommonConstants` 补 4 常量**(随 consume G1 划付02/ADD① 扣款,lsym 全缺):`TRANSFER_MODE_01`/`TRANSFER_MODE_02`/`TRANSFER_MODE_SWITCH`/`TRANS_TYPE_D`
3. **D5 bugfix**:`PaWithDrawUpdateStatusService` 的 paramResult 判空 `||`→`&&` + 加 model 判空(lsry 有 NPE bug,mdl 已修)

### lsym 独有(5,保)
`AccountSumInfo`、`AutoWithdrawRemarkDto`、`FlowTransNoInfo`、`DepositRegJobService`、`ZxUnidentifiedRemittanceRefundJobService`

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
