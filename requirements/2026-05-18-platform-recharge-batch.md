# 平台 03 渠道充值批量高并发上账需求规格

更新时间：2026-05-25

状态：已完成

适用代码仓：`/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy`

主要模块：

- `fund-catering-task`
- `fund-catering-consume`
- `fund-catering-base`

## 当前验收结论

平台 03 渠道充值批量实收已按当前业务口径完成。

当前确认口径：

- 数据库表和唯一索引已处理。
- Redis done 继续沿用单流水 key：`platform_recharge:done:{operatorCode}:{platformCode}:{transNo}`。
- 5000 笔及更大批量的分页提交后续再优化，当前先保留一次提交当前查询结果。
- 用户本地编译已通过。

已落地链路：

1. `fund-catering-task` 采集银行平台流水，过滤 `TRANS_TP = 03`。
2. task 按 Redis done 预过滤已完成流水。
3. task 组装 `PlatformRechargeBatchReq` 并调用 `TransConsumeApi.platformRechargeBatch`。
4. `fund-catering-consume` 入口抢平台卡锁，抢不到直接返回 `SKIPPED_LOCKED`。
5. consume 同步写入 `trans_platform_recharge_batch_detail`，状态为 `I`。
6. consume 异步按 `platform.recharge.batch.chunkSize` 分片处理。
7. TX1 写入 `trans_recharge_t` 和 `trans_recharge_sub_t`，状态先为 `P`。
8. TX2 写账户变动明细并更新平台卡 04 账户余额。
9. 成功后更新明细状态 `S`、充值状态 `S`、写 Redis done。
10. 失败明细状态 `F`，可通过补偿接口重试。

主要源码入口：

- task：`fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/zx/PlatformRechargeJobService.java`
- API：`fund-catering/fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/TransConsumeApi.java`
- controller：`fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/controller/TransConsumeController.java`
- 批量服务：`fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/PlatformRechargeBatchServiceImpl.java`
- 批量明细：`fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/domain/TransPlatformRechargeBatchDetail.java`

后续非阻塞优化：

- task 查询结果按分页或固定大小拆分提交，降低单次 Feign 请求体和 SQL `IN` 压力。
- 根据压测结果调整 `platform.recharge.batch.chunkSize`。
- 补充运维查询入口，查看 `I/P/S/F` 明细状态和补偿结果。

待处理问题：

- 平台卡锁当前存在跨线程释放风险：HTTP 线程加 Redisson 锁，异步线程释放时 `isHeldByCurrentThread()` 为 false，业务日志可能误报“释放卡锁成功”，实际锁仍需等 TTL 过期。详见 `../bugs/2026-05-26-platform-recharge-card-lock-async-release.md`。

## 一、背景

当前平台实收充值入口在 `fund-catering-task`：

- `PlatformRechargeJobService`
- XXL-JOB 名称：`platformRechargeJobService`

现有逻辑会按平台流水逐笔调用：

- `TransConsumeApi.rechargeTrans`
- `TransRechargeServiceImpl.rechargeTrans`
- LiteFlow 充值链路

该机制对普通单笔充值是合理的，但对“03 渠道充值到平台账户”的场景存在明显性能瓶颈。

本次需求确认：

- `03` 类型是渠道充值。
- 当前 `03` 渠道充值只会充值到一个平台账户。
- 不需要按每条流水查企业卡并走完整充值流程。
- 不再逐笔调用原 `rechargeTrans`。
- 需要参考批量划付的高并发处理方式。
- `task` 负责采集和初步过滤。
- `consume` 负责真正批量上账。
- 平台上账卡需要 Redis 锁控制。
- 抢不到锁直接跳过，不等待。
- 账户变动明细必须连续，不能断链。
- 新对象和新流程必须有完整日志，不能省略。

## 二、现有问题

### 2.1 热点平台账户锁竞争

原充值流程会按 `cardCode` 做 Redis 锁控制。平台 03 渠道充值全部落到同一个平台卡时，这张卡会成为热点账户。

如果继续走单笔充值流程，会出现：

- 每笔都抢同一张平台卡锁。
- 每笔都查询账户、企业、商户、业务配置。
- 每笔都执行 LiteFlow。
- 每笔都做账户余额更新。
- 每笔都生成账户变动明细。
- 当前 task 中还有逐笔 `sleep(500)`。

