package com.testini.generatoreorario.GUI;

import com.testini.generatoreorario.Parser.ParserCSV;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Collection;

public class AvvisiController {
    private Collection<String> listaAvvisi = ParserCSV.avvisi;
    private Collection<String> listaErrori = ParserCSV.errori;

    @FXML
    protected TextArea txtAreaAvvisi = new TextArea();

    @FXML
    protected TextArea txtAreaErrori = new TextArea();

}
