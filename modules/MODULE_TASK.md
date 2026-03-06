# fund-catering-task 任务调度模块文档

## 模块概述

fund-catering-task 是餐饮资金体系的任务调度服务，基于 XXL-Job 实现分布式任务调度，负责各种定时任务和后台处理任务的执行。

| 属性 | 值 |
|------|------|
| **模块名称** | fund-catering-task |
| **主要功能** | 定时任务调度、后台处理、数据核对 |
| **代码文件数** | 217 Java 文件 |
| **调度框架** | XXL-Job |
| **服务端口** | - |

---

## 一、模块架构

### 1.1 目录结构

```
fund-catering-task/
├── config/                    # 配置类
│   └── ScheduledConfig.java   # 任务调度线程池配置
├── constant/                  # 常量定义
│   ├── ZxTransTypeEnum.java   # ZX交易类型枚举
│   └── TaskConstants.java     # 任务常量
├── domain/                    # 数据传输对象
│   ├── ActivityBalanceDto.java    # 活动余额DTO
│   ├── BankBalanceDto.java       # 银行余额DTO
│   ├── CardBalanceDto.java       # 卡片余额DTO
│   └── ...
├── handle/                    # 处理器
│   ├── BasAccountCheckHandle.java      # 账户检查处理器接口
│   ├── AbstractBasAccountCheckHandle.java  # 账户检查抽象处理器
│   └── impl/
│       ├── PaAccountCheckHandle.java     # PA账户检查处理器
│       └── ZxAccountCheckHandle.java     # ZX账户检查处理器
├── job/                       # 定时任务
│   ├── AccountCheckingJobService.java     # 账户核对任务
│   ├── CardBalanceJobService.java         # 卡片余额查询任务
│   ├── AutoWithdrawJobService.java        # 自动提现任务
│   ├── BillJobService.java                # 账单生成任务
│   ├── FileDownloadJobService.java        # 文件下载任务
│   ├── FileDeleteJobService.java          # 文件删除任务
│   ├── DaySumAmtJobService.java           # 日终汇总任务
│   ├── TransRecallJobService.java         # 交易冲正任务
│   ├── TransferRecallJobService.java      # 转账冲正任务
│   ├── TransferAtJobService.java          # 转账处理任务
│   ├── UpdateLockAccountDailyJobService.java  # 更新锁定账户任务
│   ├── WithDrawBatchJobService.java       # 批量提现任务
│   ├── pa/                    # PA平台相关任务
│   │   ├── PaAccountCheckingJobService.java    # PA账户核对任务
│   │   ├── PaFileDownloadJobService.java       # PA文件下载任务
│   │   ├── PaTransNoticeJobService.java        # PA交易通知任务
│   │   └── ...
│   └── zx/                    # ZX平台相关任务
│       ├── ZxAccountCheckingJobService.java    # ZX账户核对任务
│       ├── ZxFileDownloadJobService.java       # ZX文件下载任务
│       ├── ZxTransNoticeJobService.java        # ZX交易通知任务
│       └── ...
├── service/                   # 服务接口
│   ├── AccountCheckService.java        # 账户核对服务
│   ├── AccountEntityService.java       # 账户实体服务
│   ├── AutoWithdrawService.java        # 自动提现服务
│   ├── CardBalanceService.java         # 卡片余额服务
│   ├── ContractAndBillService.java     # 合同账单服务
│   ├── DaySumAmtService.java           # 日终汇总服务
│   ├── TransRecallService.java         # 交易冲正服务
│   ├── TransferAtService.java          # 转账服务
│   ├── TransferRecallService.java      # 转账冲正服务
│   ├── UpdateLockAccountDailyService.java  # 更新锁定账户服务
│   ├── WithDrawBatchService.java       # 批量提现服务
│   ├── impl/                           # 服务实现
│   ├── pa/                            # PA平台服务
│   │   └── impl/
│   └── zx/                            # ZX平台服务
│       └── impl/
└── utils/                     # 工具类
    ├── FtpUtil.java              # FTP工具类
    ├── SftpUtils.java            # SFTP工具类
    ├── StringUtils.java          # 字符串工具类
    └── RedisLockUtils.java       # Redis分布式锁工具类
```

