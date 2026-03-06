# fund-catering-data-batch 数据批处理模块文档

## 模块概述

fund-catering-data-batch 是餐饮资金体系的数据批处理服务，主要负责数据导入、数据核对、批量结算、文件处理等功能。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-data-batch |
| **主要功能** | 数据批处理、批量结算、数据导入导出 |
| **代码文件数** | 100+ Java 文件 |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-data-batch/
├── fund-catering-data-batch-api/        # API 定义层
│   ├── api/                              # 服务接口定义
│   ├── bo/                               # 业务对象
│   ├── enums/                            # 枚举定义
│   ├── request/                          # 请求对象
│   └── response/                         # 响应对象
├── fund-catering-data-batch-common/      # 公共模块
│   ├── constant/                         # 常量定义
│   ├── enums/                            # 枚举定义
│   └── utils/                            # 工具类
└── fund-catering-data-batch-service/     # 服务实现层
    ├── controller/                       # 控制器
    │   ├── DkController.java                    # 代扣控制器
    │   ├── ManualAccountFacadeController.java   # 手动账户控制器
    │   ├── SelfOwnFundController.java           # 自有资金控制器
    │   ├── FzLedgerController.java              # 分账账本控制器
    │   ├── SettleCleanController.java           # 结算清算控制器
    │   ├── ShopFeeController.java               # 商户费率控制器
    │   ├── SummaryAccountDataController.java    # 汇总账户数据控制器
    │   ├── TaskMonitorPlanController.java       # 任务监控控制器
    │   ├── WriteOffController.java              # 核销控制器
    │   └── ...
    ├── domain/                           # 数据对象
    ├── job/                              # 定时任务
    ├── mapper/                           # MyBatis映射
    ├── process/                          # 处理流程
    ├── service/                          # 服务接口
    │   ├── DkImportRecordService.java            # 代扣导入记录服务
    │   ├── DkTransactionDetailService.java       # 代扣交易明细服务
    │   ├── ManualAccountService.java             # 手动账户服务
    │   ├── GdAccountSummarySettleService.java    # 账户结算汇总服务
    │   ├── MpStoreContractService.java           # 商户合约服务
    │   ├── GdDetailRecordService.java            # 明细记录服务
    │   ├── GdTaskMonitorService.java             # 任务监控服务
    │   ├── ISummaryAccountDataReRunService.java  # 汇总数据重跑服务
    │   ├── IWriteOffService.java                 # 核销服务
    │   ├── impl/                                  # 服务实现
    │   │   ├── DkImportRecordServiceImpl.java
    │   │   ├── DkTransactionDetailServiceImpl.java
    │   │   ├── GdAccountSummarySettleServiceImpl.java
    │   │   ├── GdDetailRecordServiceImpl.java
    │   │   ├── GdTaskMonitorServiceImpl.java
    │   │   ├── SummaryAccountDataDataReRunServiceImpl.java
    │   │   ├── WriteOffService.java
    │   │   ├── DkImportServiceImpl.java
    │   │   ├── GdTransferFileRecordServiceImpl.java
    │   │   ├── GdSettleCleanDetailServiceImpl.java
    │   │   ├── GdShopReceiveDetailServiceImpl.java
    │   │   ├── GdShopReceiveSummaryServiceImpl.java
    │   │   ├── HuaFuAutoCommitServiceImpl.java
    │   │   ├── MpStoreContractServiceImpl.java
    │   │   ├── MpStoreContractFeeServiceImpl.java
    │   │   ├── MpStoreContractPaymentServiceImpl.java
    │   │   ├── MpSettleAgencyServiceImpl.java
    │   │   ├── MpSettleAgencyBillServiceImpl.java
    │   │   ├── NpkMchntServiceImpl.java
    │   │   ├── PostAccountServiceImpl.java
    │   │   ├── SelfOwnFundServiceImpl.java
    │   │   ├── SysSettlementagenciesServiceImpl.java
    │   │   └── ...
    │   └── ...
    └── DataBatchServiceApplication.java  # 启动类
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **代扣处理** | 代扣交易明细同步、代扣划付处理 |
| **商户管理** | 商户信息、配置、合约管理 |
| **结算处理** | 账户结算、清算、对账 |
| **数据导入** | 各类批量数据导入功能 |
| **手动账务** | 手动账户数据处理、调账 |
| **汇总数据** | 汇总数据生成和查询、数据重跑 |
| **任务监控** | 批量任务执行监控 |
| **转账交易** | 转账明细处理 |
| **核销处理** | 单据核销功能 |
| **商户费率** | 商户费率计算和管理 |

