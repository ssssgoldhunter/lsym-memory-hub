# Topic Index

> Status: current
> Last updated: 2026-05-12

This directory is the LLM-facing topic layer for `lsym-memory-hub`.

Topic pages collect the current working context for one problem area and point to source files, design docs, requirements, bugs, and historical notes. They are not a replacement for source code. Use them to find the right starting point quickly, then verify current behavior in `../slhy/`.

## Current Topics

| Topic | File | Use When |
|------|------|----------|
| Consume refund | `consume-refund.md` | 消费退款、比例退款、逐笔退款、04 退款、兜底分摊、同收款卡合并 |
| Account change | `account-change.md` | 账户余额更新、MAC/CAS、账户变动明细、task 旧路径排查 |
| Self fund account | `self-fund-account.md` | 自有资金池、特殊账户充值、自有资金账户开发计划 |

## How To Maintain

- Add one topic page when a subject appears repeatedly across requirements, bugs, conversation logs, and source references.
- Keep topic pages short enough to scan.
- Put stable conclusions near the top.
- Put historical links near the bottom.
- Mark uncertain or old claims with `needs-source-check` or `historical`.
- If a topic becomes high-frequency, add a pointer from `workflow/PROJECT_MEMORY.md`.
