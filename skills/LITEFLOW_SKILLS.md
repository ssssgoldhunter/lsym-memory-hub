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

## Skills 文件位置

| Skill名称 | 源文件位置 |
|----------|------------|
| 通用框架 | `/Users/limeng/workspaces/skills/GENERAL_FRAMEWORK_SKILL.md` |
| LiteFlow主流程 | `/Users/limeng/workspaces/skills/liteflow-skills/MAIN_TASK_SKILL.md` |
| Pack组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_PACK.md` |
| Check组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_CHECK.md` |
| Trans组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_TRANS.md` |
| After组件 | `/Users/limeng/workspaces/skills/liteflow-skills/SUB_TASK_SKILL_AFTER.md` |
