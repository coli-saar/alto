/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.align.alignment_marking.SpanAligner;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class HillClimbing {
    
    /**
     * 
     */
    private final int threads;
    
    
    public HillClimbing(int threads){
        this.threads = threads;
    }
    
    
    public InterpretedTreeAutomaton findRules(List<TreeAutomaton> left, List<TreeAutomaton> right,
                                            List<StateAlignmentMarking> leftAlign,
                                            List<StateAlignmentMarking> rightAlign, Signature leftSig,
                                            Signature rightSig){
        
        
        
        //TODO
        return null;
    }
}