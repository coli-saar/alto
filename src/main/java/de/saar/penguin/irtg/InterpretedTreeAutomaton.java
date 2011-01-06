/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {
    private BottomUpAutomaton<String> automaton;
    private Map<String,Interpretation> interpretations;

    public InterpretedTreeAutomaton(BottomUpAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new HashMap<String, Interpretation>();
    }

    public void addInterpretation(String name, Interpretation interp) {
        interpretations.put(name, interp);
    }

    public BottomUpAutomaton parse(Map<String,Object> inputs) {
        BottomUpAutomaton ret = automaton;

        for( String interpName : inputs.keySet() ) {
            ret = ret.intersect(interpretations.get(interpName).parse(inputs.get(interpName)));
        }

        return ret;
    }
}
