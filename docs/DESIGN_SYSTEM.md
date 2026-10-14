# IRL RPG — Claymorphism Design System (Step 1)

A light, premium claymorphism language: warm ivory canvas, pastel clay slabs, tinted
soft shadows, inner bevels and hand-drawn charts. The "3D" feel is produced entirely
with gradients, elevation shadows and `Canvas` drawing — no Unity, no real-time 3D
renderer, no heavy image assets.

## 1. Colour tokens

### Ink (text)

| Token | Hex | Use |
| --- | --- | --- |
| `ClayInk.Primary` | `#3B3450` | Headings, values |
| `ClayInk.Secondary` | `#6D6781` | Body copy |
| `ClayInk.Muted` | `#9A93AB` | Secondary labels |
| `ClayInk.Faint` | `#BDB6CA` | Hints, axis labels |

Deliberately no pure black — a soft plum-grey keeps the warm clay feel readable.

### Surfaces

| Token | Hex | Use |
| --- | --- | --- |
| `WarmIvory` | `#F8F2EA` | Page background |
| `SurfaceCream` | `#FFFBF6` | Default clay slab |
| `SurfacePure` | `#FFFFFF` | Raised controls, pressed states |
| `SurfaceMuted` | `#F3EDF7` | Inset tracks and wells |
| Background brush | `#F8F2EA → #F7F0E9 → #F4F0FA` | Page wash |

The background also paints three very soft radial light pools (lavender top-right, sky
mid-left, mint bottom-right) at 12–16% alpha — cheap to draw, gives the page depth.

### Pastel accents

Each accent is a triple: `base` (fills/icons), `soft` (tinted clay tile), `deep` (text
and iconography on `soft`).

| Accent | base | soft | deep | Typical meaning |
| --- | --- | --- | --- | --- |
| Lavender | `#9F86EF` | `#EDE6FD` | `#6A4CC2` | Primary actions, level |
| Sky | `#6FB6F0` | `#E0F0FD` | `#2B79BA` | Analytics, reading |
| Mint | `#55D2AC` | `#DDF6EE` | `#1B9375` | Completion, quests |
| Peach | `#F7A97F` | `#FDEAE0` | `#C4672F` | Streaks, fitness, undo |
| Butter | `#F2C868` | `#FBF0D5` | `#AE7E15` | XP, rewards, skills |
| Blush | `#EF98B5` | `#FCE5ED` | `#B94B72` | Profile, writing, destructive |

Users pick an accent for every category and custom skill, so the app re-colours itself
around their own organisation.

### Shadow tint

Shadows are lavender-grey (`#8E82AA`, `#7A6D9C`) at low alpha, never black — this is
what keeps the surfaces looking like warm clay instead of flat Material cards.

## 2. Shape scale

| Token | Radius | Used for |
| --- | --- | --- |
| `ClayShape.Tile` | 18 dp | Emoji tiles |
| `ClayShape.Small` | 16 dp | Text fields, mini stats |
| `ClayShape.Medium` | 22 dp | Compact cards, menus |
| `ClayShape.Card` | 28 dp | Standard cards |
| `ClayShape.Hero` | 34 dp | Player hero, bottom bar, sheets |
| `ClayShape.Pill` | 50% | Chips, buttons, progress bars |

## 3. The clay elevation recipe

Three stacked effects (`ui/design/ClayModifiers.kt`):

1. **`clayAmbientGlow`** — a wide radial pool behind the surface (`spread 1.16×`,
   alpha 0.10–0.32) that tints the ivory background underneath. Works on every API
   level, unlike modern blur APIs.
2. **`clayShadow`** — Compose elevation with tinted `ambientColor`/`spotColor`, so the
   shadow reads as warm plum rather than grey.
3. **`clayBevel`** — a 1.5–2 dp stroke following the shape with a vertical gradient
   (white 92% → transparent → shadow 10%): the light catches the top edge and the
   bottom edge sinks. This single detail is what makes surfaces read as moulded clay.

