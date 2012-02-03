/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.io.Serializable;

/**
 *
 * @author koller
 */
public class CorpusInstance implements Serializable {
    private BottomUpAutomaton parseChart;

    public CorpusInstance() {
    }

    public void setParseChart(BottomUpAutomaton parseChart) {
        this.parseChart = parseChart;
    }
    
    public BottomUpAutomaton getParseChart() {
        return parseChart;
    }
    
}
