/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.HashSet;
import java.util.Set;

/**
 * The tree algebra. The elements of this algebra are the ranked
 * trees over a given signature. Any string f can be used as a tree-combining
 * operation of an arbitrary arity; the term f(t1,...,tn) evaluates
 * to the tree f(t1,...,tn). Care must be taken that only ranked
 * trees can be described; the parseString method will infer the arity
 * of each symbol f that you use, and will throw an exception if you
 * try to use f with two different arities.
 * 
 * @author koller
 */
public class TreeAlgebra extends Algebra<Tree<String>> {
    protected final Signature signature = new Signature();

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
        private Tree<Integer> tree;
        private final Set<Integer> labels;
        private ListMultimap<Integer, Integer> leafLabelsToStateIds;

        public TreeDecomposingAutomaton(Tree<String> derivedTree) {
            super(TreeAlgebra.this.getSignature());

            this.tree = TreeAlgebra.this.getSignature().addAllSymbols(derivedTree);

            labels = new HashSet<Integer>();
            leafLabelsToStateIds = ArrayListMultimap.create();

            collectStatesAndLabels(tree, "q");
            finalStates.add(addState("q"));
        }

        @Override
        public Set<Rule> getRulesBottomUp(int label, int[] childStates) {
            Set<Rule> ret = new HashSet<Rule>();

            if (childStates.length == 0) {
                for (int state : leafLabelsToStateIds.get(label)) {
                    Rule rule = createRule(state, label, childStates, 1);
                    storeRule(rule);
                    ret.add(rule);
                }
            } else {
                String firstChildState = getStateForId(childStates[0]);
                String potentialParent = firstChildState.substring(0, firstChildState.length() - 1);
                boolean correctChildren = true;

                for (int i = 0; i < childStates.length; i++) {
                    if (! getStateForId(childStates[i]).equals(potentialParent + i)) {
                        correctChildren = false;
                    }
                }

                if (correctChildren && tree.select(potentialParent, 1).getLabel().equals(label)) {
                    Rule rule = createRule(addState(potentialParent), label, childStates, 1);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }

        @Override
        public Set<Rule> getRulesTopDown(int label, int parentState) {
            Set<Rule> ret = new HashSet<Rule>();
            String parentPath = getStateForId(parentState);
            Tree<Integer> t = tree.select(parentPath, 1);

            if (t.getLabel().equals(label)) {
                int[] children = new int[t.getChildren().size()];
                for (int i = 0; i < t.getChildren().size(); i++) {
                    children[i] = addState(parentPath + i);
                }

                Rule rule = createRule(parentState, label, children, 1);
                ret.add(rule);
                storeRule(rule);
            }


            return ret;
        }

        private void collectStatesAndLabels(Tree<Integer> node, String state) {
            int stateId = addState(state);
            labels.add(node.getLabel());

            if (node.getChildren().isEmpty()) {
                leafLabelsToStateIds.put(node.getLabel(), stateId);
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
    
    /*  ** unused? **
    public Tree<StringOrVariable> parseStringWithVariables(String representation) {
        Tree<StringOrVariable> ret = TermParser.parse(representation).toTreeWithVariables();
        signature.addAllConstants(ret);
        return ret;
    }
    */
}
