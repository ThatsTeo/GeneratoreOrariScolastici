package com.testini.generatoreorario.Generatori;

import com.testini.generatoreorario.ClassiBasi.*;
import java.util.*;

public class GeneratoreOrarioScuola {

    private static final int MAX_TENTATIVI = 200;
    private static final int MAX_SCAMBI = 20000;

    private final Scuola scuola;
    private final int giorni = GiorniSettimana.GIORNI_SCOLASTICI;
    private final int ore = GiorniSettimana.ORE_AL_GIORNO;
    private final int TUTTI = (1 << (giorni * ore)) - 1;
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

    // calcola gli slot in cui il docente non puo essere assegnato
    private int vietati(Docente docente) {
        int slotOccupati = bitmaskDocenti.getOrDefault(docente, 0);
        int slotVietati = slotOccupati | bitmaskOreNonDisponibili.getOrDefault(docente, 0);
        int maxOreConsecutive = docente.getOreMaxConsecutive();
        int mascheraGiorno = (1 << ore) - 1;
        for (int g = 0; g < giorni; g++) {
            int bitmaskGiorno = (slotOccupati >> (g * ore)) & mascheraGiorno;
            if (Integer.bitCount(bitmaskGiorno) >= docente.getMaxOreGiorno(GiorniSettimana.values()[g])) {
                slotVietati |= mascheraGiorno << (g * ore);
            } else if (maxOreConsecutive < ore) {
                for (int o = 0; o < ore; o++) {
                    if ((bitmaskGiorno & (1 << o)) == 0 && maxConsecutive(bitmaskGiorno | (1 << o)) > maxOreConsecutive) {
                        slotVietati |= bit(o, g);
                    }
                }
            }
        }
        return slotVietati;
    }

    // calcola le ore consecutive massime
    private int maxConsecutive(int bitmaskGiorno) {
        int maxConsecutive = 0, consecutiveCorrenti = 0;
        for (int ora = 0; ora < ore; ora++) {
            consecutiveCorrenti = ((bitmaskGiorno & (1 << ora)) != 0) ? consecutiveCorrenti + 1 : 0;
            if (consecutiveCorrenti > maxConsecutive) maxConsecutive = consecutiveCorrenti;
        }
        return maxConsecutive;
    }

    // calcola gli slot liberi
    private int liberi(Richiesta richiesta) {
        int slotOccupati = bitmaskClassi.getOrDefault(richiesta.classe, 0);
        for (Docente d : richiesta.docenti) {
            slotOccupati |= vietati(d);
        }
        return ~slotOccupati & TUTTI;
    }

    // seleziona uno slot disponibile dalla bitmask
    private int[] scegliSlot(Richiesta richiesta, int bitmaskSlotDisponibili) {
        int bitmaskSlotAdiacenti = 0, mascheraGiorno = (1 << ore) - 1;
        for (Docente d : richiesta.docenti) {
            if (d.getOreMinConsecutive() > 1) {
                int slotOccupati = bitmaskDocenti.getOrDefault(d, 0);
                for (int g = 0; g < giorni; g++) {
                    int bitmaskGiorno = (slotOccupati >> (g * ore)) & mascheraGiorno;
                    bitmaskSlotAdiacenti |= (((bitmaskGiorno << 1) | (bitmaskGiorno >> 1)) & mascheraGiorno) << (g * ore);
                }
            }
        }
        int slotPreferiti = bitmaskSlotDisponibili & bitmaskSlotAdiacenti;
        return casuale(slotPreferiti != 0 ? slotPreferiti : bitmaskSlotDisponibili);
    }

    // seleziona casualmente un bit e lo rende una coppia ora:giorno
    private int[] casuale(int bitmaskSlot) {
        int indiceCasuale = random.nextInt(Integer.bitCount(bitmaskSlot));
        for (int k = 0; k < indiceCasuale; k++) {
            bitmaskSlot &= bitmaskSlot - 1;
        }
        int posizioneBit = Integer.numberOfTrailingZeros(bitmaskSlot);
        return new int[]{posizioneBit % ore, posizioneBit / ore};
    }

    // converte tutti i bit in coppie ore:giorno
    private List<int[]> tutti(int bitmaskSlot) {
        List<int[]> listaSlot = new ArrayList<>();
        while (bitmaskSlot != 0) {
            int posizioneBit = Integer.numberOfTrailingZeros(bitmaskSlot);
            listaSlot.add(new int[]{
                    posizioneBit % ore, posizioneBit / ore
            });
            bitmaskSlot &= bitmaskSlot - 1;
        }
        return listaSlot;
    }

