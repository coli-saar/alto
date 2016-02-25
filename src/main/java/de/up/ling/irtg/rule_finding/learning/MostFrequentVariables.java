/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 *
 * @author christoph_teichmann
 */
public class MostFrequentVariables implements TreeExtractor {    
    /**
     * 
     * @param it
     * @return 
     */
    @Override
    public Iterable<Tree<String>> getChoices(final Iterable<TreeAutomaton> it){
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
        TreeAutomaton.BottomUpStateVisitor visitor = (int state, Iterable<Rule> rulesTopDown) -> {
            
            rulesTopDown.forEach((Rule r) -> {
                String label = data.getSignature().resolveSymbolId(r.getLabel());
                
                double d = counts.getDouble(label);
                
                r.setWeight(d+1);
            });
        };
        data.foreachStateInBottomUpOrder(visitor);
        
        return data.viterbi();
    }
    
    /**
     * 
     * @param data
     * @return 
     */
    public Object2DoubleMap<String> countVariablesTopDown(Iterable<TreeAutomaton> data) {
        Object2DoubleOpenHashMap<String> result = new Object2DoubleOpenHashMap<>();
        result.defaultReturnValue(0.0);
        
        IntArrayList toDo = new IntArrayList();
        IntSet seen = new IntOpenHashSet();
        
        data.forEach((TreeAutomaton ta) -> {
            toDo.clear();
            seen.clear();
            
            toDo.addAll(ta.getFinalStates());
            seen.addAll(ta.getFinalStates());
            
            IntSet labels = ta.getAllLabels();
            IntIterator iit = labels.iterator();
            
            while(iit.hasNext()) {
                int label = iit.nextInt();
                String s = ta.getSignature().resolveSymbolId(label);
                
                if(Variables.isVariable(s)) {
                    result.addTo(s, 1);
                }
            }
        });
        
        return result;
    }
}
