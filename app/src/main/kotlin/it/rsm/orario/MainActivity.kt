package it.rsm.orario

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.rsm.orario.core.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val archivio = Archivio(this)
        setContent { MaterialTheme { Schermata(archivio) } }
    }
}

private fun adesso(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

private fun scegliOra(c: Context, iniziale: Int, ok: (Int) -> Unit) =
    TimePickerDialog(c, { _, h, m -> ok(h * 60 + m) }, iniziale / 60, iniziale % 60, true).show()

@Composable
private fun Scheda(modifier: Modifier = Modifier, colore: Color = Color.White, content: @Composable ColumnScope.() -> Unit) =
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colore),
        border = BorderStroke(1.dp, Color(0xFFD3D1C7)),
    ) { Column(Modifier.padding(16.dp), content = content) }

@Composable
private fun Schermata(archivio: Archivio) {
    val ctx = LocalContext.current
    val oggi = remember { LocalDate.now() }
    val venerdi = oggi.dayOfWeek == DayOfWeek.FRIDAY
    var stato by remember { mutableStateOf(archivio.carica(venerdi)) }
    var sveglieNascoste by remember { mutableStateOf(setOf<String>()) }
    var info by remember { mutableStateOf(false) }
    var novita by remember { mutableStateOf(archivio.ultimaVersioneVista != BuildConfigVersion.NOME) }
    var testoPausa by remember { mutableStateOf(stato.minutiPausaTotali.toString()) }

    fun aggiorna(n: StatoGiorno) { stato = n; archivio.salva(n) }

    val r = Calcolo.calcola(stato)
    val presenza = stato.modalita == Modalita.PRESENZA
    val data = oggi.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.ITALY))
        .replaceFirstChar { it.uppercase() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7F4F0)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(data, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = { info = true }) { Text("Info") }
        }

        Scheda {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (m in Modalita.values()) {
                    val sel = stato.modalita == m
                    TextButton(onClick = {
                        aggiorna(stato.copy(modalita = m))
                        scegliOra(ctx, stato.ingressoReale ?: adesso()) { aggiorna(stato.copy(modalita = m, ingressoReale = it)) }
                    }) {
                        Text(if (m == Modalita.PRESENZA) "Presenza" else "Smart Working",
                            fontSize = if (sel) 20.sp else 15.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        if (presenza) Scheda {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Orario spalmato", Modifier.weight(1f))
                Switch(stato.spalmato, { archivio.spalmato = it; aggiorna(stato.copy(spalmato = it)) })
            }
        }

        Scheda(colore = Color(0xFFF7F4F0)) {
            Text("Entrata", fontSize = 14.sp)
            TextButton(onClick = { scegliOra(ctx, stato.ingressoReale ?: adesso()) { aggiorna(stato.copy(ingressoReale = it)) } }) {
                Text(r?.let { Calcolo.fmt(it.ingressoMostrato) } ?: "--:--", fontSize = 40.sp, color = Color.Red)
            }
        }

        if (r != null && r.ritardoMin > 0) Scheda(colore = Color(0xFFFFB6C1)) {
            Text("Ritardo: ${Calcolo.fmtDurata(r.ritardoMin)}", fontWeight = FontWeight.Bold)
        }

        Scheda(colore = Color(0xFFE8F2FA)) {
            Text("Uscita", fontSize = 14.sp)
            Text(r?.let { Calcolo.fmt(it.uscita) } ?: "--:--", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        if (presenza) Scheda {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pausa pranzo estesa", Modifier.weight(1f))
                Switch(stato.pausaEstesaAttiva, {
                    testoPausa = "15"
                    aggiorna(stato.copy(pausaEstesaAttiva = it, minutiPausaTotali = Regole.PAUSA_MIN))
                })
            }
            if (stato.pausaEstesaAttiva) Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(testoPausa, { testoPausa = it }, label = { Text("Minuti (16-60)") }, singleLine = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val v = testoPausa.toIntOrNull()
                    if (v != null && v > Regole.PAUSA_MIN && v <= Regole.PAUSA_MAX) aggiorna(stato.copy(minutiPausaTotali = v))
                    else { testoPausa = "15"; aggiorna(stato.copy(minutiPausaTotali = Regole.PAUSA_MIN))
                        Toast.makeText(ctx, "Inserisci un valore tra 16 e 60", Toast.LENGTH_SHORT).show() }
                }) { Text("OK") }
            }
        }

        Scheda(colore = Color(0xFFF7FAFC)) {
            Text("Permesso", fontSize = 14.sp)
            TextButton(onClick = { scegliOra(ctx, stato.permessoMin) { aggiorna(stato.copy(permessoMin = it, straordinarioMin = 0)) } }) {
                Text(if (stato.permessoMin > 0) Calcolo.fmtDurata(stato.permessoMin) else "--:--", fontSize = 28.sp)
            }
        }
        if (presenza) Scheda(colore = Color(0xFFF7FAFC)) {
            Text("Straordinario", fontSize = 14.sp)
            TextButton(onClick = { scegliOra(ctx, stato.straordinarioMin) { aggiorna(stato.copy(straordinarioMin = it, permessoMin = 0)) } }) {
                Text(if (stato.straordinarioMin > 0) Calcolo.fmtDurata(stato.straordinarioMin) else "--:--", fontSize = 28.sp)
            }
        }

        r?.uscitaAggiornata?.let { Scheda { Text("Uscita aggiornata", fontSize = 14.sp); Text(Calcolo.fmt(it), fontSize = 32.sp, fontWeight = FontWeight.Bold) } }

        if (r != null && Calcolo.svegliaProponibile(r.riferimentoUscita, adesso()) && "uscita" !in sveglieNascoste)
            Scheda { Text("Imposta una sveglia 10 minuti prima dell'uscita (${Calcolo.fmt(r.riferimentoUscita)})?")
                Row { TextButton({ Sveglie.imposta(ctx, "Uscita tra 10 minuti", r.riferimentoUscita); sveglieNascoste += "uscita" }) { Text("Sì") }
                    TextButton({ sveglieNascoste += "uscita" }) { Text("No") } } }

        r?.timbraturaPausaVenerdi?.let { p ->
            if (Calcolo.svegliaProponibile(p, adesso()) && "pausa" !in sveglieNascoste)
                Scheda { Text("Venerdì: sveglia 10 minuti prima della timbratura di pausa (${Calcolo.fmt(p)})?")
                    Row { TextButton({ Sveglie.imposta(ctx, "Pausa pranzo tra 10 minuti", p); sveglieNascoste += "pausa" }) { Text("Sì") }
                        TextButton({ sveglieNascoste += "pausa" }) { Text("No") } } }
        }
    }

    if (info) AlertDialog({ info = false }, confirmButton = { TextButton({ info = false }) { Text("OK") } },
        title = { Text("Orario RSM") },
        text = { Text("Versione: ${BuildConfigVersion.NOME}\nFase: Beta\nCredits to Andrea Spadazzi") })
    if (novita) AlertDialog({ }, confirmButton = { TextButton({ archivio.ultimaVersioneVista = BuildConfigVersion.NOME; novita = false }) { Text("OK") } },
        title = { Text("Novità versione ${BuildConfigVersion.NOME}") },
        text = { Text("Versione nativa Android (Kotlin): stesse regole di calcolo, stato giornaliero salvato e sveglie tramite l'app Orologio.") })
}

object BuildConfigVersion { const val NOME = "5.5" }
