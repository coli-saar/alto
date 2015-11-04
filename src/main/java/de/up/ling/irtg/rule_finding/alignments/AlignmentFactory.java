/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.alignments;

import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 */
public interface AlignmentFactory<Type> {
    /**
     * 
     * @param <Type>
     * @param alignments
     * @return 
     */
    public StateAlignmentMarking<Type> makeInstance(String alignments, TreeAutomaton<Type> input);
}
