/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.induction;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.ToDoubleFunction;

/**
 *
 * @author koller
 */
public class LabeledChineseRestaurant<E> {
    private final double alpha, beta;
    protected final Map<E, Integer> labels = new IdentityHashMap<>();            // keyset = different eltree tables that were used
    protected final Multiset<E> NE = HashMultiset.create();                               // n^-_e = # e got used in derivations
    protected final Multiset<E> KE = HashMultiset.create();                               // K^-_e = # different copies of e were used in derivations
    private int NC;                                                                                // n^-_c = # rewrites in the entire corpus
    protected int KC;                                                                                // K^-_c = # tables = sum_e(KE)
    protected boolean mustRecomputeK = true;
    private List<Consumer<LabeledChineseRestaurant>> listeners = new ArrayList<Consumer<LabeledChineseRestaurant>>();

    public LabeledChineseRestaurant(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }
    
    /**
     * ElementaryTrees and NE are both multisets of elementary trees, which keep
     * track of how many instances of these eltrees we have seen. The difference
     * is that elementaryTrees distinguishes between different eltree objects
     * (even if they are equals), whereas NE treats two equals eltrees as
     * identical.
     */
    /**
     * updating counts *
     */
    public void clear() {
        NC = 0;
        KC = 0;
        labels.clear();
        NE.clear();
        KE.clear();
        setMustRecompute();
    }

    protected int getLabelTokenCount(E label) {
        Integer count = labels.get(label);
        return count == null ? 0 : count;
    }

    public void observeLabel(E label) {
        labels.put(label, labels.getOrDefault(label, 0) + 1);
        NE.add(label);
        NC++;
        setMustRecompute();
    }

    protected void recomputeK() {
        if (mustRecomputeK) {
            // compute KE from elementaryTrees
            KE.clear();
            KE.addAll(labels.keySet());

            // compute KC from KE
            KC = KE.size();

            mustRecomputeK = false;
        }
    }

    protected void setMustRecompute() {
        mustRecomputeK = true;
        listeners.forEach(cons -> cons.accept(this));
    }

    protected void addUpdateListener(Consumer<LabeledChineseRestaurant> listener) {
        listeners.add(listener);
    }

    /**
     * querying current counts *
     */
    // only returns each "equals" elementary tree once
    public Set<E> getLabelTypes() {
        recomputeK();
        return KE.elementSet();
    }

    // may return multiple "equals" copies of the same elementary tree
    public Set<E> getLabelTokens() {
        return labels.keySet();
    }

    public int getCount(E eltree) {
        return NE.count(eltree);
    }

    public int getTableCount(E eltree) {
        recomputeK();
        return KE.count(eltree);
    }

    public int getTotalCount() {
        return NC;
    }

    public int getTotalTableCount() {
        recomputeK();
        return KC;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }
    
    

    /**
     * derived statistics -- no need to override these *
     */
    public double getExistingTableProbability(E label) {
        double ret = (getCount(label) - getTableCount(label) * alpha) / (getTotalCount() + beta);

        if (ret < 0) {
            System.err.println("getExistingTableProb < 0!!!");
            System.err.println("  for label " + label);
            System.err.println("  count=" + getCount(label) + ", tablecount=" + getTableCount(label) + ", total count=" + getTotalCount());
            new RuntimeException().printStackTrace(System.err);
            System.exit(0);
        }

        return ret;
    }

    public double getNewTableProbability() {
        return (getTotalTableCount() * alpha + beta) / (getTotalCount() + beta);  // prob for inventing new eltree
    }
    
    public double getTotalProbability(E label, ToDoubleFunction<E> baseProbability) {
        return getExistingTableProbability(label) + getNewTableProbability()*baseProbability.applyAsDouble(label);
    }
    
    
    

    public LabeledChineseRestaurant plus(LabeledChineseRestaurant other) {
        return new CombinationOfLabeledCRs(this, other, (x, y) -> x + y);
    }

    public LabeledChineseRestaurant minus(LabeledChineseRestaurant other) {
        return new CombinationOfLabeledCRs(this, other, (x, y) -> x - y);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        recomputeK();
        
        buf.append("total: " + NC + " guests at " + KC + " tables\n");
        
        for( E eltree : getLabelTypes() ) {
            buf.append("\n" + KE.count(eltree) + " tables for " + eltree + " (total observed: " + NE.count(eltree) + ")\n");
            
            for( E copy : getLabelTokens() ) {
                if( copy.equals(eltree)) {
                    buf.append("  -> " + labels.get(copy) + "\n");
                }
            }
        }
        
        return buf.toString();
    }
    
    

    private static class CombinationOfLabeledCRs<E> extends LabeledChineseRestaurant<E> {
        private final LabeledChineseRestaurant<E> first, second;
        private final IntBinaryOperator op;

        public CombinationOfLabeledCRs(LabeledChineseRestaurant<E> first, LabeledChineseRestaurant<E> second, IntBinaryOperator op) {
            super(first.alpha, first.beta);
            
            assert first.alpha == second.alpha && first.beta == second.beta;
            
            this.first = first;
            this.second = second;
            this.op = op;

            mustRecomputeK = true;

            first.addUpdateListener(x -> setMustRecompute());
            second.addUpdateListener(x -> setMustRecompute());
        }

        @Override
        protected void recomputeK() {
            if (mustRecomputeK) {
                first.recomputeK();
                second.recomputeK();

                labels.clear();
                labels.putAll(first.labels);

                for (E eltree : second.labels.keySet()) {
                    int newCount = op.applyAsInt(labels.getOrDefault(eltree, 0), second.labels.get(eltree));

                    if (newCount > 0) {
                        labels.put(eltree, newCount);
                    } else {
                        labels.remove(eltree);
                    }
                }

                KE.clear();
                KE.addAll(labels.keySet());
                KC = KE.size();

                mustRecomputeK = false;
            }
        }

        @Override
        public int getTotalCount() {
            return op.applyAsInt(first.getTotalCount(), second.getTotalCount());
        }

        @Override
        public int getCount(E eltree) {
            return op.applyAsInt(first.getCount(eltree), second.getCount(eltree));
        }
    }
}
