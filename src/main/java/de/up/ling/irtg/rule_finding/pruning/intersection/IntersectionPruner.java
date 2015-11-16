/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph
 */
public class IntersectionPruner implements Pruner<Object,Object> {

    /**
     * 
     */
    private final Function<TreeAutomaton,TreeAutomaton> mapToIntersect;

    /**
     * 
     * @param mapToIntersect 
     */
    public IntersectionPruner(Function<TreeAutomaton, TreeAutomaton> mapToIntersect) {
        this.mapToIntersect = mapToIntersect;
    }
    
    @Override
    public List<AlignedTrees<Object>> prePrune(List<AlignedTrees<Object>> alignmentFree) {
        return alignmentFree;
    }

    @Override
    public List<AlignedTrees<Object>> postPrune(List<AlignedTrees<Object>> variablesPushed, List<AlignedTrees<Object>> otherSide) {
        List<AlignedTrees<Object>> result = new ArrayList<>();
        
        for(int i=0;i<variablesPushed.size();++i){
            AlignedTrees at = variablesPushed.get(i);
            TreeAutomaton base = at.getTrees();
            
            TreeAutomaton intersect = this.mapToIntersect.apply(base);
            TreeAutomaton<Pair<? extends Object, ? extends Object>> aut
                    = new IntersectionAutomaton<>(base,intersect);
            
            SpecifiedAligner spal = new SpecifiedAligner(aut);
            IntIterator iit = aut.getAllStates().iterator();
            while(iit.hasNext()){
                Pair<Object,RightBranchingNormalForm.State> state = (Pair<Object,RightBranchingNormalForm.State>) aut.getStateForId(iit.nextInt());
                
                spal.put(state, at.getAlignments().getAlignmentMarkers(state.getLeft()));
            }
            
            result.add(new AlignedTrees<>(aut,spal));
        }
        return result;
    }
}
