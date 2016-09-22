/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

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
    public Rule generateBinarizedRule(Tree<String> nodeInVartree, List<String> binarizedChildStates, Rule originalRule, Tree<String> vartree, InterpretedTreeAutomaton originalIrtg, InterpretedTreeAutomaton binarizedIrtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = (ConcreteTreeAutomaton<String>) binarizedIrtg.getAutomaton();
        String oldRuleParent = originalIrtg.getAutomaton().getStateForId(originalRule.getParent());
        boolean toplevel = (nodeInVartree == vartree);

        String parent = toplevel ? oldRuleParent : (oldRuleParent + "-bar");
        Rule candidateRule = binarizedRtg.createRule(parent, nodeInVartree.getLabel(), binarizedChildStates, originalRule.getWeight());

//        if(toplevel) {
//            return candidateRule;
//        }
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

    private static class RuleByEquivalenceStore {

        private final IntTrie<Rule> ruleTrie;
        private final List<String> interpsInOrder;
        private final ToIntBiFunction<Integer, String> interpToId; // rule_label_id x interpretation_name -> labelSetID

        public RuleByEquivalenceStore(InterpretedTreeAutomaton irtg) {
            this.interpToId = (labelId, interpretation) -> irtg.getInterpretation(interpretation).getHomomorphism().getLabelSetID(labelId);
            ruleTrie = new IntTrie<>();
            interpsInOrder = new ArrayList<>(irtg.getInterpretations().keySet());
        }

        public Rule get(Rule rule) {
            int[] fineRuleHomSignature = makeHomSignature(rule, interpsInOrder);
            int[] key = makeTrieKey(rule, fineRuleHomSignature);
            Rule earlierEquivalentRule = ruleTrie.get(key);
            return earlierEquivalentRule;
        }

        public void add(Rule rule) {
            int[] fineRuleHomSignature = makeHomSignature(rule, interpsInOrder);
            int[] key = makeTrieKey(rule, fineRuleHomSignature);
            ruleTrie.put(key, rule);
        }

        private int[] makeTrieKey(Rule rule, int[] fineHomSignature) {
            int ar = rule.getArity();
            int fhs = fineHomSignature.length;

            int[] ret = new int[ar + fhs + 1];

            System.arraycopy(rule.getChildren(), 0, ret, 0, ar);
            System.arraycopy(fineHomSignature, 0, ret, ar, fhs);
            ret[ar + fhs] = rule.getParent();

            return ret;
        }

        private int[] makeHomSignature(Rule rule, List<String> interpsInOrder) {
            int[] ret = new int[interpsInOrder.size()];

            for (int i = 0; i < interpsInOrder.size(); i++) {
                ret[i] = interpToId.applyAsInt(rule.getLabel(), interpsInOrder.get(i));
            }

            return ret;
        }
    }

}