结果是吞吐量低、任务执行时间长、锁过期风险高。

### 2.2 不适合完整充值校验

03 渠道充值当前业务已明确只进入固定平台账户，不需要完整充值流程中的大部分校验。

可去掉：

- 逐笔企业卡定位。
- 逐笔商户/业务完整校验。
- 逐笔 LiteFlow 流程编排。
- 逐笔账户 CAS 更新。
- 逐笔卡锁。
- 逐笔等待。

必须保留：

- 流水号重复过滤。
- 平台账户存在性校验。
- 金额合法性校验。
- 03 类型校验。
- 充值主/子流水入库。
- 账户变动明细入库。
- 平台卡 04 账户余额更新。
- 数据库唯一约束兜底。
- 完整日志和处理结果可追踪。

## 三、目标

建设一条“平台 03 渠道充值批量高并发上账”专用链路。

目标包括：

- `task` 只负责采集 03 渠道充值流水和 Redis done 预过滤。
- `consume` 新增内部批量上账接口。
- `consume` 一进入接口就抢平台卡 Redis 锁，抢不到直接返回跳过，不等待。
- `consume` 抢到锁后异步处理批量上账。
- 重复充值流水直接跳过，不做对外接口式幂等返回。
- 平台卡锁内读取当前账户余额。
- 过滤后剩余流水按顺序在内存中完整计算账户变动明细余额链。
- 账户变动明细批量写入。
- 平台卡 04 账户余额一次性更新。
- 成功后由 `consume` 写 Redis done 流水号，过期时间 7 天。
- 数据库 `trans_no` 唯一约束作为最终防重底线。

## 四、非目标

本需求不做以下内容：

- 不改造普通充值接口 `rechargeTrans`。
- 不复用 LiteFlow 充值链路。
- 不建设对外幂等返回语义。
- 不处理非 `03` 类型平台流水。
- 不处理多平台卡入账场景。
- 不做付款方银行卡到企业卡的匹配。
- 不改变现有普通用户充值、通知充值、特殊账户充值等链路。

## 五、总体架构

### 5.1 task 职责

`fund-catering-task` 只做数据采集和轻量过滤：

1. 读取 XXL-JOB 参数。
2. 查询 `platformRecharge` 参数配置。
3. 调用 front 查询平台流水。
4. 只保留 `TRANS_TP = 03` 的渠道充值流水。
5. 校验基础字段：
   - `transNo` 非空。
   - 金额非空且大于 0。
   - 交易时间非空。
6. 使用 Redis done key 预过滤已完成流水。
7. 组装批量请求调用 `consume` 新增内部接口。
8. 记录本次提交结果，不等待 consume 异步处理完成。

task 不再：

- 查询每条流水的企业卡信息。
- 组装单笔 `ConsumeRechargeRequest`。
- 调用 `transConsumeApi.rechargeTrans`。
- 按笔 sleep。
- 写最终 done 标记。

### 5.2 consume 职责

`fund-catering-consume` 新增平台批量充值上账服务。

consume 接口进入后必须先抢平台卡锁：

1. 解析平台上账卡 `platformCardCode`。
2. `SET NX EX` 抢 Redis 卡锁。
3. 抢不到锁，立即返回 `SKIPPED_LOCKED`，不等待、不重试。
4. 抢到锁，提交异步处理任务。
5. 接口立即返回 `ACCEPTED`。
6. 异步线程在锁保护下完成批量上账。
7. 异步处理完成后释放平台卡锁。

平台卡锁必须覆盖：

- 读取平台卡当前余额。
- 计算账户变动明细余额链。
- 批量插入充值流水。
- 批量插入账户变动明细。
- 一次性更新平台卡 04 账户余额。

## 六、推荐新增对象

### 6.1 consume API

建议在 `TransConsumeApi` 新增内部接口：

```java
@PostMapping(value = PATH_PREFIX + "/platformRechargeBatch")
DefaultResult<PlatformRechargeBatchSubmitRes> platformRechargeBatch(@RequestBody PlatformRechargeBatchReq request);
```

也可以单独新增 `PlatformRechargeBatchApi`，避免继续扩大 `TransConsumeApi`。如新增独立 API，建议路径仍保持在 consume 服务内：

```text
/consume/platformRecharge/batchSubmit
```

