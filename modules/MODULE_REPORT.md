# fund-catering-report 报表模块文档

## 模块概述

fund-catering-report 是餐饮资金体系的报表服务，主要负责生成各类业务报表、查询报表数据等功能。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-report |
| **主要功能** | 报表生成、数据查询、统计分析 |
| **代码文件数** | 50+ Java 文件 |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-report/
├── fund-catering-report-api/             # API 定义层
│   ├── api/                              # 服务接口定义
│   │   ├── catering/                     # 餐饮相关API
│   │   └── databatch/                    # 数据批处理API
│   ├── request/                          # 请求对象
│   └── response/                         # 响应对象
└── fund-catering-report-service/         # 服务实现层
    ├── controller/                       # 控制器
    │   ├── catering/                     # 餐饮控制器
    │   │   ├── ReportTransQueryController.java      # 交易查询控制器
    │   │   ├── ReportConsumeController.java         # 消费报表控制器
    │   │   ├── ReportRechargeController.java        # 充值报表控制器
    │   │   ├── ReportBasAccountController.java      # 基础账户报表控制器
    │   │   ├── ReportChangeDetailController.java    # 变动明细控制器
    │   │   ├── ReportCardAccountQueryController.java # 卡片账户查询控制器
    │   │   ├── TransConsumePaController.java        # PA消费控制器
    │   │   ├── TransConsumeZxController.java        # ZX消费控制器
    │   │   └── ClearingAgreementController.java     # 清算协议控制器
    │   └── databatch/                    # 数据批处理控制器
    │       ├── DkSelfOwnFundController.java         # DK自有资金控制器
    │       ├── DkTransactionDetailController.java   # DK交易明细控制器
    │       ├── FzLedgerDetailApiController.java    # FZ分户账明细控制器
    │       ├── GdAccountSummarySettleApiController.java # GD账户结算汇总控制器
    │       ├── GdDetailRecordController.java        # GD明细记录控制器
    │       ├── GdModifyDetailController.java        # GD修改明细控制器
    │       ├── GdSettleCleanSummaryController.java  # GD结算清算汇总控制器
    │       ├── GdShopReceiveSummaryController.java  # GD商店收款汇总控制器
    │       └── GdStoreReceiveDetailController.java  # GD商店收款明细控制器
    ├── domain/                           # 数据对象
    ├── mapper/                           # MyBatis映射
    ├── service/                          # 服务接口
    │   ├── catering/                     # 餐饮服务
    │   │   ├── TransConsumeService.java           # 消费交易服务
    │   │   ├── TransRechargeService.java          # 充值交易服务
    │   │   ├── TransTransferService.java          # 转账服务
    │   │   ├── AbnormalCardBalanceLogTService.java # 异常卡余额服务
    │   │   ├── AbnormalTransLogTService.java       # 异常交易日志服务
    │   │   ├── DaySumSubAccountTService.java       # 日终子账户汇总服务
    │   │   ├── BasSubAccountExpendTService.java    # 子账户支出服务
    │   │   ├── ConsumeReportService.java          # 消费报表服务
    │   │   ├── RechargeReportService.java         # 充值报表服务
    │   │   ├── ChangeDetailReportService.java     # 变动明细报表服务
    │   │   └── ...
    │   ├── databatch/                    # 数据批处理服务
    │   └── impl/                         # 服务实现
    └── ReportServiceApplication.java     # 启动类
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **账户报表** | 账户余额、变动明细报表 |
| **交易报表** | 消费、充值、转账、提现交易报表 |
| **商户报表** | 商户结算、收款、费率报表 |
| **对账报表** | 银行对账、清算对账报表 |
| **异常报表** | 异常交易、异常余额报表 |
| **日终报表** | 日终汇总、日切报表 |
| **平台报表** | PA/ZX平台交易报表 |
| **数据批处理报表** | DK/GD/FZ等批处理相关报表 |

---

## 二、餐饮报表 Controller

### 2.1 ReportTransQueryController（交易查询控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 交易流水查询 | `/report/trans/query` | 查询交易流水 |
| 交易汇总查询 | `/report/trans/summary` | 查询交易汇总 |
| 交易统计查询 | `/report/trans/statistics` | 查询交易统计 |

### 2.2 ReportConsumeController（消费报表控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 消费明细查询 | `/report/consume/detail` | 查询消费明细 |
| 消费汇总查询 | `/report/consume/summary` | 查询消费汇总 |
| 消费日报查询 | `/report/consume/daily` | 查询消费日报 |
| 消费月报查询 | `/report/consume/monthly` | 查询消费月报 |

### 2.3 ReportRechargeController（充值报表控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 充值明细查询 | `/report/recharge/detail` | 查询充值明细 |
| 充值汇总查询 | `/report/recharge/summary` | 查询充值汇总 |
| 充值日报查询 | `/report/recharge/daily` | 查询充值日报 |
| 充值统计查询 | `/report/recharge/statistics` | 查询充值统计 |

