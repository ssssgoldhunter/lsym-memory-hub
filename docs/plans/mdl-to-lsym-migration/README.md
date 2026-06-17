# mdl → lsym fund-catering 迁移总览

> 建立：2026-06-15 ｜ 来源：mdl（活跃开发线）→ 目标：lsym(slhy) UAT ｜ 范围：fund-catering 7 个模块，自 **2026-04-01** 起
> 关联记忆：[[platform-recharge-batch]]、[[wiki-sync-rule]]

## 1. 背景

- mdl 与 lsym(slhy) 共享基础包 `com.chinaums.erp.slhy.catering.*`（mdl 由 lsym 分叉而来）。
- 历史上平台批量实收等曾 **lsym → mdl**；自 2026-04 起 **mdl 成为活跃开发线**，新增大量功能与 bugfix，本次 **mdl → lsym** 回迁。
- **方向已验证**：mdl 较新——`PlatformRechargeBatchServiceImpl` mdl 1284 行 vs lsym 1206 行；`DeductionBatchExecuteServiceImpl`(1250)、`TransDeductionBatchBusinessServiceImpl`(376)、`TransferTi02TaskJobService`(250) 等 lsym 缺失。

## 2. 范围

- **纳入**：fund-catering-`base` / `consume` / `front` / `management` / `report` / `task` / `web`（7 个）。
- **排除**：fund-catering-data-batch（差异最大：mdl 新增 603 / 改 168 / lsym 独有 57，如后续需要单独追加）。
- **时间线**：mdl 自 2026-04-01 起的改动。
- **路径对齐**：mdl 在仓库根 `mdl/fund-catering-*`；lsym 嵌套 `slhy/fund-catering/fund-catering-*`。

## 3. 差异统计

### A. mdl 自 2026-04-01 改动（迁移权威范围 = 1233 文件）— 校正自 [DIFF-ANALYSIS §1](./DIFF-ANALYSIS.md)
> ⚠️ 旧版「496 文件 / 339 提交」口径混乱（既非文件数也非提交数），**已作废**；以本表为准（实测 `git log --since=2026-04-01 --name-only`，未去空格）。
| 模块 | 改动文件 | java | xml | 其它 |
|---|---|---|---|---|
| consume | 562 | 512 | 39 | 11 |
| report | 389 | 347 | 38 | 4 |
| management | 84 | 75 | 8 | 1 |
| web | 83 | 80 | 2 | 1 |
| base | 41 | 29 | 11 | 1 |
| front | 39 | 36 | 2 | 1 |
| task | 35 | 32 | 2 | 1 |
| **合计** | **1233** | **1111** | **102** | **20** |

> 提交规模：**427 次非 merge 提交 / 278 条去重 subject**。分类：feat **43** / fix **86** / refactor **15** / chore **2** / 优化调整 **282**。
> 注：A 表是 mdl「动过的文件」范围（含纯空格触碰）；真正需迁移的工作量见 §B/C 与 [DIFF-ANALYSIS §1.B](./DIFF-ANALYSIS.md)（去空格后真实 differ 469 + ADD 295 java/26 xml）。

### B. 当前文件树整体差异（含 4 月前历史分叉，参考）
| 模块 | mdl 新增 | lsym 独有 | 内容不同 |
|---|---|---|---|
| base | 8 | 11 | 46 |
| consume | 68 | 2 | 103 |
| front | 16 | 3 | 50 |
| management | 102 | 4 | 76 |
| report | 109 | 44 | 133 |
| task | 7 | 5 | 25 |
| web | 45 | 2 | 45 |
| **合计** | **355** | **71** | **478** |

> B 表（833）> A 表（496）的差额 = 4 月前历史分叉，不在本次范围。

### C. 需新增代码文件（java/xml，精简后）
| 模块 | java ADD | xml ADD |
|---|---|---|
| base | 6 | 0 |
| consume | 54 | 7 |
| front | 13 | 0 |
| management | 90 | 7 |
| report | 91 | 13 |
| task | 5 | 0 |
| web | 40 | 0 |
| **合计** | **299** | **27** |

## 4. 同步策略（谨慎三路合并）

