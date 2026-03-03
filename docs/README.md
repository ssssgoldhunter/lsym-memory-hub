# lsym 记忆体文档索引

> 本目录包含lsym项目的所有设计文档和参考资料

## 📚 文档列表

### 核心文档

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [供应链交易系统设计文档 v5.5](./SUPPLY_CHAIN_DESIGN_V5.5.md) | 完整的设计文档，包含六大交易流程详解 | 2026-03-02 |
| [交易流程快速参考](./TRANSACTION_QUICK_REFERENCE.md) | 六大交易流程的快速参考指南 | 2026-03-03 |

## 🔗 外部链接

### 飞书文档
- **主文档**: https://jvn4jogcy6u.feishu.cn
- **最新设计文档**: https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f

### GitHub
- **记忆体仓库**: https://github.com/ssssgoldhunter/lsym-memory-hub

## 📖 六大交易流程

### 1. 消费交易流程
- **流程链**: chainConsume
- **接口**: /scConsumeFree
- **特点**: 02膨胀金优先扣款，支持分账消费

### 2. 充值交易流程
- **流程链**: chainRecharge
- **接口**: /scRecharge
- **特点**: 支持01现金+02膨胀金赠送

### 3. 充值退款流程 ⭐
- **流程链**: chainRefundRecharge
- **接口**: /scRefundRecharge
- **特点**: 原路退回，膨胀金收回

### 4. 提现交易流程
- **流程链**: chainWithDraw
- **接口**: /scWithdraw
- **特点**: 自动提现+人工审核

### 5. 转账交易流程
- **流程链**: chainTransfer
- **接口**: /scTransfer
- **特点**: 三层锁机制，支持批量

### 6. 消费退款流程
- **流程链**: chainConsumeRefund
- **接口**: /scConsumeRefund
- **特点**: 按比例/按单退款

## 🔧 技术架构

### 核心技术栈
- Java 17
- Spring Boot 2.x
- LiteFlow (流程编排引擎)
- MyBatis Plus
- Redis
- Nacos

### 组件类型
- **Pack**: 数据打包、参数校验
- **Check**: 业务规则校验
- **Trans**: 核心交易处理
- **After**: 后处理、账户变动明细
- **Route**: 路由、分支逻辑

## 📊 性能指标

| 交易类型 | 响应时间(P99) | 吞吐量(TPS) |
|----------|---------------|-------------|
| 消费 | 500ms | 1000 |
| 充值 | 300ms | 500 |
| 提现 | 1000ms | 100 |
| 转账 | 500ms | 300 |
| 消费退款 | 500ms | 200 |

---

**最后更新**: 2026-03-03
