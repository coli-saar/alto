/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 * @param <State>
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    /**
     * 
     */
    private final IntSet allLabels = new IntOpenHashSet();
    
    /**
     * 
     */
    private final IntSet immutableWrapper = new IntSet() {

        @Override
        public IntIterator iterator() {
           return allLabels.iterator();
        }

        @Override
        public boolean remove(int i) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        @Deprecated
        public IntIterator intIterator() {
            return allLabels.intIterator();
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            return allLabels.toArray(ts);
        }

        @Override
        public boolean contains(int i) {
            return allLabels.contains(i);
        }

        @Override
        public int[] toIntArray() {
            return allLabels.toIntArray();
        }

        @Override
        public int[] toIntArray(int[] ints) {
            return allLabels.toIntArray(ints);
        }

        @Override
        public int[] toArray(int[] ints) {
            return allLabels.toArray(ints);
        }

        @Override
        public boolean add(int i) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean rem(int i) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean addAll(IntCollection ic) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean containsAll(IntCollection ic) {
            return allLabels.containsAll(ic);
        }

        @Override
        public boolean removeAll(IntCollection ic) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean retainAll(IntCollection ic) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public int size() {
            return allLabels.size();
        }

        @Override
        public boolean isEmpty() {
            return allLabels.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return allLabels.contains(o);
        }

        @Override
        public Object[] toArray() {
            return allLabels.toArray();
        }

        @Override
        public boolean add(Integer e) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return allLabels.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Immutable");
        }
    };
    
    
    public ConcreteTreeAutomaton() {
        this(new Signature());
    }

    public ConcreteTreeAutomaton(Signature signature) {
        super(signature);
        ruleStore.setExplicit(true);
    }

    @Override
    public int addState(State state) {
        return super.addState(state);
    }

    @Override
    public void addFinalState(int state) {
        super.addFinalState(state);
    }
    

    public void addRule(Rule rule) {
        this.allLabels.add(rule.getLabel());
        storeRuleBottomUp(rule);
        storeRuleTopDown(rule);
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return ruleStore.isBottomUpDeterministic();
    }

    @Override
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final SignatureMapper signatureMapper, final Consumer<Rule> fn) {
        ruleStore.foreachRuleBottomUpForSets(labelIds, childStateSets, signatureMapper, fn);
    }

    @Override
    public IntSet getAllLabels() {
        return this.immutableWrapper;
    }
}
