# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Čuvar Kolone** is a portrait escort-defense game: the player guards a protest march through Belgrade, Paris, or Milan. It is Kotlin Multiplatform + Compose Multiplatform (Android, iOS, desktop). All art is drawn in code with Compose Canvas. There are no image assets.

## Commands

- `just run` starts the desktop game (`./gradlew :desktopApp:run`). Close the game window before you run another Gradle task: the running game holds Gradle's project lock.
- `./gradlew :shared:jvmTest :desktopApp:classes` runs all tests and compiles desktop. It includes the difficulty benchmark (about 12 s with three cities).
- `./gradlew :shared:jvmTest --tests '*CityMapTest*'` runs one test class. Add `.testName` for one test.
- `./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` compiles the iOS Kotlin code.
- `./gradlew :androidApp:assembleDebug` needs an Android SDK (see README).
- Test report: `shared/build/reports/tests/jvmTest/index.html`. Count tests from `shared/build/test-results/jvmTest/*.xml` (the README states the count).
- There is no linter.
- Visual checks: the shell cannot take screenshots of the desktop window. Render screens headlessly in a throwaway jvmTest with `ImageComposeScene { KolonaApp(session) }` (see `CityFlowTest` for the setup and taps), write PNGs to a scratch folder, look at them, then delete the throwaway test.

## Architecture

All game code is in `shared/`. `androidApp`, `iosApp` (SwiftUI host) and `desktopApp` only host `KolonaApp` and give a `PreferenceStore`.

- `game/` is pure Kotlin. It has no Compose, platform, or clock code. The simulation is deterministic from its seed and runs at a fixed 60 Hz (`GameEngine`).
- Each game builds a new `CityMap` with `generateMap(city, seed, mapScale(seed, config.maxMapScale))`:
  1. The route goes through the template's stops and legs. Streets wobble at random. Named side streets come next, then unnamed filler streets (`StreetStyle.Grid` or `Radial`).
  2. `StreetNetwork` decides where people can walk: only street corridors and squares (a clearance raster). It also has a 35-unit A* grid, pruned to the part connected to the route. City blocks are solid.
  3. Then come landmarks (no randomness), buildings, trees, and labels. Props come last, with their own `Random(seed * 31 + 7)`.
- Only the route, the streets, and the squares change gameplay. Buildings, landmarks, trees, labels, and props are decoration. The difficulty benchmark (`DifficultyBenchmarkTest`) guards this: a scripted hero must save 59.6 ± 12% people on average in every city at 1× and at max scale, and the cities may differ by at most 12% of 59.6.
- The map size is 1400 × 1120 times a random scale from 1 to `GameConfig.maxMapScale` (2). Street widths and people do not scale.
- `ui/`:
  - `KolonaApp.kt` holds the screens, `CityDialog`, `Brand`, and `flag()`.
  - `GameScreen.kt` holds the HUD and the pause and result dialogs. The win dialog shows the souvenir.
  - `GameArt.kt` draws everything on the map. `MapPaint` caches paths. `city()` draws the map, culled to the camera rect. Each `building()`, `landmark()`, `prop()`, and `souvenir()` dispatches by city or kind. `CityArtwork` draws the welcome and menu art from a fixed preview map.
  - `GameSession.kt` holds the session state and preferences.
- `i18n/Strings.kt`: `Language` (English default, Serbian, French, Italian), `Copy(sr, en, fr, it)` (every entry needs all four), and the `Strings` helpers. The game's name is translated too: `Copy.GameName`, and the wordmark's `Subtitle` above `Wordmark` (`Brand()`, and the desktop window title). `GameIdentity` holds only the version. `City.language` (`GameSession.kt`) is the language the "Also switch the language" checkbox picks. Milan maps to Italian.
- Saved preference keys: `language`, `city`, `city.language`, `best.<city code>` (the legacy `best` belongs to Belgrade). City codes are saved, so never rename one.
- Text style:
  - Map labels always use local names, in every language.
  - French uses "tu" and "cortège" for the march. Italian uses "tu" and "corteo".
  - In the in-game header, an accent on the wordmark must not touch the subtitle above it (the French È did). Check the HUD render in every language.
  - Use only glyphs present in both bundled fonts (`shared/src/commonMain/composeResources/font/`). U+202F is missing, and "→" exists only in DM Sans.

## Adding a new city (and country)

When asked for a new city, build it right away with the defaults below. Ask only about what you cannot pick yourself.

