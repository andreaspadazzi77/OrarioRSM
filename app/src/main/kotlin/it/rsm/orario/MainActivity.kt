package it.rsm.orario

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
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

private val Sfondo = Color(0xFFF8F8FF)
private val Bordo = Color(0xFFD3D1C7)
private val Blu = Color(0xFF1E90FF)
private val GrigioData = Color(0xFF5A5E5F)
private val Rosso = Color(0xFFFF0000)

@Composable
private fun Scheda(
    modifier: Modifier = Modifier,
    colore: Color = Color.White,
    bordo: Color = Bordo,
    raggio: Int = 14,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.fillMaxWidth()
        .clip(RoundedCornerShape(raggio.dp))
        .background(colore)
        .border(1.5.dp, bordo, RoundedCornerShape(raggio.dp)),
    content = content,
)

@Composable
private fun Icona(id: Int, size: Int = 28, modifier: Modifier = Modifier) =
    Image(painterResource(id), null, modifier.size(size.dp))

@Composable
private fun Schermata(archivio: Archivio) {
    val ctx = LocalContext.current
    val oggi = remember { LocalDate.now() }
    val venerdi = oggi.dayOfWeek == DayOfWeek.FRIDAY
    var stato by remember { mutableStateOf(archivio.carica(venerdi)) }
    var sveglieNascoste by remember { mutableStateOf(setOf<String>()) }
    var info by remember { mutableStateOf(false) }
    var chiediReset by remember { mutableStateOf(false) }
    var novita by remember { mutableStateOf(archivio.ultimaVersioneVista != BuildConfigVersion.NOME) }
    var testoPausa by remember { mutableStateOf(if (stato.pausaEstesaAttiva) stato.minutiPausaTotali.toString() else "") }

    var svegliaUscita by remember { mutableStateOf(archivio.svegliaUscita) }
    var svegliaPausa by remember { mutableStateOf(archivio.svegliaPausa) }

    fun aggiorna(n: StatoGiorno) { stato = n; archivio.salva(n) }
    fun scegliIngresso(m: Modalita) = scegliOra(ctx, stato.ingressoReale ?: adesso()) { aggiorna(stato.copy(modalita = m, ingressoReale = it)) }

    val r = Calcolo.calcola(stato)
    val presenza = stato.modalita == Modalita.PRESENZA
    val data = oggi.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.ITALY))

    // Se l'orario cambia dopo aver aggiunto una sveglia, la sveglia viene aggiornata da sola.
    val rifUscita = r?.riferimentoUscita
    val rifPausa = r?.timbraturaPausaVenerdi
    LaunchedEffect(rifUscita) {
        val vecchia = svegliaUscita
        if (vecchia != null && rifUscita != vecchia) {
            val nuova = rifUscita?.takeIf { Calcolo.svegliaProponibile(it, adesso()) }
            Sveglie.aggiorna(ctx, "Uscita tra 10 minuti", vecchia, nuova)
            svegliaUscita = nuova; archivio.svegliaUscita = nuova
        }
    }
    LaunchedEffect(rifPausa) {
        val vecchia = svegliaPausa
        if (vecchia != null && rifPausa != vecchia) {
            val nuova = rifPausa?.takeIf { Calcolo.svegliaProponibile(it, adesso()) }
            Sveglie.aggiorna(ctx, "Pausa pranzo tra 10 minuti", vecchia, nuova)
            svegliaPausa = nuova; archivio.svegliaPausa = nuova
        }
    }

    Column(
        Modifier.fillMaxSize().background(Sfondo).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Intestazione
        Box(Modifier.fillMaxWidth().height(130.dp)) {
            Row(Modifier.padding(start = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icona(R.drawable.ic_calendario, 40)
                Spacer(Modifier.width(6.dp))
                Text(data, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GrigioData)
            }
            Text("Orario RSM", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp, top = 30.dp))
            Icona(R.drawable.ic_menu, 32, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, top = 30.dp)
                .clickable { info = true })
        }

        // Presenza / Smart Working
        Scheda(Modifier.height(70.dp)) {
            Row(Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in Modalita.values()) {
                    val sel = stato.modalita == m
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Bordo, RoundedCornerShape(12.dp))
                        .clickable { aggiorna(stato.copy(modalita = m)); scegliIngresso(m) },
                        contentAlignment = Alignment.Center) {
                        Text(if (m == Modalita.PRESENZA) "Presenza" else "Smart Working", color = if (sel) Color(0xFF3F5EE8) else Color.Gray,
                            fontSize = if (sel) 20.sp else 15.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        if (presenza) {
            // Spalmato
            Scheda(Modifier.height(70.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icona(R.drawable.ic_spalmato, 28)
                    Text("  Spalmato", fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Interruttore(stato.spalmato) { archivio.spalmato = it; aggiorna(stato.copy(spalmato = it)) }
                }
            }

            // Pausa pranzo estesa
            Scheda {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icona(R.drawable.ic_posate, 36)
                        Text(" Pausa pranzo estesa", fontSize = 20.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("15 min inclusi - estendibile fino a 60", fontSize = 16.sp, color = Color(0xFF888888),
                            modifier = Modifier.weight(1f).padding(start = 10.dp))
                        Interruttore(stato.pausaEstesaAttiva) {
                            testoPausa = ""
                            aggiorna(stato.copy(pausaEstesaAttiva = it, minutiPausaTotali = Regole.PAUSA_MIN))
                        }
                    }
                }
            }
            if (stato.pausaEstesaAttiva) Scheda(Modifier.height(70.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icona(R.drawable.ic_orologio, 32)
                    OutlinedTextField(testoPausa, { testoPausa = it.filter(Char::isDigit).take(2) },
                        placeholder = { Text("16 - 60", color = Color(0xFFCCCCCC)) }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Button(onClick = {
                        val v = testoPausa.toIntOrNull()
                        if (v != null && v > Regole.PAUSA_MIN && v <= Regole.PAUSA_MAX) aggiorna(stato.copy(minutiPausaTotali = v))
                        else { testoPausa = ""; aggiorna(stato.copy(minutiPausaTotali = Regole.PAUSA_MIN))
                            Toast.makeText(ctx, "Inserisci un valore tra 16 e 60", Toast.LENGTH_SHORT).show() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCCCCCC), contentColor = Color.Black)) { Text("Set") }
                }
            }
        }

        // Ingresso / Uscita
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Scheda(Modifier.weight(1f).fillMaxHeight().clickable { scegliIngresso(stato.modalita) }, raggio = 15) {
                Row(Modifier.padding(8.dp).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icona(R.drawable.ic_entrata, 48); Text(" Ingresso", fontSize = 24.sp)
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(r?.let { Calcolo.fmt(it.ingressoMostrato) } ?: "--:--", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                        color = Color.Black)
                }
            }
            Scheda(Modifier.weight(1f).fillMaxHeight(), colore = Color(0xFFE8F2FA), raggio = 15) {
                Row(Modifier.padding(8.dp).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icona(R.drawable.ic_uscita, 48); Text(" Uscita", fontSize = 24.sp, color = Blu)
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(r?.let { Calcolo.fmt(it.uscita) } ?: "--:--", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Blu)
                }
            }
        }

        // Ritardo
        if (r != null && r.ritardoMin > 0) Scheda(Modifier.height(50.dp), colore = Color(0xFFFFB6C1), bordo = Color(0xFFA52A2A)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ritardo di", fontSize = 24.sp, color = Color(0xFFA52A2A), modifier = Modifier.weight(1f))
                Text(Calcolo.fmtDurata(r.ritardoMin), fontSize = 24.sp, color = Color(0xFFA52A2A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        // Permesso / Straordinario
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Scheda(Modifier.weight(1f).fillMaxHeight(), colore = Color(0xFFFFF0F5), raggio = 15) {
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icona(R.drawable.ic_permesso, 40); Text(" Permesso", fontSize = 20.sp)
                }
                CampoOra(if (stato.permessoMin > 0) Calcolo.fmtDurata(stato.permessoMin) else "--:--") {
                    scegliOra(ctx, stato.permessoMin) { aggiorna(stato.copy(permessoMin = it, straordinarioMin = 0)) }
                }
            }
            if (presenza) Scheda(Modifier.weight(1f).fillMaxHeight(), colore = Color(0xFFFFF5EE), raggio = 15) {
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icona(R.drawable.ic_straordinario, 40); Text(" Straordinario", fontSize = 20.sp)
                }
                CampoOra(if (stato.straordinarioMin > 0) Calcolo.fmtDurata(stato.straordinarioMin) else "--:--") {
                    scegliOra(ctx, stato.straordinarioMin) { aggiorna(stato.copy(straordinarioMin = it, permessoMin = 0)) }
                }
            } else Spacer(Modifier.weight(1f))
        }

        // Uscita aggiornata
        r?.uscitaAggiornata?.let {
            Scheda(Modifier.height(80.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Uscita Aggiornata", fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Text(Calcolo.fmt(it), fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }

        // Sveglie
        r?.timbraturaPausaVenerdi?.let { p ->
            if (svegliaPausa == null && Calcolo.svegliaProponibile(p, adesso()) && "pausa" !in sveglieNascoste)
                CardSveglia("Vuoi aggiungere una sveglia", " 10 min prima del pranzo?",
                    { Sveglie.imposta(ctx, "Pausa pranzo tra 10 minuti", p); svegliaPausa = p; archivio.svegliaPausa = p; sveglieNascoste += "pausa" },
                    { sveglieNascoste += "pausa" })
        }
        if (r != null && svegliaUscita == null && Calcolo.svegliaProponibile(r.riferimentoUscita, adesso()) && "uscita" !in sveglieNascoste)
            CardSveglia("Vuoi aggiungere una sveglia", " 10 min prima dell'uscita?",
                { Sveglie.imposta(ctx, "Uscita tra 10 minuti", r.riferimentoUscita); svegliaUscita = r.riferimentoUscita; archivio.svegliaUscita = r.riferimentoUscita; sveglieNascoste += "uscita" },
                { sveglieNascoste += "uscita" })
        if (stato.ingressoReale != null)
            OutlinedButton({ chiediReset = true }, Modifier.fillMaxWidth()) { Text("Azzera gli orari") }
        Spacer(Modifier.height(16.dp))
    }

    if (chiediReset) AlertDialog({ chiediReset = false },
        title = { Text("Azzerare gli orari?") },
        text = { Text("Vengono cancellati ingresso, permesso, straordinario e pausa estesa, e le sveglie impostate dall'app. Potrai aggiungere di nuovo le sveglie.") },
        confirmButton = { TextButton({
            svegliaUscita?.let { Sveglie.annullaPerOrario(ctx, it) }
            svegliaPausa?.let { Sveglie.annullaPerOrario(ctx, it) }
            svegliaUscita = null; svegliaPausa = null
            archivio.svegliaUscita = null; archivio.svegliaPausa = null
            sveglieNascoste = emptySet(); testoPausa = ""
            aggiorna(StatoGiorno(modalita = stato.modalita, spalmato = stato.spalmato, venerdi = venerdi))
            chiediReset = false
        }) { Text("Azzera") } },
        dismissButton = { TextButton({ chiediReset = false }) { Text("Annulla") } })

    if (info) AlertDialog({ info = false }, confirmButton = { TextButton({ info = false }) { Text("OK") } },
        title = { Text("Orario RSM") },
        text = { Text("Versione: 5\nRelease: 5\nFase: Beta\nCredits to Andrea Spadazzi") })
    if (novita) AlertDialog({ }, confirmButton = { TextButton({ archivio.ultimaVersioneVista = BuildConfigVersion.NOME; novita = false }) { Text("OK") } },
        title = { Text("Novità versione ${BuildConfigVersion.NOME}") },
        text = { Text("Versione nativa Android (Kotlin): stesse regole di calcolo, stato giornaliero salvato e sveglie tramite l'app Orologio.") })
}

@Composable
private fun Interruttore(on: Boolean, onChange: (Boolean) -> Unit) =
    Box(Modifier.width(48.dp).height(28.dp).clickable { onChange(!on) }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(38.dp).height(15.dp).clip(RoundedCornerShape(8.dp))
            .background(if (on) Color(0xFF7DE05A) else Color(0xFF6B6B6B)))
        Box(Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart).let { it }
            .padding(horizontal = 0.dp).size(24.dp)
            .shadow(3.dp, CircleShape).clip(CircleShape)
            .background(if (on) Color(0xFFF7F7F7) else Color(0xFFCFCFCF)))
    }

@Composable
private fun CampoOra(testo: String, onClick: () -> Unit) =
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).height(44.dp)
        .clip(RoundedCornerShape(4.dp)).background(Color.White).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(testo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

@Composable
private fun CardSveglia(riga1: String, riga2: String, ok: () -> Unit, no: () -> Unit) =
    Scheda(Modifier.height(100.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icona(R.drawable.ic_sveglia, 50)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(riga1, fontSize = 20.sp); Text(riga2, fontSize = 20.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                Icona(R.drawable.ic_accetta, 40, Modifier.clickable(onClick = ok))
                Spacer(Modifier.height(6.dp))
                Icona(R.drawable.ic_rifiuta, 40, Modifier.clickable(onClick = no))
            }
        }
    }

object BuildConfigVersion { const val NOME = "5.5" }