1. **新增**：mdl 独有新文件 → 直接加入 lsym。
2. **合并**：两边都有、mdl 自 4 月改过 → 取「mdl 自 2026-04-01 diff」作补丁应用到 lsym，**保留 lsym 侧独有改动**。
3. **保留**：lsym 独有文件 → **一律不删**。
4. mdl/lsym 是两个独立 git 仓库（无自动 merge-base）：
   - `git -C <mdl> diff <mdl@2026-04-01> HEAD -- <path>` 生成补丁；
   - lsym 工作区逐 hunk 判断：mdl 改动→采纳、lsym 独有→保留、冲突→人工合并。

## 5. mdl 4 月以来新功能清单（16 主题 + 小功能）

> 逐功能 lsym 现状判定（❌纯新增 / 🟠补全 / 🟡核对合并）以 [DIFF-ANALYSIS §3 矩阵](./DIFF-ANALYSIS.md) 为准。下表「状态」为早期粗判，A1/A2 已据 06-17 实测修正为 🟠（lsym 有基础，仅缺新功能）。

| # | 功能主题 | 主要模块 | 状态 |
|---|---|---|---|
| 1 | 扣款（冻结/非冻结）批量业务 + 热点账户异步入账 | consume/task | 🟠 补全（lsym 有 91 个 deduction 明细 DTO，缺 transDeduction API+chainDeduction+DeductionBatch）|
| 2 | 02 模式划付 / 中信划付 | consume/task/web | 🟠 补全（lsym 有 01 基础 TransTransferTiBatch 全套，缺 02/中信 transferTi02/TransferTi02Task）|
| 3 | 批量冻结 / 解冻 | consume | 随扣款迁移 |
| 4 | 平台批量实收 + MQ 异步到账通知 | consume/task | lsym 部分（+78 行待合并） |
| 5 | 通知体系重构（企微替换飞书/结构改造/手动批量重发/去重/中核银行实收通知） | base/consume/front/web | 合并 + 新增 |
| 6 | 自有资金账户（阶段1/2，registerAttr=12） | base/consume | 新增 |
| 7 | 月度调账（同步/推送/查询/详情/管理端/取消） | consume/management/web | 新增 |
| 8 | 垫支账户配置及结算 + advance 汇总反写（热库→SR） | management/consume/report | 新增 |
| 9 | 日终交易明细处理（report→consume 迁移 + 表重设计） | consume/report | 新增 |
| 10 | 门店同步与创建（同步/扣率/协议去重/分布式锁/并发修复） | management | 新增 |
| 11 | 自动提现（余额 zdy_tx / 固定 zd_tx / 手动触发） | consume | 新增 |
| 12 | 数据加密改造（Jasypt 配置 + SM4 字段） | base/consume | 环境配置类，需配套密钥 |
| 13 | 批量下载银行凭证 | consume/web | 新增 |
| 14 | 清分结果推送 + 渠道账单报表 | report/web | 新增 |
| 15 | 附言提取规则功能 | consume/management | 新增 |
| 16 | 不明来款上账 | consume/task | 新增 |

**小功能**：distributor_account_status、批量开压测账户接口、运营商查询映射、类型映射动态加载、操作日志注解、Feign 暴露 `existsTransferTransNo`/`existsWithDrawTransNo`、麦当劳网关(Mcd*)、商户黑名单(NpkMchntBlackList)、三方渠道映射(StoreTripartite*)、部门(SysDept)。

**bugfix/优化**：70 个 fix + 222 个优化调整，随对应主题合并。

## 6. 必须保留的 lsym 独有功能（不得删除）

- **保证金/押金登记 DepositReg**（base/task/web）：`BasDepositRegDetail` 全套 + api `DepositRegReq/Res`、`DepositRegJobService`、`ScDepositRegReq`/`DepositRegResp`
- **management**：`NpkStoreJoinDkPlan*`（门店贷款计划）、`CheckStoreApp`/`CheckStoreContract`（门店审核）
- **report**：`ReportTransAcctChangeEntryDetailT*`、`ReportTransTransferTiBatch*`、`TNegativeHuafuDetail*`（负数花付明细）、`SettleActualPayDetail*`、`AmountUtils`
- **task**：`AccountSumInfo`、`AutoWithdrawRemarkDto`、`FlowTransNoInfo`、`ZxUnidentifiedRemittanceRefundJobService`
- 其余 only-in-lsym 多为 `.DS_Store` / 误入仓库的 `D:\…`/`km` → 清理即可。

