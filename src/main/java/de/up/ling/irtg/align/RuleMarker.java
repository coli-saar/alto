/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * This interface is used to connect rules in two different automata via alignments.
 * 
 * It also manages the way these connections are translated into labels and then making sure
 * that these labels are manages the correct way.
 * 
 * @author christoph
 */
public interface RuleMarker {
    
    /**
     * Returns a rule evaluator for automaton number num (0 or 1).
     * 
     * The rule evaluator should be such that the collected alingments for a state
     * should be unique for all possible derivations from that state.
     * 
     * @param num Indicates which of the two automata this RuleMarker is aware of is used can be 0 or 1.
     * @return 
     */
    public RuleEvaluator<IntSet> ruleMarkings(int num);

    /**
     * Returns a  set of ints that indicate which rules are aligned two rules are aligned, if they share the
     * same marker.
     * 
     * No marker should be used twice.
     * 
     * @param ta
     * @param r
     * @return 
     */
    public IntSet getMarkings(int num, Rule r);
    
    /**
     * Returns a label that encodes the given alignments.
     * 
     * @param alignments
     * @param original
     * @param state
     * @return 
     */
    public String makeCode(IntSet alignments, TreeAutomaton original, int state);
    
    /**
     * Returns true if the two alignment sets are compatible with each other.
     * 
     * @param label1
     * @param label2
     * @return 
     */
    public boolean checkCompatible(String label1, String label2);

    /**
     * Returns true if the given label corresponds to an alignment label as this
     * instances generates them.
     * 
     * @param label
     * @return 
     */
    public boolean isFrontier(String label);

    /**
     * Returns the alignment label that should be aligned to the given one, usually
     * this will be the same label.
     * 
     * @param variable
     * @return 
     */
    public String getCorresponding(String variable);
}