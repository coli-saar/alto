/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author koller
 */
public class GensymBinaryRuleFactory implements BinaryRuleFactory {

    private long nextGensym = 1;

    @Override
    public Rule generateBinarizedRule(Tree<String> nodeInVartree, List<String> binarizedChildStates, String pathToNode, Rule originalRule, Tree<String> vartree, InterpretedTreeAutomaton originalIrtg, InterpretedTreeAutomaton binarizedIrtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = (ConcreteTreeAutomaton<String>) binarizedIrtg.getAutomaton();
        String oldRuleParent = originalIrtg.getAutomaton().getStateForId(originalRule.getParent());
        String parent;

        double weight;

        if (nodeInVartree == vartree) {
            parent = oldRuleParent;
            weight = originalRule.getWeight();
        } else {
            parent = gensym("q");
            weight = 1;
        }

        Rule newRule = binarizedRtg.createRule(parent, nodeInVartree.getLabel(), binarizedChildStates, weight);
        return newRule;
    }

    private String gensym(String prefix) {
        return prefix + (nextGensym++);
    }
    
    public static Function<InterpretedTreeAutomaton, BinaryRuleFactory> createFactoryFactory() {
        return irtg -> new GensymBinaryRuleFactory();
    }
}
