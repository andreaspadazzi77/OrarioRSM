package it.rsm.orario.core

/** Costanti di calcolo (minuti). Equivalgono alle variabili globali dell'app App Inventor. */
object Regole {
    const val DURATA_NORMALE = 8 * 60 + 27      // DurataOre=8, DurataMinuti=27
    const val PAUSA_BASE = 15                   // PausaPranzo
    const val DURATA_VENERDI = 5 * 60 + 12      // DurataOreVenerdi=5, DurataMinutiVenerdi=12
    const val PAUSA_MIN = 15
    const val PAUSA_MAX = 60
    const val PAUSA_VENERDI_CON_STRAORD = 15    // aggiunta se venerdì e straordinario >= 1h
    const val MINUTI_SVEGLIA_ANTICIPO = 10

    const val SPALMATO_INIZIO = 6 * 60 + 50     // 06:50
    const val SPALMATO_FINE = 8 * 60 + 50       // 08:50
    const val NORMALE_INIZIO = 8 * 60           // 08:00
    const val NORMALE_FINE = 10 * 60            // 10:00
}

enum class Modalita { PRESENZA, SMART_WORKING }

/** Dati inseriti dall'utente per la giornata. Tutti i valori orari sono in minuti dalla mezzanotte. */
data class StatoGiorno(
    val ingressoReale: Int? = null,
    val modalita: Modalita = Modalita.PRESENZA,
    val spalmato: Boolean = false,
    val pausaEstesaAttiva: Boolean = false,
    /** Minuti totali di pausa (16..60) quando la pausa estesa è attiva. */
    val minutiPausaTotali: Int = Regole.PAUSA_MIN,
    val permessoMin: Int = 0,
    val straordinarioMin: Int = 0,
    val venerdi: Boolean = false,
) {
    /** Minuti oltre i 15 standard (MinutiPausaPranzo). */
    val eccedenzaPausa: Int
        get() = if (pausaEstesaAttiva && modalita == Modalita.PRESENZA)
            (minutiPausaTotali - Regole.PAUSA_MIN).coerceIn(0, Regole.PAUSA_MAX - Regole.PAUSA_MIN) else 0
    val spalmatoEffettivo: Boolean get() = modalita == Modalita.PRESENZA && spalmato
}

data class Risultato(
    /** Ingresso mostrato (dopo il clamp alla soglia minima). */
    val ingressoMostrato: Int,
    val ingressoCorretto: Boolean,
    val uscita: Int,
    val uscitaAggiornata: Int?,
    val ritardoMin: Int,
    /** Orario di riferimento per la sveglia di uscita (aggiornata se presente). */
    val riferimentoUscita: Int,
    /** Solo venerdì: orario della timbratura di pausa (ingresso effettivo + 5h12m). */
    val timbraturaPausaVenerdi: Int?,
)

object Calcolo {
    private const val GIORNO = 24 * 60

    fun norm(min: Int): Int = ((min % GIORNO) + GIORNO) % GIORNO

    fun fmt(min: Int): String { val m = norm(min); return "%02d:%02d".format(m / 60, m % 60) }

    fun fmtDurata(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

    fun parse(testo: String): Int? {
        val p = testo.split(":"); if (p.size != 2) return null
        val h = p[0].toIntOrNull() ?: return null; val m = p[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun calcola(s: StatoGiorno): Risultato? {
        val reale = s.ingressoReale ?: return null
        val inizio = if (s.spalmatoEffettivo) Regole.SPALMATO_INIZIO else Regole.NORMALE_INIZIO
        val fine = if (s.spalmatoEffettivo) Regole.SPALMATO_FINE else Regole.NORMALE_FINE

        val corretto = reale < inizio
        val ingresso = if (corretto) inizio else reale
        val ritardo = if (reale > fine) reale - fine else 0

        val durata = if (s.venerdi) Regole.DURATA_VENERDI + s.eccedenzaPausa
        else Regole.DURATA_NORMALE + Regole.PAUSA_BASE + s.eccedenzaPausa
        val uscita = norm(ingresso + durata)

        val aggiornata: Int? = when {
            s.permessoMin > 0 -> norm(uscita - s.permessoMin)
            s.straordinarioMin > 0 -> {
                val extra = if (s.venerdi && s.straordinarioMin >= 60)
                    Regole.PAUSA_VENERDI_CON_STRAORD + s.eccedenzaPausa else 0
                norm(uscita + s.straordinarioMin + extra)
            }
            else -> null
        }
        return Risultato(
            ingressoMostrato = ingresso,
            ingressoCorretto = corretto,
            uscita = uscita,
            uscitaAggiornata = aggiornata,
            ritardoMin = ritardo,
            riferimentoUscita = aggiornata ?: uscita,
            timbraturaPausaVenerdi = if (s.venerdi) norm(ingresso + Regole.DURATA_VENERDI) else null,
        )
    }

    /** La sveglia si propone solo se l'orario di riferimento è ancora futuro. */
    fun svegliaProponibile(riferimento: Int, adessoMin: Int): Boolean = adessoMin < riferimento
}
