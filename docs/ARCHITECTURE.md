# IRL RPG — Architecture (Steps 1–2)

## Principles

1. **Offline-first, local-only.** Room/SQLite is the single source of truth. The app has
   no `INTERNET` permission, no accounts, no analytics SDKs and no paid APIs.
2. **Clear layering.** `ui → domain → data`. UI never touches DAOs; the domain never
   touches Android UI classes.
3. **One way to award XP.** Every XP number in the app comes from `GameEngine` inside a
   single Room transaction, or is recomputed from the resulting log rows. Nowhere else
   writes XP.
4. **Derived data is never stored twice.** Levels are derived from `totalXp`; charts,
   streaks and breakdowns are derived from the append-only `quest_logs` table.
5. **Corrections are exact.** Undo and reset replay the surviving rows through the same
   XP rules, so totals always equal what the recorded history implies.

## Module map

```
core/                      platform-free helpers
  DateX                    day/week/month maths, labels, "1h 20m" formatting
  TodaySource              holds the current LocalDate; refresh() on resume (no timers)

domain/
  model/Models.kt          TrackingKind, ScheduleKind, UnitKind, AttributeKind
  model/QuestDraft.kt      validated editor output → QuestEntity
  model/ViewModels.kt      QuestWithProgress, PlayerSnapshot, DashboardState, AnalyticsState…
  game/LevelCurve.kt       level thresholds, rank titles
  game/XpEngine.kt         proportional XP, completion bonus, anti-abuse caps
  game/PeriodKeys.kt       daily / weekly / flexible buckets + schedule matching
  game/StreakCalculator.kt current & longest streak, forgiving consistency rate
  game/GameEngine.kt       transactional logQuest / undoLog / resetQuestProgress

data/
  local/IrlRpgDatabase.kt  Room v1: quests, categories, quest_logs, player_stats,
                           attributes, progression_history
  local/DatabaseSeeder.kt  first-run categories + the six attributes (idempotent)
  local/dao/*              QuestDao, CategoryDao, QuestLogDao, PlayerDao,
                           AttributeDao, ProgressionDao
  repository/MetricsAggregator.kt  pure aggregation over log rows
  repository/QuestProgress.kt      per-period progress resolution
  repository/QuestRepository.kt    quests, categories, logs, CRUD
  repository/PlayerRepository.kt   player snapshot, attributes, history
  repository/DashboardRepository.kt  assembles DashboardState
  repository/AnalyticsRepository.kt  assembles AnalyticsState
  prefs/SettingsStore.kt   one flag: has the database been seeded

di/AppContainer.kt         builds the whole graph once, lazily

ui/
  theme/                   ClayPalette, ClayTypography, ClayTheme (Material mapping)
  design/                  clay modifiers, components, charts, glyphs, scaffold
  components/              CelebrationOverlay, QuickLogSheet, ClayToast, ClayConfirmDialog
  dashboard|quests|skills|analytics|profile/   screen + ViewModel pairs
  navigation/Destinations.kt   routes + bottom-bar destinations
  IrlRpgRoot.kt            NavHost wiring
```

## Data model

| Table | Purpose | Notes |
| --- | --- | --- |
| `categories` | user groupings | deletable; quests fall back to *uncategorised* via `SET_NULL` |
| `quests` | activity definitions | tracking kind, unit, target, schedule, XP reward, skill attribute |
| `quest_logs` | append-only activity entries | amount, unit label, **xpEarned**, `isCompletion`, `periodKey`, `epochDay` |
| `player_stats` | single row | name, `totalXp`, `createdAtEpochDay` |
| `attributes` | skills | six built-ins + user-created; `xpContributed` kept in sync |
| `progression_history` | level-ups, XP awards, undos | `refLogId` links an event to its log row for exact undo |

Indexes cover `questId`, `periodKey`, `epochDay` and the `(questId, periodKey)` pair,
which is exactly how progress is read back.

## Check-in flow (the core loop)

```
UI (Log button)
  └─ GameEngine.logQuest(questId, amount)            [single Room transaction]
       1. load quest → tracking kind + schedule → periodKey (day / week / flex)
       2. read progress for that period
       3. XpEngine.xpForProgress(...)                proportional, capped at target
          XpEngine.completesTarget(...)              bonus only on the crossing entry
       4. insert quest_logs row (xpEarned, isCompletion)
       5. update player_stats.totalXp + attribute total + progression history
       6. return LogResult → celebration overlay (real numbers only)
```

