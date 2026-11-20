package it.rsm.orario

object Testi {
    val NOTE = """Versione 6.0 Test

Novità
• Prima versione nativa per Android: stesse regole di calcolo dell'app precedente.
• Le sveglie si aggiornano da sole se cambi ingresso, permesso o straordinario dopo averle aggiunte.
• Nuovo pulsante "Azzera gli orari": cancella gli orari della giornata e le sveglie create dall'app, così puoi ripartire.
• Il menu dei tre puntini raccoglie release, note di rilascio e guida.

Correzioni
• Venerdì: la sveglia della pausa pranzo si calcola dall'ingresso effettivo e non più da un orario fisso.
• Venerdì con orario spalmato e ingresso anticipato: si usano sempre le durate del venerdì.
• L'app si aggiorna sopra la versione già installata senza dover essere disinstallata."""

    val GUIDA = """INSERIRE L'INGRESSO
Tocca Presenza o Smart Working e scegli l'orario di ingresso: l'app calcola subito l'uscita. Puoi cambiare l'orario toccando il riquadro Ingresso.

PRESENZA E SMART WORKING
In Smart Working non sono disponibili spalmato, pausa pranzo estesa e straordinario. Il permesso è disponibile in entrambe le modalità.

SPALMATO
Con lo spalmato l'ingresso va dalle 06:50 alle 08:50; senza, dalle 08:00 alle 10:00. Se entri prima, l'orario viene portato all'inizio della fascia. Se entri dopo, viene mostrato il ritardo.

PAUSA PRANZO ESTESA
Attiva l'interruttore, scrivi i minuti totali di pausa (da 16 a 60) e premi Set: l'eccedenza oltre i 15 minuti viene aggiunta all'uscita.

PERMESSO E STRAORDINARIO
Il permesso sottrae il tempo indicato dall'uscita, lo straordinario lo aggiunge. Non si possono usare insieme: impostarne uno azzera l'altro. Il risultato compare in "Uscita Aggiornata". Il venerdì, uno straordinario di almeno 1 ora aggiunge anche 15 minuti.

VENERDÌ
Il venerdì la giornata dura 5 ore e 12 minuti, senza pausa pranzo.

SVEGLIE
L'app propone una sveglia 10 minuti prima dell'uscita. Il venerdì, se inserisci permesso o straordinario, propone anche quella prima della pausa pranzo; senza permesso né straordinario la pausa coincide con l'uscita e basta la sveglia di uscita. Tocca il segno di spunta per aggiungerla all'app Orologio. Se poi cambi gli orari, la sveglia si aggiorna da sola.

AZZERARE
Il pulsante "Azzera gli orari" in fondo cancella ingresso, permesso, straordinario, pausa estesa e le sveglie create dall'app.

SALVATAGGIO
Gli orari inseriti restano salvati per tutta la giornata, anche se chiudi l'app. Il giorno dopo si riparte da zero."""
}
