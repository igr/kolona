# App Store listing

Basic information for the App Store page, the beta on TestFlight, and the 1.0.0 release. Copy these into App Store Connect. Character counts are within Apple's limits.

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

## App Store page

### Promotional Text (144/170)

Guard a protest of 72 people through real streets of Belgrade, Paris and Milan. Hold the line, throw bombs, and bring the protest to the square.

### Description (1288/4000)

The same text as the full description of the Google Play listing (`playstore/listing.md`).

### Keywords (92/100)

escort,defense,barricade,crowd,demonstration,rally,action,arcade,belgrade,paris,milan,street

The words of the name are already found, so they are not keywords.

### Screenshots

`screenshots/` holds the screenshots, taken on the iPhone Simulator, six of each size: `1-welcome.png`, `2-cities.png`, `3-belgrade.png`, `4-paris.png`, `5-milan.png` and `6-souvenir.png`.

- iPhone 6.9" Display: `screenshots/6.9-inch/`, 1320 × 2868, from the iPhone 17 Pro Max. App Store Connect asks for this size, and can scale it for the smaller iPhones.
- iPhone 6.3" and 6.1" Display: `screenshots/6.3-inch/`, 1206 × 2622, from the iPhone 17 Pro.

| Shot | What it shows |
|---|---|
| `1-welcome` | The welcome screen |
| `2-cities` | The city dialog, with the best rescues 58 in Belgrade and 72 in Paris |
| `3-belgrade` | A game in Belgrade: thugs come at the protest by Beograđanka |
| `4-paris` | A game in Paris, by the Cirque d'Hiver |
| `5-milan` | A game in Milan, at Porta Venezia |
| `6-souvenir` | A won game in Paris: 72 of 72 people saved, and the croissant |

To take them again, install a Debug build on the simulator, and set a clean status bar:

```bash
xcrun simctl status_bar booted override --time 9:41 --dataNetwork wifi --wifiMode active --wifiBars 3 --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100
```

Before each launch, write the saved values into the app's preferences: English, the city of the shot, the language box unticked, and the best rescues.

```bash
P="$(xcrun simctl get_app_container booted dev.oblac.kolona data)/Library/Preferences/dev.oblac.kolona"
xcrun simctl spawn booted defaults write "$P" kolona.language -string en
xcrun simctl spawn booted defaults write "$P" kolona.city -string paris
xcrun simctl spawn booted defaults write "$P" kolona.city.language -string false
xcrun simctl spawn booted defaults write "$P" kolona.best.belgrade -string 58
xcrun simctl spawn booted defaults write "$P" kolona.best.paris -string 72
```

Then launch the app, tap ENTER THE CITY and DEFEND THE PROTEST, and take the shots with `xcrun simctl io booted screenshot <file>`, about 5 to 10 seconds into the game. For the win, launch with `-calm`: a Debug build launched with it sends no attackers, so the protest reaches the square after 3 minutes.

```bash
xcrun simctl launch booted dev.oblac.kolona -calm
```

The screenshots must have no alpha channel.

### Notes for App Review

The same as the notes for Beta App Review below.

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

No account or login is needed. Tap ENTER THE CITY, choose a city, and tap DEFEND THE PROTEST. Tap anywhere on the map to move the defender; it walks around buildings. When the defender stops, it raises a barricade and fires at nearby threats. Swipe from the defender to where a bomb should land to throw it. The police and thugs are generic cartoon figures: a beaten figure falls over and fades away, with no blood. The game works offline and collects no data.

## App icon

`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon-1024.png` (1024 × 1024, no transparency), drawn from the Android launcher icon.

## Build

- `just beta-ios` archives this commit and uploads it to App Store Connect with `iosApp/ExportOptions.plist`, as the account signed in to Xcode. The Organizer works too (Product > Archive, then Distribute App > TestFlight & App Store). The signing team is K5GKT84X3Y, as in sisyphus.
- Version 1.0.0. `just beta-ios` sets the build number to the commit count, so each upload is higher. `Info.plist` reads the version and the build number from the project.
- `just release-ios` archives this commit and uploads it to App Store Connect in the same way. Add the build to version 1.0.0, fill in the App Store page, and submit it for review.
- `PrivacyInfo.xcprivacy` declares the UserDefaults and file timestamp APIs, as in sisyphus. `Info.plist` sets `ITSAppUsesNonExemptEncryption` to NO, so uploads skip the encryption question.
- The project targets iPhone only, so the App Store needs only iPhone screenshots.

## Pages

The support page and the privacy policy are `docs/index.html` and `docs/privacy.html`. GitHub Pages serves them from `docs/` on `main`, so a push to `main` updates them.

## Review

Internal testers (up to 100 App Store Connect users) need no review. External testers need Beta App Review. Guideline 1.1.2 says that game enemies cannot target only a real government or another real entity. The police here are generic, but review may still ask; the review notes answer that.
