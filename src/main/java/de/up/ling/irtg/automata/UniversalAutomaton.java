/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import java.util.HashSet;
import java.util.Set;

/**
 * A tree automaton that accepts all terms over the given signature.
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
        Set<Rule> ret = new HashSet<>();
        ret.add(createRule(stateId, label, childStates, 1));
        return ret;
    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        Set<Rule> ret = new HashSet<>();
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
