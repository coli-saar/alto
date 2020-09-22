/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.index;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public abstract class BottomUpRuleIndex {
    public abstract void setRules(Collection<Rule> rules, int labelId, int[] childStates);
    public abstract Collection<Rule> get(int labelId, int[] childStates);
    
    public abstract Iterable<Rule> getAllRules();
    
    /**
     * Returns false if adding this rule makes the automaton bottom-up
     * nondeterministic. That is: when q → f(q1,...,qn) is added, the method
     * returns true; when subsequently q' → f(q1,...,qn) is added, with q !=
     * q', the method returns false. (However, adding q → f(q1,...,qn) for a
     * second time does not actually change the set of rules; in this case, the
     * method returns true.)
     *
     */
    public boolean add(Rule rule) {
        Collection<Rule> rulesHere = get(rule.getLabel(), rule.getChildren());
        boolean ret = true;
        
        if( rulesHere != null ) {
            if( rulesHere.size() > 1 || ! rulesHere.contains(rule)) {
                // check if rulesHere contains an element other than the new rule
                ret = false;
            }
        }
        
        if( rulesHere == null ) {
            rulesHere = new HashSet<>();
            setRules(rulesHere, rule.getLabel(), rule.getChildren());
        }        
        
        rulesHere.add(rule);
        
        return ret;
    }
    
    public void printStatistics() {
        
    }
    
//    public Collection<Rule> getRulesLike(Rule likeRule) {
//        return get(likeRule.getLabel(), likeRule.getChildren());
//    }

//    public abstract void foreachValueForKeySets(List<IntSet> keySets, Consumer<Rule> fn);

    public abstract void foreachRuleForSets(IntSet labelIds, List<IntSet> childStateSets, SignatureMapper signatureMapper, Consumer<Rule> fn);
}