## 7. 推荐迁移顺序（依赖自底向上）

`base` → `consume(api+service)` → `front` → `management` → `report` → `task` → `web`。每步一个分支 → 编译 → 冒烟 → 合入。

## 8. 风险点

- **API/契约漂移**：base/consume-api 签名变化让 web/management 编译失败 → 先迁 API 再迁实现。
- **DB schema**：`trans_platform_recharge_batch_detail`、垫支账户配置表、自有资金账户(registerAttr=12) → 核实 lsym 是否已有表/字段。
- **配置/MQ topic**：`MessageTypeTopicEnum`、到账通知 topic、Redis key 需补齐。
- **lsym 独有代码**：mdl 删除/重命名但 lsym 仍引用（如 DepositReg）→ 不能盲删。
- **4 月前历史分叉**：496 之外的 differ 文件不在本次范围。

## 9. 验证

1. 编译：`mvn -pl <module> -am compile` 全绿。
2. 冒烟：扣款冻结/非冻结、02 划付、平台批量实收（submit→异步分批→两阶段事务→补偿→MQ 通知；用 test 重发接口验证）、通知触发与重发。
3. 回归：保证金登记、负数花付报表等 lsym 独有功能未被破坏。
4. UAT 部署端到端验证。

## 10. 待办清单

### 按模块
- [ ] 01-base（[详情](./01-base.md)）
- [ ] 02-consume（[详情](./02-consume.md)）
- [ ] 03-front（[详情](./03-front.md)）
- [ ] 04-management（[详情](./04-management.md)）
- [ ] 05-report（[详情](./05-report.md)）
- [ ] 06-task（[详情](./06-task.md)）
- [ ] 07-web（[详情](./07-web.md)）

### 按重点功能
- [ ] 扣款（冻结/非冻结）批量 + 热点账户异步入账
- [ ] 02 模式划付 / 中信划付
- [ ] 批量冻结 / 解冻
- [ ] 平台批量实收 +78 行合并 + detail 建表 + MQ/Redis 配置
- [ ] 通知体系重构（企微/重发/去重/中核银行实收通知）
- [ ] 自有资金账户（阶段1/2）
- [ ] 月度调账
- [ ] 垫支账户配置 + advance 汇总反写
- [ ] 日终交易明细处理迁移
- [ ] 门店同步与创建
- [ ] 自动提现
- [ ] 数据加密改造（配套密钥/配置）
- [ ] 批量下载银行凭证 / 清分推送 / 渠道报表 / 附言提取 / 不明来款上账

### 配套项
- [ ] DB：核实/建立 `trans_platform_recharge_batch_detail`（uk_trans_no）、垫支账户配置表、自有资金账户字段、日终汇总/明细/余额变更表
- [ ] 配置：MQ topic、Redis key、Jasypt/SM4 密钥
- [ ] wiki：更新 `wiki/WIKI_INDEX.md` 与业务域 wiki 页
- [ ] 编译全绿 + 冒烟 + 回归 + UAT

## 11. 各模块迁移文档

- [01-base.md](./01-base.md) — 通知基础(AlertMessage)、自有资金、加密、账户接口
- [02-consume.md](./02-consume.md) — 扣款/冻结解冻/平台批量实收/划付/通知/日终明细（重头）
- [03-front.md](./03-front.md) — 提现通知、消息类型、开户协议
- [04-management.md](./04-management.md) — 垫支、门店同步、月度调账、麦当劳网关、商户黑名单
- [05-report.md](./05-report.md) — 日终明细报表、清分推送、渠道报表、垫支汇总
- [06-task.md](./06-task.md) — 02 划付任务、扣款批量任务、Zx 提现通知
- [07-web.md](./07-web.md) — 划付/扣款管理 controller、test 重发接口、月度调账入口

## 附录：可复现命令

```bash
# mdl 自 4 月改动清单
git -C <mdl> log --since=2026-04-01 --name-only --pretty=format: -- fund-catering-*/  | sort -u
# 文件树差异（对齐模块层）
diff -rq --exclude=target --exclude=.idea --exclude='*.iml' --exclude='*.class' \
  <mdl>/fund-catering-<m> <lsym>/fund-catering/fund-catering-<m>
# 单文件 mdl 补丁
git -C <mdl> diff <mdl@2026-04-01> HEAD -- <path>
```
