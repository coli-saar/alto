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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A top-down rule index that organizes the rules in a two-level hashmap.
 *
 * @author koller
 */
public class MapTopDownIndex implements TopDownRuleIndex, Serializable {

    private TreeAutomaton auto;
    private Int2ObjectMap<Int2ObjectMap<Set<Rule>>> explicitRulesTopDown;              // parent -> label -> set(rules)
    private Int2ObjectMap<List<Rule>> explicitRulesTopDownByParent;                    // parent -> list(rule), for all labels, list contains no duplicates
    private List<Rule> unprocessedUpdatesForTopDown;

    public MapTopDownIndex(TreeAutomaton auto) {
        explicitRulesTopDown = new ArrayMap<>();
        explicitRulesTopDownByParent = new ArrayMap<>();
        unprocessedUpdatesForTopDown = new ArrayList<>();
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
                    topdownHere = new Int2ObjectOpenHashMap<>();
                    explicitRulesTopDown.put(rule.getParent(), topdownHere);
                }

                Set<Rule> rulesHere = topdownHere.get(rule.getLabel());
                if (rulesHere == null) {
                    rulesHere = new HashSet<>();
                    topdownHere.put(rule.getLabel(), rulesHere);
                }

                if( rulesHere.add(rule) ) {
                    // added a rule we didn't know before => update by-parent index
                    List<Rule> byParentRulesHere = explicitRulesTopDownByParent.get(rule.getParent());
                    
                    if( byParentRulesHere == null ) {
                        byParentRulesHere = new ArrayList<>();
                        explicitRulesTopDownByParent.put(rule.getParent(), byParentRulesHere);
                    }
                    
                    byParentRulesHere.add(rule);
                }
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
//        Int2ObjectMap<Set<Rule>> topdown = explicitRulesTopDown.get(parentState);
        List<Rule> topdown = explicitRulesTopDownByParent.get(parentState);

        if (topdown == null) {
            return Collections.emptyList();
        } else {
//            return Iterables.concat(topdown.values());
            return topdown;
        }
    }

    @Override
    public void foreachRule(int parentState, Consumer<Rule> fn) {
        processNewTopDownRules();
        
        List<Rule> rules = explicitRulesTopDownByParent.get(parentState);
        
        if( rules != null ) {
            rules.forEach(fn);
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

        if (useCachedRule(labelId, parentState)) {
            Int2ObjectMap<Set<Rule>> topdownHere = explicitRulesTopDown.get(parentState);

            if (topdownHere != null) {
                Set<Rule> rulesHere = topdownHere.get(labelId);

                if (rulesHere != null) {
                    return rulesHere;
                }
            }

            return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
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

    private Iterable<Rule> concatInnerIterables(Int2ObjectMap<Set<Rule>> map) {
        return Iterables.concat(map.values());
    }

    @Override
    public Iterable<Rule> getAllRules() {
        processNewTopDownRules();
        Collection<Iterable<Rule>> iterables = explicitRulesTopDown.values().stream().map(this::concatInnerIterables).collect(Collectors.toList());
        return Iterables.concat(iterables);
    }

}
