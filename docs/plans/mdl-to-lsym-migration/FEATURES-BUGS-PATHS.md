# mdl→lsym 迁移：业务功能 + Bug修复 + 代码路径 完整清单

> 数据来源：mdl 自 2026-04-01 起 7 模块（base/consume/front/management/report/task/web）git 记录 + 文件树 diff。
> 路径为 **mdl 仓库内相对路径**，迁移时复制/合并到 lsym 对应位置（lsym 嵌套在 `slhy/fund-catering/` 下，包名同为 `com.chinaums.erp.slhy.catering.*`）。
> 配套：[README.md](./README.md)（策略/顺序/待办）、01-base~07-web.md（每模块）、[RESUME.md](./RESUME.md)（继续提示）。

## 统计
- 提交：feat 34（去重约 26）/ fix 70 / refactor 11 / 优化 222，共 339 次非 merge 提交。
- 需新增代码文件：**299 java + 27 xml**（完整清单见文末附录 C）。
- 改造(MODIFY)文件：自 4 月共 496 文件区间；旗舰如 `PlatformRechargeBatchServiceImpl` mdl 1284 vs lsym 1206(+78)。

---

# A. 新增业务功能（16 主题）

## A1. 扣款（冻结/非冻结）批量业务 + 热点账户异步入账
模块：consume / task / web
- 新增类：
  - `fund-catering-consume/fund-catering-consume-service/.../service/DeductionBatchExecuteService.java` + `impl/DeductionBatchExecuteServiceImpl.java`（**1250 行**）
  - `.../service/TransDeductionBatchBusinessService.java` + `impl/TransDeductionBatchBusinessServiceImpl.java`（**376 行**）
  - `.../service/TransDeductionBatchDetailService.java` + `impl/TransDeductionBatchDetailServiceImpl.java`
  - `.../controller/TransDeductionBatchBusinessController.java`
  - `.../domain/TransDeductionBatchDetail.java`、`TransBatchStatusT.java`
  - `.../mapper/TransDeductionBatchDetailMapper.java/.xml`、`TransBatchStatusTMapper.java/.xml`
  - `.../flow/vo/DeductionBatchPreCreateVo.java`、`DeductionTransVo.java`
  - `.../flow/component/trans/frozen/FrozenPoolHelper.java`
  - `.../flow/component/trans/withDraw/WithDrawRuleCheck.java`
  - consume-api：`TransDeductionBatchBusinessApi.java`、`ConsumeDeductionTransRequest.java`、`ScDeductionBatchReq.java`、`TransDeductionBatchDetailQueryPageReq.java`、`TransDeductionBatchDetailReq.java`、`TransDeductionBatchDetailQueryRes.java`、`BatchPreCreateRes.java`、`UnFrozenFailRes.java`
  - task：`fund-catering-task/.../job/DeductionBatchTaskJobService.java`
  - web：`.../controller/manage/trans/DeductionTransManageController.java`、`request/trans/ScTransDeduction{Batch,BatchDetail,BatchDetailQueryPage,Deduction}Req.java`、`response/trans/ScTransDeductionBatchDetailRes.java`、`BatchPreDeductionRes.java`、`ScUnFrozenFailRes.java`
- 改造(MODIFY)：扣款既有 flow 组件；4 项缺陷对齐（余额校验/冻结流水号/冻结幂等/冻结池一致性）；普通扣款+冻结扣款热点账户锁卡异步入账；`updateResult` 统一到账户变动成功后再更新交易状态 S
- 代表提交：`9692c6e1`/`32ff9375` 冻结解冻流水号+日志、`b86e730a` frozenTransNo 自动生成、`387a9917`/`55d179ea` 普通扣款冻结对齐 legacy、`eeb53464` split batch unfreeze、`0b8a5f58` E 批量扣款通知+冻结池 NPE+D02 异常隔离、`9e444be2` markDetailFailed 补 status、`55ddc3b9` batchPreDeduction 返回失败明细、`868097e4` loadContext 异常回写失败

## A2. 02 模式划付 / 中信划付
模块：consume / task / web
- 新增类：
  - task：`fund-catering-task/.../job/TransferTi02TaskJobService.java`（**250 行**）
  - web：`.../request/trans/ScTransTransferTi02DataReq.java`、`response/trans/ScTransTransferTiBatchDetailRes.java`、`ScTransTransferTiBatchQueryRes.java`
- 改造(MODIFY)：`TransferRecallService`（task）、`flow/component/trans/transfer` 组件、web 划付管理 controller、中信划付逻辑
- 代表提交：`dfc4fd43` 中信划付功能、`划付扣款 冻结解冻 流水号一致性`、`划付明细新增批次号查询条件`

