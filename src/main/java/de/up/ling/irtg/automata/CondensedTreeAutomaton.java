/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public class CondensedTreeAutomaton<State> {  // TODO Extend TreeAutomaton
    protected Interner<State> stateInterner;
    protected IntSet allStates;                                                 // subset of stateInterner.keySet() that actually occurs in this automaton; allows for sharing interners across automata to preserve state IDs
    protected Signature signature;

    
    public CondensedTreeAutomaton(Signature signature) {
        this.signature = signature;
        allStates = new IntArraySet();
        stateInterner = new Interner<State>();

    }
    
    protected Rule createRule(int parent, int label, int[] children, double weight) {
        return new Rule(parent, label, children, weight);
    }
    
    /**
     * Creates a weighted rule for this automaton. If the terminal symbol in the
     * rule is not already known in the automaton's signature, it is added to
     * the signature using the number of children as the arity.
     *
     * @param parent the rule's parent state
     * @param label the terminal symbol used in the rule
     * @param children the child states, from left to right (as an array)
     * @param weight the rule weight
     * @return
     */
    public Rule createRule(State parent, String label, State[] children, double weight) {
//        System.err.println("createrule: " + parent + "/" + label + "/" + StringTools.join(children, ","));
        Rule ret = createRule (
                addState(parent), 
                signature.addSymbol(label, children.length),                 addStates(children), weight);
//        System.err.println("  -> " + ret);
        return ret;
    }
    
    protected int addState(State state) {
        int ret = stateInterner.addObject(state);
        allStates.add(ret);
        return ret;
    }
    
    private int[] addStates(State[] states) {
        int[] ret = new int[states.length];

        for (int i = 0; i < states.length; i++) {
            ret[i] = addState(states[i]);
        }

        return ret;
    }
}