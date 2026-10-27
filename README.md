# Čuvar Kolone

A portrait escort-defense game built with **Kotlin Multiplatform and Compose Multiplatform**. Android and iOS share the game engine, rendering, UI, and translations. A desktop app makes development and playtesting easier.

Inspired by [Čuvar kolone](https://cuvar-kolone.controid.chatgpt.site). This is a native Kotlin implementation, not a WebView wrapper. The maps, characters, and effects are drawn with Compose Canvas; fonts are bundled for offline use.

The game's name follows the app language: **Čuvar Kolone** in Serbian, **Guardian of the March** in English, **Gardien du Cortège** in French, and **Guardiano del Corteo** in Italian. The wordmark shows the small first part above the big last word, for example ČUVAR above KOLONE. The names are the `GameName`, `Subtitle`, and `Wordmark` entries in `shared/src/commonMain/kotlin/dev/oblac/kolona/i18n/Strings.kt`. The desktop window title follows them too. The Android, iOS, and desktop app names stay **Čuvar Kolone**, because the system shows them before you pick a language. Application IDs and saved preferences remain stable across brand changes.

The base Kotlin package and the Android, iOS, and desktop app ID are `dev.oblac.kolona`; shared code and generated resources use subpackages of the same base.

## Run

Use JDK 17 or 21. The Gradle wrapper is included and verifies the distribution checksum. The first build downloads dependencies.

### Desktop

```sh
./gradlew :desktopApp:run
```

The `justfile` in the project root has the same command as a shortcut: `just run`.

The preview uses a fixed portrait window. Click to move and use the on-screen pause/resume controls. Changing app focus automatically pauses gameplay.

On macOS, a standalone app can also be built:

```sh
./gradlew :desktopApp:createDistributable
open "desktopApp/build/compose/binaries/main/app/Cuvar Kolone.app"
```

Close a running Gradle preview before launching another Gradle build in the same checkout; the running task can hold Gradle's project lock. The standalone app avoids this.

### Android

Install Android SDK Platform 37 and its build tools through Android Studio, then set `ANDROID_HOME` or add `sdk.dir=/absolute/path/to/sdk` to an untracked `local.properties` file. Open this folder in Android Studio and run `androidApp`, or:

```sh
./gradlew :androidApp:assembleDebug
```

For Google Play, `./gradlew :androidApp:bundleRelease` writes `androidApp/build/outputs/bundle/release/androidApp-release.aab`, shrunk with R8. It is signed with the upload key named in an untracked `keystore.properties`; without that file the bundle is unsigned. `playstore/listing.md` shows how to make the key.

The APK is written to `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Minimum API is 24. The activity requests portrait orientation; the shared UI also constrains the playfield to portrait when an OS or tablet window overrides orientation requests.

### iOS

Open `iosApp/iosApp.xcodeproj`, choose the **Kolona** scheme and an iPhone simulator, and Run. Xcode's build phase compiles and embeds the Kotlin framework and Compose resources. The project's signing team is used for a physical device and for distribution. The app icon is in `iosApp/iosApp/Assets.xcassets`, and `PrivacyInfo.xcprivacy` declares the UserDefaults and file timestamp APIs for App Store Connect.

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme Kolona \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath iosApp/DerivedData CODE_SIGNING_ALLOWED=NO build
```

Minimum iOS is 16; device and Apple Silicon simulator targets are included. Both iPhone and iPad declare portrait-only support. Scene changes pause the game. There is no Intel simulator target.

### Store uploads

`just beta` uploads the current commit to Google Play internal testing and to TestFlight (`just beta-android` or `just beta-ios` for one of them). `just release` uploads it as a production draft on Google Play (roll it out in Play Console) and as a build in App Store Connect (submit it for review there).

- Commit first: the build number is the commit count, so every upload needs its own commit.
- Android needs the upload key from `keystore.properties` and the Play service account key in `~/.android/sisyphus-play.json`, with access to this app in Play Console. Google Play takes the first bundle of an app only by hand in Play Console; the recipes work after that.
- iOS uses the account signed in to Xcode and `iosApp/ExportOptions.plist`.

## Play

1. Welcome → **ENTER THE CITY** → choose Belgrade, Paris, or Milan in the city dialog → **DEFEND THE MARCH** starts the game. Belgrade is preselected the first time.
2. Tap a street to move. The defender walks only on streets and squares and finds a path around the city blocks. A tap inside a block sends him to the nearest street. Walking through marchers slows him down.
3. Stop to raise a barricade and automatically shoot nearby threats. Moving disables shooting.
4. Tap an active group of **at least four enemies** to throw a bomb. Recharge takes **five seconds**. Bombs cannot damage marchers.
5. Escort at least **36 of 72 people** to the last square of the route: Trg Republike in Belgrade, Nation in Paris, Piazza del Duomo in Milan. The march takes 150 seconds, plus two 15-second speech stops. Attackers include thugs, police, and cars.

Every game, including a restart, builds a new map. The base world is **1400 × 1120 units**, and a random scale from 1× up to `maxMapScale` (2 by default) can make it larger; see Cities. Every size is wider than the phone's viewport. A smooth, bounded camera follows **only the defender**. At the top of the map, the camera can go past the map edge by the height of the phase box and the minimap, so they do not cover the top streets. The small route overview and screen-edge markers show where the march and threats are; they never move the camera. Gold identifies the defender, mint the march, and coral threats.

Pause, resume, restart, return to menu, win/loss results, and the best successful rescue for each city are implemented. Ongoing runs are not restored after the app process is terminated.

## Cities

Belgrade (Serbia), Paris (France), and Milan (Italy) are all free. Belgrade is the default.

Every game starts with the city dialog: **ENTER THE CITY** on the welcome screen and **DEFEND THE MARCH** on the main menu both open it. It has one card per city, and it fits a 360 × 700 dp phone without scrolling. Tap a card to choose that city, then **DEFEND THE MARCH** in the dialog starts the game there. The choice is saved locally and preselected next time. Restarting from the pause or result screen keeps the same city.

- **Belgrade:** Slavija → Kralja Milana → Terazije → Kolarčeva → Trg Republike. A street grid with roughly rectangular blocks.
- **Paris:** République → Bd du Temple / Bd Beaumarchais → Bastille → Rue du Faubourg Saint-Antoine → Nation. Diagonal boulevards with trees, star-shaped squares, and wedge-shaped blocks.
- **Milan:** Porta Venezia → Corso Venezia → San Babila → Corso Vittorio Emanuele II → Piazza del Duomo, the route of the 25 April march. Streets radiate from the squares, as the corsi do in Milan. The map has no boulevard trees.

Each map also has a few named side streets, such as Knez Mihailova in Belgrade, Bd Voltaire in Paris, and Via Monte Napoleone in Milan.

Each city has its own buildings. Paris has cream stone fronts under grey-blue zinc mansard roofs, with dormers and rows of chimney pots. Belgrade has red tile roofs over plaster fronts, mixed with grey concrete blocks that have flat roofs and vents. Milan has palazzi with ochre (giallo Milano) and terracotta plaster fronts and green shutters. Most have a ring of tiled roof around a dark inner courtyard (cortile). Some have a flat roof with a terrace of plants under a parasol. The colours stay dark, so the route, the march, the threats, the names, and the defender stay the brightest things on the map.

Each map also has landmarks near their real places. Each landmark has a map label with its local name:

- **Belgrade:** Hram Svetog Save by Slavija, Beograđanka on Kralja Milana near Slavija, Hotel Moskva at Terazije, Narodni muzej on Trg Republike.
- **Paris:** Cirque d'Hiver on Bd du Temple near République, Opéra Bastille at Bastille.
- **Milan:** Duomo di Milano and Galleria Vittorio Emanuele II on Corso Vittorio Emanuele II by Piazza del Duomo, Caselli di Porta Venezia on Corso Venezia near Porta Venezia. The Duomo shows its white marble roofs full of spires, the golden Madonnina on the tallest spire, and the pointed front over five portals. The Galleria shows its cross of glass vaults under the glass dome. The Caselli are the two neoclassical toll houses of the old gate.

The template gives each landmark a point on the route and a preferred side of the march (`landmarks` in `game/Cities.kt`). The landmark takes the closest free spot in a city block near that point, never on a street or a square. This spot can be across the street, on the other side of the march. The label goes below the landmark, or above it at the bottom edge of the map. Landmarks and their labels keep clear of the square names. Near the stops, the named streets make small blocks on a small map. A landmark that has no room there shrinks in steps of 10%, down to at most 20% of its size. Large shrinks are rare. Landmarks do not change where anyone can walk, so the game is as hard as before. The welcome and menu artwork show the landmarks too.

Each map also has street life by the block edges. These small drawings tell more about the city. They have no effect on the game, and they show no brand names.

- **Paris:** a boulangerie with baguettes and croissants, a crêpe stand, a café terrace with a red awning and small round tables, a green Morris column with posters, a Wallace drinking fountain, an Art Nouveau Métro entrance, a green newsstand, an accordion player, a rack of shared bikes, and pigeons.
- **Belgrade:** a grill kiosk with pljeskavica and ćevapi, a pekara with burek, a kafana terrace with checked tablecloths, a trafika, street chess tables, a brass band (trubači), and pigeons.
- **Milan:** a gelato cart under a striped parasol, a pasticceria window with panettoni, an espresso bar with a small terrace, the green cast-iron drinking fountain (vedovella) with the city's arms, a row of parked scooters, a fashion boutique window with mannequins, and pigeons.

The template lists the kinds of props for each city (`props` in `game/Cities.kt`). The generator places the props last, after the labels:

- A prop stands in a city block next to a street, and it faces that street. It keeps clear of the streets, the buildings, the landmarks, the trees, the labels, and the other props.
- Props go near the landmarks, the squares, and points along the route, where the player looks.
- Each kind comes once before a kind repeats. There is about one prop for each 140 units of route: about 11 on a 1× map and up to 26 on a 2× map. On a 1× map, a kind can be missing when the blocks have no room for it. Every kind appears on a 2× map.
- Pigeons are the exception. A flock of 3 to 6 birds sits on each square that the march crosses after the start, next to the path of the march.
- Props use their own random numbers. The streets, buildings, trees, labels, and the game stay exactly as before, and the difficulty benchmark gives the same numbers.

In the game, the grill smokes, the musicians sway, and the pigeons peck. When the defender or a marcher comes within about 60 units, the pigeons fly up and away. The welcome and menu artwork show the props too, without motion.

When the march wins, the result dialog shows a souvenir postcard from the city: a croissant from Paris, a pljeskavica from Belgrade, or a panettone from Milan, with its local name. A lost game shows no souvenir.

Maps are generated, not drawn by hand:

- Every game generates a new map from the city's template (`game/Cities.kt`, `game/CityMap.kt`). The same seed gives the same map. The welcome and menu artwork show a fixed preview map of the chosen city.
- The template has the real names of squares and streets, in the real order of the march. The map is not a precise plan: streets wobble at random. Every name appears as a map label. Labels use the local names in every language.
- City blocks are solid. The defender and the enemies move only on streets and squares (`game/StreetNetwork.kt`: corridor walkability on a 35-unit grid, A* with a binary heap). Cells that the march route cannot reach are pruned, so every map is reachable and nobody gets stuck.
- The base world is 1400 × 1120 units. Each game multiplies it by a random scale from 1 to `GameConfig.maxMapScale`, which is 2 by default (`game/GameEngine.kt`).
- The march takes 150 seconds plus two 15-second speeches on every map, so marchers walk slower on a small map. Enemy waves compensate. The base wait between waves is `spawnSeconds` (3.2 s) on a 1× map and `spawnSecondsAtMaxScale` (2.5 s) on a `maxMapScale` map. Sizes in between interpolate linearly.
- A difficulty benchmark (see Verify) checks that all cities stay about as hard as the game was before cities, at the smallest and the largest map size.

Below the city cards, the **Also switch the language** checkbox is unchecked by default. When it is checked, choosing Belgrade switches the game to Serbian, choosing Paris switches it to French, and choosing Milan switches it to Italian. Checking the box switches to the current city's language at once. The globe button on the main menu still changes the language at any time and does not uncheck the box. The checkbox is saved locally, like the city.

The best rescue is saved per city. The best rescue saved before cities existed counts as Belgrade's.

## Languages

- English is the default. Serbian (Latin), French, and Italian are also available. All four are free; there is no unlock or purchase.
- The globe button at the top right of the main menu opens the language list. The choice is saved locally and restored on the next launch. A missing or unknown saved value falls back to English.
- All menu text, instructions, wave names, and result text are translated into every language, and so are the game's name and the city and country names. Map labels are not translated. They keep the local names of squares and streets, like street signs.
- The **Also switch the language** checkbox in the city dialog can change the language together with the city: Belgrade switches to Serbian, Paris to French, and Milan to Italian. See Cities.

Translations use a typed `Copy` catalog; each key requires Serbian, English, French, and Italian. To add another language, extend `Language` and add a translation to every `Copy` entry.

## Project layout

```text
shared/src/commonMain/kotlin/dev/oblac/kolona/
  game/       Pure Kotlin simulation, city templates, map generation, street network with A*, geometry, camera
  ui/         Compose screens, vector art, frame loop, app session
  i18n/       Brand, typed translations, languages
  data/       Preferences interface and in-memory store
shared/src/commonTest/   Simulation, map, street network, camera, language, and city session tests
shared/src/iosMain/      iOS entry point and UserDefaults persistence
shared/src/jvmMain/      Desktop preferences
shared/src/jvmTest/      Sprite and map rendering tests, difficulty benchmark
androidApp/             Android activity, manifest, icon, preferences
iosApp/                 SwiftUI host, orientation/lifecycle, Xcode project
desktopApp/             Portrait desktop launcher and macOS packaging
playstore/              Google Play listing for the beta, and the 512 px store icon
appstore/               App Store and TestFlight listing for the beta
docs/                   Support and privacy pages for GitHub Pages
```

Simulation runs at a fixed 60 Hz, independently of display refresh rate. Long frame stalls are capped instead of fast-forwarding. Geometry and navigation edges are cached; randomness can be seeded for repeatable tests. No network connection is required for gameplay.

## Verify

```sh
./gradlew :shared:jvmTest :desktopApp:classes
./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
```

`jvmTest` runs 94 tests: 76 common tests and 18 JVM-only tests. The 76 common tests are:

- **Game rules (15):** whole-route victory (also in every city at both map sizes when no enemies spawn), the defeat threshold, speech stops, shooting and movement, bomb grouping and cooldowns, pause behavior, frame-rate independence, seeded waves, waves that keep coming when `maxMapScale` is 1, and enemies walking back to the street.
- **Maps (29):** for every city at three map sizes, the real order of the stops, route lengths within 15% of each other across the cities, no building on a street, every name labeled, street names clear of the squares, every street reachable, walkable starts, paths that never cut building corners, every landmark in a city block near its place on the route (on the map, labeled, and clear of the other landmarks and of the buildings), props in city blocks (on the map, clear of the buildings, the landmarks, and each other, every kind on a 2× map, and the rest of the map the same as without props), repeatable seeds, the limits of the random map scale, and generation time. One checks that a leg without names gets no labels. One checks that a landmark too big for the blocks near its place shrinks. Nine more check that a bad template fails with a clear message: fewer than two stops, a number of legs that does not match the stops, a street with fewer than two points, a street that never enters the map, a star square so far off the map that a spoke misses it, a street with no room for its label, a first leg that is too short for its label, a landmark at a stop that does not exist, and a landmark with no room even at its smallest size.
- **Street network (7):** clearance, paths that stay on streets, taps inside blocks, pruning of unreachable streets, cells past the map edge, repeated searches that return the same paths as fresh ones, and a clear error when the route does not start on a street.
- **Camera (5):** tracking only the defender, clamping at the world edges (also on a larger map), the top inset that keeps the top of the map below the HUD, coordinate mapping, and smoothing.
- **Languages (8):** the English default, language selection and persistence, saved-value compatibility and fallback, complete translations in every language, the game's name and wordmark in every language, and each city's souvenir line in every language.
- **City session (12):** the Belgrade default, saving the city and the checkbox, language switching with and without the checkbox, the best rescue per city, fallbacks for bad saved values, and which city a game uses. With the box checked, Milan switches to Italian.

Of the 18 JVM-only tests, two render the actual sprite into an image. They check the gun's attachment at eight aiming angles and multiple character positions, and the gun and selection ring under camera translation and zoom, so incorrect canvas pivots are caught. Six check the map art. One draws the map of every city at both sizes and checks that the route road shows on top of the streets. One draws the map of every city and checks that each landmark's main colour fills part of its footprint, so the landmark is drawn in its place. One does the same for each prop with its own colour, which nothing else on the map uses. One checks that the pigeons fly up when the defender stands on their flock, and that a defender far away leaves them as they are. One checks that culling keeps only the buildings, the landmarks, and the props in view. One checks that no crosswalk lies inside a square. One draws each city's souvenir postcard and checks that its food fills the middle of the card, with no other city's food colour there. Three check the country flags in the city dialog: Serbia's stripes run red, blue, white from the top, France's run blue, white, red from the left, and Italy's run green, white, red from the left. One checks that the brand in every language keeps its size when the system text size grows, because its two lines are pinned to the icon's height.

Four drive the real screens (`CityFlowTest`). Two use taps: **ENTER THE CITY** on the welcome screen and **DEFEND THE MARCH** on the main menu both open the city dialog, and the dialog's **DEFEND THE MARCH** starts the game in the chosen city. The third opens the city dialog on a 360 × 700 dp screen in every language. The dialog's **DEFEND THE MARCH** button must show whole, so all the city cards and the checkbox fit without scrolling, and the button must start the game. The fourth plays a game with no enemies in each city to the end. The result dialog must show the city's souvenir at 430 and 360 dp wide, and a lost game must show none. On a 360 × 700 dp screen, both buttons of the dialog must show whole, so the dialog fits without scrolling.

The seventeenth is the difficulty benchmark, `DifficultyBenchmarkTest` (about 12 seconds). A scripted hero plays seeds 1 to 40 in each city, at 1× and at `maxMapScale`. The mean number of survivors must stay within 12% of 59.6, the mean on the old fixed map before cities were added. At each size, the gap between the highest and the lowest city mean must be at most 12% of the 59.6 baseline (about 7). If a change to maps, spawn timing, or enemies makes the game much easier or harder, this test fails.

Test report: `shared/build/reports/tests/jvmTest/index.html`.

Last verified on this development machine: both commands above succeed, and all 94 tests pass. The standalone macOS app, the Xcode simulator build, the Android build, and physical devices were not checked for the city, map, landmark, and prop changes, for Italian, or for the translated game name.

Pinned toolchain: Kotlin 2.4.21, Compose Multiplatform 1.12.1, Android Gradle Plugin 9.3.1, Gradle 9.6.1. Compatibility references: [Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), [Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html), and the [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin).

## Assets

Vector artwork is implemented in Kotlin. The only images are the app icons for the stores and the pages (`playstore/icon-512.png`, the iOS `AppIcon`, and `docs/img/icon.png`), drawn from the Android launcher icon `ic_kolona.xml`. Barlow Condensed and DM Sans are bundled under the SIL Open Font License; copies are in `licenses/`. No reference-site photographs, JavaScript, or external runtime assets are bundled.
