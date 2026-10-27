package dev.oblac.kolona.game

enum class City(val code: String) {
    Belgrade("belgrade"), Paris("paris"), Milan("milan");
    companion object { fun fromCode(code: String?): City = entries.firstOrNull { it.code == code } ?: Belgrade }
}

enum class StreetStyle { Grid, Radial }
data class Stop(val name: String, val u: Float, val v: Float, val kind: SquareKind, val radius: Float)
data class Leg(val names: List<String>, val via: List<Pair<Float, Float>>)
data class NamedStreet(val name: String, val points: List<Pair<Float, Float>>, val width: Float)
enum class LandmarkKind { Church, Tower, Hotel, Museum, Circus, Opera, Cathedral, Arcade, Tollhouse }
/**
 * Near the route point [along] units past stop [stop] (negative: before it), preferably on the march's right (+1) or left (-1) [side].
 * It takes the closest free spot, which can be across the street. With no room, it shrinks, down to 20% of its size.
 */
data class LandmarkSpec(val name: String, val kind: LandmarkKind, val stop: Int, val along: Float, val side: Int, val width: Float, val height: Float)
/** Street life drawn by a block's edge: [width] along the street, [depth] toward it, and [height] for what stands up (people, columns). */
enum class PropKind(val width: Float, val depth: Float, val height: Float = 0f) {
    Boulangerie(50f, 30f), Crepes(38f, 30f, 7f), Cafe(58f, 35f, 8f), MorrisColumn(18f, 18f, 35f), WallaceFountain(15f, 15f, 28f),
    Metro(30f, 40f, 23f), Newsstand(32f, 28f, 10f), Accordion(23f, 18f, 38f), Bikes(50f, 20f, 5f),
    Grill(33f, 30f, 23f), Pekara(48f, 33f), Kafana(58f, 35f, 8f), Trafika(27f, 25f, 12f), Chess(53f, 23f, 13f), BrassBand(50f, 23f, 38f),
    Gelato(30f, 19f, 36f), Pasticceria(50f, 28f, 11f), Espresso(50f, 33f, 8f), Vedovella(15f, 15f, 27f), Scooters(50f, 15f, 13f), Boutique(50f, 20f, 21f),
    Pigeons(45f, 35f),
}
/** (u, v) are fractions of the map inside a 110-unit margin; points outside 0..1 lie past the map edge. */
data class CityTemplate(val stops: List<Stop>, val legs: List<Leg>, val streets: List<NamedStreet>, val style: StreetStyle,
                        val landmarks: List<LandmarkSpec> = listOf(), val props: List<PropKind> = listOf())

val City.template: CityTemplate get() = when (this) {
    City.Belgrade -> belgrade
    City.Paris -> paris
    City.Milan -> milan
}

// Bd Voltaire runs almost straight from République to Nation.
internal val NamedStreet.wobble get() = if (name == "Bd Voltaire") 0f..10f else 15f..35f
// Bastille's spokes fill the south-west. Without streets there, enemies that spawn past the map edge land next to the crowd.
internal val Stop.spokes get() = when (name) {
    "République" -> 6; "Bastille" -> 6; "Nation" -> 8
    "Porta Venezia" -> 4; "San Babila" -> 6; "Piazza del Duomo" -> 8
    else -> 0
}

private val belgrade = CityTemplate(
    stops = listOf(
        Stop("Slavija", .03f, .94f, SquareKind.Roundabout, 120f),
        Stop("Terazije", .51f, .49f, SquareKind.Open, 90f),
        Stop("Trg Republike", .96f, .08f, SquareKind.Open, 130f),
    ),
    legs = listOf(
        Leg(listOf("Kralja Milana"), listOf(.33f to .84f)),
        Leg(listOf("Kolarčeva"), listOf(.69f to .16f)),
    ),
    streets = listOf(
        NamedStreet("Bulevar oslobođenja", listOf(.03f to .94f, .22f to 1.05f), 95f),
        NamedStreet("Nemanjina", listOf(.03f to .94f, -.05f to .62f), 95f),
        NamedStreet("Kneza Miloša", listOf(-.05f to .13f, .44f to 1.05f), 95f),
        NamedStreet("Dečanska", listOf(.51f to .49f, 1.05f to .46f), 95f),
        NamedStreet("Knez Mihailova", listOf(.96f to .08f, .70f to -.05f), 95f),
    ),
    style = StreetStyle.Grid,
    landmarks = listOf(
        LandmarkSpec("Hram Svetog Save", LandmarkKind.Church, 0, 120f, -1, 150f, 150f),
        LandmarkSpec("Beograđanka", LandmarkKind.Tower, 0, 330f, -1, 70f, 90f),
        LandmarkSpec("Hotel Moskva", LandmarkKind.Hotel, 1, -40f, -1, 90f, 70f),
        LandmarkSpec("Narodni muzej", LandmarkKind.Museum, 2, -60f, 1, 120f, 80f),
    ),
    props = listOf(PropKind.Grill, PropKind.Pekara, PropKind.Kafana, PropKind.Trafika, PropKind.Chess, PropKind.BrassBand, PropKind.Pigeons),
)

