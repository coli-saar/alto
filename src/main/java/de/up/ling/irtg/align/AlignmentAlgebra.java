/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public abstract interface AlignmentAlgebra<Type> {
    /**
     * 
     * @param one
     * @param two
     * @return 
     */
    public abstract Pair<RuleMarker,Pair<TreeAutomaton,TreeAutomaton>> decompose(String one, String two);
}