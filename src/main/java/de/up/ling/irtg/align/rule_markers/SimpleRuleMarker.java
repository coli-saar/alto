/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.rule_markers;

import de.up.ling.irtg.align.RuleMarker;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * A basic implementation of the rule marker interface where each pair of aligned rules has to be
 * specified independently.
 * 
 * @author christoph_teichmann
 */
public class SimpleRuleMarker implements RuleMarker {
    /**
     * The prefix used to encode label sets
     */
    private final String prefix;
    
    /**
     * The pattern used to identify encoded label sets.
     */
    private final Pattern patt;
    
    /**
     * Counter for giving numbers to alignment pairs. 
     */
    private final AtomicInteger num = new AtomicInteger(0);
    
    /**
     * Rules from the first automaton mapped to numbers.
     */
    private final Map<Rule,IntSet> firstRules = new HashMap<>();
    
    /**
     * Rules from the second automaton mapped to numbers.
     */
    private final Map<Rule,IntSet> secondRules = new HashMap<>();
    
    /**
     * Create a new instance that will use prefix+"_"+Intset.tostring() to encode alignment sets.
     * 
     * @param prefix 
     */
    public SimpleRuleMarker(String prefix) {
        this.prefix = prefix+"_";
        this.patt = Pattern.compile(prefix+"_.+");
    }

    /**
     * Add a pair of r1 for the first automaton and r2 for the second.
     * 
     * @param r1
     * @param r2 
     */
    public void addPair(Rule r1, Rule r2){
        int marker = num.getAndIncrement();
        
        IntSet is = firstRules.get(r1);
        if(is == null){
            is = new IntOpenHashSet();
            firstRules.put(r1, is);
        }
        is.add(marker);
        
        is = secondRules.get(r2);
        if(is == null){
            is = new IntOpenHashSet();
            secondRules.put(r2, is);
        }
        is.add(marker);
    }

    @Override
    public String makeCode(IntSet alignments, TreeAutomaton original, int state) {
        String code = this.prefix+alignments.toString();
        return code;
    }

    @Override
    public boolean checkCompatible(String label1, String label2) {
        return label1.equals(label2);
    }

    @Override
    public boolean isFrontier(String label) {
        return this.patt.matcher(label).matches();
    }

    @Override
    public String getCorresponding(String variable) {
        return variable;
    }

    @Override
    public RuleEvaluator<IntSet> ruleMarkings(int num) {
        return new SimpleEvaluator(num);
    }

    @Override
    public IntSet getMarkings(int num, Rule r) {
        IntSet i = null;
        switch(num){
            case 0:
                i =  this.firstRules.get(r);
                if(i == null){
                    i = new IntOpenHashSet();
                    this.firstRules.put(r, i);
                }
                
                break;
            case 1:
                i = this.secondRules.get(r);
                if(i == null){
                    i = new IntOpenHashSet();
                    this.secondRules.put(r, i);
                }
                
                break;
            default:
                throw new IllegalArgumentException("Only carries rules for automaton 0 or 1");
        }
        
        return i;
    }
    
    /**
     * An evaluator that maps rules to IntSets, needs to be aware of the automaton it is
     * working on.
     */
    public class SimpleEvaluator implements RuleEvaluator<IntSet>{

        /**
         * The number of the relevant automaton.
         */
        private final int num;

        /**
         * Create a new instance.
         * 
         * @param num
         */
        public SimpleEvaluator(int num) {
            this.num = num;
        }
        
        @Override
        public IntSet evaluateRule(Rule rule) {
            return getMarkings(num,rule);
        }   
    }
}