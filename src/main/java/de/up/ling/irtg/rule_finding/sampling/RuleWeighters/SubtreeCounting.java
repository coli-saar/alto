/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.RuleWeighters;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.sampling.models.SubtreeIterator;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntTrieCounter;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.HashSet;
import java.util.Set;
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
    private final CentralCounter counter;

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
        private final Interner<String> mainInterner;

        /**
         * 
         * @param smooth
         * @param signatures 
         */
        public CentralCounter(double smooth, Iterable<Signature> signatures) {
            this.counter = new IntTrieCounter();
            this.smooth = smooth;
            this.mainInterner = new Interner<>();

            double count = 0.0;

            Set<String> set = new HashSet();
            for (Signature sig : signatures) {
                set.addAll(sig.getSymbols());
            }

            this.lexiconSize = set.size();
        }

        /**
         * 
         * @param sample
         * @param main
         * @return 
         */
        public double getLogProbability(Tree<Rule> sample, InterpretedTreeAutomaton main) {
            double logFactor = 0.0;

            SubtreeIterator lIt = new SubtreeIterator(sample, createPredicate(main));
            IntUnaryOperator iuo = this.createMapping(main);

            while (lIt.hasNext()) {
                IntArrayList il = lIt.next();
                
                for (int i = 0; i < il.size(); ++i) {
                    il.set(i, iuo.applyAsInt(il.get(i)));
                }

                double seen = this.counter.get(il);
                double smoo = (Math.pow(this.lexiconSize, -(il.size() - 1))) * smooth;
                smoo = smoo <= 0.0 ? Double.MIN_VALUE : smoo;

                IntTrieCounter st = this.counter.getSubtrie(il.get(0));

                double allSeen = st == null ? 0.0 : st.getNorm();

                logFactor += Math.log(seen + smoo) - Math.log(allSeen + this.smooth);
            }

            return logFactor;
        }

        /**
         *
         * @param ita
         * @return
         */
        private static IntPredicate createPredicate(InterpretedTreeAutomaton ita) {
            final Signature sig = ita.getAutomaton().getSignature();
            IntPredicate choice = (int i) -> {
                return Variables.isVariable(sig.resolveSymbolId(i));
            };
            return choice;
        }

        /**
         *
         * @param ita
         * @return
         */
        private IntUnaryOperator createMapping(InterpretedTreeAutomaton ita) {
            final Signature sig = ita.getAutomaton().getSignature();
            IntUnaryOperator iuo = (int i) -> {
                return this.mainInterner.addObject(sig.resolveSymbolId(i));
            };

            return iuo;
        }

        /**
         *
         * @param tr
         * @param ita
         * @param amount
         */
        public void add(Tree<Rule> tr, InterpretedTreeAutomaton ita, double amount) {
            IntPredicate vars = createPredicate(ita);
            IntUnaryOperator iuo = this.createMapping(ita);

            SubtreeIterator it = new SubtreeIterator(tr, vars);

            while (it.hasNext()) {
                IntArrayList il = it.next();

                for (int i = 0; i < il.size(); ++i) {
                    il.set(i, iuo.applyAsInt(il.get(i)));
                }

                this.counter.add(il, amount);
            }
        }
    }
}