## A3. 批量冻结 / 解冻
模块：consume（随 A1 一并）
- 改造(MODIFY)：`DeductionBatchExecuteServiceImpl.batchUnfreezeTrade()/batchUnfreezeOriginal()`、`flow/component/trans/{frozen,unfrozen}` 批量组件、每 10 笔分组、冻结/解冻金额口径统一为分、Before 组件 ×100 转换修复
- 代表提交：`eeb53464` split unfreeze、`冻结扣款 冻结明细 transno 与 orgFrozenTransNo 修复`、`修复冻结/解冻Before组件漏掉的×100转换`

## A4. 平台批量实收 + MQ 异步到账通知
模块：consume / task（**lsym 已有，需对齐 +78 行**）
- 改造(MODIFY)：
  - `PlatformRechargeBatchServiceImpl`（mdl 1284 vs lsym 1206）、`PlatformRechargeBatchReq/SubmitRes`、`TransConsumeApi.platformRechargeBatch`、`TransConsumeController.platformRechargeBatch`
  - 实收通知 `buildActualReceiptNotify` 改查 zx 通知表 + 充值子表、`transJrno/bankProcessingSerialNo` 改用 zx `frscSenum`
  - `PlatformRechargeJobService`（task 采集过滤）
  - 热点账户异步上账、移除固定等待
- 代表提交：`8323761d` 平台实收批量迁移+到账通知、`2c759e5d`/`03410392` support async posting for hot account、`eebb52db`/`84817854` buildActualReceiptNotify 改造、`dcf79c53` platformNotifyQuery 查询接口

## A5. 通知体系重构（到账/扣款/划付/提现/中核银行/手动重发）
模块：base / consume / front / web
- 新增类：
  - base：`AlertMessageApi`、`AlertMessageReq`、`AlertMessageController`、`AlertMessageService`、`AlertMessageServiceImpl`
  - consume-api：`ManualNotifyResendApi`、`ManualNotifyResendReq`
  - consume-service：`ManualNotifyResendController`、`ManualNotifyResendService`、`ManualNotifyResendServiceImpl`
  - front-api：`ActualReceiptNotifyDto`、`TransferNotifyDto`、`WithDrawNotifyItemDto`、`FrontTransWSFacadeApi`、`BatchCreateReq`
  - front-service：`FrontTransWsFacadeController`、`handle/impl/message/HttpActualReceiptMessageConsumeHandle`、`HttpDeductionMessageConsumeHandle`、`HttpTransferMessageConsumeHandle`、`config/WsChannelConfig`、`service/sass/WsChannelService`、`utils/WebUtils`、`test/ActualReceiptNotifyTest`
  - web：`controller/manage/test/TestBatchNotifyMainController`（含 `testBatchSendActualReceiptNotify`/`testSendActualReceiptNotifyByTransNo`）、`request/management/ManualNotifyResendRequest`、`BatchNotifyRequestRequest`、`request/query/ScPlatformNotifyQueryReq`
- 改造(MODIFY)：消息通知结构改造、企业微信替换飞书、通知金额分转元、`noticeNo` 字段、实收通知去重、Zx 提现状态更新通知（仅成功/失败）
- 代表提交：`da2ce40d` 中核银行实收通知处理器、`02a55569` manual notify resend、`26d65371` 通知测试接口、`45a8d1be` 收口通知链路+代扣一致性、`b02323d3` Zx 提现通知、`ccd5f2e4` 实收通知去重

## A6. 自有资金账户（阶段1/2，registerAttr=12）
模块：base / consume（多为 MODIFY）
- 改造(MODIFY)：虚拟自有资金账户、转账入金、`subaccountType=银行资金账户`、`RechargeTransAfter` 口径
- 代表提交：`1b659cb4`/`a0574fdc` 自有资金第二阶段、`aa174f14` set subaccountType、`自有资金账户 阶段1 支持虚拟自有资金账户 转账入金`

## A7. 月度调账（同步/推送/查询/详情/管理端/取消/校验结算日期）
模块：consume / management / report / web
- 新增类：
  - web：`controller/manage/cating/TzMonthBatchDateController`、`ZtBatchDateController`、`request/query/TimeRangeQueryReq`
  - report：`response/databatch/GdMonthAdjustFeeRes`、`GdMonthAdjustFeeDetailRes`、`api/databatch/AdjustFeeDetailApi`、`request/databatch/AdjustFeeListReq`、`response/databatch/AdjustFeeDetailRes`、`mapper/databatch/AdjustFeeMapper.java/.xml`、`controller/databatch/AdjustFeeDetailController`
- 改造(MODIFY)：consume 调账扣款、management 管理端、`gd_payment_system_notice_main`/`gd_huafu_detail` 查询
- 代表提交：`月度调账` 系列（同步/推送/查询/详情/管理端/取消/校验结算日期）

