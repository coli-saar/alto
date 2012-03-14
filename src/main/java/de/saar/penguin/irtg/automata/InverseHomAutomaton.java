package de.saar.penguin.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.hom.Homomorphism;
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
class InverseHomAutomaton<State> extends BottomUpAutomaton<State> {
    private BottomUpAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
    private Set<String> computedLabels;

    public InverseHomAutomaton(BottomUpAutomaton<State> rhsAutomaton, Homomorphism hom) {
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
        computedLabels = new HashSet<String>();
    }

    @Override
    public Set<State> getFinalStates() {
        return rhsAutomaton.getFinalStates();
    }

    @Override
    public Set<Rule<State>> getRulesBottomUp(String label, final List<State> childStates) {
        // lazy bottom-up computation of bottom-up rules
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule<State>> ret = new HashSet<Rule<State>>();
            Map<String, StringOrVariable> varmap = hom.getVariableMap(label);
            Map<String, State> statemap = new HashMap<String, State>();

            if (new HashSet<StringOrVariable>(varmap.values()).size() == childStates.size()) {
                for (String node : varmap.keySet()) {
                    statemap.put(node, childStates.get(Homomorphism.getIndexForVariable(varmap.get(node))));
                }

                Set<State> resultStates = rhsAutomaton.run(hom.get(label), statemap);
                for (State r : resultStates) {
                    Rule<State> rule = new Rule<State>(r, label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }

    @Override
    public Set<Rule<State>> getRulesTopDown(String label, State parentState) {
        if (!computedLabels.contains(label)) {
            computeRulesForLabel(label);
        }

        return getRulesTopDownFromExplicit(label, parentState);
    }

    private void computeRulesForLabel(String label) {
        final Tree<StringOrVariable> rhsTree = hom.get(label);

        if (rhsTree != null) {

            Set<Item> rootItems = rhsTree.dfs(new TreeVisitor<Void, Set<Item>>() {
                @Override
                public Set<Item> combine(String node, List<Set<Item>> childrenValues) {
                    Set<Item> ret = new HashSet<Item>();

                    // BUG - not all states are necessarily known at this point, so
                    // some rules are not found
                    if (rhsTree.getLabel(node).isVariable()) {
                        for (State state : getAllStates()) {
                            ret.add(new Item(state, subst(rhsTree.getLabel(node), state)));
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

                            Set<Rule<State>> rules = rhsAutomaton.getRulesBottomUp(rhsTree.getLabel(node).toString(), childStates);

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
                List<State> childStates = new ArrayList<State>();

                for (int i = 0; i < hom.getArity(label); i++) {
                    // TODO - deal with unbound variables here!!
                    childStates.add(rootItem.substitution.get(new StringOrVariable("?" + (i + 1), true)));
                }

                storeRule(new Rule<State>(rootItem.state, label, childStates));
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

    @Override
    public Set<State> getAllStates() {
        return rhsAutomaton.getAllStates();
    }


    /**
     * Returns the set of labels in the domain of the homomorphism.
     * The actual inverse homomorphism automaton may not contain
     * rules for all of these labels.
     * 
     * @return 
     */
    @Override
    public Set<String> getAllLabels() {
        return hom.getDomain();
    }
}