推荐返回为“提交结果”，不是最终入账结果。

### 6.2 请求对象

建议新增：

```java
PlatformRechargeBatchReq
PlatformRechargeBatchDetailReq
```

`PlatformRechargeBatchReq` 字段建议：

```java
private String batchNo;
private String operatorCode;
private String orgCode;
private String firstOrgCode;
private String platformCode;
private String transDate;
private String platformCardCode;
private String platformMerchantId;
private String terminalCode;
private String accountType; // 固定 04
private List<PlatformRechargeBatchDetailReq> details;
```

`PlatformRechargeBatchDetailReq` 字段建议：

```java
private String transNo;
private String amount; // 单位：分
private String txnTime;
private String transDate;
private String transTime;
private String payAccount;
private String payAccountName;
private String summaryMsg;
private String bankEAccountId;
private String sourceTransType; // 03
private String rawJson; // 可选，便于排查
```

### 6.3 返回对象

建议新增：

```java
PlatformRechargeBatchSubmitRes
```

字段建议：

```java
private String batchNo;
private String status; // ACCEPTED, SKIPPED_LOCKED, EMPTY, INVALID, REJECTED
private String message;
private int receivedCount;
private int submitCount;
private int redisDoneSkippedCount;
```

异步处理内部日志必须记录最终处理统计，不依赖同步返回。

### 6.4 consume 服务对象

建议新增：

```java
PlatformRechargeBatchService
PlatformRechargeBatchServiceImpl
```

建议内部方法：

```java
DefaultResult<PlatformRechargeBatchSubmitRes> submit(PlatformRechargeBatchReq request);
void processAsync(PlatformRechargeBatchContext context);
PlatformRechargeBatchFilterResult filterDetails(...);
PlatformRechargeAccountSnapshot loadAccountSnapshot(...);
PlatformRechargeBuildResult buildRecordsInMemory(...);
void saveChunkInTransaction(...);
void markDoneAfterCommit(...);
```

### 6.5 task 服务对象

可以在 `PlatformRechargeJobService` 中改造，也可以新增组件拆分：

```java
PlatformRechargeCollectService
PlatformRechargeBatchSubmitClient
```

建议避免把新逻辑全部塞回 job 类中。

## 七、Redis Key 规范

### 7.1 平台卡锁

```text
platform_recharge:card_lock:{platformCardCode}
```

规则：

- consume 入口第一步抢锁。
- 使用 `SET NX EX`。
- 抢不到直接返回 `SKIPPED_LOCKED`。
- 不 wait。
- 不自旋。
- 不阻塞 Feign/XXL-JOB 线程。
- TTL 建议 30 分钟。
- 如果单批可能超过 TTL，需要实现续期，或强制 chunk 控制在 TTL 内。

### 7.2 已完成流水

```text
platform_recharge:done:{operatorCode}:{platformCode}:{transNo}
```

规则：

- TTL：7 天。
- task 读取该 key 做预过滤。
- consume 成功提交数据库事务后写该 key。
- 只有真正成功入账的流水才能写 done。
- 失败流水不能写 done。
- 数据库已存在的流水可以视为已完成，consume 可补写 done。

### 7.3 处理状态流水

如需要防同一批之外重复提交，可选：

```text
platform_recharge:processing:{operatorCode}:{platformCode}:{transNo}
```

本需求中平台卡锁已经串行化平台账户上账，`processing` key 不是必须。

如果实现 `processing` key：

- 只能作为性能优化。
- 不能替代数据库查重。
- 异常失败必须释放或等 TTL 过期。

## 八、重复流水过滤规则

本需求不定义“幂等返回”，只定义“重复流水跳过”。

过滤顺序：

1. task 使用 Redis done 预过滤。
2. consume 批内按 `transNo` 去重。
3. consume 批量查询 `trans_recharge_t` 已存在的 `transNo`。
4. 数据库已存在的流水直接跳过，不参与余额链计算。
5. 剩余流水进入本批上账。
6. 入库时依赖 `trans_recharge_t.trans_no` 唯一索引兜底。

重要要求：

