scheme := "Kolona"

_default:
    @just --list

# runs the game in a desktop window
run:
    ./gradlew :desktopApp:run

# the release and beta recipes of the games: see ~/prj/igr/gamekit/README.md
import '~/prj/igr/gamekit/release.just'
