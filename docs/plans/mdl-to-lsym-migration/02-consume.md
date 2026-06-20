# 02 · fund-catering-consume 迁移总结（mdl → lsym）

> 迁移顺序：**第 2 步（重头）** — 交易核心：扣款/冻结解冻/平台批量实收/划付/通知/日终明细 ｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：早期口径「ADD 54 java / differ 97 / 独有 0」已被下方 **§0 最终对账结论** 校正(ADD 实测 70 java),以 §0 为准。

## 0. 最终对账结论（2026-06-19 实测校正,权威）

> ⚠️ 本节校正 §1 与 [FEATURES-BUGS-PATHS §C](./FEATURES-BUGS-PATHS.md) 的早期口径(ADD 原记 54 java,实测 **70 java**,漏 20)。冲突以本节为准。
> 口径:mdl `/IdeaProjects_mdl_dep/mdl/fund-catering-consume` × lsym `_dep`,`find+comm` 排除 target。

### 数字校正
- **ADD(mdl-only)= 70 java + 7 xml**(原记 54 java,漏 20);**lsym 独有源码 = 0**;共有 587、differ ~103。
- **文档漏列 20 java**:
  - 平台收付款/自有资金 MC-MR(5):`AbstractPlatformTransPack`、`PlatformPayTransPack`(MC)、`PlatformReceiveTransPack`(MR)、`PlatformTrans`、`PlatformTransAfter`
  - 自有资金配置(1):`SelfFundAccountConfig`
  - 扣款 LiteFlow 组件(7):`DeductionTrans`/`DeductionTransPack`/`DeductionTransAfter`/`DeductionTransBAfter`/`DeductionFrozenPoolSupport`/`DeductionBatchPreCreate`/`DeductionBatchPreCreatePack`
  - 异常测试(4):`TestExceptionApi/Controller/Service/Impl`
  - 测试类(3):`PlatformTransAfterHotAccountTest`、`PlatformTransPackAccountChangeTypeTest`、`ManualNotifyResendServiceImplTest`

### 迁移约束(已定)
- **businessCode / businessType:一律用 lsym 的,保持不变**。ADD 新代码里这俩是 passthrough(无硬编码),自动满足;MODIFY 文件三路合并时 mdl 对这俩的改动**一律跳过**(高频:TransTransferTiBatchBusinessServiceImpl 25 / TransTransferServiceImpl 20 / UnknownTransServiceImpl 18 / TransWithDrawServiceImpl 11 / TransConsumeServiceImpl 10)。
- **lsym 独有逻辑(在共享文件,合并务必保留)**:消费退款分摊机制升级、平台批量实收卡锁(`forceUnlock`/TTL 30→10min)、平台03渠道充值批量上账、自有资金 v2(03走01/02)。

### 新增业务功能 ADD(70 java + 7 xml,6 组)—— ✅ 全迁
① 接口扣款/批量扣款(冻结+非冻结)32 ｜ ② 平台收付款/自有资金 MC-MR 6+2测试 ｜ ③ 日终交易明细 21 ｜ ④ 通知手动重发 5+1测试 ｜ ⑤ 批量下载凭证 A13 6 ｜ ⑥ 异常测试 4
- 注:扣款「交易类型 D(冻结扣款)→ `frozenTransNo = 原始冻结流水 oldFrozenDetail.getTransNo()`;普通扣款→ 自己 transNo」的类型分支,在 ADD① 的 `DeductionTransBAfter`/`DeductionTransAfter` 里,随①迁;lsym `createFrozenDetail` 已兼容(非空 frozenTransNo 保留,不覆盖)。

### 改动 differ 97 —— 最终决策(2026-06-20,方法级细比后全定)
| # | 业务 | 决策 | 要点 |
|:-:|---|---|---|
| G1 | 划付02/中信划付 | 🔄 合并 | 并 mdl 02模式(`batchAddTransferTiDataInternal(mode02)`+`TRANSFER_MODE_02`+`processDetail02`双边下账/上账+校验/冻结/通知);**保 lsym 异步**(`processBatchDataAsync`/`processPayCardAsync`/`processReceiveCardBatchAsync`);集成:按 `transferMode` 分流(02→processDetail02,01→原异步) |
| G2 | 冻结/解冻 | 🔄 合并 | 解冻链路并 mdl(`buildFailRes`+FrozenPoolHelper+orElseThrow);冻结明细:`createFrozenDetail`不改(两边一致)+`checkUnFrozenDetail`合并(并mdl类型分支 D/UF→frozenTransNo + 保lsym UF重复校验,互补)+`checkFrozenDetailTransNoRepeated`死代码忽略;`TransFrozenTServiceImpl/Mapper.updateStatusByTransNoAndCardCode` 签名 int/Boolean 统一 |
| G3 | 平台批量实收+MQ通知 | ❌ 先不迁 | — |
| G4 | 不明来款 | ❌ 先不迁 | — |
| G5 | 自动提现 | ❌ 先不迁 | — |
| G6 | 消费主流程 | ✅ 并入口 / ConsumeTransAfter不动 | 并 `transDeduction`/`transPlatformPay·Receive·Deduction`(ADD①②入口);**`ConsumeTransAfter` 不动**(两边等价,膨胀金/01/02/收款卡锁都有,差异是并发策略bugfix级) |
| G7 | 消费退款分摊 | 🔴 保 lsym | lsym 独有算法 |
| G8 | 各类查询 | 🔴 保 lsym | — |
| G9 | 授信 | 🔴 保 lsym | lsym 独有授信 |
| G10 | 账户变动明细/冻结查询 | 🔄 合并 | 并 mdl `queryTransFrozen`/`queryTransFrozenDetail`;**保 lsym 6 个增删改入口**(账户明细+划付batch add/delete/update) |
| G11 | 充值/退款充值 | 🔴 保 lsym | `setOrgCode` 保lsym(已修对,mdl有bug);`userBusiness*`字段不加(用lsym);`lockKey`保lsym(锁内已重查余额) |
| G12 | 配置/框架 | ⚙️ 部分 | **必须更**:`liteflow/consume.el.xml`(扣款链编排)+`ConsumeServiceUtils.deductionBaseCheck`;**按需并**:`pom.xml`(新依赖)、`FlowChainEnums`/`ConsumeConstants`/`Converter`/`TransSlot`;**保lsym/不动**:logback、bootstrap、CodeGenerator、RedisLockUtils |

### 改造决策汇总
- ✅ 并 mdl 新增:G1(02方法)、G6(transDeduction/transPlatform*入口)、G10(queryTransFrozen)、G12(liteflow.el.xml+deductionBaseCheck+按需)
- 🔄 合并(并mdl+保lsym独有):G1(保异步)、G2(解冻链路+checkUnFrozenDetail合并)、G10(保增删改)
- 🔴 保 lsym:G7、G8、G9、G11
- ❌ 先不迁:G3、G4、G5

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 136 |
| mdl 新增（文件树） | 70 java + 7 xml（实测,原记 54 java 漏 20,见 §0） |
| lsym 独有（源码） | 0 |
| 内容不同 | 103 |
| **java ADD** | **70**｜**xml ADD** | **7** |

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
