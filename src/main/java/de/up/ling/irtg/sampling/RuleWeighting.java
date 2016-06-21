/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 * This interface represents the target distribution for sampling and the auxiliary
 * distribution.
 * 
 * All probability are only computed when the implementing class is told that they
 * will be needed. The interface imposes some order on the start states and the
 * rules for a state. This makes it possible to address them both by numbers.
 * 
 * @author christoph
 */
public interface RuleWeighting {
    /**
     * Returns the log of the probability of choosing the rule identified by the number given the state.
     * 
     * The value should be those computed by the last prepareProbability call.
     * 
     * @param state
     * @param number
     * @return 
     */
    public double getLogProbability(int state, int number);
    
    /**
     * Tells the class to recompute the proposal probabilities for the rules of the given state.
     * 
     * If it is certain that they have not changed, then it is acceptable to do nothing.
     * 
     * @param state
     */
    public void prepareProbability(int state);
    
    /**
     * Returns the number of the first rule such that the cumulative probability of
     * the earlier rules plus this one is larger than choicePoint.
     * 
     * May throw an error if there is no such rule.
     * 
     * @param state
     * @param choicePoint
     * @return 
     */
    public int getRuleNumber(int state, double choicePoint);
    
    /**
     * Returns the rule identified by the given state and number.
     * 
     * May throw an error if there is no such rule.
     * 
     * @param state
     * @param number
     * @return 
     */
    public Rule getRuleByNumber(int state, int number);
    
    
    /**
     * Returns the log of the proposal probability of the given start state.
     * 
     * May throw an error if there is no such state. The value is always the
     * last one computed by prepareStartProbability.
     * 
     * @param number
     * @return 
     */
    public double getStateStartLogProbability(int number);

    /**
     * Returns the start state with the given number.
     * 
     * May throw an error if there is no such state.
     * 
     * @param number
     * @return 
     */
    public int getStartStateByNumber(int number);
    
    /**
     * Returns the start first start state such that the cumulative probability
     * of earlier start states plus this one is larger than choicePoint.
     * 
     * May throw an error if there is no such state.
     * 
     * @param choicePoint
     * @return 
     */
    public int getStartStateNumber(double choicePoint);
    
    /**
     * Recomputes the proposal probabilities for the start states.
     * 
     * If they are guaranteed to be unchanged, then this step can be skipped
     * by the implementing class.
     */
    public void prepareStartProbability();

    /**
     * Returns the overall number of start states available.
     * 
     * @return 
     */
    public int getNumberOfStartStates();
    
    /**
     * Resets any adaption of the proposal distribution.
     */
    public void reset();
    
    /**
     * Adapts the proposal distribution with the assumption that
     * treSamp is an importance sample generated from this proposal distribution.
     * 
     * 
     * @param treSamp
     * @param deterministic  indicates whether we can assume the underlying
     * automaton to be unambiguous.
     */
    public void adapt(TreeSample<Rule> treSamp, boolean deterministic);
    
    /**
     * Returns the underlying tree automaton from which the rules and start
     * states are drawn.
     * 
     * @return 
     */
    public TreeAutomaton getAutomaton();
    
    /**
     * Returns the unnormalized probability of the given tree in the target
     * distribution.
     * 
     * @param sample
     * @return 
     */
    public double getLogTargetProbability(Tree<Rule> sample);

    /**
     * Returns the proposal probability of the given rule given its parent.
     * 
     * @param r
     * @return 
     */
    public double getLogProbability(Rule r);
}
