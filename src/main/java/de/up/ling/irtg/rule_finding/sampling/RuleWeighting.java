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
     * @param candidate
     * @return 
     */
    public double getLogProbability(Rule candidate);
    
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
    public Rule getRule(int state, double choicePoint);
    
    /**
     * 
     * @param state
     * @return 
     */
    public double getStateStartLogProbability(int state);
    
    /**
     * 
     */
    public void prepareStateStartProbability();
    
    /**
     * 
     */
    public void reset();
    
    /**
     * 
     * @param treSamp 
     */
    public void adaptNormalized(TreeSample<Rule> treSamp);
    
    /**
     * 
     * @param treSamp 
     */
    public void adaptUnNormalized(TreeSample<Rule> treSamp);
    
    /**
     * 
     * @return 
     */
    public TreeAutomaton getAutomaton();

    /**
     * 
     * @param choicePoint
     * @return 
     */
    public int getStartState(double choicePoint);
    
    /**
     * 
     * @param sample
     * @return 
     */
    public double getLogTargetProbability(Tree<Rule> sample);
    
    /**
     * 
     * @return 
     */
    public boolean adaptsNormalized();

    public int getNumberOfStartStates();

    public int getStartStateByNumber(int i);

    public double getStateStartLogProbabilityByPosition(int i);
}
