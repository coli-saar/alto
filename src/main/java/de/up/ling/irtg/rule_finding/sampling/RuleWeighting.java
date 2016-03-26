/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph
 */
public interface RuleWeighting {
    /**
     * 
     * @param candidate
     * @param automaton
     * @return 
     */
    public double getProbability(Rule candidate, TreeAutomaton automaton);
    
    /**
     * 
     * @param state
     * @param automaton
     */
    public void prepareProbability(int state, TreeAutomaton automaton);
    
    /**
     * 
     * @param state
     * @param automaton
     * @return 
     */
    public double getStateStartProbability(int state, TreeAutomaton automaton);
    
    /**
     * 
     * @param automaton 
     */
    public void prepareStateStartProbability(TreeAutomaton automaton);
    
    /**
     * 
     */
    public void reset();
}
