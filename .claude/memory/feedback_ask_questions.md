---
name: Always ask questions via AskUserQuestion tool
description: User wants ALL questions asked via the interactive AskUserQuestion tool, never as inline text questions.
type: feedback
---

**Always** use the `AskUserQuestion` tool when asking this user anything — single questions, multiple questions, clarifications, confirmations, yes/no, open-ended. Never embed questions inline in a text response.

**Why:** User stated this twice on 2026-04-19 — first said "can you ask them in prompt?" after an inline question list, then reinforced "whenevr you have questions for me always ask them in prompts." The interactive UI is noticeably faster for them to answer.

**How to apply:**
- Any time the response would end with a question (or contain one the user must answer before you proceed), use `AskUserQuestion` instead of inline text.
- For yes/no, provide 2 options (Yes / No) rather than skipping the tool.
- For open-ended questions, still use the tool — the "Other" option auto-appears for free-text.
- Batch up to 4 questions per call when they're all needed before you can proceed.
- Exception: rhetorical questions inside explanations ("why do we do this? because...") are fine inline.
