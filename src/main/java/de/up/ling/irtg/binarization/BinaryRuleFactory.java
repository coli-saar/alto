/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.binarization;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 *
 * @author koller
 */
public interface BinaryRuleFactory {
    /**
     * Generates an automaton rule for a single node of the common variable tree.
     * 
     * @param nodeInVartree - the node in the variable tree at which we are generating a rule
     * @param pathToNode - the path from the root to this node in the variable tree (in a suitable format for {@link Tree#select(java.lang.String, int) })
     * @param binarizedChildStates - the states that were generated for the children
     * @param originalRule - the rule in the original, unbinarized IRTG
     * @param vartree - the variable tree for which we are generating rules
     * @param originalIrtg - the original, unbinarized IRTG
     * @param binarizedIrtg - the binarized IRTG whose rules we are currently creating
     * @return - the created binarized rule
     */
    Rule generateBinarizedRule(Tree<String> nodeInVartree, List<String> binarizedChildStates, String pathToNode, Rule originalRule, Tree<String> vartree, InterpretedTreeAutomaton originalIrtg, InterpretedTreeAutomaton binarizedIrtg);
}
