# 01 · fund-catering-base 迁移总结（mdl → lsym）

> 迁移顺序：**第 1 步**（账户/领域/接口契约基础，其余模块依赖）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：早期口径「ADD 6 java / differ 43 / 独有 11」已被下方 **§0 最终对账结论** 修正,以 §0 为准。

## 0. 最终对账结论（2026-06-19 实测修正,权威）

> 本节为 base 模块逐项对账（mdl master `93b05207..HEAD` × lsym `_dep` 实际文件树 × 两边 git 提交）后的权威结论。**下方 §1~§7 早期统计/动作如与本节冲突,一律以本节为准。**

- **base 业务功能一律不迁（用 lsym 自己的）**：AlertMessage 告警整套、加密改造（Jasypt/SM4/typeHandler/autoResultMap）、账户·开户·注册重构（`AccountServiceImpl` +298/-909、`AccountManageServiceImpl` +91/-593）全部跳过。
- **base 唯一迁入项**：`BasBusinessInfoReq` 新增 `status` 字段（`N-正常 / D-删除`）。
- **跳过项**：`WsBankServiceApi`（实测 mdl 全仓 0 引用 = 死代码）、`application-local.yml`（活密钥,lsym 自配）、`src/test/AccountTest.java`。
- **保留（lsym 独有,绝不删）= 14**：`DepositReg` 保证金 11 + 3 个账户类 `FieldUpdateReq` / `UnifiedAccountDeleteReq` / `UnifiedAccountUpdateReq`。
- **数字校正（旧值 → 实测）**：自 4 月改动 `30 → 41`；内容不同 `46 → 43`；lsym 独有 `11 → 14`。
- ⚠️ **下游代价**：mdl 的 front（`HttpWithDrawMessageConsumeHandle`）/ task（`TransferTiBatchAlertJobService`）/ web（`TestMessageController`）注入 `base.AlertMessageService`；lsym 无此类,改用 lsym 自己的告警（`system-service` 的 `AlertWarningService` 等）。后续迁这些模块时需把 mdl 对 `base.AlertMessageService` 的调用**改接**到 lsym 告警服务,不能直接复制。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 41（旧值误记 30,已校正） |
| mdl 新增（文件树,java） | 8 |
| lsym 独有 | 14 |
| 内容不同（去空格 differ） | 43 |
| **java ADD（文件树）** | **6**（xml 0；按 §0 业务全不迁,实际整文件迁入=0） |

> 以上为 2026-06-19 实测校正;早期 30 / 46 已作废,权威以 §0 为准。

## 2. 涉及功能主题
- 通知基础（AlertMessage 告警消息服务）
- 自有资金账户（registerAttr=12）
- 数据加密改造（Jasypt/SM4、敏感数据类型处理器）
- 账户/企业注册接口优化（businessType 去必选、余额查询、提现规则返回参数）
- 告警统一加到 baseService

## 3. 新增文件 ADD（文件树 mdl-only）

> ⚠️ 按 §0 最终结论,**以下整文件 ADD 全部不迁**（base 业务一律用 lsym 自己的）。唯一实际迁入是 `BasBusinessInfoReq.status` 字段（见 §0,非新文件）。此节仅作文件树记录保留。

- `AlertMessageApi` / `AlertMessageController` / `AlertMessageReq` / `AlertMessageService` / `AlertMessageServiceImpl` — 告警消息通知整套（base 层基础能力）→ **不迁**
- ~~`WsBankServiceApi`~~ — 网商银行服务 Feign 接口 → **跳过**：实测 mdl 全仓 0 引用,死代码
- 文件树另有 `application-local.yml`、`src/test/AccountTest.java` 为 mdl-only,均**不纳入迁移**

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- 账户/企业注册相关 domain/service/controller（接口签名变化 → 注意 API 契约）
- `bas_business_info` 加密字段处理（移除/改造）
- 余额查询、变更提现规则接口返回参数

> 完整清单：`git -C <mdl> log --since=2026-04-01 --name-only --pretty=format: -- fund-catering-base/ | sort -u`

## 5. 必须保留（lsym 独有，不得删）— DepositReg 保证金 + 3 个账户类（共 14）
- api：`DepositRegReq`、`DepositRegRes`
- service：`BasDepositRegDetailController`、`BasDepositRegDetail`(domain)、`BasDepositRegDetailQueryPageReq/QueryRes/Req`、`BasDepositRegDetailMapper`(.java/.xml)、`BasDepositRegDetailService`/`Impl`
- ⚠️ **文档原漏列的 3 个 lsym 独有账户类**（2026-06-19 实测补）:`FieldUpdateReq`、`UnifiedAccountDeleteReq`、`UnifiedAccountUpdateReq`(账户管理类;mdl 账户重构 `AccountServiceImpl`/`AccountManageServiceImpl` 合并时勿碰其引用)

## 6. 关键提交
- `46e21e48` 移除 bas_business_info 加密
- `8f3db6e3` 企业注册接口去掉 businessType 必选项
- `57f083e6` 余额查询 accountNo 报错信息优化
- `83b89e3` 变更提现规则接口返回参数优化

## 7. 迁移动作清单（2026-06-19 对账后）
- [x] 业务功能一律不迁（AlertMessage / 加密 / 账户·开户·注册重构）——用 lsym 自己的
- [x] **唯一迁入**：给 lsym `BasBusinessInfoReq` 加 `status` 字段（`N-正常 / D-删除`)——已落地(2026-06-19)
- [ ] **保留**：DepositReg 11 + `FieldUpdateReq` / `UnifiedAccountDeleteReq` / `UnifiedAccountUpdateReq`（共 14）不被删除
- [ ] 后续迁 front / task / web 时,把 mdl 对 `base.AlertMessageService` 的调用改接到 lsym 告警服务（`AlertWarningService` 等）
- [ ] （跳过）`WsBankServiceApi` / `application-local.yml` / `src/test`
- [ ] 编译 base（api + service）验证 status 字段不破坏现有引用

## 8. 依赖
- 无前置（最先迁）。后续 consume/management/web 依赖 base 的账户与接口契约。
