# Self Fund Account

> Status: current
> Last updated: 2026-05-12

This page is the current entry point for 自有资金池 and 特殊账户 related docs in `lsym`.

## Current Memory

- 自有资金池相关设计文档当前主要沉淀在 `docs/specs/` 和 `docs/superpowers/specs/`。
- 这部分属于专项设计/计划资料，使用前必须回到 `../slhy/` 源码确认当前落地状态。
- 如果后续形成稳定接口交接口径，应补一份 `docs/*_API.md` 并从本主题页链接。

## Existing Docs

| 文档 | 用途 |
|------|------|
| `../docs/specs/2026-05-08-self-fund-development-guide.md` | 自有资金相关开发指引 |
| `../docs/superpowers/specs/2026-04-22-special-account-recharge-design.md` | 特殊账户充值设计 |
| `../docs/superpowers/specs/2026-04-23-self-fund-account-design.md` | 自有资金账户设计 |
| `../docs/superpowers/plans/2026-04-23-self-fund-account-plan.md` | 自有资金账户实施计划 |

## Maintenance Rule

- 不把设计稿里的未落地内容写成当前事实。
- 新增业务结论时标明 `current`、`historical` 或 `needs-source-check`。
- 如果清结算、consume、front 三方接口出现固定调用口径，优先沉淀到 `docs/` 下的接口文档，再在这里做摘要。
