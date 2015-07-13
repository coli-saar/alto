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
import java.util.List;

/**
 *
 * @author christoph
 */
public interface RuleMarker extends RuleEvaluator<IntSet[]> {
    
    /**
     * 
     * @param r
     * @return 
     */
    @Override
    public IntSet[] evaluateRule(Rule r);

    /**
     * 
     * @return 
     */
    public int width();

    /**
     * 
     * @param alignments
     * @param original
     * @param state
     * @return 
     */
    public String makeCode(IntSet[] alignments, TreeAutomaton original, int state);
    
    /**
     * 
     * @param labels
     * @return 
     */
    public boolean checkCompatible(List<String> labels);
}