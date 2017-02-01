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
public class XbarRuleFactory implements BinaryRuleFactory {

    private RuleByEquivalenceStore equiv;

    public XbarRuleFactory(InterpretedTreeAutomaton irtg) {
        equiv = new RuleByEquivalenceStore(irtg);
    }

    @Override
    public Rule generateBinarizedRule(Tree<String> nodeInVartree, List<String> binarizedChildStates, String pathToNode, Rule originalRule, Tree<String> vartree, InterpretedTreeAutomaton originalIrtg, InterpretedTreeAutomaton binarizedIrtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = (ConcreteTreeAutomaton<String>) binarizedIrtg.getAutomaton();
        String oldRuleParent = originalIrtg.getAutomaton().getStateForId(originalRule.getParent());
        boolean toplevel = (nodeInVartree == vartree);

        String parent = toplevel ? oldRuleParent : (oldRuleParent + "-bar");
        Rule candidateRule = binarizedRtg.createRule(parent, nodeInVartree.getLabel(), binarizedChildStates, originalRule.getWeight());
        Rule lookup = equiv.get(candidateRule);

        if (lookup == null) {
            binarizedRtg.addRule(candidateRule);
            equiv.add(candidateRule);
            return candidateRule;
        } else {
            // update rule "count"
            double oldWeight = lookup.getWeight();
            lookup.setWeight(oldWeight + candidateRule.getWeight());
            return lookup;
        }
    }

    public static Function<InterpretedTreeAutomaton, BinaryRuleFactory> createFactoryFactory() {
        return irtg -> new XbarRuleFactory(irtg);
    }
}
