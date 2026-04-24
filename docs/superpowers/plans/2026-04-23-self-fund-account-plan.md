# 自有资金账户（Self-Owned Fund Account）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持自有资金账户的入金充值、补偿查询、转账（平台付款/收款）全流程

**Architecture:** 在现有入金链路上扩展 transType=03 分支，Front 层新增平台收付款 Handle 方法，Task 层新建独立补偿 Job。通过 bas_param_t 配置驱动银行差异化参数。

**Tech Stack:** Java 17, Spring Boot 3.2.4, LiteFlow, MyBatis Plus, XXL-Job, Redis

**Spec:** `docs/superpowers/specs/2026-04-23-self-fund-account-design.md`

---

## File Structure

### Modify

| File | 改动 |
|------|------|
| `common-core/.../constant/CommonConstants.java` | 新增 TRANS_TYPE_SELF_FUND 常量 |
| `fund-catering-front-api/.../request/BasTransTransferReq.java` | 新增 bizFunc/fundType/dealType/bankEAccountId 字段 |
| `fund-catering-front-service/.../handle/BasTransTransferHandle.java` | 新增 platformPay/platformReceive 方法 |
| `fund-catering-front-service/.../handle/impl/AbstractTransTransferHandle.java` | 新增默认空实现 |
| `fund-catering-front-service/.../handle/impl/zx/ZxTransTransferHandle.java` | 实现 platformPay/platformReceive |
| `fund-catering-front-service/.../handle/BasTransQueryHandle.java` | 新增 queryRegisterTransPages 方法 |
| `fund-catering-front-service/.../handle/impl/AbstractTransQueryHandle.java` | 新增默认空实现 |
| `fund-catering-front-service/.../handle/impl/zx/ZxTransQueryHandle.java` | 实现 queryRegisterTransPages |
| `fund-catering-front-service/.../sass/SaasZxInterService.java` | 新增 zxPlatformPay/zxPlatformReceive/zxQueryRegisterDetails 方法 |
| `fund-catering-front-api/.../api/SaasZxApi.java` | 新增 API 路径常量 |
| `fund-catering-consume/.../recharge/RechargeTrans.java` | isAccess() 加 03 + bankEAccountId null 处理 |
| `fund-catering-consume/.../recharge/RechargeTransAfter.java` | isAccess() 加 03 |
| `fund-catering-web/.../zx/ReverseNoticeController.java` | 支持 trans_tp=03 |
| `fund-catering-task/.../constant/TaskConstants.java` | 新增常量 |

### Create

| File | 说明 |
|------|------|
| `fund-catering-front-api/.../request/BasPlatformAccountDetailQueryReq.java` | 登记簿明细查询请求 DTO |
| `fund-catering-task/.../job/zx/ZxSelfFundNotifyJobService.java` | 自有资金补偿同步 Job |
| `fund-catering-task/.../job/zx/ZxSelfFundNotifyRechargeJobService.java` | 自有资金补偿充值 Job |
| `fund-catering-task/.../service/impl/zx/ZxSelfFundNotifyRechargeServiceImpl.java` | 自有资金补偿服务实现 |

---

## Task 1: CommonConstants 新增常量

**Files:**
- Modify: `slhy/common-core/src/main/java/com/chinaums/slhy/common/catering/constant/CommonConstants.java:84`

- [ ] **Step 1: 在 TRANS_TYPE_CR 之后新增常量**

在 `CommonConstants.java` 第 84 行 `TRANS_TYPE_CR = "CR"` 之后添加：

```java
/** 自有资金入金 */
String TRANS_TYPE_SELF_FUND = "03";
```

- [ ] **Step 2: 编译验证**

```bash
cd /Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy && mvn compile -pl common-core -am -q
```

- [ ] **Step 3: Commit**

```bash
git add common-core/src/main/java/com/chinaums/slhy/common/catering/constant/CommonConstants.java
git commit -m "feat: add TRANS_TYPE_SELF_FUND constant for self-owned fund deposit"
```

---

## Task 2: BasTransTransferReq 扩展字段

