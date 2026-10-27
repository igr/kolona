build := `git rev-list --count HEAD`

_default:
    @just --list

# runs the game in a desktop window
run:
    ./gradlew :desktopApp:run

# uploads this commit to Google Play internal testing and to TestFlight
beta: beta-android beta-ios

# uploads this commit to Google Play internal testing
beta-android: _committed
    ./gradlew :androidApp:publishReleaseBundle -PversionCode={{build}}

# uploads this commit to TestFlight
beta-ios: _committed _ios

# uploads this commit for the stores: a production draft on Google Play, a build in App Store Connect
release: release-android release-ios

# uploads this commit as a production draft on Google Play; roll it out in Play Console
release-android: _committed
    ./gradlew :androidApp:publishReleaseBundle -PversionCode={{build}} --track production --release-status draft

# uploads this commit to App Store Connect; submit it for review there
release-ios: _committed _ios

# archives and uploads to App Store Connect; Apple's rsync goes first, Homebrew's breaks the export
_ios:
    rm -rf build/Kolona.xcarchive build/upload
    PATH=/usr/bin:/bin:/usr/sbin:/sbin:$PATH xcodebuild -project iosApp/iosApp.xcodeproj -scheme Kolona -configuration Release -destination 'generic/platform=iOS' -archivePath build/Kolona.xcarchive -allowProvisioningUpdates archive CURRENT_PROJECT_VERSION={{build}}
    PATH=/usr/bin:/bin:/usr/sbin:/sbin:$PATH xcodebuild -exportArchive -archivePath build/Kolona.xcarchive -exportOptionsPlist iosApp/ExportOptions.plist -exportPath build/upload -allowProvisioningUpdates

# the build number is the commit count, so each upload needs its own commit
_committed:
    @test -z "$(git status --porcelain)" || (echo "Commit first: each upload is one commit." && exit 1)