## A8. 垫支账户配置及结算 + advance 汇总反写（热库→SR）
模块：management / consume / report
- 新增类（report 侧）：
  - `api/databatch/GdAccountSummarySettleExcApi`、`request/databatch/GdAccountSummarySettleExcListReq`、`response/databatch/GdAccountSummarySettleExc{ListRes,ListPageRes,ExportRes,FileRes}`、`controller/databatch/GdAccountSummarySettleExcController`、`domain/databatch/GdAccountSummarySettleExc`、`mapper/databatch/GdAccountSummarySettleExcMapper.java/.xml`、`service/databatch/GdAccountSummarySettleExcService`+impl、`response/databatch/HuafuTotalSummaryRes`、`domain/databatch/SummaryKey`
  - management：`domain/BasCompanyInfo`、结算方实体（`b8dc178c`/`511c7d6c`）
- 改造(MODIFY)：management 垫支账户配置、consume `insertAdvanceSummary`、report 数据源切换、热库→SR 延迟 10s 汇总
- 代表提交：`0e15022f` 垫支账户配置及结算、`852574fc`/`aa4d1269` insertAdvanceSummary 反写、`9f893470`/`d13d8517`/`bf3fdc8a` 热库→SR、`b8dc178c`/`511c7d6c` 结算方实体

## A9. 日终交易明细处理（report→consume 迁移 + 表重设计）
模块：consume / report / task
- 新增类：
  - consume（实体+mapper+处理）：`domain/TransDaily{BalanceChangeDetailT,ConsumeDetailT,RechargeDetailT,SummaryT,TransferDetailT}.java`、对应 `mapper/*.java + *.xml`、`domain/TransBatchStatusT`、`mapper/TransBatchStatusTMapper.java/.xml`、`api/TransDailyDetailApi`、`request/TransDailyDetailProcessRequest`、`response/TransDailyDetailProcessResponse`、`controller/TransDailyDetailController`、`service/TransDailyDetailProcessService`+impl
  - report（查询侧）：`api/catering/TransDailyDetailApi`、`request/catering/TransDaily{...}QueryPageReq/Req`、`response/catering/TransDaily{...}Res`、`controller/catering/TransDailyDetailController`、`domain/catering/TransDaily{T...}+TransBatchStatusT`、`mapper/catering/*.java/.xml`、`service/catering/*Service+impl`
  - task：`job/TransDailyDetailJobService`、`service/TransDailyDetailService`+impl
- 改造(MODIFY)：reportDate→bizDate、余额变更明细表新增、批次状态追踪、字段映射/空值/Json
- 代表提交：`14c297b6`/`08a55d33` 日终明细 report→consume、`afeaa57a`/`6709a07c` reportDate→bizDate+余额变更表、`87d80a4c`/`267124b6`/`5df4bf1f` 汇总表字段映射、`11a83670`/`0cbb2d97` 修复处理功能

## A10. 门店同步与创建（同步/扣率/协议/锁/并发）
模块：management / web
- 新增类：
  - management：`domain/{MpStoreAccountNoHis,MpStoreAppHis,StoreTripartiteMap,MerchantStoreTerminal,TempThirdNoInfo}.java`、`mapper/*`(.java/.xml)、`service/*+impl`、`api/{MpStoreAppServiceFacadeApi}.java`、`request/{BaseQueryStoreContractApiRequest,BaseQueryStoreInfoApiRequest,BaseSyncStoreApiRequest,BaseSyncStoreChannelApiRequest,BaseSyncStoreFeeRateApiRequest,BindAndUnbindReq,StoreAccountBindReq,MpStoreAccountNoHisReq}.java`、`response/{BaseStore{Channel,ContractQuery,FeeRate,InfoQuery}ApiResponse,MpStoreAccountNoHisRes}.java`、`controller/{MpStoreAppServiceFacadeController,StoreTripartiteMapController,MerchantStoreTerminalController}.java`、`common/enums/{MerchantStoreTerminalChannelEnum,StoreTripartiteChannelEnum}.java`
  - web：`request/base/{QueryStoreContractReq,QueryStoreInfoReq,ScStoreSyncReq,ScStoreFeeRateSyncReq,ScStoreChannelBindReq,StoreAccountBindReq,ScRegistrationResultReq}.java`、`response/trans/{StoreInfoQueryRes,StoreContractQueryRes,StoreFeeRateRes}.java`、`controller/api/base/account/AccountTestController`
- 改造(MODIFY)：结算门店信息同步逻辑、扣率同步、协议模板 id 去重、结算账户有效性校验、分布式锁、并发冲突修复
- 代表提交：`60f2b101` 并发错误提示、`cec0bb66` 并发冲突、`门店同步接口逻辑优化` 系列、`d128d606` MpStoreAccountNoHis settleCardNo 注解

