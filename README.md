<div align="center">

# ⚔️ IRL RPG

**Turn real life into an RPG.**

A premium, offline-first Android app that turns real-life activities, skills and goals into an RPG-style progression system — for students, readers, writers, developers, athletes, artists and professionals alike.

Built with **Kotlin · Jetpack Compose · Room · Claymorphism UI**

</div>

---

## What is IRL RPG?

IRL RPG is not a habit tracker with XP sprinkled on top. It is a personal progression system for real life:

- **You define everything** — your own quests, categories, targets, units and skills. Nothing is hardcoded around studying.
- **Everything counts** — reading 25 pages, writing 500 words, coding for 90 minutes, finishing a novel, hitting a target score, practising guitar.
- **Progress is honest** — XP is only ever granted for recorded activity. No XP for opening the app, no fake achievements, no invented statistics.
- **Your data stays yours** — a local SQLite database, no account, no ads, no network calls, no paid APIs.
- **It looks and feels premium** — a warm ivory Claymorphism design system with pastel clay surfaces, soft ambient shadows, tactile buttons and hand-drawn charts (no 3D engine, no heavy assets).

## Step 2 — what is included

Everything in Step 1 still works exactly as before; Step 2 adds depth without changing a single stored XP value.

- **Advanced quest system** — five tracking styles (time, count, milestone, performance, custom) plus three *shapes*: single goal, **multi-step**, and **long-term challenge**. Every quest can carry a description, difficulty, optional deadline and configurable XP reward, and can be searched, filtered, sorted, edited, paused, resumed, duplicated and deleted. Completion history is kept per entry.
- **Recurring quests done safely** — daily and weekly quests materialise an *instance* row per period (`quest_instances`, unique on quest + period), so a completion bonus is paid at most once per period and re-opening or re-logging can never mint XP.
- **Character progression** — level thresholds are configurable (base 90 / step 60 by default, 20–400 / 0–300). Ranks run Wanderer → Ascendant. Levels are derived from stored XP, so changing the rules never resets or rewrites your progress.
- **Universal skill tree** — create, edit and nest your own skill nodes (Reading → Comprehension → Analysis → Critical Thinking, Programming → Fundamentals → Projects → Advanced Development, and anything you invent). Nodes unlock from parent mastery or lifetime XP, show locked/available/in-progress/mastered states, and always explain *why* something is locked and how far along you are. Rendered with a lightweight custom Compose canvas — no game engine.
- **Boss challenges** — optional, customisable real-life goals with several objectives (some linked to your quests, some manual). Progress comes from the activity log, the reward is paid exactly once, and completion plays a victory sequence. There is no combat gameplay.
- **Achievements & cosmetics** — 27 achievements across 5 categories, all computed from **recorded activity** (never hard-coded, never seeded). Unlocking one grants an avatar frame or a title; cosmetics are purely visual and never gate real-life progress.
- **History** — a day-grouped audit trail of every logged entry with its XP and level-up flags.

## Step 1 — what is included

| Deliverable | Status |
| --- | --- |
| Claymorphism design system (light, premium) | ✅ |
| Polished, functional Home Dashboard | ✅ |
| Character level, XP, rank and skill attributes | ✅ |
| Custom quest creation + completion flow (5 tracking styles) | ✅ |
| Local persistence for all core data | ✅ |
| Basic analytics summary from real recorded data | ✅ |
| Navigation: Dashboard · Quests · Skills · Analytics · Profile | ✅ |
| Working Android build + unit tests | ✅ |

## The five tracking styles

| Style | Example | Behaviour |
| --- | --- | --- |
| ⏱️ Time-based | Read for 30 minutes | XP scales with minutes logged; target resets daily |
| 🔢 Count-based | Read 25 pages, write 500 words | XP scales with quantity; target resets per period |
| 🏁 Milestone | Finish a book, ship a project | Completes once, awards once |
| 🎯 Performance | Reach a score / accuracy target | Numeric target you control |
| 🧩 Custom | Anything with your own unit | Your unit, your target, your reward |

Schedules: every day, weekdays, chosen days, weekly target, or “any time” (accumulates without pressure).

## XP rules (and anti-abuse)

**Step 2 additions:** a multi-step quest pays each step from its own share of the reward budget and adds the completion bonus only once per period; a long-term challenge accumulates under one period key; a boss challenge pays its reward once and only when every required objective is verified against the log.



```
Level 1 → 2   : 150 XP          Level 10 → 11 : 690 XP
Level 2 → 3   : 210 XP          Level 20 → 21 : 1290 XP
cost(L → L+1) = 90 + 60 × L      cumulative = (L−1)(90 + 30L)
```

- Progress XP is proportional: logging 25 pages once equals logging 1 page 25 times.
- A **completion bonus** (half the quest reward, minimum 10 XP) is granted exactly once per period, on the entry that crosses the target.
- After a period's target is reached, further entries are still stored as history but award **zero XP**.
- Opening the app, navigating, or creating quests never awards XP.
- **Undo** and **reset** recompute XP from the surviving rows, so totals can never drift.

## Step 2 screens

