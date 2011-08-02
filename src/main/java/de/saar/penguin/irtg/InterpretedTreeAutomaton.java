/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.util.HashMap;
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

    public BottomUpAutomaton<String> getAutomaton() {
        return automaton;
    }

    public Map<String, Interpretation> getInterpretations() {
        return interpretations;
    }
    
    public Object parseString(String interpretation, String representation) {
        return getInterpretations().get(interpretation).getAlgebra().parseString(representation);
    }

    public BottomUpAutomaton parse(Map<String,Object> inputs) {
        BottomUpAutomaton ret = automaton;

        for( String interpName : inputs.keySet() ) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);
            ret = ret.intersect(interp.parse(input));
        }

        return ret;
    }
}
