/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
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
        addState(STATE);
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(int label, List<String> childStates) {
        Set<Rule<String>> ret = new HashSet<Rule<String>>();
        ret.add(createRule(STATE, label, childStates, 1));
        return ret;
    }

    @Override
    public Set<Rule<String>> getRulesTopDown(int label, String parentState) {
        Set<Rule<String>> ret = new HashSet<Rule<String>>();
        List<String> childStates = new ArrayList<String>();
        for( int i = 0; i < signature.getArity(label); i++ ) {
            childStates.add(STATE);
        }
        
        ret.add(createRule(STATE, label, (String[]) childStates.toArray(), 1));
        
        return ret;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return true;
    }
    
}
