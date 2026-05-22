# Everything Claude Code for Codex

## What was installed

The following files were added to this project:

- `AGENTS.md`
- `.codex/config.toml`
- `.codex/AGENTS.md`
- `.codex/agents/explorer.toml`
- `.codex/agents/reviewer.toml`
- `.codex/agents/docs-researcher.toml`

This gives the project an ECC-style Codex setup with:

- stronger project instructions
- multi-agent role definitions
- a starter MCP server configuration

## Why this was installed project-locally

The upstream repository normally syncs its Codex assets into `~/.codex` and into the current repo. GitHub access from this machine failed during clone, so a safe local install was applied to this project instead.

This means the setup is usable now in this workspace, without overwriting your existing global Codex config.

## How to use it in Codex

From this project directory, ask Codex to work with the ECC conventions. Useful examples:

1. "先做一个计划，再实现这个功能，并补测试。"
2. "先像 reviewer 一样审查这个改动，优先找 bug 和安全问题。"
3. "先查官方文档确认 API，再改代码。"
4. "先做只读探索，告诉我这个功能的真实调用链。"

The role files are here if you want to reference them directly:

- `.codex/agents/explorer.toml`
- `.codex/agents/reviewer.toml`
- `.codex/agents/docs-researcher.toml`

## Recommended workflow

1. Explore before editing for unfamiliar code.
2. Plan before larger changes.
3. Implement with tests.
4. Run targeted verification.
5. Ask for a review pass before merging.

## Optional global follow-up

If you later regain GitHub access, the ideal follow-up is to clone the upstream repository and sync its latest `.codex` assets into `~/.codex` so every project benefits from the same baseline.

Until then, this project already has a working ECC-style Codex setup.