- 重复流水必须在计算账户变动明细前过滤掉。
- 被跳过的流水不能参与本批总金额。
- 被跳过的流水不能参与期初/期末余额链。
- 如果批量插入时出现唯一键冲突，当前 chunk 必须回滚。
- 回滚后重新查询数据库已存在流水，重新过滤，重新读取当前余额或在锁内基于当前处理状态重算余额链。
- 禁止在唯一键冲突后“跳过单笔但沿用旧余额链继续提交”。

## 九、账户变动明细连续性要求

账户变动明细必须在内存中完整算好，再批量入库。

### 9.1 排序规则

最终参与上账的流水按以下规则排序：

1. `txnTime` 升序。
2. `transNo` 升序。

如果 `txnTime` 为空，该流水应判定为非法，不进入上账。

### 9.2 余额链计算

在平台卡锁内查询当前平台卡 04 子账户：

- `balance`
- `realBalance`
- `withdrawBalance`
- `waitReleaseBalance`
- `frozenAmt`
- 当前总账户金额，即所有子账户 `realBalance` 汇总。

对每一笔入金流水在内存中计算：

第 1 笔：

```text
orgAmt = 当前 balance
balance = orgAmt + amount
orgRealAmt = 当前 realBalance
realBalance = orgRealAmt + amount
```

第 2 笔：

```text
orgAmt = 第 1 笔 balance
balance = orgAmt + amount
orgRealAmt = 第 1 笔 realBalance
realBalance = orgRealAmt + amount
```

后续依次衔接。

总账户类明细也必须同样以前一笔期末作为后一笔期初。

### 9.3 批量入库对象

一批流水需要在内存中组装：

- `trans_recharge_t`
- `trans_recharge_sub_t`
- `trans_acct_change_detail_t`
- `trans_acct_sum_change_detail_t`
- `trans_acct_act_sum_change_detail_t`
- `trans_sum_change_detail_t`

其中账户变动相关明细建议复用现有批量能力：

- `AccountChangeDetailBatchReq`
- `TransAccountApi.batchChangeAccountDetail`

账户余额更新建议复用：

- `BaseAccountServiceApi.batchChangeAccountForRecharge`

或在确认无需膨胀金创建时，使用更轻量的账户更新方式。但必须保证与现有账户 MAC/CAS 机制兼容。

### 9.4 平台卡余额一次性更新

本批成功流水总额：

```text
successAmount = sum(details.amount)
```

平台卡 04 账户一次性增加：

- `balance += successAmount`
- `realBalance += successAmount`
- `withdrawBalance += successAmount`

具体字段以现有充值入账语义为准。如现有 04 充值不增加 `withdrawBalance`，以现有充值口径为准，但必须在 spec 实现前核对 `RechargeTransAfter`。

## 十、事务边界

推荐按 chunk 处理。

### 10.1 chunk 大小

建议：

- 默认每个 chunk 500 笔。
- 可配置为 200、500、1000。
- 首期建议 500，便于控制事务大小和锁持有时间。

### 10.2 单 chunk 事务

每个 chunk 必须在同一个事务中完成：

1. 批量插入充值主流水。
2. 批量插入充值子流水。
3. 批量插入账户变动明细。
4. 一次性更新平台卡 04 账户余额。

以上任意一步失败，当前 chunk 回滚。

### 10.3 chunk 之间的关系

平台卡锁覆盖整个批次，chunk 顺序执行。

原因：

- 平台卡只有一个。
- 账户变动明细必须全局连续。
- 并发 chunk 会破坏期初/期末衔接。

如果未来要并发，只能在“不同平台卡”之间并发。本需求当前只有一个平台账户，不做同卡并发。

## 十一、日志要求

本需求特别要求：新对象日志必须完整，不能省。

所有日志必须包含可追踪字段：

- `batchNo`
- `operatorCode`
- `platformCode`
- `platformCardCode`
- `transDate`
- `chunkIndex`
- `transNo`
- `count`
- `amount`
- `costMs`

### 11.1 task 日志

task 开始：

```text
platformRechargeBatchJob:start batchNo={}, transDate={}, jobParam={}
```

读取配置：

```text
platformRechargeBatchJob:loadConfig:start batchNo={}, operatorCodes={}
platformRechargeBatchJob:loadConfig:success batchNo={}, configCount={}
platformRechargeBatchJob:loadConfig:empty batchNo={}, paramName={}
```

查询平台流水：

