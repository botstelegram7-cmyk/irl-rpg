# IRL RPG — Step 2 Delivery Report

**Step 2: Advanced Quest System, Skill Trees & Character Progression**
Build verified: `:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` ✅ 41/41 · `:app:assembleDebug` ✅ · `:app:assembleRelease` ✅

---

## 1. What was built

### 1.1 Advanced quest system
| Capability | Where | Notes |
| --- | --- | --- |
| Five tracking styles | `domain/model/TrackingKind` | time, count, milestone, performance, custom (unchanged from Step 1, still universal) |
| Three quest **shapes** | `domain/model/QuestKind`, `QuestDraft`, `QuestEditorScreen` | single goal, **multi-step**, **long-term challenge** |
| Description, difficulty, deadline | `QuestEntity` + editor "Shape of the quest" card | difficulty suggests a reward (Easy 40 / Medium 60 / Hard 110 / Epic 180) but a reward you tuned yourself is never overwritten |
| Multi-step steps | `quest_steps` table, `QuestStepDao` | per-step title, target, required/optional; progress shown as "2 of 4 steps" |
| Long-term challenges | `PeriodKeys.FLEX_KEY` | everything accumulates under one key; `startedEpochDay` anchors progress |
| Search | `QuestsViewModel.setQuery` | matches title, description, notes and category |
| Filters & sorting | `QuestFilter` (All / Today / Active / Challenges / Multi-step / Paused), `QuestSort` (Progress / Newest / **Deadline** / XP / A-Z) | deadline sort puts dated quests first, soonest first |
| Editing, pause/resume, duplicate, delete | `QuestsViewModel` | deleting removes the quest's logs so XP totals stay honest |
| Completion history | `ui/history`, `HistoryRepository` | day-grouped audit trail with XP and level-up flags per entry |

### 1.2 Recurring quests without duplicate rewards
- Every period gets an **instance row** (`quest_instances`), unique on `(questId, periodKey)`, inserted with `INSERT OR IGNORE`.
- The completion bonus is gated on `quest_instances.status`, so it is paid **at most once per period**.
- Multi-step quests pay each step from its own share of the reward budget (`xpReward / requiredSteps`), and a step that is already done pays 0.
- Verified by tests: `StepTwoEngineTest` (overshoot, re-log, next-day reset) and `GameEngineTest` (Step 1 behaviour intact).

### 1.3 Character progression
- `LevelRules(base, step)` — default 90/60; base 20–400, step 0–300 (SettingsStore).
- Levels, rank titles (Wanderer → Ascendant, max level 60) and progress bars are **derived from stored XP**, so changing the rules re-maps the display and never rewrites progress.
- Level-up sequence (`ui/components/RewardOverlays.kt`) fires only when the engine reports a real level change — there is no UI-only path to it.

### 1.4 Universal skill tree
- Custom skill creation/editing: name, icon, description, colour, parent, linked attribute, mastery requirement, unlock rule.
- Unlock rules: **Root**, **Parent mastery**, **Parent progress**, **Lifetime XP** — each with a plain-language explanation when locked ("Unlocks at 120 XP in Reading · you have 40 XP") plus a progress bar towards the requirement.
- Node states: Locked / Available / In progress / Mastered, all derived from the linked attribute's earned XP.
- 5 seeded branches (Reading, Programming, Fitness, Writing, Learning) — every node is editable and deletable; deleting a parent **detaches** children (they become branch roots) instead of cascading.
- Rendering: `SkillTreeLayout.place()` (pure Kotlin, unit-testable) + one edge `Canvas` + nodes in a single `graphicsLayer` transform with `detectTransformGestures` pan/zoom (0.5×–1.6×). No game engine, no per-node layout work.

