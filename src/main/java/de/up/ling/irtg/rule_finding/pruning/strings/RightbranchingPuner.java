/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.strings;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph
 */
public class RightbranchingPuner implements Pruner<Object,Object> {

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
            
            RightBranchingNormalForm rbnf = new RightBranchingNormalForm(base.getSignature(), base.getAllLabels());
            TreeAutomaton<Pair<? extends Object, ? extends Object>> aut
                    = new IntersectionAutomaton<>(base,rbnf);
            
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
