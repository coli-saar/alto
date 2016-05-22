/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 *
 * @author christoph_teichmann
 */
public interface RuleMarking extends IntIterable {
    /**
     * 
     * @return 
     */
    public IntIterator iterator();

    /**
     * 
     * @param parent
     * @return 
     */
    public int getNumberOfRules(int parent);

    /**
     * 
     * @param parent
     * @param ruleNumber
     * @return 
     */
    public Rule getRule(int parent, int ruleNumber);

    /**
     * 
     * @param r
     * @return 
     */
    public double getInsideChoiceLogProb(Rule r);

    /**
     * 
     * @param parent
     * @param ruleNumber 
     */
    public void setRule(int parent, int ruleNumber);

    /**
     * 
     * @return 
     */
    public Tree<Rule> getCurrentTree();

    /**
     * 
     * @param tree
     * @return 
     */
    public double getLogTargetProb(Tree<Rule> tree);
}