| Screen | What you do there |
| --- | --- |
| Skill tree (`skill_tree`) | Pan/zoom the tree, tap a node for mastery and unlock requirements, add root skills, child skills, edit or remove nodes |
| Challenges (`challenges`) | Start from 5 templates or build your own multi-objective challenge, log manual objectives, complete, archive, delete |
| Achievements (`achievements`) | Browse earned + locked achievements with live progress and earn dates |
| History (`history`) | Filter by quest and read every recorded entry, grouped by day |
| Customize (`customize`) | Equip unlocked frames and titles; locked entries show the achievement and its progress |

## Screens

| Screen | What it does |
| --- | --- |
| **Home** | Character card with XP ring and rank, today's overview, today's quests with one-tap logging, weekly chart, recent achievements, analytics shortcut |
| **Quests** | Full quest board with filters (all/today/active/paused), category filters, sorting, per-quest menu (edit, duplicate, pause, delete) and a category manager |
| **Quest editor** | Create/edit any activity: icon, title, category, tracking style, target + unit, schedule, XP reward, skill, notes |
| **Quest detail** | Live progress, quick logging stepper, full entry history with undo, reset and delete |
| **Skills** | XP per attribute derived from real entries, share bars, add/rename/remove custom skills |
| **Analytics** | Today/week/month/year summaries, weekly trend chart, today vs yesterday, goal progress, category split, reading stats, consistency |
| **Profile** | Character, lifetime stats, progression history, data summary, rename |

## Tech stack

- **Kotlin 2.0.21**, **Jetpack Compose** (BOM 2024.10.01, Material 3 base), **Navigation Compose**
- **Room 2.6.1** (SQLite) with KSP codegen — single source of truth for all state
- **Coroutines / Flow** state streams, `StateFlow` per screen, `stateIn(WhileSubscribed)`
- **MVVM + clean layering**: `ui → domain → data`, with a hand-rolled `AppContainer` for DI (no reflection, fast cold start)
- Compose animations only (no Lottie, no Unity, no real-time 3D) — the 3D clay look comes from layered gradients, tinted elevation shadows, inner bevels and custom `Canvas` art
- Offline-first: no permissions at all in the manifest, no background services

## Project structure

```
app/src/main/java/com/irlrpg/app/
├── core/            DateX, TodaySource (day rollover handling)
├── data/
│   ├── local/       Room database, entities, DAOs, seeder
│   ├── prefs/       tiny SharedPreferences wrapper
│   └── repository/  QuestRepository, PlayerRepository, DashboardRepository,
│                    AnalyticsRepository, MetricsAggregator, QuestProgress
├── di/              AppContainer (single object graph)
├── domain/
│   ├── game/        LevelCurve, XpEngine, GameEngine (transactions), PeriodKeys, StreakCalculator
│   └── model/       TrackingKind, ScheduleKind, UnitKind, AttributeKind, view models
└── ui/
    ├── design/      clay modifiers, components, charts, nav glyphs, scaffold
    ├── theme/       palette, typography, Material colour mapping
    ├── components/  celebration overlay, quick-log sheet, toast, confirm dialog
    └── dashboard|quests|skills|analytics|profile/
```

## Build & run

Requirements: **JDK 17**, Android SDK **API 35** (build-tools 35.0.0), Gradle 8.10.2 (wrapper is included).

```bash
git clone https://github.com/botstelegram7-cmyk/irl-rpg.git
cd irl-rpg

# Prebuilt APK (no build needed): prebuilt/app-debug.apk
# or download the Step 2 build directly:
#   https://github.com/botstelegram7-cmyk/irl-rpg/releases/tag/v1.0-step2
#   (irl-rpg-step2-debug.apk · 11,053,229 bytes · sha256 40be0838ef89348139ec5ddd622b97091446771c85f4b1a2fc372c26a3c93963)

# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug

# Unit tests (progression, XP rules, streaks, Room persistence)
./gradlew testDebugUnitTest
```

No `local.properties` needed if `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) is set. There are no API keys, no accounts and no network dependencies.

## Testing

`app/src/test/` contains JVM + Robolectric tests that cover the parts that must never be wrong:

- level curve maths, exact boundaries and rank titles
- XP proportional awards, capped over-logging, one-time milestone and bonus behaviour
- streak and consistency calculations that never punish missed days
- period key roll-over (daily / weekly / flexible)
- Room end-to-end: proportional XP, duplicate-reward protection, milestone once-only, daily reset, level-up detection, undo correction, reset correction, attribute totals
- **persistence across an app restart** (file-backed database reopened)
- seeder idempotency and category deletion keeping quests

Run with `./gradlew testDebugUnitTest` (report at `app/build/reports/tests/testDebugUnitTest/index.html`).

## Design system

Full details in [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md): palette tokens, clay elevation recipe, component inventory and motion rules. Architecture notes live in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), and the Step 1 delivery report in [`docs/DELIVERY.md`](docs/DELIVERY.md).

## Roadmap

- **Step 2** — deeper analytics (monthly/year heatmaps, custom activity metrics), reminders, widgets
- **Step 3** — cosmetic rewards, unlockable avatar art, quest chains, streaks without pressure
- **Step 4** — optional local backup/export, onboarding flow, accessibility pass

## License

MIT — see [LICENSE](LICENSE).
