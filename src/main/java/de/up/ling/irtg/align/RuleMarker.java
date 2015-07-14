/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author christoph
 */
public interface RuleMarker {    
    /**
     * 
     * @param r
     * @return 
     */
    public RuleEvaluator<IntSet> ruleMarkings(int num);

    /**
     * 
     * @param ta
     * @param r
     * @return 
     */
    public IntSet getMarkings(int num, Rule r);
    
    /**
     * 
     * @param alignments
     * @param original
     * @param state
     * @return 
     */
    public String makeCode(IntSet alignments, TreeAutomaton original, int state);
    
    /**
     *
     * @param label1
     * @param label2
     * @return 
     */
    public boolean checkCompatible(String label1, String label2);

    /**
     * 
     * @param label
     * @return 
     */
    public boolean isFrontier(String label);

    /**
     * 
     * @param variable
     * @return 
     */
    public String getCorresponding(String variable);
}