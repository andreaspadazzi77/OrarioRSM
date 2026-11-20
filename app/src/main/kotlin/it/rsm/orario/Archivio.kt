package it.rsm.orario

import android.content.Context
import it.rsm.orario.core.Modalita
import it.rsm.orario.core.StatoGiorno
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Persistenza (equivalente di TinyDB): lo stato del giorno è ripristinato solo se la data coincide. */
class Archivio(context: Context) {
    private val p = context.getSharedPreferences("orario_rsm", Context.MODE_PRIVATE)
    private fun oggi() = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date())

    var spalmato: Boolean
        get() = p.getBoolean("Spalmato", false)
        set(v) = p.edit().putBoolean("Spalmato", v).apply()

    var ultimaVersioneVista: String
        get() = p.getString("UltimaVersioneVista", "") ?: ""
        set(v) = p.edit().putString("UltimaVersioneVista", v).apply()

    fun salva(s: StatoGiorno) = p.edit()
        .putString("DataSalvataggio", oggi())
        .putInt("IngressoReale", s.ingressoReale ?: -1)
        .putInt("Permesso", s.permessoMin)
        .putInt("Straordinario", s.straordinarioMin)
        .putInt("MinutiPausaTotali", s.minutiPausaTotali)
        .putBoolean("PausaPranzoAttiva", s.pausaEstesaAttiva)
        .putBoolean("ModalitaPresenza", s.modalita == Modalita.PRESENZA)
        .putBoolean("Spalmato", s.spalmato)
        .apply()

    fun carica(venerdi: Boolean): StatoGiorno {
        val vuoto = StatoGiorno(spalmato = spalmato, venerdi = venerdi)
        if (p.getString("DataSalvataggio", "") != oggi()) return vuoto
        val ing = p.getInt("IngressoReale", -1)
        return StatoGiorno(
            ingressoReale = if (ing >= 0) ing else null,
            modalita = if (p.getBoolean("ModalitaPresenza", true)) Modalita.PRESENZA else Modalita.SMART_WORKING,
            spalmato = spalmato,
            pausaEstesaAttiva = p.getBoolean("PausaPranzoAttiva", false),
            minutiPausaTotali = p.getInt("MinutiPausaTotali", 15),
            permessoMin = p.getInt("Permesso", 0),
            straordinarioMin = p.getInt("Straordinario", 0),
            venerdi = venerdi,
        )
    }
}
