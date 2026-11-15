package com.testini.generatoreorario.Parser;

import com.testini.generatoreorario.ClassiBasi.*;
import com.testini.generatoreorario.Generatori.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

// Esportazione orari generati come CSV
public class EsportatoreCSV {

    // Esporta l'orario completo di tutte le classi in un file CSV a griglia
    public static boolean esportaOrarioClassi(Collection<Classe> classi, File fileDestinazione) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDestinazione))) {
            bufferedWriter.write("Classe;Ora;Lunedi;Martedi;Mercoledi;Giovedi;Venerdi");
            bufferedWriter.newLine();

            for (Classe c : classi) {
                Lezione[][] tabellaLezioni = c.getTabellaLezioni();
                for (int ora = 0; ora < GiorniSettimana.ORE_AL_GIORNO; ora++) {
                    StringBuilder riga = new StringBuilder();
                    riga.append(c.getNomeCompleto()).append(";").append(ora + 1).append("° ora");

                    for (int g = 0; g < GiorniSettimana.GIORNI_SCOLASTICI; g++) {
                        riga.append(";");
                        if (tabellaLezioni != null && tabellaLezioni[ora][g] != null) {
                            riga.append(tabellaLezioni[ora][g].toString().replace(";", ","));
                        } else {
                            riga.append("-");
                        }
                    }
                    bufferedWriter.write(riga.toString());
                    bufferedWriter.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Errore durante l'esportazione: " + e.getMessage());
            return false;
        }
    }

    public static boolean esportaOrarioClasseCSV(Classe classe, File fileDestinazione) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDestinazione))) {
            bufferedWriter.write("Classe;Ora;Lunedi;Martedi;Mercoledi;Giovedi;Venerdi");
            bufferedWriter.newLine();

            Lezione[][] tabellaLezioni = classe.getTabellaLezioni();
            for (int ora = 0; ora < GiorniSettimana.ORE_AL_GIORNO; ora++) {
                StringBuilder riga = new StringBuilder();
                riga.append(classe.getNomeCompleto()).append(";").append(ora + 1).append("° ora");

                for (int g = 0; g < GiorniSettimana.GIORNI_SCOLASTICI; g++) {
                    riga.append(";");
                    if (tabellaLezioni != null && tabellaLezioni[ora][g] != null) {
                        riga.append(tabellaLezioni[ora][g].toString().replace(";", ","));
                    } else {
                        riga.append("-");
                    }
                }
                bufferedWriter.write(riga.toString());
                bufferedWriter.newLine();
            }

            return true;
        } catch (IOException e) {
            System.err.println("Errore durante esportazione orario classe!");
            e.printStackTrace();
            return false;
        }
    }

    // TODO: Converte CSV in PDF
    public static boolean OrarioClasseCSVtoPDF(Classe classe, File fileDestinazione) {
        return true;
    }

    // Esporta orario di tutti i docenti in CSV
    public static boolean esportaOrarioDocenti(Collection<Docente> docenti, GeneratoreOrarioScuola generatore, File fileDestinazione) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileDestinazione))) {
            bufferedWriter.write("Docente;Ora;Lunedi;Martedi;Mercoledi;Giovedi;Venerdi");
            bufferedWriter.newLine();

            for (Docente d : docenti) {
                Lezione[][] tabellaLezioni = generatore.estraiOrarioDocente(d);
                for (int ora = 0; ora < GiorniSettimana.ORE_AL_GIORNO; ora++) {
                    StringBuilder riga = new StringBuilder();
                    riga.append(d.getNomeCognome()).append(";").append(ora + 1).append("° ora");

                    for (int g = 0; g < GiorniSettimana.GIORNI_SCOLASTICI; g++) {
                        riga.append(";");
                        if (tabellaLezioni != null && tabellaLezioni[ora][g] != null) {
                            Lezione lezione = tabellaLezioni[ora][g];
                            riga.append(lezione.getMateria().getNomeMateria())
                                .append(" (")
                                .append(lezione.getClasse().getNomeCompleto())
                                .append(")");
                        } else {
                            riga.append("-");
                        }
                    }
                    bufferedWriter.write(riga.toString());
                    bufferedWriter.newLine();
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Errore durante l'esportazione docenti: " + e.getMessage());
            return false;
        }
    }
}
