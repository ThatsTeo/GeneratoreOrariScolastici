package com.testini.generatoreorario.Parser;

import com.testini.generatoreorario.ClassiBasi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

// Gestisce la lettura, validazione e costruzione del modello Scuola da file CSV
public class ParserCSV {
    public static File fileCSV;
    public static List<String[]> datiFile = new ArrayList<>();
    public static List<String> errori = new ArrayList<>();
    public static List<String> avvisi = new ArrayList<>();

    // Legge il file
    public static List<String[]> parse() {
        if (fileCSV == null) {
            errori.add("File CSV non selezionato.");
            return null;
        }

        List<String[]> data = new ArrayList<>();
        errori.clear();
        avvisi.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(fileCSV, StandardCharsets.UTF_8))) {
            String line;
            boolean primaRiga = true;
            int numRiga = 0;
            String delimiter = ";";

            while ((line = br.readLine()) != null) {
                numRiga++;
                if (numRiga == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.trim().isEmpty()) continue;

                if (primaRiga) {
                    primaRiga = false;
                    long countSemicolon = line.chars().filter(ch -> ch == ';').count();
                    long countComma = line.chars().filter(ch -> ch == ',').count();
                    if (countComma > countSemicolon) {
                        delimiter = ",";
                    } else {
                        delimiter = ";";
                    }
                    continue; // Salta intestazione CSV
                }

                String[] values = parseLine(line, delimiter);
                if (values.length < 6) {
                    String alternateDelimiter = delimiter.equals(";") ? "," : ";";
                    String[] altValues = parseLine(line, alternateDelimiter);
                    if (altValues.length >= 6) {
                        values = altValues;
                        delimiter = alternateDelimiter;
                    }
                }

                if (values.length < 6) {
                    errori.add("Riga " + numRiga + " malformata (campi insufficienti): " + line);
                    continue;
                }
                data.add(values);
            }
        } catch (IOException e) {
            errori.add("Errore di lettura I/O: " + e.getMessage());
            return null;
        }

        datiFile = data;
        return data;
    }

    public static String[] parseLine(String line) {
        long countSemicolon = line.chars().filter(ch -> ch == ';').count();
        long countComma = line.chars().filter(ch -> ch == ',').count();
        String delim = (countComma > countSemicolon) ? "," : ";";
        String[] values = parseLine(line, delim);
        if (values.length < 6) {
            String alt = delim.equals(";") ? "," : ";";
            String[] altValues = parseLine(line, alt);
            if (altValues.length >= 6) {
                return altValues;
            }
        }
        return values;
    }

    public static String[] parseLine(String line, String delimiter) {
        char delim = delimiter.charAt(0);
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // virgoletta escapata ("")
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delim && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());

        while (tokens.size() > 6 && tokens.get(tokens.size() - 1).isEmpty()) {
            tokens.remove(tokens.size() - 1);
        }

        return tokens.toArray(new String[0]);
    }

    public static ArrayList<String> getDocenti() {
        ArrayList<String> nomi = new ArrayList<>();
        for(String[] s: datiFile) {
            nomi.add(s[0]);
        }

        return new ArrayList<>(new LinkedHashSet<>(nomi));
    }

    public Scuola costruisciScuola() {
        if (datiFile == null || datiFile.isEmpty()) {
            parse();
        }
        if (datiFile == null) {
            return null;
        }

        Scuola scuola = new Scuola();
        int rigaIdx = 1;

        for (String[] riga : datiFile) {
            rigaIdx++;
            String nomeDocente = riga[0].trim();
            String annoStr = riga[1].trim();
            String sezStr = riga[2].trim();
            String indirizzo = riga[3].trim();
            String nomeMateria = riga[4].trim();
            String oreStr = riga[5].trim();

            if (nomeDocente.isEmpty() || sezStr.isEmpty() || nomeMateria.isEmpty()) {
                errori.add("Riga " + rigaIdx + ": dati anagrafici mancanti.");
                continue;
            }

            int anno;
            try {
                anno = Integer.parseInt(annoStr);
                if (anno <= 0 || anno > 5) {
                    errori.add("Riga " + rigaIdx + ": anno non valido (" + annoStr + ").");
                    continue;
                }
            } catch (NumberFormatException e) {
                errori.add("Riga " + rigaIdx + ": anno non numerico (" + annoStr + ").");
                continue;
            }

            char sezione = sezStr.charAt(0);

            int ore;
            try {
                ore = Integer.parseInt(oreStr);
                if (ore <= 0) {
                    errori.add("Riga " + rigaIdx + ": numero di ore minore o uguale a zero (" + oreStr + ").");
                    continue;
                }
            } catch (NumberFormatException e) {
                errori.add("Riga " + rigaIdx + ": ore non numeriche (" + oreStr + ").");
                continue;
            }

            Docente docente = scuola.getOCreaDocente(nomeDocente);
            Classe classe = scuola.getOCreaClasse(anno, sezione, indirizzo);
            Materia materia = new Materia(nomeMateria);

            // Crea e aggiunge la nuova assegnazione
            Assegnazione assegnazione = new Assegnazione(docente, classe, materia, ore);
            scuola.aggiungiAssegnazione(assegnazione);
        }

        // Segnala eventuali docenti con ore eccessive
        for (Docente d : scuola.getTuttiDocenti()) {
            if (d.getOreTotali() > GiorniSettimana.GIORNI_SCOLASTICI * GiorniSettimana.ORE_AL_GIORNO) {
                avvisi.add("Docente " + d.getNomeCognome() + " ha " + d.getOreTotali() + " ore (supera il limite di 30 ore).");
            }
        }

        return scuola;
    }
}