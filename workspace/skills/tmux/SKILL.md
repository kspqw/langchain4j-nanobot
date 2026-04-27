---
name: tmux
description: Terminal multiplexer for managing persistent sessions.
metadata: {"nanobot":{"emoji":"🖥️","requires":{"bins":["tmux"]},"install":[{"id":"brew","kind":"brew","formula":"tmux","bins":["tmux"],"label":"Install tmux (brew)"},{"id":"apt","kind":"apt","package":"tmux","bins":["tmux"],"label":"Install tmux (apt)"}]}}
---

# Tmux Skill

Use tmux for persistent terminal sessions.

## Common Commands

List sessions:
```bash
tmux ls
```

Create new session:
```bash
tmux new -s mysession
```

Attach to session:
```bash
tmux a -t mysession
```

Send command to session:
```bash
tmux send-keys -t mysession "echo hello" Enter
```

Kill session:
```bash
tmux kill-session -t mysession
```

## Session Patterns

For long-running tasks, use a detached tmux session:
```bash
tmux new -d -s build "cd proj && make"
```

Then check output later:
```bash
tmux capture-pane -t build -p | tail -20
```