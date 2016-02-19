/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 * 
 * @author christoph_teichmann
 */
public interface Pruner {
    /**
     * 
     * @param ta
     * @return 
     */
    public TreeAutomaton apply(TreeAutomaton<String> ta);
    
    /**
     * 
     */
    public Pruner DEFAULT_PRUNER = (TreeAutomaton<String> ta) -> ta;
}