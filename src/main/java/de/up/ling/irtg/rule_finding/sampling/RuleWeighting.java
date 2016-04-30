/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph
 */
public interface RuleWeighting {
    /**
     * 
     * @param state
     * @param number
     * @return 
     */
    public double getLogProbability(int state, int number);
    
    /**
     * 
     * @param state
     */
    public void prepareProbability(int state);
    
    /**
     * 
     * @param state
     * @param choicePoint
     * @return 
     */
    public int getRuleNumber(int state, double choicePoint);
    
    /**
     * 
     * @param state
     * @param number
     * @return 
     */
    public Rule getRuleByNumber(int state, int number);
    
    
    /**
     * 
     * @param position
     * @return 
     */
    public double getStateStartLogProbability(int position);

    /**
     * 
     * @param numbr
     * @return 
     */
    public int getStartStateByNumber(int number);
    
    /**
     * 
     * @param choicePoint
     * @return 
     */
    public int getStartStateNumber(double choicePoint);
    
    /**
     * 
     */
    public void prepareStartProbability();

    /**
     * 
     * @return 
     */
    public int getNumberOfStartStates();
    
    /**
     * 
     */
    public void reset();
    
    /**
     * 
     * @param treSamp
     * @param deterministic 
     */
    public void adapt(TreeSample<Rule> treSamp, boolean deterministic);
    
    /**
     * 
     * @return 
     */
    public TreeAutomaton getAutomaton();
    
    /**
     * 
     * @param sample
     * @return 
     */
    public double getLogTargetProbability(Tree<Rule> sample);

    public double getLogProbability(Rule r);
}
