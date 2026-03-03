# 消费流程业务详细设计 - 流程图

本文档将消费流程的业务设计转换为可视化流程图。

---

## 一、消费链路完整流程图（chainConsume）

```mermaid
flowchart TD
    Start([开始]) --> Pack[consumeTransPack<br/>数据打包+参数校验]

    Pack --> AccountCheck[accountCheck<br/>账户校验]
    AccountCheck --> MerchantCheck[merchantCheck<br/>商户校验]
    MerchantCheck --> CompanyCheck[companyCheck<br/>公司校验]
    CompanyCheck --> BankInfoCheck[bankInfoCheck<br/>银行信息校验]
    BankInfoCheck --> AccountBankInfoCheck[accountBankInfoCheck<br/>账户银行信息校验]
    AccountBankInfoCheck --> CardBinInfoCheck[cardBinInfoCheck<br/>卡BIN校验]
    CardBinInfoCheck --> BusinessInfoCheck[businessInfoCheck<br/>业务信息校验]
    BusinessInfoCheck --> PlatformInfoCheck[platformInfoCheck<br/>平台信息校验]

    PlatformInfoCheck --> DeductionPack[consumeTransDeductionSplitPack<br/>扣款拆分打包]

    DeductionPack --> Route[baseChainConsumeSubTypeRoute<br/>子类型路由]

    Route --> |直接消费| DirectChain[bashChainConsume]
    Route --> |需鉴权消费| AuthChain[baseChainConsumeSubTypeU]

    AuthChain --> |密码验证通过| DirectChain
    AuthChain --> |密码验证失败| AuthAfter[consumeTransAuthAfter<br/>鉴权后处理]

    DirectChain --> Cal02[consumeTrans02Cal<br/>膨胀金算价]
    Cal02 --> Cal01[consumeTrans01Cal<br/>现金算价]
    Cal01 --> Frozen[consumeTransFrozen<br/>冻结资金]
    Frozen --> Trans04[consumeTrans04<br/>04账户交易]
    Trans04 --> Trans04Error{04交易<br/>是否成功?}
    Trans04Error --> |失败| Trans04FirstError[consumeTrans04FirstError<br/>04失败处理]
    Trans04FirstError --> Trans04FirstErrorAfter[consumeTrans04FirstErrorAfter<br/>04失败后处理]
    Trans04Error --> |成功| Trans02[consumeTrans02<br/>02账户交易]
    Trans02 --> Trans01[consumeTrans01<br/>01账户交易]
    Trans01 --> TransAfter[consumeTransAfter<br/>账户变动明细]
    TransAfter --> UnFrozen[consumeTransUnFrozen<br/>解冻资金]

    UnFrozen --> Success([成功])
    AuthAfter --> Fail([失败])

    style Pack fill:#e1f5fe
    style Route fill:#fff9c4
    style DirectChain fill:#c8e6c9
    style Trans04 fill:#ffccbc
    style TransAfter fill:#d1c4e9
```

---

## 二、通用链路结构流程图（Pack → Check → Trans → After）

```mermaid
flowchart LR
    subgraph Pack["Pack（打包）"]
        P1[请求入参校验]
        P2[数据转换]
        P3[写入 Slot]
    end

    subgraph Check["Check（校验）"]
        C1[账户校验]
        C2[商户校验]
        C3[公司校验]
        C4[银行信息校验]
        C5[卡BIN校验]
        C6[平台信息校验]
    end

    subgraph Trans["Trans（执行）"]
        T1[路由选择]
        T2[核心交易处理]
        T3[写主表/子表]
        T4[调用外部接口]
    end

    subgraph After["After（后处理）"]
        A1[写账户变动明细]
        A2[更新汇总]
        A3[解冻资金]
    end

    Pack --> Check --> Trans --> After

    style Pack fill:#e3f2fd
    style Check fill:#fff3e0
    style Trans fill:#e8f5e9
    style After fill:#f3e5f5
```

---

## 三、业务处理模型图（Controller → 数据库）

```mermaid
sequenceDiagram
    participant API as Controller (API入口)
    participant SVC as Service (业务服务层)
    participant LF as LiteFlow FlowExecutor
    participant SLOT as TransSlot (交易上下文)
    participant COMP as 流程组件 (Component)
    participant DB as 数据库

    API->>SVC: 接收请求
    SVC->>SVC: 1. 加分布式锁
    SVC->>SVC: 2. 生成MAC值
    SVC->>SVC: 3. 幂等性检查
    SVC->>LF: 4. 执行流程链

    LF->>SLOT: 创建/获取TransSlot

    Note over COMP: Pack 组件
    COMP->>SLOT: 写入业务数据
    SLOT-->>COMP: 返回上下文

    Note over COMP: Check 组件
    COMP->>SLOT: 读取校验数据
    SLOT-->>COMP: 返回基础信息

    Note over COMP: Trans 组件
    COMP->>DB: 写交易表
    DB-->>COMP: 返回结果

    Note over COMP: After 组件
    COMP->>DB: 写账户变动明细
    DB-->>COMP: 返回结果

    LF-->>SVC: 返回TransSlot
    SVC-->>API: 返回处理结果
```

---

## 四、消费退款链路流程图（chainConsumeRefund）

