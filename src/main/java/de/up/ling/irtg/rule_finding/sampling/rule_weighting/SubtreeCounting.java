/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.rule_weighting;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.sampling.models.SubtreeIterator;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.irtg.util.LogSpaceOperations;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 *
 * @author teichmann
 */
public class SubtreeCounting extends RegularizedKLRuleWeighting {

    /**
     *
     */
    private CentralCounter counter;

    /**
     *
     */
    private final InterpretedTreeAutomaton ita;

    /**
     *
     * @param basis
     * @param normalizationExponent
     * @param normalizationDivisor
     * @param rate
     * @param model
     */
    public SubtreeCounting(InterpretedTreeAutomaton basis, int normalizationExponent, double normalizationDivisor, LearningRate rate, CentralCounter model) {
        super(basis.getAutomaton(), normalizationExponent, normalizationDivisor, rate);

        this.counter = model;
        this.ita = basis;
    }

    @Override
    public double getLogTargetProbability(Tree<Rule> sample) {
        return this.counter.getLogProbability(sample, ita);
    }

    /**
     *
     * @param tree
     * @param amount
     */
    public void add(Tree<Rule> tree, double amount) {
        this.counter.add(tree, this.ita.getAutomaton().getSignature(), amount);
    }

    /**
     *
     * @param counter
     */
    public void setCounter(CentralCounter counter) {
        this.counter = counter;
    }

    /**
     *
     * @return
     */
    public InterpretedTreeAutomaton getBasis() {
        return this.ita;
    }

    /**
     *
     */
    public static class CentralCounter {

        /**
         *
         */
        private final IntTrieCounter counter;

        /**
         *
         */
        private final double smooth;

        /**
         *
         */
        private final double lexiconSize;

        /**
         *
         */
        private final double variablesSize;

        /**
         *
         */
        private final Object synchronizer;
        
        /**
         * 
         */
        private final double addition;
        
        /**
         * 
         */
        private final IntPredicate isVariable;
        
        /**
         * 
         */
        private final double logSmooth;
        
        /**
         * 
         */
        private final IntUnaryOperator arities;

        /**
         * 
         * @param smooth
         * @param addition
         * @param signature 
         */
        public CentralCounter(double smooth, double addition, Signature signature) {
            this.synchronizer = new Object();
            this.counter = new IntTrieCounter();
            this.smooth = smooth;
            this.logSmooth = Math.log(smooth);
            this.addition = addition;
            this.arities = signature::getArity;
            
            double countLexicon = 0;
            
            IntSet variables = new IntOpenHashSet();
            
            for(int symbol=1;symbol<=signature.getMaxSymbolId();++symbol) {
                String label = signature.resolveSymbolId(symbol);
                
                if(Variables.isVariable(label)) {
                    variables.add(symbol);
                } else {
                    countLexicon += 1;
                }
            }
            
            isVariable = variables::contains;
            this.lexiconSize = -Math.log(countLexicon);
            this.variablesSize = -Math.log(variables.size());
        }

        /**
         *
         * @param sample
         * @param main
         * @return
         */
        public double getLogProbability(Tree<Rule> sample, InterpretedTreeAutomaton main) {
            double logFactor = 0.0;
            SubtreeIterator lIt = new SubtreeIterator(sample, isVariable);
            
            IntList open = new IntArrayList();

            while (lIt.hasNext()) {                
                double baseProbability = 0.0;
                IntArrayList il = lIt.next();

                open.clear();
                open.add(1);
                for (int i = 1; i < il.size(); ++i) {
                    int value = il.get(i);

                    int arity;
                    if (isVariable.test(value)) {
                        baseProbability += Math.log(1.0 - (1.0 / (open.size()*this.addition)));
                        baseProbability += this.variablesSize;
                        
                        arity = 0;
                    } else {
                        baseProbability -= open.size() == 1 ? 0.0 : Math.log(open.size())+Math.log(this.addition);
                        baseProbability += this.lexiconSize;
                        
                        arity = arities.applyAsInt(value);
                    }
                    
                    if(arity < 1) {
                        while(arity < 1 && !open.isEmpty()) {
                           int pos = open.size()-1;
                           int lastVal = open.get(pos)-1;
                            
                           if(lastVal < 1) {
                               open.size(pos);
                           } else {
                               open.set(pos, lastVal);
                           }
                           
                           arity = lastVal;
                        }
                    } else {
                        open.add(arity);
                    }
                }

                double seen;
                synchronized (this.counter) {
                    seen = this.counter.get(il);
                }
                seen = seen < 0.0 && seen > -0.001 ? 0.0 : seen;

                double norm;
                synchronized (this.counter) {
                    IntTrieCounter st = this.counter.getSubtrie(il.get(0));
                    norm = st == null ? 0.0 : st.getNorm();
                }

                double above = LogSpaceOperations.add(Math.log(seen), baseProbability+this.logSmooth);
                logFactor += above - Math.log(norm + this.smooth);

                if (!Double.isFinite(logFactor)) {
                    System.out.println(seen);
                    System.out.println(norm);
                    System.out.println(logFactor);
                    throw new IllegalStateException("Could not produce consistent probability");
                }
            }

            return logFactor;
        }

        /**
         *
         * @param tr
         * @param sig
         * @param amount
         */
        public void add(Tree<Rule> tr, Signature sig, double amount) {
            SubtreeIterator it = new SubtreeIterator(tr, this.isVariable);

            while (it.hasNext()) {
                IntArrayList il = it.next();

                synchronized (this.counter) {
                    this.counter.add(il, amount);
                }
            }
        }
    }
}
