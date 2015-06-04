/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.util.ArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 *
 * @author koller
 */
public class TrieRuleCache implements RuleCache {
    private final IntTrie<Int2ObjectMap<Iterable<Rule>>> storedRules;

    public TrieRuleCache() {
        storedRules = new IntTrie<>(depth -> new ArrayMap<>());
    }

    @Override
    public Iterable<Rule> put(Iterable<Rule> rules, int labelId, int[] childStates) {
         Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);

        if (rulesHere == null) {
            rulesHere = new Int2ObjectOpenHashMap<>();
            storedRules.put(childStates, rulesHere);
        }
         rulesHere.put(labelId, rules);
         
         return rules;
    }

    @Override
    public Iterable<Rule> get(int labelId, int[] childStates) {
        Int2ObjectMap<Iterable<Rule>> rulesHere = storedRules.get(childStates);
        
        if( rulesHere == null ) {
            return null;
        } else {
            return rulesHere.get(labelId);
        }
    }
}
