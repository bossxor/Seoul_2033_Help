# Seed Bulk (Basic Encounters Batch 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand `data/encounters.json` with 30+ basic-category nodes so search/OCR matching is useful beyond the wild-dogs sample.

**Architecture:** Keep a wiki text snapshot under `data/raw/`. A Python heuristic converter (`scripts/seed-from-wiki.py`) splits `###` sections into EncounterNode drafts (choices as bullets; unclear outcomes as `text` effects). Curated nodes (e.g. wild-dogs) stay hand-authored and win on id merge. Sync into Android assets via `scripts/sync-seed.ps1`.

**Tech Stack:** Python 3 stdlib only; existing kotlinx.serialization EncounterSeedFile schema; PowerShell sync.

## Global Constraints

- Seed schema matches Android `EncounterSeedFile` / sealed `Effect` (`type` discriminator).
- Attribution: personal non-commercial; source namu/readonly wiki.
- Do not invent precise chance rates when wiki is vague — use `text` effect.
- Keep existing `wild-dogs` / `wild-dogs-pass` ids and recommend flags.
- No runtime AI / Web in this plan.

---

### Task 1: Raw snapshot + SOURCE note

**Files:**
- Create: `data/raw/SOURCE.md`
- Create: `data/raw/basic-encounters-readonly.txt`

- [ ] **Step 1:** Copy readonly.wiki dump of 기본 인카운터 into `data/raw/basic-encounters-readonly.txt`
- [ ] **Step 2:** Write `SOURCE.md` with URL, license note (CC BY-NC-SA personal use), fetch date
- [ ] **Step 3:** Commit `docs: raw wiki snapshot for basic encounters`

---

### Task 2: Converter script

**Files:**
- Create: `scripts/seed-from-wiki.py`
- Create: `data/encounters.draft.json` (generated)

**Interfaces:**
- Consumes: `data/raw/basic-encounters-readonly.txt`
- Produces: JSON `{version:1, nodes:[...]}` with fields `id,title,story,body,choices,source,updatedAt`
- Choice: `{text, effects, nextId, recommend}`
- Effect heuristics: `resource` (체력/멘탈/돈 ±N), `item`/`ability`/`status` via `획득`, `condition` from `(...)`, else `text`

- [ ] **Step 1:** Implement section split on `^### \d+(\.\d+)*\. `
- [ ] **Step 2:** Parse `- choice - outcome` lines; skip empty / nested ###
- [ ] **Step 3:** Slugify Korean titles to ascii-ish ids (`slug`)
- [ ] **Step 4:** Run: `python scripts/seed-from-wiki.py` → writes draft
- [ ] **Step 5:** Commit `feat: wiki-to-seed draft converter`

---

### Task 3: Batch-1 curated merge (30+)

**Files:**
- Modify: `data/encounters.json`
- Modify: Android assets via sync

- [ ] **Step 1:** Select ≥30 simpler sections (pickup / short choice lists); prefer converter output
- [ ] **Step 2:** Merge: curated wild-dogs overwrite same ids; drop empty-title junk
- [ ] **Step 3:** Run `.\scripts\sync-seed.ps1`
- [ ] **Step 4:** Commit `feat: expand basic encounter seed batch 1`

---

### Task 4: Verify + docs + push

**Files:**
- Modify: `README.md` (seed fill section)

- [ ] **Step 1:** `gradlew :app:testDebugUnitTest` (via C:\tmp\s2033 if UNC fails)
- [ ] **Step 2:** Device smoke: search shows many titles
- [ ] **Step 3:** README seed procedure
- [ ] **Step 4:** `git push origin main`

## Spec coverage

| Spec | Task |
|------|------|
| Shared seed schema | 2–3 |
| Staged seed fill | 3 |
| Attribution | 1, 4 |

## Placeholder scan

None.