---

## 二、核心 Controller

### 2.1 DkController（代扣控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 代扣导入 | `/dk/import` | 导入代扣数据 |
| 代扣明细查询 | `/dk/detail/query` | 查询代扣明细 |
| 代扣划付 | `/dk/payment` | 代扣划付处理 |
| 代扣状态查询 | `/dk/status/query` | 查询代扣状态 |

### 2.2 ManualAccountFacadeController（手动账户控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 手动记账 | `/manual/account/create` | 创建手动记账 |
| 手动记账查询 | `/manual/account/query` | 查询手动记账 |
| 手动记账审核 | `/manual/account/audit` | 审核手动记账 |
| 调账处理 | `/manual/account/adjust` | 调账处理 |

### 2.3 SelfOwnFundController（自有资金控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 自有资金查询 | `/self/fund/query` | 查询自有资金 |
| 自有资金划拨 | `/self/fund/transfer` | 自有资金划拨 |
| 自有资金明细 | `/self/fund/detail` | 自有资金明细 |

### 2.4 FzLedgerController（分账账本控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 分账账本查询 | `/fz/ledger/query` | 查询分账账本 |
| 分账账本生成 | `/fz/ledger/generate` | 生成分账账本 |
| 分账账本明细 | `/fz/ledger/detail` | 分账账本明细 |

### 2.5 SettleCleanController（结算清算控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 结算查询 | `/settle/clean/query` | 查询结算清算 |
| 结算生成 | `/settle/clean/generate` | 生成结算数据 |
| 结算审核 | `/settle/clean/audit` | 审核结算数据 |

### 2.6 ShopFeeController（商户费率控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 费率配置 | `/shop/fee/config` | 配置商户费率 |
| 费率查询 | `/shop/fee/query` | 查询商户费率 |
| 费率计算 | `/shop/fee/calculate` | 计算商户费率 |

### 2.7 SummaryAccountDataController（汇总账户数据控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 汇总数据查询 | `/summary/data/query` | 查询汇总数据 |
| 汇总数据生成 | `/summary/data/generate` | 生成汇总数据 |
| 汇总数据重跑 | `/summary/data/rerun` | 重跑汇总数据 |

### 2.8 TaskMonitorPlanController（任务监控控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 任务列表 | `/task/monitor/list` | 查询任务列表 |
| 任务详情 | `/task/monitor/detail` | 查询任务详情 |
| 任务执行 | `/task/monitor/execute` | 执行任务 |

### 2.9 WriteOffController（核销控制器）

| 功能 | 接口 | 说明 |
|------|------|------|
| 核销处理 | `/writeoff/process` | 处理核销 |
| 核销查询 | `/writeoff/query` | 查询核销记录 |
| 核销撤销 | `/writeoff/cancel` | 撤销核销 |

---

## 三、核心服务

### 3.1 代扣服务

#### DkImportRecordService（代扣导入记录服务）

```java
public interface DkImportRecordService {
    // 创建导入记录
    void createImportRecord(DkImportRecord record);

    // 查询导入记录
    DkImportRecord queryImportRecord(String importId);

    // 更新导入状态
    void updateImportStatus(String importId, String status);
}
```

#### DkTransactionDetailService（代扣交易明细服务）

```java
public interface DkTransactionDetailService {
    // 批量插入代扣明细
    void batchInsert(List<DkTransactionDetail> details);

    // 查询代扣明细
    List<DkTransactionDetail> queryDetails(DkQueryRequest request);

    // 代扣划付
    void paymentProcess(String batchNo);
}
```

### 3.2 手动账户服务

#### ManualAccountService（手动账户服务）

```java
public interface ManualAccountService {
    // 创建手动记账
    void createManualAccount(ManualAccountRequest request);

    // 审核手动记账
    void auditManualAccount(String accountId, String auditResult);

    // 调账处理
    void adjustAccount(AdjustAccountRequest request);
}
```

### 3.3 结算服务

#### GdAccountSummarySettleService（账户结算汇总服务）

```java
public interface GdAccountSummarySettleService {
    // 生成结算汇总
    void generateSettleSum(SettleSumRequest request);

    // 查询结算汇总
    List<GdAccountSummarySettle> querySettleSum(SettleQueryRequest request);
}
```

### 3.4 商户合约服务

#### MpStoreContractService（商户合约服务）

