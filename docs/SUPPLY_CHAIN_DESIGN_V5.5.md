# 供应链交易系统 - 完整设计文档 v5.5

> **文档版本**: v5.5
> **编制时间**: 2026-03-02
> **项目**: fund-catering-consume
> **负责人**: 李蒙

## 版本更新记录

| 版本 | 日期 | 更新内容 | 更新人 |
|------|------|----------|--------|
| v5.0 | 2026-02-28 | 初始版本，包含完整的5大交易流程详解 | AI 助手 |
| v5.1 | 2026-02-28 | 新增预消费异步流程详解 | AI 助手 |
| v5.2 | 2026-02-28 | 消费交易流程优化：新增完整流程图、异步上账机制详解 | AI 助手 |
| v5.3 | 2026-02-28 | API接口汇总补充完整，新增下载接口，总计22个接口 | AI 助手 |
| v5.4 | 2026-03-02 | 充值/提现/转账/消费退款流程详细化 | AI 助手 |
| v5.5 | 2026-03-02 | 新增充值退款流程详解 | AI 助手 |

## 飞书文档链接

**最新文档**: https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f

## 文档目录

### 一、项目概述
1.1 项目信息
1.2 核心技术框架

### 二、业务流程架构
2.1 LiteFlow 流程编排设计
2.2 核心流程链（完整版）
2.3 流程组件类型
2.4 消费交易流程详解
2.5 预消费异步流程详解
2.6 充值交易流程详解
2.7 充值退款流程详解
2.8 提现交易流程详解
2.9 转账交易流程详解
2.10 消费退款流程详解
2.11 上下文传递设计

### 三、框架结构与流程设计
3.1 业务处理 vs 业务查询
3.2 通用链路结构

### 四、核心交易类型
4.1 交易类型表
4.2 子账户类型
4.3 消费交易接口
4.4 充值交易接口
4.5 转账交易接口
4.6 提现交易接口
4.7 冻结/解冻接口

### 五、数据模型
5.1 核心交易表
5.2 账户变动明细表

### 六、安全机制
6.1 MAC 校验
6.2 分布式锁
6.3 密码校验

### 七、API接口汇总
7.1 消费相关接口（11个）
7.2 充值相关接口（3个）
7.3 转账相关接口（4个）
7.4 提现相关接口（1个）
7.5 冻结/解冻接口（2个）
7.6 下载相关接口（1个）

### 八、设计亮点
8.1 流程可视化
8.2 组件化设计
8.3 上下文隔离
8.4 事务一致性
8.5 并发安全

### 九、部署信息

### 十、扩展指南
10.1 新增交易类型
10.2 新增子账户类型
10.3 新增校验规则

### 附录
A. 状态码定义
B. 性能指标

---

## 核心流程链

| 流程链名称 | 说明 | Controller |
|------------|------|------------|
| chainConsume | 消费交易流程 | TransConsumeController |
| chainConsumeAuth | 授权消费流程 | TransConsumeController |
| chainConsumeRefund | 消费退款流程 | TransConsumeController |
| chainRecharge | 充值交易流程 | TransRechargeController |
| chainRefundRecharge | 充值退款流程 | TransRechargeController |
| chainTransfer | 转账交易流程 | TransTransferController |
| chainTransferAuth | 授权转账流程 | TransTransferController |
| chainWithDraw | 提现交易流程 | TransWithDrawController |
| chainFrozen | 冻结交易流程 | - |
| chainUnFrozen | 解冻交易流程 | - |
| chainConsumePre | 预消费流程 | TransConsumeController |
| chainConsumeCal | 订单金额计算流程 | TransConsumeController |

## 子账户类型

| 代码 | 名称 | 说明 |
|------|------|------|
| 01 | 现金账户 | 可提现 |
| 02 | 膨胀金账户 | 赠送金额，优先消费，不可提现 |
| 04 | 综合账户 | 综合子账户 |

## 接口总计：22个

| 类别 | 数量 |
|------|------|
| 消费相关 | 11 |
| 充值相关 | 3 |
| 转账相关 | 4 |
| 提现相关 | 1 |
| 冻结/解冻 | 2 |
| 下载相关 | 1 |

---

**本地项目路径**: `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy`
