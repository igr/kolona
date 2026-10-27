package dev.oblac.kolona.i18n

import dev.oblac.kolona.game.City

object GameIdentity {
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
    GameName("Čuvar Kolone", "Guardian of the March", "Gardien du Cortège", "Guardiano del Corteo"),
    Subtitle("ČUVAR", "GUARDIAN OF THE", "GARDIEN DU", "GUARDIANO DEL"),
    Wordmark("KOLONE", "MARCH", "CORTÈGE", "CORTEO"),
    Tagline("OD A DO SLOBODE", "FROM A TO FREEDOM", "DE A À LA LIBERTÉ", "DA A ALLA LIBERTÀ"),
    CityAwake("GRAD JE NA NOGAMA", "THE CITY IS AWAKE", "LA VILLE EST DEBOUT", "LA CITTÀ È SVEGLIA"),
    WelcomeTitle("Kolona ne sme\nda stane.", "The march\nmust go on.", "Le cortège\ndoit avancer.", "Il corteo\nnon si ferma."),
    WelcomeBody("72 ljudi. Jedan put do slobode.\nTi između njih i haosa.", "72 people. One road to freedom.\nYou stand between them and chaos.", "72 personnes. Une route vers la liberté.\nTu es leur rempart contre le chaos.", "72 persone. Una strada verso la libertà.\nTra loro e il caos ci sei tu."),
    Enter("UĐI U GRAD", "ENTER THE CITY", "ENTRER DANS LA VILLE", "ENTRA IN CITTÀ"),
    WelcomeHint("Tvoja pozicija menja sve.", "Your position makes the difference.", "Ta position fait la différence.", "La tua posizione fa la differenza."),
    Menu("GLAVNI MENI", "MAIN MENU", "MENU PRINCIPAL", "MENU PRINCIPALE"),
    MissionNumber("MISIJA 01", "MISSION 01", "MISSION 01", "MISSIONE 01"),
    MissionTitle("Sačuvaj kolonu.", "Protect the march.", "Protège le cortège.", "Proteggi il corteo."),
    MissionBelgrade("Od Slavije do Trga Republike.\nSvaki čovek se računa.", "From Slavija to Republic Square.\nEvery person counts.", "De Slavija à la place de la République.\nChaque personne compte.", "Da Slavija a piazza della Repubblica.\nOgni persona conta."),
    MissionParis("Od Trga Republike do Trga Nacije.\nSvaki čovek se računa.", "From République to Nation.\nEvery person counts.", "De République à Nation.\nChaque personne compte.", "Da République a Nation.\nOgni persona conta."),
    MissionMilan("Od Porta Venezia do Trga Duomo.\nSvaki čovek se računa.", "From Porta Venezia to Piazza del Duomo.\nEvery person counts.", "De Porta Venezia à la place du Dôme.\nChaque personne compte.", "Da Porta Venezia a piazza del Duomo.\nOgni persona conta."),
    People("LJUDI", "PEOPLE", "PERSONNES", "PERSONE"),
    Minutes("MINUTA", "MINUTES", "MINUTES", "MINUTI"),
    ToSave("ZA PROLAZ", "TO SURVIVE", "À SAUVER", "DA SALVARE"),
    Start("BRANI KOLONU", "DEFEND THE MARCH", "DÉFENDRE LE CORTÈGE", "DIFENDI IL CORTEO"),
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
    NoRecord("Tvoja prva šetnja čeka.", "Your first march awaits.", "Ta première marche t’attend.", "Il tuo primo corteo ti aspetta."),
    Back("NAZAD", "BACK", "RETOUR", "INDIETRO"),
    GuideTitle("Zauzmi položaj.\nSačuvaj ljude.", "Take your position.\nProtect your people.", "Prends position.\nProtège les tiens.", "Prendi posizione.\nProteggi i tuoi."),
    MoveTitle("Dodirni. Potrči.", "Tap. Move.", "Touche. Déplace-toi.", "Tocca. Muoviti."),
    MoveBody("Dodirni mesto u gradu. Čuvar bira put oko zgrada. Kamera prati samo njega, a strelice pokazuju gde su kolona i pretnje.", "Tap a place in the city. Your defender finds a way around buildings. The camera follows only you; edge markers point to the march and threats.", "Touche un endroit de la ville. Ton défenseur contourne les bâtiments. La caméra ne suit que toi. Des repères au bord de l’écran indiquent le cortège et les menaces.", "Tocca un punto della città. Il tuo difensore aggira gli edifici. La telecamera segue solo te; gli indicatori ai bordi dello schermo mostrano dove sono il corteo e le minacce."),
    DefendTitle("Stani. Odbrani.", "Stop. Defend.", "Arrête-toi. Défends.", "Fermati. Difendi."),
    DefendBody("Kad staneš, podižeš barikadu i automatski pucaš na pretnje u dometu. Dok trčiš, nema paljbe. Kroz kolonu se krećeš sporije.", "Stop to deploy a barricade and automatically fire at threats in range. You cannot fire while running. Moving through the crowd slows you down.", "Arrête-toi pour dresser une barricade et tirer automatiquement sur les menaces à portée. Impossible de tirer en courant. Traverser la foule te ralentit.", "Fermati per alzare una barricata e sparare in automatico alle minacce a portata di tiro. Mentre corri non puoi sparare. Attraversare la folla ti rallenta."),
    BombTitle("Četiri pretnje. Jedna bomba.", "Four threats. One bomb.", "Quatre menaces. Une bombe.", "Quattro minacce. Una bomba."),
    BombBody("Dodirni grupu od najmanje 4 napadača da baciš bombu. Ponovo je spremna za 5 sekundi. Bomba ne povređuje kolonu.", "Tap a group of at least 4 attackers to throw a bomb. It recharges in 5 seconds. Bombs never hurt the marchers.", "Touche un groupe d’au moins 4 assaillants pour lancer une bombe. Elle se recharge en 5 secondes. Les bombes ne blessent jamais le cortège.", "Tocca un gruppo di almeno 4 aggressori per lanciare una bomba. Si ricarica in 5 secondi. Le bombe non feriscono mai i manifestanti."),
    GoalTitle("Do trga, zajedno.", "To the square, together.", "Jusqu’à la place, ensemble.", "Fino alla piazza, insieme."),
    GoalBody("Dovedi najmanje 36 od 72 ljudi do tačke B. Tokom dva govora kolona staje na 15 sekundi. Odbrani je dok ne nastavi put.", "Escort at least 36 of 72 people to point B. The march stops for two 15-second speeches. Protect it until it moves again.", "Escorte au moins 36 des 72 personnes jusqu’au point B. Le cortège s’arrête 15 secondes à chacun des deux discours. Protège-le jusqu’à ce qu’il reparte.", "Scorta almeno 36 persone su 72 fino al punto B. Il corteo si ferma per due discorsi di 15 secondi. Proteggilo finché non riparte."),
    GotIt("RAZUMEM", "GOT IT", "COMPRIS", "HO CAPITO"),
    Pause("Pauza", "Pause", "Pause", "Pausa"),
    PausedEyebrow("PREDHANI, ČUVARE", "TAKE A BREATH, DEFENDER", "SOUFFLE UN PEU, DÉFENSEUR", "PRENDI FIATO, DIFENSORE"),
    PausedTitle("Grad može\nda sačeka.", "The city\ncan wait.", "La ville\npeut attendre.", "La città\npuò aspettare."),
    PausedBody("Kolona je na sigurnom dok traje pauza.", "The march is safe while the game is paused.", "Le cortège est à l’abri pendant la pause.", "Il corteo è al sicuro durante la pausa."),
    Resume("NASTAVI ODBRANU", "RESUME DEFENDING", "REPRENDRE LA DÉFENSE", "RIPRENDI LA DIFESA"),
    Restart("POČNI PONOVO", "START AGAIN", "RECOMMENCER", "RICOMINCIA"),
    ReturnMenu("GLAVNI MENI", "MAIN MENU", "MENU PRINCIPAL", "MENU PRINCIPALE"),
    InMarch("U KOLONI", "MARCHERS", "CORTÈGE", "IN CORTEO"),
    Defended("ODBRANJENO", "DEFENDED", "REPOUSSÉS", "RESPINTI"),
    Route("DO TRGA", "TO THE SQUARE", "VERS LA PLACE", "VERSO LA PIAZZA"),
    Speech("GOVOR", "SPEECH", "DISCOURS", "DISCORSO"),
    Prepare("PRIPREMI ODBRANU", "PREPARE YOUR DEFENSE", "PRÉPARE TA DÉFENSE", "PREPARA LA DIFESA"),
    Phase0("BATINAŠI U ULICAMA", "THUGS IN THE STREETS", "VOYOUS DANS LES RUES", "PICCHIATORI IN STRADA"),
    Phase1("POLICIJA I BATINAŠI", "POLICE AND THUGS", "POLICE ET VOYOUS", "POLIZIA E PICCHIATORI"),
    Phase2("PAZI, AUTOMOBILI!", "WATCH FOR CARS!", "GARE AUX VOITURES !", "OCCHIO ALLE AUTO!"),
    Phase3("NAPADI U GRUPAMA", "GROUP ATTACKS", "ATTAQUES EN GROUPE", "ATTACCHI IN GRUPPO"),
    Phase4("ODBRANI GOVOR", "DEFEND THE SPEECH", "DÉFENDS LE DISCOURS", "DIFENDI IL DISCORSO"),
    Phase5("POSLEDNJI PRODOR", "THE FINAL PUSH", "LE DERNIER EFFORT", "L’ULTIMO SFORZO"),
    Moving("U POKRETU", "ON THE MOVE", "EN MOUVEMENT", "IN MOVIMENTO"),
    Slowed("KROZ KOLONU · USPORENO", "IN THE CROWD · SLOWED", "DANS LA FOULE · RALENTI", "TRA LA FOLLA · RALLENTATO"),
    Barricade("U BARIKADI · AUTOMATSKA PALJBA", "BARRICADED · AUTO FIRE", "BARRICADÉ · TIR AUTO", "BARRICATO · FUOCO AUTOMATICO"),
    Deploying("PODIZANJE BARIKADE", "DEPLOYING BARRICADE", "POSE DE LA BARRICADE", "BARRICATA IN COSTRUZIONE"),
    BombReady("BOMBA SPREMNA", "BOMB READY", "BOMBE PRÊTE", "BOMBA PRONTA"),
    BombHint("Dodirni grupu od 4+", "Tap a group of 4+", "Touche un groupe de 4+", "Tocca un gruppo di 4+"),
    BombWait("BOMBA SE OBNAVLJA", "BOMB RECHARGING", "BOMBE EN RECHARGE", "BOMBA IN RICARICA"),
    TapHint("Dodirni ulicu da zauzmeš položaj.", "Tap the street to take your position.", "Touche la rue pour prendre position.", "Tocca la strada per prendere posizione."),
    Crowd("KOLONA", "MARCH", "CORTÈGE", "CORTEO"),
    Threat("PRETNJA", "THREAT", "MENACE", "MINACCIA"),
    You("TI", "YOU", "TOI", "TU"),
    Banner("NE ODUSTAJEMO", "WE KEEP GOING", "ON NE LÂCHE RIEN", "NON MOLLIAMO"),
    Thug("BATINAŠI", "THUGS", "VOYOUS", "PICCHIATORI"),
    Police("POLICIJA", "POLICE", "POLICE", "POLIZIA"),
    Cars("AUTOMOBILI", "CARS", "VOITURES", "AUTO"),
    WonEyebrow("STIGLI SMO DO B", "WE MADE IT TO B", "ON EST ARRIVÉS AU POINT B", "SIAMO ARRIVATI AL PUNTO B"),
    LostEyebrow("KOLONA JE ZAUSTAVLJENA", "THE MARCH WAS STOPPED", "LE CORTÈGE A ÉTÉ BLOQUÉ", "IL CORTEO È STATO FERMATO"),
    WonTitle("Ulice su naše.", "The streets are ours.", "La rue est à nous.", "Le strade sono nostre."),
    LostTitle("Ne odustajemo.", "We keep going.", "On ne lâche rien.", "Non molliamo."),
    WonBelgrade("Proveo si kolonu do Trga Republike.\nZajedno smo stigli dalje.", "You led the march to Republic Square.\nTogether, we made it further.", "Tu as mené le cortège place de la République.\nEnsemble, on est allés plus loin.", "Hai guidato il corteo in piazza della Repubblica.\nInsieme siamo andati più lontano."),
    WonParis("Proveo si kolonu do Trga Nacije.\nZajedno smo stigli dalje.", "You led the march to Place de la Nation.\nTogether, we made it further.", "Tu as mené le cortège place de la Nation.\nEnsemble, on est allés plus loin.", "Hai guidato il corteo fino a place de la Nation.\nInsieme siamo andati più lontano."),
    WonMilan("Proveo si kolonu do Trga Duomo.\nZajedno smo stigli dalje.", "You led the march to Piazza del Duomo.\nTogether, we made it further.", "Tu as mené le cortège place du Dôme.\nEnsemble, on est allés plus loin.", "Hai guidato il corteo fino a piazza del Duomo.\nInsieme siamo andati più lontano."),
    SouvenirBelgrade("SUVENIR IZ BEOGRADA", "A SOUVENIR FROM BELGRADE", "SOUVENIR DE BELGRADE", "SOUVENIR DA BELGRADO"),
    SouvenirParis("SUVENIR IZ PARIZA", "A SOUVENIR FROM PARIS", "SOUVENIR DE PARIS", "SOUVENIR DA PARIGI"),
    SouvenirMilan("SUVENIR IZ MILANA", "A SOUVENIR FROM MILAN", "SOUVENIR DE MILAN", "SOUVENIR DA MILANO"),
    LostBody("Potrebno je sačuvati najmanje 36 ljudi.\nZauzmi novi položaj i pokušaj ponovo.", "At least 36 people must survive.\nFind a new position and try again.", "Il faut sauver au moins 36 personnes.\nTrouve une autre position et réessaie.", "Devono sopravvivere almeno 36 persone.\nTrova una nuova posizione e riprova."),
    Saved("SAČUVANIH LJUDI", "PEOPLE SAVED", "PERSONNES SAUVÉES", "PERSONE SALVATE"),
    Time("VREME", "TIME", "TEMPS", "TEMPO"),
    Again("JOŠ JEDNA ŠETNJA", "ONE MORE MARCH", "ENCORE UNE MARCHE", "UN ALTRO CORTEO"),
    TryAgain("POKUŠAJ PONOVO", "TRY AGAIN", "RÉESSAYER", "RIPROVA"),
    Rotate("Igra se uspravno", "Play in portrait", "Joue en mode portrait", "Gioca in verticale"),
}

