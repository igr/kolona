# App Store listing

Basic information for the first beta, on TestFlight. Copy these into App Store Connect. Character counts are within Apple's limits.

## App information

| Field | Value |
|---|---|
| Name (12/30) | Čuvar Kolone |
| Subtitle (21/30) | Guardian of the March |
| Bundle ID | dev.oblac.kolona |
| SKU | kolona |
| Primary language | English (U.S.) |
| Category | Games: Action, Strategy |
| Price | Free |
| Copyright | 2026 Igor Spasic |
| Age rating | Cartoon or Fantasy Violence: Frequent/Intense, because firing at cartoon police and thugs is the core of the game. Answer "None" or "No" to every other question. The result should be 13+. |
| App Privacy | Data Not Collected |
| Support URL | https://igr.github.io/kolona/ |
| Privacy Policy URL | https://igr.github.io/kolona/privacy.html |

The name under the icon on the phone is also "Čuvar Kolone". It matches the name of the web game this one is based on. If that game is not yours, Apple can reject the name (guideline 4.1); then use "Guardian of the March".

## TestFlight

| Field | Value |
|---|---|
| Feedback email | Required for external testers. Choose the address. |
| Marketing URL | Optional. Leave it empty. |
| Sign-in required | No |
| Beta App Review contact | Your name, phone and email. |

### Beta App Description (333/4000)

Čuvar Kolone (Guardian of the March) is a portrait action game. Guard a protest march of 72 people through real streets of Belgrade, Paris and Milan. Tap to move, stop to raise a barricade and fire, and tap a group of attackers to throw a bomb. Get at least 36 people to the final square. Play in English, Serbian, French or Italian.

### What to Test (274/4000)

First beta. Play a march in Belgrade, Paris and Milan. Try the four languages with the globe button in the main menu (pause the game and tap MAIN MENU) or with the language box in the city dialog. Tell us what felt too hard or too easy, and what looked wrong on your device.

### Notes for Beta App Review

No account or login is needed. Tap ENTER THE CITY, choose a city, and tap DEFEND THE MARCH. Tap the street to move the defender. When the defender stops, it raises a barricade and fires at nearby threats. Tap a group of four or more attackers to throw a bomb. The police and thugs are generic cartoon figures: a beaten figure leaves the map, with no blood. The game works offline and collects no data.

## App icon

`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon-1024.png` (1024 × 1024, no transparency), drawn from the Android launcher icon.

## Build

- Archive with the Kolona scheme (Product > Archive) and upload from the Organizer (Distribute App > TestFlight & App Store). The signing team is K5GKT84X3Y, as in sisyphus.
- Version 0.1.0, build 1. Every upload needs a higher build number: change `CURRENT_PROJECT_VERSION` in the Xcode project. `Info.plist` reads the version and the build number from the project.
- `PrivacyInfo.xcprivacy` declares the UserDefaults and file timestamp APIs, as in sisyphus. `Info.plist` sets `ITSAppUsesNonExemptEncryption` to NO, so uploads skip the encryption question.
- The project targets iPhone and iPad, and TestFlight runs on both. The App Store will then also need iPad screenshots. Set the target to iPhone only if you do not want iPad.

## Pages

The support page and the privacy policy are `docs/index.html` and `docs/privacy.html`. GitHub Pages serves them from `docs/` on `main`, so a push to `main` updates them.

## Review

Internal testers (up to 100 App Store Connect users) need no review. External testers need Beta App Review. Guideline 1.1.2 says that game enemies cannot target only a real government or another real entity. The police here are generic, but review may still ask; the review notes answer that.
