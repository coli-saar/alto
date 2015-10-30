/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.extractRules;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author christoph_teichmann
 */
public class SignificantVariables {
    
    /**
     * 
     */
    private final Function<String,String> getLeft;
    
    /**
     * 
     */
    private final Function<String,String> getRight;
    
    /**
     * 
     */
    private final Function<String,String> getReversePair;
    
    /**
     * 
     */
    private final Function<String,String> getPair;

    /**
     * 
     */
    private final SignificanceMeasure sm;

    /**
     * 
     * @param getLeft
     * @param getRight
     * @param getReversePair
     * @param getPair
     * @param sm 
     */
    public SignificantVariables(Function<String, String> getLeft,
            Function<String, String> getRight, Function<String, String> getReversePair,
            Function<String, String> getPair, SignificanceMeasure sm) {
        this.getLeft = getLeft;
        this.getRight = getRight;
        this.getReversePair = getReversePair;
        this.getPair = getPair;
        this.sm = sm;
    }    
    
    /**
     * 
     * @param inputCorpus
     * @param preventVarLoops
     * @return 
     */
    public List<Tree<String>> select(List<TreeAutomaton> inputCorpus, boolean preventVarLoops){
        Interner<String> inti = new Interner();
        
        Int2DoubleOpenHashMap singleCounts = new Int2DoubleOpenHashMap();
        Int2DoubleOpenHashMap pairCounts   = new Int2DoubleOpenHashMap();
        
        count(inputCorpus, inti, singleCounts, pairCounts);
        double overall = 0.0;
        DoubleIterator di = pairCounts.values().iterator();
        while(di.hasNext()){
            overall += di.nextDouble();
        }
        
        List<Tree<String>> trees = new ArrayList<>();
        select(inputCorpus, preventVarLoops, singleCounts, inti, pairCounts, trees, overall);
        
        return trees;
    }

    /**
     * 
     * @param inputCorpus
     * @param preventVarLoops
     * @param singleCounts
     * @param inti
     * @param pairCounts
     * @param trees 
     */
    private void select(List<TreeAutomaton> inputCorpus, boolean preventVarLoops,
            Int2DoubleOpenHashMap singleCounts, Interner<String> inti, Int2DoubleOpenHashMap pairCounts,
            List<Tree<String>> trees, double overall) {
        for(int i=0;i<inputCorpus.size();++i){
            TreeAutomaton aut = inputCorpus.get(i);
            
            if(preventVarLoops){
                TreeAutomaton rest = makeOnlyOneVariable(aut.getAllLabels(), aut.getSignature());
                aut = aut.intersect(rest);
            }
            
            final TreeAutomaton q = aut;
            
            Iterable<Rule> it = aut.getAllRulesTopDown();
            it.forEach((Rule r) -> {
                String label = q.getSignature().resolveSymbolId(r.getLabel());
                
                if (!Variables.IS_VARIABLE.test(label)) {
                    return;
                }

                String left = this.getLeft.apply(label);
                String right = this.getRight.apply(label);
                String pair = this.getPair.apply(label);
                String reverse = this.getReversePair.apply(label);

                double lC = singleCounts.get(inti.resolveObject(left));
                double rC = singleCounts.get(inti.resolveObject(right));
                double pC = pairCounts.get(inti.resolveObject(pair));
                double revC = pairCounts.get(inti.resolveObject(reverse));

                double score = this.sm.getPairSignificance(lC, rC, pC, revC, overall);
                r.setWeight(score);
            });

            trees.add(aut.viterbi());
        }
    }

    /**
     *
     * @param inputCorpus
     * @param inti
     * @param singleCounts
     * @param pairCounts
     */
    private void count(List<TreeAutomaton> inputCorpus, Interner<String> inti, Int2DoubleOpenHashMap singleCounts, Int2DoubleOpenHashMap pairCounts) {
        for (int i = 0; i < inputCorpus.size(); ++i) {
            TreeAutomaton ta = inputCorpus.get(i);
            Iterable<Rule> it = ta.getAllRulesTopDown();

            it.forEach((Rule t) -> {
                String lab = ta.getSignature().resolveSymbolId(t.getLabel());

                if (!Variables.IS_VARIABLE.test(lab)) {
                    return;
                }

                String left = getLeft.apply(lab);
                String right = getRight.apply(lab);
                String pair = getPair.apply(lab);

                int li = inti.addObject(left);
                int ri = inti.addObject(right);
                int pi = inti.addObject(pair);

                singleCounts.addTo(li, 1.0);
                singleCounts.addTo(ri, 1.0);
                pairCounts.addTo(pi, 1.0);
            });
        }
    }

    /**
     * 
     * @param allLabels
     * @return 
     */
    private TreeAutomaton makeOnlyOneVariable(IntSet allLabels, Signature sig) {
        ConcreteTreeAutomaton<Boolean> cta = new ConcreteTreeAutomaton<>(sig);
        int t = cta.addState(Boolean.TRUE);
        int f = cta.addState(Boolean.FALSE);
        
        cta.addFinalState(f);
        
        IntIterator iit = allLabels.iterator();
        while(iit.hasNext()){
            int label = iit.nextInt();
            String s = sig.resolveSymbolId(label);
            
            int[] children = new int[sig.getArity(label)];
            if(Variables.IS_VARIABLE.test(s)){
                
                for(int i=0;i<children.length;++i){
                    children[i] = t;
                }
                
                cta.addRule(cta.createRule(f, label, children, 1.0));
            }else{
                for(int i=0;i<children.length;++i){
                    children[i] = f;
                }
                
                cta.addRule(cta.createRule(t, label, children, 1.0));
                
                for(int i=0;i<children.length;++i){
                    children[i] = f;
                }
                
                cta.addRule(cta.createRule(f, label, children, 1.0));
            }
        }
        
        return cta;
    }
}
