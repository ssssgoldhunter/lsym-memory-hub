# fund-catering-consume 框架结构分析

## 一、框架架构概述

项目采用 **LiteFlow 流程编排引擎** 作为核心架构，将业务逻辑分为两大类：

1. **业务处理（Transaction/Process）** - 写操作，修改数据
2. **业务查询（Query）** - 读操作，查询数据

两类业务采用不同的上下文（Slot）、流程配置和组件结构。

---

## 二、业务处理（Transaction）结构

### 2.1 目录结构

```
flow/component/trans/
├── consume/                    # 消费交易
│   ├── auth/                  # 授权相关
│   ├── calOrder/              # 订单计算
│   ├── close/                 # 关闭相关
│   ├── consume/               # 核心消费逻辑
│   ├── frozen/                # 冻结相关
│   └── pre/                   # 预处理相关
├── consumeRefund/             # 消费退款
├── frozen/                    # 冻结交易
├── recharge/                  # 充值交易
├── refundRecharge/           # 充值退款
├── transfer/                  # 转账交易
│   ├── api/                   # API转账
│   ├── auth/                  # 授权转账
│   ├── manage/                # 内部转账
│   ├── ti/                    # TI转账
│   └── verfily/               # 验证相关
├── unfrozen/                  # 解冻交易
└── withDraw/                  # 提现交易
```

### 2.2 上下文（Slot）

**类名**: `TransSlot`  
**继承**: `BaseSlot`

**主要属性**:
```java
// 交易数据对象
private ConsumeTransVo consumeTransVo;           // 消费交易
private RechargeTransVo rechargeTransVo;         // 充值交易
private TransferTransVo transferTransVo;         // 转账交易
private WithDrawTransVo withDrawTransVo;         // 提现交易
private ConsumeTransRefundVo consumeTransRefundVo; // 消费退款
private FrozenTransVo frozenTransVo;            // 冻结交易
private UnFrozenTransVo unFrozenTransVo;        // 解冻交易

// 外部调用结果
private DefaultResult<BasTransTransferRes> transTransferResult;
private DefaultResult<BasTransWithDrawRes> withdrawalResult;
private DefaultResult<TransFrozenRes> frozenResult;
private DefaultResult<TransUnFrozenRes> unFrozenResult;

// 业务数据
private List<String> consumeSubAccountSort;      // 消费子账户排序
```

### 2.3 流程配置

**配置文件**: `liteflow/consume.el.xml`

**典型流程链**:
```xml
<chain name="chainConsume">
    THEN(
        consumeTransPack,              # 数据打包
        accountCheck,                  # 账户校验
        merchantCheck,                 # 商户校验
        companyCheck,                  # 公司校验
        bankInfoCheck,                 # 银行信息校验
        accountBankInfoCheck,          # 账户银行信息校验
        cardBinInfoCheck,              # 卡BIN校验
        bussinessInfoCheck,           # 业务信息校验
        platformInfoCheck,            # 平台信息校验
        consumeTransDeductionSplitPack,# 扣款拆分打包
        baseChainConsumeSubTypeRoute   # 子类型路由
    );
</chain>
```

### 2.4 组件类型

| 组件类型 | 命名规范 | 职责 | 示例 |
|---------|---------|------|------|
| **Pack** | `{业务}TransPack` | 数据打包、参数校验、初始化 | `ConsumeTransPack` |
| **Check** | `{校验项}Check` | 业务规则校验 | `accountCheck`, `merchantCheck` |
| **Trans** | `{业务}Trans{类型}` | 核心交易处理、写库 | `ConsumeTrans01`, `RechargeTrans` |
| **After** | `{业务}TransAfter` | 后处理、账户变动明细 | `ConsumeTransAfter` |
| **Route** | `{业务}Route` | 路由、分支逻辑 | `consumeTransSubTypeRoute` |

### 2.5 业务流程特点

1. **分布式锁**: 使用 Redis 锁防止并发冲突（基于 `cardCode`）
2. **MAC 校验**: 交易前生成 MAC，流程中校验数据完整性
3. **事务性**: 流程内保证事务一致性
4. **写库操作**: 创建/更新交易表、账户变动明细表
5. **外部调用**: 可能调用平台接口（转账、提现）

### 2.6 服务层调用示例

```java
// 1. 加分布式锁
boolean lockPayFlag = redisLockUtils.lockKeyNoWait(cardCode, REDIS_LOCK_MINUTES);

// 2. 生成 MAC
String mac = ConsumeMacUtils.generateMac(request);

// 3. 构建 VO
ConsumeTransVo consumeTransVo = converter.reqToDto(request);
consumeTransVo.setMac(mac);

// 4. 执行 LiteFlow 流程
LiteflowResponse liteflowResponse = 
    flowExecutor.execute2Resp(
        FlowChainEnums.CHAIN_TRANS_CONSUME_FREE.getCode(), 
        consumeTransVo, 
        TransSlot.class
    );

// 5. 获取结果
TransSlot slot = liteflowResponse.getFirstContextBean();
```

