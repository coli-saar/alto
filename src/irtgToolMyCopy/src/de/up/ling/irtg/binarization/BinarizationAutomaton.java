/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author koller
 */
public class BinarizationAutomaton extends TreeAutomaton<StringAlgebra.Span> {
    private int concatId;
    
    public BinarizationAutomaton(int len, Signature signature, int concatId) {
        super(signature);
        addFinalState(addState(new StringAlgebra.Span(0, len)));
        this.concatId = concatId;
    }

    @Override
    public Set<Rule> getRulesBottomUp(int labelId, int[] childStateIds) {
        Set<Rule> ret = new HashSet<Rule>();
        String label = signature.resolveSymbolId(labelId);

        StringAlgebra.Span[] childStates = new StringAlgebra.Span[childStateIds.length];
        for (int i = 0; i < childStateIds.length; i++) {
            childStates[i] = getStateForId(childStateIds[i]);
        }

        if (label.startsWith("?")) {
            if (childStateIds.length == 0) {
                int var = Integer.parseInt(label.substring(1)) - 1;

                ret.add(createRule(new StringAlgebra.Span(var, var + 1), label, childStates));
            }
        } else if (labelId == concatId && childStateIds.length == 2) {
            if (childStates[0].end == childStates[1].start) {
                ret.add(createRule(new StringAlgebra.Span(childStates[0].start, childStates[childStates.length - 1].end), label, childStates));
            }
        }

        return ret;
    }

    @Override
    public Set<Rule> getRulesTopDown(int labelId, int parentStateId) {
        Set<Rule> ret = new HashSet<Rule>();
        String label = signature.resolveSymbolId(labelId);
        StringAlgebra.Span parentState = getStateForId(parentStateId);

        if (HomomorphismSymbol.isVariableSymbol(label)) {
            int var = Integer.parseInt(label.substring(1)) - 1;

            if (parentState.start == var && parentState.end == var + 1) {
                ret.add(createRule(parentState, label, new StringAlgebra.Span[]{}));
            }
        } else if (labelId == concatId) {
            int width = parentState.end - parentState.start;

            for (int i = 1; i < width; i++) {
                ret.add(createRule(parentState, label,
                        new StringAlgebra.Span[]{new StringAlgebra.Span(parentState.start, parentState.start + i),
                    new StringAlgebra.Span(parentState.start + i, parentState.end)}));
            }
        }

        return ret;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return true;
    }
}
