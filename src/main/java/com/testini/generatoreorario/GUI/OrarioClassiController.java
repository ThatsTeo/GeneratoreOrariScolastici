package com.testini.generatoreorario.GUI;

import com.testini.generatoreorario.ClassiBasi.Classe;
import com.testini.generatoreorario.ClassiBasi.Docente;
import com.testini.generatoreorario.ClassiBasi.GiorniSettimana;
import com.testini.generatoreorario.ClassiBasi.Lezione;
import com.testini.generatoreorario.ClassiBasi.Scuola;
import com.testini.generatoreorario.Parser.EsportatoreCSV;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OrarioClassiController {

    @FXML
    private Label labelOrario;

    @FXML
    private ComboBox<String> comboBoxClassi;

    @FXML
    private GridPane grigliaOrario;

    @FXML
    private Button bottoneEsporta;

    private Scuola scuola;

    @FXML
    protected void initialize() {
        comboBoxClassi.setOnAction(event -> {
            String nomeClasse = comboBoxClassi.getValue();
            if (nomeClasse != null && scuola != null) {
                Classe classe = scuola.getClassi().get(nomeClasse);
                if (classe != null) {
                    mostraOrarioClasse(classe);
                }
            }
        });
    }

    public void setDati(Scuola scuola, String classeIniziale) {
        this.scuola = scuola;
        var classiOrdinate = new TreeMap<>(this.scuola.getClassi());

        comboBoxClassi.getItems().clear();
        comboBoxClassi.getItems().addAll(classiOrdinate.keySet());

        if (classeIniziale != null && comboBoxClassi.getItems().contains(classeIniziale)) {
            comboBoxClassi.setValue(classeIniziale);
        } else if (!comboBoxClassi.getItems().isEmpty()) {
            comboBoxClassi.setValue(comboBoxClassi.getItems().getFirst());
        }
    }

    private void mostraOrarioClasse(Classe classe) {
        labelOrario.setText("Orario Classe: " + classe.getNomeCompleto());
        grigliaOrario.getChildren().clear();

        String[] giorni = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì"};

        // Cella intestazione ORA
        Label labelIntestazioneOra = new Label("Ora");
        labelIntestazioneOra.getStyleClass().add("cella-header");
        labelIntestazioneOra.setPrefSize(70, 35);
        labelIntestazioneOra.setAlignment(Pos.CENTER);
        grigliaOrario.add(labelIntestazioneOra, 0, 0);

        // Intestazioni dei giorni
        for (int g = 0; g < giorni.length; g++) {
            Label labelGiorno = new Label(giorni[g]);
            labelGiorno.getStyleClass().add("cella-header");
            labelGiorno.setPrefSize(130, 35);
            labelGiorno.setAlignment(Pos.CENTER);
            grigliaOrario.add(labelGiorno, g + 1, 0);
        }

        // Estrazione matrice lezioni [6][5] della classe
        Lezione[][] orario = classe.getTabellaLezioni();

        for (int o = 0; o < GiorniSettimana.ORE_AL_GIORNO; o++) {
            // Etichetta dell'ora
            Label labelOra = new Label((o + 1) + "ª ora");
            labelOra.getStyleClass().add("cella-ora");
            labelOra.setPrefSize(70, 55);
            labelOra.setAlignment(Pos.CENTER);
            grigliaOrario.add(labelOra, 0, o + 1);

            // Singole lezioni per ogni giorno
            for (int g = 0; g < GiorniSettimana.GIORNI_SCOLASTICI; g++) {
                Lezione lezione = (orario != null) ? orario[o][g] : null;
                Label cella = new Label();
                cella.setPrefSize(130, 55);
                cella.setAlignment(Pos.CENTER);
                cella.setTextAlignment(TextAlignment.CENTER);
                cella.setWrapText(true);

                if (lezione != null) {
                    String nomiDocenti = "";
                    if (lezione.getDocenti() != null && !lezione.getDocenti().isEmpty()) {
                        nomiDocenti = lezione.getDocenti().stream()
                                .map(Docente::getNomeCognome)
                                .collect(Collectors.joining(", "));
                    }
                    cella.setText(lezione.getMateria().getNomeMateria() + "\n(" + nomiDocenti + ")");
                    cella.getStyleClass().add("cella-lezione");
                    cella.getStyleClass().add(getClasseStileMateria(lezione.getMateria().getNomeMateria()));
                } else {
                    cella.setText("-");
                    cella.getStyleClass().add("cella-libera");
                }
                grigliaOrario.add(cella, g + 1, o + 1);
            }
        }
    }

    private String getClasseStileMateria(String nomeMateria) {
        if (nomeMateria == null) return "cella-lezione";
        String materiaMinuscolo = nomeMateria.toLowerCase().trim();

        if (materiaMinuscolo.contains("matematica")) return "materia-matematica";
        if (materiaMinuscolo.contains("italiano")) return "materia-italiano";
        if (materiaMinuscolo.contains("storia")) return "materia-storia";
        if (materiaMinuscolo.contains("bio") || materiaMinuscolo.contains("igiene") || materiaMinuscolo.contains("scienze umane") || materiaMinuscolo.contains("scienze integrate")) return "materia-biologia";
        if (materiaMinuscolo.contains("geografia") || materiaMinuscolo.contains("geopedologia")) return "materia-geografia";
        if (materiaMinuscolo.contains("inglese")) return "materia-inglese";
        if (materiaMinuscolo.contains("diritto") || materiaMinuscolo.contains("econ")) return "materia-diritto";
        if (materiaMinuscolo.contains("chimica")) return "materia-chimica";
        if (materiaMinuscolo.contains("fisica")) return "materia-fisica";
        if (materiaMinuscolo.contains("informatica") || materiaMinuscolo.contains("telecomunicazioni") || materiaMinuscolo.contains("sistemi") || materiaMinuscolo.contains("tpsit") || materiaMinuscolo.contains("gpoi")) return "materia-informatica";
        if (materiaMinuscolo.contains("motori") || materiaMinuscolo.contains("sport")) return "materia-motoria";
        if (materiaMinuscolo.contains("religione") || materiaMinuscolo.contains("irc")) return "materia-religione";
        if (materiaMinuscolo.contains("tedesco") || materiaMinuscolo.contains("francese") || materiaMinuscolo.contains("spagnolo")) return "materia-lingue";
        if (materiaMinuscolo.contains("tec") || materiaMinuscolo.contains("cantiere") || materiaMinuscolo.contains("topografia") || materiaMinuscolo.contains("estimo") || materiaMinuscolo.contains("prog.cost")) return "materia-tecnica";

        return "cella-lezione";
    }

    @FXML
    protected void esportaOrarioCSV(ActionEvent e) {
        String nomeClasse = comboBoxClassi.getValue();

        if (nomeClasse != null && scuola != null) {
            Classe classe = scuola.getClassi().get(nomeClasse);

            if (classe != null) {
                String nomeFile = classe.getAnno() + classe.getSezione() + classe.getIndirizzo() + "_Orario" + ".csv";

                if(EsportatoreCSV.esportaOrarioClasseCSV(classe, new File(nomeFile))) System.out.println("Esportazione avvenuta!");
                else System.err.println("Esportazione non avvenuta!");
            }
        }
    }
}
