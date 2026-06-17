# mdl → lsym 差异分析基线（实测口径）

> 建立：2026-06-16 ｜ 状态：**草稿，待 review** ｜ 口径：`--ignore-all-space` 去空格后的真实差异
> 关联：[[README]]（总览/计划，其 A 表已在本文件修正）、[[01-base]]~[[07-web]]（模块清单，待按本文件「lsym 现状」列补齐）
> 来源：mdl 源 `/Users/limeng/workspaces/IdeaProjects_mdl_dep/mdl`、lsym 目标 `/Users/limeng/workspaces/IdeaProjects_lsym_uat/slhy`

本文是对 [README](./README.md) 差异统计的**实测校正 + 功能级 lsym 现状判定**。README 的 B/C 表经校验可信，**A 表不可信**（已重算）。

---

## 0. 方法与口径（重要）

- 两个仓库是独立 git 仓库，历史格式分叉，存在大量**纯空格/回车差异**（`docs/CODE_MIGRATION_ANALYSIS.md` 记录的 728 文件级假差异）。
- 因此「内容不同」一律用 `diff --ignore-all-space` 判定，**裸 `diff -rq` 会把空格差异算进去而严重高估**。
- 功能级「lsym 现状」用关键词 grep 判方向，**仅方向性，文件级 ADD/MERGE 以 §3 的 mdl-only / differ 清单为准**。
- 逐文件清单落盘于工作机 `/tmp/mig/<模块>_{mdl_only,lsym_only,differ,real_diff}.txt`（迁移执行时以实际重新生成为准）。

---

## 1. 校正差异统计

### A. mdl 自 2026-04-01 改动（权威范围）— **重算，替换 README §3.A**
| 模块 | README 旧 | 实测文件数 | java | xml | 其它 |
|---|---|---|---|---|---|
| base | 30 | **41** | 29 | 11 | 1 |
| consume | 136 | **562** | 512 | 39 | 11 |
| front | 35 | **39** | 36 | 2 | 1 |
| management | 83 | **84** | 75 | 8 | 1 |
| report | 110 | **389** | 347 | 38 | 4 |
| task | 23 | **35** | 32 | 2 | 1 |
| web | 79 | **83** | 80 | 2 | 1 |
| **合计** | **496** | **1233** | 1111 | 102 | 20 |

> 提交规模：**427 次非 merge 提交 / 278 条去重 subject**。分类：feat **43** / fix **86** / refactor **15** / chore **2** / 优化调整 **282**。
> ⚠️ README 旧 A 表口径混乱（既非文件数也非提交数），**作废，以本表为准**。

### B. 文件树差异（去空格后真实口径）— **校验 README §3.B，基本吻合**
| 模块 | mdl 独有 ADD(java/xml) | lsym 独有(保留) | 真实 differ | 其中纯空格(忽略) |
|---|---|---|---|---|
| base | 6 / 0 | 11 | 43 | 1 |
| consume | 54 / 7 | 0 | 97 | 368 |
| front | 13 / 0 | 2 | 51 | 0 |
| management | 90 / 7 | 4 | 76 | 116 |
| report | 87 / 12 | 21 | 134 | 432 |
| task | 5 / 0 | 5 | 25 | 0 |
| web | 40 / 0 | 2 | 43 | 2 |
| **合计** | **295 / 26** | **45** | **469** | 919 |

> 结论：ADD 295 java + 26 xml（README C 表记 299/27，吻合）；真实 differ 469（README B 表记 478，吻合）。**README B/C 表可信。**

---

## 2. mdl 新增功能 + bugfix（自 2026-04-01，7 模块）

### 新增功能（feat 43，映射 16 主题，详见 [README §5](./README.md)）
平台批量实收+热点账户异步入账+到账通知查询 ｜ 日终交易明细处理(report→consume 迁移+表重设计) ｜ 自有资金账户阶段2(MC/MR/AT) ｜ 垫支账户配置/结算+advance 汇总反写(热库→SR) ｜ 通知体系(手动批量重发+中核银行实收+收口代扣) ｜ 数据加密(Jasypt+SM4 类型处理器) ｜ 批量下载银行凭证 ｜ 附言提取规则 ｜ 渠道账单报表 ｜ 运营商查询映射/操作日志注解/Feign existsTransfer·WithDraw ｜ 自动提现。

