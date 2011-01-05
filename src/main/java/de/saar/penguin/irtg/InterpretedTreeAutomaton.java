/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {
    private BottomUpAutomaton<String> automaton;
    private List<Interpretation> interpretations;

    public InterpretedTreeAutomaton(BottomUpAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new ArrayList<Interpretation>();
    }

    public void addInterpretation(Interpretation interp) {
        interpretations.add(interp);
    }

    public BottomUpAutomaton parse(List inputs) {
        BottomUpAutomaton ret = automaton;

        for( int i = 0; i < inputs.size(); i++ ) {
            Object inp = inputs.get(i);
            if( inp != null ) {
                ret = ret.intersect(interpretations.get(i).parse(inp));
            }
        }

        return ret;
    }
}
