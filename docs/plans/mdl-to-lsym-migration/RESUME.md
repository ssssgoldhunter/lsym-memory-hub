# 继续任务提示（明天另一台机器 / 另一个 AI 会话用）

> 把下面「粘贴给 AI」整段复制到明天的新会话即可恢复上下文。

## 粘贴给 AI

```
我在继续一个 mdl → lsym 的代码迁移任务，分析和迁移清单文档已写好，现在要开始实际迁移代码。

任务：把 mdl 项目 fund-catering 7 个模块（base/consume/front/management/report/task/web）自 2026-04-01 起的新功能与 bugfix，回迁到 lsym(slhy) UAT 项目。data-batch 排除。

先读迁移文档（在本仓库）：mdl-memory-hub/docs/plans/mdl-to-lsym-migration/README.md 以及 01-base.md ~ 07-web.md。README 里有「待办清单」和迁移顺序，每个模块 md 末尾有「迁移动作清单」。

关键约定（已和用户确认，不要再问）：
- 范围：7 模块，排除 data-batch，自 2026-04-01 起。
- 策略：谨慎三路合并——mdl 独有文件新增；两边都有的按「mdl 自 4 月 diff」打补丁、保留 lsym 侧独有改动；lsym 独有文件一律不删。
- 必须保留 lsym 独有功能：保证金登记 DepositReg(base/task/web)、门店审核 CheckStoreApp/Contract、门店贷款计划 NpkStoreJoinDkPlan、ReportTrans* / TNegativeHuafuDetail / SettleActualPayDetail / AmountUtils 等。
- 仓库：mdl 源在 /Users/limeng/workspaces/IdeaProjects_mdl_dep/mdl（模块在根 fund-catering-*）；lsym 目标在 /Users/limeng/workspaces/IdeaProjects_lsym_uat/slhy/fund-catering/fund-catering-*。
- 迁移顺序：base → consume(api+service) → front → management → report → task → web。每步一个分支→编译→冒烟→合入。

先从 base 开始，按 01-base.md 的动作清单执行：新增 ADD 文件、三路合并 MODIFY、保留 lsym 独有、配套 DB/配置、编译。开始前先复算差异：
  diff -rq --exclude=target <mdl>/fund-catering-<m> <lsym>/fund-catering/fund-catering-<m>
  git -C <mdl> log --since=2026-04-01 --name-only --pretty=format: -- fund-catering-<m>/ | sort -u
```

## 当前状态（截至 2026-06-15）
- ✅ 已完成：迁移分析 + 迁移清单文档（README + 7 模块 md + 本 RESUME）。
- ❌ 未开始：实际文件 add/merge（496 文件区间，需新增 299 java + 27 xml，三路合并约 478 differ）。

## 数据速查
- mdl 自 2026-04 起 7 模块：339 提交 / 262 去重 / feat34 fix70 refactor11 / 约 16 功能主题。
- 旗舰差异：PlatformRechargeBatchServiceImpl mdl1284 vs lsym1206(+78)；DeductionBatchExecuteServiceImpl(1250)、TransDeductionBatchBusinessServiceImpl(376)、TransferTi02TaskJobService(250) lsym 缺失。
- 重点功能：扣款冻结/非冻结、02 划付(中信)、批量冻结解冻、平台批量实收+MQ通知、通知体系重构、自有资金账户、月度调账、垫支、日终明细迁移、门店同步、自动提现、加密改造(Jasypt/SM4)。

## 配套项（迁移时别漏）
- DB：trans_platform_recharge_batch_detail(uk_trans_no)、垫支账户配置表、自有资金账户字段、日终汇总/明细/余额变更表。
- 配置：MQ topic、MessageTypeTopicEnum、Redis key(platform_recharge:card_lock 30min / done 7d)、Jasypt/SM4 密钥。