### 2.4 ReportBasAccountController（基础账户报表控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 账户余额查询 | `/report/account/balance` | 查询账户余额 |
| 账户明细查询 | `/report/account/detail` | 查询账户明细 |
| 账户汇总查询 | `/report/account/summary` | 查询账户汇总 |
| 账户变动查询 | `/report/account/change` | 查询账户变动 |

### 2.5 ReportChangeDetailController（变动明细控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 变动明细查询 | `/report/change/detail` | 查询变动明细 |
| 变动汇总查询 | `/report/change/summary` | 查询变动汇总 |
| 变动趋势查询 | `/report/change/trend` | 查询变动趋势 |

### 2.6 ReportCardAccountQueryController（卡片账户查询控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 卡片余额查询 | `/report/card/balance` | 查询卡片余额 |
| 卡片明细查询 | `/report/card/detail` | 查询卡片明细 |
| 卡片交易查询 | `/report/card/trans` | 查询卡片交易 |

### 2.7 TransConsumePaController（PA消费控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| PA消费查询 | `/report/pa/consume` | 查询PA消费 |
| PA对账查询 | `/report/pa/reconcile` | 查询PA对账 |
| PA统计查询 | `/report/pa/statistics` | 查询PA统计 |

### 2.8 TransConsumeZxController（ZX消费控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| ZX消费查询 | `/report/zx/consume` | 查询ZX消费 |
| ZX对账查询 | `/report/zx/reconcile` | 查询ZX对账 |
| ZX统计查询 | `/report/zx/statistics` | 查询ZX统计 |

### 2.9 ClearingAgreementController（清算协议控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 清算协议查询 | `/report/clearing/agreement` | 查询清算协议 |
| 清算明细查询 | `/report/clearing/detail` | 查询清算明细 |
| 清算汇总查询 | `/report/clearing/summary` | 查询清算汇总 |

---

## 三、数据批处理报表 Controller

### 3.1 DkSelfOwnFundController（DK自有资金控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 自有资金查询 | `/report/dk/fund` | 查询自有资金 |
| 自有资金明细 | `/report/dk/fund/detail` | 查询资金明细 |
| 自有资金汇总 | `/report/dk/fund/summary` | 查询资金汇总 |

### 3.2 DkTransactionDetailController（DK交易明细控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 代扣明细查询 | `/report/dk/detail` | 查询代扣明细 |
| 代扣汇总查询 | `/report/dk/summary` | 查询代扣汇总 |
| 代扣统计查询 | `/report/dk/statistics` | 查询代扣统计 |

### 3.3 FzLedgerDetailApiController（FZ分户账明细控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 分户账明细查询 | `/report/fz/ledger/detail` | 查询分户账明细 |
| 分户账汇总查询 | `/report/fz/ledger/summary` | 查询分户账汇总 |
| 分户账日报查询 | `/report/fz/ledger/daily` | 查询分户账日报 |

### 3.4 GdAccountSummarySettleApiController（GD账户结算汇总控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 结算汇总查询 | `/report/gd/settle/summary` | 查询结算汇总 |
| 结算明细查询 | `/report/gd/settle/detail` | 查询结算明细 |
| 结算日报查询 | `/report/gd/settle/daily` | 查询结算日报 |

### 3.5 GdDetailRecordController（GD明细记录控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 明细记录查询 | `/report/gd/detail` | 查询明细记录 |
| 明细汇总查询 | `/report/gd/detail/summary` | 查询明细汇总 |

### 3.6 GdModifyDetailController（GD修改明细控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 修改明细查询 | `/report/gd/modify/detail` | 查询修改明细 |
| 修改汇总查询 | `/report/gd/modify/summary` | 查询修改汇总 |

### 3.7 GdSettleCleanSummaryController（GD结算清算汇总控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 清算汇总查询 | `/report/gd/clean/summary` | 查询清算汇总 |
| 清算明细查询 | `/report/gd/clean/detail` | 查询清算明细 |
| 清算日报查询 | `/report/gd/clean/daily` | 查询清算日报 |

### 3.8 GdShopReceiveSummaryController（GD商店收款汇总控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 收款汇总查询 | `/report/gd/shop/receive/summary` | 查询收款汇总 |
| 收款明细查询 | `/report/gd/shop/receive/detail` | 查询收款明细 |
| 收款日报查询 | `/report/gd/shop/receive/daily` | 查询收款日报 |

### 3.9 GdStoreReceiveDetailController（GD商店收款明细控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 收款明细查询 | `/report/gd/store/receive/detail` | 查询收款明细 |
| 收款汇总查询 | `/report/gd/store/receive/summary` | 查询收款汇总 |
| 收款趋势查询 | `/report/gd/store/receive/trend` | 查询收款趋势 |

---

## 四、核心服务

### 4.1 消费报表服务