```mermaid
flowchart TD
    Start([开始]) --> Pack[consumeTransRefundPack<br/>退款数据打包]

    Pack --> Checks[各Check组件<br/>账户/商户/公司等校验]

    Checks --> SplitSwitch[consumeTransRefundSplitSwitch<br/>拆分方式路由]

    SplitSwitch --> |按比例拆分| PercentPack[bashChainTransRefundSplitPercentPack<br/>按比例拆分打包]
    SplitSwitch --> |按单拆分| OrderPack[bashChainTransRefundSplitOrderPack<br/>按单拆分打包]

    PercentPack --> ModelSwitch{退款模型<br/>选择}
    OrderPack --> ModelSwitch

    ModelSwitch --> |04模型| Model04[bashChainRefundModel04<br/>04退款模型]
    ModelSwitch --> |01模型| Model01[bashChainRefundModel01<br/>01退款模型]

    Model04 --> RefundRecharge04[RefundRecharge04<br/>04账户退款]
    Model01 --> RefundRecharge01[RefundRecharge01<br/>01账户退款]

    RefundRecharge04 --> After[consumeTransRefundAfter<br/>退款后处理]
    RefundRecharge01 --> After

    After --> Success([完成])

    style Pack fill:#e1f5fe
    style SplitSwitch fill:#fff9c4
    style ModelSwitch fill:#ffe0b2
    style After fill:#d1c4e9
```

---

## 五、转账流程图（chainTransfer）

```mermaid
flowchart TD
    Start([开始]) --> Pack[TransferTransPack<br/>转账数据打包]

    Pack --> Checks[各Check组件<br/>账户/商户/公司等校验]

    Checks --> Trans[TransferTrans<br/>核心转账处理]

    Trans --> Call{调用类型}

    Call --> |内部转账| Inner[chainTransferInner<br/>内部转账]
    Call --> |API转账| API[外部转账接口]

    Inner --> After[TransferTransAfter<br/>转账后处理]
    API --> After

    After --> UpdateBalance[updateSubAccountBalance<br/>更新账户余额]
    UpdateBalance --> Service1[baseAccountServiceApi<br/>批量更新账户余额]
    UpdateBalance --> Service2[transAccountApi<br/>批量创建变动明细]

    Service1 --> Frozen[handleTransFrozens<br/>处理冻结/解冻]
    Service2 --> Frozen

    Frozen --> Success([完成])

    style Pack fill:#e1f5fe
    style Trans fill:#c8e6c9
    style UpdateBalance fill:#fff9c4
    style Frozen fill:#ffccbc
```

---

## 六、充值流程图（chainRecharge）

```mermaid
flowchart TD
    Start([开始]) --> Pack[RechargeTransPack<br/>充值数据打包]

    Pack --> Checks[各Check组件<br/>账户/商户/公司等校验]

    Checks --> Trans[RechargeTrans<br/>核心充值处理]

    Trans --> UpdateAccount[更新账户余额<br/>01/02账户增加]

    UpdateAccount --> After[RechargeTransAfter<br/>充值后处理]

    After --> CreateDetail[创建账户变动明细<br/>4张表明细记录]

    CreateDetail --> Success([完成])

    style Pack fill:#e1f5fe
    style Trans fill:#c8e6c9
    style After fill:#d1c4e9
```

---

## 七、组件职责分类图

```mermaid
graph LR
    subgraph Components["LiteFlow 组件"]
        direction TB

        subgraph Pack["Pack 组件"]
            P1[ConsumeTransPack]
            P2[RechargeTransPack]
            P3[TransferTransPack]
            P4[WithDrawTransPack]
        end

        subgraph Check["Check 组件"]
            C1[accountCheck]
            C2[merchantCheck]
            C3[companyCheck]
            C4[bankInfoCheck]
            C5[cardBinInfoCheck]
            C6[platformInfoCheck]
        end

        subgraph Trans["Trans 组件"]
            T1[ConsumeTrans01/02/04]
            T2[RechargeTrans]
            T3[TransferTrans]
            T4[WithDrawTrans]
        end

        subgraph After["After 组件"]
            A1[ConsumeTransAfter]
            A2[RechargeTransAfter]
            A3[TransferTransAfter]
            A4[WithDrawTransAfter]
        end

        subgraph Route["Route 组件"]
            R1[consumeTransSubTypeRoute]
            R2[consumeTransRefundSplitSwitch]
        end
    end

    Pack --> Check --> Trans --> After
    Trans -.-> Route

    style Pack fill:#e3f2fd
    style Check fill:#fff3e0
    style Trans fill:#e8f5e9
    style After fill:#f3e5f5
    style Route fill:#fce4ec
```

---

## 八、TransSlot 结构图

```mermaid
classDiagram
    class BaseSlot {
        +List~String~ payCardCodes
        +List~String~ recCardCodes
        +Map compayInfoMaps
        +Map basBankInfoMap
        +Map cardSubAccountMap
        +Map businessInfos
        +Map merchantInfos
        +String transNo
        +String transType
        +String transSubType
    }

    class TransSlot {
        +ConsumeTransVo consumeTransVo
        +RechargeTransVo rechargeTransVo
        +TransferTransVo transferTransVo
        +WithDrawTransVo withDrawTransVo
        +ConsumeTransRefundVo consumeTransRefundVo
        +DefaultResult transTransferResult
        +DefaultResult withdrawalResult
        +List~String~ consumeSubAccountSort
    }

    class QuerySlot {
        +ConsumeQueryVo consumeQueryVo
        +RechargeQueryVo rechargeQueryVo
        +TransConsumeTQueryRes consumeTQueryRes
    }

    BaseSlot <|-- TransSlot : 继承
    BaseSlot <|-- QuerySlot : 继承
```

---

## 使用说明

以上流程图使用 **Mermaid** 语法编写，可以在以下工具中查看：

1. **VS Code** - 安装 "Mermaid Preview" 插件
2. **Typora** - 原生支持 Mermaid
3. **GitHub/GitLab** - 直接在 README 中渲染
4. **在线工具** - https://mermaid.live/

---

**文档生成时间**: 2026-02-27
**基于文档**: FRAMEWORK_STRUCTURE.md + 功能与LiteFlow介绍.md
