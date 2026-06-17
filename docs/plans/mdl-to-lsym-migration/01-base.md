# 01 · fund-catering-base 迁移总结（mdl → lsym）

> 迁移顺序：**第 1 步**（账户/领域/接口契约基础，其余模块依赖）｜ [返回总览](./README.md)
> **lsym 现状（实测，权威见 [DIFF-ANALYSIS §5](./DIFF-ANALYSIS.md)）**：ADD 6 java/0 xml ｜ 真实 differ 43 ｜ lsym 独有(保留) 11 = DepositReg 保证金全套 ｜ 主功能 AlertMessage/自有资金/加密/账户接口。本文件下方早期统计数字以 DIFF-ANALYSIS 为准。

## 1. 差异统计
| 维度 | 数量 |
|---|---|
| mdl 自 4 月改动 | 30 |
| mdl 新增（文件树） | 8 |
| lsym 独有 | 11 |
| 内容不同 | 46 |
| **java ADD** | **6**（xml 0） |

## 2. 涉及功能主题
- 通知基础（AlertMessage 告警消息服务）
- 自有资金账户（registerAttr=12）
- 数据加密改造（Jasypt/SM4、敏感数据类型处理器）
- 账户/企业注册接口优化（businessType 去必选、余额查询、提现规则返回参数）
- 告警统一加到 baseService

## 3. 新增文件 ADD（6 java）
- `AlertMessageApi` / `AlertMessageController` / `AlertMessageReq` / `AlertMessageService` / `AlertMessageServiceImpl` — 告警消息通知整套（base 层基础能力）
- `WsBankServiceApi` — 网商银行服务 Feign 接口

## 4. 修改文件 MODIFY（自 4 月，三路合并，关键）
- 账户/企业注册相关 domain/service/controller（接口签名变化 → 注意 API 契约）
- `bas_business_info` 加密字段处理（移除/改造）
- 余额查询、变更提现规则接口返回参数

> 完整清单：`git -C <mdl> log --since=2026-04-01 --name-only --pretty=format: -- fund-catering-base/ | sort -u`

## 5. 必须保留（lsym 独有，不得删）— DepositReg 保证金登记
- api：`DepositRegReq`、`DepositRegRes`
- service：`BasDepositRegDetailController`、`BasDepositRegDetail`(domain)、`BasDepositRegDetailQueryPageReq/QueryRes/Req`、`BasDepositRegDetailMapper`(.java/.xml)、`BasDepositRegDetailService`/`Impl`

## 6. 关键提交
- `46e21e48` 移除 bas_business_info 加密
- `8f3db6e3` 企业注册接口去掉 businessType 必选项
- `57f083e6` 余额查询 accountNo 报错信息优化
- `83b89e3` 变更提现规则接口返回参数优化

## 7. 迁移动作清单
- [ ] 新增 6 个 java（AlertMessage 整套 + WsBankServiceApi）
- [ ] 三路合并账户/企业注册/加密相关 modify 文件
- [ ] 配套：Jasypt/SM4 密钥与配置（环境相关，lsym 需准备）
- [ ] 核实自有资金账户(registerAttr=12) 表/字段
- [ ] **保留** DepositReg 全套不被删除
- [ ] 编译 base（api + service）

## 8. 依赖
- 无前置（最先迁）。后续 consume/management/web 依赖 base 的账户与接口契约。
