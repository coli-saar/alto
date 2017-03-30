/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;

/**
 * This class is used to predict the outside score of a state.
 * 
 * This is used in the agenda based intersection algorithms which attempt to find
 * the based tree based on an A* approach.
 * 
 * @author koller
 */
public interface EdgeEvaluator {
    /**
     * This method is called each time a rule is added to the output
     * automaton. Its standard implementation does nothing, but
     * subclasses might implement some other behavior, e.g. to keep
     * track of inside probabilities of states.
     * 
     * @param rule 
     */
    public default void ruleAdded(Rule rule) {
        
    }
    
    /**
     * This method is called each time the intersection algorithm
     * discovers a new state to put on the agenda. This is a state
     * of the intersection automaton, and thus it represents a pair
     * (p,q) of states from the original left and and right automata.
     * The evaluate method is supposed to return a numeric "evaluation"
     * of the new state, so it can be sorted into the right place in
     * the agenda (which is a priority queue, sorted by descending
     * evaluation values). You can use {@link IntersectionAutomaton#getLeftState(int) }
     * and {@link IntersectionAutomaton#getRightState(int) } with the
     * "auto" argument to obtain p and q.
     * 
     * @param outputState
     * @param auto
     * @return 
     */
    public abstract double evaluate(int outputState, IntersectionAutomaton auto);
}