**Files:**
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/request/BasTransTransferReq.java`

- [ ] **Step 1: 新增平台收付款字段**

在 `BasTransTransferReq.java` 现有字段后添加：

```java
/** 业务用途（平台付款2041/平台收款2042） */
private String bizFunc;

/** 资金类型 */
private String fundType;

/** 交易类型 */
private String dealType;

/** 自有资金账户J编号 */
private String bankEAccountId;
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-front/fund-catering-front-api -am -q
```

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/request/BasTransTransferReq.java
git commit -m "feat: add platform transfer fields to BasTransTransferReq"
```

---

## Task 3: SaasZxApi 新增 API 路径常量

**Files:**
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/api/SaasZxApi.java`

- [ ] **Step 1: 在 Saas_Api 类中新增常量**

在 `Saas_Api` 类中 `Transfer = "transfer"` 附近添加：

```java
PlatformPay = "transfer";
PlatformReceive = "transfer";
QueryRegisterDetails = "query-trans-details";
```

> 注：2041/2042 与 27 共用 `/cwap/account/send/transfer` URL，24 共用 `/cwap/account/send/query-trans-details` URL，通过 bizFunc 区分。

- [ ] **Step 2: Commit**

```bash
git add fund-catering/fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/api/SaasZxApi.java
git commit -m "feat: add ZX platform pay/receive/query API path constants"
```

---

## Task 4: SaasZxInterService 新增方法

**Files:**
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/service/sass/SaasZxInterService.java`

- [ ] **Step 1: 新增 3 个方法**

参照现有 `zxTransfer()` 方法的模式，新增：

```java
/**
 * 平台付款（自有资金 → 用户登记簿）bizFunc=2041
 */
public JSONObject zxPlatformPay(ZxPlatformTransferRequest request) {
    String body = JSONObject.toJSONString(request);
    return excute(request.getAppIdBank(), request.getAppKeyBank(),
        request.getUrlBank(), SaasZxApi.Saas_Api.PlatformPay, body);
}

/**
 * 平台收款（用户登记簿 → 自有资金）bizFunc=2042
 */
public JSONObject zxPlatformReceive(ZxPlatformTransferRequest request) {
    String body = JSONObject.toJSONString(request);
    return excute(request.getAppIdBank(), request.getAppKeyBank(),
        request.getUrlBank(), SaasZxApi.Saas_Api.PlatformReceive, body);
}

/**
 * 登记簿交易明细查询 bizFunc=24
 */
public JSONObject zxQueryRegisterDetails(ZxQueryTransDetailRequest request) {
    String body = JSONObject.toJSONString(request);
    return excute(request.getAppIdBank(), request.getAppKeyBank(),
        request.getUrlBank(), SaasZxApi.Saas_Api.QueryRegisterDetails, body);
}
```

> 请求 DTO 复用现有的 `ZxQueryTransDetailRequest`（查询）和 `ZxTransferRequest`（转账），或根据需要扩展。平台收付款请求体字段与现有 transfer 不同（reserve 中有 dealType/fundTp 等），需要确认 DTO 是否够用，不够则新增 `ZxPlatformTransferRequest`。

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-front/fund-catering-front-service -am -q
```

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/service/sass/SaasZxInterService.java
git commit -m "feat: add ZX platform pay/receive and register query inter service methods"
```

---

## Task 5: Transfer Handle 接口 + 抽象类 + ZX 实现

**Files:**
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/BasTransTransferHandle.java`
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/AbstractTransTransferHandle.java`
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/zx/ZxTransTransferHandle.java`

- [ ] **Step 1: BasTransTransferHandle 接口新增 2 个方法**

在 `BasTransTransferHandle.java` 接口中 `transTransferRecall` 方法之后添加：

```java
/**
 * 平台付款（自有资金 → 用户登记簿）
 */
<T> T platformPay(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo);

/**
 * 平台收款（用户登记簿 → 自有资金）
 */
<T> T platformReceive(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo);
```

- [ ] **Step 2: AbstractTransTransferHandle 默认空实现**

在 `AbstractTransTransferHandle.java` 中添加：

```java
@Override
public <T> T platformPay(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo) {
    return null;
}

