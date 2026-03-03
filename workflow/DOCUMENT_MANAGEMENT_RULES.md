# 工作偏好：文档和记忆体管理规则

> **重要规则**: 除非用户直接要求，所有 md 文档和记忆体内容都应存放在 lsym-memory 记忆体项目中

## 📋 核心规则

### 文档存储规则

| 类型 | 存储位置 | 说明 |
|------|----------|------|
| **md 文档** | `lsym-memory/` | 所有 Markdown 文档都放在记忆体项目 |
| **记忆体内容** | `lsym-memory/` | AI 助手的学习内容、项目知识等 |
| **项目文档** | `lsym-memory/docs/` | 项目的核心设计文档 |
| **快速参考** | `lsym-memory/business-flows/` | 业务流程快速参考 |
| **技术文档** | `lsym-memory/architecture/` | 架构设计文档 |
| **工作流程** | `lsym-memory/workflow/` | 工作流程和偏好设置 |

### 不放在记忆体的内容

| 类型 | 存储位置 | 说明 |
|------|----------|------|
| **源代码** | `slhy/` 项目目录 | 代码文件放在项目中 |
| **配置文件** | `slhy/` 项目目录 | 配置文件放在项目中 |
| **资源文件** | `slhy/` 项目目录 | 静态资源放在项目中 |
| **编译产物** | `slhy/` 项目目录 | 编译生成的文件 |

## 🎯 使用场景

### 多台电脑协作

1. **电脑 A** 工作
   - 拉取最新的 lsym-memory 仓库
   - 查看记忆体中的项目信息
   - 继续工作

2. **电脑 B** 工作
   - 拉取最新的 lsym-memory 仓库
   - 获取电脑 A 更新的记忆体内容
   - 继续工作

3. **同步流程**
   ```
   工作 → 更新记忆体 → git push
   ↓
   切换电脑 → git pull → 获取最新记忆体
   ↓
   继续工作
   ```

## 📝 记忆体更新时机

AI 助手会在以下情况更新记忆体：

- ✅ 发现重要的项目信息
- ✅ 学习新的业务流程
- ✅ 记录技术决策
- ✅ 总结常见问题解决方案
- ✅ 更新文档版本
- ✅ 用户明确要求保存内容

## 🔧 操作流程

### 保存文档到记忆体

```bash
cd /Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory

# 编辑或添加文档
vim docs/NEW_DOCUMENT.md

# 提交到 Git
git add .
git commit -m "docs: add new document"
git push
```

### 从记忆体获取信息

```bash
cd /Users/limeng/workspaces/IdeaProjects_lsym_dep/lsym-memory

# 拉取最新内容
git pull

# 查看文档
cat docs/TRANSACTION_QUICK_REFERENCE.md
```

## 📂 记忆体目录结构

```
lsym-memory/
├── docs/                    # 所有设计文档和参考文档
│   ├── SUPPLY_CHAIN_DESIGN_V5.5.md
│   ├── TRANSACTION_QUICK_REFERENCE.md
│   └── MIGRATION_LOG.md
├── architecture/            # 技术架构文档
│   ├── FRAMEWORK_BLUEPRINT.md
│   ├── FRAMEWORK_STRUCTURE.md
│   └── TRANS_COMPONENT_STRUCTURE.md
├── business-flows/          # 业务流程文档
│   ├── TRANSACTION_FLOWS.md
│   └── CONSUME_FLOW_DIAGRAMS.md
├── technical-decisions/     # 技术决策记录
│   └── BATCH_TRANSFER_IMPLEMENTATION.md
├── project-overview/        # 项目概览信息
│   └── PROJECT_INFO.md
├── workflow/               # 工作流程和偏好
│   └── USER_PREFERENCES.md
└── knowledge-base/         # 知识库
    └── (待添加)
```

## 🔄 与项目的关系

```
slhy/                          lsym-memory/
├── fund-catering/            ├── docs/           (从slhy迁移的文档)
│   ├── fund-catering-        ├── architecture/    (技术架构)
│   ├── fund-catering-front/   ├── business-flows/  (业务流程)
│   └── ...                   ├── workflow/       (工作偏好)
└── ...                       └── ...
```

## 🚫 例外情况

只有用户明确要求时，才将内容放在其他位置：

- 用户要求：`把这个文档放在项目目录下`
- 用户要求：`在项目中创建这个文件`
- 特殊需求：某些文档必须和代码放在一起

---

**生效日期**: 2026-03-03
**更新原因**: 支持多台电脑协作，统一文档管理
**GitHub**: https://github.com/ssssgoldhunter/lsym-memory-hub
