/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import de.saar.penguin.irtg.signature.Signature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class UniversalAutomaton extends TreeAutomaton<String> {
    public static final String STATE = "q";
    
    public UniversalAutomaton(Signature signature) {
        super(signature);
        
        finalStates.add(STATE);
        allStates.add(STATE);
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(String label, List<String> childStates) {
        Set<Rule<String>> ret = new HashSet<Rule<String>>();
        ret.add(new Rule(STATE, label, childStates));
        return ret;
    }

    @Override
    public Set<Rule<String>> getRulesTopDown(String label, String parentState) {
        Set<Rule<String>> ret = new HashSet<Rule<String>>();
        List<String> childStates = new ArrayList<String>();
        for( int i = 0; i < signature.getArity(label); i++ ) {
            childStates.add(STATE);
        }
        
        ret.add(new Rule(STATE, label, childStates));
        
        return ret;
    }
    
}
