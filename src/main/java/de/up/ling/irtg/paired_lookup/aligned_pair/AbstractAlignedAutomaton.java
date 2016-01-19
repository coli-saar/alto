/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;

/**
 *
 * @author christoph
 * @param <X>
 */
public abstract class AbstractAlignedAutomaton<X> extends TreeAutomaton<X> implements PushedStateAlignments {
    /**
     * 
     * @param signature 
     */
    public AbstractAlignedAutomaton(Signature signature) {
        super(signature);
    }
}