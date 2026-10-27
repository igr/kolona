package dev.oblac.kolona.game

import kotlin.math.*
import kotlin.random.Random

enum class GameStatus { Playing, Paused, Won, Lost }
enum class EnemyKind(val health: Int, val speed: Float, val radius: Float) {
    Thug(3, 39f, 11f), Police(4, 33f, 12f), Car(10, 52f, 20f)
}
enum class CommandResult { Move, Bomb, CoolingDown, Ignored }

class Hero(var position: Vec2) {
    var path = mutableListOf<Vec2>()
    var barricade = 1f
    var fireCooldown = 0f
    var angle = 0f
    var slowedByCrowd = false
    val moving get() = path.isNotEmpty()
}
class Marcher(val id: Int, val row: Int, val offset: Float, var position: Vec2) {
    var health = 3
    var arrived = false
    var hit = 0f
    val alive get() = health > 0
}
class Enemy(val id: Int, val kind: EnemyKind, var position: Vec2) {
    var health = kind.health
    var warning = 1.2f
    var age = 0f
    var hit = 0f
    var attackCooldown = 0f
    var repath = 0f
    var path = mutableListOf<Vec2>()
}
class Shot(var position: Vec2, val target: Enemy) { var life = 0.8f }
class Bomb(val origin: Vec2, val target: Vec2) { var age = 0f }
class Effect(val position: Vec2, val blast: Boolean = false) { var life = if (blast) .6f else .3f }

data class GameConfig(
    val marchSeconds: Float = 150f,
    val speechSeconds: Float = 15f,
    val speechesAt: List<Float> = listOf(43f, 97f),
    val spawnEnemies: Boolean = true,
    val maxMapScale: Float = 2f,
    // The march takes the same time on every map, so on a small map the crowd is slow and easy to catch.
    // A 1x map uses spawnSeconds and a maxMapScale map uses spawnSecondsAtMaxScale. Sizes between interpolate linearly.
    val spawnSeconds: Float = 3.2f,
    val spawnSecondsAtMaxScale: Float = 2.5f,
    val spawnJitterSeconds: Float = 1.1f,
    val spawnPhaseSpeedup: Float = .12f,
    val spawnDistance: Float = 335f,
    val spawnDistanceJitter: Float = 90f,
    val enemySpeedScale: Float = 1f,
)