### bugfix（86 fix + 已登记 BC/D/E/AF/F 系列，详见 [bugs/README](../../bugs/README.md)）
集中在**交易一致性/并发/幂等**：批量扣款 E 系列(19)、冻结 BC/C 系列(11)、划付 D 系列(11)、扣款 B 系列(6)、front F/AF 系列；其余门店创建并发、提现 biz_type/biz_code 写反、实收通知去重、日终汇总字段映射等。

---

## 3. 功能 × lsym 现状矩阵（16 主题）

判定：❌ 真·纯新增(lsym 无) ｜ 🟠 补全(lsym 有基础，缺新功能) ｜ 🟡 核对合并(lsym 已有体量，保 lsym 独有)

| # | 功能 | 关键符号 mdl/lsym | 判定 | lsym 现状 / 缺口 |
|---|---|---|---|---|
| 6 | 自有资金**平台付款/收款/扣款** | MC·MR·chainPlatform / 0 | ❌ 纯新增 | lsym 连 MC/MR/chainPlatform 一次引用都没有；`trans/platform/` 5 类 + `SelfFundAccountConfig` 全缺。**最干净**。详见 §4 |
| 2 | 02 划付/中信划付 | 8 / 0(02专属) | 🟠 补全 | **已验证(06-17)**：lsym 有 01 基础全套（TransTransferTiBatchBusiness/Detail/Req/QueryRes，consume-api+service）；**缺** 02/中信：transferTi02/processDetail02/TRANSFER_MODE_SWITCH/TransferTi02Task 全 0。在已有 Ti-batch 上补 02 模式 |
| 7 | 月度调账 | 59 / 0 | ❌ 纯新增 | **已验证(06-17)**：MonthAdjust/MonthBatch/AdjustFee/TzMonth/ZtBatch lsym 全 0（mdl 59）。整套纯新增 |
| 8 | 垫支配置+advance 反写 | 85 / 0 | ❌ 纯新增 | **已验证(06-17)**：insertAdvanceSummary/AdvanceSummary/SelfFundAccountConfig/GdAccountSummarySettleExc lsym in-scope 全 0（mdl 85）。注：lsym 有相关 AT 内部转账垫资基础（见 §4），advance 反写+配置仍为纯新增 |
| 10 | 门店同步与创建 | 19 / 0 | ❌ 纯新增 | **已验证(06-17)**：StoreSync/ScStoreSync/门店同步/StoreTripartite/merchant_store_terminal lsym 全 0（mdl 19）。注：lsym 有门店审核 CheckStoreApp/Contract（须保留），同步/创建为纯新增 |
| 1 | 扣款(B/E 链路) | 34 / 0(专用) | 🟠 补全 | lsym 有 91 个 deduction 明细 DTO，**缺** transDeduction API + chainDeduction + DeductionBatch |
| 3 | 批量冻结/解冻 | 43 / 20 | 🟠 补全 | lsym 有原 FrozenTrans(需求C)，**缺** batchFreeze/批量冻结 |
| 9 | 日终明细 | 75 / 0(专用) | 🟠 补全 | lsym 有 18 个余额变更明细/账户变动查询，**缺** 日终处理任务 + 表重设计(reportDate→bizDate) |
| 5 | 通知体系重构 | 18 / 1 | 🟠 补全 | lsym 仅 1 处 webhook 残留；**缺** AlertMessage 整套 + 手动重发 + 中核银行实收 + 企微替换飞书 |
| 11 | 自动提现 | 38 / 35 | 🟠 补全 | lsym 几乎齐，差几个 |
| 12 | 加密 Jasypt/SM4 | 35 / 19 | 🟠 补全 | lsym 部分，差配置/类型处理器 |
| 13 | 批量下载银行凭证 | 20 / 9 | 🟠 补全 | lsym 部分 |
| 14 | 清分推送+渠道报表 | 76 / 51 | 🟠 补全 | lsym 大部分，差渠道账单查询 |
| 15 | 附言提取规则 | 49 / 43 | 🟠 补全 | lsym 大部分 |
| 4 | 平台批量实收+MQ通知 | 18 / 18 | 🟡 核对合并 | 两边等量；保 lsym `PlatformRechargeBatch*`（注意这是**独立功能**，非自有资金平台付款） |
| 16 | 不明来款上账 | 154 / 166 | 🟡 核对合并 | lsym 反而更多；**保 lsym 独有 `ZxUnidentifiedRemittanceRefundJobService`** |

> ✅ **4 项已交叉验证（2026-06-17，对 lsym=_dep 宽口径 grep 实测）**：A2 升级为 🟠（lsym 有 01 Ti-batch 基础，仅缺 02/中信）；A7/A8/A10 确认 ❌ 纯新增（核心符号 lsym in-scope 全 0）。
> ⚠️ 方法教训：中文词（如「中信」「垫支」）会冲爆命中数，判定是否存在某功能时**只用代码标识符 token**，不混入中文关键词。

