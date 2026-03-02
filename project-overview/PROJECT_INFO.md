# lsym 项目概览

## 项目基本信息

| 属性 | 值 |
|------|------|
| **项目名称** | lsym (餐饮资金体系) |
| **本地路径** | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy` |
| **GitHub** | https://github.com/ssssgoldhunter |
| **飞书文档** | https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f |

## 项目结构

```
slhy/
├── fund-catering/          # 餐饮资金核心模块
│   ├── fund-catering-consume/    # 消费服务
│   ├── fund-catering-front/      # 前置服务
│   └── ...
├── md/                     # 设计文档目录
│   └── fund-catering/
│       └── 供应链设计文档-飞书/
├── api-modules/            # API模块
├── common-core/            # 核心组件
└── ...
```

## 核心子项目

### fund-catering-consume (消费服务)
- **端口**: 8092
- **主启动类**: ConsumeServiceApplication
- **配置**: bootstrap.yml (Nacos配置中心)
- **核心框架**: LiteFlow 流程编排引擎

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 2.x | 应用框架 |
| LiteFlow | 最新版 | 流程编排引擎（核心） |
| MyBatis Plus | 最新版 | ORM框架 |
| Redis | - | 分布式锁、缓存 |
| Nacos | - | 配置中心、服务注册发现 |

## 关键文档

### 飞书设计文档
- **最新版本**: v5.5 (2026-03-02)
- **文档链接**: https://jvn4jogcy6u.feishu.cn/docx/IYn3dcLQ9odELzxY5MjcHdTAn6f
- **包含内容**:
  - 消费交易流程详解
  - 充值交易流程详解
  - 充值退款流程详解
  - 提现交易流程详解
  - 转账交易流程详解
  - 消费退款流程详解

## 重要路径

| 类型 | 路径 |
|------|------|
| 源码 | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/fund-catering/fund-catering-consume` |
| 文档 | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/slhy/md/fund-catering` |
| 记忆体 | `/Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory` |
