package com.testini.generatoreorario.Generatori;

import com.testini.generatoreorario.ClassiBasi.*;
import java.util.*;

public class GeneratoreOrarioScuola {

    private static final int MAX_TENTATIVI = 200;
    private static final int MAX_SCAMBI = 20000;

    private final Scuola scuola;
    private final int giorni = GiorniSettimana.GIORNI_SCOLASTICI;
    private final int ore = GiorniSettimana.ORE_AL_GIORNO;
    private final int MASCHERA_TUTTI_SLOT = (1 << (giorni * ore)) - 1;
    private final Random random = new Random();

    private Map<Docente, Integer> bitmaskDocenti;
    private Map<Docente, Integer> bitmaskOreNonDisponibili;
    private Map<Classe, Integer> bitmaskClassi;
    private Map<Classe, Lezione[][][]> orariClassi;

    private record Gruppo(List<Docente> docenti, Classe classe, Materia materia, int ore) {}
    private record Richiesta(List<Docente> docenti, Classe classe, Materia materia) {}

    public GeneratoreOrarioScuola(Scuola scuola) {
        this.scuola = scuola;
    }

    // converte [ore][giorno] in bitmask
    private int bit(int ora, int giorno) {
        return 1 << (giorno * this.ore + ora);
    }

    // ritorna se il prof ha gia assegnato quello slot
    private boolean docenteInsegna(Docente docente, int ora, int giorno) {
        int slotOccupati = bitmaskDocenti.getOrDefault(docente, 0);
        return (slotOccupati & bit(ora, giorno)) != 0;
    }

    // calcola quante ore insegna docente in quel giorno
    private int calcolaOreDocenteNelGiorno(Docente docente, int giorno) {
        int conteggio = 0;
        for (int o = 0; o < ore; o++) {
            if (docenteInsegna(docente, o, giorno)) {
                conteggio++;
            }
        }
        return conteggio;
    }

    // calcola se il prof insegna piu ore del max
    private boolean superaMaxConsecutiveSeAssegnato(Docente docente, int ora, int giorno, int maxConsentite) {
        int consecutive = 0;
        int maxRaggiunte = 0;
        for (int o = 0; o < ore; o++) {
            boolean insegna = (o == ora) || docenteInsegna(docente, o, giorno);
            if (insegna) {
                consecutive++;
                if (consecutive > maxRaggiunte) {
                    maxRaggiunte = consecutive;
                }
            } else {
                consecutive = 0;
            }
        }
        return maxRaggiunte > maxConsentite;
    }

    // calcola gli slot in cui il docente non puo essere assegnato
    private int slotVietati(Docente docente) {
        int vietati = bitmaskDocenti.getOrDefault(docente, 0) | bitmaskOreNonDisponibili.getOrDefault(docente, 0);
        int maxConsecutive = docente.getOreMaxConsecutive();

        for (int g = 0; g < giorni; g++) {
            GiorniSettimana giornoEnum = GiorniSettimana.values()[g];
            int maxOreGiorno = docente.getMaxOreGiorno(giornoEnum);
            int oreGiaAssegnate = calcolaOreDocenteNelGiorno(docente, g);

            if (oreGiaAssegnate >= maxOreGiorno) {
                for (int o = 0; o < ore; o++) {
                    vietati |= bit(o, g);
                }
            } else if (maxConsecutive < ore) {
                for (int o = 0; o < ore; o++) {
                    if (!docenteInsegna(docente, o, g) && superaMaxConsecutiveSeAssegnato(docente, o, g, maxConsecutive)) {
                        vietati |= bit(o, g);
                    }
                }
            }
        }
        return vietati;
    }

    // ritorna bitmask degli slot liberi
    private int slotLiberi(Richiesta richiesta) {
        int slotNonDisponibili = bitmaskClassi.getOrDefault(richiesta.classe, 0);
        for (Docente docente : richiesta.docenti) {
            slotNonDisponibili |= slotVietati(docente);
        }
        return ~slotNonDisponibili & MASCHERA_TUTTI_SLOT;
    }