/** Pure Kotlin simulation: no platform, UI, wall clock, or payment dependencies. */
class GameEngine(seed: Int = Random.nextInt(), val config: GameConfig = GameConfig(),
                 val map: CityMap = generateMap(City.Belgrade, seed, 1f)) {
    private val random = Random(seed)
    val hero = Hero(map.heroStart)
    val people = buildList {
        var id = 0
        repeat(16) { row ->
            val count = if (row % 2 == 0) 4 else 5
            repeat(count) { col ->
                val offset = (col - (count - 1) / 2f) * 13f
                add(Marcher(id++, row, offset, map.route.at(280f - row * 15f, offset)))
            }
        }
    }
    val enemies = mutableListOf<Enemy>()
    val shots = mutableListOf<Shot>()
    val bombs = mutableListOf<Bomb>()
    val effects = mutableListOf<Effect>()
    var status = GameStatus.Playing
        private set
    var elapsed = 0f
        private set
    var marchTime = 0f
        private set
    var speechRemaining = 0f
        private set
    var bombCooldown = 0f
        private set
    var defended = 0
        private set
    private var speechIndex = 0
    private val spawnSeconds = config.spawnSeconds + (config.spawnSecondsAtMaxScale - config.spawnSeconds) *
        if (config.maxMapScale > 1f) ((map.scale - 1f) / (config.maxMapScale - 1f)).coerceIn(0f, 1f) else 0f
    private var spawnTimer = 3f
    private var nextEnemyId = 0
    private var accumulator = 0.0
    val survivors get() = people.count { it.alive }
    val progress get() = (marchTime / config.marchSeconds).coerceIn(0f, 1f)
    val phase get() = (elapsed / 30).toInt().coerceIn(0, 5)
    val crowdCenter: Vec2 get() {
        val active = people.filter { it.alive && !it.arrived }
        return if (active.isEmpty()) map.route.at(map.route.length)
        else active.fold(Vec2.Zero) { p, marcher -> p + marcher.position } * (1f / active.size)
    }

    fun pause() { if (status == GameStatus.Playing) { status = GameStatus.Paused; accumulator = 0.0 } }
    fun resume() { if (status == GameStatus.Paused) { status = GameStatus.Playing; accumulator = 0.0 } }

    /** Fixed 60 Hz simulation; cap stalls so foregrounding never fast-forwards a run. */
    fun advance(seconds: Float) {
        if (status != GameStatus.Playing || !seconds.isFinite() || seconds <= 0f) return
        accumulator += seconds.coerceAtMost(0.1f).toDouble()
        val step = 1.0 / 60.0
        while (accumulator + 1e-8 >= step && status == GameStatus.Playing) {
            update(step.toFloat())
            accumulator -= step
        }
    }

    fun command(point: Vec2): CommandResult {
        if (status != GameStatus.Playing || !point.x.isFinite() || !point.y.isFinite()) return CommandResult.Ignored
        val target = enemies.filter { it.health > 0 && it.warning <= 0f && it.position.distanceTo(point) < 43f }
            .sortedBy { it.position.distanceTo(point) }
            .firstOrNull { enemy -> enemies.count { it.health > 0 && it.warning <= 0f && it.position.distanceTo(enemy.position) <= BOMB_RADIUS } >= 4 }
        if (target != null) {
            if (bombCooldown > 0) return CommandResult.CoolingDown
            bombs += Bomb(hero.position, target.position)
            bombCooldown = 5f
            return CommandResult.Bomb
        }
        hero.path = map.path(hero.position, point)
        if (hero.moving) hero.barricade = 0f
        return if (hero.moving) CommandResult.Move else CommandResult.Ignored
    }

    private fun update(dt: Float) {
        elapsed += dt
        bombCooldown = max(0f, bombCooldown - dt)
        if (speechRemaining > 0f) speechRemaining = max(0f, speechRemaining - dt)
        else {
            marchTime = min(config.marchSeconds, marchTime + dt)
            if (speechIndex < config.speechesAt.size && marchTime >= config.speechesAt[speechIndex]) {
                speechRemaining = config.speechSeconds
                speechIndex++
            }
        }
        val front = 280f + marchTime / config.marchSeconds * (map.route.length - 280f + 225f)
        people.forEach { p ->
            p.hit = max(0f, p.hit - dt)
            if (p.alive && !p.arrived) {
                val s = front - p.row * 15f
                if (s >= map.route.length - 0.01f) p.arrived = true
                p.position = map.route.at(s, p.offset)
            }
        }
        if (survivors < MIN_SURVIVORS) { status = GameStatus.Lost; return }
        val active = people.filter { it.alive && !it.arrived }
        if (active.isEmpty()) { status = GameStatus.Won; return }

        hero.slowedByCrowd = false
        repeat(4) {
            val crowded = active.any { it.position.distanceTo(hero.position) < 20f }
            hero.slowedByCrowd = hero.slowedByCrowd || crowded
            hero.position = move(hero.position, hero.path, (if (crowded) 105f else 280f) * dt / 4)
        }
        if (!hero.moving) hero.barricade = min(1f, hero.barricade + dt * 5)
        hero.fireCooldown -= dt
        if (!hero.moving && hero.barricade >= .7f && hero.fireCooldown <= 0f) {
            enemies.filter { it.warning <= 0f && it.health > 0 && it.position.distanceTo(hero.position) < FIRE_RANGE }
                .minByOrNull { it.position.distanceTo(hero.position) }?.let { enemy ->
                    val direction = enemy.position - hero.position
                    hero.angle = atan2(direction.y, direction.x)
                    shots += Shot(hero.position, enemy)
                    hero.fireCooldown = .115f
                }
        }
        if (config.spawnEnemies) {
            spawnTimer -= dt
            if (spawnTimer <= 0 && progress < .99f) {
                spawnWave(active)
                spawnTimer = spawnSeconds + random.nextFloat() * config.spawnJitterSeconds - phase * config.spawnPhaseSpeedup
            }
        }
        enemies.forEach { enemy ->
            enemy.age += dt
            enemy.hit = max(0f, enemy.hit - dt)
            enemy.warning -= dt
            if (enemy.warning > 0f || enemy.health <= 0) return@forEach
            enemy.repath -= dt
            enemy.attackCooldown -= dt
            val target = active.minBy { it.position.distanceTo(enemy.position) }
            if (enemy.repath <= 0f) {
                enemy.path = map.path(enemy.position, target.position)
                enemy.repath = .8f + random.nextFloat() * .6f
            }
            enemy.position = move(enemy.position, enemy.path, enemy.kind.speed * config.enemySpeedScale * dt)
            if (!hero.moving && hero.barricade >= .7f && enemy.position.distanceTo(hero.position) < 45f) {
                var direction = (enemy.position - hero.position).normalized()
                if (direction == Vec2.Zero) direction = Vec2(1f, 0f)
                enemy.position = hero.position + direction * 45f
                if (enemy.attackCooldown <= 0f) { enemy.health--; enemy.attackCooldown = .55f }
            }
            if (enemy.health > 0 && enemy.position.distanceTo(target.position) < enemy.kind.radius + 12f && enemy.attackCooldown <= 0f) {
                enemy.attackCooldown = 1.1f
                val victims = if (enemy.kind == EnemyKind.Car) active.filter { it.position.distanceTo(enemy.position) < 42f }.take(3) else listOf(target)
                victims.forEach { if (it.alive) { it.health--; it.hit = .25f } }
            }
        }
        shots.forEach { shot ->
            shot.life -= dt
            val direction = shot.target.position - shot.position
            if (shot.target.health <= 0) shot.life = 0f
            else if (direction.length() < 700 * dt + 8) {
                shot.target.health--
                shot.target.hit = .12f
                effects += Effect(shot.target.position)
                shot.life = 0f
            } else shot.position += direction.normalized() * (700 * dt)
        }
        bombs.forEach { bomb ->
            bomb.age += dt
            if (bomb.age >= .6f) {
                enemies.filter { it.position.distanceTo(bomb.target) <= BOMB_RADIUS }.forEach { it.health -= 14 }
                effects += Effect(bomb.target, blast = true)
            }
        }
        bombs.removeAll { it.age >= .6f }
        shots.removeAll { it.life <= 0 }
        defended += enemies.count { it.health <= 0 }
        enemies.removeAll { it.health <= 0 }
        effects.forEach { it.life -= dt }
        effects.removeAll { it.life <= 0 }
        if (survivors < MIN_SURVIVORS) status = GameStatus.Lost
    }

    private fun spawnWave(active: List<Marcher>) {
        if (enemies.size > 70) return
        val kind = when {
            elapsed < 22 -> EnemyKind.Thug
            elapsed > 45 && random.nextFloat() < .2f -> EnemyKind.Car
            random.nextBoolean() -> EnemyKind.Police
            else -> EnemyKind.Thug
        }
        val target = active.random(random).position
        val angle = random.nextFloat() * (PI * 2).toFloat()
        val radius = config.spawnDistance + random.nextFloat() * config.spawnDistanceJitter
        var origin = Vec2((target.x + cos(angle) * radius).coerceIn(25f, map.width - 25),
            (target.y + sin(angle) * radius).coerceIn(25f, map.height - 25))
        origin = map.nearestWalkable(origin)
        if (origin.distanceTo(target) < 205f) {
            origin = map.nearestWalkable(Vec2(if (target.x < map.width / 2) map.width - 30 else 30f, target.y))
        }
        val count = if (kind == EnemyKind.Car) random.nextInt(1, 3) else if (random.nextFloat() < .4f) 1 else random.nextInt(4, 7)
        repeat(count) {
            val p = map.nearestWalkable(origin + Vec2(random.nextFloat() * 55 - 27, random.nextFloat() * 55 - 27))
            enemies += Enemy(nextEnemyId++, kind, p)
        }
    }

    companion object {
        const val MIN_SURVIVORS = 36
        const val FIRE_RANGE = 285f
        const val BOMB_RADIUS = 110f

        private fun move(start: Vec2, path: MutableList<Vec2>, distance: Float): Vec2 {
            var point = start
            var budget = distance
            while (path.isNotEmpty() && budget > 0f) {
                val direction = path.first() - point
                if (direction.length() <= budget + .001f) {
                    point = path.removeAt(0)
                    budget -= direction.length()
                } else {
                    point += direction.normalized() * budget
                    budget = 0f
                }
            }
            return point
        }
    }
}
