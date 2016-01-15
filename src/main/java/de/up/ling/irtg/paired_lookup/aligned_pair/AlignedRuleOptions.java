/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.paired_lookup.aligned_pair;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.paired_lookup.aligned_pair.interpretation_signatures.BasicInterpretationSignature;
import de.up.ling.irtg.util.NumberWrapping;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Stream;

/**
 *
 * @author christoph_teichmann
 * @param <X>
 * @param <Y>
 */
public class AlignedRuleOptions<X, Y> extends TreeAutomaton<Pair<X, Y>> {

    /**
     *
     */
    private AlignedStructure<X> structures1;

    /**
     *
     */
    private AlignedStructure<Y> structures2;

    /**
     *
     */
    private final IntSet done = new IntOpenHashSet();

    /**
     *
     */
    private final InterpretationSignature ints;

    /**
     *
     */
    private final DoubleBinaryOperator combo;

    /**
     *
     */
    private final Int2LongMap stateToCode;

    /**
     *
     */
    private final Long2IntMap codeToState;

    /**
     *
     */
    private final boolean includeBothEmpty;

    /**
     *
     * @param structures1
     * @param structures2
     * @param ins
     * @param dbo
     * @param includeBothEmpty
     */
    public AlignedRuleOptions(AlignedStructure structures1,
            AlignedStructure structures2,
            InterpretationSignature ins,
            DoubleBinaryOperator dbo,
            boolean includeBothEmpty) {
        super(ins.getUnderlyingSignature());

        this.stateToCode = new Int2LongOpenHashMap();
        this.stateToCode.defaultReturnValue(-1L);

        this.codeToState = new Long2IntOpenHashMap();
        this.codeToState.defaultReturnValue(-1);

        this.structures1 = structures1;
        this.structures2 = structures2;
        this.ints = ins;

        this.combo = dbo;

        this.structures1.getFinalStates().forEach((int state1) -> {

            this.structures2.getFinalStates().forEach((int state2) -> {
                IntCollection align1 = this.structures1.getAlignments(state1);
                IntCollection align2 = this.structures2.getAlignments(state2);

                if (align1.equals(align2)) {
                    int state = this.getStateForCode(NumberWrapping.combine(state1, state2));

                    this.addFinalState(state);
                }
            });
        });

        this.includeBothEmpty = includeBothEmpty;
    }

    /**
     *
     * @param structures1
     * @param structures2
     */
    public AlignedRuleOptions(AlignedStructure structures1,
            AlignedStructure structures2) {
        this(structures1, structures2, new BasicInterpretationSignature(),
                (double d1, double d2) -> d1 * d2, false);
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        if (!this.useCachedRuleTopDown(labelId, parentState)) {
            this.getRulesTopDown(parentState);
        }

        return this.getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int parentState) {
        if (this.done.contains(parentState)) {
            return this.getRuleStore().getRulesTopDown(parentState);
        }

        long parent = this.getCodeForState(parentState);

        int state1 = NumberWrapping.getFirst(parent);
        int state2 = NumberWrapping.getSecond(parent);

        Stream<AlignedTree> allowed = this.structures1.getAlignedTrees(state1);

        allowed.forEach((AlignedTree at1) -> {
            Stream<AlignedTree> alsoAllowed = this.structures2.getAlignedTrees(state2, at1);

            alsoAllowed.forEach((AlignedTree at2) -> {
                if (!(at1.isEmpty() && at2.isEmpty() && !this.includeBothEmpty)) {

                    int label = this.ints.getCode(at1, at2);

                    int[] children = new int[at1.getNumberVariables()];

                    for (int i = 0; i < children.length; ++i) {
                        int child1 = at1.getStateForVariable(i);
                        int child2 = at2.getStateForVariable(i);

                        int state = this.getStateForCode(NumberWrapping.combine(child1, child2));

                        children[i] = state;
                    }

                    this.addRule(parentState, label, children,
                            this.combo.applyAsDouble(at1.getWeight(), at2.getWeight()));
                }
            });
        });

        this.done.add(parentState);
        return this.getRulesTopDown(parentState);
    }

    @Override
    public IntIterable getLabelsTopDown(int parentState) {
        if (!this.done.contains(parentState)) {
            this.getRulesTopDown(parentState);
        }

        return this.getRuleStore().getLabelsTopDown(parentState);
    }

    @Override
    public boolean supportsBottomUpQueries() {
        return false;
    }

    @Override
    public boolean supportsTopDownQueries() {
        return true;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        throw new UnsupportedOperationException("Supports only top down questions.");
    }

    /**
     *
     * @param labelId
     * @return
     */
    private long getCodeForState(int state) {
        return this.stateToCode.get(state);
    }

    /**
     *
     * @param combination
     * @return
     */
    private int getStateForCode(long combination) {
        int state = this.codeToState.get(combination);

        if (state < 0) {
            int s1 = NumberWrapping.getFirst(combination);
            int s2 = NumberWrapping.getSecond(combination);

            X x = this.structures1.getState(s1);
            Y y = this.structures2.getState(s2);

            Pair<X, Y> p = new Pair<>(x, y);
            state = this.addState(p);

            this.codeToState.put(combination, state);
            this.stateToCode.put(state, combination);

            return state;
        } else {
            return state;
        }
    }

    /**
     *
     * @param parentState
     * @param label
     * @param children
     * @param d
     */
    private void addRule(int parentState, int label, int[] children, double weight) {
        Rule r = this.createRule(parentState, label, children, weight);

        this.storeRuleTopDown(r);
    }
}
