# App Store listing

Basic information for the first beta, on TestFlight. Copy these into App Store Connect. Character counts are within Apple's limits.

## App information

| Field | Value |
|---|---|
| Name (16/30) | Protest Defender |
| Subtitle (30/30) | Lead the protest to the square |
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

The name under the icon on the phone is also "Protest Defender"; the home screen may shorten it. If the store says the name is taken, add a word, for example "Protest Defender Game".

## TestFlight

| Field | Value |
|---|---|
| Feedback email | Required for external testers. Choose the address. |
| Marketing URL | Optional. Leave it empty. |
| Sign-in required | No |
| Beta App Review contact | Your name, phone and email. |

### Beta App Description (307/4000)

Protest Defender is a portrait action game. Guard a protest of 72 people through real streets of Belgrade, Paris and Milan. Tap to move, stop to raise a barricade and fire, and swipe from your defender to throw a bomb. Get at least 36 people to the final square. Play in English, Serbian, French or Italian.

### What to Test (273/4000)

First beta. Play a game in Belgrade, Paris and Milan. Try the four languages with the globe button in the main menu (pause the game and tap MAIN MENU) or with the language box in the city dialog. Tell us what felt too hard or too easy, and what looked wrong on your device.

### Notes for Beta App Review

No account or login is needed. Tap ENTER THE CITY, choose a city, and tap DEFEND THE PROTEST. Tap anywhere on the map to move the defender; it walks around buildings. When the defender stops, it raises a barricade and fires at nearby threats. Swipe from the defender to a group of at least four attackers to throw a bomb there. The police and thugs are generic cartoon figures: a beaten figure falls over and fades away, with no blood. The game works offline and collects no data.

## App icon

`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon-1024.png` (1024 × 1024, no transparency), drawn from the Android launcher icon.

## Build

- `just beta-ios` archives this commit and uploads it to App Store Connect with `iosApp/ExportOptions.plist`, as the account signed in to Xcode. The Organizer works too (Product > Archive, then Distribute App > TestFlight & App Store). The signing team is K5GKT84X3Y, as in sisyphus.
- Version 0.1.0. `just beta-ios` sets the build number to the commit count, so each upload is higher. `Info.plist` reads the version and the build number from the project.
- `PrivacyInfo.xcprivacy` declares the UserDefaults and file timestamp APIs, as in sisyphus. `Info.plist` sets `ITSAppUsesNonExemptEncryption` to NO, so uploads skip the encryption question.
- The project targets iPhone and iPad, and TestFlight runs on both. The App Store will then also need iPad screenshots. Set the target to iPhone only if you do not want iPad.

## Pages

The support page and the privacy policy are `docs/index.html` and `docs/privacy.html`. GitHub Pages serves them from `docs/` on `main`, so a push to `main` updates them.

## Review

Internal testers (up to 100 App Store Connect users) need no review. External testers need Beta App Review. Guideline 1.1.2 says that game enemies cannot target only a real government or another real entity. The police here are generic, but review may still ask; the review notes answer that.
