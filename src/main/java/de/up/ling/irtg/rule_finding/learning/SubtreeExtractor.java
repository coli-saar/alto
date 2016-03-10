/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public interface SubtreeExtractor {
    /**
     * 
     * @param analyses
     * @return 
     */
    public Iterable<Iterable<Tree<String>>> getRuleTrees(Iterable<InterpretedTreeAutomaton> analyses);
}
