# LiteFlow Skills 指南

## 1. 通用技术业务框架设计指南

### 框架设计原则

**分层架构原则：**
```
API层 (Controller)
  ↓
Client层 (Service实现)
  ↓
Biz层 (业务逻辑)
  ↓
Domain层 (领域服务)
  ↓
ORM层 (数据访问)
```

**职责划分原则：**

| 层级 | 职责 | 特点 |
|------|------|------|
| **API层** | 接口定义、参数校验、结果封装 | 薄层，只做参数转换 |
| **Client层** | 服务实现、事务管理、异常处理 | 实现业务逻辑 |
| **Biz层** | 业务规则、业务流程编排 | 业务逻辑核心 |
| **Domain层** | 领域服务、数据访问 | 数据操作 |
| **ORM层** | 数据库操作 | 数据持久化 |

### 包结构设计

```
com.chinaums.erp.slhy.catering.{module}
├── controller/          # API层
├── domain/              # Domain层
│   └── impl/
├── biz/                 # Biz层
│   └── impl/
├── service/             # Client层（Service实现）
├── mapper/              # ORM层
├── request/             # 请求对象
├── response/            # 响应对象
└── vo/                  # VO对象
```

---

## 2. LiteFlow 流程设计原则

- **Pack → Check → Trans → After**：标准流程顺序
- **单一职责**：每个组件只负责一个职责
- **数据流转**：通过Slot传递数据，避免组件间直接依赖
- **异常处理**：每个组件都要有完善的异常处理机制

### 组件划分

| 组件类型 | 职责 | 特点 |
|---------|------|------|
| **Pack** | 数据打包、参数校验 | 只读、不修改数据 |
| **Check** | 业务规则校验 | 只读、不修改数据 |
| **Trans** | 核心业务处理 | 读写、修改数据库 |
| **After** | 后处理、明细记录 | 读写、创建明细记录 |

### Slot设计

```java
public class TransSlot extends BaseSlot {
    // 交易基本信息
    private String transNo;
    private String transType;

    // VO对象
    private ConsumeTransRefundVo consumeTransRefundVo;

    // 基础数据Map
    private Map<String, List<BasCardSubAccountTQueryRes>> cardSubAccountMap;
    private Map<String, BasBusinessInfoQueryRes> businessInfos;

    // Redis锁
    private List<String> redisLockKeys;
}
```

---

## 3. Pack 组件

### 职责
1. 数据打包：将请求参数转换为内部VO对象
2. 参数校验：校验必填参数、格式、业务规则
3. 数据转换：将外部数据格式转换为内部处理格式
4. 数据准备：查询基础数据，填充到Slot中
5. 唯一性校验：校验交易单号等唯一性标识

### 唯一性校验示例
```java
int count = transConsumeTService.existsConsumeByTransNo(vo.getTransNo());
if(count > 0){
    throw new BaseException(BaseErrorCodeEnum.TRANSNOREPEAT.code(),
            BaseErrorCodeEnum.TRANSNOREPEAT.message());
}
```

---

## 4. Check 组件

### 职责
1. 业务规则校验：余额是否充足、状态是否允许
2. 权限校验：是否有权限操作该账户
3. 状态校验：订单状态是否允许操作
4. 关联数据校验：关联数据是否存在、是否有效

### 账户校验示例
```java
BasCardSubAccountTQueryRes account = slot.getCardSubAccountMap()
    .get(cardCode).stream()
    .findFirst()
    .orElse(null);
if (account == null) {
    throw new BaseException(BaseErrorCodeEnum.ACCOUNT_NOT_EXISTS.code(), "账户不存在");
}
```

### 余额校验示例
```java
BigInteger availableBalance = account.getBalance();
BigInteger requiredAmount = new BigInteger(vo.getAmount());
if (availableBalance.compareTo(requiredAmount) < 0) {
    throw new BaseException(BaseErrorCodeEnum.BALANCE_NOT_ENOUGH.code(), "余额不足");
}
```

---

## 5. Trans 组件

