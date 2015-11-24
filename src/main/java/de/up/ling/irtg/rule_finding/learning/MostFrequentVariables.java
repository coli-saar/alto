/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    public Tree<String> getBestAnalysis(final TreeAutomaton data, Object2DoubleMap<String> counts){
        final Int2DoubleMap score = new Int2DoubleAVLTreeMap();
        final Int2ObjectMap<Rule> expandChoice = new Int2ObjectAVLTreeMap<>();
        
        TreeAutomaton.BottomUpStateVisitor visitor = (int state, Iterable<Rule> rulesTopDown) -> {
            
            TrackingConsumer con = new TrackingConsumer(data, score, counts);
            rulesTopDown.forEach(con);
            
            expandChoice.put(state, con.bestRule);
            score.put(state, con.bestScore);
        };
        
        data.foreachStateInBottomUpOrder(visitor);
        IntIterator iit = data.getFinalStates().iterator();
        
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestFinal = -1;
        
        while(iit.hasNext()){
            int state = iit.nextInt();
            double val = score.get(state);
            
            if(val > bestScore){
                bestScore = val;
                bestFinal = state;
            }
        }
        
        return findTree(bestFinal,expandChoice, data.getSignature());
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
     * @param expandChoice
     * @param varChoice
     * @param signature
     * @return 
     */
    private Tree<String> findTree(int state,
            Int2ObjectMap<Rule> expandChoice,
            Signature signature) {
        Rule ex = expandChoice.get(state);
        
        String label = signature.resolveSymbolId(ex.getLabel());
        List<Tree<String>> children = new ArrayList<>();
        for(int child : ex.getChildren()){
            children.add(findTree(child, expandChoice, signature));
        }
        
        return Tree.create(label, children);
    }

    /**
     *
     */
    private class TrackingConsumer implements Consumer<Rule> {

        private final TreeAutomaton data;
        private final Int2DoubleMap score;
        private final Object2DoubleMap<String> counts;

        private Rule bestRule = null;
        private double bestScore = Double.NEGATIVE_INFINITY;

        /**
         *
         * @param data
         * @param score
         * @param counts
         */
        public TrackingConsumer(TreeAutomaton data, Int2DoubleMap score, Object2DoubleMap<String> counts) {
            this.data = data;
            this.score = score;
            this.counts = counts;
        }

        @Override
        public void accept(Rule rule) {
            String label = data.getSignature().resolveSymbolId(rule.getLabel());

            double value = 0.0;
            for (int child : rule.getChildren()) {
                value += score.get(child);
            }

            if (Variables.IS_VARIABLE.test(label)) {
                value += counts.get(label);
            }

            if (value > bestScore) {
                bestRule = rule;
                bestScore = value;
            }
        }
    }
}
