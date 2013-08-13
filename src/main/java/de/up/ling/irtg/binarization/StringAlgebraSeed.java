/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class StringAlgebraSeed extends RegularSeed<Span> {
    private Signature sourceSignature;
    private Signature targetSignature;
//    private String binaryConcatenation; // \in targetSignature
    private int binaryConcatenationId;  // in targetSignature

    public StringAlgebraSeed(Signature sourceSignature, Signature targetSignature, String binaryConcatenation) {
        this.sourceSignature = sourceSignature;
        this.targetSignature = targetSignature;
//        this.binaryConcatenation = binaryConcatenation;
        this.binaryConcatenationId = targetSignature.getIdForSymbol(binaryConcatenation);
    }

    @Override
    public TreeAutomaton binarize(String symbol) {
        if (sourceSignature.getArityForLabel(symbol) <= 2) {
            return new SingletonAutomaton(symbol);
        } else {
            return new BinarizationAutomaton(symbol);
        }
    }

    private class SingletonAutomaton extends ConcreteTreeAutomaton<String> {
        public SingletonAutomaton(String symbol) {
            int arity = sourceSignature.getArityForLabel(symbol);
            List<String> childStates = new ArrayList<String>();
            List<String> empty = new ArrayList<String>();

            for (int i = 0; i < arity; i++) {
                String childState = "q" + i;

                createRule(childState, "?" + i, empty);
//                addRule("?"+i, empty, childState);
                childStates.add(childState);
            }

            createRule("q", symbol, childStates);
            addFinalState(getIdForState("q"));
        }
    }

    private class BinarizationAutomaton extends TreeAutomaton<Span> {
        public BinarizationAutomaton(String symbol) {
            super(targetSignature);
            addFinalState(addState(new Span(0, sourceSignature.getArityForLabel(symbol))));
        }

        @Override
        public Set<Rule> getRulesBottomUp(int labelId, int[] childStateIds) {
            Set<Rule> ret = new HashSet<Rule>();
            String label = signature.resolveSymbolId(labelId);

            Span[] childStates = new Span[childStateIds.length];
            for (int i = 0; i < childStateIds.length; i++) {
                childStates[i] = getStateForId(childStateIds[i]);
            }

            if (label.startsWith("?")) {
                if (childStateIds.length == 0) {
                    int var = Integer.parseInt(label.substring(1));

                    ret.add(createRule(new Span(var, var + 1), label, childStates));
                }
            } else if (labelId == binaryConcatenationId && childStateIds.length == 2) {
                if (childStates[0].end == childStates[1].start) {
                    ret.add(createRule(new Span(childStates[0].start, childStates[childStates.length - 1].end), label, childStates));
                }
            }

            return ret;
        }

        @Override
        public Set<Rule> getRulesTopDown(int labelId, int parentStateId) {
            Set<Rule> ret = new HashSet<Rule>();
            String label = signature.resolveSymbolId(labelId);
            Span parentState = getStateForId(parentStateId);

            if (label.startsWith("?")) {
                int var = Integer.parseInt(label.substring(1));

                if (parentState.start == var && parentState.end == var + 1) {
                    ret.add(createRule(parentState, label, new Span[]{}));
                }
            } else if (labelId == binaryConcatenationId) {
                int width = parentState.end - parentState.start;

                for (int i = 1; i < width - 1; i++) {
                    ret.add(createRule(parentState, label,
                            new Span[]{ new Span(parentState.start, parentState.start + i),
                                        new Span(parentState.start + i, parentState.end)}));
                }
            }

            return ret;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
    }
}
