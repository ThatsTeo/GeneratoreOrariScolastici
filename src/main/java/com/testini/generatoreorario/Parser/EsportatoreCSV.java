package com.testini.generatoreorario.Parser;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.testini.generatoreorario.ClassiBasi.*;
import com.testini.generatoreorario.Generatori.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.awt.Color;
import java.io.FileOutputStream;

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

    // Converte CSV in PDF
    public static boolean esportaOrarioClassePDF(Classe classe, File fileDestinazione) {
        Document document = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileDestinazione));
            document.open();

            Font fontTitolo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph titolo = new Paragraph("Orario Classe " + classe.getNomeCompleto(), fontTitolo);
            titolo.setSpacingAfter(15f);
            document.add(titolo);

            int numColonne = 1 + GiorniSettimana.GIORNI_SCOLASTICI;
            PdfPTable table = new PdfPTable(numColonne);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2.2f, 2.2f, 2.2f, 2.2f, 2.2f});

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fontOra = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font fontCella = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            String[] intestazioni = {"Ora", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì"};
            for (String h : intestazioni) {
                PdfPCell cellaHeader = new PdfPCell(new Phrase(h, fontHeader));
                cellaHeader.setBackgroundColor(new Color(45, 52, 54));
                cellaHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellaHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellaHeader.setPadding(8f);
                table.addCell(cellaHeader);
            }

            Lezione[][] tabellaLezioni = classe.getTabellaLezioni();
            for (int ora = 0; ora < GiorniSettimana.ORE_AL_GIORNO; ora++) {
                Color bgRiga = (ora % 2 == 1) ? new Color(245, 246, 250) : Color.WHITE;

                PdfPCell cellaOra = new PdfPCell(new Phrase((ora + 1) + "° ora", fontOra));
                cellaOra.setBackgroundColor(bgRiga);
                cellaOra.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellaOra.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellaOra.setPadding(6f);
                table.addCell(cellaOra);

                for (int g = 0; g < GiorniSettimana.GIORNI_SCOLASTICI; g++) {
                    String testo = "-";
                    if (tabellaLezioni != null && tabellaLezioni[ora][g] != null) {
                        testo = tabellaLezioni[ora][g].toString();
                    }

                    PdfPCell cellaLezione = new PdfPCell(new Phrase(testo, fontCella));
                    cellaLezione.setBackgroundColor(bgRiga);
                    cellaLezione.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cellaLezione.setPadding(6f);
                    if (testo.equals("-")) {
                        cellaLezione.setHorizontalAlignment(Element.ALIGN_CENTER);
                    }
                    table.addCell(cellaLezione);
                }
            }

            document.add(table);
            document.close();
            return true;

        } catch (Exception e) {
            System.err.println("Errore durante esportazione PDF orario classe!");
            e.printStackTrace();
            if (document.isOpen()) document.close();
            return false;
        }
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
