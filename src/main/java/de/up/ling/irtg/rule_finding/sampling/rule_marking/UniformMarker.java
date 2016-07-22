/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.rule_marking;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.RuleMarking;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Assumes that there is exactly one start state.
 *
 * @author christoph_teichmann
 */
public abstract class UniformMarker implements RuleMarking {
    /**
     * 
     */
    private final TreeAutomaton ta;
    
    /**
     * 
     */
    private final Int2ObjectMap<Rule[]> parentRules = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2IntMap marks = new Int2IntOpenHashMap();
    
    /**
     * 
     */
    private final int startState;
    
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     * @param ta
     * @param rg 
     */
    public UniformMarker(TreeAutomaton ta, RandomGenerator rg) {
        this.ta = ta;
        
        startState = ta.getFinalStates().iterator().nextInt();
        
        this.rg = rg;
    }

    @Override
    public IntIterator iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumberOfRules(int parent) {
        Rule[] r = this.ensureRules(parent);
        
        return r.length;
    }

    @Override
    public Rule getRule(int parent, int ruleNumber) {
        Rule[] r = this.ensureRules(parent);
        
        return r[ruleNumber];
    }

    @Override
    public double getInsideChoiceLogProb(Rule r) {
        double d = 0.0;
        
        for(int i=0;i<r.getArity();++i) {
            int state = r.getChildren()[i];
            Rule[] rs = this.ensureRules(state);
            
            d -= Math.log(rs.length);
            
            int mark = this.marks.get(state);
            d += this.getInsideChoiceLogProb(rs[mark]);
        }
        
        return d;
    }

    @Override
    public void setRule(int parent, int ruleNumber) {
        this.marks.put(parent, ruleNumber);
    }

    @Override
    public Tree<Rule> getCurrentTree() {
        return this.getCurrentTree(this.startState);
    }

    /**
     * 
     * @param parent
     * @return 
     */
    private Rule[] ensureRules(int parent) {
        Rule[] rs = this.parentRules.get(parent);
        
        if(rs == null) {
            ArrayList<Rule> al = new ArrayList<>();
            
            for(Rule r : (Iterable<Rule>) ta.getRulesTopDown(startState)) {
                al.add(r);
            }
            
            rs = al.toArray(new Rule[al.size()]);
            
            this.marks.put(parent, this.rg.nextInt(rs.length));
            this.parentRules.put(parent, rs);
        }
        
        return rs;
    }

    /**
     * 
     * @param parent
     * @return 
     */
    private Tree<Rule> getCurrentTree(int parent) {
        Rule[] r = this.ensureRules(parent);
        int mark = this.marks.get(parent);
        
        Rule rule = r[mark];
        Tree<Rule>[] children = new Tree[rule.getArity()];
        
        for(int i=0;i<rule.getArity();++i) {
            children[i] = this.getCurrentTree(rule.getChildren()[i]);
        }
        
        return Tree.create(rule,children);
    }
}
