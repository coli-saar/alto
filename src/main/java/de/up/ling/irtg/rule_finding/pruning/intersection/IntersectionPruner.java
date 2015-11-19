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
import de.up.ling.irtg.util.FunctionIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.function.Function;

/**
 *
 * @author christoph
 */
public class IntersectionPruner<X,Y> implements Pruner<X,Y,X,Pair<X,? extends Object>> {

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
    public Iterable<AlignedTrees<X>> prePrune(Iterable<AlignedTrees<X>> alignmentFree) {
        return alignmentFree;
    }

    @Override
    public Iterable<AlignedTrees<Pair<X, ? extends Object>>> postPrune(Iterable<AlignedTrees<X>> variablesPushed, Iterable<AlignedTrees<Y>> otherSide) {
        return new FunctionIterable<>(variablesPushed, (AlignedTrees<X> at) -> {
            TreeAutomaton base = at.getTrees();
            
            TreeAutomaton intersect = mapToIntersect.apply(base);
            TreeAutomaton<Pair<X, ? extends Object>> aut
                    = new IntersectionAutomaton<>(base,intersect);
            
            SpecifiedAligner spal = new SpecifiedAligner(aut);
            IntIterator iit = aut.getAllStates().iterator();
            while(iit.hasNext()){
                Pair<X,? extends Object> state = aut.getStateForId(iit.nextInt());
                
                spal.put(state, at.getAlignments().getAlignmentMarkers(state.getLeft()));
            }
            
            return new AlignedTrees<>(aut,spal);
        });
    }
}
