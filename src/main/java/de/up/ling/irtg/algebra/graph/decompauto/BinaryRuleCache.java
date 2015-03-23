/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph.decompauto;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 *
 * @author koller
 */
public class BinaryRuleCache implements RuleCache {
    private final Int2ObjectMap<Iterable<Rule>> storedRulesNullary;   // label -> rules
    private final Long2ObjectMap<Iterable<Rule>> storedRulesUnary;    // child|label -> rules
    private final Int2ObjectMap<Long2ObjectMap<Iterable<Rule>>> storedRulesBinary; // label -> child1|child2 -> rules

    public BinaryRuleCache() {
        storedRulesNullary = new ArrayMap<>();
        storedRulesUnary = new Long2ObjectOpenHashMap<>();
        storedRulesBinary = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public Iterable<Rule> put(Iterable<Rule> rules, int labelId, int[] childStates) {
        long key;

        switch (childStates.length) {
            case 0:
                storedRulesNullary.put(labelId, rules);
                break;

            case 1:
                key = NumbersCombine.combine(childStates[0], labelId);
                storedRulesUnary.put(key, rules);
                break;

            case 2:
                Long2ObjectMap<Iterable<Rule>> rulesHere = storedRulesBinary.get(labelId);
                if (rulesHere == null) {
                    rulesHere = new Long2ObjectOpenHashMap<>();
                    storedRulesBinary.put(labelId, rulesHere);
                }

                key = NumbersCombine.combine(childStates[0], childStates[1]);
                rulesHere.put(key, rules);
                break;

            default:
                throw new RuntimeException("using label with arity > 2");
        }

        return rules;
    }

    @Override
    public Iterable<Rule> get(int labelId, int[] childStates) {
        long key;
        
        switch(childStates.length) {
            case 0:
                return storedRulesNullary.get(labelId);
                
            case 1:
                key = NumbersCombine.combine(childStates[0], labelId);
                return storedRulesUnary.get(key);
                
            case 2:
                Long2ObjectMap<Iterable<Rule>> rulesHere = storedRulesBinary.get(labelId);
                
                if( rulesHere == null ) {
                    return null;
                } else {
                    key = NumbersCombine.combine(childStates[0], childStates[1]);
                    return rulesHere.get(key);
                }
            
            default:
                throw new RuntimeException("using label with arity > 2");
        }
    }

}
