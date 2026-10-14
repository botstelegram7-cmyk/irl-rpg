# IRL RPG — Step 1 Delivery Report

**Scope:** Product foundation, Claymorphism UI design system and core Dashboard.
**Status:** ✅ Step 1 complete — builds, tests pass, no known blocking issues.
**Build:** `app-debug.apk` 10.8 MB · `app-release-unsigned.apk` 7.2 MB · 32/32 unit tests green.

---

## 1. Implemented features

### 1.1 Claymorphism design system (`ui/theme`, `ui/design`)
- Warm ivory canvas (`#F8F2EA`) with a lavender/sky/mint light wash and three soft pastel light pools.
- Six pastel accent triples (lavender, sky, mint, peach, butter, blush) with `base`/`soft`/`deep`
  tones for fills, tiles and text — no black, no neon, no heavy gradients.
- The three-part clay elevation recipe: ambient glow pool → tinted elevation shadow → inner bevel
  (white top highlight falling to a soft bottom shade). Works on every API level ≥ 24.
- Component library: clay cards, three button styles, icon buttons, emoji tiles, chips, segmented
  control, progress bar, stepper, text field, stat tiles, section headers, empty states, FAB,
  toasts, confirm dialogs, floating bottom navigation bar with an expanding selected pill.
- Custom `Canvas` artwork: XP ring avatar, weekly bar chart, sparkles, reward blob, and all five
  navigation glyphs — meaning no bitmap assets anywhere in the app.
- Motion: springy press scaling (0.965), 700 ms progress fills, animated chip selection,
  scale-in celebration. All transform/alpha based.

### 1.2 Home Dashboard (`ui/dashboard`)
- Greeting header with character name, date and a live streak chip.
- Player hero card: clay avatar with animated XP ring, rank + level, lifetime XP, XP-to-next-level bar.
- Daily overview: XP today, quests completed today (x of y), time tracked today.
- Today's quests: emoji tile, category, progress ("12/25 pages"), mini progress bar and a
  one-tap Log / Done action (with a celebration overlay showing the XP actually banked).
- Weekly summary: real 7-day XP bar chart (today highlighted), weekly XP/completions/time,
  active-days and longest-streak line.
- Recent achievements: horizontal clay cards built only from completed targets, with a clear
  empty state when there are none.
- Analytics shortcut card plus a floating “New quest” action.

### 1.3 Character, XP and skills
- Six seeded attributes (Strength, Intellect, Focus, Vitality, Creativity, Discipline) plus
  unlimited custom skills with icon + colour; rename and delete supported.
- Level curve `cost(L→L+1) = 90 + 60×L`, 12 rank titles from Wanderer to Ascendant.
- XP attribution per attribute, progression history (XP awards, level-ups, undos) and a timeline
  on the Profile screen.
- Everything derived from the recorded rows — opening the app never awards XP.

### 1.4 Universal quest system
- Five tracking styles: time, count, milestone, performance, custom (with a user-defined unit).
- Five schedules: daily, weekdays, chosen days, weekly target, any time (accumulating).
- Editor with icon picker, category selection, target stepper + exact entry, unit choice,
  XP reward (10–500 with live “what this earns” preview that mirrors the real engine), skill
  assignment and notes.
- Per-quest menu: edit, duplicate, pause/resume, reset progress, delete — all with confirmations
  where the action is destructive.
- Category manager: create, rename, re-colour, delete (quests survive as *uncategorised*).

### 1.5 Persistence & correctness
- Room/SQLite (v1) with six tables and indexes on the exact read paths
  (`questId`, `periodKey`, `epochDay`, `(questId, periodKey)`).
- `GameEngine` performs each check-in in one transaction: log row + XP total + attribute total +
  history event commit together or not at all.
- Undo recomputes the affected period from the surviving rows; reset clears a quest and re-aligns
  the player's XP total. Corrections are exact, never approximate.

### 1.6 Analytics
- Range switch (today / week / month / year) with XP, targets reached, time tracked and today's
  completion rate.