```java
public interface ConsumeReportService {
    // 消费明细查询
    List<ConsumeDetail> queryConsumeDetail(ConsumeQueryRequest request);

    // 消费汇总查询
    ConsumeSummary queryConsumeSummary(ConsumeSummaryRequest request);

    // 消费日报查询
    ConsumeDaily queryConsumeDaily(String date);

    // 消费月报查询
    ConsumeMonthly queryConsumeMonthly(String year, String month);
}
```

### 4.2 充值报表服务

```java
public interface RechargeReportService {
    // 充值明细查询
    List<RechargeDetail> queryRechargeDetail(RechargeQueryRequest request);

    // 充值汇总查询
    RechargeSummary queryRechargeSummary(RechargeSummaryRequest request);

    // 充值统计查询
    RechargeStatistics queryRechargeStatistics(RechargeStatisticsRequest request);
}
```

### 4.3 变动明细报表服务

```java
public interface ChangeDetailReportService {
    // 变动明细查询
    List<ChangeDetail> queryChangeDetail(ChangeDetailRequest request);

    // 变动汇总查询
    ChangeSummary queryChangeSummary(ChangeSummaryRequest request);

    // 变动趋势查询
    List<ChangeTrend> queryChangeTrend(ChangeTrendRequest request);
}
```

### 4.4 交易服务

```java
public interface TransConsumeService {
    // 消费交易查询
    List<TransConsume> queryTransConsume(TransConsumeQueryRequest request);
}

public interface TransRechargeService {
    // 充值交易查询
    List<TransRecharge> queryTransRecharge(TransRechargeQueryRequest request);
}

public interface TransTransferService {
    // 转账交易查询
    List<TransTransfer> queryTransTransfer(TransTransferQueryRequest request);
}
```

### 4.5 异常处理服务

```java
public interface AbnormalCardBalanceLogTService {
    // 异常卡余额查询
    List<AbnormalCardBalanceLog> queryAbnormalBalance(AbnormalQueryRequest request);

    // 异常卡余额处理
    void handleAbnormalBalance(String logId);
}

public interface AbnormalTransLogTService {
    // 异常交易查询
    List<AbnormalTransLog> queryAbnormalTrans(AbnormalQueryRequest request);

    // 异常交易处理
    void handleAbnormalTrans(String logId);
}
```

### 4.6 日终汇总服务

```java
public interface DaySumSubAccountTService {
    // 日终汇总查询
    List<DaySumSubAccount> queryDaySum(DaySumQueryRequest request);

    // 日终汇总生成
    void generateDaySum(String sumDate);

    // 日终汇总重跑
    void reRunDaySum(String sumDate);
}
```

### 4.7 子账户支出服务

```java
public interface BasSubAccountExpendTService {
    // 子账户支出查询
    List<BasSubAccountExpend> querySubAccountExpend(SubAccountExpendRequest request);

    // 膨胀金使用明细
    List<ExpendDetail> queryExpendDetail(ExpendDetailRequest request);
}
```

---

## 五、报表类型

### 5.1 账户报表

| 报表名称 | 说明 | 频率 |
|---------|------|------|
| 账户余额日报 | 每日账户余额汇总 | 每日 |
| 账户变动明细 | 账户变动明细记录 | 实时 |
| 账户汇总表 | 账户资金汇总 | 每日 |
| 子账户余额表 | 各子账户余额 | 每日 |

### 5.2 交易报表

| 报表名称 | 说明 | 频率 |
|---------|------|------|
| 消费交易日报 | 每日消费交易汇总 | 每日 |
| 充值交易日报 | 每日充值交易汇总 | 每日 |
| 转账交易日报 | 每日转账交易汇总 | 每日 |
| 提现交易日报 | 每日提现交易汇总 | 每日 |

### 5.3 商户报表

| 报表名称 | 说明 | 频率 |
|---------|------|------|
| 商户结算表 | 商户结算汇总 | 每月 |
| 商户收款表 | 商户收款明细 | 每日 |
| 商户费率表 | 商户费率统计 | 每月 |
| 商户对账表 | 商户对账明细 | 每日 |

### 5.4 对账报表

| 报表名称 | 说明 | 频率 |
|---------|------|------|
| 银行对账表 | 银行交易对账 | 每日 |
| PA平台对账表 | PA平台对账 | 每日 |
| ZX平台对账表 | ZX平台对账 | 每日 |
| 清算对账表 | 清算对账明细 | 每日 |

### 5.5 异常报表

| 报表名称 | 说明 | 频率 |
|---------|------|------|
| 异常交易表 | 异常交易记录 | 实时 |
| 异常余额表 | 异常余额记录 | 实时 |
| 异常汇总表 | 异常汇总统计 | 每日 |

---

## 六、技术特点

1. **多维度查询**: 支持按时间、商户、账户等多维度查询
2. **数据汇总**: 支持日、周、月、年等多种时间维度的数据汇总
3. **统计分析**: 提供丰富的统计分析功能
4. **数据导出**: 支持Excel、CSV等多种格式导出
5. **缓存机制**: 采用缓存提高查询效率
6. **异步处理**: 大数据量查询采用异步处理

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
