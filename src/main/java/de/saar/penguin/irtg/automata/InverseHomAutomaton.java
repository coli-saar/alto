package de.saar.penguin.irtg.automata;

import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.saar.basic.StringOrVariable;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
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
class InverseHomAutomaton<State> extends TreeAutomaton<String> {
    public static String FAIL_STATE = "q_FAIL_";
    private TreeAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
    private Set<String> computedLabels;
    private Map<String, State> rhsState;

    public InverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
        computedLabels = new HashSet<String>();
        rhsState = new HashMap<String, State>();

        for (State fin : rhsAutomaton.getFinalStates()) {
            finalStates.add(fin.toString());
        }

        // _must_ do this here to cache mapping from strings to rhs states
        for (State s : rhsAutomaton.getAllStates()) {
            allStates.add(s.toString());
            rhsState.put(s.toString(), s);
        }
        allStates.add(FAIL_STATE);
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(String label, final List<String> childStates) {
        // lazy bottom-up computation of bottom-up rules
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();

            // run RHS automaton on given child states
            Set<State> resultStates = rhsAutomaton.run(hom.get(label), new Function<Tree<StringOrVariable>, State>() {
                @Override
                public State apply(Tree<StringOrVariable> f) {
                    if (f.getLabel().isVariable()) {
                        String child = childStates.get(Homomorphism.getIndexForVariable(f.getLabel()));

                        if (FAIL_STATE.equals(child)) {
                            // Hom is requesting the value of a variable, but the state we
                            // got passed bottom-up is FAIL. In this case, the run of the automaton
                            // on the hom image should fail as well. We do this in a slightly hacky
                            // way by simply leaving the variable in place and hoping that things like
                            // "?x1" are not terminal symbols of the automaton.
                            return null;
                        } else {
                            return rhsState.get(child);
                        }
                    } else {
                        return null;
                    }
                }
            });

            if (resultStates.isEmpty()) {
                // no successful runs found, add rule with FAIL parent
                Rule<String> rule = new Rule<String>(FAIL_STATE, label, childStates);
                storeRule(rule);
                ret.add(rule);
            } else {
                // found successful runs, add rules with ordinary parents
                for (State r : resultStates) {
                    Rule<String> rule = new Rule<String>(r.toString(), label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }

    private boolean containsFailedState(List<String> states) {
        for (String child : states) {
            if (FAIL_STATE.equals(child)) {
                return true;
            }
        }

        return false;
    }

    @Override
    // BUG - getRulesTopDown assumes that the homomorphism is non-deleting in order
    // to identify the arity of the symbols. For deleting homomorphisms, it will generate
    // symbols of the wrong arity, and intersection with an automaton of the correct
    // arity will fail.
    public Set<Rule<String>> getRulesTopDown(String label, String parentState) {
        if (FAIL_STATE.equals(parentState)) {
//            makeFailRulesExplicit(label);
        } else {

            if (!computedLabels.contains(label)) {
                computeRulesForLabel(label);
            }
        }

        return getRulesTopDownFromExplicit(label, parentState);
    }

    private void makeFailRulesExplicit(String label) {
        int arity = hom.getArity(label);
        List<Set<String>> listOfStateSets = new ArrayList<Set<String>>();

        for (int i = 0; i < arity; i++) {
            listOfStateSets.add(getAllStates());
        }

        CartesianIterator<String> it = new CartesianIterator<String>(listOfStateSets);
        while (it.hasNext()) {
            List<String> children = it.next();
            if (containsFailedState(children)) {
                storeRule(new Rule<String>(FAIL_STATE, label, children));
            }
        }
    }

    private void computeRulesForLabel(String label) {
        final Tree<StringOrVariable> rhsTree = hom.get(label);

        if (rhsTree != null) {

            Set<Item> rootItems = rhsTree.dfs(new TreeVisitor<StringOrVariable, Void, Set<Item>>() {
                @Override
                public Set<Item> combine(Tree<StringOrVariable> node, List<Set<Item>> childrenValues) {
                    Set<Item> ret = new HashSet<Item>();

                    // BUG - not all states are necessarily known at this point, so
                    // some rules are not found
                    if (node.getLabel().isVariable()) {
                        for (State state : rhsAutomaton.getAllStates()) {
                            ret.add(new Item(state, subst(node.getLabel(), state)));
                        }
                    } else {
                        CartesianIterator<Item> it = new CartesianIterator<Item>(childrenValues);

                        while (it.hasNext()) {
                            List<Item> childItems = it.next();
                            List<State> childStates = new ArrayList<State>();
                            List<Map<StringOrVariable, State>> childSubsts = new ArrayList<Map<StringOrVariable, State>>();

                            for (Item item : childItems) {
                                childStates.add(item.state);
                                childSubsts.add(item.substitution);
                            }

                            Set<Rule<State>> rules = rhsAutomaton.getRulesBottomUp(node.getLabel().toString(), childStates);

                            if (!rules.isEmpty()) {
                                Map<StringOrVariable, State> subst = mergeSubstitutions(childSubsts);
                                for (Rule<State> r : rules) {
                                    ret.add(new Item(r.getParent(), subst));
                                }
                            }
                        }
                    }

                    return ret;
                }
            });

            for (Item rootItem : rootItems) {
                List<String> childStates = new ArrayList<String>();

                for (int i = 0; i < hom.getArity(label); i++) {
                    // TODO - deal with unbound variables here!!
                    childStates.add(rootItem.substitution.get(new StringOrVariable("?" + (i + 1), true)).toString());
                }

                storeRule(new Rule<String>(rootItem.state.toString(), label, childStates));
            }

            computedLabels.add(label);


        }
    }

    private class Item {
        public State state;
        public Map<StringOrVariable, State> substitution;

        public Item(State state, Map<StringOrVariable, State> substitution) {
            this.state = state;
            this.substitution = substitution;
        }

        @Override
        public String toString() {
            return state.toString() + substitution;
        }
    }

    private Map<StringOrVariable, State> subst(StringOrVariable sov, State state) {
        Map<StringOrVariable, State> ret = new HashMap<StringOrVariable, State>();
        ret.put(sov, state);
        return ret;
    }

    private Map<StringOrVariable, State> mergeSubstitutions(List<Map<StringOrVariable, State>> substs) {
        Map<StringOrVariable, State> ret = new HashMap<StringOrVariable, State>();

        for (Map<StringOrVariable, State> subst : substs) {
            for (StringOrVariable key : subst.keySet()) {
                if (ret.containsKey(key)) {
                    if (!subst.get(key).equals(ret.get(key))) {
                        return null;
                    }
                } else {
                    ret.put(key, subst.get(key));
                }
            }
        }

        return ret;
    }

    /**
     * Returns the set of labels in the domain of the homomorphism. The actual
     * inverse homomorphism automaton may not contain rules for all of these
     * labels.
     *
     * @return
     */
    @Override
    public Set<String> getAllLabels() {
        return hom.getDomain();
    }

}
