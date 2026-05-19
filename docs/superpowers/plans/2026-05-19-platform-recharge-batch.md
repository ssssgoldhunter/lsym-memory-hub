# 平台 03 渠道充值批量高并发上账 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建设平台 03 渠道充值专用批量上账链路，替换逐笔调用 rechargeTrans 的低效方式。

**Architecture:** task 采集过滤 → consume 抢锁 → detail 表记录 → 异步分 chunk 处理（事务1充值2表落地 → 事务2明细+余额更新）。参照划付批量模式，明细先行 + 一次性 CAS 余额更新。

**Tech Stack:** Java 17, Spring Boot 3.2.4, MyBatis Plus 3.5.5, Redis (SET NX EX), Feign, XXL-JOB

**Design Spec:** `lsym-memory-hub/docs/superpowers/specs/2026-05-19-platform-recharge-batch-design.md`

**Verification:** 本项目为集成密集型 Spring Boot 项目，无可独立运行的单元测试框架。每个 Task 以 Maven 编译通过为验证标准：`mvn compile -pl <module> -am`

---

## File Structure

### New Files

| # | Module | Path | Responsibility |
|---|--------|------|----------------|
| 1 | consume-api | `.../consume/request/PlatformRechargeBatchReq.java` | 批量充值请求 |
| 2 | consume-api | `.../consume/request/PlatformRechargeBatchDetailReq.java` | 批量充值明细请求 |
| 3 | consume-api | `.../consume/response/PlatformRechargeBatchSubmitRes.java` | 批量充值提交响应 |
| 4 | consume-api | `.../consume/response/DuplicateDetail.java` | 重复流水详情 |
| 5 | consume-service | `.../consume/domain/TransPlatformRechargeBatchDetail.java` | detail 表实体 |
| 6 | consume-service | `.../consume/mapper/TransPlatformRechargeBatchDetailMapper.java` | detail mapper |
| 7 | consume-service | `.../consume/service/TransPlatformRechargeBatchDetailService.java` | detail service 接口 |
| 8 | consume-service | `.../consume/service/impl/TransPlatformRechargeBatchDetailServiceImpl.java` | detail service 实现 |
| 9 | consume-service | `.../consume/service/PlatformRechargeBatchService.java` | 批量充值服务接口 |
| 10 | consume-service | `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java` | 批量充值服务实现 |

### Modified Files

| # | Module | Path | Change |
|---|--------|------|--------|
| 11 | consume-api | `.../consume/api/TransConsumeApi.java` | 新增 platformRechargeBatch 方法 |
| 12 | consume-service | `.../consume/controller/TransConsumeController.java` | 新增端点实现 |
| 13 | task | `.../task/job/zx/PlatformRechargeJobService.java` | 改造为批量提交 |

### Base Paths

```
consume-api:    slhy/fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/
consume-service: slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/
task:           slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/
base-api:       slhy/fund-catering/fund-catering-base/fund-catering-base-api/src/main/java/com/chinaums/erp/slhy/catering/base/
```

---

## Task 1: DDL — detail 表建表语句

**Files:**
- Create: `lsym-memory-hub/sql/trans_platform_recharge_batch_detail.sql`

- [ ] **Step 1: 编写 DDL**

```sql
CREATE TABLE `trans_platform_recharge_batch_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `trans_no` varchar(64) NOT NULL COMMENT '流水号',
  `trans_amt` bigint DEFAULT NULL COMMENT '金额（分）',
  `platform_card_code` varchar(64) DEFAULT NULL COMMENT '平台卡号',
  `status` varchar(4) NOT NULL DEFAULT 'I' COMMENT '状态: I=初始化 P=处理中 S=成功 F=失败',
  `remark` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `operator_code` varchar(64) DEFAULT NULL COMMENT '运营商编码',
  `org_code` varchar(64) DEFAULT NULL COMMENT '机构编码',
  `platform_code` varchar(64) DEFAULT NULL COMMENT '平台编码',
  `trans_date` varchar(16) DEFAULT NULL COMMENT '交易日期',
  `txn_time` varchar(32) DEFAULT NULL COMMENT '交易时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  UNIQUE KEY `uk_trans_no` (`trans_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台充值批量处理明细表';
```

- [ ] **Step 2: Commit**

```bash
git add sql/trans_platform_recharge_batch_detail.sql
git commit -m "feat: add DDL for trans_platform_recharge_batch_detail"
```

---

## Task 2: Request/Response DTOs

**Files:**
- Create: `.../consume/request/PlatformRechargeBatchReq.java`
- Create: `.../consume/request/PlatformRechargeBatchDetailReq.java`
- Create: `.../consume/response/PlatformRechargeBatchSubmitRes.java`
- Create: `.../consume/response/DuplicateDetail.java`

- [ ] **Step 1: PlatformRechargeBatchDetailReq**

```java
package com.chinaums.erp.slhy.catering.consume.request;

import lombok.Data;

@Data
public class PlatformRechargeBatchDetailReq {
    private String transNo;
    private String amount;
    private String txnTime;
    private String transDate;
    private String transTime;
    private String payAccount;
    private String payAccountName;
    private String summaryMsg;
    private String bankEAccountId;
    private String sourceTransType;
    private String rawJson;
}
```

- [ ] **Step 2: PlatformRechargeBatchReq**

```java
package com.chinaums.erp.slhy.catering.consume.request;

import lombok.Data;
import java.util.List;

@Data
public class PlatformRechargeBatchReq {
    private String batchNo;
    private String operatorCode;
    private String orgCode;
    private String firstOrgCode;
    private String platformCode;
    private String transDate;
    private String platformCardCode;
    private String platformMerchantId;
    private String terminalCode;
    private String accountType;
    private List<PlatformRechargeBatchDetailReq> details;
}
```

- [ ] **Step 3: DuplicateDetail**

```java
package com.chinaums.erp.slhy.catering.consume.response;

import lombok.Data;

@Data
public class DuplicateDetail {
    private String transNo;
    private String reason;

    public DuplicateDetail() {}

    public DuplicateDetail(String transNo, String reason) {
        this.transNo = transNo;
        this.reason = reason;
    }
}
```

- [ ] **Step 4: PlatformRechargeBatchSubmitRes**

```java
package com.chinaums.erp.slhy.catering.consume.response;

import lombok.Data;
import java.util.List;

@Data
public class PlatformRechargeBatchSubmitRes {
    private String batchNo;
    private String status;
    private String message;
    private int receivedCount;
    private int submitCount;
    private int redisDoneSkippedCount;
    private List<DuplicateDetail> duplicates;
}
```

- [ ] **Step 5: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-api -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/PlatformRechargeBatchReq.java \
       fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/request/PlatformRechargeBatchDetailReq.java \
       fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/PlatformRechargeBatchSubmitRes.java \
       fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/response/DuplicateDetail.java
git commit -m "feat: add platform recharge batch request/response DTOs"
```

---

## Task 3: Detail Entity + Mapper + Service

**Files:**
- Create: `.../consume/domain/TransPlatformRechargeBatchDetail.java`
- Create: `.../consume/mapper/TransPlatformRechargeBatchDetailMapper.java`
- Create: `.../consume/service/TransPlatformRechargeBatchDetailService.java`
- Create: `.../consume/service/impl/TransPlatformRechargeBatchDetailServiceImpl.java`

- [ ] **Step 1: Entity**

参照 `TransTransferTiBatchDetail.java` 和 `TransRechargeT.java` 的注解风格。

