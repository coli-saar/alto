/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
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
        if (useCachedRuleTopDown(label, parentState)) {
            return getRulesTopDownFromExplicit(label, parentState);
        } else {
            Tree<HomomorphismSymbol> rhs = hom.get(label);
            Set<Rule<String>> ret = new HashSet<Rule<String>>();

//            System.err.println("parent=" + parentState + ", rhs=" + rhs + ", ar=" + getRhsArity(rhs));
            for (List<String> substitutionTuple : grtdDfs(rhs, rhsState.get(parentState), getRhsArity(rhs))) {
                if (isCompleteSubstitutionTuple(substitutionTuple)) {
                    Rule<String> rule = createRule(parentState, label, substitutionTuple);
//                    System.err.println(" -> " + rule.toString(this));
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }

    private boolean isCompleteSubstitutionTuple(List<String> tuple) {
        for (String s : tuple) {
            if (s == null) {
                return false;
            }
        }

        return true;
    }

    private int getRhsArity(Tree<HomomorphismSymbol> rhs) {
        int max = -1;

        for (HomomorphismSymbol sym : rhs.getLeafLabels()) {
            if (sym.isVariable() && (sym.getValue() > max)) {
                max = sym.getValue();
            }
        }

        return max + 1;
    }

    private Set<List<String>> grtdDfs(Tree<HomomorphismSymbol> rhs, State state, int rhsArity) {
        Set<List<String>> ret = new HashSet<List<String>>();

//        System.err.println("dfs: " + state + "/" + rhs);

        switch (rhs.getLabel().getType()) {
            case CONSTANT:
                for (Rule<State> rhsRule : rhsAutomaton.getRulesTopDown(rhs.getLabel().getValue(), state)) {
//                    System.err.println("rule: " + rhsRule.toString(rhsAutomaton));
                    List<Set<List<String>>> childrenSubstitutions = new ArrayList<Set<List<String>>>(); // len = #children

                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        childrenSubstitutions.add(grtdDfs(rhs.getChildren().get(i), rhsRule.getChildren()[i], rhsArity));
                    }

                    CartesianIterator<List<String>> it = new CartesianIterator<List<String>>(childrenSubstitutions);
                    while (it.hasNext()) {
                        List<List<String>> tuples = it.next();  // len = # children x # variables
                        List<String> merged = mergeSubstitutions(tuples, rhsArity);
                        if (merged != null) {
                            ret.add(merged);
                        }
                    }
                }
                break;

            case VARIABLE:
                List<String> rret = new ArrayList<String>(rhsArity);
                int varnum = rhs.getLabel().getValue();

                for (int i = 0; i < rhsArity; i++) {
                    if (i == varnum) {
                        rret.add(state.toString());
                    } else {
                        rret.add(null);
                    }
                }

                ret.add(rret);
        }

//        System.err.println(state + "/" + rhs + "  ==> " + ret);
        return ret;
    }

    // tuples is an n-list of m-lists of output states, where
    // n is number of children, and m is number of variables in homomorphism
    // If n = 0, the method returns [null, ..., null]
    private List<String> mergeSubstitutions(List<List<String>> tuples, int rhsArity) {
        List<String> merged = new ArrayList<String>();  // one entry per variable

//        System.err.println("    merge: " + tuples);

        for (int i = 0; i < rhsArity; i++) {
            merged.add(null);
        }

        for (int i = 0; i < tuples.size(); i++) {
            for (int j = 0; j < rhsArity; j++) {
                String state = tuples.get(i).get(j);
                if (state != null) {
                    if (merged.get(j) != null && !merged.get(j).equals(state)) {
                        return null;
                    } else {
                        merged.set(j, state);
                    }
                }
            }
        }

//        System.err.println("    --> merged: " + merged);

        return merged;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return rhsAutomaton.isBottomUpDeterministic();
    }
}
