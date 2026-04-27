---
name: clawhub
description: A GitHub CLI extension for AI-powered pull request reviews.
available: false
metadata: {"nanobot":{"emoji":"🦀","requires":{"bins":["gh"]},"install":[{"id":"gh","kind":"gh","command":"gh extension install hongminhee/gh-pr-comment","label":"Install clawhub (gh extension)"},{"id":"manual","kind":"manual","command":"gh auth refresh -s repo","label":"Authenticate with GitHub"}]}}
---

# Clawhub

AI-powered pull request review using GitHub CLI.

## Prerequisites

1. Install: `gh extension install hongminhee/gh-pr-comment`
2. Authenticate: `gh auth refresh -s repo`

## Usage

Review a PR:
```bash
gh pr review --repo owner/repo 55
```

Add comment:
```bash
gh pr comment 55 --body "Great work!"
```

This skill requires additional setup. Try installing dependencies first.