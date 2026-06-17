# 03 · fund-catering-front 迁移总结（mdl → lsym）

> 迁移顺序：**第 3 步**（平台对接层，依赖 consume-api）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 13 java/0 xml ｜ 真实 differ 51 ｜ lsym 独有(保留) 2 ｜ 主功能 提现通知/消息类型/开户协议。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

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
- 仅 3 个，均为误入仓库的垃圾文件（`D:\…` 路径、`km`）→ **清理即可，无功能代码**。

## 6. 关键提交
- `4b575322` 提现通知增加通知结果状态获取
- `75ebf3ca` 开户协议号以及是否签约协议号传入

## 7. 迁移动作清单
- [ ] 新增 13 个 java（通知消费处理 + WS 门面 + 渠道）
- [ ] 三路合并提现通知/开户协议/MessageTypeTopicEnum 等修改
- [ ] 配套：MQ topic 与 MessageTypeTopicEnum 对齐
- [ ] 清理 lsym 误入的 `D:\…`/`km` 垃圾文件
- [ ] 编译 front（依赖 consume-api）

## 8. 依赖
- 前置：base、consume-api。
