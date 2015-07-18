/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.index;

import com.google.common.collect.Iterables;
import de.up.ling.irtg.automata.*;
import de.up.ling.irtg.util.ArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author koller
 */
public class TrieBottomUpRuleIndex extends BottomUpRuleIndex implements Serializable {

    private final IntTrie<Int2ObjectMap<Collection<Rule>>> storedRules;

    public TrieBottomUpRuleIndex() {
//        storedRules = new IntTrie<>(depth -> new ArrayMap<>());
        
        storedRules = new IntTrie<>();
        
        storedRules.setValueCounter(e -> {
            long ret = 0;

            for (Collection<Rule> rules : e.values()) {
                ret += rules.size();
            }

            return ret;
        });
    }

    @Override
    public boolean add(Rule rule) {
        int[] children = rule.getChildren();
        int label = rule.getLabel();
        boolean ret = true;

        Int2ObjectMap<Collection<Rule>> rulesHere = storedRules.get(children);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(children, rulesHere);
        }

        Collection<Rule> knownRules = rulesHere.get(label);

        if (knownRules == null) {
            // no rules known at all for this RHS => always return true
            knownRules = new HashSet<Rule>();
            rulesHere.put(rule.getLabel(), knownRules);
            knownRules.add(rule);
        } else {
            // some rules were known for this RHS => return false if the new rule is new
            ret = !knownRules.add(rule);  // add returns true iff rule is new
        }

        return ret;
    }

    @Override
    public void put(Collection<Rule> rules, int labelId, int[] childStates) {
        Int2ObjectMap<Collection<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(childStates, rulesHere);
        }

        rulesHere.put(labelId, rules);
    }

    @Override
    public Collection<Rule> get(int labelId, int[] childStates) {
        Int2ObjectMap<Collection<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            return null;
        } else {
            return rulesHere.get(labelId);
        }
    }

    @Deprecated
    IntTrie<Int2ObjectMap<Collection<Rule>>> getTrie() {
        return storedRules;
    }

    @Override
    public Iterable<Rule> getAllRules() {
        List<Iterable<Rule>> ruleSets = new ArrayList<Iterable<Rule>>();

        storedRules.foreach(entry -> {
            ruleSets.addAll(entry.values());
        });

        return Iterables.concat(ruleSets);
    }

    @Override
    public void printStatistics() {
        storedRules.printStatistics();
    }

}
