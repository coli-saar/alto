/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.up.ling.irtg.automata.Rule;
import java.util.stream.Stream;

/**
 *
 * @author christoph
 */
public interface PushedStateAlignments {
    /**
     * 
     * @param state
     * @return 
     */
    public AlignmentContainer getAlignments(int state);
    
    /**
     * 
     * @param mainStorage
     * @param alignwith
     * @return 
     */
    public Stream<Rule> getFittingRules(PushedStateAlignments mainStorage, Rule alignwith);
}