@Override
public <T> T platformReceive(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo) {
    return null;
}
```

- [ ] **Step 3: ZxTransTransferHandle 实现平台付款**

参照现有 `transTransfer()` 方法的实现模式，在 `ZxTransTransferHandle.java` 中添加：

```java
@Override
public DefaultResult<BasTransTransferRes> platformPay(BasTransTransferReq request, BasPlatformSettleInfoQueryRes plaformInfo) {
    DefaultResult<BasTransTransferRes> defaultResult = new DefaultResult<>();
    try {
        // 构建 ZX 转账请求
        ZxTransferRequest zxRequest = new ZxTransferRequest();
        // 填充公共字段（参照 transTransfer）
        zxRequest.setTransSsn(generateTransSsn(plaformInfo.getPlatformMchntMbrId()));
        zxRequest.setTransTime(DateFomatUtils.getTransTime());
        zxRequest.setMchntId(plaformInfo.getPlatformMchntId());
        zxRequest.setMchntMbrId(plaformInfo.getPlatformMchntMbrId());
        zxRequest.setBizFunc(request.getBizFunc()); // "2041"
        zxRequest.setChnlNo(plaformInfo.getChannelNo());
        zxRequest.setOutAcctNo(SMUtil.sm2EncryptHex(plaformInfo.getPublicKey(), request.getBankEAccountId()));
        zxRequest.setInAcctNo(SMUtil.sm2EncryptHex(plaformInfo.getPublicKey(), request.getPayCardCode()));
        zxRequest.setTransAmt(request.getTransAmount());
        // reserve 填充平台付款特有字段
        Map<String, Object> reserve = new HashMap<>();
        reserve.put("DEAL_TYPE", request.getDealType());
        reserve.put("USER_C_NM", "平台付款"); // 或从request取
        reserve.put("BUSS_ID", request.getTransNo());
        reserve.put("BUSS_SUB_ID", request.getTransNo());
        reserve.put("TRANS_DT", DateFomatUtils.getDateStr(new Date()));
        reserve.put("TRANS_TM", DateFomatUtils.getTimeStr(new Date()));
        reserve.put("FUND_TP", request.getFundType());
        zxRequest.setReserve(reserve);
        // 设置银行连接参数
        zxRequest.setAppIdBank(plaformInfo.getPlatformAppIdBank());
        zxRequest.setAppKeyBank(plaformInfo.getPlatformAppKeyBank());
        zxRequest.setUrlBank(plaformInfo.getPlatformUrlBank());

        JSONObject result = saasZxInterService.zxPlatformPay(zxRequest);
        // 解析结果（参照 transTransfer 的解析逻辑）
        // ...
        defaultResult.setCode(result.getString("errCode"));
        defaultResult.setMsg(result.getString("errInfo"));
    } catch (Exception e) {
        log.error("平台付款异常", e);
        defaultResult.setCode("FAIL");
        defaultResult.setMsg(e.getMessage());
    }
    return defaultResult;
}
```

> `platformReceive()` 实现类似，区别：bizFunc="2042"，outAcctNo=付款用户，inAcctNo=自有资金bankEAccountId，reserve 中用 outAcctNm 和 recDealType/recFundType。完整代码实现时参照上述模式调整字段。

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-front/fund-catering-front-service -am -q
```

- [ ] **Step 5: Commit**

```bash
git add fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/
git commit -m "feat: add platformPay/platformReceive to Transfer Handle + Zx implementation"
```

---

## Task 6: Query Handle 接口 + 抽象类 + ZX 实现

**Files:**
- Create: `slhy/fund-catering/fund-catering-front/fund-catering-front-api/src/main/java/com/chinaums/erp/slhy/catering/front/request/BasPlatformAccountDetailQueryReq.java`
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/BasTransQueryHandle.java`
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/AbstractTransQueryHandle.java`
- Modify: `slhy/fund-catering/fund-catering-front/fund-catering-front-service/src/main/java/com/chinaums/erp/slhy/catering/front/handle/impl/zx/ZxTransQueryHandle.java`

- [ ] **Step 1: 创建 BasPlatformAccountDetailQueryReq DTO**