```text
platformRechargeBatchJob:queryPlatformTrans:start batchNo={}, operatorCode={}, platformCode={}, transDate={}, pageNum={}
platformRechargeBatchJob:queryPlatformTrans:success batchNo={}, operatorCode={}, platformCode={}, pageNum={}, total={}, totalPage={}, bizCount={}, costMs={}
platformRechargeBatchJob:queryPlatformTrans:failed batchNo={}, operatorCode={}, platformCode={}, pageNum={}, code={}, message={}
```

过滤 03 流水：

```text
platformRechargeBatchJob:filter03:start batchNo={}, sourceCount={}
platformRechargeBatchJob:filter03:success batchNo={}, sourceCount={}, type03Count={}, invalidCount={}
```

Redis done 预过滤：

```text
platformRechargeBatchJob:redisDoneFilter:start batchNo={}, candidateCount={}
platformRechargeBatchJob:redisDoneFilter:success batchNo={}, candidateCount={}, skippedDoneCount={}, submitCount={}, costMs={}
```

提交 consume：

```text
platformRechargeBatchJob:submitConsume:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, submitCount={}
platformRechargeBatchJob:submitConsume:result batchNo={}, status={}, code={}, message={}, receivedCount={}, submitCount={}
platformRechargeBatchJob:submitConsume:error batchNo={}, operatorCode={}, platformCode={}, error={}
```

task 结束：

```text
platformRechargeBatchJob:end batchNo={}, totalSourceCount={}, totalSubmitCount={}, totalSkippedDoneCount={}, costMs={}
```

### 11.2 consume 提交接口日志

入口：

```text
platformRechargeBatch:submit:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, receivedCount={}
```

参数校验：

```text
platformRechargeBatch:submit:validateFailed batchNo={}, reason={}
platformRechargeBatch:submit:validateSuccess batchNo={}, receivedCount={}
```

抢平台卡锁：

```text
platformRechargeBatch:lock:start batchNo={}, lockKey={}, platformCardCode={}
platformRechargeBatch:lock:success batchNo={}, lockKey={}, ttlSeconds={}
platformRechargeBatch:lock:skipped batchNo={}, lockKey={}, platformCardCode={}, message=card locked
```

异步提交：

```text
platformRechargeBatch:asyncSubmit:success batchNo={}, platformCardCode={}, receivedCount={}
platformRechargeBatch:asyncSubmit:failed batchNo={}, platformCardCode={}, error={}
```

同步返回：

```text
platformRechargeBatch:submit:end batchNo={}, status={}, message={}, costMs={}
```

### 11.3 consume 异步处理日志

异步开始：

```text
platformRechargeBatch:process:start batchNo={}, operatorCode={}, platformCode={}, platformCardCode={}, receivedCount={}
```

批内去重：

```text
platformRechargeBatch:deduplicate:start batchNo={}, receivedCount={}
platformRechargeBatch:deduplicate:success batchNo={}, receivedCount={}, duplicateInBatchCount={}, uniqueCount={}
```

数据库查重：

```text
platformRechargeBatch:dbExistsFilter:start batchNo={}, uniqueCount={}
platformRechargeBatch:dbExistsFilter:success batchNo={}, uniqueCount={}, dbExistsCount={}, pendingCount={}, costMs={}
```

账户快照：

```text
platformRechargeBatch:accountSnapshot:start batchNo={}, platformCardCode={}
platformRechargeBatch:accountSnapshot:success batchNo={}, platformCardCode={}, subAccountId={}, balance={}, realBalance={}, withdrawBalance={}, totalRealBalance={}
platformRechargeBatch:accountSnapshot:failed batchNo={}, platformCardCode={}, reason={}
```

排序和余额链：

```text
platformRechargeBatch:buildChain:start batchNo={}, pendingCount={}
platformRechargeBatch:buildChain:item batchNo={}, index={}, transNo={}, amount={}, orgAmt={}, balance={}, orgRealAmt={}, realBalance={}
platformRechargeBatch:buildChain:success batchNo={}, pendingCount={}, totalAmount={}, firstTransNo={}, lastTransNo={}
platformRechargeBatch:buildChain:failed batchNo={}, transNo={}, reason={}
```

注意：`buildChain:item` 会很多。生产环境如担心日志量，可以用 `debug`，但首期排查建议保留 `info` 或按配置开关控制。用户明确要求“完整日志”，默认不要省略关键链路日志。

