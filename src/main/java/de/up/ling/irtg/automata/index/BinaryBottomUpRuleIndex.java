/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.index;

import com.google.common.collect.Iterables;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.NumbersCombine;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class BinaryBottomUpRuleIndex extends BottomUpRuleIndex implements Serializable {

    private final Int2ObjectMap<Collection<Rule>> storedRulesNullary;   // label -> rules
    private final Long2ObjectMap<Collection<Rule>> storedRulesUnary;    // child|label -> rules
    private final Int2ObjectMap<Long2ObjectMap<Collection<Rule>>> storedRulesBinary; // label -> child1|child2 -> rules
    private final TreeAutomaton auto;

    public BinaryBottomUpRuleIndex(TreeAutomaton auto) {
        storedRulesNullary = new ArrayMap<>();
        storedRulesUnary = new Long2ObjectOpenHashMap<>();
        storedRulesBinary = new Int2ObjectOpenHashMap<>();
        this.auto = auto;
    }
    
    @Override
    public void setRules(Collection<Rule> rules, int labelId, int[] childStates) {
        long key;

//        System.err.println("put: " + Util.mapToList(rules, rule -> rule.toString(auto)));
        switch (childStates.length) {
            case 0:
                storedRulesNullary.put(labelId, rules);
                break;

            case 1:
                key = NumbersCombine.combine(childStates[0], labelId);
                storedRulesUnary.put(key, rules);
                break;

            case 2:
                Long2ObjectMap<Collection<Rule>> rulesHere = storedRulesBinary.get(labelId);
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
    }

    @Override
    public Collection<Rule> get(int labelId, int[] childStates) {
        long key;

        switch (childStates.length) {
            case 0:
                return storedRulesNullary.get(labelId);

            case 1:
                key = NumbersCombine.combine(childStates[0], labelId);
                return storedRulesUnary.get(key);

            case 2:
                Long2ObjectMap<Collection<Rule>> rulesHere = storedRulesBinary.get(labelId);

                if (rulesHere == null) {
                    return null;
                } else {
                    key = NumbersCombine.combine(childStates[0], childStates[1]);
                    return rulesHere.get(key);
                }

            default:
                throw new RuntimeException("using label with arity > 2");
        }
    }

    @Override
    public Iterable<Rule> getAllRules() {
        final List<Iterable<Rule>> allRules = new ArrayList<>();

//        System.err.println("getAllRules, rules are:");
//        System.err.println(this);
        allRules.addAll(storedRulesNullary.values());
        allRules.addAll(storedRulesUnary.values());

        storedRulesBinary.forEach((key, map) -> {
            allRules.addAll(map.values());
        });

        return Iterables.concat(allRules);
    }

    private String st(int q) {
        return auto.getStateForId(q).toString();
    }

    private String lb(int label) {
        return auto.getSignature().resolveSymbolId(label);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("nullary rules:\n");
        storedRulesNullary.forEach((k, v) -> {
            buf.append(lb(k) + ": " + Util.mapToList(v, rule -> rule.toString(auto)) + "\n");
        });

        buf.append("\nunary rules:\n");
        storedRulesUnary.forEach((k, v) -> {
            int child = NumbersCombine.getFirst(k);
            int label = NumbersCombine.getSecond(k);

            buf.append(lb(label) + "(" + st(child) + "): " + Util.mapToList(v, rule -> rule.toString(auto)) + "\n");
        });

        buf.append("\nbinary rules:\n");
        storedRulesBinary.forEach((k, v) -> {
            v.forEach((k2, v2) -> {
                int label = k;
                int ch1 = NumbersCombine.getFirst(k2);
                int ch2 = NumbersCombine.getSecond(k2);

                buf.append(lb(label) + "(" + st(ch1) + "," + st(ch2) + "): " + Util.mapToList(v2, rule -> rule.toString(auto)) + "\n");
            });
        });

        return buf.toString();
    }

    @Override
    public void foreachRuleForSets(IntSet labelIds, List<IntSet> childStateSets, SignatureMapper signatureMapper, Consumer<Rule> fn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
