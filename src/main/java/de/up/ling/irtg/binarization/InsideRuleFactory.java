/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generates nonterminals in the style of Klein and Manning's (2003)
 * "inside" strategy (see their Fig. 5).
 * 
 * Note that this binarization technique pools rules together
 * after binarization. Therefore
 * {@link de.up.ling.irtg.InterpretedTreeAutomaton#filterBinarizedForAppearingConstants(java.lang.String, java.lang.Object)},
 * which removes all rules connected by binarization
 * to rules removed due to constants, removes some of these pooled rules that
 * would be necessary for parsing. Thus, for grammars binarized with this strategy,
 * use {@link de.up.ling.irtg.InterpretedTreeAutomaton#filterForAppearingConstants(java.lang.String, java.lang.Object)}
 * instead.
 * 
 * @author koller
 */
public class InsideRuleFactory implements BinaryRuleFactory {
    public static String NONTERMINAL_SEPARATOR = ">>";
    
    private RuleByEquivalenceStore equiv;

    public InsideRuleFactory(InterpretedTreeAutomaton irtg) {
        equiv = new RuleByEquivalenceStore(irtg);
    }

     /**
     * Generates an automaton rule for a single node of the common variable tree.
     * 
     * @param nodeInVartree - the node in the variable tree at which we are generating a rule
     * @param binarizedChildStates - the states that were generated for the children
     * @param originalRule - the rule in the original, unbinarized IRTG
     * @param vartree - the variable tree for which we are generating rules
     * @param originalIrtg - the original, unbinarized IRTG
     * @param binarizedIrtg - the binarized IRTG whose rules we are currently creating
     * @return - the created binarized rule
     */
    @Override
    public Rule generateBinarizedRule(Tree<String> nodeInVartree, List<String> binarizedChildStates, String pathToNode, Rule originalRule, Tree<String> vartree, InterpretedTreeAutomaton originalIrtg, InterpretedTreeAutomaton binarizedIrtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = (ConcreteTreeAutomaton<String>) binarizedIrtg.getAutomaton();
        String oldRuleParent = originalIrtg.getAutomaton().getStateForId(originalRule.getParent());
        boolean toplevel = (nodeInVartree == vartree);
        
        String parent = oldRuleParent;
        
        if( ! toplevel ) {
            int firstLeafPosition = getFirstLeafPositionBelow(vartree, pathToNode, "_");
            int numLeaves = nodeInVartree.getLeafLabels().size();
            
            List<String> insideLeaves = new ArrayList<>();
            for( int i = 0; i < originalRule.getArity(); i++ ) {
                if( i >= firstLeafPosition && i < firstLeafPosition + numLeaves) {
                    insideLeaves.add(originalIrtg.getAutomaton().getStateForId(originalRule.getChildren()[i]));
                }
            }
            
            parent = StringTools.join(insideLeaves, NONTERMINAL_SEPARATOR);
        }
        
        
        
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
    
    static int[] parseSelectors(String selector, String separator) {
        String[] ss = selector.split(separator);
        int[] ret = new int[ss.length];
        for( int i = 0; i < ss.length; i++ ) {
            ret[i] = Integer.parseInt(ss[i]);
        }
        return ret;
    }
    
    static <E> int getFirstLeafPositionBelow(Tree<E> tree, String selector, String separator) {
        return getFirstLeafPositionBelow(tree, parseSelectors(selector, separator), 0);
    }
    
    /**
     * Gets the position of the first leaf of the given subtree, within the list
     * of all leaves of the entire tree. First leaf = 0.
     * 
     * @param <E>
     * @param tree
     * @param pathToNode
     * @param positionInPath
     * @return 
     */
    static <E> int getFirstLeafPositionBelow(Tree<E> tree, int[] pathToNode, int positionInPath) {
        if( positionInPath == pathToNode.length ) {
            return 0;
        } else {
            int child = pathToNode[positionInPath];
            int leavesToLeft = 0;
            
            for( int i = 0; i < child; i++ ) {
                Tree<E> leftSibling = tree.getChildren().get(i);
                leavesToLeft += leftSibling.getLeafLabels().size();
            }
            
            return leavesToLeft + getFirstLeafPositionBelow(tree.getChildren().get(child), pathToNode, positionInPath+1);
        }
    }
    
//    public static void main(String[] args) throws Exception {
//        Tree t = TestingTools.pt("f(g(a,h(c,d)), k(l,m))");
//        System.err.println(getFirstLeafPositionBelow(t, "0_1_1", "_"));
//    }
    
    private static <E> Tree<E> selectFromIntArray(Tree<E> tree, int[] selectors, int start) {
        if (start == selectors.length) {
            return tree;
        } else {
            int childNumber = selectors[start];
            return selectFromIntArray(tree.getChildren().get(childNumber), selectors, start+1);
        }
    }

     public static Function<InterpretedTreeAutomaton, BinaryRuleFactory> createFactoryFactory() {
        return irtg -> new InsideRuleFactory(irtg);
    }
}
