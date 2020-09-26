// Copyright 2012-2015 Alexander Koller
// Copyright 2017 Christoph Teichmann
// Copyright 2019 Arne Köhn
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.up.ling.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * An InverseHomAutomaton computes the inverse of a homomorphism hom
 * for a whole tree automaton.  Given a homomorphism hom from
 * a signature A to a signature B, and a TreeAutomaton T over B,
 * InverseHomAutomaton is a TreeAutomaton that generates exactly those
 * trees d over A for which hom(d) ∈ T.
 */
public class InverseHomAutomaton<State> extends TreeAutomaton<Object> {

    private static final long serialVersionUID = 8577758932160718432L;
	public static final String FAIL_STATE = "q_FAIL_";
    private final int failStateId;
    private final TreeAutomaton<State> rhsAutomaton;
    private final Homomorphism hom;
    private final Set<Integer> computedLabels;
    private int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private final ToIntFunction<HomomorphismSymbol> remappingHomSymbolToIntFunction;
    private final IntSet provisionalStateSet;

    public InverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());

        // Below, we need to enumerate the states of the rhsAutomaton. This can
        // only be done easily if it supports top-down queries. If it does not,
        // we convert it into a concrete tree automaton here.  This is costly,
        // but InverseHomAutomaton needs to be fixed anyway.
        if( rhsAutomaton.supportsTopDownQueries() ) {
            this.rhsAutomaton = rhsAutomaton;
        } else {
            this.rhsAutomaton = rhsAutomaton.asConcreteTreeAutomatonBottomUp();
        }

        this.hom = hom;

        labelsRemap = hom.getTargetSignature().remap(this.rhsAutomaton.getSignature());
        
        // TODO replace by sig mapper
        remappingHomSymbolToIntFunction = f -> labelsRemap[HomomorphismSymbol.getHomSymbolToIntFunction().applyAsInt(f)];

        computedLabels = new HashSet<>();

        this.stateInterner = (Interner) this.rhsAutomaton.stateInterner;
        this.allStates = new IntOpenHashSet(this.rhsAutomaton.getAllStates());

        finalStates.addAll(this.rhsAutomaton.getFinalStates());
        failStateId = addState(FAIL_STATE);

        // Record a provisional set of states, which is sure to be a
        // superset of all the states that are used in the invhom automaton.
        // This is necessary to avoid calling getAllStates in getRulesTopDown
        // (i.e. at a time before the automaton has been made explicit).
        provisionalStateSet = new IntOpenHashSet();
        provisionalStateSet.addAll(this.rhsAutomaton.getAllStates());
        provisionalStateSet.add(failStateId);
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, final int[] childStates) {
        // lazy bottom-up computation of bottom-up rules
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule> ret = new HashSet<>();

            // run RHS automaton on given child states
            IntIterable resultStates = rhsAutomaton.run(hom.get(label), remappingHomSymbolToIntFunction, tree -> {
                if (tree.getLabel().isVariable()) {
                    int child = childStates[tree.getLabel().getValue()]; // -> i-th child state (= this state ID)

                    if (child == failStateId) {
                        // Hom is requesting the value of a variable, but the state we
                        // got passed bottom-up is FAIL. In this case, the run of the automaton
                        // on the hom image should fail as well. We do this in a slightly hacky
                        // way by simply leaving the variable in place and hoping that things like
                        // "?x1" are not terminal symbols of the automaton.
                        return 0;
                    } else {
                        return child;
                    }
                } else {
                    return 0;
                }
            });

            if (!resultStates.iterator().hasNext()) {
                // no successful runs found, add rule with FAIL parent
                // TODO weight??
                Rule rule = createRule(failStateId, label, childStates, 1);
                storeRuleBottomUp(rule);
                ret.add(rule);
            } else {
                // found successful runs, add rules with ordinary parents
                for (int r : resultStates) {
                    // TODO: weight
                    Rule rule = createRule(r, label, childStates, 1);
                    storeRuleBottomUp(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }

//    private boolean containsFailedState(List<String> states) {
//        for (String child : states) {
//            if (FAIL_STATE.equals(child)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        if (FAIL_STATE.equals(parentState)) {
            makeFailRulesExplicit(label);
        } else {
            if (!computedLabels.contains(label)) {
                computeRulesForLabel(label);
            }
        }

        return getRulesTopDownFromExplicit(label, parentState);
    }

    private void makeFailRulesExplicit(int label) {
        int arity = signature.getArity(label);
        List<Set<Integer>> listOfStateSets = new ArrayList<>();

        for (int i = 0; i < arity; i++) {
            listOfStateSets.add(provisionalStateSet);
        }

        CartesianIterator<Integer> it = new CartesianIterator<>(listOfStateSets);
        while (it.hasNext()) {
            List<Integer> children = it.next();
            // TODO: weight??
            storeRuleBoth(createRule(failStateId, label, children, 1));
        }
    }

    private void computeRulesForLabel(int label) {
        final Tree<HomomorphismSymbol> rhsTree = hom.get(label);

        if (rhsTree != null) {
            Set<Item> rootItems = rhsTree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Set<Item>>() {
                @Override
                public Set<Item> combine(Tree<HomomorphismSymbol> node, List<Set<Item>> childrenValues) {
                    Set<Item> ret = new HashSet<>();

                    // BUG - not all states are necessarily known at this point, so
                    // some rules are not found
                    if (node.getLabel().isVariable()) {
                        for (int state : rhsAutomaton.getAllStates()) {
                            ret.add(new Item(state, subst(node.getLabel(), state)));
                        }
                    } else {
                        CartesianIterator<Item> it = new CartesianIterator<>(childrenValues);

                        while (it.hasNext()) {
                            List<Item> childItems = it.next();
                            List<Integer> childStates = new ArrayList<>();
                            List<Map<HomomorphismSymbol, Integer>> childSubsts = new ArrayList<>();

                            for (Item item : childItems) {
                                childStates.add(item.state);
                                childSubsts.add(item.substitution);
                            }

                            Iterable<Rule> rules = rhsAutomaton.getRulesBottomUp(labelsRemap[node.getLabel().getValue()], childStates);

                            if (rules.iterator().hasNext()) {
                                Map<HomomorphismSymbol, Integer> subst = mergeSubstitutions(childSubsts);
                                for (Rule r : rules) {
                                    ret.add(new Item(r.getParent(), subst));
                                }
                            }
                        }
                    }

                    return ret;
                }
            });

            for (Item rootItem : rootItems) {
                List<List<Integer>> optionsForEachChild = new ArrayList<>();

                for (int i = 0; i < signature.getArity(label); i++) {
                    HomomorphismSymbol sov = HomomorphismSymbol.createVariable("?" + (i + 1));
                    List<Integer> optionsForThisChild = new ArrayList<>();

                    if (rootItem.substitution.containsKey(sov)) {
                        optionsForThisChild.add(rootItem.substitution.get(sov));
                    } else {
                        // for variables that didn't occur in the homomorphic image,
                        // we can set arbitrary states
                        optionsForThisChild.addAll(provisionalStateSet);
                    }

                    optionsForEachChild.add(optionsForThisChild);
                }

                CartesianIterator<Integer> optionsIterator = new CartesianIterator<>(optionsForEachChild);
                while (optionsIterator.hasNext()) {
                    // TODO weights
                    storeRuleTopDown(createRule(rootItem.state, label, optionsIterator.next(), 1));
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

        public int state;
        public Map<HomomorphismSymbol, Integer> substitution;

        public Item(int state, Map<HomomorphismSymbol, Integer> substitution) {
            this.state = state;
            this.substitution = substitution;
        }

        @Override
        public String toString() {
            return InverseHomAutomaton.this.getStateForId(state).toString() + substitution;
        }
    }

    private Map<HomomorphismSymbol, Integer> subst(HomomorphismSymbol sov, int state) {
        Map<HomomorphismSymbol, Integer> ret = new HashMap<>();
        ret.put(sov, state);
        return ret;
    }

    private Map<HomomorphismSymbol, Integer> mergeSubstitutions(List<Map<HomomorphismSymbol, Integer>> substs) {
        Map<HomomorphismSymbol, Integer> ret = new HashMap<>();

        for (Map<HomomorphismSymbol, Integer> subst : substs) {
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

    public static boolean isFailedRule(Rule rule, TreeAutomaton auto) {
        if (auto.getStateForId(rule.getParent()).toString().contains(InverseHomAutomaton.FAIL_STATE)) {
            return true;
        }

        for (int child : rule.getChildren()) {
            if (auto.getStateForId(child).toString().contains(InverseHomAutomaton.FAIL_STATE)) {
                return true;
            }
        }

        return false;
    }
}
