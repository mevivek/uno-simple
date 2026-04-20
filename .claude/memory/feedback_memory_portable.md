---
name: Memory lives inside the project
description: All persistent memory for UNO Simple is stored in `.claude/memory/` inside the project repo, not at the user level — so it travels across machines.
type: feedback
---

All project memory lives in `.claude/memory/` inside the repo. This includes the `MEMORY.md` index and all individual entry files (user, feedback, project, reference types).

**Why:** User stated on 2026-04-19: "keep you memory inside the project so that if i work on this project on another machine, everything will be avilable there." The default user-level memory path (`~/.claude/projects/<cwd-slug>/memory/`) is machine-local and doesn't travel. Putting memory in the project keeps it in git and follows the code.

**How to apply:**
- When saving new memory for this project, write to `E:\personal hobby projects\UNO Simple\.claude\memory\*.md` and update `E:\personal hobby projects\UNO Simple\.claude\memory\MEMORY.md`.
- Do NOT write new memory to `C:\Users\meviv\.claude\projects\E--personal-hobby-projects-UNO-Simple\memory\` — that location is deprecated for this project.
- If the user-level memory directory still contains old files, they are stale mirrors. The in-project copies are the source of truth.
- CLAUDE.md at the project root instructs every new session to read `.claude/memory/MEMORY.md` first, so portability is preserved.
- When making decisions that should persist across sessions (preferences, architectural rules, user profile facts), always save them here — not just in session context.
