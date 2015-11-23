/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class MostFrequentVariables {
    
    /**
     * 
     * @param it
     * @return 
     */
    public Iterable<Tree<String>> getOptimalChoices(Iterable<TreeAutomaton> it){
        Object2DoubleMap<String> counts = this.countVariablesTopDown(it);
        
        return new FunctionIterable<>(it,(TreeAutomaton ta) -> {
            return getBestAnalysis(ta, counts);
        });
    }
    
    /**
     * 
     * @param data
     * @param counts
     * @return 
     */
    public Tree<String> getBestAnalysis(TreeAutomaton data, Object2DoubleMap<String> counts){
        CountWeight cw = new CountWeight(counts, data.getSignature());
        SumViterbiWithBackPointers ring = new SumViterbiWithBackPointers();
        
        Int2ObjectMap<Pair<Double,Rule>> result = data.evaluateInSemiring(ring, cw);
        
        IntIterator finals = data.getFinalStates().iterator();
        
        int best = -1;
        double max = Double.NEGATIVE_INFINITY;
        while(finals.hasNext()){
            int state = finals.nextInt();
            
            Pair<Double,Rule> p = result.get(state);
            if(p.left > max){
                max = p.left;
                best = p.right.getParent();
            }
        }
        
        return findTree(best,result, data.getSignature());
    }
    
    /**
     * 
     * @param data
     * @return 
     */
    public Object2DoubleMap<String> countVariablesTopDown(Iterable<TreeAutomaton> data) {
        Object2DoubleOpenHashMap<String> result = new Object2DoubleOpenHashMap<>();
        
        IntArrayList toDo = new IntArrayList();
        IntSet seen = new IntOpenHashSet();
        
        data.forEach((TreeAutomaton ta) -> {
            toDo.clear();
            seen.clear();
            
            toDo.addAll(ta.getFinalStates());
            seen.addAll(ta.getFinalStates());
            
            for(int i=0;i<toDo.size();++i){
                int parent = toDo.getInt(i);
                Iterable<Rule> rules = ta.getRulesTopDown(parent);
                
                rules.forEach((Rule rule) -> {
                    String label = ta.getSignature().resolveSymbolId(rule.getLabel());
                    if(Variables.IS_VARIABLE.test(label)){
                        result.addTo(label, 1);
                    }
                    
                    for(int child : rule.getChildren()){
                        if(!seen.contains(child)){
                            seen.add(child);
                            toDo.add(child);
                        }
                    }
                });
            }
        });
        
        return result;
    }

    /**
     * 
     * @param state
     * @param result
     * @return 
     */
    private Tree<String> findTree(int state, Int2ObjectMap<Pair<Double, Rule>> result, Signature s) {
        Rule r = result.get(state).getRight();
        
        String label = s.resolveSymbolId(r.getLabel());
        List<Tree<String>> children = new ArrayList<>();
        
        for(int child : r.getChildren()){
            children.add(this.findTree(child, result, s));
        }
        
        return Tree.create(label, children);
    }
    
    
    /**
     * 
     */
    public static class CountWeight implements RuleEvaluator<Pair<Double,Rule>> {
        /**
         * 
         */
        private final Object2DoubleMap<String> counts;

        /**
         * 
         */
        private final Signature sig;

        /**
         * 
         * @param counts
         * @param sig 
         */
        public CountWeight(Object2DoubleMap<String> counts, Signature sig) {
            this.counts = counts;
            this.sig = sig;
        }
        
        @Override
        public Pair<Double, Rule> evaluateRule(Rule rule) {
            String label = this.sig.resolveSymbolId(rule.getLabel());
            
            double d = this.counts.getDouble(label);
            
            return new Pair<>(d,rule);
        }

        
        
    }
    
    /**
     * 
     */
    public static class SumViterbiWithBackPointers implements Semiring<Pair<Double,Rule>> {
        /**
         * 
         */
        private final static Pair<Double,Rule> ZERO = new Pair<>(Double.NEGATIVE_INFINITY, null);
        
        
        @Override
        public Pair<Double, Rule> add(Pair<Double, Rule> x, Pair<Double, Rule> y) {
            if(y.getLeft()  > x.getLeft()){
                return y;
            }else{
                return x;
            }
        }

        @Override
        public Pair<Double, Rule> multiply(Pair<Double, Rule> x, Pair<Double, Rule> y) {
            if (x.left == Double.NEGATIVE_INFINITY || y.left == Double.NEGATIVE_INFINITY) {
                // ensure that zero * x = x * zero = zero;
                // otherwise could get zero * zero = +Infinity
                return new Pair<>(Double.NEGATIVE_INFINITY, x.right);
            } else {
                return new Pair<>(x.left + y.left, x.right);
            }
        }

        @Override
        public Pair<Double, Rule> zero() {
            return ZERO;
        }
    }
}