`Modifier.clay(shape, elevation, glowAlpha, bevelWidth)` composes all three.
`Modifier.clayInset(...)` creates the opposite effect for tracks and wells.

Elevation ladder: 3 dp (inline tiles) · 8–10 dp (list rows, chips) · 12–14 dp (cards) ·
16–20 dp (hero cards) · 22 dp (celebration).

## 4. Typography

System sans-serif (no bundled fonts → smaller APK, faster startup), weights
Bold/SemiBold/Medium only:

`displaySmall 32` · `headlineMedium 26` · `headlineSmall 21` · `titleLarge 19` ·
`titleMedium 16` · `titleSmall 14` · `bodyLarge 15` · `bodyMedium 14` ·
`bodySmall 12.5` · `labelLarge 14` · `labelMedium 12` · `labelSmall 11`

Display sizes carry slight negative tracking (−0.6 sp at 32) for a modern editorial feel.

## 5. Component inventory

`ui/design/ClayComponents.kt`

- `ClayCard` — slab with optional tap; press scales to 0.965 with a springy, low-bounce curve
- `ClayButton` — Filled (3-stop gradient + white text), Soft (pastel fill + deep text), Plain
- `ClayIconButton` — circular action button (back, menu, edit)
- `ClayIconTile` — pastel tile holding an emoji (activities, categories, rewards)
- `ClayChip` — selectable pill with animated container/label colour
- `ClaySegmentedControl` — sliding pastel selection for ranges
- `ClayProgressBar` — bevelled track + animated gradient fill (700 ms, fast-out-slow-in)
- `ClayStepper` — tactile −/+ control for targets and logged amounts
- `ClayTextField` — soft inset field with accent focus ring
- `ClayStatTile`, `ClaySectionHeader`, `ClayEmptyState`, `ClayFab`, `ClayRow`

`ui/design/ClayCharts.kt`

- `ClayBarChart` — per-day inset tracks with gradient fills, value labels only where data exists
- `ClayAvatar` — clay disc with an animated sweep-gradient XP ring and level badge
- `ClayRewardBlob` + `ClaySparkles` — reward artwork slots (floating clay orbs and highlights)

`ui/design/ClayGlyphs.kt` — navigation glyphs (home, quests, skills, analytics, profile)
drawn with `Canvas` paths so stroke weight, caps and joins match the UI everywhere.

`ui/design/ClayScaffold.kt` — page background + floating bottom bar with an expanding
selected pill (glyph fills in and the label slides out).

## 6. Motion rules

| Interaction | Motion |
| --- | --- |
| Button/chip press | scale → 0.965, spring(damping 0.55, stiffness 900) |
| Progress fill | 700 ms tween, fast-out-slow-in; XP ring 800 ms |
| Chip selection | 220 ms colour crossfade |
| Bottom bar selection | 200–250 ms fade + horizontal expand |
| Celebration | scrim fade + scale-in from 0.72 with a soft spring |
| Toast | fade + slide from bottom third, 2.6 s dwell |

All animations are transform/alpha based, so they stay cheap on budget hardware.

## 7. Artwork slots for later phases

The design system already exposes the hooks for richer 3D-style artwork without changing
layout: `ClayRewardBlob` (celebration), `ClaySparkles` (hero decoration), `ClayAvatar`
(character). Dropping in pre-rendered PNG/WebP art later is a one-file change per slot.

## 8. Accessibility & sizing

- Minimum touch target 40–44 dp for every interactive element (chips 40 dp tall, buttons 52 dp)
- Body text ≥ 12.5 sp, values ≥ 14 sp; contrast of `ClayInk.Primary` on cream is ~9:1
- Layouts use flexible weights and `LazyRow`/`LazyColumn`, so they adapt from 4.7" phones
  to tablets; long lists scroll with generous 20 dp gutters and safe-area padding
- Status/navigation bars are transparent with dark icons (warm ivory background)