chunk 处理：

```text
platformRechargeBatch:chunk:start batchNo={}, chunkIndex={}, chunkSize={}, firstTransNo={}, lastTransNo={}
platformRechargeBatch:chunk:insertRecharge:start batchNo={}, chunkIndex={}, masterCount={}, subCount={}
platformRechargeBatch:chunk:insertRecharge:success batchNo={}, chunkIndex={}, masterCount={}, subCount={}, costMs={}
platformRechargeBatch:chunk:insertAccountDetail:start batchNo={}, chunkIndex={}, acctDetailCount={}, acctSumCount={}, acctActSumCount={}, transSumCount={}
platformRechargeBatch:chunk:insertAccountDetail:success batchNo={}, chunkIndex={}, totalDetailCount={}, costMs={}
platformRechargeBatch:chunk:updateAccount:start batchNo={}, chunkIndex={}, platformCardCode={}, amount={}
platformRechargeBatch:chunk:updateAccount:success batchNo={}, chunkIndex={}, platformCardCode={}, amount={}, costMs={}
platformRechargeBatch:chunk:commit:success batchNo={}, chunkIndex={}, successCount={}, amount={}
platformRechargeBatch:chunk:rollback batchNo={}, chunkIndex={}, reason={}, error={}
```

唯一键冲突重算：

```text
platformRechargeBatch:uniqueConflict batchNo={}, chunkIndex={}, message={}, willReloadExists=true
platformRechargeBatch:recalculateAfterConflict:start batchNo={}, chunkIndex={}
platformRechargeBatch:recalculateAfterConflict:success batchNo={}, chunkIndex={}, pendingCount={}
```

Redis done：

```text
platformRechargeBatch:markDone:start batchNo={}, count={}, ttlDays=7
platformRechargeBatch:markDone:item batchNo={}, transNo={}, redisKey={}
platformRechargeBatch:markDone:success batchNo={}, count={}, costMs={}
platformRechargeBatch:markDone:failed batchNo={}, transNo={}, error={}
```

释放锁：

```text
platformRechargeBatch:unlock:start batchNo={}, lockKey={}
platformRechargeBatch:unlock:success batchNo={}, lockKey={}
platformRechargeBatch:unlock:failed batchNo={}, lockKey={}, error={}
```

异步结束：

```text
platformRechargeBatch:process:end batchNo={}, status={}, receivedCount={}, pendingCount={}, successCount={}, skippedCount={}, failedCount={}, totalAmount={}, costMs={}
```

### 11.4 错误日志要求

所有异常日志必须打完整异常堆栈：

```java
log.error("... error", e);
```

禁止只打：

```java
log.error("error: {}", e.getMessage());
```

## 十二、数据库要求

### 12.1 唯一约束

必须确认或新增：

```text
trans_recharge_t.trans_no 唯一
```

建议约束名：

```text
uk_trans_recharge_trans_no
```

如果历史数据不允许单字段唯一，则至少需要明确组合唯一策略。但本需求推荐单字段 `trans_no` 唯一，因为现有充值流水号本身承担交易唯一号语义。

### 12.2 批量查询

consume 需要支持按 `transNoList` 批量查询已存在充值流水。

建议新增 service/mapper 方法：

```java
List<String> selectExistingTransNos(List<String> transNos);
```

### 12.3 批量插入

建议新增或复用 MyBatis Plus 批量保存能力：

- `TransRechargeTService.saveBatch`
- `TransRechargeSubTService.saveBatch`
- 账户变动明细使用现有批量接口优先。

如现有批量接口内部仍逐条 save，需要评估性能，但首期可以先复用，后续再优化 mapper batch insert。

## 十三、金额规则

平台流水金额需要统一转为分。

当前旧 job 使用：

```java
new BigDecimal(transAmt).multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).longValue()
```

新实现建议改为明确规则：

```java
BigDecimal yuan = new BigDecimal(transAmt);
long cents = yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
```

要求：

- 金额必须大于 0。
- 不允许超过两位小数。
- 非法金额记录失败，不进入上账。
- 禁止静默截断。

如果平台返回本来就是分，则不要再乘 100。开发前必须根据 front 返回字段 `TRANS_AMT` 的实际单位确认。

## 十四、异步处理要求

task 调用 consume 后不等待最终处理完成。

consume 提交接口返回语义：

