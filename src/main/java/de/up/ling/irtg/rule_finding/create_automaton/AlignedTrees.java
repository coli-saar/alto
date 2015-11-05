/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.automata.TreeAutomaton;

/**
 * Combines the trees in a tree automaton with their alignments.
 * 
 * It is often necessary to keep track of tree automata and the alignments
 * associated with them at the same time. This class simplifies that task
 * without forcing long constructions like Pair<TreeAutomaton,StateAlignmentMarking>.
 * 
 * @author christoph_teichmann
 * @param <Type>
 */
public class AlignedTrees<Type> {
    /**
     * The trees currently in use.
     */
    private final TreeAutomaton<Type> trees;
    
    /**
     * The alignments for the trees.
     */
    private final StateAlignmentMarking<Type> alignments;

    /**
     * Create a new instance that contains the given tree automaton and the 
     * given alignments.
     * 
     * No defensive copy is made.
     * 
     * @param trees
     * @param alignments 
     */
    public AlignedTrees(TreeAutomaton<Type> trees, StateAlignmentMarking<Type> alignments) {
        this.trees = trees;
        this.alignments = alignments;
    }

    /**
     * Returns the tree automaton contained in this instance.
     * 
     * @return 
     */
    public TreeAutomaton<Type> getTrees() {
        return trees;
    }

    /**
     * Returns the alignments contained in this instance.
     * 
     * @return 
     */
    public StateAlignmentMarking<Type> getAlignments() {
        return alignments;
    }

    @Override
    public String toString() {
        return "AlignedTrees{" + "trees=" + trees + ", alignments=" + alignments + '}';
    }
}
