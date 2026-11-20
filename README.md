# Orario RSM

App Android nativa (Kotlin + Jetpack Compose) che calcola l'orario di uscita a partire dall'orario di ingresso, per Roma Servizi per la Mobilità. Replica l'app realizzata con MIT App Inventor (versione 5.4 Beta).

## Funzioni
- Modalità **Presenza** e **Smart Working** (in Smart Working: niente spalmato, pausa estesa e straordinario).
- Orario **spalmato** (ingresso 06:50–08:50) o normale (08:00–10:00), con correzione dell'ingresso anticipato e calcolo del **ritardo**.
- **Pausa pranzo estesa** (16–60 minuti), **permesso** (sottrae) e **straordinario** (somma), mutuamente esclusivi, con "Uscita aggiornata".
- **Venerdì**: 5h12 senza pausa; con straordinario ≥ 1h si aggiungono 15 minuti e l'eccedenza della pausa estesa.
- **Sveglie** 10 minuti prima dell'uscita (e, il venerdì, della timbratura di pausa) tramite l'app Orologio.
- Stato della giornata salvato e ripristinato solo nello stesso giorno; popup novità a ogni nuova versione.

## Struttura
- `core/` — logica di calcolo in Kotlin puro, con test unitari (`gradle :core:test`).
- `app/` — interfaccia Android (Compose), archivio (SharedPreferences) e sveglie (`AlarmClock.ACTION_SET_ALARM`).

## Build
Apri la cartella con Android Studio (Koala o successivo) e lancia *Run*, oppure:
```
gradle :app:assembleDebug
```
Servono JDK 17 e Android SDK 34. L'APK si trova in `app/build/outputs/apk/debug/`.

## Differenze rispetto alla versione App Inventor
- Il venerdì con ingresso anticipato in modalità spalmata usa sempre le durate del venerdì (nell'originale usava per errore quelle di un giorno normale).
- La sveglia di pausa del venerdì usa l'ingresso effettivo (nell'originale era fisso alle 06:50).
- Le sveglie già impostate non si possono cancellare dall'app (limite di Android).