- `ACCEPTED`：抢到平台卡锁，已提交异步处理。
- `SKIPPED_LOCKED`：平台卡正在处理，本次跳过。
- `EMPTY`：没有可提交明细。
- `INVALID`：参数错误。
- `REJECTED`：线程池拒绝或系统状态不允许提交。

如果异步线程池提交失败：

- 必须释放平台卡锁。
- 返回 `REJECTED`。
- 打完整错误日志。

## 十五、验收标准

### 15.1 功能验收

- task 能采集 03 渠道充值流水。
- task 能通过 Redis done 跳过已完成流水。
- task 能异步提交 consume 批量接口。
- consume 抢不到平台卡锁时立即返回 `SKIPPED_LOCKED`。
- consume 抢到锁后异步处理。
- 重复 `transNo` 不参与上账。
- 数据库已存在 `transNo` 不参与上账。
- 成功流水写入充值主/子表。
- 成功流水写入账户变动相关表。
- 平台卡 04 账户余额按本批成功总金额增加。
- 成功后 Redis done key 写入，TTL 7 天。

### 15.2 连续性验收

给定平台卡初始余额 10000 分，三笔入金：

- A：100 分
- B：200 分
- C：300 分

账户变动明细必须为：

```text
A orgAmt=10000 balance=10100
B orgAmt=10100 balance=10300
C orgAmt=10300 balance=10600
```

如果 B 是重复流水被过滤，则必须为：

```text
A orgAmt=10000 balance=10100
C orgAmt=10100 balance=10400
```

### 15.3 并发验收

- 同一平台卡并发提交两批，只有一批抢到锁。
- 未抢到锁的请求立即返回，不等待。
- 抢到锁的批次处理期间，账户变动明细连续。
- 下一轮任务重新扫描时，已成功流水被 Redis done 或 DB 已存在过滤。

### 15.4 异常验收

- 批量插入充值流水失败，chunk 回滚。
- 账户变动明细插入失败，chunk 回滚。
- 账户余额更新失败，chunk 回滚。
- 唯一键冲突时，chunk 回滚，重新查重并重算余额链。
- Redis done 写入失败不影响数据库已成功事实，但必须打错误日志；下次由 DB 查重过滤。

### 15.5 日志验收

必须能通过 `batchNo` 追踪完整链路：

- task 查询了多少流水。
- task 过滤了多少 done。
- consume 是否抢到平台卡锁。
- consume 实际处理多少流水。
- consume 跳过多少重复流水。
- 每个 chunk 写入多少数据。
- 平台卡余额增加多少。
- Redis done 写入多少。
- 失败原因和异常堆栈。

## 十六、开发参考源码

### 16.1 当前入口

```text
fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/zx/PlatformRechargeJobService.java
```

### 16.2 原充值流程

```text
fund-catering-consume/fund-catering-consume-api/src/main/java/com/chinaums/erp/slhy/catering/consume/api/TransConsumeApi.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransRechargeServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTrans.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTransAfter.java
```

### 16.3 批量划付参考

```text
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/TransTransferTiBatchBusinessServiceImpl.java
```

重点参考：

- 按卡加锁。
- 明细先计算/写入。
- 账户余额最后一次性更新。
- 分批处理。
- 批量任务日志。

### 16.4 账户批量能力

```text
fund-catering-base/fund-catering-base-api/src/main/java/com/chinaums/erp/slhy/catering/base/api/BaseAccountServiceApi.java
fund-catering-base/fund-catering-base-service/src/main/java/com/chinaums/erp/slhy/catering/base/service/impl/AccountChangeBatchServiceImpl.java
fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/service/impl/AccountChangeDetailBatchServiceImpl.java
```

## 十七、开发注意事项

- 不要把新批量逻辑写成继续循环调用 `rechargeTrans`。
- 不要在 task 中持有平台卡锁。
- 不要在 task 中写最终 done。
- 不要让 consume 抢锁失败后等待。
- 不要让重复流水参与余额链计算。
- 不要边入库边计算余额链。
- 不要在唯一键冲突后跳过单笔继续使用旧余额链。
- 不要省略日志。
- 不要吞异常。
- 不要使用 Redis 作为唯一防重依据。
- 不要绕过充值主/子流水表，否则报表、查询、后续退款/对账会断链。