```java
package com.chinaums.erp.slhy.catering.front.request;

import lombok.Data;

@Data
public class BasPlatformAccountDetailQueryReq {
    /** 交易类型（99=所有） */
    private String transType;
    /** 交易日期 YYYYMMDD */
    private String transDate;
    /** 页码 */
    private String page;
    /** 登记簿类型（12=自有资金） */
    private String registerAttr;
    /** 自有资金账户J编号 */
    private String bankEAccountId;
    /** 银行连接参数 */
    private String appIdBank;
    private String appKeyBank;
    private String urlBank;
}
```

- [ ] **Step 2: BasTransQueryHandle 接口新增方法**

在 `BasTransQueryHandle.java` 中添加：

```java
/**
 * 平台账户登记簿明细查询
 */
<T> T queryRegisterTransPages(BasPlatformAccountDetailQueryReq request, BasPlatformSettleInfoQueryRes plaformInfo);
```

- [ ] **Step 3: AbstractTransQueryHandle 默认空实现**

```java
@Override
public <T> T queryRegisterTransPages(BasPlatformAccountDetailQueryReq request, BasPlatformSettleInfoQueryRes plaformInfo) {
    return null;
}
```

- [ ] **Step 4: ZxTransQueryHandle 实现**

参照现有 `queryPlatformTransPages()` 方法的模式：

```java
@Override
public BankPagedListResultDto<BasTransDetailQueryRes> queryRegisterTransPages(
        BasPlatformAccountDetailQueryReq request, BasPlatformSettleInfoQueryRes plaformInfo) {
    ZxQueryTransDetailRequest zxRequest = new ZxQueryTransDetailRequest();
    zxRequest.setTransSsn(generateTransSsn(plaformInfo.getPlatformMchntMbrId()));
    zxRequest.setTransTime(DateFomatUtils.getTransTime());
    zxRequest.setMchntId(plaformInfo.getPlatformMchntId());
    zxRequest.setMchntMbrId(plaformInfo.getPlatformMchntMbrId());
    zxRequest.setBizFunc("24"); // 登记簿交易明细查询
    zxRequest.setChnlNo(plaformInfo.getChannelNo());
    zxRequest.setAcctNo(SMUtil.sm2EncryptHex(plaformInfo.getPublicKey(), request.getBankEAccountId()));
    // reserve
    Map<String, Object> reserve = new HashMap<>();
    reserve.put("TRANS_TYPE", request.getTransType());
    reserve.put("REGISTER_ATTR", request.getRegisterAttr());
    reserve.put("TRANS_DATE", request.getTransDate());
    reserve.put("PAGE", request.getPage());
    reserve.put("laasSsn", generateLaasSsn());
    zxRequest.setReserve(reserve);
    // 银行连接参数
    zxRequest.setAppIdBank(plaformInfo.getPlatformAppIdBank());
    zxRequest.setAppKeyBank(plaformInfo.getPlatformAppKeyBank());
    zxRequest.setUrlBank(plaformInfo.getPlatformUrlBank());

    JSONObject result = saasZxInterService.zxQueryRegisterDetails(zxRequest);
    // 解析结果（参照 queryPlatformTransPages 的解析逻辑）
    // ...
    return parsedResult;
}
```