### 1.2 核心功能

| 功能 | 说明 |
|------|------|
| **账户核对** | 定期核对账户余额，确保数据一致性 |
| **卡片余额查询** | 定期查询卡片账户余额，更新系统数据 |
| **自动提现** | 自动处理提现请求，调用平台接口 |
| **账单生成** | 定时生成各类账单和结算单据 |
| **文件管理** | 定时下载对账文件，删除过期文件 |
| **交易通知** | 处理平台交易通知，更新交易状态 |
| **调账处理** | 处理交易调账和冲正 |
| **日终汇总** | 每日金额汇总计算，生成日终报表 |
| **合约管理** | 合约状态更新和管理 |
| **平台充值** | 处理平台充值任务 |

---

## 二、定时任务详解

### 2.1 账户核对任务

#### AccountCheckingJobService

```java
@Component
public class AccountCheckingJobService {

    @XxlJob("accountCheckingJob")
    public void accountChecking() {
        // 账户核对任务
        // 1. 查询需要核对的账户
        // 2. 调用平台查询余额
        // 3. 比对系统余额
        // 4. 记录差异
        // 5. 发送告警
    }
}
```

**执行频率**: 每小时一次
**处理逻辑**:
1. 获取需要核对的账户列表
2. 调用 PA/ZX 平台查询接口获取余额
3. 与系统内余额进行比对
4. 发现差异时记录异常日志
5. 发送告警通知

### 2.2 卡片余额查询任务

#### CardBalanceJobService

```java
@Component
public class CardBalanceJobService {

    @XxlJob("cardBalanceJob")
    public void queryCardBalance() {
        // 卡片余额查询任务
        // 1. 查询活跃卡片列表
        // 2. 批量查询卡片余额
        // 3. 更新系统余额
    }
}
```

**执行频率**: 每 30 分钟一次
**处理逻辑**:
1. 查询状态正常的卡片
2. 批量调用平台余额查询接口
3. 更新系统内卡片余额

### 2.3 自动提现任务

#### AutoWithdrawJobService

```java
@Component
public class AutoWithdrawJobService {

    @XxlJob("autoWithdrawJob")
    public void autoWithdraw() {
        // 自动提现任务
        // 1. 查询待处理提现请求
        // 2. 调用平台提现接口
        // 3. 更新提现状态
        // 4. 处理提现结果
    }
}
```

**执行频率**: 每 10 分钟一次
**处理逻辑**:
1. 查询状态为"待处理"的提现请求
2. 调用 PA/ZX 平台提现接口
3. 更新提现请求状态
4. 处理提现结果通知

### 2.4 日终汇总任务

#### DaySumAmtJobService

```java
@Component
public class DaySumAmtJobService {

    @XxlJob("daySumAmtJob")
    public void daySumAmt() {
        // 日终汇总任务
        // 1. 汇总当日交易金额
        // 2. 汇总账户余额
        // 3. 生成日终报表
        // 4. 更新汇总表
    }
}
```

**执行频率**: 每天凌晨 1 点
**处理逻辑**:
1. 汇总当日所有交易金额
2. 汇总各账户余额
3. 生成日终报表数据
4. 更新日终汇总表

### 2.5 文件下载任务

#### FileDownloadJobService

```java
@Component
public class FileDownloadJobService {

    @XxlJob("fileDownloadJob")
    public void downloadFiles() {
        // 文件下载任务
        // 1. 查询待下载文件列表
        // 2. 下载对账文件
        // 3. 解析文件内容
        // 4. 入库处理
    }
}
```

**执行频率**: 每天凌晨 2 点
**处理逻辑**:
1. 查询前一天的对账文件
2. 通过 SFTP/FTP 下载文件
3. 解析对账文件内容
4. 入库处理对账数据

