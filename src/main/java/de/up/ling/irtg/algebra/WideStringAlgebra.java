/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.base.Function;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.binarization.StringAlgebraSeed;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A string algebra with concatenation of arbitrary width.
 * This algebra behaves like the ordinary {@link StringAlgebra}
 * in most ways. However, instead of a single binary concatenation
 * operation "*", the WideStringAlgebra defines a whole collection
 * of concatenation operations conc1, conc2, conc3, ... with arity
 * 1, 2, 3, ...<p>
 * 
 * The most direct way of writing a context-free grammar as an
 * IRTG is by using a WideStringAlgebra. For instance, the context-free rule
 * <code>A -&gt; B C D</code> corresponds to an RTG rule <code>A -&gt; r(B,C,D)</code>
 * with h(r) = conc3(?1, ?2, ?3). However, parsing with a WideStringAlgebra
 * takes time O(n^(r+1)), where r is the maximum arity of a concatenation
 * operation. It is therefore a good idea to {@link BkvBinarizer binarize}
 * the IRTG into an IRTG over a {@link StringAlgebra} (using a
 * {@link StringAlgebraSeed}) for O(n^3) parsing.
 * 
 * @author koller
 */
public class WideStringAlgebra extends StringAlgebra {

    private static final String WIDE_BINARY_CONCATENATION = "conc2";

    /**
     * Creates a new instance.
     * 
     * This algebra will not define the symbol * as the StringAlgebra does,
     * but instead has a conc2 operation.
     */
    public WideStringAlgebra() {
        getSignature().clear(); // remove * from StringAlgebra

        getSignature().addSymbol(WIDE_BINARY_CONCATENATION, 2);

        // but we still keep the special star symbol
        this.specialStarId = getSignature().addSymbol(StringAlgebra.SPECIAL_STAR, 0);
    }

    @Override
    public TreeAutomaton decompose(List<String> value) {
        return new WideCkyAutomaton(value);
    }

    @Override
    protected List<String> evaluate(String label, List<List<String>> childrenValues) {
        List<String> val = new ArrayList<>();
        
        if(label.equals(SPECIAL_STAR)) {
            val.add("*");
            return val;
        }
        
        if(childrenValues.size() > 0) {
            for(int i=0;i<childrenValues.size();++i) {
                val.addAll(childrenValues.get(i));
            }
            
            return val;
        }
        
        val.add(label);
        
        return val;
    }


    private class WideCkyAutomaton extends TreeAutomaton<Span> {

        private final int[] words;
        private final boolean isBottomUpDeterministic;
        private final Int2IntMap concatArities;

        /**
         * We have to allow unary productions to represent for example the
         * grammars we get from the WSJ corpus, but this means we also need to
         * be able to handle them correctly when we are asked for all labels.
         */
        private final IntSet unaryLabels;

        public WideCkyAutomaton(List<String> words) {
            super(WideStringAlgebra.this.getSignature());

            this.words = new int[words.size()];
            for (int i = 0; i < words.size(); i++) {
                String s = words.get(i);

                // if we encounter a star, then we need to convert it into the special symbol
                if (StringAlgebra.CONCAT.equals(s)) {
                    this.words[i] = WideStringAlgebra.this.specialStarId;
                } else {
                    this.words[i] = WideStringAlgebra.this.getSignature().getIdForSymbol(words.get(i));
                }
            }

            finalStates.add(addState(new Span(0, words.size())));

            concatArities = new Int2IntOpenHashMap();
            this.unaryLabels = new IntOpenHashSet();

            for (int symId = 0; symId <= signature.getMaxSymbolId(); symId++) {
                if (signature.getArity(symId) > 0) {
                    if (signature.getArity(symId) == 1) {
                        unaryLabels.add(symId);
                    }

                    concatArities.put(symId, signature.getArity(symId));
                }
            }

            // automaton is nondeterministic iff the same word
            // occurs twice in the string
            isBottomUpDeterministic = new HashSet<>(words).size() == words.size();
        }

        @Override
        public IntSet getAllStates() {
            IntSet ret = new IntOpenHashSet();

            for (int i = 0; i < words.length; i++) {
                for (int k = i + 1; k <= words.length; k++) {
                    ret.add(addState(new Span(i, k)));
                }
            }

            return ret;
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule> ret = new HashSet<>();

                if (concatArities.containsKey(label)) {
                    int arity = concatArities.get(label);

                    if (childStates.length != arity) {
                        return new HashSet<>();
                    }

                    for (int i = 0; i < arity - 1; i++) {
                        if (getStateForId(childStates[i]).end != getStateForId(childStates[i + 1]).start) {
                            return new HashSet<>();
                        }
                    }

                    Span span = new Span(getStateForId(childStates[0]).start, getStateForId(childStates[arity - 1]).end);
                    int spanState = addState(span);
                    Rule rule = createRule(spanState, label, childStates, 1);
                    ret.add(rule);
                    storeRuleBottomUp(rule);

                    return ret;
                } else {
                    for (int i = 0; i < words.length; i++) {
                        if (words[i] == label) {
                            ret.add(createRule(addState(new Span(i, i + 1)), label, new int[0], 1));
                        }
                    }

                    return ret;
                }
            }
        }

        private void forAscendingTuple(int minValue, int maxValue, int i, int[] tuple, Function<int[], Void> fn) {
            if (i == tuple.length) {
                fn.apply(tuple);
            } else {
                for (int j = minValue; j < maxValue; j++) {
                    tuple[i] = j;
                    forAscendingTuple(j + 1, maxValue, i + 1, tuple, fn);
                }
            }
        }

        @Override
        public Iterable<Rule> getRulesTopDown(final int label, final int parentState) {
            if (!useCachedRuleTopDown(label, parentState)) {
                final Span parentSpan = getStateForId(parentState);

                if (concatArities.containsKey(label)) {

                    final int arity = concatArities.get(label);

                    // handle unary steps if there are such labels
                    if (arity == 1) {
                        Rule rule = createRule(parentState, label, new int[]{parentState}, 1);
                        storeRuleTopDown(rule);
                    } else {
                        assert arity >= 2;

                        forAscendingTuple(parentSpan.start + 1, parentSpan.end, 0, new int[arity - 1], (int[] tuple) -> {
                            int[] childStates = new int[arity];
                            
                            childStates[0] = addState(new Span(parentSpan.start, tuple[0]));
                            
                            for (int i = 0; i < arity - 2; i++) {
                                childStates[i + 1] = addState(new Span(tuple[i], tuple[i + 1]));
                            }
                            
                            childStates[arity - 1] = addState(new Span(tuple[arity - 2], parentSpan.end));
                            Rule rule = createRule(parentState, label, childStates, 1);
                            storeRuleTopDown(rule);
                            
                            return null;
                        });
                    }
                } else if ((parentSpan.length() == 1) && label == words[parentSpan.start]) {
                    Rule rule = createRule(parentState, label, new int[0], 1);
                    storeRuleTopDown(rule);
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public IntIterable getLabelsTopDown(int parentState) {
            Span parentSpan = getStateForId(parentState);

            if (parentSpan.end == parentSpan.start + 1) {
                // if there are any unary labels, then we always have to add them
                IntSet ret = this.unaryLabels.isEmpty() ? new IntOpenHashSet() : new IntOpenHashSet(this.unaryLabels);

                ret.add(words[parentSpan.start]);
                return ret;
            } else {
                return concatArities.keySet();
            }
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return isBottomUpDeterministic;
        }
    }

    @Override
    public String getBinaryConcatenation() {
        return WIDE_BINARY_CONCATENATION;
    }
}
