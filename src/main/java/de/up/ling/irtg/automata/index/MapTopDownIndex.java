/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.index;

import com.google.common.collect.Iterables;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.ArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A top-down rule index that organizes the rules in a two-level hashmap.
 *
 * @author koller
 */
public class MapTopDownIndex implements TopDownRuleIndex, Serializable {

    private TreeAutomaton auto;
    private Int2ObjectMap<Int2ObjectMap<Set<Rule>>> explicitRulesTopDown;              // parent -> label -> set(rules)
    private List<Rule> unprocessedUpdatesForTopDown;

    public MapTopDownIndex(TreeAutomaton auto) {
        explicitRulesTopDown = new ArrayMap<Int2ObjectMap<Set<Rule>>>();
        unprocessedUpdatesForTopDown = new ArrayList<Rule>();
        this.auto = auto;
    }

    @Override
    public void add(Rule rule) {
        unprocessedUpdatesForTopDown.add(rule);
    }

    private void processNewTopDownRules() {
        if (!unprocessedUpdatesForTopDown.isEmpty()) {
            unprocessedUpdatesForTopDown.forEach(rule -> {
                Int2ObjectMap<Set<Rule>> topdownHere = explicitRulesTopDown.get(rule.getParent());
                if (topdownHere == null) {
                    topdownHere = new Int2ObjectOpenHashMap<Set<Rule>>();
                    explicitRulesTopDown.put(rule.getParent(), topdownHere);
                }

                Set<Rule> rulesHere = topdownHere.get(rule.getLabel());
                if (rulesHere == null) {
                    rulesHere = new HashSet<Rule>();
                    topdownHere.put(rule.getLabel(), rulesHere);
                }

                rulesHere.add(rule);
            });

            unprocessedUpdatesForTopDown.clear();

            if (TreeAutomaton.DEBUG_STORE) {
                System.err.println("processed rules, now:");
                System.err.println(this);
            }
        }
    }

    @Override
    public Iterable<Rule> getRules(int parentState) {
        processNewTopDownRules();
        Int2ObjectMap<Set<Rule>> topdown = explicitRulesTopDown.get(parentState);

        if (topdown == null) {
            return Collections.emptyList();
        } else {
            return Iterables.concat(topdown.values());
        }
    }

    @Override
    public IntIterable getLabelsTopDown(int parentState) {
        processNewTopDownRules();
        Int2ObjectMap<Set<Rule>> topdown = explicitRulesTopDown.get(parentState);

        if (topdown == null) {
            return IntLists.EMPTY_LIST;
        } else {
            return topdown.keySet();
        }
    }

    @Override
    public Iterable<Rule> getRules(int labelId, int parentState) {
        processNewTopDownRules();

//        System.err.println("grtde " + getStateForId(parentState) + "/" + signature.resolveSymbolId(labelId));
//        System.err.println("  -> cached: " + useCachedRuleTopDown(labelId, parentState));
        if (useCachedRule(labelId, parentState)) {
            Int2ObjectMap<Set<Rule>> topdownHere = explicitRulesTopDown.get(parentState);

            if (topdownHere != null) {
                Set<Rule> rulesHere = topdownHere.get(labelId);

                if (rulesHere != null) {
                    return rulesHere;
                }
            }

            return Collections.emptyList();

//            
//            
//            SetMultimap<Integer, Rule> rulesHere = explicitRulesTopDown.get(labelId);
//
//            if (rulesHere != null) {
//                Set<Rule> ret = rulesHere.get(parentState);
//                if (ret != null) {
//                    return ret;
//                }
//            }
        } else {
            return Collections.emptyList();
        }

//        return new HashSet<Rule>();
    }

    @Override
    public boolean useCachedRule(int label, int parent) {
        processNewTopDownRules();

        Int2ObjectMap<Set<Rule>> topdown = explicitRulesTopDown.get(parent);
        if (topdown != null) {
            return topdown.containsKey(label);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Signature sig = auto.getSignature();
        Interner intr = auto.getStateInterner();

        for (int parent : explicitRulesTopDown.keySet()) {
            Int2ObjectMap<Set<Rule>> rulesHere = explicitRulesTopDown.get(parent);
            for (int label : rulesHere.keySet()) {
                Set<Rule> rules = rulesHere.get(label);

                buf.append("rules for " + intr.resolveId(parent).toString() + " -> " + sig.resolveSymbolId(label) + "(...)\n");
                for (Rule rule : rules) {
                    buf.append(" - " + rule.toString(auto) + "\n");
                }
                buf.append("\n");
            }
        }

        buf.append("unprocessed rules:\n");
        for (Rule rule : unprocessedUpdatesForTopDown) {
            buf.append(" - " + rule.toString(auto) + "\n");
        }

        return buf.toString();
    }

}
