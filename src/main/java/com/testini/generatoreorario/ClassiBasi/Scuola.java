package com.testini.generatoreorario.ClassiBasi;

import java.util.*;

// Rappresenta l'intera scuola con docenti, classi e asegnazioni
public class Scuola {
    private String nome;
    private Map<String, Docente> docenti;
    private Map<String, Classe> classi;
    private List<Assegnazione> assegnazioni;

    public Scuola() {
        this.nome = "Scuola";
        this.docenti = new LinkedHashMap<>();
        this.classi = new LinkedHashMap<>();
        this.assegnazioni = new ArrayList<>();
    }

    public Scuola(String nome) {
        this();
        this.nome = nome;
    }

    // Ritorna un docente esistente o ne crea uno
    public Docente getOCreaDocente(String nomeCognome) {
        return docenti.computeIfAbsent(nomeCognome, Docente::new);
    }

    // Ritorna una classe esistente o ne crea una
    public Classe getOCreaClasse(int anno, char sezione, String indirizzo) {
        String chiaveClasse = anno + "" + sezione + " " + indirizzo;
        return classi.computeIfAbsent(chiaveClasse, chiave -> new Classe(anno, sezione, indirizzo));
    }

    public void aggiungiAssegnazione(Assegnazione assegnazione) {
        assegnazioni.add(assegnazione);
        assegnazione.getDocente().aggiungiAssegnazione(assegnazione);
        assegnazione.getClasse().aggiungiAssegnazione(assegnazione);
    }

    public Map<String, Docente> getDocenti() {
        return docenti;
    }

    public Map<String, Classe> getClassi() {
        return classi;
    }

    public List<Assegnazione> getAssegnazioni() {
        return assegnazioni;
    }

    public Collection<Classe> getTutteClassi() {
        return classi.values();
    }

    public Collection<Docente> getTuttiDocenti() {
        return docenti.values();
    }
}
