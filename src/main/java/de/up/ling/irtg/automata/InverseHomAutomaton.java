package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
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
            String normalized = addState(s.toString());
            rhsState.put(normalized, s);
        }
        addState(FAIL_STATE);
    }

    @Override
    public Set<Rule<String>> getRulesBottomUp(String label, final List<String> childStates) {
        // lazy bottom-up computation of bottom-up rules
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();
            
//            System.err.println("\nrun on hom(" + label + "), children " + childStates);

            // run RHS automaton on given child states
            Set<State> resultStates = rhsAutomaton.run(hom.get(label), new Function<Tree<HomomorphismSymbol>, State>() {
                @Override
                public State apply(Tree<HomomorphismSymbol> f) {
//                    System.err.println("    - " + f.getLabel() + " var:" + f.getLabel().isVariable());
                    if (f.getLabel().isVariable()) {
                        String child = childStates.get(Homomorphism.getIndexForVariable(f.getLabel()));
//                        System.err.println("      + child: " + child);

                        if (FAIL_STATE.equals(child)) {
                            // Hom is requesting the value of a variable, but the state we
                            // got passed bottom-up is FAIL. In this case, the run of the automaton
                            // on the hom image should fail as well. We do this in a slightly hacky
                            // way by simply leaving the variable in place and hoping that things like
                            // "?x1" are not terminal symbols of the automaton.
//                            System.err.println("      + is FAIL, return NULL");
                            return null;
                        } else {
//                            System.err.println("      + is not FAIL, return " + rhsState.get(child));
                            return rhsState.get(child);
                        }
                    } else {
                        return null;
                    }
                }
            });
            
//            System.err.println("result states: " + resultStates);

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
    public Set<Rule<String>> getRulesTopDown(String label, String parentState) {
        if (FAIL_STATE.equals(parentState)) {
            makeFailRulesExplicit(label);
        } else {
            if (!computedLabels.contains(label)) {
                computeRulesForLabel(label);
            }
        }

        return getRulesTopDownFromExplicit(label, parentState);
    }

    private void makeFailRulesExplicit(String label) {
        int arity = signature.getArity(label);
        List<Set<String>> listOfStateSets = new ArrayList<Set<String>>();

        for (int i = 0; i < arity; i++) {
            listOfStateSets.add(getAllStates());
        }

        CartesianIterator<String> it = new CartesianIterator<String>(listOfStateSets);
        while (it.hasNext()) {
            List<String> children = it.next();
            storeRule(new Rule<String>(FAIL_STATE, label, children));
        }
    }

    private void computeRulesForLabel(String label) {
        final Tree<HomomorphismSymbol> rhsTree = hom.get(label);

        if (rhsTree != null) {

            Set<Item> rootItems = rhsTree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Set<Item>>() {
                @Override
                public Set<Item> combine(Tree<HomomorphismSymbol> node, List<Set<Item>> childrenValues) {
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
                            List<Map<HomomorphismSymbol, State>> childSubsts = new ArrayList<Map<HomomorphismSymbol, State>>();

                            for (Item item : childItems) {
                                childStates.add(item.state);
                                childSubsts.add(item.substitution);
                            }

                            Set<Rule<State>> rules = rhsAutomaton.getRulesBottomUp(node.getLabel().toString(), childStates);

                            if (!rules.isEmpty()) {
                                Map<HomomorphismSymbol, State> subst = mergeSubstitutions(childSubsts);
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
                List<List<String>> optionsForEachChild = new ArrayList<List<String>>();

                for (int i = 0; i < signature.getArity(label); i++) {
                    HomomorphismSymbol sov = HomomorphismSymbol.createVariable("?" + (i + 1));
                    List<String> optionsForThisChild = new ArrayList<String>();

                    if (rootItem.substitution.containsKey(sov)) {
                        optionsForThisChild.add(rootItem.substitution.get(sov).toString());
                    } else {
                        // for variables that didn't occur in the homomorphic image,
                        // we can set arbitrary states
                        optionsForThisChild.addAll(getAllStates());
                    }

                    optionsForEachChild.add(optionsForThisChild);
                }

                CartesianIterator<String> optionsIterator = new CartesianIterator<String>(optionsForEachChild);
                while (optionsIterator.hasNext()) {
                    storeRule(new Rule<String>(rootItem.state.toString(), label, optionsIterator.next()));
                }
            }

            computedLabels.add(label);


        }
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return rhsAutomaton.isBottomUpDeterministic();
    }


    private class Item {
        public State state;
        public Map<HomomorphismSymbol, State> substitution;

        public Item(State state, Map<HomomorphismSymbol, State> substitution) {
            this.state = state;
            this.substitution = substitution;
        }

        @Override
        public String toString() {
            return state.toString() + substitution;
        }
    }

    private Map<HomomorphismSymbol, State> subst(HomomorphismSymbol sov, State state) {
        Map<HomomorphismSymbol, State> ret = new HashMap<HomomorphismSymbol, State>();
        ret.put(sov, state);
        return ret;
    }

    private Map<HomomorphismSymbol, State> mergeSubstitutions(List<Map<HomomorphismSymbol, State>> substs) {
        Map<HomomorphismSymbol, State> ret = new HashMap<HomomorphismSymbol, State>();

        for (Map<HomomorphismSymbol, State> subst : substs) {
            for (HomomorphismSymbol key : subst.keySet()) {
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

    public static <State> boolean isFailedRule(Rule<State> rule) {
        if( rule.getParent().toString().contains(InverseHomAutomaton.FAIL_STATE) ) {
            return true;
        }
        
        for( State child : rule.getChildren() ) {
            if( child.toString().contains(InverseHomAutomaton.FAIL_STATE)) {
                return true;
            }
        }
        
        return false;
    }
}
