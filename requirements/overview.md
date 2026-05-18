# 需求总览

更新时间：2026-05-18

> 当前实现边界以 `workflow/PROJECT_MEMORY.md`、`topics/` 主题页和当前源码为准。旧讨论、计划和冷冻分支内容不直接作为当前开发依据。

## 当前主线

- 主代码仓：`../slhy/`
- 记忆仓：`../lsym-memory-hub/`
- 当前主工作范围：`slhy/fund-catering`
- 当前活跃分支口径：以 `lsym_prod` / 当前工作分支实际源码为准；`lsym_20260116_limeng_restruct` 是冷冻分支记录。

## 当前高频需求域

1. 消费退款分摊
   - 支持比例退款和订单/逐笔退款。
   - 多收款卡场景以用户传入的 `subTransList.receiveAmount` 为准。
   - 同一 `receiveCardCode` 多条明细需要合并。
   - 账户类型层、科目层、流水层需要兜底，兜底金额必须进入最终落库金额。
   - 当前主题入口：`../topics/consume-refund.md`。

2. 账户变动与 MAC/CAS
   - 交易路径优先排查 `BaseAccountServiceApi` 分场景批量接口。
   - task/回溯路径仍需重点检查旧账户更新方式。
   - 当前主题入口：`../topics/account-change.md`。

3. 自有资金池 / 特殊账户
   - 当前主要是设计和开发指引沉淀。
   - 使用前必须核对当前源码落地状态。
   - 当前主题入口：`../topics/self-fund-account.md`。

4. 平台 03 渠道充值批量高并发上账
   - `task` 负责采集 03 渠道充值流水和 Redis done 预过滤。
   - `consume` 新增内部批量上账接口，入口先抢平台卡锁，抢不到直接跳过不等待。
   - 重复充值流水直接跳过；数据库 `trans_no` 唯一约束兜底。
   - 平台卡锁内按最终待处理流水在内存中计算连续账户变动明细，再批量入库并一次性更新平台卡 04 账户余额。
   - 当前需求规格：`2026-05-18-platform-recharge-batch.md`。

## 当前文档依据

- `../llms.txt`
- `../topics/README.md`
- `../workflow/PROJECT_MEMORY.md`
- `../docs/TRANSACTION_QUICK_REFERENCE.md`
- `../docs/ACCOUNT_CHANGE_SOURCE_MAP.md`
- `../modules/MODULE_FUND_CATERING.md`