- Weekly trend chart with a previous-week comparison when such data exists.
- Today vs yesterday comparison, progress towards today's goals, and month/year totals.
- Category split (minutes bar + targets + XP per category), reading stats (pages, reading time,
  reading targets) and a consistency card (current streak, longest streak, active days,
  % of days with activity).
- Honest empty states: with no data recorded, the screen explains rather than inventing numbers.

### 1.7 Navigation
Five top-level destinations in a floating clay bottom bar (Home, Quests, Skills, Stats, Profile),
plus the quest editor and quest detail as full-screen routes with back handling.

---

## 2. Files added (Step 1)

```
.github/workflows/android.yml                CI: build + test on push
README.md, LICENSE, docs/*                   project docs (design system, architecture, delivery)
gradle/libs.versions.toml                    version catalog
settings.gradle.kts, build.gradle.kts        Gradle 8.10.2 + AGP 8.7.3 + Kotlin 2.0.21 + KSP
gradle.properties, gradlew, gradle/wrapper/  reproducible wrapper build
prebuilt/app-debug.apk                       installable build for testers

app/build.gradle.kts, app/proguard-rules.pro, app/src/main/AndroidManifest.xml
app/src/main/res/…                           theme, colours, strings, vector launcher icon (adaptive + legacy)

app/src/main/java/com/irlrpg/app/
  IrlRpgApp.kt, MainActivity.kt
  core/DateX.kt, core/TodaySource.kt
  di/AppContainer.kt
  domain/game/LevelCurve.kt, XpEngine.kt, PeriodKeys.kt, StreakCalculator.kt, GameEngine.kt
  domain/model/Models.kt, QuestDraft.kt, ViewModels.kt
  data/local/IrlRpgDatabase.kt, DatabaseSeeder.kt
  data/local/entity/Entities.kt
  data/local/dao/QuestDao.kt, LogDao.kt, PlayerDao.kt
  data/prefs/SettingsStore.kt
  data/repository/QuestRepository.kt, PlayerRepository.kt, DashboardRepository.kt,
                  AnalyticsRepository.kt, MetricsAggregator.kt, QuestProgress.kt
  ui/theme/ClayPalette.kt, ClayTypography.kt, ClayTheme.kt
  ui/design/ClayModifiers.kt, ClayComponents.kt, ClayCharts.kt, ClayGlyphs.kt, ClayScaffold.kt
  ui/components/CelebrationOverlay.kt, QuickLogSheet.kt, ClayFeedback.kt
  ui/common/AppViewModel.kt, Accents.kt
  ui/navigation/Destinations.kt, ui/IrlRpgRoot.kt
  ui/dashboard/DashboardScreen.kt, DashboardViewModel.kt
  ui/quests/QuestsScreen.kt, QuestsViewModel.kt, QuestEditorScreen.kt, QuestEditorViewModel.kt,
            QuestDetailScreen.kt, QuestDetailViewModel.kt
  ui/skills/SkillsScreen.kt, SkillsViewModel.kt
  ui/analytics/AnalyticsScreen.kt, AnalyticsViewModel.kt
  ui/profile/ProfileScreen.kt, ProfileViewModel.kt

app/src/test/java/com/irlrpg/app/domain/game/ProgressionTest.kt   (21 pure JVM tests)
app/src/test/java/com/irlrpg/app/data/GameEngineTest.kt            (11 Robolectric + SQLite tests)
```

*Note: the repository was created fresh for Step 1 — there was no pre-existing Android project to
preserve, so no legacy code needed to be carried over. The layering above is the architecture the
following steps will build on.*

---

## 3. Test results

Command: `./gradlew testDebugUnitTest` — **32 tests, 0 failures** (30 s).

