# 03 · fund-catering-front 迁移总结（mdl → lsym）

> 迁移顺序：**第 3 步**（平台对接层，依赖 consume-api）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：早期口径「ADD 13 java / differ 51 / 独有 2」已被下方 **§0 最终对账结论** 修正,以 §0 为准。

## 0. 最终对账结论（2026-06-19 实测 + 逐项决策,权威）

> ⚠️ base 的「业务一律用 lsym」**只对 base**;front 按**逐项决策**,有迁有不迁。本节权威,下方 §1~§8 冲突以本节为准。
> lsym 独有实测 = **0**(早期记的 3 个垃圾文件已清理)。

### 通知体系
- **实收(ActualReceipt→清结算)**:mdl handler **不迁**;把清结算 api 调用以**注释+TODO** 搬进 lsym 现有 `HttpRechargeMessageConsumeHandle`(✅已落地)。lsym 已有 Recharge(实收/充值)handler 推商户,只需补「通知清结算」一处。
- **扣款(Deduction→清结算)**:lsym 无对应 handler,**方案A 新建** `HttpDeductionMessageConsumeHandle`(✅已落地,ResultNotifyApi 注释+TODO),用 lsym 已有 `ConsumeNotifyDto`。
- **划付(Transfer→清结算)**:lsym 无对应 handler,**方案A 新建** `HttpTransferMessageConsumeHandle` + `TransferNotifyDto`(✅已落地,ResultNotifyApi 注释+TODO)。
- **ResultNotifyApi**(清结算推送,定义于 mdl `api-modules/api-routing`/`routing-cateringzx`):lsym 全仓无,**全部注释+TODO 占位**,待其他组迁入 routing 接口后放开。
- **共有处理器**(HttpConsume/HttpRecharge/HttpWithDraw)mdl 改动:**一律不要,按 lsym**(含 HttpWithDraw 的提现结果状态/多次失败警告/手动触发/zdy_tx)。
  > ⚠️ **2026-06-21 实测订正**:上述 4 项 lsym **全无**(grep `zdy_tx`/通知结果状态/多次失败重试/手动触发 命中均=0)。"按 lsym"真实含义是 **用户决策不迁、维持 lsym 现状**(主动放弃这批提现增强),非"lsym 已有等价"。HttpWithDraw(383 行 differ)+WithDrawNotifyDto(mdl 4 月 +25 字段)整簇不迁。详见 §9。
- **MessageTypeTopicEnum**:✅新增 DEDUCTION/TRANSFER 两 topic + `getTopicByTopicName` 方法;ACTUAL_RECEIPT 未加(实收走 recharge)。
- **FrontConstants**:✅新增 3 重试常量(FIRST_SEND_TIME_KEY / RETRY_WINDOW_HOURS=1L / RETRY_DELAY_MS=5000L)。

### 银行渠道
- **PA(平安)**:mdl 4月后无改动 → **无迁移内容**。
- **ZX platformPay/platformReceive**:两边都有,**✅已对齐 mdl 投产版**——bizFunc 硬编码 2041/2042、memo 硬编码平台付款/收款、payDate/payTime 用请求传入的 transDate/transTime(原 new Date() 当前时间)。reserveMap 两边本就一致(都只放 laasSsn)。
- **ZX bugfix① 报错信息带银行详情**:lsym **已有**(`setMessage` 用 `sysRespDesc==null?BANK_ERROR_INFO:sysRespDesc` 三元;mdl 写法是 `BANK_ERROR_INFO+":"+frontMessage`,功能等价,lsym 更直接)→ **无需改**。
- **ZX bugfix② 开户传 sigctFlg/agrmNum**:lsym **已有**(`reserveMap` 4 处都在传,与 mdl 一致)→ **无需改**。
- **批量下载凭证 A13**:✅ front 部分 `ZxDownLoadRequest` +6 批量下载字段(acctNo/userIdOpp/userTypeOpp/transStartDate/transEndDate/flag,用途14)已落地;consume(`BankReceiptsService/Impl` + 批量 Request/OutDto)+ web 入口待做(属那些模块)。

