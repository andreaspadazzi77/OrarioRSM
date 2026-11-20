package it.rsm.orario.core

import kotlin.test.*

class CalcoloTest {
    private fun h(s: String) = Calcolo.parse(s)!!
    private fun base(ing: String, f: StatoGiorno.() -> StatoGiorno = { this }) =
        StatoGiorno(ingressoReale = h(ing)).f()

    @Test fun normale_0800_esce_1642() {
        val r = Calcolo.calcola(base("08:00"))!!
        assertEquals("16:42", Calcolo.fmt(r.uscita))
    }

    @Test fun nonSpalmato_prima_delle_8_viene_portato_alle_8() {
        val r = Calcolo.calcola(base("07:30"))!!
        assertTrue(r.ingressoCorretto); assertEquals("08:00", Calcolo.fmt(r.ingressoMostrato))
        assertEquals("16:42", Calcolo.fmt(r.uscita))
    }

    @Test fun spalmato_prima_delle_0650() {
        val r = Calcolo.calcola(base("06:20") { copy(spalmato = true) })!!
        assertEquals("06:50", Calcolo.fmt(r.ingressoMostrato)); assertEquals("15:32", Calcolo.fmt(r.uscita))
    }

    @Test fun ritardo_spalmato_e_normale() {
        assertEquals(15, Calcolo.calcola(base("09:05") { copy(spalmato = true) })!!.ritardoMin)
        assertEquals(0, Calcolo.calcola(base("09:05"))!!.ritardoMin)
        assertEquals(30, Calcolo.calcola(base("10:30"))!!.ritardoMin)
    }

    @Test fun venerdi_0743_esce_1255() {
        val r = Calcolo.calcola(base("07:43") { copy(venerdi = true, spalmato = true) })!!
        assertEquals("12:55", Calcolo.fmt(r.uscita))
        assertEquals("12:55", Calcolo.fmt(r.timbraturaPausaVenerdi!!))
    }

    @Test fun venerdi_straordinario_2h30_aggiunge_15_minuti() {
        val r = Calcolo.calcola(base("07:43") { copy(venerdi = true, spalmato = true, straordinarioMin = 150) })!!
        assertEquals("15:40", Calcolo.fmt(r.uscitaAggiornata!!))   // 12:55 + 2:30 + 0:15
    }

    @Test fun venerdi_straordinario_sotto_1h_non_aggiunge() {
        val r = Calcolo.calcola(base("07:43") { copy(venerdi = true, spalmato = true, straordinarioMin = 45) })!!
        assertEquals("13:40", Calcolo.fmt(r.uscitaAggiornata!!))
    }

    @Test fun permesso_sottrae() {
        val r = Calcolo.calcola(base("08:00") { copy(permessoMin = 90) })!!
        assertEquals("15:12", Calcolo.fmt(r.uscitaAggiornata!!)); assertEquals("16:42", Calcolo.fmt(r.uscita))
    }

    @Test fun pausa_estesa_aggiunge_eccedenza() {
        val r = Calcolo.calcola(base("08:00") { copy(pausaEstesaAttiva = true, minutiPausaTotali = 45) })!!
        assertEquals("17:12", Calcolo.fmt(r.uscita))
    }

    @Test fun smartWorking_ignora_spalmato_e_pausa_estesa() {
        val r = Calcolo.calcola(base("07:00") {
            copy(modalita = Modalita.SMART_WORKING, spalmato = true, pausaEstesaAttiva = true, minutiPausaTotali = 60)
        })!!
        assertEquals("08:00", Calcolo.fmt(r.ingressoMostrato)); assertEquals("16:42", Calcolo.fmt(r.uscita))
    }

    @Test fun sveglia_solo_se_futura() {
        assertTrue(Calcolo.svegliaProponibile(h("16:42"), h("12:00")))
        assertFalse(Calcolo.svegliaProponibile(h("16:42"), h("17:00")))
    }
}

class CalcoloVenerdiNonSpalmatoTest {
    @Test fun venerdi_0743_senza_spalmato_ingresso_alle_0800() {
        val r = Calcolo.calcola(StatoGiorno(ingressoReale = Calcolo.parse("07:43")!!, venerdi = true))!!
        assertEquals("13:12", Calcolo.fmt(r.uscita))
    }
}