private val paris = CityTemplate(
    stops = listOf(
        Stop("République", .08f, .10f, SquareKind.Star, 115f),
        Stop("Bastille", .30f, .88f, SquareKind.Round, 105f),
        Stop("Nation", .92f, .66f, SquareKind.Star, 120f),
    ),
    legs = listOf(
        Leg(listOf("Bd du Temple", "Bd Beaumarchais"), listOf(.10f to .50f)),
        Leg(listOf("Rue du Faubourg Saint-Antoine"), listOf(.62f to .76f)),
    ),
    streets = listOf(
        NamedStreet("Bd Voltaire", listOf(.08f to .10f, .92f to .66f), 110f),
        NamedStreet("Bd Richard-Lenoir", listOf(.30f to -.05f, .30f to .88f), 110f),
        NamedStreet("Rue de la Roquette", listOf(.30f to .88f, .72f to .28f), 95f),
        NamedStreet("Av. de la République", listOf(.08f to .10f, .80f to -.05f), 95f),
        NamedStreet("Bd Diderot", listOf(.92f to .66f, .66f to 1.05f), 110f),
    ),
    style = StreetStyle.Radial,
    landmarks = listOf(
        LandmarkSpec("Cirque d'Hiver", LandmarkKind.Circus, 0, 260f, -1, 80f, 90f),
        LandmarkSpec("Opéra Bastille", LandmarkKind.Opera, 1, 160f, 1, 160f, 104f),
    ),
    props = listOf(PropKind.Boulangerie, PropKind.Crepes, PropKind.Cafe, PropKind.MorrisColumn, PropKind.WallaceFountain, PropKind.Metro,
        PropKind.Newsstand, PropKind.Accordion, PropKind.Bikes, PropKind.Pigeons),
)

private val milan = CityTemplate(
    stops = listOf(
        Stop("Porta Venezia", .92f, .05f, SquareKind.Open, 105f),
        Stop("San Babila", .62f, .78f, SquareKind.Round, 95f),
        Stop("Piazza del Duomo", .04f, .90f, SquareKind.Star, 125f),
    ),
    legs = listOf(
        Leg(listOf("Corso Venezia"), listOf(.78f to .42f)),
        Leg(listOf("Corso Vittorio Emanuele II"), listOf(.34f to .86f)),
    ),
    streets = listOf(
        NamedStreet("Corso Buenos Aires", listOf(.92f to .05f, 1.05f to -.08f), 95f),
        NamedStreet("Via Monte Napoleone", listOf(.62f to .78f, .33f to .40f), 90f),
        NamedStreet("Via Manzoni", listOf(.25f to .55f, .55f to -.05f), 95f),
        NamedStreet("Via Torino", listOf(.04f to .90f, -.05f to 1.05f), 95f),
        NamedStreet("Corso Europa", listOf(.62f to .78f, .57f to 1.05f), 95f),
    ),
    style = StreetStyle.Radial,
    landmarks = listOf(
        LandmarkSpec("Duomo di Milano", LandmarkKind.Cathedral, 2, -170f, -1, 100f, 150f),
        LandmarkSpec("Galleria Vittorio Emanuele II", LandmarkKind.Arcade, 2, -250f, 1, 80f, 96f),
        LandmarkSpec("Caselli di Porta Venezia", LandmarkKind.Tollhouse, 0, 140f, 1, 70f, 47f),
    ),
    props = listOf(PropKind.Gelato, PropKind.Pasticceria, PropKind.Espresso, PropKind.Vedovella, PropKind.Scooters, PropKind.Boutique,
        PropKind.Pigeons),
)