### 2.6 交易冲正任务

#### TransRecallJobService

```java
@Component
public class TransRecallJobService {

    @XxlJob("transRecallJob")
    public void transRecall() {
        // 交易冲正任务
        // 1. 查询待冲正交易
        // 2. 执行冲正操作
        // 3. 更新交易状态
    }
}
```

**执行频率**: 每小时一次
**处理逻辑**:
1. 查询状态为"待冲正"的交易
2. 执行账户冲正操作
3. 更新交易状态为"已冲正"

### 2.7 PA 平台任务

#### PaAccountCheckingJobService

```java
@Component
public class PaAccountCheckingJobService {

    @XxlJob("paAccountCheckingJob")
    public void paAccountChecking() {
        // PA平台账户核对任务
    }
}
```

#### PaFileDownloadJobService

```java
@Component
public class PaFileDownloadJobService {

    @XxlJob("paFileDownloadJob")
    public void paFileDownload() {
        // PA平台文件下载任务
    }
}
```

#### PaTransNoticeJobService

```java
@Component
public class PaTransNoticeJobService {

    @XxlJob("paTransNoticeJob")
    public void paTransNotice() {
        // PA平台交易通知处理任务
    }
}
```

### 2.8 ZX 平台任务

#### ZxAccountCheckingJobService

```java
@Component
public class ZxAccountCheckingJobService {

    @XxlJob("zxAccountCheckingJob")
    public void zxAccountChecking() {
        // ZX平台账户核对任务
    }
}
```

#### ZxFileDownloadJobService

```java
@Component
public class ZxFileDownloadJobService {

    @XxlJob("zxFileDownloadJob")
    public void zxFileDownload() {
        // ZX平台文件下载任务
    }
}
```

#### ZxTransNoticeJobService

```java
@Component
public class ZxTransNoticeJobService {

    @XxlJob("zxTransNoticeJob")
    public void zxTransNotice() {
        // ZX平台交易通知处理任务
    }
}
```

---

## 三、核心服务

### 3.1 AccountCheckService（账户核对服务）

```java
public interface AccountCheckService {
    // 账户核对
    void checkAccount(String cardCode);

    // 批量账户核对
    void batchCheckAccount(List<String> cardCodes);

    // 获取账户差异
    List<AccountDifference> getAccountDifferences(Date startDate, Date endDate);
}
```

### 3.2 AutoWithdrawService（自动提现服务）

```java
public interface AutoWithdrawService {
    // 自动提现
    void autoWithdraw(String withdrawId);

    // 批量自动提现
    void batchAutoWithdraw(List<String> withdrawIds);

    // 查询待处理提现
    List<WithdrawRequest> queryPendingWithdraws();
}
```

### 3.3 DaySumAmtService（日终汇总服务）

```java
public interface DaySumAmtService {
    // 日终汇总
    void daySumAmt(Date sumDate);

    // 生成日终报表
    DaySumReport generateDaySumReport(Date sumDate);

    // 查询日终汇总
    DaySumDto queryDaySumAmt(Date sumDate);
}
```

### 3.4 TransferRecallService（转账冲正服务）

```java
public interface TransferRecallService {
    // 转账冲正
    void transferRecall(String transferId);

    // 批量转账冲正
    void batchTransferRecall(List<String> transferIds);

    // 查询待冲正转账
    List<TransferRequest> queryPendingRecalls();
}
```

### 3.5 WithDrawBatchService（批量提现服务）

```java
public interface WithDrawBatchService {
    // 批量提现
    void batchWithdraw(List<WithdrawRequest> requests);

    // 查询批量提现结果
    List<WithdrawResult> queryBatchResults(String batchId);
}
```

---

## 四、处理器模式

### 4.1 账户检查处理器接口