- [ ] **Step 5: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-front -am -q
```

- [ ] **Step 6: Commit**

```bash
git add fund-catering/fund-catering-front/
git commit -m "feat: add queryRegisterTransPages to Query Handle + Zx implementation + DTO"
```

---

## Task 7: RechargeTrans isAccess() 改造

**Files:**
- Modify: `slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTrans.java:237`

- [ ] **Step 1: 修改 isAccess() 方法**

找到 `isAccess()` 方法（约第 237 行），将条件从：

```java
slot.getTransType().equals(CommonConstants.TRANS_TYPE_C) ||
slot.getTransType().equals(CommonConstants.TRANS_TYPE_IC)
```

改为：

```java
slot.getTransType().equals(CommonConstants.TRANS_TYPE_C) ||
slot.getTransType().equals(CommonConstants.TRANS_TYPE_IC) ||
slot.getTransType().equals(CommonConstants.TRANS_TYPE_SELF_FUND)
```

- [ ] **Step 2: 处理 bankEAccountId null**

在 `createRechargeTrans()` 方法中找到 accountType=04 路径里 `setBankEAccountCode` 的调用：

```java
if(rechargeTransVo.getAccountType().equals(CommonConstants.CARD_SUB_ACCOUNT_TYPE_04)){
    rechargeSubTReq.setBankEAccountCode(companyInfo.getBankEAccountId());
}
```

改为：

```java
if(rechargeTransVo.getAccountType().equals(CommonConstants.CARD_SUB_ACCOUNT_TYPE_04)){
    if (StringUtils.isNotBlank(companyInfo.getBankEAccountId())) {
        rechargeSubTReq.setBankEAccountCode(companyInfo.getBankEAccountId());
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q
```

- [ ] **Step 4: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTrans.java
git commit -m "fix: allow TRANS_TYPE_SELF_FUND in RechargeTrans isAccess + handle null bankEAccountId"
```

---

## Task 8: RechargeTransAfter isAccess() 改造

**Files:**
- Modify: `slhy/fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTransAfter.java`

- [ ] **Step 1: 修改 isAccess() 方法**

与 Task 7 相同的改法，在 `isAccess()` 条件中增加 `TRANS_TYPE_SELF_FUND`：

```java
slot.getTransType().equals(CommonConstants.TRANS_TYPE_C) ||
slot.getTransType().equals(CommonConstants.TRANS_TYPE_IC) ||
slot.getTransType().equals(CommonConstants.TRANS_TYPE_SELF_FUND)
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-consume/fund-catering-consume-service -am -q
```

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-consume/fund-catering-consume-service/src/main/java/com/chinaums/erp/slhy/catering/consume/flow/component/trans/recharge/RechargeTransAfter.java
git commit -m "fix: allow TRANS_TYPE_SELF_FUND in RechargeTransAfter isAccess"
```

---

## Task 9: ReverseNoticeController 支持 trans_tp=03

**Files:**
- Modify: `slhy/fund-catering/fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/zx/ReverseNoticeController.java`

- [ ] **Step 1: trans_tp 过滤集合新增 "03"**

找到 `trans_tp` 过滤逻辑（`transTp` 在 `{01, 02}` 中的判断），将 `03` 加入允许集合。

- [ ] **Step 2: trans_tp=03 分支处理**

在 `executeRechargeProcess()` 方法或 `transNotifyZx()` 方法中，公司查询逻辑处增加分支：

```java
if ("03".equals(transTp)) {
    // 自有资金入金：从配置读 cardCode
    String configJson = baseServiceCommonApi.selectOneParamByRequest("SELF_FUND_ACCOUNT_CONFIG");
    SelfFundAccountConfig config = JSON.parseArray(configJson, SelfFundAccountConfig.class).get(0);
    companyInfo = queryCompanyByCardCode(config.getCardCode());
} else {
    // 现有逻辑
    companyInfo = queryByBankEAccountCode(mchntId);
}
```

> `SelfFundAccountConfig` 是一个简单的 POJO，字段对应 `SELF_FUND_ACCOUNT_CONFIG` 的 JSON 结构。可以用内部类或独立类。

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-web -am -q
```

- [ ] **Step 4: Commit**

```bash
git add fund-catering/fund-catering-web/src/main/java/com/chinaums/erp/slhy/catering/web/controller/manage/zx/ReverseNoticeController.java
git commit -m "feat: support trans_tp=03 self-owned fund deposit in ReverseNoticeController"
```

---

## Task 10: TaskConstants 新增常量

**Files:**
- Modify: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/constant/TaskConstants.java`

- [ ] **Step 1: 新增自有资金补偿配置常量**

```java
/** 中信自有资金补偿同步配置 */
String PARAM_ZX_SELF_FUND_TRANS_CONFIG = "ZX_SELF_FUND_TRANS_CONFIG";
```

- [ ] **Step 2: Commit**

```bash
git add fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/constant/TaskConstants.java
git commit -m "feat: add ZX self-fund compensation task constants"
```

---

## Task 11: 自有资金补偿服务实现

**Files:**
- Create: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/zx/ZxSelfFundNotifyRechargeServiceImpl.java`

- [ ] **Step 1: 创建服务类**

参照 `ZxTransNotifyRechargeServiceImpl.java` 的模式，创建 `ZxSelfFundNotifyRechargeServiceImpl`：

核心方法：
- `syncSelfFundTransDetails()`：调用 `queryRegisterTransPages`（bizFunc=24, registerAttr=12）查询当日自有资金登记簿明细，逐条判断是否需要补偿（流水号去重），创建通知记录（INIT）
- `handleSelfFundNotifyRecharge()`：查询状态为 INIT 的自有资金通知记录，读取 `SELF_FUND_ACCOUNT_CONFIG` 的 `cardCode` 查找公司，调用 `transConsumeApi.rechargeTrans()` 充值

关键区别（与现有 ZX 补偿服务不同）：
- 使用 `queryRegisterTransPages` 而非 `queryPlatformTransPages`
- 公司查询用 `SELF_FUND_ACCOUNT_CONFIG` 的 `cardCode` 而非 `platformComCode + payAccno`
- 扫描当日所有数据（分页获取），逐条判断，非时间窗口模式
- `transType` 标记为 "03"

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-task -am -q
```

- [ ] **Step 3: Commit**

```bash
git add fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/service/impl/zx/ZxSelfFundNotifyRechargeServiceImpl.java
git commit -m "feat: add ZxSelfFundNotifyRechargeServiceImpl for self-owned fund compensation"
```

---

## Task 12: 自有资金补偿 Job

**Files:**
- Create: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/zx/ZxSelfFundNotifyJobService.java`
- Create: `slhy/fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/zx/ZxSelfFundNotifyRechargeJobService.java`

- [ ] **Step 1: 创建同步 Job**

参照 `ZxTransNotifyJobService.java` 的模式：

```java
@Service
@Slf4j
public class ZxSelfFundNotifyJobService {

    @Autowired
    private ZxSelfFundNotifyRechargeService zxSelfFundNotifyRechargeService;

    @XxlJob("zxSelfFundNotifyJobService")
    public void run() {
        // 读取 ZX_SELF_FUND_TRANS_CONFIG 配置
        // 调用 zxSelfFundNotifyRechargeService.syncSelfFundTransDetails()
    }
}
```

- [ ] **Step 2: 创建充值 Job**

参照 `ZxTransNotifyRechargeJobService.java` 的模式：

```java
@Service
@Slf4j
public class ZxSelfFundNotifyRechargeJobService {

    @Autowired
    private ZxSelfFundNotifyRechargeService zxSelfFundNotifyRechargeService;

    @XxlJob("zxSelfFundNotifyRechargeJobService")
    public void run() {
        // 读取 ZX_SELF_FUND_TRANS_CONFIG 配置
        // 调用 zxSelfFundNotifyRechargeService.handleSelfFundNotifyRecharge()
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl fund-catering/fund-catering-task -am -q
```

- [ ] **Step 4: Commit**

```bash
git add fund-catering/fund-catering-task/src/main/java/com/chinaums/erp/slhy/catering/task/job/zx/
git commit -m "feat: add ZX self-fund compensation jobs (sync + recharge)"
```

---

## Task 13: 全量编译 + 集成验证

- [ ] **Step 1: 全量编译**

```bash
cd /Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy && mvn compile -q
```

- [ ] **Step 2: 检查编译错误并修复**

- [ ] **Step 3: 确认 XXL-Job handler 名称在配置中心注册**

需要注册的 Job handler：
- `zxSelfFundNotifyJobService`
- `zxSelfFundNotifyRechargeJobService`

- [ ] **Step 4: UAT 测试验证**

测试场景：
1. 银行推送 trans_tp=03 通知 → 充值成功
2. 补偿 Job 同步 → 查询登记簿明细 → 创建通知记录
3. 补偿 Job 充值 → 处理 INIT 记录 → 充值成功
4. 现有 trans_tp=01/02 充值不受影响（回归测试）
