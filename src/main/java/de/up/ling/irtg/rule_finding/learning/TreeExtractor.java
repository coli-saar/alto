/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public interface TreeExtractor {

    /**
     *
     * @param it
     * @return
     */
    Iterable<Tree<String>> getChoices(final Iterable<TreeAutomaton> it);
}