### ✅ 已落地代码(working tree, 未提交)
- 改:`HttpRechargeMessageConsumeHandle`、`ZxTransTransferHandle`、`FrontConstants`、`MessageTypeTopicEnum`
- 新建:`HttpDeductionMessageConsumeHandle`、`HttpTransferMessageConsumeHandle`、`TransferNotifyDto`

### 🟡 待落地(非 front 模块)
- [ ] 批量下载凭证 A13 —— consume(`BankReceiptsService/Impl` + 批量 Request/OutDto)+ web 入口(front 的 `ZxDownLoadRequest` 已加批量字段)
- (ZX bugfix①② 经实测 lsym 已有,无需改)

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 35 |
| mdl 新增（文件树） | 16 |
| lsym 独有 | 3 |
| 内容不同 | 50 |
| **java ADD** | **13**（xml 0） |

## 2. 涉及功能主题
- 通知体系（实收/扣款/划付 HTTP 消息消费处理、提现通知）
- 消息类型枚举（MessageTypeTopicEnum）
- 开户协议号传入
- 网商渠道配置（WsChannel）

## 3. 新增文件 ADD（13 java）
- 实收到账通知：`ActualReceiptNotifyDto`、`ActualReceiptNotifyTest`
- HTTP 消息消费处理：`HttpActualReceiptMessageConsumeHandle`、`HttpDeductionMessageConsumeHandle`、`HttpTransferMessageConsumeHandle`
- 划付通知：`TransferNotifyDto`
- 提现通知：`WithDrawNotifyItemDto`
- WS 门面：`FrontTransWSFacadeApi`、`FrontTransWsFacadeController`
- 渠道：`WsChannelConfig`、`WsChannelService`
- 批量：`BatchCreateReq`
- 工具：`WebUtils`

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- 提现通知：增加通知结果状态获取、withdrawType 增加 zdy_tx 自动提现类型
- 开户协议号 + 是否签约协议号传入
- `MessageTypeTopicEnum`（topic 枚举变化 → MQ 路由需对齐）
- scQueryTransDetail 接口返回字段对齐（transTime→umsTxnTime 等）

## 5. 必须保留（lsym 独有）
- 实测 lsym 独有 = **0**(早期记的 3 个垃圾文件 `D:\…`/`km` 已清理)。front 无需保留的 lsym 独有代码。

## 6. 关键提交
- `4b575322` 提现通知增加通知结果状态获取
- `75ebf3ca` 开户协议号以及是否签约协议号传入

## 7. 迁移动作清单（2026-06-19 逐项决策后）
- [x] 实收:清结算 api 注释搬进 `HttpRechargeMessageConsumeHandle`(mdl ActualReceipt 不迁)
- [x] 扣款:新建 `HttpDeductionMessageConsumeHandle`(ResultNotifyApi 注释+TODO)
- [x] 划付:新建 `HttpTransferMessageConsumeHandle` + `TransferNotifyDto`(ResultNotifyApi 注释+TODO)
- [x] MessageTypeTopicEnum +2 topic(DEDUCTION/TRANSFER) + getTopicByTopicName;FrontConstants +3 重试常量
- [x] ZX platformPay/platformReceive 对齐 mdl 投产版
- [x] ZX bugfix① 报错信息带银行详情(ZxAccountHandle)——实测 lsym 已有,无需改
- [x] ZX bugfix② 开户传 sigctFlg/agrmNum(ZxAccountHandle)——实测 lsym 已有,无需改
- [x] 批量下载凭证 A13 front 部分(ZxDownLoadRequest +6 批量字段)
- [ ] 批量下载凭证 A13 consume+web(属那些模块)
- [x] 共有处理器(Consume/Recharge/WithDraw)mdl 改动 —— 不动,按 lsym
- [x] PA 渠道 —— 无迁移内容
- [ ] 编译 front + 冒烟(扣款/划付监听 topic;ResultNotifyApi 迁入后放开注释)

## 8. 依赖
- 前置：base、consume-api。

