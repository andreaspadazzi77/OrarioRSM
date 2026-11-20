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
    }

    /** Chiede all'Orologio di cancellare la sveglia con questa etichetta. */
    fun annulla(context: Context, messaggio: String) {
        val i = Intent(AlarmClock.ACTION_DISMISS_ALARM)
            .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            .putExtra(AlarmClock.EXTRA_MESSAGE, messaggio)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
    }

    /** Cancella la vecchia sveglia e, se l'orario nuovo è ancora futuro, ne crea una nuova. */
    fun aggiorna(context: Context, messaggio: String, nuovoOrarioMin: Int?) {
        annulla(context, messaggio)
        if (nuovoOrarioMin != null)
            Handler(Looper.getMainLooper()).postDelayed({ imposta(context, messaggio, nuovoOrarioMin) }, 1500)
        else Toast.makeText(context, "Sveglia rimossa", Toast.LENGTH_SHORT).show()
    }
}