---

## 三、业务查询（Query）结构

### 3.1 目录结构

```
flow/component/query/
├── consume/
│   ├── ConsumeQueryPack.java      # 消费查询打包
│   └── ConsumeQuery.java           # 消费查询核心
├── recharge/
│   ├── RechargeQueryPack.java     # 充值查询打包
│   └── RechargeQuery.java         # 充值查询核心
├── transfer/
│   ├── TransferQueryPack.java     # 转账查询打包
│   └── TransferQuery.java         # 转账查询核心
├── withdraw/
│   ├── WithdrawQueryPack.java     # 提现查询打包
│   └── WithdrawQuery.java         # 提现查询核心
└── frozen/
    └── FrozenQueryPack.java       # 冻结查询打包
```

### 3.2 上下文（Slot）

**类名**: `QuerySlot`  
**继承**: `BaseSlot`

**主要属性**:
```java
// 查询请求对象
private ConsumeQueryVo consumeQueryVo;              // 消费查询请求
private RechargeQueryVo rechargeQueryVo;            // 充值查询请求
private TransferQueryVo transferQueryVo;            // 转账查询请求
private WithDrawQueryVo withdrawQueryVo;             // 提现查询请求
private FrozenQueryVo frozenQueryVo;                // 冻结查询请求

// 查询结果对象
private TransConsumeTQueryRes consumeTQueryRes;      // 消费查询结果
private TransTransferTQueryRes transTransferTQueryRes; // 转账查询结果
```

### 3.3 流程配置

**配置文件**: `liteflow/query.el.xml`

**典型流程链**:
```xml
<chain name="chainConsumeResultQuery">
    THEN(
        consumeQueryPack,        # 查询数据打包
        consumeQuery,            # 执行查询
        merchantCheck,          # 商户校验（权限校验）
        accountCheck            # 账户校验（权限校验）
    );
</chain>
```

### 3.4 组件类型

| 组件类型 | 命名规范 | 职责 | 示例 |
|---------|---------|------|------|
| **QueryPack** | `{业务}QueryPack` | 查询参数打包、初始化 | `ConsumeQueryPack` |
| **Query** | `{业务}Query` | 执行查询、填充结果 | `ConsumeQuery` |
| **Check** | `{校验项}Check` | 权限校验（复用） | `merchantCheck`, `accountCheck` |

### 3.5 查询流程特点

1. **无分布式锁**: 查询操作不需要加锁
2. **无 MAC 校验**: 查询不需要 MAC 校验
3. **只读操作**: 不修改数据库，只查询
4. **权限校验**: 校验商户、账户权限
5. **结果填充**: 将查询结果填充到 `QuerySlot`

### 3.6 服务层调用示例

```java
// 1. 构建查询 VO
ConsumeQueryVo consumeQueryVo = Convert.convert(ConsumeQueryVo.class, request);

// 2. 执行 LiteFlow 查询流程
LiteflowResponse liteflowResponse = 
    flowExecutor.execute2Resp(
        FlowChainEnums.CHAIN_QUERY_CONSUME_RESULT.getCode(), 
        consumeQueryVo, 
        QuerySlot.class
    );

// 3. 获取查询结果
QuerySlot slot = liteflowResponse.getFirstContextBean();
TransConsumeTQueryRes consumeTQueryRes = slot.getConsumeTQueryRes();
```

---

## 四、BaseSlot（公共上下文）

### 4.1 继承关系

```
BaseSlot (基础上下文)
├── TransSlot (交易上下文)
└── QuerySlot (查询上下文)
```

### 4.2 BaseSlot 主要属性

```java
// 卡号列表
private List<String> payCardCodes;      // 付款方卡号
private List<String> recCardCodes;      // 收款方卡号

// 基础信息缓存（key: cardCode）
private Map<String, BasCompanyInfoQueryRes> compayInfoMaps;           // 公司信息
private Map<String, List<BasBankInfoQueryRes>> basBankInfoMap;        // 银行信息
private Map<String, BasAccountBankInfoQueryRes> basAccountBankInfoMap; // 账户银行信息
private Map<String, BasCardAccountTQueryRes> cardAccountMap;          // 卡账户信息
private Map<String, List<BasCardSubAccountTQueryRes>> cardSubAccountMap; // 子账户信息
private Map<String, BasBusinessInfoQueryRes> businessInfos;            // 业务信息
private Map<String, BasPlatformSettleInfoQueryRes> basPlatformSettleInoMap; // 平台结算信息
private Map<String, BasActivityTQueryRes> basActivityMap;               // 活动信息
private Map<String, BasCardBinTQueryRes> basCardBindMap;               // 卡BIN信息
private Map<String, BasMerchantTQueryRes> merchantInfos;                // 商户信息

// 交易标识
private String transNo;                 // 交易单号
private String transId;                 // 交易ID
private List<String> transSubNo;        // 子交易单号列表
private List<String> transSubId;        // 子交易ID列表
private String transType;               // 交易类型
private String transSubType;            // 交易子类型
private List<String> acctSubTypes;      // 子账户类型列表

// 冻结/解冻请求
private Map<String, TransAcctFrozenChangeDetailTReq> transFrozenRquests;    // 冻结请求
private Map<String, TransAcctFrozenChangeDetailTReq> transUnFrozenRquests; // 解冻请求
```