## A11. 自动提现（余额 zdy_tx / 固定 zd_tx / 手动触发）
模块：consume
- 新增类：`flow/component/trans/withDraw/WithDrawRuleCheck.java`
- 改造(MODIFY)：提现逻辑、consumeId、默认参数/限制、`MessageTypeTopicEnum` 增加 zdy_tx
- 代表提交：`自动提现 zdy_tx/zd_tx` 系列、`提现支持手动触发`、`cf4e5056`/`4eb835b8` 避免过早状态刷新、`0dc0e117` biz_type/biz_code 写反

## A12. 数据加密改造（Jasypt 配置 + SM4 字段）
模块：base / consume
- 改造(MODIFY)：配置加密（db/redis/oss）、字段加密、移除脱敏、SM4 替换 FTP 硬编码凭据、settleCardNo/手机号 类型处理器
- 代表提交：`f1775799` Jasypt+SM4、`253c35f9` 配置加解密工具类、`826fff7b`/`9a975e83` 敏感数据类型处理器、`46e21e48`/`deaabfa3` 移除 bas_business_info 加密、`119bed8a`/`5804a9d4` SM4 替换 FTP 凭据、`67458738` 手机号处理器、`b1769f6f`/`70d33a87` settleCardNo 处理器

## A13. 批量下载银行凭证
模块：consume / web
- 新增类：
  - consume：`service/BankReceiptsService`+impl、`api/request/BatchDownloadBankReceiptsRequest`、`QueryDownloadStatusRequest`、`api/response/BatchDownloadBankReceiptsOutDto`、`QueryDownloadStatusOutDto`
  - web：`request/management/BatchDownloadBankReceiptsReq`、`QueryDownloadStatusReq`、`response/trans/{BatchBankReceiptsDownloadWebResponse,BatchDownloadBankReceiptsModel,BatchDownloadBankReceiptsWebResponse,DownloadBankReceiptsModel,DownloadBankReceiptsResponse,DownloadBankReceiptsWebResponse,QueryDownloadStatusModel,QueryDownloadStatusWebResponse}.java`
- 代表提交：`1bf1f9dd` 实现功能、`b4f4f22a`/`187cc599`/`7121d4f7` 返回报文调整、`b7e11228` 参数调整

## A14. 清分结果推送 + 渠道账单报表
模块：report / web
- 新增类（report）：`api/databatch/ChannelBillReportApi`、`SystemDashboardApi`、`PartitionCleanupApi`、`request/catering/DashboardQueryConditionReq`、`response/databatch/ChannelBillDataStatusRes`、`controller/databatch/{ChannelBillReportController,SystemDashboardApiController,PartitionCleanupApiController}`、`mapper/databatch/{ChannelBillReportMapper,SystemDashboardApiMapper,PartitionCleanupMapper}.java/.xml`、`service/databatch/{ChannelBillReportService,SystemDashboardApiService,PartitionCleanupService,LongReceiveRecordService}+impl`、`domain/databatch/{GdLongTermRecord,SummaryKey}`、`utils/DateUtils`
- 改造(MODIFY)：清分结果推送 url、账单日期分组、按商户号/终端号查询、清算成功记录、prod report 数据源切换
- 代表提交：`6e312889` 渠道账单按商户号/终端号查询、`渠道数据报表分组优化`、`清分结果推送url`

## A15. 附言提取规则功能
模块：consume / management
- 新增类：management `response/AppBusiItemRes.java`
- 改造(MODIFY)：routing 附言提取规则、npkBusiItem 附言字段
- 代表提交：`e32ed59f` 附言提取规则、`5473b61d` npkBusiItem 附言字段+界面

## A16. 不明来款上账
模块：consume / task
- 改造(MODIFY)：银行渠道上账处理、消息通知
- 代表提交：`不明来款 银行渠道上帐 处理优化`、`不明来款消息通知问题修复`

## 小功能 / 接口增强
- Feign 暴露跨服务查重：`existsTransferTransNo`(`e74eb0d3`/`ea179fc7`)、`existsWithDrawTransNo`(`47e47245`/`5a9f0334`)
- 运营商查询映射：`92f71ab0`
- 操作日志注解（系统参数/卡BIN/交易账户）：`f666dd7b`
- 麦当劳网关：management `McdGatewayFacadeApi/Controller`、`McDonaldGatewayProperties`、`McdGatewayRequestInfoReq/Res`、`McdTokenService/Impl`
- 商户黑名单：management `NpkMchntBlackListApi/Controller/Res/Service/Impl`、`NpkMchtBlackListMapper`、`domain/NpkMchntBlackList`
- 部门：management `SysDept`、`SysDeptMapper`、`SysDeptRes`
- 手工账单：management `MpHandBillFacadeApi/Controller`、`MpHandBill/Mapper/Service/Impl`、`HandBilRequest`、`BillAccountDto`、`Bill*Response*`、`MdlBill*`、`MiddleResponse`
- 批量通知中台：management `BatchNotifyMidService/Impl`、`BatchNotify{MsgBody,Request,RequestRequest,Response,ResponseDetail,YMsgBody}`、`MdlBatchNotify{MsgBody,Request}`、`NoticeStatusEnum`、`NotifyResult`
- 分区清理：report `PartitionCleanup*`
- 结算方实体：`b8dc178c`/`511c7d6c`
- 账户业务码配置：report `GdAccountBusicodeConfig*`（api/controller/domain/mapper/service/impl）
- distributor_account_status：`6b2f4126`（report）
- BC/D/E/AF 交易一致性与重试链路收敛：`12aa3d1f`/`0c1568a1`/`534df8d4`/`1d50f516`/`7238a915`/`dccc497d`/`ec006d7a`

