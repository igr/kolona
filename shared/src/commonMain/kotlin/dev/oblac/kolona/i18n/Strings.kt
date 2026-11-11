package dev.oblac.kolona.i18n

import dev.oblac.kolona.game.City

/** The brand is the same in every language. */
object GameIdentity {
    const val NAME = "Protest Defender"
    const val SUBTITLE = "PROTEST"
    const val WORDMARK = "DEFENDER"
    const val VERSION = "0.1.0"
}

enum class Language(val code: String, val nativeName: String) {
    English("en", "English"), Serbian("sr", "Srpski"), French("fr", "Français"), Italian("it", "Italiano");

    companion object {
        fun fromCode(code: String?) = entries.find { it.code == code } ?: English
    }
}

// Every key requires a translation in every language, including all names drawn inside the game.
enum class Copy(val sr: String, val en: String, val fr: String, val it: String) {
    Tagline("OD A DO SLOBODE", "FROM A TO FREEDOM", "DE A À LA LIBERTÉ", "DA A ALLA LIBERTÀ"),
    CityAwake("GRAD JE NA NOGAMA", "THE CITY IS AWAKE", "LA VILLE EST DEBOUT", "LA CITTÀ È SVEGLIA"),
    WelcomeTitle("Protest ne\nsme da stane.", "The protest\nmust go on.", "La manif\ndoit avancer.", "La protesta\nnon si ferma."),
    WelcomeBody("72 ljudi. Jedan put do slobode.\nTi između njih i haosa.", "72 people. One road to freedom.\nYou stand between them and chaos.", "72 personnes. Une route vers la liberté.\nTu es leur rempart contre le chaos.", "72 persone. Una strada verso la libertà.\nTra loro e il caos ci sei tu."),
    Enter("UĐI U GRAD", "ENTER THE CITY", "ENTRER DANS LA VILLE", "ENTRA IN CITTÀ"),
    WelcomeHint("Tvoja pozicija menja sve.", "Your position makes the difference.", "Ta position fait la différence.", "La tua posizione fa la differenza."),
    Menu("GLAVNI MENI", "MAIN MENU", "MENU PRINCIPAL", "MENU PRINCIPALE"),
    MissionNumber("MISIJA 01", "MISSION 01", "MISSION 01", "MISSIONE 01"),
    MissionTitle("Sačuvaj protest.", "Protect the protesters.", "Protège la manif.", "Proteggi i manifestanti."),
    MissionBelgrade("Od Slavije do Trga Republike.\nSvaki čovek se računa.", "From Slavija to Republic Square.\nEvery person counts.", "De Slavija à la place de la République.\nChaque personne compte.", "Da Slavija a piazza della Repubblica.\nOgni persona conta."),
    MissionParis("Od Trga Republike do Trga Nacije.\nSvaki čovek se računa.", "From République to Nation.\nEvery person counts.", "De République à Nation.\nChaque personne compte.", "Da République a Nation.\nOgni persona conta."),
    MissionMilan("Od Porta Venezia do Trga Duomo.\nSvaki čovek se računa.", "From Porta Venezia to Piazza del Duomo.\nEvery person counts.", "De Porta Venezia à la place du Dôme.\nChaque personne compte.", "Da Porta Venezia a piazza del Duomo.\nOgni persona conta."),
    People("LJUDI", "PEOPLE", "PERSONNES", "PERSONE"),
    Minutes("MINUTA", "MINUTES", "MINUTES", "MINUTI"),
    ToSave("ZA PROLAZ", "TO SURVIVE", "À SAUVER", "DA SALVARE"),
    Start("BRANI PROTEST", "DEFEND THE PROTEST", "DÉFENDRE LA MANIF", "DIFENDI LA PROTESTA"),
    HowTo("Kako se igra", "How to play", "Comment jouer", "Come si gioca"),
    CityEyebrow("IZABERI GRAD", "CHOOSE YOUR CITY", "CHOISIS TA VILLE", "SCEGLI LA TUA CITTÀ"),
    CityHeading("Svaki grad\nima svoj trg.", "Every city\nhas its square.", "Chaque ville\na sa place.", "Ogni città\nha la sua piazza."),
    CityLanguage("Promeni i jezik", "Also switch the language", "Changer aussi la langue", "Cambia anche la lingua"),
    Belgrade("Beograd", "Belgrade", "Belgrade", "Belgrado"),
    Paris("Pariz", "Paris", "Paris", "Parigi"),
    Milan("Milano", "Milan", "Milan", "Milano"),
    Serbia("Srbija", "Serbia", "Serbie", "Serbia"),
    France("Francuska", "France", "France", "Francia"),
    Italy("Italija", "Italy", "Italie", "Italia"),
    ActiveLanguage("JEZIK IGRE", "GAME LANGUAGE", "LANGUE DU JEU", "LINGUA DEL GIOCO"),
    Record("NAJBOLJE SAČUVANO", "BEST RESCUE", "MEILLEUR SAUVETAGE", "IL TUO RECORD"),
    CityBest("REKORD", "BEST", "RECORD", "RECORD"),
    NoRecord("Tvoj prvi protest čeka.", "Your first protest awaits.", "Ta première manif t’attend.", "La tua prima protesta ti aspetta."),
    Back("NAZAD", "BACK", "RETOUR", "INDIETRO"),
    GuideTitle("Zauzmi položaj.\nSačuvaj ljude.", "Take your position.\nProtect your people.", "Prends position.\nProtège les tiens.", "Prendi posizione.\nProteggi i tuoi."),
    MoveTitle("Dodirni. Potrči.", "Tap. Move.", "Touche. Déplace-toi.", "Tocca. Muoviti."),
    MoveBody("Dodirni mesto u gradu. Čuvar bira put oko zgrada. Kamera prati samo njega, a strelice pokazuju gde su protest i pretnje.", "Tap a place in the city. Your defender finds a way around buildings. The camera follows only you; edge markers point to the protest and threats.", "Touche un endroit de la ville. Ton défenseur contourne les bâtiments. La caméra ne suit que toi. Des repères au bord de l’écran indiquent la manif et les menaces.", "Tocca un punto della città. Il tuo difensore aggira gli edifici. La telecamera segue solo te; gli indicatori ai bordi dello schermo mostrano dove sono la protesta e le minacce."),
    DefendTitle("Stani. Odbrani.", "Stop. Defend.", "Arrête-toi. Défends.", "Fermati. Difendi."),
    DefendBody("Kad staneš, podižeš barikadu i automatski pucaš na pretnje u dometu. Dok trčiš, nema paljbe. Kroz masu se krećeš sporije.", "Stop to deploy a barricade and automatically fire at threats in range. You cannot fire while running. Moving through the crowd slows you down.", "Arrête-toi pour dresser une barricade et tirer automatiquement sur les menaces à portée. Impossible de tirer en courant. Traverser la foule te ralentit.", "Fermati per alzare una barricata e sparare in automatico alle minacce a portata di tiro. Mentre corri non puoi sparare. Attraversare la folla ti rallenta."),
    BombTitle("Jedan potez. Jedna bomba.", "One swipe. One bomb.", "Un geste. Une bombe.", "Un gesto. Una bomba."),
    BombBody("Prevuci prstom od čuvara do mesta gde treba da padne bomba. Ponovo je spremna za 5 sekundi. Bomba ne povređuje demonstrante.", "Swipe from your defender to where the bomb should land. It recharges in 5 seconds. Bombs never hurt the protesters.", "Glisse depuis ton défenseur jusqu’à l’endroit où la bombe doit tomber. Elle se recharge en 5 secondes. Les bombes ne blessent jamais les manifestants.", "Trascina dal tuo difensore fino al punto in cui deve cadere la bomba. Si ricarica in 5 secondi. Le bombe non feriscono mai i manifestanti."),
    GoalTitle("Do trga, zajedno.", "To the square, together.", "Jusqu’à la place, ensemble.", "Fino alla piazza, insieme."),
    GoalBody("Dovedi najmanje 36 od 72 ljudi do tačke B. Tokom dva govora protest staje na 15 sekundi. Odbrani ga dok ne nastavi put.", "Escort at least 36 of 72 people to point B. The protest stops for two 15-second speeches. Protect it until it moves again.", "Escorte au moins 36 des 72 personnes jusqu’au point B. La manif s’arrête 15 secondes à chacun des deux discours. Protège-la jusqu’à ce qu’elle reparte.", "Scorta almeno 36 persone su 72 fino al punto B. La protesta si ferma per due discorsi di 15 secondi. Proteggila finché non riparte."),
    GotIt("RAZUMEM", "GOT IT", "COMPRIS", "HO CAPITO"),
    Pause("Pauza", "Pause", "Pause", "Pausa"),
    PausedEyebrow("PREDAHNI, ČUVARE", "TAKE A BREATH, DEFENDER", "SOUFFLE UN PEU, DÉFENSEUR", "PRENDI FIATO, DIFENSORE"),
    PausedTitle("Grad može\nda sačeka.", "The city\ncan wait.", "La ville\npeut attendre.", "La città\npuò aspettare."),
    PausedBody("Protest je na sigurnom dok traje pauza.", "The protest is safe while the game is paused.", "La manif est à l’abri pendant la pause.", "La protesta è al sicuro durante la pausa."),
    Resume("NASTAVI ODBRANU", "RESUME DEFENDING", "REPRENDRE LA DÉFENSE", "RIPRENDI LA DIFESA"),
    Restart("POČNI PONOVO", "START AGAIN", "RECOMMENCER", "RICOMINCIA"),
    ReturnMenu("GLAVNI MENI", "MAIN MENU", "MENU PRINCIPAL", "MENU PRINCIPALE"),
    InMarch("NA PROTESTU", "PROTESTERS", "MANIFESTANTS", "MANIFESTANTI"),
    Route("DO TRGA", "TO THE SQUARE", "VERS LA PLACE", "VERSO LA PIAZZA"),
    Speech("GOVOR", "SPEECH", "DISCOURS", "DISCORSO"),
    Prepare("PRIPREMI ODBRANU", "PREPARE YOUR DEFENSE", "PRÉPARE TA DÉFENSE", "PREPARA LA DIFESA"),
    Phase0("BATINAŠI U ULICAMA", "THUGS IN THE STREETS", "VOYOUS DANS LES RUES", "PICCHIATORI IN STRADA"),
    Phase1("POLICIJA I BATINAŠI", "POLICE AND THUGS", "POLICE ET VOYOUS", "POLIZIA E PICCHIATORI"),
    Phase2("PAZI, AUTOMOBILI!", "WATCH FOR CARS!", "GARE AUX VOITURES !", "OCCHIO ALLE AUTO!"),
    Phase3("NAPADI U GRUPAMA", "GROUP ATTACKS", "ATTAQUES EN GROUPE", "ATTACCHI IN GRUPPO"),
    Phase4("ODBRANI GOVOR", "DEFEND THE SPEECH", "DÉFENDS LE DISCOURS", "DIFENDI IL DISCORSO"),
    Phase5("POSLEDNJI PRODOR", "THE FINAL PUSH", "LE DERNIER EFFORT", "L’ULTIMO SFORZO"),
    BombReady("BOMBA SPREMNA", "BOMB READY", "BOMBE PRÊTE", "BOMBA PRONTA"),
    BombHint("Prevuci od čuvara", "Swipe from your defender", "Glisse depuis ton défenseur", "Trascina dal tuo difensore"),
    BombWait("BOMBA SE OBNAVLJA", "BOMB RECHARGING", "BOMBE EN RECHARGE", "BOMBA IN RICARICA"),
    TapHint("Dodirni ulicu da zauzmeš položaj.", "Tap the street to take your position.", "Touche la rue pour prendre position.", "Tocca la strada per prendere posizione."),
    Crowd("PROTEST", "PROTEST", "MANIF", "PROTESTA"),
    Threat("PRETNJA", "THREAT", "MENACE", "MINACCIA"),
    You("TI", "YOU", "TOI", "TU"),
    ShoutKeepGoing("NE ODUSTAJEMO", "WE KEEP GOING", "ON NE LÂCHE RIEN", "NON MOLLIAMO"),
    ShoutSystem("BORBA PROTIV SISTEMA", "FIGHT THE SYSTEM", "COMBATTONS LE SYSTÈME", "LOTTA AL SISTEMA"),
    ShoutCorruption("DOLE KORUPCIJA", "DOWN WITH CORRUPTION", "À BAS LA CORRUPTION", "ABBASSO LA CORRUZIONE"),
    ShoutTerror("KRAJ TERORA", "END THE TERROR", "FIN DE LA TERREUR", "FINE DEL TERRORE"),
    ShoutPeace("HOĆEMO MIR", "BRING PEACE", "ON VEUT LA PAIX", "VOGLIAMO LA PACE"),
    ShoutJustice("PRAVDA ZA SVE", "JUSTICE FOR ALL", "JUSTICE POUR TOUS", "GIUSTIZIA PER TUTTI"),
    ShoutLies("DOSTA LAŽI", "NO MORE LIES", "ASSEZ DE MENSONGES", "BASTA BUGIE"),
    ShoutFear("NE BOJIMO SE", "WE ARE NOT AFRAID", "ON N’A PAS PEUR", "NON ABBIAMO PAURA"),
    ShoutFreedom("SLOBODA", "FREEDOM", "LIBERTÉ", "LIBERTÀ"),
    ShoutShame("SRAMOTA", "SHAME ON YOU", "HONTE À VOUS", "VERGOGNA"),
    Thug("BATINAŠI", "THUGS", "VOYOUS", "PICCHIATORI"),
    Police("POLICIJA", "POLICE", "POLICE", "POLIZIA"),
    Cars("AUTOMOBILI", "CARS", "VOITURES", "AUTO"),
    WonEyebrow("STIGLI SMO DO B", "WE MADE IT TO B", "ON EST ARRIVÉS AU POINT B", "SIAMO ARRIVATI AL PUNTO B"),
    LostEyebrow("PROTEST JE ZAUSTAVLJEN", "THE PROTEST WAS STOPPED", "LA MANIF A ÉTÉ BLOQUÉE", "LA PROTESTA È STATA FERMATA"),
    WonTitle("Ulice su naše.", "The streets are ours.", "La rue est à nous.", "Le strade sono nostre."),
    LostTitle("Ne odustajemo.", "We keep going.", "On ne lâche rien.", "Non molliamo."),
    WonBelgrade("Proveo si protest do Trga Republike.\nZajedno smo stigli dalje.", "You led the protest to Republic Square.\nTogether, we made it further.", "Tu as mené la manif place de la République.\nEnsemble, on est allés plus loin.", "Hai guidato la protesta in piazza della Repubblica.\nInsieme siamo andati più lontano."),
    WonParis("Proveo si protest do Trga Nacije.\nZajedno smo stigli dalje.", "You led the protest to Place de la Nation.\nTogether, we made it further.", "Tu as mené la manif place de la Nation.\nEnsemble, on est allés plus loin.", "Hai guidato la protesta fino a place de la Nation.\nInsieme siamo andati più lontano."),
    WonMilan("Proveo si protest do Trga Duomo.\nZajedno smo stigli dalje.", "You led the protest to Piazza del Duomo.\nTogether, we made it further.", "Tu as mené la manif place du Dôme.\nEnsemble, on est allés plus loin.", "Hai guidato la protesta fino a piazza del Duomo.\nInsieme siamo andati più lontano."),
    SouvenirBelgrade("SUVENIR IZ BEOGRADA", "A SOUVENIR FROM BELGRADE", "SOUVENIR DE BELGRADE", "SOUVENIR DA BELGRADO"),
    SouvenirParis("SUVENIR IZ PARIZA", "A SOUVENIR FROM PARIS", "SOUVENIR DE PARIS", "SOUVENIR DA PARIGI"),
    SouvenirMilan("SUVENIR IZ MILANA", "A SOUVENIR FROM MILAN", "SOUVENIR DE MILAN", "SOUVENIR DA MILANO"),
    LostBody("Potrebno je sačuvati najmanje 36 ljudi.\nZauzmi novi položaj i pokušaj ponovo.", "At least 36 people must survive.\nFind a new position and try again.", "Il faut sauver au moins 36 personnes.\nTrouve une autre position et réessaie.", "Devono sopravvivere almeno 36 persone.\nTrova una nuova posizione e riprova."),
    Saved("SAČUVANIH LJUDI", "PEOPLE SAVED", "PERSONNES SAUVÉES", "PERSONE SALVATE"),
    Time("VREME", "TIME", "TEMPS", "TEMPO"),
    Again("JOŠ JEDAN PROTEST", "ONE MORE PROTEST", "ENCORE UNE MANIF", "UN’ALTRA PROTESTA"),
    TryAgain("POKUŠAJ PONOVO", "TRY AGAIN", "RÉESSAYER", "RIPROVA"),
    Rotate("Igra se uspravno", "Play in portrait", "Joue en mode portrait", "Gioca in verticale"),
}

