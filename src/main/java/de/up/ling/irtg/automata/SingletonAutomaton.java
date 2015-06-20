/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author koller
 */
public class SingletonAutomaton extends TreeAutomaton<String> {
    private Tree<Integer> treeWithIntLabels;
    private final Set<Integer> labels;
    private ListMultimap<Integer, Integer> leafLabelsToStateIds;
    private static final String SEPARATOR = "_";
    
    public SingletonAutomaton(Tree<String> tree, Signature signature) {
        super(signature);

        treeWithIntLabels = getSignature().addAllSymbols(tree);
        
        labels = new HashSet<Integer>();
        leafLabelsToStateIds = ArrayListMultimap.create();

        collectStatesAndLabels(treeWithIntLabels, "q");
        finalStates.add(addState("q"));
    }

    public SingletonAutomaton(Tree<String> derivedTree) {
        this(derivedTree, new Signature());
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
            String potentialParent = stripLastPart(firstChildState);
            boolean correctChildren = true;

            for (int i = 0; i < childStates.length; i++) {
                if (!getStateForId(childStates[i]).equals(combine(potentialParent, i))) {
                    correctChildren = false;
                }
            }

            if (correctChildren && treeWithIntLabels.selectWithSeparators(potentialParent, 1, SEPARATOR).getLabel().equals(label)) {
                Rule rule = createRule(addState(potentialParent), label, childStates, 1);
                storeRule(rule);
                ret.add(rule);
            }
        }

        return ret;
    }
    
    private String stripLastPart(String state) {
        char sep = SEPARATOR.charAt(0);
        
        for( int i = state.length()-1; i >= 0; i-- ) {
            if( state.charAt(i) == sep ) {
                return state.substring(0, i);
            }
        }
        
        return null;
    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        Set<Rule> ret = new HashSet<Rule>();
        String parentPath = getStateForId(parentState);
        Tree<Integer> t = treeWithIntLabels.selectWithSeparators(parentPath, 1, SEPARATOR);

        if (t.getLabel().equals(label)) {
            int[] children = new int[t.getChildren().size()];
            for (int i = 0; i < t.getChildren().size(); i++) {
                children[i] = addState(combine(parentPath, i));
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
            collectStatesAndLabels(node.getChildren().get(i), combine(state, i));
        }
    }
    
    private String combine(String oldStateName, int childId) {
        return oldStateName + SEPARATOR + childId;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return true;
    }
}