---

# B. Bug 修复（按区域，附代表提交 hash）

| 区域 | 代表提交（hash） | 影响类/说明 |
|---|---|---|
| 扣款/冻结/解冻 一致性 | `9692c6e1` `32ff9375` `b86e730a` `387a9917` `55d179ea` `eeb53464` `0b8a5f58` `9e444be2` `55ddc3b9` `868097e4` `45a8d1be` | 冻结明细 transno/orgFrozenTransNo、frozenTransNo 自动生成、普通扣款冻结对齐、split unfreeze、E 批量扣款通知+冻结池 NPE+D02 隔离、markDetailFailed 补 status、batchPreDeduction 返回失败明细、loadContext 异常回写 |
| 平台收付款 | `bf7899dc` `2dd88a2f` `260447f5` `b72b4764` `2c8b0bec` `319e756a` `abc8878a` `ccd5f2e4` | 付款账户可用余额校验+日志、ZX 字段名修正、IC remark、account change detail types、实收通知去重 |
| 日终交易明细 | `87d80a4c` `267124b6` `5df4bf1f` `0ad1501f` `99e60a60` `11a83670` `0cbb2d97` `afeaa57a` `6709a07c` `9ccf1c41` `580c2bf2` `e032bb33` `3fb488ca` `7bcb0300` | 汇总表字段映射/空值/Json、reportDate→bizDate、修复处理功能、时间格式化、trade_time/trans_type/biz_category、消费表字段类型转换、字典中文转换+remark |
| 字典/类型转换 | `309d3cc3` `2a9f40a0` `35205f65` `32985fce` `fb782569` | 更新字典库、支付方式转中文、新增交易类型「T 转账」 |
| 银行凭证批量下载 | `b4f4f22a` `187cc599` `7121d4f7` `b7e11228` | 返回报文调整、状态查询报文、参数调整 |
| 提现 | `0dc0e117` `cf4e5056` `4eb835b8` `5589222b` `e1d886e3` `a9cba2ad` `ca14094c` | biz_type/biz_code 写反、避免过早状态刷新、notify 字段/handlers/payload 对齐、bank_ssn→trans_ssn |
| 门店 | `60f2b101` `cec0bb66` `d128d606` | 并发错误提示、并发冲突、MpStoreAccountNoHis settleCardNo 注解 |
| 加密 | `67458738` `b1769f6f` `70d33a87` `119bed8a` `5804a9d4` | 手机号/settleCardNo 类型处理器、SM4 替换 FTP 凭据 |
| 时间/日志 | `d4af7cc7` `4f421ca1` `e7c774e0` `7649206f` `1850814a` | trans_time 替代 create_time、查询时间日志/前缀、新增所属组织 |
| 转账一致性 | `12aa3d1f` `0c1568a1` `534df8d4` `1d50f516` `7238a915` `dccc497d` `ec006d7a` | 收敛 BC/D/E/AF 交易一致性与重试链路 |
| 通知空指针 | `038f6457` | 补齐 front 通知链路空指针保护 |
| 其他 | `转账污染代码去除` `消费交易多线程并发问题修复` `不明来款消息通知问题修复` | 转账污染、消费并发、不明来款通知 |

---

# C. 附录：完整新增文件清单（299 java + 27 xml，按模块）

> 用 `git -C <mdl> diff` 或文件对照迁移；每个文件 lsym 侧均缺失（ADD）。
> 复现：`diff -rq --exclude=target <mdl>/fund-catering-<m> <lsym>/fund-catering/fund-catering-<m> | grep "^Only in <mdl>"`

### base（6 java）
```
fund-catering-base/fund-catering-base-api/src/main/java/com/chinaums/erp/slhy/catering/base/api/AlertMessageApi.java
fund-catering-base/fund-catering-base-api/src/main/java/com/chinaums/erp/slhy/catering/base/api/WsBankServiceApi.java
fund-catering-base/fund-catering-base-api/src/main/java/com/chinaums/erp/slhy/catering/base/request/AlertMessageReq.java
fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/controller/AlertMessageController.java
fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/AlertMessageService.java
fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/impl/AlertMessageServiceImpl.java
```