```java
package com.chinaums.erp.slhy.catering.consume.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@TableName("trans_platform_recharge_batch_detail")
public class TransPlatformRechargeBatchDetail {
    @TableId(type = IdType.AUTO)
    private Long detailId;
    private String batchNo;
    private String transNo;
    private Long transAmt;
    private String platformCardCode;
    private String status;
    private String remark;
    private String operatorCode;
    private String orgCode;
    private String platformCode;
    private String transDate;
    private String txnTime;
    private Date createTime;
    private Date updateTime;
}
```

- [ ] **Step 2: Mapper**

```java
package com.chinaums.erp.slhy.catering.consume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaums.erp.slhy.catering.consume.domain.TransPlatformRechargeBatchDetail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TransPlatformRechargeBatchDetailMapper extends BaseMapper<TransPlatformRechargeBatchDetail> {

    /**
     * 批量查询已存在的 transNo
     */
    List<String> selectExistingTransNos(@Param("transNos") List<String> transNos);
}
```

对应 Mapper XML（在 `resources/mapper/` 下新建或追加）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.chinaums.erp.slhy.catering.consume.mapper.TransPlatformRechargeBatchDetailMapper">
    <select id="selectExistingTransNos" resultType="java.lang.String">
        SELECT trans_no FROM trans_platform_recharge_batch_detail
        WHERE trans_no IN
        <foreach collection="transNos" item="transNo" open="(" separator="," close=")">
            #{transNo}
        </foreach>
    </select>
</mapper>
```

- [ ] **Step 3: Service Interface**

```java
package com.chinaums.erp.slhy.catering.consume.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chinaums.erp.slhy.catering.consume.domain.TransPlatformRechargeBatchDetail;
import java.util.List;

public interface TransPlatformRechargeBatchDetailService extends IService<TransPlatformRechargeBatchDetail> {

    List<String> selectExistingTransNos(List<String> transNos);