---

## 4. 专题：自有资金垫资 —— mdl 全套，lsym 只有半边

自有资金垫资 = ①清结算垫资(内部转账 AT) + ②平台付款/收款/扣款(MC/MR) + 平台链 + 自有资金账户配置。

| 能力 | 交易类型 | mdl | lsym |
|---|---|---|---|
| 清结算垫资(内部转账) | AT | ✅ transTransferInner/InnerPre | ✅ **有**（唯一 lsym 已有的半边） |
| 平台付款 | MC | ✅ transPlatformPay + chainPlatformPay | ❌ |
| 平台收款/垫资扣款 | MR | ✅ transPlatformReceive/Deduction + chainPlatformReceive | ❌ |
| 平台链实现目录 | — | ✅ `trans/platform/` 5 类 | ❌ 目录不存在 |
| 自有资金账户配置 | — | ✅ SelfFundAccountConfig + SELF_FUND_ACCOUNT_CONFIG | ❌ |

**迁移含义**：平台付款/收款是**纯 ADD**（5 platform pack 类 + 3 API + LiteFlow 链 + SelfFundAccountConfig），不与 lsym 冲突；但依赖自有资金账户基础设施（registerType=12 开户、`bas_param_t.SELF_FUND_ACCOUNT_CONFIG`、`bas_card_bin_business_t` MC/MR 卡 BIN），需随 base 一起迁 + 配套 DB/配置。

---

## 5. 按 7 模块：lsym 现状 / 必须保留 / 主要待补

| 模块 | ADD(java/xml) | 真实 differ | lsym 独有(保留) | 主功能 |
|---|---|---|---|---|
| base | 6/0 | 43 | 11 = **DepositReg 保证金全套** | AlertMessage、自有资金、加密、账户接口 |
| consume | 54/7 | 97 | 0 | 扣款/冻结/平台实收/02划付/日终/自动提现（重头） |
| front | 13/0 | 51 | 2 | 提现通知、消息类型、开户协议 |
| management | 90/7 | 76 | 4 = **NpkStoreJoinDkPlan/CheckStoreApp/Contract** | 垫支、门店同步、月度调账、麦当劳网关、商户黑名单 |
| report | 87/12 | 134 | 21 = **ReportTrans*/TNegativeHuafuDetail/SettleActualPayDetail/AmountUtils** | 日终明细报表、清分推送、渠道报表、垫支汇总 |
| task | 5/0 | 25 | 5 = **AccountSumInfo/AutoWithdrawRemarkDto/FlowTransNoInfo/ZxUnidentifiedRemittanceRefundJobService** | 02划付任务、扣款批量任务、Zx 提现通知 |
| web | 40/0 | 43 | 2 | 划付/扣款 controller、test 重发、月度调账入口、批量下载凭证 |

---

## 6. 配套项（迁移时别漏，需逐项核实）

- **DB**：`trans_platform_recharge_batch_detail`(uk_trans_no)、垫支账户配置表、自有资金账户字段(registerType=12)、日终汇总/明细/余额变更表。
- **配置**：MQ topic、`MessageTypeTopicEnum`、Redis key(`platform_recharge:card_lock` 30min / `done` 7d)、Jasypt/SM4 密钥、`SELF_FUND_ACCOUNT_CONFIG`、`bas_card_bin_business_t` MC/MR 卡 BIN。

---

## 7. 待办

- [x] 交叉验证 4 个 ❌?（02划付/月度调账/垫支/门店同步），定死 §3 矩阵 — 结论：A2→🟠 补全（lsym 有 01 基础），A7/A8/A10→❌ 纯新增
- [x] 把 §3「lsym 现状」列 + §5 回填进 README 与 01~07（README §3.A/§5 + 01~07 各加「lsym 现状」实测行）
- [x] 按 §1 校正 README 的 A 表（496→1233）
- [x] commit 到 mdl-memory-hub（本地 commit，未 push）。**注**：`_bwcj` 克隆不存在、文档仓库仅单份，**无需「三副本同步」**；本次实际工作代码克隆 = `_dep`（`/IdeaProjects_lsym_dep/slhy`），与 `_uat` 同远端 `slhy.git`、同 commit `46789bc8bf`，分析结论同等适用
- [ ] 之后从 base 起，按本基线开迁