### consume（54 java + 7 xml）
```
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/ManualNotifyResendApi.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/TransDailyDetailApi.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/TransDeductionBatchBusinessApi.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/BatchDownloadBankReceiptsRequest.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/ConsumeDeductionTransRequest.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/ManualNotifyResendReq.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/QueryDownloadStatusRequest.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/ScDeductionBatchReq.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/TransDailyDetailProcessRequest.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/TransDeductionBatchDetailQueryPageReq.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/TransDeductionBatchDetailReq.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/BatchDownloadBankReceiptsOutDto.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/BatchPreCreateRes.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/QueryDownloadStatusOutDto.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/TransDailyDetailProcessResponse.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/TransDeductionBatchDetailQueryRes.java
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/UnFrozenFailRes.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/controller/ManualNotifyResendController.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/controller/TransDailyDetailController.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/controller/TransDeductionBatchBusinessController.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransBatchStatusT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDailyBalanceChangeDetailT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDailyConsumeDetailT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDailyRechargeDetailT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDailySummaryT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDailyTransferDetailT.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransDeductionBatchDetail.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/frozen/FrozenPoolHelper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/withDraw/WithDrawRuleCheck.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/vo/DeductionBatchPreCreateVo.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/vo/DeductionTransVo.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransBatchStatusTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransBatchStatusTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyBalanceChangeDetailTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyBalanceChangeDetailTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyConsumeDetailTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyConsumeDetailTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyRechargeDetailTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyRechargeDetailTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailySummaryTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailySummaryTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyTransferDetailTMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDailyTransferDetailTMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDeductionBatchDetailMapper.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransDeductionBatchDetailMapper.xml
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/BankReceiptsService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/DeductionBatchExecuteService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/ManualNotifyResendService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/TransDailyDetailProcessService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/TransDeductionBatchBusinessService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/TransDeductionBatchDetailService.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/BankReceiptsServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/DeductionBatchExecuteServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/ManualNotifyResendServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransDailyDetailProcessServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransDeductionBatchBusinessServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransDeductionBatchDetailServiceImpl.java
```

### front（13 java）
```
fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/api/FrontTransWSFacadeApi.java
fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/message/ActualReceiptNotifyDto.java
fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/message/TransferNotifyDto.java
fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/message/WithDrawNotifyItemDto.java
fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/request/BatchCreateReq.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/config/WsChannelConfig.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/controller/FrontTransWsFacadeController.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/message/HttpActualReceiptMessageConsumeHandle.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/message/HttpDeductionMessageConsumeHandle.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/message/HttpTransferMessageConsumeHandle.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/service/sass/WsChannelService.java
fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/utils/WebUtils.java
fund-catering-front/fund-catering-front-service/src/test/java/com/chinaums/erp/slhy/catering/front/test/ActualReceiptNotifyTest.java
```