### 1.5 Boss challenges (no combat)
- Optional, customisable goals with multiple objectives.
- Objective metrics: `LINKED_QUEST` (sum of that quest's logged amounts since the challenge started) or `MANUAL` (explicit ticks).
- Required objectives gate completion; optional ones are nudges.
- Reward is paid **exactly once** and re-checked inside the paying transaction; a victory overlay plays after it is paid.
- 5 templates (each copied into an editable user row — never a shared row) + a custom builder.
- Archive / reactivate / delete supported; deleting keeps earned XP in history.

### 1.6 Achievements & rewards
- 27 achievements across 5 categories (Progress, Consistency, Mastery, Challenges, Creator) and 3 tiers (Bronze/Silver/Gold).
- Progress and earned state are computed from **recorded data only** (logs, quests, attributes, skill nodes, boss rows, progression history). Nothing is seeded, nothing is pre-marked complete.
- First launch after the update calls `prime()`: history is recorded **silently** (the app reports the count, not 11 pop-ups), and only genuinely new unlocks are announced afterwards.
- 14 cosmetics (7 frames + 7 titles) each granted by exactly one achievement; `frame_ivory` is the free default. Cosmetics are vanity only — they never gate quests, XP or skills, and nothing is purchasable.

### 1.7 UI, animation and accessibility
- Light claymorphism preserved: ivory background, pastel surfaces, layered clay cards, tactile buttons, consistent 20 dp gutters and ClayShape radii (18/16/22/28/34/pill).
- New animations are transform/alpha only (level-up overlay, achievement banner queue, victory sequence, progress-bar tweens) — no 3D, no flashing, nothing that runs on a timer.
- Respects system font scaling and keeps minimum 44 dp touch targets; all new strings live in the same terse, readable style as Step 1.

---

## 2. Data integrity

### 2.1 Schema migration (v1 → v2)
`MIGRATION_1_2` is **additive**:
- `quests` += description, difficulty (default `MEDIUM`), deadlineEpochDay, questKind (default `STANDARD`), durationDays, startedEpochDay (backfilled from `createdAt`),
- `quest_logs` += stepId,
- `player_stats` += displayTitle, cosmeticFrame (default `frame_ivory`),
- new tables (`quest_steps`, `quest_instances`, `skill_nodes`, `achievements_earned`, `boss_challenges`, `boss_objectives`) start **empty**.

No column is dropped, no row is rewritten, and the destructive fallback was removed. If the migration fails, the app fails loudly rather than wiping data.

### 2.2 Migration test
`MigrationTest` writes a real Step 1 database (Step 1 schema, with XP 1840, a quest, a log, an attribute, a category and a level-up event), opens it with the real v2 database and the real migrations, then asserts:
- XP, name, quest, log, attribute and progression rows all survive byte-for-byte,
- new columns carry their documented defaults, `startedEpochDay` is backfilled,
- every Step 2 table is empty (no fake achievements, no fake challenges).

### 2.3 Anti-abuse summary
| Risk | Guard | Test |
| --- | --- | --- |
| Re-logging / re-opening a quest | no XP path exists outside `GameEngine.logQuest` | `GameEngineTest`, `StepTwoEngineTest` |
| Overshooting the target | post-completion entries award 0 XP | `StepTwoEngineTest` |
| Same period completed twice | unique `(questId, periodKey)` + `status` gate | `StepTwoEngineTest` |
| Step XP stacking | per-step budget + finished steps pay 0 | `StepTwoEngineTest` |
| Boss reward twice | `status`/`xpAwarded` re-checked in the paying transaction | `StepTwoEngineTest` |
| Editing a quest to re-settle progress | XP rows are immutable; edits never touch `quest_logs` | `StepTwoEngineTest` |
| Retroactive achievements | nothing is marked earned without a matching real record | `AchievementEngine`/catalog design + migration test |

---

## 3. Test results

Run as two invocations to stay inside the sandbox's RAM budget (the whole suite in one process exhausts metaspace on this machine; a normal dev machine can run `gradle test` in one go).

| Suite | Tests | Result |
| --- | --- | --- |
| `GameEngineTest` (Step 1 core, Robolectric + Room) | 11 | ✅ pass |
| `LevelCurveTest` | 6 | ✅ pass |
| `XpEngineTest` | 6 | ✅ pass |
| `StreakCalculatorTest` | 5 | ✅ pass |
| `PeriodKeysTest` | 4 | ✅ pass |
| `StepTwoEngineTest` (new) | 8 | ✅ pass |
| `MigrationTest` (new) | 1 | ✅ pass |
| **Total** | **41** | **41 passed, 0 failed** |

`StepTwoEngineTest` covers: multi-step logging and a single completion bonus · recurring instance de-duplication (daily vs ISO-week) · overshoot/re-log/next-day reset · long-term accumulation under one key · boss reward paid once and only when required objectives are met · boss progress ignoring pre-challenge history · skill unlock prerequisites and lock explanations · custom level rules not resetting stored XP.

---

## 4. Files added or changed in Step 2

**Data layer:** `data/local/entity/Step2Entities.kt`, `data/local/entity/Entities.kt`, `data/local/Migrations.kt`, `data/local/IrlRpgDatabase.kt`, `data/local/DatabaseSeeder.kt`, `data/local/dao/Step2Daos.kt`, `data/local/dao/LogDao.kt`, `data/local/dao/QuestDao.kt`, `data/local/dao/PlayerDao.kt`, `data/prefs/SettingsStore.kt`

**Domain:** `domain/model/Step2Models.kt`, `domain/model/QuestDraft.kt`, `domain/model/ViewModels.kt`, `domain/game/GameEngine.kt`, `domain/game/BossEngine.kt`, `domain/game/LevelProgression.kt`, `domain/game/LevelCurve.kt`, `domain/game/SkillTreeEngine.kt`, `domain/game/AchievementCatalog.kt`, `domain/game/AchievementEngine.kt`, `domain/game/ActivitySnapshot.kt`, `domain/game/CosmeticCatalog.kt`, `domain/game/BossTemplates.kt`

**Repositories:** `data/repository/QuestRepository.kt`, `QuestProgress.kt`, `PlayerRepository.kt`, `DashboardRepository.kt`, `AchievementRepository.kt`, `SkillTreeRepository.kt`, `BossRepository.kt`, `ProgressSync.kt`, `HistoryRepository.kt` (the obsolete `ProgressRepository.kt` draft was deleted)

**UI:** `ui/components/RewardOverlays.kt`, `ui/components/StepLogSheet.kt`, `ui/design/ClaySkillTree.kt`, `ui/design/ClayComponents.kt`, `ui/skills/SkillTreeScreen.kt`, `ui/skills/SkillTreeViewModel.kt`, `ui/skills/SkillsScreen.kt`, `ui/boss/BossesScreen.kt`, `ui/boss/BossViewModel.kt`, `ui/achievements/AchievementsScreen.kt`, `ui/achievements/AchievementsViewModel.kt`, `ui/history/HistoryScreen.kt`, `ui/history/HistoryViewModel.kt`, `ui/profile/CustomizeScreen.kt`, `ui/profile/CustomizeViewModel.kt`, `ui/profile/ProfileScreen.kt`, `ui/dashboard/DashboardScreen.kt`, `ui/dashboard/DashboardViewModel.kt`, `ui/quests/QuestEditorScreen.kt`, `ui/quests/QuestEditorViewModel.kt`, `ui/quests/QuestsScreen.kt`, `ui/quests/QuestsViewModel.kt`, `ui/navigation/Destinations.kt`, `ui/IrlRpgRoot.kt`, `IrlRpgApp.kt`, `di/AppContainer.kt`

**Tests:** `app/src/test/java/com/irlrpg/app/data/StepTwoEngineTest.kt`, `app/src/test/java/com/irlrpg/app/data/MigrationTest.kt`

---

## 5. Build status

| Artifact | Command | Result |
| --- | --- | --- |
| Kotlin compile | `:app:compileDebugKotlin` | ✅ successful, 0 errors |
| Unit tests | `:app:testDebugUnitTest` (two batches) | ✅ 41/41 |
| Debug APK | `:app:assembleDebug` | ✅ `app-debug.apk` — 11,053,229 bytes, sha256 `40be0838ef89348139ec5ddd622b97091446771c85f4b1a2fc372c26a3c93963` |
| Release APK (unsigned) | `:app:assembleRelease` | ✅ `app-release-unsigned.apk` — 7,455,293 bytes, sha256 `b840bb9fb4ced9ae56937d38b48b8af3a42976014271c31430145fb85eb6be8c` |

The debug APK is published in `prebuilt/app-debug.apk` and attached to the GitHub release, so it can be installed on any Android 7.0+ (API 24) device without a build step.

---

## 6. Known limitations

1. **No instrumented (on-device UI) tests** — this environment has no emulator, so verification is JVM/Robolectric plus a successful release build. Layout polish was checked by inspection against the design tokens.
2. **Level threshold editor UI** is not exposed in Settings yet; the rules are read from `SettingsStore` (defaults applied, `resetLevelRules()` available) and are fully unit-tested, but exposing the sliders is deferred.
3. **Boss/skill achievements count progress, not time-weighted analysis** — e.g. "3 challenges" counts completions; there is no per-objective trend chart.
4. **Skill-tree pan/zoom is not persisted** between sessions (it resets to a fitted view).
5. **Unicode emoji** are used for icons/nodes; on very old devices the fallback glyph set may differ.
6. **Achievement banner queue** shows one unlock at a time (3.2 s each); unlocking many at once after a big import takes a few seconds to cycle.
7. **No cloud sync / export** — still offline-first by design, as requested.

---

## 7. What is explicitly *not* in Step 2

- No Unity, Unreal or real-time 3D engine — the skill tree is a Compose canvas.
- No combat gameplay in boss challenges.
- No paid API, no AI dependency, no background service, no wakeups.
- No achievements, XP or progress seeded as "already done".
- Step 3 has **not** been started, per the delivery plan.
