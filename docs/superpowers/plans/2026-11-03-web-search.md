# Web Search Answer Sheet Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans or implement directly. Checkbox steps for tracking.

**Goal:** PC static web app that loads the same `data/encounters.json` for search, detail, nextId navigation, recommend borders, and chance % formatting. No OCR/overlay/AI.

**Architecture:** Vanilla ES modules under `web/`. Seed synced to `web/encounters.json`. Serve via local HTTP.

**Tech Stack:** HTML/CSS/JS only. Node `--test` for normalize/effects.

## Global Constraints

- Same seed schema as Android.
- Bunker CSS tokens from design-system MASTER.
- Empty seed load must show error, not blank sheet.

---

### Task 1: Static shell + domain ports

**Files:** `web/index.html`, `web/styles.css`, `web/normalize.js`, `web/effects.js`, `web/app.js`

- [ ] Search list + detail + recommend toggle + nextId
- [ ] Attribution footer

### Task 2: sync-seed

- [ ] Copy to `web/encounters.json`

### Task 3: Tests + README + push

- [ ] `node --test web/test.mjs`
- [ ] README Web section
- [ ] Commit push main
