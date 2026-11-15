package com.testini.generatoreorario.GUI;

import com.testini.generatoreorario.ClassiBasi.Docente;
import com.testini.generatoreorario.ClassiBasi.GiorniSettimana;
import com.testini.generatoreorario.ClassiBasi.Lezione;
import com.testini.generatoreorario.ClassiBasi.Scuola;
import com.testini.generatoreorario.Generatori.GeneratoreOrarioScuola;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

public class OrarioController {

    @FXML
    private Label labelOrario;

    @FXML
    private ComboBox<String> comboBoxDocenti;

    @FXML
    private GridPane grigliaOrario;

    private GeneratoreOrarioScuola generatore;
    private Scuola scuola;

    @FXML
    protected void initialize() {
        comboBoxDocenti.setOnAction(event -> {
            String nomeDocente = comboBoxDocenti.getValue();
            if (nomeDocente != null && scuola != null && generatore != null) {
                Docente docente = scuola.getDocenti().get(nomeDocente);
                if (docente != null) {
                    mostraOrarioDocente(docente);
                }
            }
        });
    }

    public void setDati(GeneratoreOrarioScuola generatore, Scuola scuola, String docenteIniziale) {
        this.generatore = generatore;
        this.scuola = scuola;

        comboBoxDocenti.getItems().clear();
        if (scuola != null) {
            comboBoxDocenti.getItems().addAll(scuola.getDocenti().keySet());
        }

        if (docenteIniziale != null && comboBoxDocenti.getItems().contains(docenteIniziale)) {
            comboBoxDocenti.setValue(docenteIniziale);
        } else if (!comboBoxDocenti.getItems().isEmpty()) {
            comboBoxDocenti.setValue(comboBoxDocenti.getItems().getFirst());
        }
    }

    public void setDocente(String docente) {
        if (docente != null && !docente.isEmpty()) {
            comboBoxDocenti.setValue(docente);
        }
    }

    private void mostraOrarioDocente(Docente docente) {
        labelOrario.setText("Orario di: " + docente.getNomeCognome());
        grigliaOrario.getChildren().clear();

        String[] giorni = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì"};

        // Cella intestazione
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

        // Estrazione lezioni dal generatore del backend
        Lezione[][] orario = generatore.estraiOrarioDocente(docente);

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
                    cella.setText(lezione.getMateria().getNomeMateria() + "\n(" + lezione.getClasse().getNomeCompleto() + ")");
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
}