    private boolean haDocenteConOreVicine(List<Docente> docenti, int ora, int giorno) {
        for (Docente docente : docenti) {
            if (docente.getOreMinConsecutive() > 1) {
                boolean oraPrecedenteOccupata = (ora > 0) && docenteInsegna(docente, ora - 1, giorno);
                boolean oraSuccessivaOccupata = (ora < ore - 1) && docenteInsegna(docente, ora + 1, giorno);
                if (oraPrecedenteOccupata || oraSuccessivaOccupata) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[] scegliSlot(Richiesta richiesta, int bitmaskSlotDisponibili) {
        List<int[]> disponibili = convertiBitmaskInListaSlot(bitmaskSlotDisponibili);
        List<int[]> preferiti = new ArrayList<>();

        for (int[] slot : disponibili) {
            int oraSlot = slot[0];
            int giornoSlot = slot[1];

            if (haDocenteConOreVicine(richiesta.docenti, oraSlot, giornoSlot)) {
                preferiti.add(slot);
            }
        }

        if (!preferiti.isEmpty()) {
            return preferiti.get(random.nextInt(preferiti.size()));
        }
        return disponibili.get(random.nextInt(disponibili.size()));
    }

    private List<int[]> convertiBitmaskInListaSlot(int bitmask) {
        List<int[]> listaSlot = new ArrayList<>();
        for (int g = 0; g < giorni; g++) {
            for (int o = 0; o < ore; o++) {
                if ((bitmask & bit(o, g)) != 0) {
                    listaSlot.add(new int[]{o, g});
                }
            }
        }
        return listaSlot;
    }

    // seleziona uno slot casuale
    private int[] estraiSlotCasuale(int bitmask) {
        List<int[]> slotDisponibili = convertiBitmaskInListaSlot(bitmask);
        return slotDisponibili.get(random.nextInt(slotDisponibili.size()));
    }

    // assegna un ora a un docente
    private void assegna(Richiesta richiesta, int ora, int giorno) {
        int mascheraSlot = bit(ora, giorno);
        Lezione[] lezioniSlot = new Lezione[richiesta.docenti.size()];
        for (int i = 0; i < richiesta.docenti.size(); i++) {
            Docente docente = richiesta.docenti.get(i);
            lezioniSlot[i] = new Lezione(docente, richiesta.materia, richiesta.classe);
            bitmaskDocenti.put(docente, bitmaskDocenti.getOrDefault(docente, 0) | mascheraSlot);
        }
        orariClassi.get(richiesta.classe)[ora][giorno] = lezioniSlot;
        bitmaskClassi.put(richiesta.classe, bitmaskClassi.getOrDefault(richiesta.classe, 0) | mascheraSlot);
    }

    // rimuovi assegnazione
    private Richiesta rimuovi(Classe classe, int ora, int giorno) {
        Lezione[] lezioniSlot = orariClassi.get(classe)[ora][giorno];
        if (lezioniSlot == null || lezioniSlot.length == 0) {
            return null;
        }
        int mascheraSlot = bit(ora, giorno);
        List<Docente> docentiLezione = new ArrayList<>();
        for (Lezione lezione : lezioniSlot) {
            Docente docente = lezione.getDocente();
            docentiLezione.add(docente);
            bitmaskDocenti.put(docente, bitmaskDocenti.get(docente) & ~mascheraSlot);
        }
        orariClassi.get(classe)[ora][giorno] = null;
        bitmaskClassi.put(classe, bitmaskClassi.get(classe) & ~mascheraSlot);
        return new Richiesta(docentiLezione, classe, lezioniSlot[0].getMateria());
    }

    // genera l'orario
    public void generaOrario() {
        List<Gruppo> gruppi = identificaGruppi();

        int oreObiettivo = gruppi.stream().mapToInt(Gruppo::ore).sum();
        for (Docente docente : scuola.getTuttiDocenti()) {
            int maxOreSettimanali = giorni * ore;
            if (docente.getOreTotali() > maxOreSettimanali) {
                oreObiettivo -= (docente.getOreTotali() - maxOreSettimanali);
            }
        }

        int migliorPunteggio = -1;
        int migliorPenalita = Integer.MAX_VALUE;
        Map<Classe, Lezione[][][]> migliorOrario = null;

        for (int t = 0; t < MAX_TENTATIVI; t++) {
            int orePiazzate = tentativo(gruppi);
            int penalita = (orePiazzate >= oreObiettivo) ? penalitaMinCons() : Integer.MAX_VALUE;

            if (orePiazzate >= oreObiettivo && penalita == 0) {
                copiaFinale();
                return;
            }

            boolean trovatoMigliore = (orePiazzate > migliorPunteggio) || (orePiazzate == migliorPunteggio && penalita < migliorPenalita);

            if (trovatoMigliore) {
                migliorPunteggio = orePiazzate;
                migliorPenalita = penalita;
                migliorOrario = copiaOrari();
            }
        }

        if (migliorOrario != null) {
            orariClassi = migliorOrario;
            copiaFinale();
        }
    }

    // prova ad assegnare
    private int tentativo(List<Gruppo> gruppi) {
        bitmaskDocenti = new HashMap<>();
        for (Docente docente: scuola.getTuttiDocenti()) {
            bitmaskDocenti.put(docente, 0);
        }

        bitmaskClassi = new HashMap<>();
        orariClassi = new HashMap<>();
        for (Classe classe: scuola.getTutteClassi()) {
            bitmaskClassi.put(classe, 0);
            orariClassi.put(classe, new Lezione[ore][giorni][]);
        }

        bitmaskOreNonDisponibili = new HashMap<>();
        for (Docente docente: scuola.getTuttiDocenti()) {
            int mascheraIndisponibilita = 0;
            for (GiorniSettimana gl: docente.getGiorniLiberi()) {
                int indiceGiorno = gl.ordinal();
                if (indiceGiorno < giorni) {
                    for (int o = 0; o < ore; o++) {
                        mascheraIndisponibilita |= bit(o, indiceGiorno);
                    }
                }
            }
            if (!docente.isPuoPrimaOra()) {
                for (int g = 0; g < giorni; g++) {
                    mascheraIndisponibilita |= bit(0, g);
                }
            }
            if (!docente.isPuoUltimaOra()) {
                for (int g = 0; g < giorni; g++) {
                    mascheraIndisponibilita |= bit(ore - 1, g);
                }
            }
            bitmaskOreNonDisponibili.put(docente, mascheraIndisponibilita);
        }

        List<Richiesta> richieste = new ArrayList<>();
        for (Gruppo g: gruppi) {
            for (int i = 0; i < g.ore; i++) {
                richieste.add(new Richiesta(g.docenti, g.classe, g.materia));
            }
        }
        Collections.shuffle(richieste, random);
        richieste.sort(Comparator.comparingInt(richiesta -> Integer.bitCount(slotLiberi(richiesta))));

        int orePiazzate = 0;
        List<Richiesta> richiesteFallite = new ArrayList<>();
        for (Richiesta richiesta : richieste) {
            int disponibili = slotLiberi(richiesta);
            if (disponibili != 0) {
                int[] slotScelto = scegliSlot(richiesta, disponibili);
                assegna(richiesta, slotScelto[0], slotScelto[1]);
                orePiazzate++;
            } else {
                richiesteFallite.add(richiesta);
            }
        }

        for (int iter = 0; iter < MAX_SCAMBI && !richiesteFallite.isEmpty(); iter++) {
            Richiesta richiestaCorrente = richiesteFallite.get(random.nextInt(richiesteFallite.size()));
            int slotVietatiDocenti = 0;
            for (Docente docente : richiestaCorrente.docenti) {
                slotVietatiDocenti |= slotVietati(docente);
            }
            int bitmaskCandidati = bitmaskClassi.getOrDefault(richiestaCorrente.classe, 0)
                    & ~slotVietatiDocenti
                    & MASCHERA_TUTTI_SLOT;
            List<int[]> listaSlotCandidati = convertiBitmaskInListaSlot(bitmaskCandidati);
            Collections.shuffle(listaSlotCandidati, random);

            boolean scambioRiuscito = false;
            for (int[] slot : listaSlotCandidati) {
                int oraSlot = slot[0];
                int giornoSlot = slot[1];
                Richiesta richiestaSpostata = rimuovi(richiestaCorrente.classe, oraSlot, giornoSlot);
                int slotLiberiSpostata = slotLiberi(richiestaSpostata);

                if (slotLiberiSpostata != 0) {
                    int[] nuovoSlot = estraiSlotCasuale(slotLiberiSpostata);
                    assegna(richiestaSpostata, nuovoSlot[0], nuovoSlot[1]);
                    assegna(richiestaCorrente, oraSlot, giornoSlot);
                    richiesteFallite.remove(richiestaCorrente);
                    orePiazzate++;
                    scambioRiuscito = true;
                    break;
                } else {
                    assegna(richiestaSpostata, oraSlot, giornoSlot);
                }
            }

            if (!scambioRiuscito) {
                for (Docente docente : richiestaCorrente.docenti) {
                    if (scambioRiuscito) {
                        break;
                    }
                    int bitmaskConflitti = bitmaskDocenti.getOrDefault(docente, 0)
                            & ~bitmaskClassi.getOrDefault(richiestaCorrente.classe, 0)
                            & MASCHERA_TUTTI_SLOT;
                    List<int[]> slotConflitto = convertiBitmaskInListaSlot(bitmaskConflitti);

                    for (int[] slot : slotConflitto) {
                        int oraSlot = slot[0];
                        int giornoSlot = slot[1];
                        Lezione lezioneInConflitto = trovaLezioneDocente(docente, oraSlot, giornoSlot);
                        if (lezioneInConflitto == null) {
                            continue;
                        }

                        Richiesta richiestaSpostata = rimuovi(lezioneInConflitto.getClasse(), oraSlot, giornoSlot);
                        int slotLiberiSpostata = slotLiberi(richiestaSpostata);

                        if (slotLiberiSpostata == 0) {
                            assegna(richiestaSpostata, oraSlot, giornoSlot);
                            continue;
                        }

                        int[] nuovoSlot = estraiSlotCasuale(slotLiberiSpostata);
                        assegna(richiestaSpostata, nuovoSlot[0], nuovoSlot[1]);

                        int slotLiberiRichiesta = slotLiberi(richiestaCorrente);
                        if (slotLiberiRichiesta != 0) {
                            int[] slotRichiesta = estraiSlotCasuale(slotLiberiRichiesta);
                            assegna(richiestaCorrente, slotRichiesta[0], slotRichiesta[1]);
                            richiesteFallite.remove(richiestaCorrente);
                            orePiazzate++;
                            scambioRiuscito = true;
                            break;
                        } else {
                            rimuovi(lezioneInConflitto.getClasse(), nuovoSlot[0], nuovoSlot[1]);
                            assegna(richiestaSpostata, oraSlot, giornoSlot);
                        }
                    }
                }
            }
        }
        return orePiazzate;
    }

    // ore minime consecutive
    private int penalitaMinCons() {
        int penalitaTotale = 0;

        for (Docente docente : scuola.getTuttiDocenti()) {
            int minimoRichiesto = docente.getOreMinConsecutive();
            if (minimoRichiesto <= 1) {
                continue;
            }

            for (int g = 0; g < giorni; g++) {
                int bloccoConsecutivo = 0;

                for (int o = 0; o < ore; o++) {
                    if (docenteInsegna(docente, o, g)) {
                        bloccoConsecutivo++;
                    } else {
                        if (bloccoConsecutivo > 0 && bloccoConsecutivo < minimoRichiesto) {
                            penalitaTotale += (minimoRichiesto - bloccoConsecutivo);
                        }
                        bloccoConsecutivo = 0;
                    }
                }

                if (bloccoConsecutivo > 0 && bloccoConsecutivo < minimoRichiesto) {
                    penalitaTotale += (minimoRichiesto - bloccoConsecutivo);
                }
            }
        }

        return penalitaTotale;
    }

    // rileva le compresenza
    private List<Gruppo> identificaGruppi() {
        Map<String, List<Assegnazione>> mappaAssegnazioni = new LinkedHashMap<>();
        for (Assegnazione a : scuola.getAssegnazioni()) {
            String chiave = a.getClasse().getNomeCompleto() + "|" + a.getMateria().getNomeMateria();
            mappaAssegnazioni.computeIfAbsent(chiave, chiaveMappa -> new ArrayList<>()).add(a);
        }

        List<Gruppo> gruppi = new ArrayList<>();
        for (List<Assegnazione> lista : mappaAssegnazioni.values()) {
            if (lista.size() == 1) {
                Assegnazione primaAssegnazione = lista.getFirst();
                gruppi.add(new Gruppo(
                        List.of(primaAssegnazione.getDocente()),
                        primaAssegnazione.getClasse(),
                        primaAssegnazione.getMateria(),
                        primaAssegnazione.getOreSettimanali()
                ));
                continue;
            }

            int oreMassime = lista.stream().mapToInt(Assegnazione::getOreSettimanali).max().orElse(0);
            int oreMinime = lista.stream().mapToInt(Assegnazione::getOreSettimanali).min().orElse(0);
            List<Docente> docentiGruppo = lista.stream().map(Assegnazione::getDocente).toList();
            Assegnazione primaAssegnazione = lista.getFirst();

            if (oreMinime == oreMassime) {
                gruppi.add(new Gruppo(docentiGruppo, primaAssegnazione.getClasse(), primaAssegnazione.getMateria(), oreMassime));
            } else {
                gruppi.add(new Gruppo(docentiGruppo, primaAssegnazione.getClasse(), primaAssegnazione.getMateria(), oreMinime));
                for (Assegnazione a : lista) {
                    if (a.getOreSettimanali() > oreMinime) {
                        gruppi.add(new Gruppo(
                                List.of(a.getDocente()),
                                a.getClasse(),
                                a.getMateria(),
                                a.getOreSettimanali() - oreMinime
                        ));
                    }
                }
            }
        }
        return gruppi;
    }

    // Trova tutte le lezioni di un docente in [ora][giorno]
    private Lezione trovaLezioneDocente(Docente docente, int ora, int giorno) {
        for (Classe c : scuola.getTutteClassi()) {
            Lezione[][][] orarioClasse = orariClassi.get(c);
            if (orarioClasse != null && orarioClasse[ora][giorno] != null) {
                for (Lezione l : orarioClasse[ora][giorno]) {
                    if (l.getDocente().equals(docente)) {
                        return l;
                    }
                }
            }
        }
        return null;
    }

    private Map<Classe, Lezione[][][]> copiaOrari() {
        Map<Classe, Lezione[][][]> mappaCopia = new HashMap<>();
        for (Classe c : scuola.getTutteClassi()) {
            Lezione[][][] orarioOriginale = orariClassi.get(c);
            Lezione[][][] orarioCopiato = new Lezione[ore][giorni][];
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (orarioOriginale[o][g] != null) {
                        orarioCopiato[o][g] = orarioOriginale[o][g].clone();
                    }
                }
            }
            mappaCopia.put(c, orarioCopiato);
        }
        return mappaCopia;
    }

    private void copiaFinale() {
        for (Classe c : scuola.getTutteClassi()) {
            Lezione[][] tabellaLezioni = new Lezione[ore][giorni];
            Lezione[][][] orarioClasse = orariClassi.get(c);
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (orarioClasse[o][g] != null && orarioClasse[o][g].length > 0) {
                        List<Docente> docentiCompresenza = Arrays.stream(orarioClasse[o][g]).map(Lezione::getDocente).toList();
                        tabellaLezioni[o][g] = new Lezione(docentiCompresenza, orarioClasse[o][g][0].getMateria(), c);
                    }
                }
            }
            c.setTabellaLezioni(tabellaLezioni);
        }
    }

    // Recupera l'orario di un docente dagli orari delle classi
    public Lezione[][] estraiOrarioDocente(Docente docente) {
        Lezione[][] orarioDocente = new Lezione[ore][giorni];
        for (Classe c : scuola.getTutteClassi()) {
            Lezione[][][] orarioClasse = orariClassi.get(c);
            if (orarioClasse == null) {
                continue;
            }
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (orarioClasse[o][g] != null) {
                        for (Lezione l : orarioClasse[o][g]) {
                            if (l.getDocente().equals(docente)) {
                                orarioDocente[o][g] = l;
                            }
                        }
                    }
                }
            }
        }
        return orarioDocente;
    }
}
