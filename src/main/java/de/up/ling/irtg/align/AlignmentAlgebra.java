/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 * This interface implements an algebra that is able to take two inputs and turn them into
 * two automata with alignments.
 * @author christoph_teichmann
 */
public abstract interface AlignmentAlgebra {
    /**
     * Returns a pair of decomposition automata for the input encoding + an Rule marker for any alignments
     * that may have been discovered.
     * 
     * @param one
     * @param two
     * @return 
     */
    public abstract Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> decomposePair(String one, String two);
}