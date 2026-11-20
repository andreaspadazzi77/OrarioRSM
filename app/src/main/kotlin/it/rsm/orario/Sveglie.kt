package it.rsm.orario

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.widget.Toast
import it.rsm.orario.core.Calcolo
import it.rsm.orario.core.Regole

/** Imposta una sveglia nell'app Orologio, 10 minuti prima dell'orario dato. Non richiede permessi. */
object Sveglie {
    private val main = Handler(Looper.getMainLooper())

    /** Dopo l'intervento dell'Orologio, riporta Orario RSM davanti. */
    private fun riportaInPrimoPiano(context: Context, dopoMs: Long = 800) {
        main.postDelayed({
            val i = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(i) }
        }, dopoMs)
    }

    fun imposta(context: Context, messaggio: String, orarioMin: Int) {
        val t = Calcolo.norm(orarioMin - Regole.MINUTI_SVEGLIA_ANTICIPO)
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_MESSAGE, messaggio)
            .putExtra(AlarmClock.EXTRA_HOUR, t / 60)
            .putExtra(AlarmClock.EXTRA_MINUTES, t % 60)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val esito = runCatching { context.startActivity(i) }
        Toast.makeText(
            context,
            if (esito.isSuccess) "Sveglia impostata alle ${Calcolo.fmt(t)}" else "Impossibile impostare la sveglia",
            Toast.LENGTH_LONG,
        ).show()
        riportaInPrimoPiano(context)
    }

    /** Chiede all'Orologio di cancellare la sveglia impostata a quell'orario (10 minuti prima del riferimento). */
    fun annullaPerOrario(context: Context, riferimentoMin: Int) {
        val t = Calcolo.norm(riferimentoMin - Regole.MINUTI_SVEGLIA_ANTICIPO)
        val i = Intent(AlarmClock.ACTION_DISMISS_ALARM)
            .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME)
            .putExtra(AlarmClock.EXTRA_HOUR, t / 60)
            .putExtra(AlarmClock.EXTRA_MINUTES, t % 60)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
        riportaInPrimoPiano(context)
    }

    /** Cancella più sveglie una dopo l'altra, aspettando che l'app sia tornata davanti prima di ogni richiesta. */
    fun annullaTutte(context: Context, riferimenti: List<Int>) {
        riferimenti.forEachIndexed { n, rif -> main.postDelayed({ annullaPerOrario(context, rif) }, n * 2000L) }
    }

    /**
     * Sposta la sveglia dal vecchio al nuovo orario. Prima crea la nuova (mentre l'app è ancora in
     * primo piano, altrimenti Android blocca l'avvio) e solo dopo cancella la vecchia.
     */
    fun aggiorna(context: Context, messaggio: String, vecchioRifMin: Int, nuovoRifMin: Int?) {
        if (nuovoRifMin != null) imposta(context, messaggio, nuovoRifMin)
        else Toast.makeText(context, "Sveglia rimossa", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ annullaPerOrario(context, vecchioRifMin) }, 1000)
    }
}
