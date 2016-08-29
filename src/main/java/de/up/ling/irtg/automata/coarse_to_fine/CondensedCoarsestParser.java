/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.util.IntAgenda;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class CondensedCoarsestParser<InvhomState> {

    private final RuleRefinementTree coarseGrammar;
    private final CondensedTreeAutomaton<InvhomState> invhom;
    private List<RuleRefinementNode> coarseNodes;
    private List<CondensedRule> partnerInvhomRules;
    public static boolean DEBUG = false;
    Function<RuleRefinementNode,String> rrnToString = x -> x.toString();
    
    void setRrnToString(TreeAutomaton auto) {
        rrnToString = x -> x.localToString(auto);
    }

    public CondensedCoarsestParser(RuleRefinementTree coarseGrammar, CondensedTreeAutomaton<InvhomState> invhom) {
        this.coarseGrammar = coarseGrammar;
        this.invhom = invhom;
    }

    public void parse(List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules) {
        this.coarseNodes = coarseNodes;
        this.partnerInvhomRules = partnerInvhomRules;

        Int2ObjectMap<IntSet> partners = new Int2ObjectOpenHashMap<>();
        IntSet visited = new IntOpenHashSet();
        invhom.getFinalStates().forEach((q) -> {
            // starting the dfs by the final states ensures a topological order
            ckyDfsForStatesInBottomUpOrder(q, visited, partners, 0);
        });
    }

    private void ckyDfsForStatesInBottomUpOrder(int q, IntSet visited, Int2ObjectMap<IntSet> partners, int depth) {
        final IntList foundPartners = new IntArrayList();
        List<CondensedRule> loopRules = new ArrayList<>();

        if (!visited.contains(q)) {
            visited.add(q);

            D(depth, () -> "visit: " + invhom.getStateForId(q));

            for (final CondensedRule rightRule : invhom.getCondensedRulesByParentState(q)) {
                IntTrie<List<RuleRefinementNode>> trieForTermId = coarseGrammar.getCoarsestTrie().step(rightRule.getLabelSetID());

                D(depth, () -> "inv rule: " + rightRule.toString(invhom));
                D(depth, () -> "labelSetId: " + rightRule.getLabelSetID());
                D(depth, () -> "trie found: " + (trieForTermId != null));

                if (trieForTermId != null) {
                    // If the right rule is a "self-loop", i.e. of the form q -> f(q),
                    // the normal DFS doesn't work. We give it special treatment by
                    // postponing its combination with the right rules until below.
                    if (rightRule.isLoop()) {
                        loopRules.add(rightRule);
                        D(depth, () -> "loopy rule: " + rightRule.toString(invhom));

                        // make sure that all non-loopy children have been explored
                        for (int i = 0; i < rightRule.getArity(); i++) {
                            int ch = rightRule.getChildren()[i];
                            if (ch != rightRule.getParent()) {
                                ckyDfsForStatesInBottomUpOrder(ch, visited, partners, depth + 1);
                            }
                        }

                        continue;
                    }

                    int[] rightChildren = rightRule.getChildren();
                    List<IntSet> remappedChildren = new ArrayList<>();

                    // iterate over all children in the right rule
                    for (int i = 0; i < rightRule.getArity(); ++i) {
                        // go into the recursion first to obtain the topological order that is needed for the CKY algorithm
                        ckyDfsForStatesInBottomUpOrder(rightChildren[i], visited, partners, depth + 1);

                        // only add, if a partner state has been found.
                        if (partners.containsKey(rightChildren[i])) {
                            // take the right-automaton label for each child and get the previously calculated left-automaton label from partners.
                            remappedChildren.add(partners.get(rightChildren[i]));
                        }
                    }

                    D(depth, () -> "remappedChildren: " + remappedChildren);

                    // find all rules bottom-up in the left automaton that have the same (remapped) children as the right rule.
                    trieForTermId.foreachValueForKeySets(remappedChildren, rrns -> {
                        for (RuleRefinementNode rrn : rrns) {
                            D(depth, () -> "found rrn: " + rrnToString.apply(rrn));
                            partnerInvhomRules.add(rightRule);
                            coarseNodes.add(rrn);
                            foundPartners.add(rightRule.getParent());
                            foundPartners.add(rrn.getParent());
                        }
                    });

                    // now go through to-do list and add all state pairs to partner sets
                    for (int i = 0; i < foundPartners.size(); i += 2) {
                        addPartner(foundPartners.get(i), foundPartners.get(i + 1), partners);
                    }

                    foundPartners.clear();
                }
            }

            D(depth, () -> "partners of q: " + partners.get(q));

            // Now that we have seen all children of q through rules that
            // are not self-loops, go through the self-loops and process them.
            if (partners.get(q) != null) {
                // If q has no partners, that means we have found no non-loopy
                // rules for expanding it. Thus any loopy expansions will be
                // unproductive, so we can skip them.

                for (CondensedRule rightRule : loopRules) {
                    IntTrie<List<RuleRefinementNode>> trieForTermId = coarseGrammar.getCoarsestTrie().step(rightRule.getLabelSetID());

                    int rightParent = rightRule.getParent();
                    int[] rightChildren = rightRule.getChildren();
                    List<IntSet> leftChildStateSets = new ArrayList<>(rightChildren.length);

                    IntAgenda agenda = new IntAgenda(partners.get(q));  // agenda of coarse states that we have seen with q

                    while (!agenda.isEmpty()) {
                        int leftState = agenda.pop();
                        D(depth, () -> "pop left state: " + leftState);

                        for (int i = 0; i < rightRule.getArity(); i++) {
                            if (rightChildren[i] == rightParent) {
                                // i is a loop position
                                makeLeftChildStateSets(leftChildStateSets, rightChildren, i, leftState, partners);
                                trieForTermId.foreachValueForKeySets(leftChildStateSets, rrns -> {
                                    for (RuleRefinementNode rrn : rrns) {
                                        partnerInvhomRules.add(rightRule);
                                        coarseNodes.add(rrn);

                                        addPartner(rightRule.getParent(), rrn.getParent(), partners);
                                        agenda.enqueue(rrn.getParent());
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

    }

    private void addPartner(int rightState, int leftState, Int2ObjectMap<IntSet> partners) {
        // remember the newly found partneres if needed
        IntSet knownPartners = partners.get(rightState);

        if (knownPartners == null) {
            knownPartners = new IntOpenHashSet();
            partners.put(rightState, knownPartners);
        }

        knownPartners.add(leftState);
    }

    private void makeLeftChildStateSets(List<IntSet> leftChildStateSets, int[] rightChildren, int childPos, int leftStateAtPos, Int2ObjectMap<IntSet> partners) {
        leftChildStateSets.clear();

        for (int i = 0; i < rightChildren.length; i++) {
            if (i == childPos) {
                leftChildStateSets.add(IntSets.singleton(leftStateAtPos));
            } else {
                leftChildStateSets.add(partners.get(rightChildren[i]));
            }
        }
    }

    private void D(int depth, Supplier<String> s) {
        if (DEBUG) {
            System.err.println(Util.repeat("  ", depth) + s.get());
        }
    }
}