class Strings(val language: Language) {
    operator fun get(key: Copy) = when (language) {
        Language.English -> key.en
        Language.Serbian -> key.sr
        Language.French -> key.fr
        Language.Italian -> key.it
    }
    fun phase(index: Int) = get(listOf(Copy.Phase0, Copy.Phase1, Copy.Phase2, Copy.Phase3, Copy.Phase4, Copy.Phase5)[index.coerceIn(0, 5)])
    fun cityName(city: City) = get(when (city) { City.Belgrade -> Copy.Belgrade; City.Paris -> Copy.Paris; City.Milan -> Copy.Milan })
    fun country(city: City) = get(when (city) { City.Belgrade -> Copy.Serbia; City.Paris -> Copy.France; City.Milan -> Copy.Italy })
    fun mission(city: City) = get(when (city) { City.Belgrade -> Copy.MissionBelgrade; City.Paris -> Copy.MissionParis; City.Milan -> Copy.MissionMilan })
    fun won(city: City) = get(when (city) { City.Belgrade -> Copy.WonBelgrade; City.Paris -> Copy.WonParis; City.Milan -> Copy.WonMilan })
    fun souvenir(city: City) = get(when (city) { City.Belgrade -> Copy.SouvenirBelgrade; City.Paris -> Copy.SouvenirParis; City.Milan -> Copy.SouvenirMilan })
}