## 9. differ 全量核查结论（2026-06-21 实测,权威）

> 对 front 全部 **52 个 differ** 逐项核查(引号修正 diff + mdl 自4月净改动隔离) + 13 个 mdl-only 处置。结论:**front 无需追加任何 differ 合并**。

### 9.1 differ 52 全部按 lsym 不动(分类)
| 类别 | 数 | 代表 | 处置 |
|---|---|---|---|
| 配置/测试/历史分叉 | ~20 | logback/pom/bootstrap/SaasZxTest;**AccountServiceImpl(29行)实测4月未动=历史分叉** | 跳过 |
| router+小差异(1-10行) | ~18 | 8 个 Router(各2行)、各 Api/Req/Res/Service | 空格/import/版本,按 lsym |
| 已 commit 后剩余 | ~7 | HttpRecharge/HttpTransfer/HttpDeduction/ZxTransTransfer/MessageTypeTopicEnum/FrontConstants | 核心已迁(d94e217b),剩余按 lsym |
| 共有处理器 | 2 | HttpConsume(66)、HttpWithDraw(383) | 按 lsym(提现增强不迁,见9.3) |

### 9.2 E 类 4 项逐项结论(均不迁)
- **ZxAccountHandle**(143行,mdl4月+60/-35):实质=bugfix①(报错带银行详情)+bugfix②(开户传 sigctFlg/agrmNum),lsym 已有等价;其余 IDE 格式化噪音 → 不迁
- **BasTransTransferReq**(+4 字段 bizFunc/fundType/dealType/bankEAccountId):bizFunc lsym 已在 ZxTransTransferHandle 硬编码 2041/2042 对齐(d94e217b);bankEAccountId lsym 用自有资金池配置(非字段) → 不迁
- **SAASConfig**(+一批 `/****MDL**/` 商户配置 appId_mc/merchantId/privateKey/publicKey/appKey):🔴 **绝对不迁**——mdl 环境 ZX 商户号+密钥,覆盖会打坏 lsym ZX 渠道。与 application-local.yml 同类
- **ConsumeNotifyDto**(-4 字段 bankSsn/bankAccNo/bankAccName/bankRemark):mdl 删银行到账字段精简,lsym 保留 → 不迁

### 9.3 13 个 mdl-only 处置
- **2 DTO 不迁,改用 lsym 现有**(用户 2026-06-21 决策):`ActualReceiptNotifyDto`、`WithDrawNotifyItemDto` —— consume `ManualNotifyResendServiceImpl` 引用它们,需改用 lsym 现有通知对象(见 9.4 / 02-consume.md)
- **11 个有意跳过**:网商/WS门面簇(`FrontTransWSFacadeApi`/`FrontTransWsFacadeController`/`WsChannelConfig`/`WsChannelService`/`WebUtils`/`BatchCreateReq`/`ws`目录,lsym 不做网商渠道,全仓0引用)、`HttpActualReceiptMessageConsumeHandle`(走 recharge,仅注释引用)、`application-local.yml`(活密钥)、`ActualReceiptNotifyTest`(测试)

### 9.4 🔴 编译断链(跨 consume)
consume `ManualNotifyResendServiceImpl`(commit bbe17ffe)真实 import 了未迁的 `front.message.ActualReceiptNotifyDto`(行35/243/263)、`WithDrawNotifyItemDto`(行39/202) → **当前 consume/front 编译不过**。修法:改用 lsym 现有通知对象(lsym `front/message` 现有 `RechargeNotifyDto`/`WithDrawNotifyDto`/`ConsumeNotifyDto`/`TransferNotifyDto`),属 consume 侧改造。

### 9.5 front 最终状态
✅ 已 commit(d94e217b)8 文件正确 ｜ ✅ 52 differ 全部归类,无需追加合并 ｜ ⏭️ 13 mdl-only = 2 DTO 改用 lsym 现有 + 11 跳过 ｜ 🔴 唯一待落地 = consume ManualNotifyResendServiceImpl 改 DTO ｜ ⏭️ 编译验证未做(待 consume DTO 改造后)
