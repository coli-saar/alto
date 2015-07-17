/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import java.util.Map;

/**
 *
 * @author christoph_teichmann
 */
public class RuleTreeFinder {
    /**
     * 
     */
    private final Map<String,AlignmentAlgebra> algebras;

    /**
     * 
     */
    private final RuleTreeGenerator gen = new RuleTreeGenerator();
    
    /**
     * 
     * @param algebras 
     */
    public RuleTreeFinder(Map<String,AlignmentAlgebra> algebras) {
        this.algebras = algebras;
    }
    
    /**
     * 
     * @param inputPair
     * @param algebraUsed
     * @return 
     */
    public Pair<TreeAutomaton,AlignmentMapper> getRuleOptions(Pair<String,String> inputPair,
                                                                                String algebraUsed){
        return this.possiblyReweigh(algebraUsed, inputPair, false);
    }
    
    /**
     * 
     * @param inputPair
     * @param algebraUsed
     * @return 
     */
    public Pair<TreeAutomaton,AlignmentMapper> getRuleOptionsReweighted(Pair<String,String> inputPair,
                                                                                String algebraUsed){
        return possiblyReweigh(algebraUsed, inputPair, true);
    }

    /**
     * 
     * @param algebraUsed
     * @param inputPair
     * @param reweigh
     * @return 
     */
    private Pair<TreeAutomaton, AlignmentMapper> possiblyReweigh(String algebraUsed, Pair<String, String> inputPair, boolean reweigh) {
        AlignmentAlgebra alal = algebras.get(algebraUsed);
        
        Pair<RuleMarker, Pair<TreeAutomaton,TreeAutomaton>> alignments = alal.decomposePair(inputPair.getLeft(),
                inputPair.getRight());
        RuleMarker rlm = alignments.getLeft();
        TreeAutomaton ta1 = alignments.getRight().getLeft();
        TreeAutomaton ta2 = alignments.getRight().getRight();
        
        MarkingPropagator mp = new MarkingPropagator();
        ta1 = mp.introduce(ta1, rlm, 0);
        ta2 = mp.introduce(ta2, rlm, 1);
        
        Pair<TreeAutomaton,Pair<Homomorphism,Homomorphism>> result = gen.makeInverseIntersection(ta1, ta2, rlm);
        
        TreeAutomaton ta = result.getLeft();
        Homomorphism h1 = result.getRight().getLeft();
        Homomorphism h2 = result.getRight().getRight();
        AlignmentMapper am = new AlignmentMapper(h1, h2, rlm);
        
        if(reweigh){
            Iterable<Rule> it = ta.getRuleIterable();
            for(Rule r : it){
                String label = r.getLabel(ta);
                if(rlm.isFrontier(h1.get(label).getLabel()) || rlm.isFrontier(h2.get(label).getLabel())){
                    r.setWeight(1.1);
                }else{
                    r.setWeight(0.1);
                }
            }
        }
        return new Pair<>(result.getLeft(),am);
    }
}