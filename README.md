# Generatore di Orari Scolastici in Java 🏫📅

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)
![Gradle](https://img.shields.io/badge/Gradle-8.x-green.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)
![Status](https://img.shields.io/badge/Status-Completed-success.svg)

Un'applicazione desktop completa, ad alte prestazioni ed estensibile per la **generazione automatizzata degli orari scolastici**, sviluppata in **Java 21** con interfaccia grafica **JavaFX**.

L'applicativo risolve il problema di ottimizzazione combinatoria dell'orario scolastico (Constraint Satisfaction Problem - CSP) rispettando sia i vincoli rigidi (hard constraints) sia i desiderata dei docenti (soft constraints).

---

## 🌟 Caratteristiche Principali

- ⚡ **Algoritmo CSP ad Alte Prestazioni**: Risoluzione rapida basata su **Bitmasking a 30-bit** (5 giorni × 6 ore) per la verifica istantanea delle collisioni e backtracking stocastico con ricerca locale.
- 👥 **Gestione Nativa delle Compresenze**: Supporto completo per lezioni condivise da più docenti contemporaneamente sulla stessa classe e materia (es. IT + ITP nei laboratori).
- 📂 **Parser CSV Flessibile e Intelligente**:
  - Auto-detection del delimitatore (punto e virgola `;` o virgola `,`).
  - Gestione automatica del carattere UTF-8 BOM (`\uFEFF`).
  - Validazione preventiva con riepilogo dettagliato di errori e avvisi.
- ⚙️ **Configurazione Avanzata dei Vincoli per Docente**:
  - Selezione del giorno libero settimanale.
  - Disponibilità / divieto per la 1ª e l'ultima ora.
  - Limite minimo e massimo di ore consecutive.
  - Tetro massimo di ore di insegnamento al giorno per ciascun docente.
- 🎨 **Interfaccia Grafica Moderna (JavaFX & FXML)**:
  - Vista tabella interattiva per visualizzare l'orario del singolo docente o della singola classe.
  - Pannello dedicato alla configurazione dei vincoli in tempo reale.
  - Dashboard con registro avvisi ed errori di importazione.
- 📄 **Esportazione Multi-Formato**:
  - **CSV**: Esportazione tabellare strutturata a griglia per analisi su fogli di calcolo.
  - **PDF**: Reportistica elegante ed ufficiale generata tramite **OpenPDF**, pronta per la stampa o pubblicazione.

---

## 🏗️ Architettura del Software

Il progetto adotta un'architettura modulare a strati basata sui principi di Separation of Concerns (SoC):

```
src/main/java/com/testini/generatoreorario/
├── ClassiBasi/         # Modello di dominio (Scuola, Classe, Docente, Materia, Assegnazione, Lezione, Tuple, GiorniSettimana)
├── Parser/             # Lettura CSV, validazione e esportazione CSV / PDF (OpenPDF)
├── Generatori/         # Motore algoritmico CSP con Bitmask e gestione vincoli (GeneratoreOrarioScuola)
└── GUI/                # Controller JavaFX, Launcher e viste FXML
```

### Modello dei Dati Principale

| Classe | Descrizione |
| :--- | :--- |
| `Scuola` | Contenitore globale per la gestione delle collezioni di classi, docenti e assegnazioni. |
| `Classe` | Rappresenta la sezione scolastica e mantiene la matrice bidimensionale delle lezioni `[6 ore][5 giorni]`. |
| `Docente` | Rappresenta l'insegnante, il suo monte ore e la configurazione dei vincoli personali. |
| `Assegnazione` | Legame tra docente, classe, materia e numero di ore settimanali previste. |
| `Lezione` | Slot orario contenente la materia, la classe ed una lista di docenti (supporta compresenze). |

---

## ⚙️ Algoritmo di Generazione e Bitmasking

L'algoritmo opera su una matrice 6×5 rappresentata da un intero a 30 bit. Ogni bit rappresenta uno slot temporale:

$$\text{bit} = 1 \ll (\text{giorno} \times 6 + \text{ora})$$

1. **Vincoli HARD (Rigidi)**:
   - Impossibilità per un docente o una classe di essere assegnati contemporaneamente a due lezioni diverse.
   - Sincronizzazione perfetta dei docenti in compresenza.
   - Rispetto esatto del monte ore totale della classe e del docente.
2. **Vincoli SOFT (Preferenziali / Euristici)**:
   - Rispetto del giorno libero del docente.
   - Preferenza/esclusione della 1ª o 6ª ora.
   - Limite ore consecutive per evitare blocchi didattici eccessivi.
   - Distribuzione omogenea del carico orario giornaliero.

In caso di configurazioni particolarmente stringenti, l'algoritmo attiva una fase di **Local Search** con scambi aleatori per sbloccare eventuali deadlock.

---

## 🚀 Requisiti e Guida di Compilazione

### Prerequisiti

- **Java JDK 21** o superiore.
- **Gradle 8.x** (incluso tramite Gradle Wrapper `./gradlew`).

### Comandi Gradle

#### 1. Eseguire l'applicazione in sviluppo
```bash
./gradlew run
```

#### 2. Compilare il progetto ed eseguire i test
```bash
./gradlew build
```

#### 3. Creare il pacchetto distributibile autonomo (JLink Zip)
```bash
./gradlew jlinkZip
```
Il pacchetto ZIP contenente la Runtime Java integrata e l'eseguibile dell'app verrà generato nella cartella `build/distributions/`.

---

## 📋 Formato del File CSV di Input

Il file CSV in ingresso deve contenere l'elenco delle assegnazioni didattiche. Il parser riconosce automaticamente sia il separatore `;` che `,`.

**Esempio di formato CSV (`;`):**
```csv
Docente;Classe;Anno;Sezione;Indirizzo;Materia;Ore
Rossi Mario;1A;1;A;INFO;Informatica;4
Verdi Giuseppe;1A;1;A;INFO;Matematica;4
Bianchi Anna;1A;1;A;INFO;Informatica;2
```
*Nota: Se due o più docenti condividono la stessa classe e materia, il sistema riconoscerà automaticamente la compresenza.*

---

## 👥 Team di Sviluppo

| Nome | Ruolo | Componenti Sviluppate |
| :--- | :--- | :--- |
| **Testini Matteo Carlo** | Project Manager & Lead Dev | Modello Dati, Parser CSV, Algoritmo CSP con Bitmask e gestione vincoli. |
| **Gorreja Polizoi** | UI/UX Developer | Interfaccia grafica JavaFX, layout FXML, integrazione CSS, griglie orarie. |
| **Marku Edonis** | QA & Data Specialist | Modellazione UML, esportazione CSV e PDF (OpenPDF), test suite e collaudo. |

---

## 📄 Documentazione Allegata

Nel repository è disponibile il documento Word formattato con la relazione tecnica dettagliata:
- 📄 **`Documentazione_GeneratoreOrario.docx`**: Documentazione completa (3-4 pagine A4) per la consegna del progetto e la consultazione tecnica.
