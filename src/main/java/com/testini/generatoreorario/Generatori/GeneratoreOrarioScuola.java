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
    private final Random rand = new Random();

    private Map<Docente, Integer> bitDoc;
    private Map<Docente, Integer> bitLiberi;
    private Map<Classe, Integer> bitCla;
    private Map<Classe, Lezione[][][]> orari;

    private record Gruppo(List<Docente> doc, Classe cl, Materia mat, int ore) {}
    private record Req(List<Docente> doc, Classe cl, Materia mat) {}

    public GeneratoreOrarioScuola(Scuola scuola) {
        this.scuola = scuola;
    }

    private int bit(int o, int g) {
        return 1 << (g * ore + o);
    }

    private int vietati(Docente d) {
        int occ = bitDoc.getOrDefault(d, 0);
        int v = occ | bitLiberi.getOrDefault(d, 0);
        int maxCons = d.getOreMaxConsecutive();
        int maskG = (1 << ore) - 1;
        for (int g = 0; g < giorni; g++) {
            int day = (occ >> (g * ore)) & maskG;
            if (Integer.bitCount(day) >= d.getMaxOreGiorno(GiorniSettimana.values()[g])) {
                v |= maskG << (g * ore);
            } else if (maxCons < ore) {
                for (int o = 0; o < ore; o++) {
                    if ((day & (1 << o)) == 0 && maxConsecutive(day | (1 << o)) > maxCons) {
                        v |= bit(o, g);
                    }
                }
            }
        }
        return v;
    }

    private int maxConsecutive(int day) {
        int max = 0, cur = 0;
        for (int o = 0; o < ore; o++) {
            cur = ((day & (1 << o)) != 0) ? cur + 1 : 0;
            if (cur > max) max = cur;
        }
        return max;
    }

    private int liberi(Req r) {
        int occ = bitCla.getOrDefault(r.cl, 0);
        for (Docente d: r.doc) {
            occ |= vietati(d);
        }
        return ~occ & TUTTI;
    }

    private int[] scegliSlot(Req r, int bits) {
        int adj = 0, maskG = (1 << ore) - 1;
        for (Docente d : r.doc) {
            if (d.getOreMinConsecutive() > 1) {
                int occ = bitDoc.getOrDefault(d, 0);
                for (int g = 0; g < giorni; g++) {
                    int day = (occ >> (g * ore)) & maskG;
                    adj |= (((day << 1) | (day >> 1)) & maskG) << (g * ore);
                }
            }
        }
        int pref = bits & adj;
        return casuale(pref != 0 ? pref : bits);
    }

    private int[] casuale(int bits) {
        int n = rand.nextInt(Integer.bitCount(bits));
        for (int k = 0; k < n; k++) {
            bits &= bits - 1;
        }
        int p = Integer.numberOfTrailingZeros(bits);
        return new int[]{p % ore, p / ore};
    }

    private List<int[]> tutti(int bits) {
        List<int[]> l = new ArrayList<>();
        while (bits != 0) {
            int p = Integer.numberOfTrailingZeros(bits);
            l.add(new int[]{p % ore, p / ore});
            bits &= bits - 1;
        }
        return l;
    }

    private void assegna(Req r, int o, int g) {
        int b = bit(o, g);
        Lezione[] lez = new Lezione[r.doc.size()];
        for (int i = 0; i < r.doc.size(); i++) {
            Docente d = r.doc.get(i);
            lez[i] = new Lezione(d, r.mat, r.cl);
            bitDoc.merge(d, b, (a, v) -> a | v);
        }
        orari.get(r.cl)[o][g] = lez;
        bitCla.merge(r.cl, b, (a, v) -> a | v);
    }

    private Req rimuovi(Classe cl, int o, int g) {
        Lezione[] lez = orari.get(cl)[o][g];
        if (lez == null || lez.length == 0) {
            return null;
        }
        int b = bit(o, g);
        List<Docente> doc = new ArrayList<>();
        for (Lezione l: lez) {
            Docente d = l.getDocente();
            doc.add(d);
            bitDoc.put(d, bitDoc.get(d) & ~b);
        }
        orari.get(cl)[o][g] = null;
        bitCla.put(cl, bitCla.get(cl) & ~b);
        return new Req(doc, cl, lez[0].getMateria());
    }

    public void generaOrario() {
        List<Gruppo> gruppi = identificaGruppi();
        int obiettivo = gruppi.stream().mapToInt(Gruppo::ore).sum();
        for (Docente d: scuola.getTuttiIDocenti()) {
            if (d.getOreTotali() > giorni * ore) {
                obiettivo -= (d.getOreTotali() - giorni * ore);
            }
        }

        int best = -1, bestPen = Integer.MAX_VALUE;
        Map<Classe, Lezione[][][]> bestOrario = null;

        for (int t = 0; t < MAX_TENTATIVI; t++) {
            int p = tentativo(gruppi);
            int pen = (p >= obiettivo) ? penalitaMinCons() : Integer.MAX_VALUE;
            if (p >= obiettivo && pen == 0) {
                copiaFinale();
                return;
            }
            if (p > best || (p == best && pen < bestPen)) {
                best = p;
                bestPen = pen;
                bestOrario = copiaOrari();
            }
        }

        if (bestOrario != null) {
            orari = bestOrario;
            copiaFinale();
        }
    }

    private int tentativo(List<Gruppo> gruppi) {
        bitDoc = new HashMap<>();
        for (Docente d: scuola.getTuttiIDocenti()) {
            bitDoc.put(d, 0);
        }

        bitCla = new HashMap<>();
        orari = new HashMap<>();
        for (Classe c: scuola.getTutteLeClassi()) {
            bitCla.put(c, 0);
            orari.put(c, new Lezione[ore][giorni][]);
        }

        bitLiberi = new HashMap<>();
        for (Docente d: scuola.getTuttiIDocenti()) {
            int m = 0;
            for (GiorniSettimana gl: d.getGiorniLiberi()) {
                int gi = gl.ordinal();
                if (gi < giorni) {
                    for (int o = 0; o < ore; o++) {
                        m |= bit(o, gi);
                    }
                }
            }
            if (!d.isPuoPrimaOra()) {
                for (int g = 0; g < giorni; g++) m |= bit(0, g);
            }
            if (!d.isPuoUltimaOra()) {
                for (int g = 0; g < giorni; g++) m |= bit(ore - 1, g);
            }
            bitLiberi.put(d, m);
        }

        List<Req> richieste = new ArrayList<>();
        for (Gruppo g: gruppi) {
            for (int i = 0; i < g.ore; i++) {
                richieste.add(new Req(g.doc, g.cl, g.mat));
            }
        }
        Collections.shuffle(richieste, rand);
        richieste.sort(Comparator.comparingInt(r -> Integer.bitCount(liberi(r))));

        int piazzati = 0;
        List<Req> fallite = new ArrayList<>();
        for (Req r: richieste) {
            int l = liberi(r);
            if (l != 0) {
                int[] s = scegliSlot(r, l);
                assegna(r, s[0], s[1]);
                piazzati++;
            } else {
                fallite.add(r);
            }
        }

        for (int iter = 0; iter < MAX_SCAMBI && !fallite.isEmpty(); iter++) {
            Req r = fallite.get(rand.nextInt(fallite.size()));
            int docViet = 0;
            for (Docente d: r.doc) {
                docViet |= vietati(d);
            }
            int cand = bitCla.getOrDefault(r.cl, 0) & ~docViet & TUTTI;
            List<int[]> slots = tutti(cand);
            Collections.shuffle(slots, rand);

            boolean ok = false;
            for (int[] s: slots) {
                Req spost = rimuovi(r.cl, s[0], s[1]);
                int lib = liberi(spost);
                if (lib != 0) {
                    int[] ns = casuale(lib);
                    assegna(spost, ns[0], ns[1]);
                    assegna(r, s[0], s[1]);
                    fallite.remove(r);
                    piazzati++;
                    ok = true;
                    break;
                } else {
                    assegna(spost, s[0], s[1]);
                }
            }

            if (!ok) {
                for (Docente d: r.doc) {
                    if (ok) {
                        break;
                    }
                    int conf = bitDoc.get(d) & ~bitCla.getOrDefault(r.cl, 0) & TUTTI;
                    for (int[] cs: tutti(conf)) {
                        Lezione lez = trovaLezioneDocente(d, cs[0], cs[1]);
                        if (lez == null) {
                            continue;
                        }
                        Req spost = rimuovi(lez.getClasse(), cs[0], cs[1]);
                        int lib = liberi(spost);
                        if (lib == 0) {
                            assegna(spost, cs[0], cs[1]);
                            continue;
                        }
                        int[] ns = casuale(lib);
                        assegna(spost, ns[0], ns[1]);
                        int lr = liberi(r);
                        if (lr != 0) {
                            int[] sr = casuale(lr);
                            assegna(r, sr[0], sr[1]);
                            fallite.remove(r);
                            piazzati++;
                            ok = true;
                            break;
                        } else {
                            rimuovi(lez.getClasse(), ns[0], ns[1]);
                            assegna(spost, cs[0], cs[1]);
                        }
                    }
                }
            }
        }
        return piazzati;
    }

    private int penalitaMinCons() {
        int pen = 0, maskG = (1 << ore) - 1;
        for (Docente d : scuola.getTuttiIDocenti()) {
            int minC = d.getOreMinConsecutive();
            if (minC <= 1) continue;
            int occ = bitDoc.getOrDefault(d, 0);
            for (int g = 0; g < giorni; g++) {
                int day = (occ >> (g * ore)) & maskG;
                int run = 0;
                for (int o = 0; o <= ore; o++) {
                    if (o < ore && (day & (1 << o)) != 0) {
                        run++;
                    } else if (run > 0) {
                        if (run < minC) pen += (minC - run);
                        run = 0;
                    }
                }
            }
        }
        return pen;
    }

    private List<Gruppo> identificaGruppi() {
        Map<String, List<Assegnazione>> mappa = new LinkedHashMap<>();
        for (Assegnazione a: scuola.getAssegnazioni()) {
            String chiave = a.getClasse().getNomeCompleto() + "|" + a.getMateria().getNomeMateria();
            mappa.computeIfAbsent(chiave, k -> new ArrayList<>()).add(a);
        }

        List<Gruppo> gruppi = new ArrayList<>();
        for (List<Assegnazione> lista: mappa.values()) {
            if (lista.size() == 1) {
                Assegnazione a = lista.getFirst();
                gruppi.add(new Gruppo(List.of(a.getDocente()), a.getClasse(), a.getMateria(), a.getOreSettimanali()));
                continue;
            }
            int max = lista.stream().mapToInt(Assegnazione::getOreSettimanali).max().orElse(0);
            int min = lista.stream().mapToInt(Assegnazione::getOreSettimanali).min().orElse(0);
            List<Docente> doc = lista.stream().map(Assegnazione::getDocente).toList();
            Assegnazione p = lista.getFirst();
            if (min == max) {
                gruppi.add(new Gruppo(doc, p.getClasse(), p.getMateria(), max));
            } else {
                gruppi.add(new Gruppo(doc, p.getClasse(), p.getMateria(), min));
                for (Assegnazione a : lista) {
                    if (a.getOreSettimanali() > min) {
                        gruppi.add(new Gruppo(List.of(a.getDocente()), a.getClasse(), a.getMateria(), a.getOreSettimanali() - min));
                    }
                }
            }
        }
        return gruppi;
    }

    private Lezione trovaLezioneDocente(Docente doc, int o, int g) {
        for (Classe c: scuola.getTutteLeClassi()) {
            Lezione[][][] or = orari.get(c);
            if (or != null && or[o][g] != null) {
                for (Lezione l : or[o][g]) {
                    if (l.getDocente().equals(doc)) {
                        return l;
                    }
                }
            }
        }
        return null;
    }

    private Map<Classe, Lezione[][][]> copiaOrari() {
        Map<Classe, Lezione[][][]> copia = new HashMap<>();
        for (Classe c: scuola.getTutteLeClassi()) {
            Lezione[][][] orig = orari.get(c);
            Lezione[][][] cp = new Lezione[ore][giorni][];
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (orig[o][g] != null) {
                        cp[o][g] = orig[o][g].clone();
                    }
                }
            }
            copia.put(c, cp);
        }
        return copia;
    }

    private void copiaFinale() {
        for (Classe c: scuola.getTutteLeClassi()) {
            Lezione[][] tab = new Lezione[ore][giorni];
            Lezione[][][] or = orari.get(c);
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (or[o][g] != null && or[o][g].length > 0) {
                        List<Docente> dc = Arrays.stream(or[o][g]).map(Lezione::getDocente).toList();
                        tab[o][g] = new Lezione(dc, or[o][g][0].getMateria(), c);
                    }
                }
            }
            c.setTabellaLezioni(tab);
        }
    }

    public Lezione[][] estraiOrarioDocente(Docente docente) {
        Lezione[][] od = new Lezione[ore][giorni];
        for (Classe c: scuola.getTutteLeClassi()) {
            Lezione[][][] or = orari.get(c);
            if (or == null) {
                continue;
            }
            for (int o = 0; o < ore; o++) {
                for (int g = 0; g < giorni; g++) {
                    if (or[o][g] != null) {
                        for (Lezione l: or[o][g]) {
                            if (l.getDocente().equals(docente)) {
                                od[o][g] = l;
                            }
                        }
                    }
                }
            }
        }
        return od;
    }
}
