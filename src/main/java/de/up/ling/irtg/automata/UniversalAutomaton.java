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
    private int stateId;
    
    public UniversalAutomaton(Signature signature) {
        super(signature);
        
        stateId = addState(STATE);
        finalStates.add(stateId);        
    }

    @Override
    public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
        Set<Rule> ret = new HashSet<Rule>();
        ret.add(createRule(stateId, label, childStates, 1));
        return ret;
    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        Set<Rule> ret = new HashSet<Rule>();
        int[] childStates = new int[signature.getArity(label)];
        
        for( int i = 0; i < signature.getArity(label); i++ ) {
            childStates[i] = stateId;
        }
        
        ret.add(createRule(stateId, label, childStates, 1));
        
        return ret;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return true;
    }
    
}
