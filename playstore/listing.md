# Google Play listing

Basic information for the first beta, on the internal testing track. Copy these into Play Console. Character counts are within Google's limits.

## App details

| Field | Value |
|---|---|
| App name (12/30) | Čuvar Kolone |
| Package name | dev.oblac.kolona |
| Default language | English (United States) |
| App or game | Game |
| Free or paid | Free |
| Category | Action |
| Contact email | Required and shown on the store page. Choose the address you want to show. |
| Website | https://igr.github.io/kolona/ (in `docs/`, not online yet) |
| Privacy policy | https://igr.github.io/kolona/privacy.html (in `docs/`, not online yet) |

The name under the icon on the phone is also "Čuvar Kolone". It matches the name of the web game this one is based on. If that game is not yours, Google can reject the name as impersonation; then use "Guardian of the March".

## Short description (76/80)

Guard a protest march through Belgrade, Paris and Milan to the final square.

## Full description (1306/4000)

72 people. One road to freedom. You stand between them and chaos.

Čuvar Kolone (Guardian of the March) is a portrait action game. A protest march walks through a real city, from one square to the next, and you are its guardian.

- Tap the street to move. Your defender finds a way around the buildings. Markers at the edge of the screen show where the march and the threats are.
- Stop to raise a barricade and fire automatically at threats in range. You cannot fire while running.
- Tap a group of four or more attackers to throw a bomb. Bombs never hurt the marchers.
- Thugs, police and cars come in waves. The march stops for two speeches; hold the line until it moves on.
- Get at least 36 of the 72 people to the final square.

Three cities, three real routes:
- Belgrade: from Slavija through Terazije to Republic Square.
- Paris: from République through Bastille to Nation.
- Milan: from Porta Venezia through San Babila to Piazza del Duomo.

Every march draws a new map with real street names, landmarks and street life: a pljeskavica grill and a brass band in Belgrade, a crêpe stand and an accordion player in Paris, a gelato stand and a row of scooters in Milan. Win, and you take home a souvenir postcard.

Play in English, Serbian, French or Italian.

No ads. No accounts. No data collected.

## Beta release

| Field | Value |
|---|---|
| Track | Internal testing. Add up to 100 testers by email list. |
| Release name | 0.1.0 (1) |

### Release notes (274/500)

First beta. Play a march in Belgrade, Paris and Milan. Try the four languages with the globe button in the main menu (pause the game and tap MAIN MENU) or with the language box in the city dialog. Tell us what felt too hard or too easy, and what looked wrong on your device.

## App content

| Question | Answer |
|---|---|
| App access | All functionality is available without special access. |
| Ads | No, the app has no ads. |
| Content rating | Category: Game. Violence: yes, cartoon violence against human characters (police and thugs), with no blood; a beaten character leaves the map. Answer "No" to every other question. Expect PEGI 12 / ESRB Everyone 10+. |
| Target audience | 13 and older. Younger age groups add the Families policy. |
| Data safety | The app does not collect or share any user data. The language, the city and the best rescues stay on the device. |
| Advertising ID | No. |
| Government, financial, health, news apps | No. |

## Graphics

- App icon (512 × 512): `icon-512.png`, drawn from the Android launcher icon.
- Feature graphic (1024 × 500) and phone screenshots: not made yet. Closed testing and production need them; internal testing does not.

## Build

- `./gradlew :androidApp:bundleRelease` makes `androidApp/build/outputs/bundle/release/androidApp-release.aab`. Upload it in Internal testing > Create new release.
- Version 0.1.0, version code 1. Every upload needs a higher version code: change `versionCode` in `androidApp/build.gradle.kts`.
- The bundle is signed with the upload key named in `keystore.properties`, which is not in git. Without that file the bundle is unsigned, and Play rejects it. Make the key once; `keytool` asks for a password and your name:

  ```sh
  keytool -genkeypair -v -keystore ~/.android/kolona-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
  ```

  Then write `keystore.properties` in the project root:

  ```properties
  storeFile=/Users/igorspasic/.android/kolona-upload.jks
  storePassword=<the password>
  keyAlias=upload
  keyPassword=<the same password>
  ```

- Back up both files. Google Play signs the app with its own key; if the upload key is lost, Google support can reset it.

## Pages

The website and the privacy policy are `docs/index.html` and `docs/privacy.html`. To put them online at https://igr.github.io/kolona/, serve `docs/` from the `main` branch with GitHub Pages. The kolona repo is private: Pages on a private repo needs a paid plan, and the GitHub issue link on the pages works only when the repo is public.