### management（90 java + 7 xml）
```
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/api/McdGatewayFacadeApi.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/api/MpHandBillFacadeApi.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/api/MpMchntAppInfoFacadeApi.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/api/MpStoreAppServiceFacadeApi.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/api/NpkMchntBlackListApi.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BaseQueryStoreContractApiRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BaseQueryStoreInfoApiRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BaseSyncStoreApiRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BaseSyncStoreChannelApiRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BaseSyncStoreFeeRateApiRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BatchNotifyMsgBody.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BatchNotifyRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BatchNotifyRequestRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BatchNotifyYMsgBody.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BillMainQueryPageReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/BindAndUnbindReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/HandBilRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/McdGatewayRequestInfoReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/MdlBatchNotifyMsgBody.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/MdlBatchNotifyRequest.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/MdlBillMainQueryPageReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/MpStoreAccountNoHisReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/request/StoreAccountBindReq.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/AppBusiItemRes.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BaseStoreChannelApiResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BaseStoreContractQueryApiResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BaseStoreFeeRateApiResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BaseStoreInfoQueryApiResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BatchNotifyResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BatchNotifyResponseDetail.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BillAccountDto.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BillDetailResYponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BillDetailResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BillMainDetailResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/BillQueryDetailResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/McdGatewayRequestInfoRes.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/MdlBillMainResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/MdlBillQueryDetailResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/MdlMiddleResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/MiddleResponse.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/MpStoreAccountNoHisRes.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/NotifyResult.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/NpkMchntBlackListRes.java
fund-catering-management/fund-catering-management-api/src/main/java/com/chinaums/erp/slhy/catering/management/response/SysDeptRes.java
fund-catering-management/fund-catering-management-common/src/main/java/com/chinaums/erp/slhy/catering/management/common/enums/MerchantStoreTerminalChannelEnum.java
fund-catering-management/fund-catering-management-common/src/main/java/com/chinaums/erp/slhy/catering/management/common/enums/NoticeStatusEnum.java
fund-catering-management/fund-catering-management-common/src/main/java/com/chinaums/erp/slhy/catering/management/common/enums/StoreTripartiteChannelEnum.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/McDonaldGatewayProperties.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/McdGatewayFacadeController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/MerchantStoreTerminalController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/MpHandBillFacadeController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/MpMchntAppInfoController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/MpStoreAppServiceFacadeController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/NpkMchntBlackListController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/controller/StoreTripartiteMapController.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/BasCompanyInfo.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/MerchantStoreTerminal.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/MpHandBill.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/MpStoreAccountNoHis.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/MpStoreAppHis.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/NpkMchntBlackList.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/StoreTripartiteMap.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/SysDept.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/domain/TempThirdNoInfo.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MerchantStoreTerminalMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MerchantStoreTerminalMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpContractTemplateMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpHandBillMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpStoreAccountNoHisMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpStoreAccountNoHisMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpStoreAppHisMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/MpStoreAppHisMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/NpkMchtBlackListMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/NpkMchtBlackListMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/StoreTripartiteMapMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/StoreTripartiteMapMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/SysDeptMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/TempThirdNoInfoMapper.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/mapper/TempThirdNoInfoMapper.xml
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/BatchNotifyMidService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/McdTokenService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/MerchantStoreTerminalService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/MpHandBillService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/MpStoreAccountNoHisService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/MpStoreAppHisService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/NpkMchntBlackListService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/StoreTripartiteMapService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/TempThirdNoInfoService.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/BatchNotifyMidServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/McdTokenServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/MerchantStoreTerminalServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/MpHandBillServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/MpStoreAccountNoHisServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/MpStoreAppHisServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/NpkMchntBlackListServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/StoreTripartiteMapServiceImpl.java
fund-catering-management/fund-catering-management-service/src/main/java/com/chinaums/erp/slhy/catering/management/service/impl/TempThirdNoInfoServiceImpl.java
```

### report（91 java + 13 xml）
```
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/catering/GdAccountBusicodeConfigApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/catering/TransDailyDetailApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/databatch/AdjustFeeDetailApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/databatch/ChannelBillReportApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/databatch/GdAccountSummarySettleExcApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/databatch/PartitionCleanupApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/api/databatch/SystemDashboardApi.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/DashboardQueryConditionReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyBalanceChangeDetailQueryPageReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyBalanceChangeDetailReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyConsumeDetailQueryPageReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyConsumeDetailReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyRechargeDetailQueryPageReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyRechargeDetailReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailySummaryQueryPageReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailySummaryReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyTransferDetailQueryPageReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/catering/TransDailyTransferDetailReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/databatch/AdjustFeeListReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/request/databatch/GdAccountSummarySettleExcListReq.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/ReportTransTransferTiBatchDetailQueryRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailyAdvancePayDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailyBalanceChangeDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailyConsumeDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailyRechargeDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailySummaryRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/catering/TransDailyTransferDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/AdjustFeeDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/ChannelBillDataStatusRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdAccountSummarySettleExcExportRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdAccountSummarySettleExcFileRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdAccountSummarySettleExcListPageRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdAccountSummarySettleExcListRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdMonthAdjustFeeDetailRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/GdMonthAdjustFeeRes.java
fund-catering-report/fund-catering-report-api/src/main/java/com/chinaums/erp/slhy/catering/report/response/databatch/HuafuTotalSummaryRes.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/catering/GdAccountBusicodeConfigController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/catering/TransDailyDetailController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/databatch/AdjustFeeDetailController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/databatch/ChannelBillReportController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/databatch/GdAccountSummarySettleExcController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/databatch/PartitionCleanupApiController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/controller/databatch/SystemDashboardApiController.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/GdAccountBusicodeConfig.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransBatchStatusT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransDailyBalanceChangeDetailT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransDailyConsumeDetailT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransDailyRechargeDetailT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransDailySummaryT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/catering/TransDailyTransferDetailT.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/databatch/GdAccountSummarySettleExc.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/databatch/GdLongTermRecord.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/domain/databatch/SummaryKey.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/GdAccountBusicodeConfigMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/GdAccountBusicodeConfigMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransBatchStatusTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransBatchStatusTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyBalanceChangeDetailTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyBalanceChangeDetailTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyConsumeDetailTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyConsumeDetailTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyRechargeDetailTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyRechargeDetailTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailySummaryTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailySummaryTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyTransferDetailTMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/catering/TransDailyTransferDetailTMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/AdjustFeeMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/AdjustFeeMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/ChannelBillReportMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/ChannelBillReportMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/GdAccountSummarySettleExcMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/GdAccountSummarySettleExcMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/GdLongTermRecordMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/GdLongTermRecordMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/PartitionCleanupMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/PartitionCleanupMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/SystemDashboardApiMapper.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/mapper/databatch/SystemDashboardApiMapper.xml
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/GdAccountBusicodeConfigService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransBatchStatusTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransDailyBalanceChangeDetailTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransDailyConsumeDetailTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransDailyRechargeDetailTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransDailySummaryTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/catering/TransDailyTransferDetailTService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/databatch/ChannelBillReportService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/databatch/GdAccountSummarySettleExcService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/databatch/LongReceiveRecordService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/databatch/PartitionCleanupService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/databatch/SystemDashboardApiService.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/GdAccountBusicodeConfigServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransBatchStatusTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransDailyBalanceChangeDetailTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransDailyConsumeDetailTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransDailyRechargeDetailTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransDailySummaryTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/catering/TransDailyTransferDetailTServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/databatch/ChannelBillReportServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/databatch/GdAccountSummarySettleExcServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/databatch/LongReceiveRecordServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/databatch/PartitionCleanupServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/service/impl/databatch/SystemDashboardApiServiceImpl.java
fund-catering-report/fund-catering-report-service/src/main/java/com/chinaums/erp/slhy/catering/report/utils/DateUtils.java
```