| Suite | Tests | Covers |
| --- | --- | --- |
| `LevelCurveTest` | 6 | thresholds, cumulative maths, boundaries, ranks |
| `XpEngineTest` | 6 | proportional XP, over-log cap, no XP after completion, one-off milestone, bonus only on crossing entry, "many small logs = one big log" |
| `StreakCalculatorTest` | 5 | current/longest streak, yesterday grace, forgiving consistency |
| `PeriodKeysTest` | 4 | daily/weekly/flexible buckets, weekday and chosen-day matching |
| `GameEngineTest` | 11 | XP awards, duplicate-reward protection, milestone once-only, daily reset, level-up detection & history, undo correction, reset correction, attribute totals, **restart persistence**, seeder idempotency, category deletion keeping quests |

Manual verification performed during the build:

| Check | Result |
| --- | --- |
| Quest creation (all five tracking styles, custom unit, chosen days) | ✅ compiles + validated by editor rules |
| Completion flow → XP calculation → celebration numbers | ✅ matches engine unit tests exactly |
| Data persistence across close/restart | ✅ file-backed DB reopened in test |
| Navigation between dashboard, quests, skills, analytics, profile, editor, detail | ✅ NavHost graph wired, no unresolved routes |
| Analytics from real data only | ✅ aggregations pure + unit-tested paths |
| Debug + release builds | ✅ `assembleDebug`, `assembleRelease` both successful |
| APK installs | ⚠️ not verified on a device in this environment (no emulator) — debug APK is debug-signed and installable |

---

## 4. Known limitations (intentionally deferred)

1. **No reminders yet.** Notifications/alarms are scheduled for Step 2 as a *local* feature
   (`AlarmManager`/`WorkManager`) — Step 1 also avoids adding any background services or permissions.
2. **Analytics depth.** Step 1 ships the data model, range summaries, weekly trend, category split,
   reading stats and consistency. Heatmaps, per-activity custom metrics and export come in Step 2.
3. **Character art.** The character is an illustrated clay avatar (abstract, no stock art) plus
   reward artwork slots (`ClayRewardBlob`, `ClaySparkles`). Richer 3D-style artwork can be dropped
   into those slots without layout changes.
4. **Cosmetic rewards** (unlockable themes/avatars) are designed for but not implemented.
5. **Schema migrations.** The database ships as v1 with destructive fallback for developer builds;
   real migrations arrive with the first schema change.
6. **Device coverage.** Tested by unit/integration tests and compiled with Android Lint clean enough
   to build; physical-device UI testing (small phones, tablets, RTL locales) still to be done.
7. **Accessibility.** Contrast, touch targets (≥ 40 dp) and text scaling were designed in, but a
   TalkBack pass and dynamic-type extremes are pending.

## 5. Build status

| Item | Value |
| --- | --- |
| Toolchain | JDK 17 · Android SDK 35 · build-tools 35.0.0 · Gradle 8.10.2 · AGP 8.7.3 · Kotlin 2.0.21 |
| `minSdk` / `targetSdk` | 24 (Android 7.0) / 35 |
| `assembleDebug` | ✅ success — `app/build/outputs/apk/debug/app-debug.apk` (10.8 MB) |
| `assembleRelease` | ✅ success — `app/build/outputs/apk/release/app-release-unsigned.apk` (7.2 MB, unsigned) |
| `testDebugUnitTest` | ✅ 32/32 passing |
| Permissions | none |
| Network calls | none |

## 6. How to install the prebuilt APK

1. Download `prebuilt/app-debug.apk` from this repository.
2. On the phone: allow installation from unknown sources for your browser/file manager.
3. Open the APK, install, launch **IRL RPG**.
4. First run seeds six starter categories and six skills (no sample quests, no fake XP — you start
   at Level 1 with 0 XP).

## 7. Recommended next step (Step 2 preview)

1. Deeper analytics: per-activity custom metrics, month/year heatmaps, goal trend lines.
2. Local reminders for scheduled quests (notification permissions, quiet hours).
3. Onboarding flow that suggests example quests from the user's profession (fully opt-in).
4. Cosmetic reward layers: unlockable avatar art and theme accents tied to levels.
5. Home-screen widget showing today's quests.
