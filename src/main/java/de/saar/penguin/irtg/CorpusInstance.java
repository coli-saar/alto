/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class CorpusInstance implements Serializable {
    private Map<String,BottomUpAutomaton> decompositionAutomata;
    private BottomUpAutomaton parseChart;

    public CorpusInstance() {
        decompositionAutomata = new HashMap<String, BottomUpAutomaton>();
    }

    public void setParseChart(BottomUpAutomaton parseChart) {
        this.parseChart = parseChart;
    }
    
    public void putDecompositionAutomaton(String interpretation, BottomUpAutomaton auto) {
        decompositionAutomata.put(interpretation, auto);
    }

    public BottomUpAutomaton getParseChart() {
        return parseChart;
    }
    
    public BottomUpAutomaton getDecompositionAutomaton(String interpretation) {
        return decompositionAutomata.get(interpretation);
    }

}