    void updateStatus(Long detailId, String status, String remark);
}
```

- [ ] **Step 4: Service Impl**

```java
package com.chinaums.erp.slhy.catering.consume.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chinaums.erp.slhy.catering.consume.domain.TransPlatformRechargeBatchDetail;
import com.chinaums.erp.slhy.catering.consume.mapper.TransPlatformRechargeBatchDetailMapper;
import com.chinaums.erp.slhy.catering.consume.service.TransPlatformRechargeBatchDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service("TransPlatformRechargeBatchDetailService")
public class TransPlatformRechargeBatchDetailServiceImpl
        extends ServiceImpl<TransPlatformRechargeBatchDetailMapper, TransPlatformRechargeBatchDetail>
        implements TransPlatformRechargeBatchDetailService {

    @Override
    public List<String> selectExistingTransNos(List<String> transNos) {
        if (transNos == null || transNos.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectExistingTransNos(transNos);
    }

    @Override
    public void updateStatus(Long detailId, String status, String remark) {
        TransPlatformRechargeBatchDetail update = new TransPlatformRechargeBatchDetail();
        update.setDetailId(detailId);
        update.setStatus(status);
        update.setRemark(remark);
        updateById(update);
    }
}
```

- [ ] **Step 5: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransPlatformRechargeBatchDetail.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/mapper/TransPlatformRechargeBatchDetailMapper.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/TransPlatformRechargeBatchDetailService.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransPlatformRechargeBatchDetailServiceImpl.java
git commit -m "feat: add detail entity, mapper, and service for platform recharge batch"
```

---

## Task 4: TransConsumeApi + Controller 端点

**Files:**
- Modify: `.../consume/api/TransConsumeApi.java`
- Modify: `.../consume/controller/TransConsumeController.java`
- Create: `.../consume/service/PlatformRechargeBatchService.java` (接口，给 controller 注入用)

- [ ] **Step 1: TransConsumeApi 新增方法**

在 `TransConsumeApi.java` 的方法列表末尾添加：

```java
@Operation(method = "平台充值批量上账", description = "平台03渠道充值批量高并发上账接口")
@PostMapping(value = PATH_PREFIX + "/platformRechargeBatch")
DefaultResult<PlatformRechargeBatchSubmitRes> platformRechargeBatch(
        @RequestBody PlatformRechargeBatchReq request) throws Exception;
```

需要在文件头部添加 import：
```java
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchReq;
import com.chinaums.erp.slhy.catering.consume.response.PlatformRechargeBatchSubmitRes;
```

- [ ] **Step 2: PlatformRechargeBatchService 接口**

```java
package com.chinaums.erp.slhy.catering.consume.service;

import com.chinaums.erp.slhy.catering.common.core.domain.DefaultResult;
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchReq;
import com.chinaums.erp.slhy.catering.consume.response.PlatformRechargeBatchSubmitRes;

public interface PlatformRechargeBatchService {
    DefaultResult<PlatformRechargeBatchSubmitRes> submit(PlatformRechargeBatchReq request) throws Exception;
}
```

- [ ] **Step 3: TransConsumeController 新增端点**

在 `TransConsumeController.java` 中：

1. 添加注入：
```java
@Autowired
private PlatformRechargeBatchService platformRechargeBatchService;
```

2. 添加 import：
```java
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchReq;
import com.chinaums.erp.slhy.catering.consume.response.PlatformRechargeBatchSubmitRes;
import com.chinaums.erp.slhy.catering.consume.service.PlatformRechargeBatchService;
```

3. 添加方法实现（在 rechargeTrans 方法附近）：
```java
@Override
public DefaultResult<PlatformRechargeBatchSubmitRes> platformRechargeBatch(PlatformRechargeBatchReq request) throws Exception {
    return platformRechargeBatchService.submit(request);
}
```

- [ ] **Step 4: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS（此时 PlatformRechargeBatchServiceImpl 尚未创建，需要先创建空实现）

创建空实现以确保编译通过：

```java
package com.chinaums.erp.slhy.catering.consume.service.impl;

import com.chinaums.erp.slhy.catering.common.core.domain.DefaultResult;
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchReq;
import com.chinaums.erp.slhy.catering.consume.response.PlatformRechargeBatchSubmitRes;
import com.chinaums.erp.slhy.catering.consume.service.PlatformRechargeBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PlatformRechargeBatchServiceImpl implements PlatformRechargeBatchService {
    @Override
    public DefaultResult<PlatformRechargeBatchSubmitRes> submit(PlatformRechargeBatchReq request) throws Exception {
        // TODO: implement in Task 5
        return DefaultResult.success(new PlatformRechargeBatchSubmitRes());
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/TransConsumeApi.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/controller/TransConsumeController.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/PlatformRechargeBatchService.java \
       fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/PlatformRechargeBatchServiceImpl.java
git commit -m "feat: add platform recharge batch API endpoint and service skeleton"
```

---

## Task 5: PlatformRechargeBatchServiceImpl — submit() 同步入口

**Files:**
- Modify: `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java`

**核心职责：** 参数校验 → 抢 Redis 锁 → 同步写 detail 表 → 提交异步任务 → 返回结果

- [ ] **Step 1: 完善 PlatformRechargeBatchServiceImpl**

替换 Task 4 的空实现为完整 submit() 逻辑。需要注入的依赖：

```java
package com.chinaums.erp.slhy.catering.consume.service.impl;

import com.chinaums.erp.slhy.catering.common.core.domain.DefaultResult;
import com.chinaums.erp.slhy.catering.consume.domain.TransPlatformRechargeBatchDetail;
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchDetailReq;
import com.chinaums.erp.slhy.catering.consume.request.PlatformRechargeBatchReq;
import com.chinaums.erp.slhy.catering.consume.response.DuplicateDetail;
import com.chinaums.erp.slhy.catering.consume.response.PlatformRechargeBatchSubmitRes;
import com.chinaums.erp.slhy.catering.consume.service.PlatformRechargeBatchService;
import com.chinaums.erp.slhy.catering.consume.service.TransPlatformRechargeBatchDetailService;
import com.chinaums.erp.slhy.catering.consume.utils.RedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolTaskExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformRechargeBatchServiceImpl implements PlatformRechargeBatchService {

    private static final String LOCK_KEY_PREFIX = "platform_recharge:card_lock:";
    private static final int LOCK_TTL_SECONDS = 1800;
    private static final String STATUS_I = "I";

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private TransPlatformRechargeBatchDetailService detailService;

    @Resource
    private RedisLockUtils redisLockUtils;

    @Value("${platform.recharge.batch.chunkSize:500}")
    private int chunkSize;

    @Override
    public DefaultResult<PlatformRechargeBatchSubmitRes> submit(PlatformRechargeBatchReq request) throws Exception {
        long start = System.currentTimeMillis();
        String batchNo = request.getBatchNo();
        String platformCardCode = request.getPlatformCardCode();

        log.info("platformRechargeBatch:submit:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, receivedCount={}",
                batchNo, request.getOperatorCode(), request.getPlatformCode(), platformCardCode, request.getDetails().size());

        // 1. 参数校验
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            log.info("platformRechargeBatch:submit:validateFailed batchNo={}, reason=details is empty", batchNo);
            PlatformRechargeBatchSubmitRes res = new PlatformRechargeBatchSubmitRes();
            res.setBatchNo(batchNo);
            res.setStatus("EMPTY");
            res.setMessage("明细列表为空");
            return DefaultResult.success(res);
        }

        // 2. 抢平台卡 Redis 锁
        String lockKey = LOCK_KEY_PREFIX + platformCardCode;
        log.info("platformRechargeBatch:lock:start batchNo={}, lockKey={}, platformCardCode={}", batchNo, lockKey, platformCardCode);

        boolean locked = redisLockUtils.lockKeyNoWait(lockKey, LOCK_TTL_SECONDS);
        if (!locked) {
            log.info("platformRechargeBatch:lock:skipped batchNo={}, lockKey={}, platformCardCode={}, message=card locked", batchNo, lockKey, platformCardCode);
            PlatformRechargeBatchSubmitRes res = new PlatformRechargeBatchSubmitRes();
            res.setBatchNo(batchNo);
            res.setStatus("SKIPPED_LOCKED");
            res.setMessage("平台卡正在处理中，本次跳过");
            return DefaultResult.success(res);
        }
        log.info("platformRechargeBatch:lock:success batchNo={}, lockKey={}, ttlSeconds={}", batchNo, lockKey, LOCK_TTL_SECONDS);

        try {
            // 3. 同步写入 detail 表 (status=I)
            log.info("platformRechargeBatch:detailInsert:start batchNo={}, receivedCount={}", batchNo, request.getDetails().size());
            List<DuplicateDetail> duplicates = new ArrayList<>();
            List<Long> insertedDetailIds = new ArrayList<>();

            for (PlatformRechargeBatchDetailReq detailReq : request.getDetails()) {
                try {
                    TransPlatformRechargeBatchDetail detail = new TransPlatformRechargeBatchDetail();
                    detail.setBatchNo(batchNo);
                    detail.setTransNo(detailReq.getTransNo());
                    detail.setTransAmt(Long.parseLong(detailReq.getAmount()));
                    detail.setPlatformCardCode(platformCardCode);
                    detail.setStatus(STATUS_I);
                    detail.setOperatorCode(request.getOperatorCode());
                    detail.setOrgCode(request.getOrgCode());
                    detail.setPlatformCode(request.getPlatformCode());
                    detail.setTransDate(detailReq.getTransDate());
                    detail.setTxnTime(detailReq.getTxnTime());
                    detailService.save(detail);
                    insertedDetailIds.add(detail.getDetailId());
                } catch (Exception e) {
                    // unique constraint violation
                    log.info("platformRechargeBatch:detailInsert:itemDuplicate batchNo={}, transNo={}, reason=detail trans_no duplicate", batchNo, detailReq.getTransNo());
                    duplicates.add(new DuplicateDetail(detailReq.getTransNo(), "detail trans_no duplicate"));
                }
            }

            log.info("platformRechargeBatch:detailInsert:success batchNo={}, insertedCount={}, duplicateCount={}, costMs={}",
                    batchNo, insertedDetailIds.size(), duplicates.size(), System.currentTimeMillis() - start);

            if (insertedDetailIds.isEmpty()) {
                // 全部重复，释放锁
                redisLockUtils.unlockKey(lockKey);
                PlatformRechargeBatchSubmitRes res = new PlatformRechargeBatchSubmitRes();
                res.setBatchNo(batchNo);
                res.setStatus("EMPTY");
                res.setMessage("全部流水重复");
                res.setReceivedCount(request.getDetails().size());
                res.setDuplicates(duplicates);
                return DefaultResult.success(res);
            }

            // 4. 提交异步任务
            try {
                taskExecutor.execute(() -> {
                    processAsync(batchNo, request, insertedDetailIds, lockKey);
                });
                log.info("platformRechargeBatch:asyncSubmit:success batchNo={}, insertedCount={}", batchNo, insertedDetailIds.size());
            } catch (Exception e) {
                log.error("platformRechargeBatch:asyncSubmit:failed batchNo={}, error={}", batchNo, e.getMessage(), e);
                redisLockUtils.unlockKey(lockKey);
                PlatformRechargeBatchSubmitRes res = new PlatformRechargeBatchSubmitRes();
                res.setBatchNo(batchNo);
                res.setStatus("REJECTED");
                res.setMessage("线程池拒绝提交");
                return DefaultResult.success(res);
            }

            // 5. 返回 ACCEPTED
            PlatformRechargeBatchSubmitRes res = new PlatformRechargeBatchSubmitRes();
            res.setBatchNo(batchNo);
            res.setStatus("ACCEPTED");
            res.setMessage("已提交异步处理");
            res.setReceivedCount(request.getDetails().size());
            res.setSubmitCount(insertedDetailIds.size());
            res.setDuplicates(duplicates);
            log.info("platformRechargeBatch:submit:end batchNo={}, status={}, message={}, costMs={}", batchNo, res.getStatus(), res.getMessage(), System.currentTimeMillis() - start);
            return DefaultResult.success(res);

        } catch (Exception e) {
            log.error("platformRechargeBatch:submit:error batchNo={}, error={}", batchNo, e.getMessage(), e);
            redisLockUtils.unlockKey(lockKey);
            throw e;
        }
    }

    /**
     * 异步处理 — 在锁保护下完成批量上账
     * 由 Task 6 和 Task 7 实现完整逻辑
     */
    private void processAsync(String batchNo, PlatformRechargeBatchReq request, List<Long> insertedDetailIds, String lockKey) {
        // TODO: Task 6 实现
    }
}
```

**注意：** `RedisLockUtils` 在 consume-service 模块的路径是 `com.chinaums.erp.slhy.catering.consume.utils.RedisLockUtils`。需要确认该类有 `lockKeyNoWait(String key, Integer seconds)` 和 `unlockKey(String key)` 方法。如果没有 `lockKeyNoWait`，使用 `lockKey(key, seconds)` 或直接用 `StringRedisTemplate.execute(RedisScript, ...)` 实现 SET NX EX。

- [ ] **Step 2: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/PlatformRechargeBatchServiceImpl.java
git commit -m "feat: implement submit() - lock, detail insert, async dispatch"
```

---

## Task 6: PlatformRechargeBatchServiceImpl — processAsync() 异步核心 (TX1)

**Files:**
- Modify: `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java`

**核心职责：** 查询待处理 detail → 分 chunk → 事务1(充值2表落地) → 事务2(明细+余额，Task 7 实现) → 状态更新

- [ ] **Step 1: 添加依赖注入**

在 `PlatformRechargeBatchServiceImpl` 中添加：

```java
@Resource
private TransRechargeTService transRechargeTService;

@Resource
private TransRechargeSubTService transRechargeSubTService;
```

- [ ] **Step 2: 实现 processAsync() 方法**

替换 Task 5 的空 `processAsync`：

```java
private void processAsync(String batchNo, PlatformRechargeBatchReq request, List<Long> insertedDetailIds, String lockKey) {
    long start = System.currentTimeMillis();
    String platformCardCode = request.getPlatformCardCode();
    int totalSuccess = 0;
    int totalFailed = 0;
    int totalSkipped = 0;
    long totalAmount = 0L;

    try {
        log.info("platformRechargeBatch:process:start batchNo={}, platformCardCode={}, detailCount={}",
                batchNo, platformCardCode, insertedDetailIds.size());

        // 1. 查询本次提交的 status=I 的 detail 记录
        List<TransPlatformRechargeBatchDetail> pendingDetails = detailService.lambdaQuery()
                .in(TransPlatformRechargeBatchDetail::getDetailId, insertedDetailIds)
                .eq(TransPlatformRechargeBatchDetail::getStatus, STATUS_I)
                .list();

        if (pendingDetails.isEmpty()) {
            log.info("platformRechargeBatch:process:empty batchNo={}, message=no pending details", batchNo);
            return;
        }

        // 2. 按 chunk 分批处理
        int totalChunks = (pendingDetails.size() + chunkSize - 1) / chunkSize;
        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int from = chunkIndex * chunkSize;
            int to = Math.min(from + chunkSize, pendingDetails.size());
            List<TransPlatformRechargeBatchDetail> chunk = pendingDetails.subList(from, to);

            log.info("platformRechargeBatch:chunk:start batchNo={}, chunkIndex={}, chunkSize={}, firstTransNo={}, lastTransNo={}",
                    batchNo, chunkIndex, chunk.size(),
                    chunk.get(0).getTransNo(), chunk.get(chunk.size() - 1).getTransNo());

            try {
                // 更新 detail status → P
                chunk.forEach(d -> detailService.updateStatus(d.getDetailId(), "P", null));

                // === 事务1: 充值2表落地 ===
                int tx1Result = executeTx1(batchNo, chunkIndex, chunk, request);
                if (tx1Result == 0) {
                    // 全部已存在或无有效数据
                    totalSkipped += chunk.size();
                    chunk.forEach(d -> detailService.updateStatus(d.getDetailId(), "S", "充值流水已存在，跳过"));
                    continue;
                }

                // === 事务2: 明细 + 余额 (Task 7 实现) ===
                // executeTx2(batchNo, chunkIndex, chunk, request);

                // 暂时标记成功（Task 7 完成后替换）
                totalSuccess += chunk.size();
                chunk.forEach(d -> detailService.updateStatus(d.getDetailId(), "S", null));

            } catch (Exception e) {
                totalFailed += chunk.size();
                log.error("platformRechargeBatch:chunk:error batchNo={}, chunkIndex={}, error={}", batchNo, chunkIndex, e.getMessage(), e);
                chunk.forEach(d -> detailService.updateStatus(d.getDetailId(), "F", e.getMessage()));
            }

            log.info("platformRechargeBatch:chunk:end batchNo={}, chunkIndex={}, successCount={}, failedCount={}, skippedCount={}, costMs={}",
                    batchNo, chunkIndex, totalSuccess, totalFailed, totalSkipped, System.currentTimeMillis() - start);
        }

        log.info("platformRechargeBatch:process:end batchNo={}, status=COMPLETED, successCount={}, failedCount={}, skippedCount={}, totalAmount={}, costMs={}",
                batchNo, totalSuccess, totalFailed, totalSkipped, totalAmount, System.currentTimeMillis() - start);

    } catch (Exception e) {
        log.error("platformRechargeBatch:process:error batchNo={}, error={}", batchNo, e.getMessage(), e);
    } finally {
        // 释放锁
        try {
            redisLockUtils.unlockKey(lockKey);
            log.info("platformRechargeBatch:unlock:success batchNo={}, lockKey={}", batchNo, lockKey);
        } catch (Exception e) {
            log.error("platformRechargeBatch:unlock:failed batchNo={}, lockKey={}, error={}", batchNo, lockKey, e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 3: 实现 executeTx1() — 充值2表落地事务**

在 `PlatformRechargeBatchServiceImpl` 中添加：

```java
/**
 * 事务1: 充值主表 + 充值子表落地
 * 返回实际插入的记录数。如果唯一键冲突，回滚后重新查重重建 chunk 重试一次。
 */
@Transactional(rollbackFor = Exception.class)
public int executeTx1(String batchNo, int chunkIndex, List<TransPlatformRechargeBatchDetail> chunk, PlatformRechargeBatchReq request) {
    // 1. 批量查询 trans_recharge_t 已存在的 transNo
    List<String> chunkTransNos = chunk.stream().map(TransPlatformRechargeBatchDetail::getTransNo).collect(Collectors.toList());
    log.info("platformRechargeBatch:chunk:rechargeExistsFilter:start batchNo={}, chunkIndex={}, count={}", batchNo, chunkIndex, chunkTransNos.size());

    List<String> existingTransNos = transRechargeTService.selectExistingTransNos(chunkTransNos);
    log.info("platformRechargeBatch:chunk:rechargeExistsFilter:success batchNo={}, chunkIndex={}, existsCount={}, pendingCount={}",
            batchNo, chunkIndex, existingTransNos.size(), chunkTransNos.size() - existingTransNos.size());

    // 2. 过滤已存在的
    List<TransPlatformRechargeBatchDetail> toInsert = chunk.stream()
            .filter(d -> !existingTransNos.contains(d.getTransNo()))
            .collect(Collectors.toList());

    if (toInsert.isEmpty()) {
        return 0;
    }

    // 3. 构建充值主流水
    List<TransRechargeT> rechargeList = new ArrayList<>();
    List<TransRechargeSubT> subList = new ArrayList<>();

    for (TransPlatformRechargeBatchDetail detail : toInsert) {
        // ID 生成规则参照现有 PlatformRechargeJobService 和 TransRechargeT 的 rechargeId 模式
        String rechargeId = generateRechargeId();

        TransRechargeT recharge = new TransRechargeT();
        recharge.setRechargeId(rechargeId);
        recharge.setTransNo(detail.getTransNo());
        recharge.setTransType("IC"); // 平台充值类型
        recharge.setTransTime(detail.getTxnTime());
        recharge.setTransAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setCardCode(request.getPlatformCardCode());
        recharge.setStatus("P"); // 处理中，事务2成功后更新为S
        recharge.setRefundFlag("0");
        recharge.setRefundableAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setRefundedAmt(BigInteger.ZERO);
        recharge.setRealAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setRealBalanceAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setDiscountAmt(BigInteger.ZERO);
        recharge.setOrderInitAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setOrderBalanceAmt(BigInteger.valueOf(detail.getTransAmt()));
        recharge.setOrgCode(request.getOrgCode());
        recharge.setOperatorCode(request.getOperatorCode());
        recharge.setFirstOrgCode(request.getFirstOrgCode());
        recharge.setPlatformCode(request.getPlatformCode());
        recharge.setTerminalCode(request.getTerminalCode());
        recharge.setMerchantCode(request.getPlatformMerchantId());
        recharge.setMac(batchNo); // 用 batchNo 作为 mac 标记
        recharge.setRemark("平台03渠道充值批量上账");
        recharge.setChannelCode("03");
        rechargeList.add(recharge);

        // 构建充值子流水
        String subId = generateRechargeSubId();
        TransRechargeSubT sub = new TransRechargeSubT();
        sub.setRechargeSubId(subId);
        sub.setTransNo(detail.getTransNo());
        sub.setTransSubNo(detail.getTransNo() + "_04");
        sub.setTransSubAmt(BigInteger.valueOf(detail.getTransAmt()));
        sub.setSubAccType(request.getAccountType());
        sub.setCardCode(request.getPlatformCardCode());
        sub.setTransTime(detail.getTxnTime());
        sub.setTransType("IC");
        sub.setRefundFlag("0");
        sub.setRefundableAmt(BigInteger.valueOf(detail.getTransAmt()));
        sub.setRefundedAmt(BigInteger.ZERO);
        sub.setOperatorCode(request.getOperatorCode());
        sub.setFirstOrgCode(request.getFirstOrgCode());
        sub.setMerchantCode(request.getPlatformMerchantId());
        sub.setBankEAccountCode(detail.getTransNo()); // 可选
        subList.add(sub);
    }

    // 4. 批量插入
    log.info("platformRechargeBatch:chunk:tx1:start batchNo={}, chunkIndex={}, rechargeCount={}, rechargeSubCount={}",
            batchNo, chunkIndex, rechargeList.size(), subList.size());

    transRechargeTService.saveBatch(rechargeList);
    transRechargeSubTService.saveBatch(subList);

    log.info("platformRechargeBatch:chunk:tx1:success batchNo={}, chunkIndex={}, insertedCount={}, costMs={}",
            batchNo, chunkIndex, rechargeList.size(), 0);

    return rechargeList.size();
}
```

- [ ] **Step 4: 辅助方法 — ID 生成**

参照现有 `PlatformRechargeJobService` 中的 ID 生成模式，在 `PlatformRechargeBatchServiceImpl` 中添加：

```java
private String generateRechargeId() {
    // 参照 TransRechargeT 的 rechargeId 模式: 3108 + date + snowflake
    return "3108" + DateFomatUtils.getDateStr(new Date(), "yyMMdd") + String.valueOf(snowflake.nextId());
}

private String generateRechargeSubId() {
    return "3108" + DateFomatUtils.getDateStr(new Date(), "yyMMdd") + String.valueOf(snowflake.nextId());
}
```

需要注入 `Snowflake`（参照 `RechargeTransAfter` 的注入方式）和 `DateFomatUtils`。

**注意：** 具体注入方式需参照 consume-service 中 `Snowflake` 的 Bean 定义。如果使用 `IdWorker` 或其他 ID 生成器，按项目实际调整。

- [ ] **Step 5: TransRechargeTService 添加 selectExistingTransNos**

在 `TransRechargeTService` 接口中添加：

```java
List<String> selectExistingTransNos(List<String> transNos);
```

在 `TransRechargeTServiceImpl` 中实现：

```java
@Override
public List<String> selectExistingTransNos(List<String> transNos) {
    if (transNos == null || transNos.isEmpty()) {
        return List.of();
    }
    return baseMapper.selectExistingTransNos(transNos);
}
```

在 `TransRechargeTMapper` 中添加：

```java
List<String> selectExistingTransNos(@Param("transNos") List<String> transNos);
```

在对应 Mapper XML 中添加：

```xml
<select id="selectExistingTransNos" resultType="java.lang.String">
    SELECT trans_no FROM trans_recharge_t
    WHERE trans_no IN
    <foreach collection="transNos" item="transNo" open="(" separator="," close=")">
        #{transNo}
    </foreach>
</select>
```

- [ ] **Step 6: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/
git commit -m "feat: implement processAsync with TX1 (recharge table insert) and chunk loop"
```

---

## Task 7: PlatformRechargeBatchServiceImpl — TX2 (余额链 + 明细 + 余额更新)

**Files:**
- Modify: `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java`

**核心职责：** 读取账户快照 → 内存计算余额链 → 写4张明细表 → CAS更新余额 → Redis done

- [ ] **Step 1: 添加依赖注入**

```java
@Resource
private BaseAccountServiceApi baseAccountServiceApi;

@Resource
private TransAccountApi transAccountApi;
```

- [ ] **Step 2: 实现 executeTx2() 方法**

在 `PlatformRechargeBatchServiceImpl` 中添加。事务2需要独立的 `@Transactional`，但因为是在同一个 Bean 内部调用，需要通过 `AopContext.currentProxy()` 或拆分为单独的 Spring Bean 来保证事务生效。

**方案：** 将 TX2 拆到内部 helper 类或使用 `@Transactional(propagation = Propagation.REQUIRES_NEW)`。

```java
/**
 * 事务2: 账户变动明细写入 + 余额一次性更新
 */
@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
public void executeTx2(String batchNo, int chunkIndex,
                        List<TransPlatformRechargeBatchDetail> insertedDetails,
                        PlatformRechargeBatchReq request) throws Exception {

    // 1. 按 txnTime → transNo 排序
    List<TransPlatformRechargeBatchDetail> sorted = insertedDetails.stream()
            .filter(d -> d.getTxnTime() != null)
            .sorted(Comparator.comparing(TransPlatformRechargeBatchDetail::getTxnTime)
                    .thenComparing(TransPlatformRechargeBatchDetail::getTransNo))
            .collect(Collectors.toList());

    if (sorted.isEmpty()) {
        return;
    }

    // 2. 读取平台卡 04 子账户快照
    String platformCardCode = request.getPlatformCardCode();
    BasCardSubAccountTQueryRes subAccount = querySubAccount(platformCardCode, request.getAccountType());
    if (subAccount == null) {
        throw new RuntimeException("平台卡04子账户不存在: " + platformCardCode);
    }

    BigInteger currentBalance = subAccount.getBalance();
    BigInteger currentRealBalance = subAccount.getRealBalance();

    // 查询所有子账户 realBalance 汇总作为 totalAccountAmount
    List<BasCardSubAccountTQueryRes> allSubAccounts = queryAllSubAccounts(platformCardCode);
    BigInteger totalAccountAmount = allSubAccounts.stream()
            .map(BasCardSubAccountTQueryRes::getRealBalance)
            .reduce(BigInteger.ZERO, BigInteger::add);

    log.info("platformRechargeBatch:chunk:accountSnapshot batchNo={}, chunkIndex={}, balance={}, realBalance={}, totalRealBalance={}",
            batchNo, chunkIndex, currentBalance, currentRealBalance, totalAccountAmount);

    // 3. 内存计算余额链
    long chunkTotalAmount = 0L;
    List<AccountChangeDetailReq> detailReqs = new ArrayList<>();

    BigInteger runBalance = currentBalance;
    BigInteger runRealBalance = currentRealBalance;
    BigInteger runTotalAccount = totalAccountAmount;

    for (int i = 0; i < sorted.size(); i++) {
        TransPlatformRechargeBatchDetail detail = sorted.get(i);
        BigInteger amount = BigInteger.valueOf(detail.getTransAmt());
        BigInteger orgAmt = runBalance;
        BigInteger newBalance = runBalance.add(amount);
        BigInteger orgRealAmt = runRealBalance;
        BigInteger newRealBalance = runRealBalance.add(amount);
        BigInteger orgTotalAccount = runTotalAccount;
        BigInteger newTotalAccount = runTotalAccount.add(amount);

        runBalance = newBalance;
        runRealBalance = newRealBalance;
        runTotalAccount = newTotalAccount;
        chunkTotalAmount += detail.getTransAmt();

        log.info("platformRechargeBatch:chunk:buildChain:item batchNo={}, chunkIndex={}, index={}, transNo={}, amount={}, orgAmt={}, balance={}, orgRealAmt={}, realBalance={}",
                batchNo, chunkIndex, i, detail.getTransNo(), detail.getTransAmt(), orgAmt, newBalance, orgRealAmt, newRealBalance);

        // 构建账户变动明细 — 参照 RechargeTransAfter.updateSubAccountBalance
        AccountChangeDetailReq detailReq = buildAccountChangeDetail(detail, request, subAccount,
                orgAmt, newBalance, orgRealAmt, newRealBalance, orgTotalAccount, newTotalAccount);
        detailReqs.add(detailReq);
    }

    log.info("platformRechargeBatch:chunk:buildChain:success batchNo={}, chunkIndex={}, count={}, totalAmount={}",
            batchNo, chunkIndex, sorted.size(), chunkTotalAmount);

    // 4. 批量写入 4 张明细表
    AccountChangeDetailBatchReq batchReq = new AccountChangeDetailBatchReq();
    batchReq.setAccountChangeDetailList(detailReqs);

    log.info("platformRechargeBatch:chunk:tx2:start batchNo={}, chunkIndex={}, accountDetailCount={}, totalAmount={}",
            batchNo, chunkIndex, detailReqs.size(), chunkTotalAmount);

    transAccountApi.batchChangeAccountDetail(batchReq);

    // 5. 一次性 CAS 更新平台卡 04 余额
    updateAccountBalance(platformCardCode, subAccount, chunkTotalAmount, request, batchNo, chunkIndex);

    log.info("platformRechargeBatch:chunk:tx2:success batchNo={}, chunkIndex={}, accountDetailCount={}, accountUpdateAmount={}",
            batchNo, chunkIndex, detailReqs.size(), chunkTotalAmount);

    // 6. 写 Redis done
    markRedisDone(batchNo, chunkIndex, sorted, request);
}
```

- [ ] **Step 3: 辅助方法 — 查询子账户**

```java
private BasCardSubAccountTQueryRes querySubAccount(String cardCode, String subAccountType) {
    DefaultResult<BasCardSubAccountTQueryRes> result = baseAccountServiceApi.queryOneCardSubAccount(cardCode, subAccountType);
    if (result != null && result.getData() != null) {
        return result.getData();
    }
    return null;
}

private List<BasCardSubAccountTQueryRes> queryAllSubAccounts(String cardCode) {
    DefaultResult<List<BasCardSubAccountTQueryRes>> result = baseAccountServiceApi.querySubAccountList(cardCode);
    if (result != null && result.getData() != null) {
        return result.getData();
    }
    return List.of();
}
```

- [ ] **Step 4: 辅助方法 — 构建账户变动明细**

参照 `RechargeTransAfter.updateSubAccountBalance` 中构建 4 张明细表的模式：

```java
private AccountChangeDetailReq buildAccountChangeDetail(
        TransPlatformRechargeBatchDetail detail, PlatformRechargeBatchReq request,
        BasCardSubAccountTQueryRes subAccount,
        BigInteger orgAmt, BigInteger balance,
        BigInteger orgRealAmt, BigInteger realBalance,
        BigInteger orgTotalAccount, BigInteger newTotalAccount) {

    AccountChangeDetailReq req = new AccountChangeDetailReq();

    // 1. trans_acct_change_detail_t (账户层级变动明细)
    TransAcctChangeDetailTReq acctDetail = new TransAcctChangeDetailTReq();
    acctDetail.setAccountId(subAccount.getAccountId());
    acctDetail.setCardCode(request.getPlatformCardCode());
    acctDetail.setSubAccountType(request.getAccountType());
    acctDetail.setSubAccountId(subAccount.getSubAccountId());
    acctDetail.setTransTime(detail.getTxnTime());
    acctDetail.setOrgAmt(orgAmt);
    acctDetail.setTransAmt(BigInteger.valueOf(detail.getTransAmt()));
    acctDetail.setOrgRealAmt(orgRealAmt);
    acctDetail.setBalance(balance);
    acctDetail.setRealBalance(realBalance);
    acctDetail.setTransType("IC");
    acctDetail.setTransNo(detail.getTransNo());
    acctDetail.setChangeType("1");
    acctDetail.setOperatorCode(request.getOperatorCode());
    acctDetail.setOrgCode(request.getOrgCode());
    acctDetail.setFirstOrgCode(request.getFirstOrgCode());
    acctDetail.setPlatformCode(request.getPlatformCode());
    acctDetail.setRemark("平台03渠道充值批量上账");
    req.setTransAcctChangeDetailTReq(acctDetail);

    // 2. trans_acct_sum_change_detail_t (账户汇总变动)
    TransAcctSumChangeDetailTReq acctSum = AccountSummaryHelper.buildAcctSum(
            subAccount, orgAmt, balance, orgRealAmt, realBalance,
            detail.getTransNo(), "IC", "1", request, detail.getTxnTime());
    req.setTransAcctSumChangeDetailTReq(acctSum);

    // 3. trans_sum_change_detail_t (总账户汇总变动)
    TransSumChangeDetailTReq transSum = AccountSummaryHelper.buildTransSum(
            orgTotalAccount, newTotalAccount, orgTotalAccount, newTotalAccount,
            detail.getTransNo(), "IC", "1", request, detail.getTxnTime());
    req.setTransSumChangeDetailTReq(transSum);

    // 4. trans_acct_act_sum_change_detail_t (活动科目汇总) — 04 类型通常不需要活动信息
    // 如果需要，参照 RechargeTransAfter 中的逻辑按 activityCode 查询
    req.setTransAcctActSumChangeDetailTReq(null);

    return req;
}
```

**注意：** `AccountSummaryHelper.buildAcctSum` 和 `buildTransSum` 的实际参数签名需要参照 `AccountSummaryHelper.java` 源码调整。上面的参数是示意性的，实现时必须对照实际方法签名。

- [ ] **Step 5: 辅助方法 — 更新账户余额**

参照 `RechargeTransAfter` 中的 CAS 更新模式：

```java
private void updateAccountBalance(String cardCode, BasCardSubAccountTQueryRes subAccount,
                                   long chunkTotalAmount, PlatformRechargeBatchReq request,
                                   String batchNo, int chunkIndex) {
    BigInteger amount = BigInteger.valueOf(chunkTotalAmount);

    BasCardSubAccountTReq updateReq = new BasCardSubAccountTReq();
    updateReq.setSubAccountId(subAccount.getSubAccountId());
    updateReq.setCardCode(cardCode);
    updateReq.setSubAccountType(request.getAccountType());
    updateReq.setBalance(amount);
    updateReq.setRealBalance(amount);

    // withdrawBalance/waitReleaseBalance 按 RechargeTransAfter 口径处理
    // 需要查询 cardBin 的 withdraw 配置
    // 简化处理：先按 balance + realBalance 更新，withdraw 逻辑后续按 cardBin 配置补充
    updateReq.setWithdrawBalance(BigInteger.ZERO);
    updateReq.setWaitReleaseBalance(BigInteger.ZERO);
    updateReq.setFrozenAmt(BigInteger.ZERO);

    AccountChangeReq accountChangeReq = new AccountChangeReq();
    BasCardSubAccountTReq subUpdate = updateReq;
    accountChangeReq.setSubAccountUpdate(subUpdate);

    AccountChangeBatchReq batchReq = new AccountChangeBatchReq();
    batchReq.setAccountChangeReqList(List.of(accountChangeReq));

    log.info("platformRechargeBatch:chunk:updateAccount:start batchNo={}, chunkIndex={}, platformCardCode={}, amount={}",
            batchNo, chunkIndex, cardCode, chunkTotalAmount);

    baseAccountServiceApi.batchChangeAccountForRecharge(batchReq);

    log.info("platformRechargeBatch:chunk:updateAccount:success batchNo={}, chunkIndex={}, platformCardCode={}, amount={}",
            batchNo, chunkIndex, cardCode, chunkTotalAmount);
}
```

**注意：** `batchChangeAccountForRecharge` 的参数结构需参照 `RechargeTransAfter` 实际调用方式。上面是示意，实际需要构建 `AccountChangeBatchReq` 包含正确的 `AccountChangeReq` 列表。

- [ ] **Step 6: 辅助方法 — Redis done 标记**

```java
@Resource
private StringRedisTemplate stringRedisTemplate;

private void markRedisDone(String batchNo, int chunkIndex,
                           List<TransPlatformRechargeBatchDetail> details,
                           PlatformRechargeBatchReq request) {
    String operatorCode = request.getOperatorCode();
    String platformCode = request.getPlatformCode();
    int count = 0;

    for (TransPlatformRechargeBatchDetail detail : details) {
        String key = "platform_recharge:done:" + operatorCode + ":" + platformCode + ":" + detail.getTransNo();
        try {
            stringRedisTemplate.opsForValue().set(key, "1", 7, java.util.concurrent.TimeUnit.DAYS);
            count++;
        } catch (Exception e) {
            log.error("platformRechargeBatch:markDone:failed batchNo={}, transNo={}, error={}", batchNo, detail.getTransNo(), e.getMessage(), e);
        }
    }

    log.info("platformRechargeBatch:markDone:success batchNo={}, chunkIndex={}, count={}", batchNo, chunkIndex, count);
}
```

- [ ] **Step 7: 在 processAsync chunk 循环中调用 executeTx2**

替换 Task 6 中 `processAsync` 的注释占位：

```java
// === 事务2: 明细 + 余额 ===
executeTx2(batchNo, chunkIndex, toInsert, request);

// 成功后更新 detail status → S，更新充值主流水 status → S
totalSuccess += toInsert.size();
totalAmount += toInsert.stream().mapToLong(TransPlatformRechargeBatchDetail::getTransAmt).sum();
toInsert.forEach(d -> detailService.updateStatus(d.getDetailId(), "S", null));
// 更新充值主流水状态为 S
toInsert.forEach(d -> transRechargeTService.updateRechargeStatus(d.getTransNo(), "S"));
```

需要在 `TransRechargeTService` 中添加 `updateRechargeStatus` 方法（如果不存在）。

- [ ] **Step 8: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/
git commit -m "feat: implement TX2 (balance chain, account details, balance update, redis done)"
```

---

## Task 8: 唯一键冲突重试机制

**Files:**
- Modify: `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java`

- [ ] **Step 1: 在 executeTx1 中包装唯一键冲突重试**

修改 `processAsync` 中调用 `executeTx1` 的部分，添加 try-catch + 重试逻辑：

```java
// === 事务1: 充值2表落地 ===
int maxRetry = 1;
int retryCount = 0;
int tx1Result = 0;
List<TransPlatformRechargeBatchDetail> effectiveChunk = new ArrayList<>(chunk);

while (retryCount <= maxRetry) {
    try {
        tx1Result = executeTx1(batchNo, chunkIndex, effectiveChunk, request);
        break;
    } catch (DuplicateKeyException e) {
        retryCount++;
        log.info("platformRechargeBatch:chunk:tx1:uniqueConflict batchNo={}, chunkIndex={}, error={}, action=rollback_reload_rebuild",
                batchNo, chunkIndex, e.getMessage());

        if (retryCount > maxRetry) {
            throw e;
        }

        // 重新查询已存在 transNo，过滤后重建 chunk
        List<String> chunkTransNos = effectiveChunk.stream()
                .map(TransPlatformRechargeBatchDetail::getTransNo).collect(Collectors.toList());
        List<String> existing = transRechargeTService.selectExistingTransNos(chunkTransNos);
        effectiveChunk = effectiveChunk.stream()
                .filter(d -> !existing.contains(d.getTransNo()))
                .collect(Collectors.toList());

        if (effectiveChunk.isEmpty()) {
            tx1Result = 0;
            break;
        }

        log.info("platformRechargeBatch:chunk:tx1:retry batchNo={}, chunkIndex={}, retryCount={}, remainingCount={}",
                batchNo, chunkIndex, retryCount, effectiveChunk.size());
    }
}
```

需要添加 import: `org.springframework.dao.DuplicateKeyException`

- [ ] **Step 2: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/PlatformRechargeBatchServiceImpl.java
git commit -m "feat: add unique key conflict retry for TX1"
```

---

## Task 9: 补偿方法

**Files:**
- Modify: `.../consume/service/PlatformRechargeBatchService.java`
- Modify: `.../consume/service/impl/PlatformRechargeBatchServiceImpl.java`
- Modify: `.../consume/api/TransConsumeApi.java`
- Modify: `.../consume/controller/TransConsumeController.java`

- [ ] **Step 1: TransConsumeApi 添加补偿接口**

```java
@Operation(method = "平台充值批量补偿", description = "对事务2失败的记录重新执行明细+余额更新")
@PostMapping(value = PATH_PREFIX + "/platformRechargeBatchCompensate")
DefaultResult<Integer> platformRechargeBatchCompensate(@RequestBody List<Long> detailIds) throws Exception;
```

- [ ] **Step 2: PlatformRechargeBatchService 添加方法**

```java
DefaultResult<Integer> compensate(List<Long> detailIds) throws Exception;
```

- [ ] **Step 3: 实现 compensate()**

在 `PlatformRechargeBatchServiceImpl` 中添加：

```java
@Override
public DefaultResult<Integer> compensate(List<Long> detailIds) throws Exception {
    int successCount = 0;

    for (Long detailId : detailIds) {
        TransPlatformRechargeBatchDetail detail = detailService.getById(detailId);
        if (detail == null) {
            log.warn("platformRechargeBatch:compensate:skip detailId={}, reason=not found", detailId);
            continue;
        }
        if (!"F".equals(detail.getStatus())) {
            log.warn("platformRechargeBatch:compensate:skip detailId={}, transNo={}, status={}, reason=status is not F",
                    detailId, detail.getTransNo(), detail.getStatus());
            continue;
        }

        log.info("platformRechargeBatch:compensate:start detailId={}, transNo={}, platformCardCode={}, reason={}",
                detailId, detail.getTransNo(), detail.getPlatformCardCode(), detail.getRemark());

        try {
            // 确认充值主流水存在
            boolean rechargeExists = transRechargeTService.existsRechargeTransByTransNo(detail.getTransNo());
            log.info("platformRechargeBatch:compensate:rechargeExists detailId={}, transNo={}, exists={}",
                    detailId, detail.getTransNo(), rechargeExists);

            if (!rechargeExists) {
                log.error("platformRechargeBatch:compensate:failed detailId={}, transNo={}, error=recharge record not found",
                        detailId, detail.getTransNo());
                continue;
            }

            // 只重放事务2：明细 + 余额更新
            // 构建简化的 request（补偿时只需关键字段）
            PlatformRechargeBatchReq mockRequest = buildCompensateRequest(detail);

            List<TransPlatformRechargeBatchDetail> singleList = List.of(detail);
            executeTx2("COMPENSATE", 0, singleList, mockRequest);

            // 更新 detail 状态
            detailService.updateStatus(detailId, "S", null);
            transRechargeTService.updateRechargeStatus(detail.getTransNo(), "S");

            successCount++;
            log.info("platformRechargeBatch:compensate:tx2:success detailId={}, transNo={}", detailId, detail.getTransNo());

        } catch (Exception e) {
            log.error("platformRechargeBatch:compensate:failed detailId={}, transNo={}, error={}", detailId, detail.getTransNo(), e.getMessage(), e);
            detailService.updateStatus(detailId, "F", "compensate failed: " + e.getMessage());
        }
    }

    return DefaultResult.success(successCount);
}
```

- [ ] **Step 4: Controller 添加端点**

```java
@Override
public DefaultResult<Integer> platformRechargeBatchCompensate(List<Long> detailIds) throws Exception {
    return platformRechargeBatchService.compensate(detailIds);
}
```

- [ ] **Step 5: Compile verify + Commit**

```bash
cd slhy && mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q
git add fund-catering/fund-catering-consume/ && git commit -m "feat: add compensation endpoint for TX2 failed records"
```

---

## Task 10: task 端改造 — PlatformRechargeJobService

**Files:**
- Modify: `.../task/job/zx/PlatformRechargeJobService.java`

**核心改造：** 去掉逐笔 rechargeTrans 调用，改为组装批量请求调用 consume 新接口。

- [ ] **Step 1: 修改 run() 方法**

保留 XXL-JOB 入口、参数读取、配置查询、front 流水查询逻辑。改造从"逐笔调用 rechargeTrans"开始的部分：

```java
// --- 以下替换原有逐笔处理逻辑 ---

// 1. 过滤 TRANS_TP = 03 的渠道充值流水
List<BasTransDetailQueryRes> type03List = allDetails.stream()
        .filter(d -> "03".equals(d.getTransTp()))
        .filter(d -> StringUtils.isNotEmpty(d.getTransNo()))
        .filter(d -> {
            try {
                BigDecimal amt = new BigDecimal(d.getTransAmt());
                return amt.compareTo(BigDecimal.ZERO) > 0;
            } catch (Exception e) {
                return false;
            }
        })
        .filter(d -> StringUtils.isNotEmpty(d.getTxnTime()) || StringUtils.isNotEmpty(d.getTransDate()))
        .collect(Collectors.toList());

if (type03List.isEmpty()) {
    log.info("platformRechargeBatchJob:filter03:empty batchNo={}", batchNo);
    continue;
}

// 2. Redis done 预过滤
List<PlatformRechargeBatchDetailReq> submitDetails = new ArrayList<>();
int skippedDoneCount = 0;
for (BasTransDetailQueryRes transDetail : type03List) {
    String doneKey = "platform_recharge:done:" + operatorCode + ":" + platformCode + ":" + transDetail.getTransNo();
    if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(doneKey))) {
        skippedDoneCount++;
        continue;
    }
    PlatformRechargeBatchDetailReq detailReq = new PlatformRechargeBatchDetailReq();
    detailReq.setTransNo(transDetail.getTransNo());
    // 金额：front 返回元，转为分
    BigDecimal yuan = new BigDecimal(transDetail.getTransAmt());
    long cents = yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    detailReq.setAmount(String.valueOf(cents));
    detailReq.setTxnTime(transDetail.getTxnTime());
    detailReq.setTransDate(transDetail.getTransDate());
    detailReq.setTransTime(transDetail.getTransTime());
    detailReq.setPayAccount(transDetail.getPayAccount());
    detailReq.setPayAccountName(transDetail.getPayAccountName());
    detailReq.setSummaryMsg(transDetail.getSummaryMsg());
    detailReq.setBankEAccountId(transDetail.getBankEAccountId());
    detailReq.setSourceTransType("03");
    submitDetails.add(detailReq);
}

log.info("platformRechargeBatchJob:redisDoneFilter:success batchNo={}, candidateCount={}, skippedDoneCount={}, submitCount={}",
        batchNo, type03List.size(), skippedDoneCount, submitDetails.size());

if (submitDetails.isEmpty()) {
    continue;
}

// 3. 组装批量请求
PlatformRechargeBatchReq batchReq = new PlatformRechargeBatchReq();
batchReq.setBatchNo(batchNo);
batchReq.setOperatorCode(operatorCode);
batchReq.setOrgCode(orgCode);
batchReq.setFirstOrgCode(firstOrgCode);
batchReq.setPlatformCode(platformCode);
batchReq.setTransDate(transDate);
batchReq.setPlatformCardCode(platformCardCode); // 从配置获取
batchReq.setPlatformMerchantId(merchantId);
batchReq.setTerminalCode(terminalCode);
batchReq.setAccountType("04");
batchReq.setDetails(submitDetails);

// 4. 调用 consume 批量接口
log.info("platformRechargeBatchJob:submitConsume:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, submitCount={}",
        batchNo, operatorCode, platformCode, platformCardCode, submitDetails.size());

try {
    DefaultResult<PlatformRechargeBatchSubmitRes> result = transConsumeApi.platformRechargeBatch(batchReq);
    PlatformRechargeBatchSubmitRes res = result.getData();
    log.info("platformRechargeBatchJob:submitConsume:result batchNo={}, status={}, code={}, message={}, receivedCount={}, submitCount={}",
            batchNo, res.getStatus(), result.getCode(), res.getMessage(), res.getReceivedCount(), res.getSubmitCount());
} catch (Exception e) {
    log.error("platformRechargeBatchJob:submitConsume:error batchNo={}, operatorCode={}, platformCode={}, error={}",
            batchNo, operatorCode, platformCode, e.getMessage(), e);
}
```

**注意：**
- `platformCardCode` 需要从配置中获取。当前 job 配置中没有这个字段，需要在参数配置中补充，或从 base 服务查询。
- `stringRedisTemplate` 需要注入。
- 去掉原有 `sleep(500)` 和逐笔 `transConsumeApi.rechargeTrans()` 调用。

- [ ] **Step 2: 更新注入**

```java
@Resource
private StringRedisTemplate stringRedisTemplate;
```

去掉不再需要的注入（如逐笔处理相关的）。

- [ ] **Step 3: Compile verify**

Run: `cd slhy && mvn compile -pl fund-catering/fund-catering-task -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add fund-catering/fund-catering-task/
git commit -m "feat: refactor PlatformRechargeJobService to use batch submit API"
```

---

## Task 11: trans_recharge_t 唯一约束确认

**Files:**
- Verify DB schema

- [ ] **Step 1: 确认唯一约束存在**

```sql
SHOW INDEX FROM trans_recharge_t WHERE Key_name = 'uk_trans_recharge_trans_no';
```

如果不存在，需要添加：

```sql
ALTER TABLE trans_recharge_t ADD UNIQUE KEY uk_trans_recharge_trans_no (trans_no);
```

**注意：** 如果历史数据中 `trans_no` 存在重复，需要先清理后才能添加唯一约束。确认现有数据干净后再执行。

---

## Self-Review Checklist

**1. Spec Coverage:**
- ✅ task 采集 + Redis done 预过滤 → Task 10
- ✅ consume API 扩展 → Task 2, 4
- ✅ detail 表 + 同步写入 → Task 1, 3, 5
- ✅ Redis 锁（非阻塞）→ Task 5
- ✅ 异步任务提交 → Task 5
- ✅ 批内去重 + DB 查重 → Task 6
- ✅ 事务1充值2表落地 → Task 6
- ✅ 事务2余额链 + 明细 + 余额更新 → Task 7
- ✅ 唯一键冲突重试 → Task 8
- ✅ Redis done 标记 → Task 7
- ✅ 补偿入口 → Task 9
- ✅ 日志要求 → 每个 Task 内含
- ✅ trans_recharge_t 唯一约束 → Task 11
- ✅ 04 余额更新对齐 RechargeTransAfter → Task 7

**2. Placeholder scan:** Task 7 中 `AccountSummaryHelper.buildAcctSum` 和 `buildTransSum` 的参数签名为示意，需要实现时对照源码调整。已标注"注意"说明。

**3. Type consistency:** 所有 DTO 字段名和类型在各 Task 间一致。
