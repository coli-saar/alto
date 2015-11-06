/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.create_automaton.StateAlignmentMarking;
import de.up.ling.irtg.semiring.Semiring;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;


public class FrequencyPruner<State1, State2> implements Pruner<State1, State2> {
    /**
     * 
     */
    private final Mode when;

    /**
     * 
     */
    private final int maxTransitions;

    /**
     * 
     * @param when
     * @param maxTransitions 
     */
    public FrequencyPruner(Mode when, int maxTransitions) {
        this.when = when;
        this.maxTransitions = maxTransitions;
    }
    
    @Override
    public List<AlignedTrees<State1>> prePrune(List<AlignedTrees<State1>> alignmentFree) {
        if(when == Mode.POST){
            return alignmentFree;
        }
        
        Object2DoubleMap<String> counts = this.getVariableCounts(alignmentFree, null);
        return reduce(alignmentFree,counts);
    }

    @Override
    public List<AlignedTrees<State1>> postPrune(List<AlignedTrees<State1>> toPrune, List<AlignedTrees<State2>> otherSide) {
        if(when == Mode.PRE){
            return toPrune;
        }
        Object2DoubleMap<String> counts = this.getVariableCounts(toPrune, otherSide);
        return reduce(toPrune, counts);
    }
    
    /**
     * 
     * @param toPrune
     * @param otherSide
     * @return 
     */
    public  Object2DoubleMap<String> getVariableCounts(List<AlignedTrees<State1>> toPrune, List<AlignedTrees<State2>> otherSide){
        Object2DoubleOpenHashMap<String> counts = new Object2DoubleOpenHashMap<>();
        
        for(int i=0;i<toPrune.size();++i){
            AlignedTrees<State1> tP = toPrune.get(i);
            StateAlignmentMarking<State2> there = otherSide == null ? null : otherSide.get(i).getAlignments();
            TreeAutomaton<State1> trees = tP.getTrees();
            StateAlignmentMarking<State1> here = tP.getAlignments();
            
            Iterable<Rule> its = trees.getAllRulesTopDown();
            for(Rule r : its){
                String s = trees.getSignature().resolveSymbolId(r.getLabel());
                
                if(there != null){
                    IntSet ins = here.getAlignmentMarkers(trees.getStateForId(r.getParent()));
                    
                    if(!there.containsVarSet(ins)){
                        continue;
                    }
                }
                
                counts.addTo(s, 1);
            }
        }
        
        return counts;
    }

    /**
     * 
     * @param alignmentFree
     * @param counts
     * @return 
     */
    private List<AlignedTrees<State1>> reduce(List<AlignedTrees<State1>> alignmentFree, Object2DoubleMap<String> counts) {
        List<AlignedTrees<State1>> result = new ArrayList<>();
        
        Object2DoubleMap<Rule> weights = new Object2DoubleOpenHashMap<>();
        List<Rule> iter = new ArrayList<>();
        PriorityQueue<Rule> queue = new PriorityQueue<>((Rule r1, Rule r2) -> {
            double d1 = weights.get(r1);
            double d2 = weights.get(r2);
            
            return Double.compare(d1, d2);
        });
        
        for(int i=0;i<alignmentFree.size();++i){
            AlignedTrees<State1> org = alignmentFree.get(i);
            TreeAutomaton<State1> oTa = org.getTrees();
            
            RuleEvaluator<Double> re = (Rule r) -> {
              String label = oTa.getSignature().resolveSymbolId(r.getLabel());
              double count = counts.get(label);
              
              return Math.log(count);
            };
            
            Map<Integer,Double> inside = oTa.evaluateInSemiring(maxAdd, re);
            
            Function<Iterable<Rule>,Iterable<Rule>> selector = (Iterable<Rule> rules) -> {
                iter.clear();
                weights.clear();
                queue.clear();
                
                for(Rule rule : rules){
                    String label = oTa.getSignature().resolveSymbolId(rule.getLabel());
                    if(Variables.IS_VARIABLE.test(label)){
                        iter.add(rule);
                    }
                    
                    double sum = 0.0;
                    for(int child : rule.getChildren()){
                        sum += inside.get(child);
                    }
                    weights.put(rule, sum);
                    queue.add(rule);
                    if(queue.size() > maxTransitions){
                        queue.poll();
                    }
                }
                
                while(!queue.isEmpty()){
                    iter.add(queue.poll());
                }
                
                return iter;
            };
            
            result.add(SelectRules.select(org, selector));
        }
        
        return result;
    }
    
    /**
     * 
     */
    public Semiring<Double> maxAdd = new Semiring<Double>() {

        @Override
        public Double add(Double x, Double y) {
            return Math.max(x, y);
        }

        @Override
        public Double multiply(Double x, Double y) {
            return x+y;
        }

        @Override
        public Double zero() {
            return Double.NEGATIVE_INFINITY;
        }
    };
    
    /**
     * 
     */
    public enum Mode{
        PRE,
        POST,
        BOTH;
    }
}
