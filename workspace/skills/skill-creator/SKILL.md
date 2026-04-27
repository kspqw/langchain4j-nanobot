---
name: skill-creator
description: Create new nanobot skills from templates.
available: false
metadata: {"nanobot":{"emoji":"🛠️"}}
---

# Skill Creator

Create new skills for nanobot using templates.

## Create a New Skill

1. Create directory: `mkdir -p skills/newskill`
2. Create SKILL.md file with metadata frontmatter
3. Add skill description and usage examples

## SKILL.md Template

```yaml
---
name: skillname
description: What the skill does.
metadata: {json}
---

# Skill Name

Describe the skill here.

## Usage

```bash
some command
```

## Parameters

- param1: description
```

This skill requires additional setup or dependencies.