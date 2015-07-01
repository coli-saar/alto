/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.base.Function;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class WideStringAlgebra extends StringAlgebra {
    private static final String WIDE_BINARY_CONCATENATION = "conc2";
    
    public WideStringAlgebra() {
        getSignature().clear(); // remove * from StringAlgebra
        getSignature().addSymbol(WIDE_BINARY_CONCATENATION, 2);
        this.specialStarId = getSignature().addSymbol(StringAlgebra.SPECIAL_STAR, 0);
    }

    @Override
    public TreeAutomaton decompose(List<String> value) {
        return new WideCkyAutomaton(value);
    }

    private class WideCkyAutomaton extends TreeAutomaton<Span> {
        private int[] words;
        private boolean isBottomUpDeterministic;
        private Int2IntMap concatArities;

        public WideCkyAutomaton(List<String> words) {
            super(WideStringAlgebra.this.getSignature());

            this.words = new int[words.size()];
            for (int i = 0; i < words.size(); i++) {
                this.words[i] = WideStringAlgebra.this.getSignature().getIdForSymbol(words.get(i));
            }

            finalStates.add(addState(new Span(0, words.size())));

            concatArities = new Int2IntOpenHashMap();
            for (int symId = 0; symId < signature.getMaxSymbolId(); symId++) {
                if (signature.getArity(symId) > 0) {
                    concatArities.put(symId, signature.getArity(symId));
                }
            }

            // automaton is nondeterministic iff the same word
            // occurs twice in the string
            isBottomUpDeterministic = new HashSet<String>(words).size() == words.size();
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
                    storeRule(rule);

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
                    
                    assert arity >= 2;

                    forAscendingTuple(parentSpan.start+1, parentSpan.end, 0, new int[arity-1], new Function<int[], Void>() {
                        public Void apply(int[] tuple) {
                            int[] childStates = new int[arity];
                            
                            childStates[0] = addState(new Span(parentSpan.start, tuple[0]));

                            for (int i = 0; i < arity - 2; i++) {
                                childStates[i+1] = addState(new Span(tuple[i], tuple[i + 1]));
                            }

                            childStates[arity-1] = addState(new Span(tuple[arity-2], parentSpan.end));
                            Rule rule = createRule(parentState, label, childStates, 1);
                            storeRule(rule);
                            
                            return null;
                        }
                    });
                } else if ((parentSpan.length() == 1) && label == words[parentSpan.start]) {
                    Rule rule = createRule(parentState, label, new int[0], 1);
                    storeRule(rule);
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public IntIterable getLabelsTopDown(int parentState) {
            Span parentSpan = getStateForId(parentState);

            if (parentSpan.end == parentSpan.start + 1) {
                IntSet ret = new IntOpenHashSet();
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
