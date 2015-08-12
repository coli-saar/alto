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
     * @param stateMarkers
     * @return 
     */
    public TreeAutomaton<State> prePrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers);
    
    
    /**
     * 
     * @param automaton
     * @param stateMarkers
     * @return 
     */
    public TreeAutomaton<State> postPrune(TreeAutomaton<State> automaton, StateAlignmentMarking<State> stateMarkers);
    
    /**
     * 
     */
    public Pruner DEFAULT_PRUNER = new Pruner(){

        @Override
        public TreeAutomaton postPrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
            return automaton;
        }

        @Override
        public TreeAutomaton prePrune(TreeAutomaton automaton, StateAlignmentMarking stateMarkers) {
            return automaton;
        }

    };
}