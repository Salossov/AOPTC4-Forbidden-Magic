# Forbidden Magic port

- Target Minecraft 1.21.1, NeoForge 21.1.252 and Java 21. The permanent namespace is `forbidden`.
- Reference: Forbidden Magic 1.7.10-0.575, SHA-256 `d6b239a05a630cfb3b6de01fe14a8674ec14ef8fb34ea75d1ad45b79565d8636`.
- Use only public Thaumcraft APIs, registry IDs, tags and platform events. Propose any missing core contract before implementing it. K01-K03 and T01 were approved and implemented; the T01 receiver was also checked in a real dedicated server on 2026-10-25. K04 was approved on 2026-10-26 for local-only ParticleApi.nodeBolt; reservoirBubble is a legacy no-op.
- Preserve release behavior, numeric values and random-call order. Individually approved fixes F01-F05 are recorded in the private parent docs; D10 requires separate agreement for other defects.
- D9 excludes four unreachable classes, not the active anonymous creative tabs or physical meta variants. D13 preserves unused resources without activating unused mechanics.
- Startup gates must match the release: the cage stays registered when disabled; Gluttony items stay registered; `noLust` removes Luxuria and the collar, but changes shard 4 without changing its ID.
- Keep references, decompilation, private tools, reports, decision logs, caches, dependency JARs and worlds outside Git and the gameplay JAR. Tests stay in separate source sets and out of the gameplay JAR.
- Use PowerShell and `gradlew.ps1`; do not launch CMD or batch files. Do not use subagents unless explicitly requested.
- Each implementation batch, including corrective batches and verification-script implementation, must use its own branch and Git worktree under this repository's `.worktree/` directory, as explicitly requested by the user. Read-only audits and handoff-only tasks do not need a new worktree. Keep worktrees out of Git/artifacts; ensure generators, dependencies and reports target the selected checkout rather than the primary checkout. Preserve unrelated WIP.
- Local Git and commits of completed stages are authorized by D1. Push, publication and user-world changes are not authorized. On 2026-10-25 the user explicitly authorized gameplay acceptance and accepting the EULA for isolated test worlds, with no visible windows, focus or sound. Desktop interaction/capture remains unavailable in the current environment.
- Explain progress and results in Russian. Write AGENTS.md in English.
- Report code, build, automated rules, server acceptance and client acceptance separately. A successful build does not establish gameplay parity.
- Scope searches to the subsystem under investigation. Do not alter unrelated work.
