/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.signature.IdentitySignatureMapper;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Determinizes an unweighted tree automaton.
 *
 * @author koller
 */
public class Determinizer<State> {
    private TreeAutomaton<State> originalAutomaton;
    private IntTrie<Integer> stateListToNewState = new IntTrie<>();
    private int nextNewState = 1;
    private List<IntSet> allStateLists = new ArrayList<>();
    private IntList allNewStates = new IntArrayList();
    private IntSet allSymbolIds = new IntOpenHashSet();
    private SignatureMapper ism;

    public Determinizer(TreeAutomaton<State> originalAutomaton) {
        this.originalAutomaton = originalAutomaton;
        ism = new IdentitySignatureMapper(originalAutomaton.getSignature());
        allStateLists.add(null); // to ensure allStateLists.get(newState) == states represented by newState
    }

    public TreeAutomaton<Set<State>> determinize() {
        ConcreteTreeAutomaton<Set<State>> ret = new ConcreteTreeAutomaton<>(originalAutomaton.getSignature());
        Queue<Integer> stateSetAgenda = new ArrayDeque<>();
        int maxArity = 0;
        List<IntSet> rhsStateLists = new ArrayList<>();
        IntSet previouslyIterated = new IntOpenHashSet();

        ret.stateInterner.setTrustingMode(true);

        // initialize bottom-up
        for (int sym = 1; sym <= originalAutomaton.getSignature().getMaxSymbolId(); sym++) {
            int arity = originalAutomaton.getSignature().getArity(sym);
            maxArity = Math.max(maxArity, arity);

            allSymbolIds.add(sym);

            if (arity == 0) {
                IntSet parents = collectParents(originalAutomaton.getRulesBottomUp(sym, Collections.EMPTY_LIST));
                int newState = getNewState(parents, ret);
                stateSetAgenda.offer(newState);
                previouslyIterated.add(newState);
                ret.addRule(new Rule(newState, sym, new int[0], 1));
            }
        }

        // iterate over agenda
        while (!stateSetAgenda.isEmpty()) {
            int newState = stateSetAgenda.remove();
            List<Integer> newStateAsSingleton = Collections.singletonList(newState);

            for (int ar = 1; ar <= maxArity; ar++) {           // arity of rule
                for (int myPos = 0; myPos < ar; myPos++) {     // position of newState in arity list
                    final int _myPos = myPos;
                    List<List<Integer>> childLists = Util.makeList(ar, i -> i == _myPos ? newStateAsSingleton : allNewStates);

                    CartesianIterator<Integer> it = new CartesianIterator<>(childLists);
                    while (it.hasNext()) {
                        List<Integer> rhsNewStates = it.next();           // list of states in new automaton
                        lookupStateLists(rhsNewStates, rhsStateLists);    // list of state-sets in old automaton
                        final int[] aRhsNewStates = makeArray(rhsNewStates);

                        // collect mappings f -> {q1, ..., qn} for all rules qi -> f(RHS)
                        final Int2ObjectMap<IntSet> labelsToParents = new Int2ObjectOpenHashMap<>();
                        originalAutomaton.foreachRuleBottomUpForSets(allSymbolIds, rhsStateLists, ism, rule -> {
                            IntSet parents = labelsToParents.get(rule.getLabel());
                            if (parents == null) {
                                parents = new IntOpenHashSet();
                            }

                            parents.add(rule.getParent());
                            labelsToParents.put(rule.getLabel(), parents);
                        });

                        // create rules for all the LHSs we found
                        FastutilUtils.forEach(labelsToParents.keySet(), label -> {
                            int parent = getNewState(labelsToParents.get(label), ret);
                            ret.addRule(new Rule(parent, label, aRhsNewStates, 1));

                            // NB this may add the same rule multiple times, with the "newState"
                            // in different child positions
                            if (!previouslyIterated.contains(parent)) {
                                stateSetAgenda.offer(parent);
                                previouslyIterated.add(parent);
                            }
                        });
                    }
                }
            }
        }

        return ret;
    }

    private static int[] makeArray(List<Integer> values) {
        int[] ret = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            ret[i] = values.get(i);
        }
        return ret;
    }

    private void lookupStateLists(List<Integer> newStates, List<IntSet> ret) {
        ret.clear();
        newStates.forEach(q -> ret.add(allStateLists.get(q)));
    }

    private int getNewState(IntSet states, TreeAutomaton<Set<State>> auto) {
        int[] aStates = states.toIntArray(); // check that these are always sorted in the same way
        Integer found = stateListToNewState.get(aStates);

        if (found != null) {
            return found;
        } else {
            int x = nextNewState++;
            stateListToNewState.put(aStates, x);
            auto.stateInterner.addObjectWithIndex(x, mapStates(states));
            allStateLists.add(states);
            allNewStates.add(x);

            if (!FastutilUtils.isDisjoint(states, originalAutomaton.getFinalStates())) {
                auto.addFinalState(x);
            }

            return x;
        }
    }

    private Set<State> mapStates(IntSet stateIds) {
        return Util.mapSet(stateIds, q -> originalAutomaton.getStateForId(q));
    }

    // returns sorted, duplicate-free list of parents of the rules
    private IntSet collectParents(Iterable<Rule> rules) {
        final IntSortedSet set = new IntRBTreeSet();
        rules.forEach(rule -> set.add(rule.getParent()));
        return set;
    }
}