/** The protest shouts these in turn. */
val slogans = listOf(Copy.ShoutKeepGoing, Copy.ShoutSystem, Copy.ShoutCorruption, Copy.ShoutTerror, Copy.ShoutPeace,
    Copy.ShoutJustice, Copy.ShoutLies, Copy.ShoutFear, Copy.ShoutFreedom, Copy.ShoutShame)

class Strings(val language: Language) {
    operator fun get(key: Copy) = when (language) {
        Language.English -> key.en
        Language.Serbian -> key.sr
        Language.French -> key.fr
        Language.Italian -> key.it
    }
    // Made once, so a shout drawn every frame makes no new strings.
    private val shouts by lazy { slogans.associateWith { copy -> (1..3).map { get(copy) + (if (language == Language.French) " " else "") + "!".repeat(it) } } }
    /** A slogan as one protester shouts it, with one to three exclamation marks. */
    fun shout(slogan: Copy, marks: Int) = shouts.getValue(slogan)[marks - 1]
    fun phase(index: Int) = get(listOf(Copy.Phase0, Copy.Phase1, Copy.Phase2, Copy.Phase3, Copy.Phase4, Copy.Phase5)[index.coerceIn(0, 5)])
    fun cityName(city: City) = get(when (city) { City.Belgrade -> Copy.Belgrade; City.Paris -> Copy.Paris; City.Milan -> Copy.Milan })
    fun country(city: City) = get(when (city) { City.Belgrade -> Copy.Serbia; City.Paris -> Copy.France; City.Milan -> Copy.Italy })
    fun mission(city: City) = get(when (city) { City.Belgrade -> Copy.MissionBelgrade; City.Paris -> Copy.MissionParis; City.Milan -> Copy.MissionMilan })
    fun won(city: City) = get(when (city) { City.Belgrade -> Copy.WonBelgrade; City.Paris -> Copy.WonParis; City.Milan -> Copy.WonMilan })
    fun souvenir(city: City) = get(when (city) { City.Belgrade -> Copy.SouvenirBelgrade; City.Paris -> Copy.SouvenirParis; City.Milan -> Copy.SouvenirMilan })
}
