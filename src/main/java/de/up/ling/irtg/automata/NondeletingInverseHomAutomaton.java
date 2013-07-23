/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class NondeletingInverseHomAutomaton<State> extends TreeAutomaton<String> {
    private TreeAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
    private Map<String, State> rhsState;

    public NondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;

        assert hom.isNonDeleting();

        rhsState = new HashMap<String, State>();

        for (State fin : rhsAutomaton.getFinalStates()) {
            finalStates.add(fin.toString());
        }

        // _must_ do this here to cache mapping from strings to rhs states
        for (State s : rhsAutomaton.getAllStates()) {
            String normalized = addState(s.toString());
            rhsState.put(normalized, s);
        }
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(int label, final List<String> childStates) {
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();

            Set<State> resultStates = rhsAutomaton.run(hom.get(label), HomomorphismSymbol.getHomSymbolToIntFunction(), new Function<Tree<HomomorphismSymbol>, State>() {
                @Override
                public State apply(Tree<HomomorphismSymbol> f) {
                    if (f.getLabel().isVariable()) {
                        String child = childStates.get(f.getLabel().getIndex());
                        return rhsState.get(child);
                    } else {
                        return null;
                    }
                }
            });

            for (State r : resultStates) {
                Rule<String> rule = createRule(r.toString(), label, childStates);
                storeRule(rule);
                ret.add(rule);
            }

            return ret;
        }
    }

    @Override
    public Set<Rule<String>> getRulesTopDown(int label, String parentState) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    @Override
    public boolean isBottomUpDeterministic() {
        return rhsAutomaton.isBottomUpDeterministic();
    }
}