### 4.3 BaseSlot 的作用

1. **信息缓存**: 在流程中缓存账户、商户、平台等基础信息，避免重复查询
2. **上下文共享**: 供 `TransSlot` 和 `QuerySlot` 共同使用
3. **校验数据**: 为 Check 组件提供校验所需的基础数据

---

## 五、业务模型对比

### 5.1 业务处理模型（Transaction Model）

```
┌─────────────────────────────────────────┐
│         Controller (API 入口)           │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│         Service (业务服务层)             │
│  - 分布式锁控制                          │
│  - MAC 生成/校验                         │
│  - 幂等性检查                           │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    LiteFlow FlowExecutor                │
│    (执行流程链)                          │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    TransSlot (交易上下文)                │
│  - ConsumeTransVo                       │
│  - RechargeTransVo                      │
│  - TransferTransVo                     │
│  - 外部调用结果                         │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    流程组件 (Component)                  │
│  ├── Pack: 数据打包                     │
│  ├── Check: 业务校验                     │
│  ├── Trans: 核心交易处理                 │
│  └── After: 后处理                      │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    数据库操作 (写操作)                    │
│  - 交易表 (TransConsumeT)               │
│  - 账户变动明细表                        │
│  - 外部接口调用                          │
└─────────────────────────────────────────┘
```

### 5.2 业务查询模型（Query Model）

```
┌─────────────────────────────────────────┐
│         Controller (API 入口)           │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    QueryService (查询服务层)             │
│  - 参数转换                              │
│  - 结果组装                              │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    LiteFlow FlowExecutor                │
│    (执行查询流程链)                       │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    QuerySlot (查询上下文)                │
│  - ConsumeQueryVo                       │
│  - RechargeQueryVo                      │
│  - QueryRes (查询结果)                  │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    查询组件 (Component)                   │
│  ├── QueryPack: 查询参数打包             │
│  ├── Query: 执行查询                     │
│  └── Check: 权限校验                      │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│    数据库操作 (读操作)                    │
│  - 查询交易表                            │
│  - 查询账户信息                          │
│  - 查询明细表                            │
└─────────────────────────────────────────┘
```

---

## 六、关键差异总结

| 维度 | 业务处理（Transaction） | 业务查询（Query） |
|------|------------------------|------------------|
| **上下文** | `TransSlot` | `QuerySlot` |
| **流程配置** | `consume.el.xml` | `query.el.xml` |
| **组件目录** | `flow/component/trans/` | `flow/component/query/` |
| **分布式锁** | ✅ 需要（基于 cardCode） | ❌ 不需要 |
| **MAC 校验** | ✅ 需要 | ❌ 不需要 |
| **数据库操作** | 写操作（INSERT/UPDATE） | 读操作（SELECT） |
| **事务性** | ✅ 需要事务保证 | ❌ 不需要 |
| **外部调用** | ✅ 可能调用平台接口 | ❌ 不调用 |
| **组件类型** | Pack → Check → Trans → After | QueryPack → Query → Check |
| **流程复杂度** | 高（多步骤、分支、后处理） | 低（简单查询流程） |
| **幂等性** | ✅ 需要幂等性保证 | ❌ 不需要 |

---

## 七、设计优势

### 7.1 分离关注点

- **业务处理**: 专注于数据变更、事务一致性、并发控制
- **业务查询**: 专注于数据查询、权限校验、结果组装

### 7.2 统一流程编排

- 两类业务都使用 LiteFlow，保持架构一致性
- 流程可视化、可配置、易维护

### 7.3 上下文隔离

- `TransSlot` 和 `QuerySlot` 分离，避免数据混乱
- 共享 `BaseSlot` 基础信息，避免重复查询

### 7.4 组件复用

- Check 组件可以在两类流程中复用（如 `merchantCheck`, `accountCheck`）
- 减少代码重复

---

## 八、扩展指南

### 8.1 新增业务处理流程

1. 在 `flow/component/trans/{业务}/` 下创建组件
2. 在 `consume.el.xml` 中定义流程链
3. 在 `FlowChainEnums` 中添加枚举
4. 在 Service 中调用流程

### 8.2 新增业务查询流程

1. 在 `flow/component/query/{业务}/` 下创建组件
2. 在 `query.el.xml` 中定义流程链
3. 在 `FlowChainEnums` 中添加枚举
4. 在 QueryService 中调用流程

---

**文档生成时间**: 2026-01-28  
**框架版本**: fund-catering-consume 1.0-SNAPSHOT
