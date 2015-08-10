/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 * @param <State>
 */
public interface Pruner<State> {
    /**
     * 
     * @param automaton
     * @return 
     */
    public TreeAutomaton<State> prune(TreeAutomaton<State> automaton);
}