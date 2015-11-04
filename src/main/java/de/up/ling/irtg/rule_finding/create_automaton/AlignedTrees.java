/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 */
public class AlignedTrees<Type> {
    /**
     * 
     */
    private final TreeAutomaton<Type> trees;
    
    /**
     * 
     */
    private final StateAlignmentMarking<Type> alignments;

    /**
     * 
     * @param trees
     * @param alignments 
     */
    public AlignedTrees(TreeAutomaton<Type> trees, StateAlignmentMarking<Type> alignments) {
        this.trees = trees;
        this.alignments = alignments;
    }

    /**
     * 
     * @return 
     */
    public TreeAutomaton<Type> getTrees() {
        return trees;
    }

    /**
     * 
     * @return 
     */
    public StateAlignmentMarking<Type> getAlignments() {
        return alignments;
    }
}