### task（5 java）
```
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/DeductionBatchTaskJobService.java
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/TransDailyDetailJobService.java
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/TransferTi02TaskJobService.java
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/TransDailyDetailService.java
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/TransDailyDetailServiceImpl.java
```

### web（40 java）
```
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/api/base/account/AccountTestController.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/cating/TzMonthBatchDateController.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/cating/ZtBatchDateController.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/test/TestBatchNotifyMainController.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/trans/DeductionTransManageController.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/QueryStoreContractReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/QueryStoreInfoReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/ScRegistrationResultReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/ScStoreChannelBindReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/ScStoreFeeRateSyncReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/ScStoreSyncReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/base/StoreAccountBindReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/management/BatchDownloadBankReceiptsReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/management/BatchNotifyRequestRequest.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/management/ManualNotifyResendRequest.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/management/QueryDownloadStatusReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/query/ScPlatformNotifyQueryReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/query/ScTransAccountFrozenDetailReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/query/TimeRangeQueryReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScPlatformTransReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScTransDeductionBatchDetailQueryPageReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScTransDeductionBatchDetailReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScTransDeductionBatchReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScTransDeductionReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/request/trans/ScTransTransferTi02DataReq.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/BatchBankReceiptsDownloadWebResponse.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/BatchDownloadBankReceiptsModel.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/BatchDownloadBankReceiptsWebResponse.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/BatchPreDeductionRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/DownloadBankReceiptsModel.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/DownloadBankReceiptsResponse.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/DownloadBankReceiptsWebResponse.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/QueryDownloadStatusModel.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/QueryDownloadStatusWebResponse.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/ScTransDeductionBatchDetailRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/ScTransTransferTiBatchDetailRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/ScTransTransferTiBatchQueryRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/ScUnFrozenFailRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/StoreContractQueryRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/StoreFeeRateRes.java
fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/response/trans/StoreInfoQueryRes.java
```

---

# D. 配套项（迁移时别漏）

- **DB 表**：`trans_platform_recharge_batch_detail`(uk_trans_no)、`trans_deduction_batch_detail`、`trans_batch_status_t`、日终 `trans_daily_{summary,consume_detail,recharge_detail,transfer_detail,balance_change_detail}_t`、垫支账户配置表、自有资金账户字段(registerAttr=12)、`gd_account_summary_settle_exc`、`merchant_store_terminal`、`store_tripartite_map`、`npk_mchnt_black_list`、`sys_dept`、`mp_hand_bill`、`mp_store_account_no_his`。
- **配置/MQ/Redis**：`MessageTypeTopicEnum`(topic+ zdy_tx)、到账通知 MQ topic、Redis `platform_recharge:card_lock:*`(30min)/`platform_recharge:done:*`(7d)、Jasypt/SM4 密钥、清分推送 url、prod report 数据源。
- **MODIFY 文件**：附录仅列 ADD；MODIFY（共享文件三路合并）以 `git -C <mdl> diff <mdl@2026-04-01> HEAD -- <path>` 逐文件取补丁，旗舰如 `PlatformRechargeBatchServiceImpl`(+78)。
