/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;

/**
 *
 * @author johannes
 */
public class ConcreteCondensedTreeAutomaton<State> extends CondensedTreeAutomaton<State>{
    
    public ConcreteCondensedTreeAutomaton() {
        super(new Signature());
    }
    
    public void addRule(CondensedRule rule) {
        storeRule(rule);
    }
    
    @Override
    public void addFinalState(int state) {
        super.addFinalState(state); 
    }

    @Override
    public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
        return ruleTrie.get(childStates, labelId);
    }

    @Override
    public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
