# Protest Defender

A portrait escort-defense game built with **Kotlin Multiplatform and Compose Multiplatform**. Android and iOS share the game engine, rendering, UI, and translations. A desktop app makes development and playtesting easier.

Inspired by [Čuvar kolone](https://cuvar-kolone.controid.chatgpt.site). This is a native Kotlin implementation, not a WebView wrapper. The maps, characters, and effects are drawn with Compose Canvas; fonts are bundled for offline use.

The brand is **Protest Defender**, the same in every language: the wordmark shows PROTEST above DEFENDER. It lives in `GameIdentity` in `shared/src/commonMain/kotlin/dev/oblac/kolona/i18n/Strings.kt`, with matching Android and iOS display names, desktop packaging, and desktop window title. Application IDs (`dev.oblac.kolona`) and saved preferences remain stable across brand changes.

The base Kotlin package and the Android, iOS, and desktop app ID are `dev.oblac.kolona`; shared code and generated resources use subpackages of the same base.

## Run

Use JDK 17 or 21. The Gradle wrapper is included and verifies the distribution checksum. The first build downloads dependencies.

The game uses gamekit, the library of the games in `~/prj/igr/gamekit`. Gradle takes it from the local Maven repository, `~/.m2`. Before the first build, publish the version that `gradle/libs.versions.toml` pins: see the README of gamekit.

### Desktop

```sh
./gradlew :desktopApp:run
```

The `justfile` in the project root has the same command as a shortcut: `just run`.

The preview uses a fixed portrait window. Click to move and use the on-screen pause/resume controls. Changing app focus automatically pauses gameplay.

On macOS, a standalone app can also be built:

```sh
./gradlew :desktopApp:createDistributable
open "desktopApp/build/compose/binaries/main/app/Protest Defender.app"
```

Close a running Gradle preview before launching another Gradle build in the same checkout; the running task can hold Gradle's project lock. The standalone app avoids this.

### Android

Install Android SDK Platform 37 and its build tools through Android Studio, then set `ANDROID_HOME` or add `sdk.dir=/absolute/path/to/sdk` to an untracked `local.properties` file. Open this folder in Android Studio and run `androidApp`, or:

```sh
./gradlew :androidApp:assembleDebug
```

For Google Play, `./gradlew :androidApp:bundleRelease` writes `androidApp/build/outputs/bundle/release/androidApp-release.aab`, shrunk with R8. It is signed with the upload key named in an untracked `keystore.properties`; without that file the bundle is unsigned. `playstore/listing.md` shows how to make the key.

The APK is written to `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Minimum API is 24. The activity requests portrait orientation; the shared UI also constrains the playfield to portrait when an OS or tablet window overrides orientation requests.
The activity follows a change of the font size, the display size, the language or the keyboard itself (`configChanges`), so Android does not make it again, and a game in play goes on.

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
These recipes come from `release.just` of gamekit, which the `justfile` imports.

- Commit first: the build number is the commit count, so every upload needs its own commit.
- Android needs the upload key from `keystore.properties` and the Play service account key in `~/.android/sisyphus-play.json`, with access to this app in Play Console. Google Play takes the first bundle of an app only by hand in Play Console; the recipes work after that.
- iOS uses the account signed in to Xcode and `iosApp/ExportOptions.plist`.

## Play

1. Welcome → **ENTER THE CITY** → choose Belgrade, Paris, or Milan in the city dialog → **DEFEND THE PROTEST** starts the game. Belgrade is preselected the first time.
2. Tap anywhere to move. The defender walks across the open ground and finds a way around buildings and landmarks. A tap on a building sends him to the nearest open spot beside it. Walking through protesters slows him down. The attackers walk the same way; only the protest keeps to its street.
3. Stop to raise a barricade and automatically shoot nearby threats. Moving disables shooting.
4. **Swipe from the defender** to a group of **at least four attackers** to throw a bomb: it lands where the finger lets go. Beaten attackers on the ground do not count. While you swipe, a dashed line and a circle show where it will land and what it will hit. They stay faint while the group in the circle is too small or the bomb recharges. A tap only moves the defender, and a swipe that starts away from him, or a very short one, throws nothing. Recharge takes **five seconds**. Bombs cannot damage protesters.
5. Escort at least **36 of 72 people** to the last square of the route: Trg Republike in Belgrade, Nation in Paris, Piazza del Duomo in Milan. The protest takes 150 seconds, plus two 15-second speech stops. Attackers include thugs, police, and cars. While the protest stops for a speech, thugs come in twice the number.

Every game, including a restart, builds a new map. The base world is **1400 × 1120 units**, and a random scale from 1× up to `maxMapScale` (2 by default) can make it larger; see Cities. Every size is wider than the phone's viewport. A smooth, bounded camera follows **only the defender**. At the top of the map, the camera can go past the map edge by the height of the phase box and the minimap, so they do not cover the top of the map. The small route overview and screen-edge markers show where the protest and threats are; they never move the camera. Gold identifies the defender, mint the protest, and coral threats. Protesters sway, bob, and wave their signs as they walk, each in their own rhythm. When the protest stops for a speech, they stand still. This is drawing only: it does not move them in the game. Protesters shout slogans in turn, such as "FIGHT THE SYSTEM!!" or "DOWN WITH CORRUPTION!". A new shout pops up over a different protester every 1.8 seconds, with one to three exclamation marks, as the one before fades out. A beaten person, protester or attacker, flashes, falls over, lies still for a moment, and fades out; a beaten car simply goes. As in the original, the map shakes: a little when an attacker hits the protest, more when a car is wrecked, and most at a bomb blast. The shake dies down within about half a second, and the HUD and the edge markers hold still.

Pause, resume, restart, return to menu, win/loss results, and the best successful rescue for each city are implemented. Ongoing runs are not restored after the app process is terminated.

## Cities

Belgrade (Serbia), Paris (France), and Milan (Italy) are all free. Belgrade is the default.

Every game starts with the city dialog: **ENTER THE CITY** on the welcome screen and **DEFEND THE PROTEST** on the main menu both open it. It has one card per city, with that city's best rescue once you have won there, and it fits a 360 × 700 dp phone without scrolling. Tap a card to choose that city, then **DEFEND THE PROTEST** in the dialog starts the game there. The choice is saved locally and preselected next time. Restarting from the pause or result screen keeps the same city.

- **Belgrade:** Slavija → Kralja Milana → Terazije → Kolarčeva → Trg Republike.
- **Paris:** République → Bd du Temple / Bd Beaumarchais → Bastille → Rue du Faubourg Saint-Antoine → Nation.
- **Milan:** Porta Venezia → Corso Venezia → San Babila → Corso Vittorio Emanuele II → Piazza del Duomo, the route of the 25 April march.

As in the original web game, each map has one street: the route of the protest, which widens into a square at each stop. The rest of the map is open ground with buildings, landmarks, and trees. Only the protest keeps to the street. The defender and the attackers walk anywhere, but never through a building or a landmark.

Buildings stand one to a lot on a loose grid, at least 48 units apart, so a person (about 25 units wide) fits between any two. About one lot in five stays empty. Each city has its own buildings. Paris has cream stone fronts under grey-blue zinc mansard roofs, with dormers and rows of chimney pots. Belgrade has red tile roofs over plaster fronts, mixed with grey concrete blocks that have flat roofs and vents. Milan has palazzi with ochre (giallo Milano) and terracotta plaster fronts and green shutters. Most have a ring of tiled roof around a dark inner courtyard (cortile). Some have a flat roof with a terrace of plants under a parasol. The colours stay dark, so the route, the protest, the threats, the names, and the defender stay the brightest things on the map.

Each map also has landmarks near their real places. Each landmark has a map label with its local name:

- **Belgrade:** Hram Svetog Save by Slavija, Beograđanka on Kralja Milana near Slavija, Hotel Moskva at Terazije, Narodni muzej on Trg Republike.
- **Paris:** Cirque d'Hiver on Bd du Temple near République, Opéra Bastille at Bastille.
- **Milan:** Duomo di Milano and Galleria Vittorio Emanuele II on Corso Vittorio Emanuele II by Piazza del Duomo, Caselli di Porta Venezia on Corso Venezia near Porta Venezia. The Duomo shows its white marble roofs full of spires, the golden Madonnina on the tallest spire, and the pointed front over five portals. The Galleria shows its cross of glass vaults under the glass dome. The Caselli are the two neoclassical toll houses of the old gate.

The template gives each landmark a point on the route and a preferred side of the protest (`landmarks` in `game/Cities.kt`). The landmark takes the closest free spot off the street near that point, never on the street or a square. This spot can be across the street, on the other side of the protest. The label goes below the landmark, or above it at the bottom edge of the map. Landmarks and their labels keep clear of the square names. On a small map, the squares and the other landmarks can leave no room. A landmark that has no room shrinks in steps of 10%, down to at most 20% of its size. Large shrinks are rare. Like buildings, landmarks stand in everyone's way, and buildings keep at least 48 units from them. The welcome and menu artwork show the landmarks too.

Each map also has street life along the street. These small drawings tell more about the city. They have no effect on the game: nobody has to walk around them. They show no brand names.

- **Paris:** a boulangerie with baguettes and croissants, a crêpe stand, a café terrace with a red awning and small round tables, a green Morris column with posters, a Wallace drinking fountain, an Art Nouveau Métro entrance, a green newsstand, an accordion player, a rack of shared bikes, and pigeons.
- **Belgrade:** a grill kiosk with pljeskavica and ćevapi, a pekara with burek, a kafana terrace with checked tablecloths, a trafika, street chess tables, a brass band (trubači), and pigeons.
- **Milan:** a gelato cart under a striped parasol, a pasticceria window with panettoni, an espresso bar with a small terrace, the green cast-iron drinking fountain (vedovella) with the city's arms, a row of parked scooters, a fashion boutique window with mannequins, and pigeons.

The template lists the kinds of props for each city (`props` in `game/Cities.kt`). The generator places the props last, after the labels:

- A prop stands next to the street, off it, and faces it. It keeps clear of the street, the buildings, the landmarks, the trees, the labels, and the other props.
- Props go near the landmarks, the squares, and points along the route, where the player looks.
- Each kind comes once before a kind repeats. There is about one prop for each 140 units of route: about 11 on a 1× map and up to 26 on a 2× map. On a 1× map, a kind can be missing when there is no room for it. Every kind appears on a 2× map.
- Pigeons are the exception. A flock of 3 to 6 birds sits on each square that the protest crosses after the start, next to the path of the protest.
- Props use their own random numbers. The street, buildings, trees, labels, and the game stay exactly as they are without props, and the difficulty benchmark gives the same numbers.

In the game, the grill smokes, the musicians sway, the gelato vendor rocks, the guests at the tables lean and nod, and the pigeons peck. When the defender or a protester comes within about 60 units, the pigeons fly up and away. The welcome and menu artwork show the props too, without motion.

Supporters line the route: a row of 2 to 5 people about every 70 units, at the edge of the street on either side. Some rows hold a banner up on two poles. A banner row always faces the screen, so the banner reads the same along a street that runs in any direction. The supporters hop and wave, each in their own rhythm. While the protest passes within 130 units, they cheer harder with both arms up, and the guests at the tables raise an arm. Like the props, supporters are drawing only: nobody walks around them. They use their own random numbers, so the rest of the map and the difficulty benchmark stay the same.

When the protest wins, the result dialog shows a souvenir postcard from the city: a croissant from Paris, a pljeskavica from Belgrade, or a panettone from Milan, with its local name. A lost game shows no souvenir.

Maps are generated, not drawn by hand:

- Every game generates a new map from the city's template (`game/Cities.kt`, `game/CityMap.kt`). The same seed gives the same map. The welcome and menu artwork show a fixed preview map of the chosen city.
- The template has the real names of the squares and of the streets on the route, in the real order of the march. The map is not a precise plan: the route wobbles at random. Every name appears as a map label. Labels use the local names in every language.
- Buildings and landmarks are solid; the rest of the map is open ground (`game/Ground.kt`: a clearance raster, A* on a 20-unit grid with a binary heap, and paths straightened into clear lines). Ground that the route's start cannot reach, such as a yard walled in by buildings, is pruned, so nobody gets stuck.
- Trees grow on the open ground in every city: about 40 on a 1× map, and more on a larger map. Slavija, a roundabout, also has a ring of trees. Trees are decoration and stand in nobody's way.
- The base world is 1400 × 1120 units. Each game multiplies it by a random scale from 1 to `GameConfig.maxMapScale`, which is 2 by default (`game/GameEngine.kt`).
- The protest takes 150 seconds plus two 15-second speeches on every map, so protesters walk slower on a small map. Enemy waves compensate. The base wait between waves is `spawnSeconds` (3.2 s) on a 1× map and `spawnSecondsAtMaxScale` (2.5 s) on a `maxMapScale` map. Sizes in between interpolate linearly.
- A difficulty benchmark (see Verify) checks that all cities stay about as hard as the game was before cities, at the smallest and the largest map size.

Below the city cards, the **Also switch the language** checkbox is unchecked by default. When it is checked, choosing Belgrade switches the game to Serbian, choosing Paris switches it to French, and choosing Milan switches it to Italian. Checking the box switches to the current city's language at once. The globe button on the main menu still changes the language at any time and does not uncheck the box. The checkbox is saved locally, like the city.

The best rescue is saved per city, and the city dialog shows it on each played city's card. The best rescue saved before cities existed counts as Belgrade's.

## Languages

- English is the default. Serbian (Latin), French, and Italian are also available. All four are free; there is no unlock or purchase.
- The globe button at the top right of the main menu opens the language list. The choice is saved locally and restored on the next launch. A missing or unknown saved value falls back to English.
- All menu text, instructions, wave names, and result text are translated into every language, and so are the city and country names. The game's name, Protest Defender, is the same in every language. Map labels are not translated. They keep the local names of squares and streets, like street signs.
- The **Also switch the language** checkbox in the city dialog can change the language together with the city: Belgrade switches to Serbian, Paris to French, and Milan to Italian. See Cities.

Translations use a typed `Copy` catalog; each key requires Serbian, English, French, and Italian. To add another language, extend `Language` and add a translation to every `Copy` entry.

## Project layout

```text
shared/src/commonMain/kotlin/dev/oblac/kolona/
  game/       Pure Kotlin simulation, city templates, map generation, walkable ground with A*, geometry, camera
  ui/         Compose screens, vector art, frame loop, app session
  i18n/       Brand, typed translations, languages
shared/src/commonTest/   Simulation, map, ground, camera, language, and city session tests
shared/src/iosMain/      iOS entry point
shared/src/jvmTest/      Sprite and map rendering tests, difficulty benchmark
androidApp/             Android activity, manifest, icon
iosApp/                 SwiftUI host, orientation/lifecycle, Xcode project
desktopApp/             Portrait desktop launcher and macOS packaging
playstore/              Google Play listing for the beta, and the 512 px store icon
appstore/               App Store and TestFlight listing for the beta
docs/                   Support and privacy pages for GitHub Pages
```

The preferences stay in the `Store` of gamekit, in the same places as before gamekit: the user defaults with the prefix `kolona.` on iOS, the shared preferences `kolona` on Android, and the Java preferences node `dev/oblac/kolona` on the desktop.

Simulation runs at a fixed 60 Hz, independently of display refresh rate. Long frame stalls are capped instead of fast-forwarding. Geometry and navigation edges are cached; randomness can be seeded for repeatable tests. No network connection is required for gameplay.

## Verify

```sh
./gradlew :shared:jvmTest :desktopApp:classes
./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
```

`jvmTest` runs 127 tests: 94 common tests and 33 JVM-only tests. The 94 common tests are:

- **Game rules (26):** a beaten person falls where they stood and leaves after a moment (a car leaves no body), whole-route victory (also in every city at both map sizes when no enemies spawn), the defeat threshold, speech stops, twice as many thugs while the protest stops, shooting and movement, bombs that land where they are thrown, a bomb that needs four attackers on the map in its blast, where beaten ones do not count, their recharge, a tap on a group that only moves the hero, pause behavior, frame-rate independence, seeded waves, waves that keep coming when `maxMapScale` is 1, the defender stopping on an open spot off the street, an attacker inside a building walking out, a barricade that never shoves an attacker into a building, the screen shake for a hit on the protest, a wrecked car, and a bomb blast, which settles within a second, and attackers that never stand on top of each other.
- **Maps (27):** for every city at three map sizes, the real order of the stops, route lengths within 15% of each other across the cities, no building on the street, buildings at least 48 units from each other and from the landmarks, open ground that the defender can walk to, trees in every city, every name labeled, walkable starts, paths that never cut building or landmark corners, few pruned cells, taps on buildings and at the map corners that end on open ground, every landmark off the street near its place on the route (on the map, labeled, and clear of the other landmarks and of the buildings), props off the street (on the map, clear of the buildings, the landmarks, and each other, every kind on a 2× map, and the rest of the map the same as without props), rows of supporters all along the route (off the street but near it, clear of the buildings, the landmarks, the props, and each other, some with a banner that faces the screen), city codes, repeatable seeds, the limits of the random map scale, and generation time. One checks that a leg without names gets no labels. One checks that a landmark too big for its place shrinks. Five more check that a bad template fails with a clear message: fewer than two stops, a number of legs that does not match the stops, a first leg that is too short for its label, a landmark at a stop that does not exist, and a landmark with no room even at its smallest size.
- **Ground (9):** the distance to buildings, landmarks, and the map edge, straight lines across open ground, the short way around a building, a 48-unit gap between buildings that people walk through, a tap on a building that ends beside it, pruning of a yard walled in by buildings, repeated searches that return the same paths as fresh ones, a clear error when the route starts inside a building, and nobody walking off the map.
- **Camera (5):** tracking only the defender, clamping at the world edges (also on a larger map), the top inset that keeps the top of the map below the HUD, coordinate mapping, and smoothing.
- **Languages (9):** the English default, language selection and persistence, saved-value compatibility and fallback, complete translations in every language, the welcome text that names the protesters without a count, each city's souvenir line in every language, and the exclamation marks of a shout, with a space before them in French.
- **Bomb swipe (5):** a swipe from the defender to a group throws a bomb where it ends. A swipe that starts away from the defender, a short one, a cancelled one, or one while the bomb recharges throws nothing.
- **City session (13):** the Belgrade default, saving the city and the checkbox, language switching with and without the checkbox, the best rescue per city (and every city's best for the city dialog), fallbacks for bad saved values, and which city a game uses. With the box checked, Milan switches to Italian.

Of the 33 JVM-only tests, two render the actual sprite into an image. They check the gun's attachment at eight aiming angles and multiple character positions, and the gun and selection ring under camera translation and zoom, so incorrect canvas pivots are caught. Seven check the map art. One draws the map of every city at both sizes and checks that the route road shows on top of the ground and the squares. One draws the map of every city and checks that each landmark's main colour fills part of its footprint, so the landmark is drawn in its place. One does the same for each prop with its own colour, which nothing else on the map uses. One checks that the pigeons fly up when the defender stands on their flock, and that a defender far away leaves them as they are. One checks that culling keeps only the buildings, the landmarks, the props, and the supporters in view. One checks that no crosswalk lies inside a square. One checks that the view jolts to a new spot every frame, never farther than the shake, and holds still with no shake. One draws each city's souvenir postcard and checks that its food fills the middle of the card, with no other city's food colour there. Three check the country flags in the city dialog: Serbia's stripes run red, blue, white from the top, France's run blue, white, red from the left, and Italy's run green, white, red from the left. Nine check the protesters: a walking protester steps and sways, a stopped protest stands still, protesters do not move in step, and a beaten protester lies down. The shouts go through every slogan, never the same one twice in a row, with one to three exclamation marks. They come from different protesters, two in a row from opposite halves of the protest, never from a sign holder, and never from a beaten protester or one who has arrived. The bubble floats above the speaker's head and fades out at the end. Four check the people by the street: supporters hop and wave, they cheer harder while the protest passes but not for a protest far away, a banner row holds its banner over the heads across most of the row, and the guests at the tables move and raise an arm for the protest. One checks that the brand keeps its size when the system text size grows, because its two lines are pinned to the icon's height. One checks that the brand draws the same raised fist as the app icon.

Four drive the real screens (`CityFlowTest`). Two use taps: **ENTER THE CITY** on the welcome screen and **DEFEND THE PROTEST** on the main menu both open the city dialog, and the dialog's **DEFEND THE PROTEST** starts the game in the chosen city. The third opens the city dialog on a 360 × 700 dp screen in every language, with a best rescue on every card. The dialog's **DEFEND THE PROTEST** button must show whole, so all the city cards and the checkbox fit without scrolling, and the button must start the game. The fourth plays a game with no enemies in each city to the end. The result dialog must show the city's souvenir at 430 and 360 dp wide, and a lost game must show none. On a 360 × 700 dp screen, both buttons of the dialog must show whole, so the dialog fits without scrolling.

The last is the difficulty benchmark, `DifficultyBenchmarkTest` (about 12 seconds). A scripted hero plays seeds 1 to 40 in each city, at 1× and at `maxMapScale`. The mean number of survivors must stay within 12% of 59.6, the mean on the old fixed map before cities were added. At each size, the gap between the highest and the lowest city mean must be at most 12% of the 59.6 baseline (about 7). If a change to maps, spawn timing, or enemies makes the game much easier or harder, this test fails.

Test report: `shared/build/reports/tests/jvmTest/index.html`.

Last verified on this development machine: both commands above succeed, and all 100 tests pass. The standalone macOS app, the Xcode simulator build, the Android build, and physical devices were not checked for the city, map, landmark, and prop changes, for Italian, or for the rename to Protest Defender.

Pinned toolchain: Kotlin 2.4.21, Compose Multiplatform 1.12.1, Android Gradle Plugin 9.3.1, Gradle 9.6.1. Compatibility references: [Kotlin](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html), [Compose](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html), and the [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin).

## Assets

Vector artwork is implemented in Kotlin. The only images are the app icons for the stores and the pages (`playstore/icon-512.png`, the iOS `AppIcon`, and `docs/img/icon.png`), drawn from the Android launcher icon `ic_kolona.xml`: a flat gold raised fist on the dark night blue, with the arm running off the bottom edge. The brand in the header draws the same fist in ink on gold (`Fist` in `KolonaApp.kt`). Barlow Condensed and DM Sans are bundled under the SIL Open Font License; copies are in `licenses/`. No reference-site photographs, JavaScript, or external runtime assets are bundled.