### 职责
1. 核心业务处理：扣款、充值、转账
2. 数据库操作：创建交易记录、更新账户状态
3. 账户变动：计算变动金额、更新余额
4. 数据计算：退款金额分配、手续费计算
5. 外部调用：支付接口、通知接口

### 账户余额更新示例
```java
BasCardSubAccountTReq updateReq = Convert.convert(BasCardSubAccountTReq.class, account);
BigInteger transAmount = new BigInteger(vo.getAmount());
updateReq.setBalance(new BigInteger(CommonConstants.MINUS + transAmount));

AccountChangeReq accountChangeReq = new AccountChangeReq();
accountChangeReq.setSubAccountUpdate(updateReq);
DefaultResult<Integer> result = baseAccountServiceApi.changeAccount(accountChangeReq);
```

---

## 6. After 组件

### 职责
1. 账户变动明细：创建明细记录和汇总表数据
2. 数据汇总：汇总交易数据
3. 资源释放：释放Redis锁、清理临时数据
4. 后续处理：调用通知接口、记录日志

### 资源释放示例
```java
if (slot.getRedisLockKeys() != null && !slot.getRedisLockKeys().isEmpty()) {
    for (String lockKey : slot.getRedisLockKeys()) {
        redisLockUtils.unlockKey(lockKey);
    }
}
slot.clear();
```

---

## 7. lsym 项目强制记忆加载规则

- 凡是 `lsym` 开发项目任务，默认必须先加载 `lsym-memory-hub`（至少先读 `workflow/PROJECT_MEMORY.md` 和默认阅读顺序文档）。
- 退款、转账、提现等账户变动问题，必须先从 `lsym-memory-hub/docs/ACCOUNT_CHANGE_SOURCE_MAP.md` 定位源码入口，再进入具体组件排查。

---

## 8. 消费退款链路排查要点（含 04转01 模式）

### 路由分支

- 统一入口：`chainConsumeRefund`
- 分支判断：`consumeTransRefundSplitPercentPack / consumeTransRefundSplitOrderPack`
- 普通04退款链路：`bashChainRefundModel04` → `ConsumeTransRefundAfter`
- 04转01模式链路：`bashChainRefundModel01` → `ConsumeTransRefundModel1After`

### 关键差异

- `ConsumeTransRefundAfter`：
  - 同时处理付款侧和收款侧（04 扣回 + 解冻）。
  - 汇总明细包含付款与收款数据。
- `ConsumeTransRefundModel1After`：
  - 主要处理 01/02 侧（含 04转01充值生成的 01 变动）。
  - `trans_acct_act_sum_change_detail_t` 由 `writeSummaryChanges` 中 `refundByActivity` 聚合生成。

### `trans_acct_act_sum_change_detail_t` 重点口径

- 关注字段：`org_amt`、`trans_amt`、`balance`、`org_real_amt`、`real_balance`、`activity_code`。
- Model1 中 `org_amt` 来源：`collectInitialActivityTotals` 通过 `sumAvailableAmtByActivityCodesIn` 预采集。
- 排查时必须区分两类 `subAccountExpendTMap` 数据：
  - 普通退款恢复（`isRechargeFrom04 != true`）
  - 04转01转充值（`isRechargeFrom04 == true`）
- 常见风险点：
  1. 将余额口径与本金口径混用（`org_amt` 与 `org_real_amt` 一刀切）。
  2. 同一卡同子账户下，`sample` 取值与聚合口径不一致，导致汇总记录属性串位。
  3. 04转01转充值记录与普通退款恢复记录混合聚合时，活动科目期初被覆盖或放大。

---

## Skills 文件位置

| Skill名称 | 源文件位置 |
|----------|------------|
| 通用框架 | `/Users/limeng/workspaces/skills/GENERAL_FRAMEWORK_SKILL.md` |
| LiteFlow主流程 | `/Users/limeng/workspaces/skills/liteflow-skills/MAIN_TASK_SKILL.md` |
| Pack组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_PACK.md` |
| Check组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_CHECK.md` |
| Trans组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_TRANS.md` |
| After组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_AFTER.md` |