```java
public interface MpStoreContractService {
    // 添加商户合约
    void addStoreContract(MpStoreContract contract);

    // 编辑商户合约
    void editStoreContract(MpStoreContract contract);

    // 查询商户合约
    MpStoreContract queryStoreContract(String contractId);

    // 合同列表
    List<MpStoreContract> storeContractList(StoreContractQueryRequest request);
}
```

### 3.5 明细记录服务

#### GdDetailRecordService（明细记录服务）

```java
public interface GdDetailRecordService {
    // 插入明细记录
    void insertDetailRecord(GdDetailRecord record);

    // 批量插入明细记录
    void batchInsertDetailRecord(List<GdDetailRecord> records);

    // 查询明细记录
    List<GdDetailRecord> queryDetailRecords(DetailQueryRequest request);
}
```

### 3.6 任务监控服务

#### GdTaskMonitorService（任务监控服务）

```java
public interface GdTaskMonitorService {
    // 创建任务监控
    void createTaskMonitor(GdTaskMonitor monitor);

    // 更新任务状态
    void updateTaskStatus(String taskId, String status);

    // 查询任务监控
    List<GdTaskMonitor> queryTaskMonitors(TaskQueryRequest request);
}
```

### 3.7 汇总数据重跑服务

#### ISummaryAccountDataReRunService（汇总数据重跑服务）

```java
public interface ISummaryAccountDataReRunService {
    // 重跑汇总数据
    void reRunSummaryData(ReRunRequest request);

    // 查询重跑记录
    List<ReRunRecord> queryReRunRecords(String batchId);
}
```

### 3.8 核销服务

#### IWriteOffService（核销服务）

```java
public interface IWriteOffService {
    // 核销处理
    void writeOff(WriteOffRequest request);

    // 核销撤销
    void cancelWriteOff(String writeOffId);

    // 查询核销记录
    List<WriteOffRecord> queryWriteOffRecords(WriteOffQueryRequest request);
}
```

---

## 四、核心数据模型

### 4.1 代扣相关

#### DkImportRecord（代扣导入记录）
```java
- importId: 导入ID
- batchNo: 批次号
- fileName: 文件名
- importTime: 导入时间
- status: 状态
- recordCount: 记录数
- successCount: 成功数
- failCount: 失败数
```

#### DkTransactionDetail（代扣交易明细）
```java
- detailId: 明细ID
- batchNo: 批次号
- cardCode: 卡号
- amount: 金额
- status: 状态
- processTime: 处理时间
```

### 4.2 结算相关

#### GdAccountSummarySettle（账户结算汇总）
```java
- settleId: 结算ID
- accountNo: 账户号
- settleDate: 结算日期
- settleAmount: 结算金额
- feeAmount: 手续费金额
- actualAmount: 实际金额
```

#### GdDetailRecord（明细记录）
```java
- recordId: 记录ID
- settleDate: 结算日期
- cardCode: 卡号
- transType: 交易类型
- transAmount: 交易金额
- feeAmount: 手续费金额
```

### 4.3 任务监控相关

#### GdTaskMonitor（任务监控）
```java
- taskId: 任务ID
- taskName: 任务名称
- taskType: 任务类型
- status: 状态
- startTime: 开始时间
- endTime: 结束时间
- executeResult: 执行结果
```

### 4.4 商户合约相关

#### MpStoreContract（商户合约）
```java
- contractId: 合约ID
- storeId: 门店ID
- contractNo: 合同编号
- contractName: 合同名称
- startDate: 开始日期
- endDate: 结束日期
- status: 状态
```

---

## 五、批处理流程

### 5.1 代扣处理流程

```
1. 文件上传
   ↓
2. 数据解析
   ↓
3. 数据校验
   ↓
4. 批量插入
   ↓
5. 状态更新
   ↓
6. 划付处理
   ↓
7. 结果通知
```

### 5.2 结算处理流程

```
1. 获取待结算数据
   ↓
2. 数据汇总
   ↓
3. 费率计算
   ↓
4. 生成结算单
   ↓
5. 审核确认
   ↓
6. 账务处理
   ↓
7. 结果通知
```

### 5.3 数据重跑流程

```
1. 确认重跑范围
   ↓
2. 清除原有数据
   ↓
3. 重新计算汇总
   ↓
4. 生成新数据
   ↓
5. 数据校验
   ↓
6. 完成重跑
```

---

## 六、技术特点

1. **批量处理**: 支持大批量数据的导入和处理
2. **异步处理**: 采用异步方式处理耗时操作
3. **事务管理**: 保证数据一致性
4. **错误处理**: 完善的错误处理和重试机制
5. **进度监控**: 实时监控批处理进度
6. **数据校验**: 严格的数据校验机制

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