```java
public interface BasAccountCheckHandle {
    // 获取平台类型
    String getPlatformType();

    // 查询平台余额
    BankBalanceDto queryPlatformBalance(String cardCode);

    // 比对余额
    AccountDifference compareBalance(CardBalanceDto cardBalance, BankBalanceDto bankBalance);

    // 处理差异
    void handleDifference(AccountDifference difference);
}
```

### 4.2 PA 账户检查处理器

```java
@Service
public class PaAccountCheckHandle extends AbstractBasAccountCheckHandle {

    @Override
    public String getPlatformType() {
        return "PA";
    }

    @Override
    public BankBalanceDto queryPlatformBalance(String cardCode) {
        // 调用PA平台查询接口
    }
}
```

### 4.3 ZX 账户检查处理器

```java
@Service
public class ZxAccountCheckHandle extends AbstractBasAccountCheckHandle {

    @Override
    public String getPlatformType() {
        return "ZX";
    }

    @Override
    public BankBalanceDto queryPlatformBalance(String cardCode) {
        // 调用ZX平台查询接口
    }
}
```

---

## 五、工具类

### 5.1 SftpUtils（SFTP工具类）

```java
public class SftpUtils {
    // 连接SFTP
    public static ChannelSftp connect(String host, int port, String username, String password);

    // 下载文件
    public static void downloadFile(ChannelSftp sftp, String remotePath, String localPath);

    // 上传文件
    public static void uploadFile(ChannelSftp sftp, String localPath, String remotePath);

    // 删除文件
    public static void deleteFile(ChannelSftp sftp, String remotePath);

    // 列出文件
    public static List<String> listFiles(ChannelSftp sftp, String remotePath);
}
```

### 5.2 RedisLockUtils（Redis分布式锁工具类）

```java
public class RedisLockUtils {
    // 获取锁
    public static boolean lockKey(String key, int expireSeconds);

    // 获取锁（不等待）
    public static boolean lockKeyNoWait(String key, int expireSeconds);

    // 释放锁
    public static void unlockKey(String key);

    // 尝试获取锁
    public static boolean tryLock(String key, int expireSeconds, int timeoutSeconds);
}
```

---

## 六、任务执行流程

### 6.1 账户核对流程

```
1. 定时触发
   ↓
2. 获取待核对账户列表
   ↓
3. 遍历账户列表
   ↓
4. 根据平台类型选择处理器
   ↓
5. 调用平台查询余额
   ↓
6. 比对系统余额
   ↓
7. 记录差异
   ↓
8. 发送告警
   ↓
9. 更新核对记录
```

### 6.2 自动提现流程

```
1. 定时触发
   ↓
2. 查询待处理提现请求
   ↓
3. 批量处理提现请求
   ↓
4. 调用平台提现接口
   ↓
5. 更新提现状态
   ↓
6. 处理提现结果
   ↓
7. 发送通知
```

### 6.3 日终汇总流程

```
1. 定时触发（凌晨1点）
   ↓
2. 检查是否已汇总
   ↓
3. 汇总当日交易
   ↓
4. 汇总账户余额
   ↓
5. 生成日终报表
   ↓
6. 更新汇总表
   ↓
7. 发送汇总报告
```

---

## 七、任务配置

### 7.1 XXL-Job 配置

```yaml
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8080/xxl-job-admin
    executor:
      appname: fund-catering-task
      address:
      ip:
      port: 9999
      logpath: /data/applogs/xxl-job/jobhandler
      logretentiondays: 30
```

### 7.2 线程池配置

```java
@Configuration
public class ScheduledConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("task-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}
```

---

## 八、监控与告警

### 8.1 任务执行监控

- 任务执行时间监控
- 任务执行成功率监控
- 任务执行失败告警
- 任务执行超时告警

### 8.2 数据核对告警

- 账户余额差异告警
- 交易数据差异告警
- 文件下载失败告警
- 接口调用失败告警

---

**文档生成时间**: 2026-03-06
**模块版本**: 1.0-SNAPSHOT