    // crea le matrici Lezione[][] per ogni docente e aggiorna le bitmask
    private void assegna(Richiesta richiesta, int ora, int giorno) {
        int mascheraSlot = bit(ora, giorno);
        Lezione[] lezioniSlot = new Lezione[richiesta.docenti.size()];
        for (int i = 0; i < richiesta.docenti.size(); i++) {
            Docente docente = richiesta.docenti.get(i);
            lezioniSlot[i] = new Lezione(docente, richiesta.materia, richiesta.classe);
            bitmaskDocenti.merge(docente, mascheraSlot, (precedente, nuovo) -> precedente | nuovo);
        }
        orariClassi.get(richiesta.classe)[ora][giorno] = lezioniSlot;
        bitmaskClassi.merge(richiesta.classe, mascheraSlot, (precedente, nuovo) -> precedente | nuovo);
    }


    // rimuove una lezione
    private Richiesta rimuovi(Classe classe, int ora, int giorno) {
        Lezione[] lezioniSlot = orariClassi.get(classe)[ora][giorno];
        if (lezioniSlot == null || lezioniSlot.length == 0) {
            return null;
        }
        int mascheraSlot = bit(ora, giorno);
        List<Docente> docentiLezione = new ArrayList<>();
        for (Lezione l : lezioniSlot) {
            Docente docente = l.getDocente();
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
        for (Docente d : scuola.getTuttiDocenti()) {
            if (d.getOreTotali() > giorni * ore) {
                oreObiettivo -= (d.getOreTotali() - giorni * ore);
            }
        }

        int migliorPunteggio = -1, migliorPenalita = Integer.MAX_VALUE;
        Map<Classe, Lezione[][][]> migliorOrario = null;

        for (int t = 0; t < MAX_TENTATIVI; t++) {
            int orePiazzate = tentativo(gruppi);
            int penalita = (orePiazzate >= oreObiettivo) ? penalitaMinCons() : Integer.MAX_VALUE;
            if (orePiazzate >= oreObiettivo && penalita == 0) {
                copiaFinale();
                return;
            }
            if (orePiazzate > migliorPunteggio || (orePiazzate == migliorPunteggio && penalita < migliorPenalita)) {
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

    // prova ad assegnare una lezione
    private int tentativo(List<Gruppo> gruppi) {
        bitmaskDocenti = new HashMap<>();
        for (Docente d : scuola.getTuttiDocenti()) {
            bitmaskDocenti.put(d, 0);
        }

        bitmaskClassi = new HashMap<>();
        orariClassi = new HashMap<>();
        for (Classe c : scuola.getTutteClassi()) {
            bitmaskClassi.put(c, 0);
            orariClassi.put(c, new Lezione[ore][giorni][]);
        }

        bitmaskOreNonDisponibili = new HashMap<>();
        for (Docente d : scuola.getTuttiDocenti()) {
            int mascheraIndisponibilita = 0;
            for (GiorniSettimana gl : d.getGiorniLiberi()) {
                int indiceGiorno = gl.ordinal();
                if (indiceGiorno < giorni) {
                    for (int o = 0; o < ore; o++) {
                        mascheraIndisponibilita |= bit(o, indiceGiorno);
                    }
                }
            }
            if (!d.isPuoPrimaOra()) {
                for (int g = 0; g < giorni; g++) mascheraIndisponibilita |= bit(0, g);
            }
            if (!d.isPuoUltimaOra()) {
                for (int g = 0; g < giorni; g++) mascheraIndisponibilita |= bit(ore - 1, g);
            }
            bitmaskOreNonDisponibili.put(d, mascheraIndisponibilita);
        }

        List<Richiesta> richieste = new ArrayList<>();
        for (Gruppo g : gruppi) {
            for (int i = 0; i < g.ore; i++) {
                richieste.add(new Richiesta(g.docenti, g.classe, g.materia));
            }
        }
        Collections.shuffle(richieste, random);
        richieste.sort(Comparator.comparingInt(richiesta -> Integer.bitCount(liberi(richiesta))));

        int orePiazzate = 0;
        List<Richiesta> richiesteFallite = new ArrayList<>();
        for (Richiesta r : richieste) {
            int slotLiberi = liberi(r);
            if (slotLiberi != 0) {
                int[] slotScelto = scegliSlot(r, slotLiberi);
                assegna(r, slotScelto[0], slotScelto[1]);
                orePiazzate++;
            } else {
                richiesteFallite.add(r);
            }
        }

        for (int iter = 0; iter < MAX_SCAMBI && !richiesteFallite.isEmpty(); iter++) {
            Richiesta richiestaCorrente = richiesteFallite.get(random.nextInt(richiesteFallite.size()));
            int slotVietatiDocenti = 0;
            for (Docente d : richiestaCorrente.docenti) {
                slotVietatiDocenti |= vietati(d);
            }
            int bitmaskCandidati = bitmaskClassi.getOrDefault(richiestaCorrente.classe, 0) & ~slotVietatiDocenti & TUTTI;
            List<int[]> listaSlotCandidati = tutti(bitmaskCandidati);
            Collections.shuffle(listaSlotCandidati, random);

            boolean scambioRiuscito = false;
            for (int[] s : listaSlotCandidati) {
                Richiesta richiestaSpostata = rimuovi(richiestaCorrente.classe, s[0], s[1]);
                int slotLiberiSpostata = liberi(richiestaSpostata);
                if (slotLiberiSpostata != 0) {
                    int[] nuovoSlot = casuale(slotLiberiSpostata);
                    assegna(richiestaSpostata, nuovoSlot[0], nuovoSlot[1]);
                    assegna(richiestaCorrente, s[0], s[1]);
                    richiesteFallite.remove(richiestaCorrente);
                    orePiazzate++;
                    scambioRiuscito = true;
                    break;
                } else {
                    assegna(richiestaSpostata, s[0], s[1]);
                }
            }

            if (!scambioRiuscito) {
                for (Docente d : richiestaCorrente.docenti) {
                    if (scambioRiuscito) {
                        break;
                    }
                    int bitmaskConflitti = bitmaskDocenti.get(d) & ~bitmaskClassi.getOrDefault(richiestaCorrente.classe, 0) & TUTTI;
                    for (int[] cs : tutti(bitmaskConflitti)) {
                        Lezione lezioneInConflitto = trovaLezioneDocente(d, cs[0], cs[1]);
                        if (lezioneInConflitto == null) {
                            continue;
                        }
                        Richiesta richiestaSpostata = rimuovi(lezioneInConflitto.getClasse(), cs[0], cs[1]);
                        int slotLiberiSpostata = liberi(richiestaSpostata);
                        if (slotLiberiSpostata == 0) {
                            assegna(richiestaSpostata, cs[0], cs[1]);
                            continue;
                        }
                        int[] nuovoSlot = casuale(slotLiberiSpostata);
                        assegna(richiestaSpostata, nuovoSlot[0], nuovoSlot[1]);
                        int slotLiberiRichiesta = liberi(richiestaCorrente);
                        if (slotLiberiRichiesta != 0) {
                            int[] slotRichiesta = casuale(slotLiberiRichiesta);
                            assegna(richiestaCorrente, slotRichiesta[0], slotRichiesta[1]);
                            richiesteFallite.remove(richiestaCorrente);
                            orePiazzate++;
                            scambioRiuscito = true;
                            break;
                        } else {
                            rimuovi(lezioneInConflitto.getClasse(), nuovoSlot[0], nuovoSlot[1]);
                            assegna(richiestaSpostata, cs[0], cs[1]);
                        }
                    }
                }
            }
        }
        return orePiazzate;
    }

    //
    private int penalitaMinCons() {
        int penalitaTotale = 0, mascheraGiorno = (1 << ore) - 1;
        for (Docente d : scuola.getTuttiDocenti()) {
            int oreMinConsecutive = d.getOreMinConsecutive();
            if (oreMinConsecutive <= 1) continue;
            int slotOccupati = bitmaskDocenti.getOrDefault(d, 0);
            for (int g = 0; g < giorni; g++) {
                int bitmaskGiorno = (slotOccupati >> (g * ore)) & mascheraGiorno;
                int oreConsecutive = 0;
                for (int o = 0; o <= ore; o++) {
                    if (o < ore && (bitmaskGiorno & (1 << o)) != 0) {
                        oreConsecutive++;
                    } else if (oreConsecutive > 0) {
                        if (oreConsecutive < oreMinConsecutive) penalitaTotale += (oreMinConsecutive - oreConsecutive);
                        oreConsecutive = 0;
                    }
                }
            }
        }
        return penalitaTotale;
    }

    // Rileva le compresenze
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
                gruppi.add(new Gruppo(List.of(primaAssegnazione.getDocente()), primaAssegnazione.getClasse(), primaAssegnazione.getMateria(), primaAssegnazione.getOreSettimanali()));
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
                        gruppi.add(new Gruppo(List.of(a.getDocente()), a.getClasse(), a.getMateria(), a.getOreSettimanali() - oreMinime));
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

    // Coapia l'orario attuale (la matrice 3d serve per le compresenze)
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

    //
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
