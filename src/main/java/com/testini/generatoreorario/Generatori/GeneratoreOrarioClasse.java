package com.testini.generatoreorario.Generatori;

import com.testini.generatoreorario.ClassiBasi.*;

// Accesso e visualizzazione dell'orario di una singola classe
public class GeneratoreOrarioClasse {
    private final Classe classe;

    public GeneratoreOrarioClasse(Classe classe) {
        this.classe = classe;
    }

    // Restituisce la matrice delle lezioni della classe
    public Lezione[][] getOrario() {
        return classe.getTabellaLezioni();
    }

    public Classe getClasse() {
        return classe;
    }
}
