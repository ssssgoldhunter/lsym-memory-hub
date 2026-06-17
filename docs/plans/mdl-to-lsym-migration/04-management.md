# 04 · fund-catering-management 迁移总结（mdl → lsym）

> 迁移顺序：**第 4 步**（管理后台，依赖 base + consume-api）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 90 java/7 xml ｜ 真实 differ 76 ｜ lsym 独有(保留) 4 = NpkStoreJoinDkPlan/CheckStoreApp/Contract ｜ 主功能 垫支(A8❌)/门店同步(A10❌)/月度调账(A7❌)/麦当劳网关/商户黑名单。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 83 |
| mdl 新增（文件树） | 102 |
| lsym 独有 | 4 |
| 内容不同 | 76 |
| **java ADD** | **90**｜**xml ADD** | **7** |

## 2. 涉及功能主题
垫支账户配置及结算 ｜ 门店同步与创建（扣率/协议/锁/并发） ｜ 月度调账管理端 ｜ 附言提取(npkBusiItem) ｜ 麦当劳网关(Mcd*) ｜ 商户黑名单(NpkMchntBlackList) ｜ 三方渠道映射(StoreTripartite*) ｜ 商户终端(MerchantStoreTerminal) ｜ 手工账单(MpHandBill) ｜ 批量通知(BatchNotify*) ｜ 部门(SysDept)

## 3. 新增文件 ADD（90 java + 7 xml，分组）

**麦当劳网关**
- `McDonaldGatewayProperties`、`McdGatewayFacadeApi` / `Controller`、`McdGatewayRequestInfoReq` / `Res`、`McdTokenService` / `Impl`

**门店（账户历史 / 协议 / 同步）**
- `MpStoreAccountNoHis` / `Mapper` / `Req` / `Res` / `Service` / `Impl`
- `MpStoreAppHis` / `Mapper` / `Service` / `Impl`、`MpStoreAppServiceFacadeApi` / `Controller`
- `BaseQueryStoreContractApiRequest` / `BaseQueryStoreInfoApiRequest`
- `BaseStoreChannelApiResponse` / `BaseStoreContractQueryApiResponse` / `BaseStoreFeeRateApiResponse` / `BaseStoreInfoQueryApiResponse`
- `BaseSyncStoreApiRequest` / `BaseSyncStoreChannelApiRequest` / `BaseSyncStoreFeeRateApiRequest`
- `StoreAccountBindReq` / `BindAndUnbindReq`

**三方渠道 / 商户终端 / 黑名单**
- `StoreTripartiteChannelEnum` / `StoreTripartiteMap` / `Controller` / `Mapper` / `Service` / `Impl`
- `MerchantStoreTerminal` / `ChannelEnum` / `Controller` / `Mapper` / `Service` / `Impl`
- `NpkMchntBlackList` / `Api` / `Controller` / `Res` / `Service` / `Impl` / `NpkMchtBlackListMapper`

**手工账单 / 账单**
- `MpHandBill` / `FacadeApi` / `Controller` / `Mapper` / `Service` / `Impl`、`HandBilRequest`、`BillAccountDto`
- `BillDetailResYponse` / `BillDetailResponse` / `BillMainDetailResponse` / `BillMainQueryPageReq` / `BillQueryDetailResponse`
- `MdlBillMainQueryPageReq` / `MdlBillMainResponse` / `MdlBillQueryDetailResponse` / `MdlMiddleResponse` / `MiddleResponse`

**批量通知**
- `BatchNotifyMidService` / `Impl`、`BatchNotifyMsgBody` / `Request` / `RequestRequest` / `Response` / `ResponseDetail` / `YMsgBody`
- `MdlBatchNotifyMsgBody` / `MdlBatchNotifyRequest`、`NoticeStatusEnum`、`NotifyResult`

**部门 / 其他**
- `SysDept` / `Mapper` / `Res`
- `TempThirdNoInfo` / `Mapper` / `Service` / `Impl`、`MpMchntAppInfoController` / `FacadeApi`、`AppBusiItemRes`、`BasCompanyInfo`

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- 垫支账户配置及结算信息处理（settle）
- 门店同步接口逻辑（扣率同步、协议模板去重、结算账户有效性校验）
- 月度调账管理端
- 门店创建分布式锁 / 并发冲突修复

## 5. 必须保留（lsym 独有，不得删）
- `NpkStoreJoinDkPlanReq` / `NpkStoreJoinDkPlanQueryRes`（门店加入贷款计划）
- `CheckStoreApp` / `CheckStoreContract`（门店审核）

## 6. 关键提交
- `7473b61d` 增加垫支账户配置及结算信息处理
- `60f2b101` / `41f1d341` 门店创建并发错误提示 / 分布式锁释放优化
- `5f9f731b` 门店同步接口逻辑优化

## 7. 迁移动作清单
- [ ] 新增 90 java + 7 xml（按上分组）
- [ ] 三路合并垫支账户 / 门店同步 / 月度调账管理端 / 门店锁
- [ ] DB：垫支账户配置表核实
- [ ] **保留** NpkStoreJoinDkPlan / CheckStoreApp / CheckStoreContract
- [ ] 编译 management（依赖 base + consume-api）

## 8. 依赖
- 前置：base、consume-api。