Because steps 1–6 are one transaction, killing the app mid-award cannot double-credit or
half-credit the player: the log row and the XP total commit together or not at all.

## State management

Each screen has a ViewModel exposing `StateFlow` built from Room `Flow`s:

```kotlin
val state: StateFlow<DashboardState?> =
    dashboardRepository.observeDashboard()          // combine(player, quests, categories, logs, today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

- Room emits on every write → the UI updates without manual refresh calls.
- `WhileSubscribed(5_000)` stops database observers when the screen is off-screen.
- Heavy aggregation runs with `flowOn(Dispatchers.Default)`.
- `TodaySource.refresh()` is called from a `LifecycleResumeEffect`, so the app notices a
  day roll-over when it returns to the foreground — no alarms, no background services.

## Analytics pipeline

```
quest_logs ──► MetricsAggregator ──► DashboardState / AnalyticsState ──► Compose UI
```

`MetricsAggregator` is pure (no Android types) so it is unit-testable and cheap:
day metrics, week bars, category split, lifetime totals, reading pages/minutes, active
days. Analytics are computed for the whole history in memory, which stays in the low
milliseconds for the data volumes a personal tracker produces (thousands of rows).

## Testing strategy

- **Pure domain tests** (`LevelCurveTest`, `XpEngineTest`, `StreakCalculatorTest`,
  `PeriodKeysTest`) — instant, no Android.
- **Robolectric + real SQLite tests** (`GameEngineTest`) — XP awarding, anti-abuse,
  milestone-once, daily reset, level-up recording, undo/reset correction, attribute
  totals, seeding idempotency, category deletion behaviour, and **restart persistence**
  using a file-backed database that is closed and reopened.

## Performance notes (budget devices)

- No reflection-based DI, no annotation-heavy runtime work: cold start builds a handful of objects.
- No bitmaps anywhere: backgrounds, charts, avatar ring, nav glyphs and the launcher icon
  are vectors or `Canvas` drawing.
- Animations are transform/alpha only (`graphicsLayer`) — no layout thrash.
- Database reads are indexed and batched (`observeProgressForPeriods` aggregates every
  quest's period progress in one query).
- Single Gradle JVM with in-process Kotlin compilation keeps build memory modest.

## Step 2 additions

### Derived, never stored
Skill node state, level thresholds, achievement progress and boss progress are all *derived* at read time from the append-only log (`quest_logs`) plus a few small tables. That is why Step 2 can be added to an existing install without a single XP rewrite: nothing about Step 1 progress has to be recalculated or migrated, only new columns and empty tables are added.

### Sync point
`ProgressSync.sweep(today, announce)` is the single place that keeps the new systems consistent:

1. `gameEngine.ensureInstancesForToday(today)` — materialise recurring instances (idempotent, unique index on quest + period),
2. `bossRepository.checkCompletions(today)` — pay any challenge whose required objectives are complete (status re-checked inside the paying transaction),
3. `achievementRepository.sync(today)` — persist newly earned achievements and report them.

It is called on app start, on dashboard resume, after every logged entry and after editor changes. Deliberately **not** a background service: no wakeups, no permissions, no battery cost.

### Anti-duplication
| Risk | Guard |
| --- | --- |
| Recurring quest pays twice in one period | `quest_instances` unique index on `(questId, periodKey)` + bonus gated on `status` |
| Multi-step quest re-pays a finished step | step rows summed per `(questId, periodKey, stepId)`; finished steps pay 0 |
| Boss reward re-paid | `status`/`xpAwarded` re-checked inside the paying transaction |
| Achievement invented retroactively | first-run `prime()` records history silently; the count is announced, not the list |
| XP from opening the app or editing | no code path awards XP outside `GameEngine.logQuest` |

### Rendering the skill tree cheaply
`SkillTreeLayout.place()` computes lane/depth positions in plain Kotlin (unit-testable, no Compose), a single `Canvas` draws every connection, and the nodes are lightweight composables inside one `graphicsLayer` transform. Pan/zoom is therefore one transform for the whole tree, not per-node work — which is what keeps it smooth on budget hardware.

## Deliberate boundaries

- No reminders/notifications, no widgets, no cloud sync, no cosmetic shop (schedule is
  planned as a **local** `AlarmManager`/`WorkManager` feature in Step 2 — no servers).
- Analytics ships summaries + weekly trend; heatmaps and per-activity custom metrics are Step 2.
- Character is an illustrated clay avatar + emoji artwork slots; richer art drops into
  `ClayRewardBlob` / `ClayAvatar` without layout changes.
