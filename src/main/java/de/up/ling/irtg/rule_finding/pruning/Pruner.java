/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;

/**
 *
 * @author christoph_teichmann
 * @param <State1>
 * @param <State2>
 * @param <State3>
 * @param <State4>
 */
public interface Pruner<State1,State2,State3,State4> {
    
    /**
     * @param alignmentFree
     * 
     * @return 
     */
    public Iterable<AlignedTrees<State3>> prePrune(Iterable<AlignedTrees<State1>> alignmentFree);
    
    
    /**
     * 
     * @param variablesPushed
     * @param otherSide
     * @return 
     */
    public Iterable<AlignedTrees<State4>> postPrune(
            Iterable<AlignedTrees<State1>> variablesPushed, Iterable<AlignedTrees<State2>> otherSide);
    
    /**
     * 
     */
    public Pruner DEFAULT_PRUNER = new Pruner() {

        @Override
        public Iterable prePrune(Iterable alignmentFree) {
            return alignmentFree;
        }

        @Override
        public Iterable postPrune(Iterable variablesPushed, Iterable otherSide) {
            return variablesPushed;
        }
    };
}