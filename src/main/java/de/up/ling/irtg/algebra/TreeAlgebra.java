/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.basic.StringOrVariable;
import de.saar.chorus.term.parser.TermParser;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.MapSignature;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class TreeAlgebra implements Algebra<Tree<String>> {
    private final MapSignature signature = new MapSignature();

    @Override
    public Tree<String> evaluate(Tree<String> t) {
        return t;
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return new TreeDecomposingAutomaton(value);
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    private class TreeDecomposingAutomaton extends TreeAutomaton<String> {
        private Tree<String> tree;
        private final Set<String> labels;
        private ListMultimap<String, String> leafLabelsToStates;

        public TreeDecomposingAutomaton(Tree<String> tree) {
            super(TreeAlgebra.this.getSignature());

            this.tree = tree;

            labels = new HashSet<String>();
            leafLabelsToStates = ArrayListMultimap.create();

            collectStatesAndLabels(tree, "q");
            finalStates.add("q");
        }

        @Override
        public Set<Rule<String>> getRulesBottomUp(String label, List<String> childStates) {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();

            if (childStates.isEmpty()) {
                for (String state : leafLabelsToStates.get(label)) {
                    Rule<String> rule = new Rule<String>(state, label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            } else {
                String potentialParent = childStates.get(0).substring(0, childStates.get(0).length() - 1);
                boolean correctChildren = true;

                for (int i = 0; i < childStates.size(); i++) {
                    if (!childStates.get(i).equals(potentialParent + i)) {
                        correctChildren = false;
                    }
                }

                if (correctChildren && tree.select(potentialParent, 1).getLabel().equals(label)) {
                    Rule<String> rule = new Rule<String>(potentialParent, label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }

        @Override
        public Set<Rule<String>> getRulesTopDown(String label, String parentState) {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();
            Tree<String> t = tree.select(parentState, 1);

            if (t.getLabel().equals(label)) {
                List<String> children = new ArrayList<String>();
                for (int i = 0; i < t.getChildren().size(); i++) {
                    children.add(parentState + i);
                }

                Rule<String> rule = new Rule<String>(parentState, label, children);
                ret.add(rule);
                storeRule(rule);
            }


            return ret;
        }

        private void collectStatesAndLabels(Tree<String> node, String state) {
            state = addState(state);
            labels.add(node.getLabel());

            if (node.getChildren().isEmpty()) {
                leafLabelsToStates.put(node.getLabel(), state);
            }

            for (int i = 0; i < node.getChildren().size(); i++) {
                collectStatesAndLabels(node.getChildren().get(i), state + i);
            }
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = TreeParser.parse(representation);
        signature.addAllSymbols(ret);
        return ret;
    }

    public Tree<StringOrVariable> parseStringWithVariables(String representation) {
        Tree<StringOrVariable> ret = TermParser.parse(representation).toTreeWithVariables();
        signature.addAllSymbolsWithoutVariables(ret);
        return ret;
    }
}
