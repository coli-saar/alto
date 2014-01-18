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
        isExplicit = true;
    }
    
    @Override
    public int addState(State state) {
        return super.addState(state); 
    }
    
    @Override
    public void addFinalState(int state) {
        super.addFinalState(state); 
    }
        
    public void addRule(CondensedRule rule) {
        storeRule(rule);
    }
    
    @Override
    public Set<CondensedRule> getCondensedRulesBottomUp(IntSet labelId, int[] childStates) {
        return getCondensedRuleBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Set<CondensedRule> getCondensedRulesTopDown(IntSet labelId, int parentState) {
        return getCondensedRulesTopDownFromExplicit(labelId, parentState);
    }
    
}
