---
name: Never use work-related connections/MCPs on the UNO project
description: UNO Simple is a personal hobby project — do not call any work-related MCP servers (Jira, Confluence, Couchbase, BigQuery, Gmail, internal source-search, or any day-job-specific connector) from this project.
type: feedback
---

**Hard rule:** When working in the UNO Simple project directory, do NOT call any work-related MCP tools or connections. This is a personal hobby project and must stay fully separated from the user's day job.

**Why:** User stated on 2026-04-19: "make sure you never call my work related connections and mcps." Personal project shouldn't touch work data, credentials, or remote systems. Also prevents accidental data leakage or confused context mixing.

**How to apply:**

- **Denied MCP namespaces** (enforced in `.claude/settings.json` via `permissions.deny`):
  - Atlassian / Jira / Confluence
  - Gmail
  - Internal dev source-code search
  - Couchbase
  - BigQuery
- **Also avoid:** anything that looks like a work-specific connector, API, or credential — never fetch/push/search work tickets, documents, databases, or repos from UNO context.
- If the user ever asks for something that would require a work tool, stop and ask via `AskUserQuestion` whether they really want to cross the streams — do not silently reach for work tools.
- General-purpose tools (web search, web fetch for public docs like kotlinlang.org, pub.dev) are fine.

**If a work tool slips into the session anyway** (e.g. someone ran `/something` that calls Jira), abort the call, flag to the user, and update `.claude/settings.json` to deny it if not already denied.
