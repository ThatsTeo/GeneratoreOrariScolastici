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
import java.util.Map;

public class VincoliController {

    @FXML private Label labelVincoli;
    @FXML private CheckBox primaOraCheck;
    @FXML private CheckBox ultimaOraCheck;
    @FXML private Spinner<Integer> oreConsecSlider;
    @FXML private Spinner<Integer> oreConsMaxSpinner;
    @FXML private ComboBox<GiorniSettimana> maxOreGiornoCB;
    @FXML private Spinner<Integer> maxOreGiornoSpinner;

    private Docente docente;
    private Runnable onSave;
    private final Map<GiorniSettimana, Integer> maxOreTmp = new EnumMap<>(GiorniSettimana.class);

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

        maxOreGiornoSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (maxOreGiornoCB.getValue() != null && newV != null) {
                maxOreTmp.put(maxOreGiornoCB.getValue(), newV);
            }
        });
        maxOreGiornoCB.valueProperty().addListener((obs, oldG, newG) -> {
            if (newG != null) {
                maxOreGiornoSpinner.getValueFactory().setValue(maxOreTmp.getOrDefault(newG, 6));
            }
        });

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
            maxOreTmp.clear();
            for (GiorniSettimana g : maxOreGiornoCB.getItems()) {
                maxOreTmp.put(g, docente.getMaxOreGiorno(g));
            }
            maxOreGiornoSpinner.getValueFactory().setValue(maxOreTmp.get(maxOreGiornoCB.getValue()));
        } else {
            labelVincoli.setText("Scegli i vincoli");
        }
    }

    @FXML
    protected void onChiudi(ActionEvent event) {
        if (docente != null) {
            int minCons = oreConsecSlider.getValue();
            int maxCons = Math.max(minCons, oreConsMaxSpinner.getValue());
            docente.setPuoPrimaOra(primaOraCheck.isSelected());
            docente.setPuoUltimaOra(ultimaOraCheck.isSelected());
            docente.setOreMinConsecutive(minCons);
            docente.setOreMaxConsecutive(maxCons);
            docente.setMaxOreGiorno(maxOreTmp);
            if (onSave != null) onSave.run();
        }
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }
}
