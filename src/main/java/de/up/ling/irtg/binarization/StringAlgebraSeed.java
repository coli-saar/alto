/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra.Span;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class StringAlgebraSeed extends RegularSeed {
    private Signature sourceSignature;
    private Signature targetSignature;
    private int binaryConcatenationId;  // in targetSignature
    
    /**
     * This is only for technical reasons; don't use this constructor!
     * Use {@link #StringAlgebraSeed(de.up.ling.irtg.signature.Signature, java.lang.String) } instead.
     */
    public StringAlgebraSeed() {
        
    }

    public StringAlgebraSeed(Algebra sourceAlgebra, Algebra targetAlgebra) {
        this.targetSignature = targetAlgebra.getSignature();
        this.sourceSignature = sourceAlgebra.getSignature();
        
        if( targetAlgebra instanceof StringAlgebra ) {
            this.binaryConcatenationId = targetSignature.getIdForSymbol(((StringAlgebra) targetAlgebra).getBinaryConcatenation());
        } else {
            throw new IllegalArgumentException("Target algebra must be a StringAlgebra, but was a " + targetAlgebra.getClass());
        }
    }

    @Override
    public TreeAutomaton binarize(String symbol) {
        // ensure that signature contains the symbols ?1, ..., ?n where n = arity(symbol)
        // this is slightly hacky (these symbols have no interpretation in the algebra),
        // but necessary so the automata can accept langauges with these variable symbols
        for( int i = 0; i < sourceSignature.getArityForLabel(symbol); i++ ) {
            targetSignature.addSymbol("?" + (i+1), 0);
        }
        
        switch(sourceSignature.getArityForLabel(symbol)) {
            // return all symbols of arity 0 unchanged
            // -- except the word "*", which needs to be mapped to "__*__" 
            // (a constant that the string algebra evaluates to "*")
            case 0: if( StringAlgebra.CONCAT.equals(symbol) ) {
                return new OneSymbolAutomaton(StringAlgebra.SPECIAL_STAR, sourceSignature);
            } else {
                return new OneSymbolAutomaton(symbol, targetSignature);
            }
            
            case 1: return new OneSymbolAutomaton("?1", targetSignature);
                
            default: return new BinarizationAutomaton(sourceSignature.getArityForLabel(symbol), targetSignature, binaryConcatenationId);
        }
    }
    
    public static class OneSymbolAutomaton extends ConcreteTreeAutomaton<String> {
        public OneSymbolAutomaton(String symbol, Signature signature) {
            this.signature = signature;
            
            addRule(createRule("q", symbol, new ArrayList<String>()));
            addFinalState(getIdForState("q"));
        }        
    }

    private class SingletonAutomaton extends ConcreteTreeAutomaton<String> {
        public SingletonAutomaton(String symbol) {
            signature = targetSignature;
            int arity = sourceSignature.getArityForLabel(symbol);
            List<String> childStates = new ArrayList<String>();
            List<String> empty = new ArrayList<String>();

            for (int i = 1; i <= arity; i++) {
                String childState = "q" + i;

                addRule(createRule(childState, "?" + i, empty));
                childStates.add(childState);
            }

            addRule(createRule("q", symbol, childStates));
            addFinalState(getIdForState("q"));
        }
    }

    private class MyBinarizationAutomaton extends TreeAutomaton<Span> {
        public MyBinarizationAutomaton(String symbol) {
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
                    int var = Integer.parseInt(label.substring(1)) - 1;

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

            if (HomomorphismSymbol.isVariableSymbol(label)) {
                int var = Integer.parseInt(label.substring(1))-1;

                if (parentState.start == var && parentState.end == var + 1) {
                    ret.add(createRule(parentState, label, new Span[]{}));
                }
            } else if (labelId == binaryConcatenationId) {
                int width = parentState.end - parentState.start;

                for (int i = 1; i < width; i++) {
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
