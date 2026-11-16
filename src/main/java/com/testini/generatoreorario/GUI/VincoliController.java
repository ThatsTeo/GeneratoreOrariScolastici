package com.testini.generatoreorario.GUI;

import com.testini.generatoreorario.ClassiBasi.Docente;
import com.testini.generatoreorario.ClassiBasi.GiorniSettimana;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class VincoliController {

    @FXML private Label labelVincoli;
    @FXML private CheckBox primaOraCheck;
    @FXML private CheckBox ultimaOraCheck;
    @FXML private Spinner<Integer> oreConsecSlider;
    @FXML private Spinner<Integer> oreConsMaxSpinner;
    @FXML private ComboBox<GiorniSettimana> maxOreGiornoCB;
    @FXML private Spinner<Integer> maxOreGiornoSpinner;
    @FXML private ComboBox<String> giornoLiberoCB;

    private Docente docente;
    private Runnable onSave;
    private final Map<GiorniSettimana, Integer> maxOreGiornoTemporanee = new EnumMap<>(GiorniSettimana.class);

    @FXML
    protected void initialize() {
        oreConsecSlider.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1));
        oreConsMaxSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 6));
        maxOreGiornoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 6));

        maxOreGiornoCB.getItems().setAll(
                GiorniSettimana.LUNEDI, GiorniSettimana.MARTEDI, GiorniSettimana.MERCOLEDI,
                GiorniSettimana.GIOVEDI, GiorniSettimana.VENERDI
        );
        maxOreGiornoCB.setValue(GiorniSettimana.LUNEDI);

        maxOreGiornoSpinner.valueProperty().addListener((osservabile, vecchioValore, nuovoValore) -> {
            if (maxOreGiornoCB.getValue() != null && nuovoValore != null) {
                maxOreGiornoTemporanee.put(maxOreGiornoCB.getValue(), nuovoValore);
            }
        });
        maxOreGiornoCB.valueProperty().addListener((osservabile, vecchioGiorno, nuovoGiorno) -> {
            if (nuovoGiorno != null) {
                maxOreGiornoSpinner.getValueFactory().setValue(maxOreGiornoTemporanee.getOrDefault(nuovoGiorno, 6));
            }
        });

        giornoLiberoCB.getItems().setAll("Nessuno", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì");
        giornoLiberoCB.getSelectionModel().selectFirst();

        primaOraCheck.setSelected(true);
        ultimaOraCheck.setSelected(true);
    }

    public void setDocente(Docente docente, Runnable onSave) {
        this.docente = docente;
        this.onSave = onSave;
        if (docente != null) {
            labelVincoli.setText("Vincoli per docente: " + docente.getNomeCognome());
            primaOraCheck.setSelected(docente.isPuoPrimaOra());
            ultimaOraCheck.setSelected(docente.isPuoUltimaOra());
            oreConsecSlider.getValueFactory().setValue(docente.getOreMinConsecutive());
            oreConsMaxSpinner.getValueFactory().setValue(docente.getOreMaxConsecutive());
            maxOreGiornoTemporanee.clear();
            for (GiorniSettimana g : maxOreGiornoCB.getItems()) {
                maxOreGiornoTemporanee.put(g, docente.getMaxOreGiorno(g));
            }
            maxOreGiornoSpinner.getValueFactory().setValue(maxOreGiornoTemporanee.get(maxOreGiornoCB.getValue()));

            int indice = 0; // 0 -> non ha il giorno libero
            for (int i = 0; i < GIORNI_SCELTA.length; i++) {
                if (docente.getGiorniLiberi().contains(GIORNI_SCELTA[i])) {
                    indice = i + 1;
                    break;
                }
            }
            giornoLiberoCB.getSelectionModel().select(indice);
        } else {
            labelVincoli.setText("Scegli i vincoli");
        }
    }

    private static final GiorniSettimana[] GIORNI_SCELTA = {
            GiorniSettimana.LUNEDI, GiorniSettimana.MARTEDI, GiorniSettimana.MERCOLEDI,
            GiorniSettimana.GIOVEDI, GiorniSettimana.VENERDI
    };

    @FXML
    protected void onChiudi(ActionEvent event) {
        if (docente != null) {
            int oreMinConsecutive = oreConsecSlider.getValue();
            int oreMaxConsecutive = Math.max(oreMinConsecutive, oreConsMaxSpinner.getValue());
            docente.setPuoPrimaOra(primaOraCheck.isSelected());
            docente.setPuoUltimaOra(ultimaOraCheck.isSelected());
            docente.setOreMinConsecutive(oreMinConsecutive);
            docente.setOreMaxConsecutive(oreMaxConsecutive);
            docente.setMaxOreGiorno(maxOreGiornoTemporanee);
            Set<GiorniSettimana> liberi = EnumSet.noneOf(GiorniSettimana.class);
            int scelta = giornoLiberoCB.getSelectionModel().getSelectedIndex();
            if (scelta > 0) {
                liberi.add(GIORNI_SCELTA[scelta - 1]);
            }
            docente.setGiorniLiberi(liberi);
            if (onSave != null) onSave.run();
        }
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}