**Decide first** (real places; names matter more than geometry):
- **Route:** 3 real squares in a real march order: start (A), middle, end (B). Add 2 legs with real street names, and 4–5 named side streets.
- **Style:** `Grid` (rectangular blocks) or `Radial` (star squares, diagonal boulevards). Add a new `StreetStyle` only if neither fits.
- **Landmarks:** 2–4 real buildings next to the route squares.
  - Put one near the start (A): the welcome and menu art shows only about start.x − 250 to start.x + 500 at 1× (preview seed 2026).
  - Keep landmarks with long names away from the map edges. Only the label's centre is kept inside the map.
- **Props:** 6–9 symbolic street props: food, street furniture, street life, plus `Pigeons`. No brands, no alcohol.
- **Souvenir:** one food item, the city's flag, and the country's language.

**Code, in this order:**
1. `game/Cities.kt`:
   - Add a `City(code)` entry. The code is lowercase and stable.
   - Add its `CityTemplate`. Stop and street points are (u, v) fractions inside a 110-unit margin, and street ends may go past 0..1.
   - Set `Stop.spokes` for `Star` squares, by name. Spoke counts change 1× difficulty a lot, so they are the main tuning knob for a `Radial` city.
   - Add any wobble exception to `NamedStreet.wobble`.
   - `SquareKind.Round` draws a monument at its centre. Use it only for the start or the middle square, because the end square's B marker would cover it.
   - In a `Radial` city, street trees grow only along named streets that start with "Bd" (a Paris rule). A new `Radial` city gets no street trees unless you extend that rule without changing Paris.
   - Landmarks are `LandmarkSpec(stop, along, side, width, height)`. Props are a list of `PropKind`.
   - Add new `LandmarkKind` or `PropKind` values only for buildings or props that do not exist yet. A `PropKind` also needs its footprint sizes.
2. `i18n/Strings.kt`:
   - Add `Copy` entries for the city name, the country, `Mission<City>`, `Won<City>`, and `Souvenir<City>`, in sr, en, fr, and it.
   - Add the branches in `Strings.cityName`, `country`, `mission`, `won`, and `souvenir`. These are exhaustive `when`s, so the compiler lists what is missing.
   - Serbian needs the right case, for example "do Trga Republike" and "iz Pariza".
3. `ui/GameSession.kt`: add a `City.language` branch.
   - If the country's language is new, it is a large change: add a `Language` entry, a translation column for every `Copy` entry, the `Strings.get` branch, the `LanguageTest` lists, Xcode `knownRegions`, and the README "Languages" section.
   - Say so before you do it, or map the city to English.
4. `ui/`:
   - Add the flag to `flag()` in `KolonaApp.kt`.
   - Add a building style branch to `building()` in `GameArt.kt`. Keep the muted night palette and use `palette` for variety.
   - Add drawing functions for new landmark and prop kinds, each with a `signature` colour.
     - Take the signature colour from a large flat area. `landmarksAreDrawn` and `propsAreDrawn` render at 0.25 px per unit, so a ribbed or striped fill never produces an exact pixel.
     - Keep the drawing free of per-frame allocation: no `listOf`, `Path()`, or `polygon()` in draw code. Hoist those values, or cache them like `MapPaint` does.
   - Add the souvenir card to `souvenir()`, plus `souvenirColor` (GameArt) and `souvenirName` (GameScreen, the local name).
5. Tests: most loop over `City.entries`. For a runtime RED, first add the new enum entries with `TODO()` in every exhaustive `when` (strings, art, flag, souvenir), then fill them in. Update these by hand:
   - `FlagRenderingTest`: add a stripe-order test for the new flag.
   - `LanguageTest.souvenirLineNamesTheCityOfTheGame`: add the four lines.
   - `CityFlowTest`: the city dialog fits three cards at 360×700 (`cityDialogFitsASmallPhoneWithoutScrolling`). A fourth card needs more compacting or a new layout. `ImageComposeScene` swallows a crash inside a `Dialog`, so a dialog test must prove the dialog shows: tap its button and check that the game starts.

**Checks, in order:**
1. `CityMapTest`: every stop is in order; the route length divided by the scale is 1400–2000; every landmark is placed; every prop kind appears on 2× maps; every street can be reached.
2. `DifficultyBenchmarkTest`: if the new city is off the band, tune only that city's template structure: stop positions, legs, side streets, widths, spokes. Never add per-city gameplay values to `GameConfig`.
   - Watch for enemies that spawn past a map edge and snap to a street next to the crowd. Bastille needed spokes for this.
3. The full suite and the iOS compile (commands above).
4. Headless renders:
   - the game start;
   - each landmark;
   - props up close;
   - the welcome and menu art (preview seed 2026);
   - the city dialog;
   - the win dialog with the souvenir, at 360 × 700 dp, in en, sr, fr, and it.
5. README: update the "Cities" section (route, landmarks, props, souvenir) and the test counts in "Verify".
