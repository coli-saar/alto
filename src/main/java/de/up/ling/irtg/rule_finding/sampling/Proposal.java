/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Date;
import java.util.function.BiFunction;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well44497a;

/**
 *
 * @author christoph
 */
public class Proposal {
    /**
     * 
     */
    private final RandomGenerator rg;
    
    
    public Proposal(RandomGenerator rg) {
        this.rg = rg;
    }
    
    /**
     * 
     * @param seed 
     */
    public Proposal(long seed) {
        this(new Well44497a(seed));
    }
    
    /**
     * 
     */
    public Proposal() {
        this(new Date().getTime());
    }
    
    /**
     * 
     */
    private static final BiFunction<Rule,TreeAutomaton,Integer> RAW_MAPPING = (Rule r, TreeAutomaton t) -> r.getLabel();
    
    /**
     * 
     * @param carrier
     * @param guide
     * @return 
     */
    public TreeSample<Integer> getRawTreeSample(TreeAutomaton carrier, RuleWeighting guide) {
       return this.getTreeSample(RAW_MAPPING, carrier, guide);
    }
    
    /**
     * 
     */
    private static final BiFunction<Rule,TreeAutomaton,Rule> RULE_MAPPING = (Rule r, TreeAutomaton t) -> r;
    
    /**
     * 
     * @param carrier
     * @param guide
     * @return 
     */
    public TreeSample<Rule> getRuleTreeSample(TreeAutomaton carrier, RuleWeighting guide) {
        return this.getTreeSample(RULE_MAPPING, carrier, guide);
    }
    
    /**
     * 
     */
    private final static BiFunction<Rule,TreeAutomaton, String> STRING_MAPPING =
                                (Rule rule, TreeAutomaton t) -> t.getSignature().resolveSymbolId(rule.getLabel());
    
    /**
     * 
     * @param carrier
     * @param guide
     * @return 
     */
    public TreeSample<String> getStringTreeSample(TreeAutomaton carrier, RuleWeighting guide) {
        return this.getTreeSample(STRING_MAPPING, carrier, guide);
    }
    
    /**
     * 
     * @param <Type>
     * @param mapping
     * @param carrier
     * @param guide
     * @return 
     */
    public <Type> TreeSample<Type> getTreeSample(BiFunction<Rule,TreeAutomaton,Type> mapping,
                                                    TreeAutomaton carrier, RuleWeighting guide) {
        //TODO
        return null;
    }
    
}
