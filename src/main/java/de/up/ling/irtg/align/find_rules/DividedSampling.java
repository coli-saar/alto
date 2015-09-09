/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.Propagator;
import de.up.ling.irtg.align.StateAlignmentMarking;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class DividedSampling {
    
    /**
     * 
     */
    private final int threads;
    
    
    public DividedSampling(int threads){
        this.threads = threads;
    }
    
    
    public InterpretedTreeAutomaton findRules(List<TreeAutomaton> left, List<TreeAutomaton> right,
                                            List<StateAlignmentMarking> leftAlign,
                                            List<StateAlignmentMarking> rightAlign, Signature leftSig,
                                            Signature rightSig, Propagator prop, SampleDriver drive){
        Signature shared = new Signature();
        List<HomomorphismManager> connections = new ArrayList<>();
        
        IntSet varsL = new IntOpenHashSet();
        IntSet varsR = new IntOpenHashSet();
        
        
        
        for(int i=0;i<left.size() && i<right.size();++i){
            TreeAutomaton t1 = left.get(i);
            TreeAutomaton t2 = right.get(i);
            
            Int2ObjectMap<IntSet> setL = prop.propagate(t1, leftAlign.get(i));
            Int2ObjectMap<IntSet> setR = prop.propagate(t2, rightAlign.get(i));
            
            dumpVars(varsL,setL, prop);
            dumpVars(varsR,setR, prop);
            
            HomomorphismManager hom = new HomomorphismManager(leftSig, rightSig, shared);
            
            hom.update(t1.getAllLabels(), t2.getAllLabels());
            hom.update(varsL, varsR);
            
            
        }
        
        
        //TODO
        return null;
    }

    private void dumpVars(IntSet varsL, Int2ObjectMap<IntSet> setL, Propagator prop) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}