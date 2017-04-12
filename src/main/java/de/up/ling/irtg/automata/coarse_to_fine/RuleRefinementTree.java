/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author koller
 */
public class RuleRefinementTree {
    private List<RuleRefinementNode> toplevel;
    private Function<Rule,RuleRefinementNode> finestNodes;
    private List<IntSet> finalStatesPerLevel; // 0 = coarsest; size-1 = finest
    private IntTrie<List<RuleRefinementNode>> coarsestTrie;  // termId -> child1 -> ... -> childn -> List<RRN>

    public RuleRefinementTree(List<RuleRefinementNode> toplevel, List<IntSet> finalStatesPerLevel, Function<Rule, RuleRefinementNode> finestNodes) {
        this.toplevel = toplevel;
        this.finestNodes = finestNodes;
        this.finalStatesPerLevel = finalStatesPerLevel;
        this.coarsestTrie = new IntTrie<>();
        
        for( RuleRefinementNode node : toplevel ) {
            int[] key = makeKey(node);
            List<RuleRefinementNode> tn = coarsestTrie.get(key);
            
            if( tn == null ) {
                tn = new ArrayList<>();
                coarsestTrie.put(key, tn);
            }
            
            tn.add(node);            
        }
    }

    public List<RuleRefinementNode> getCoarsestNodes() {
        return toplevel;
    }
    
    public RuleRefinementNode getFinestNodeForRule(Rule rule) {
        return finestNodes.apply(rule);
    }
    
    /**
     * Returns the variant of the derivation tree automaton at the 
     * coarsest level of the coarse-to-fine hierarchy.
     * 
     * @param fineAutomaton
     * @return 
     */
    public TreeAutomaton makeCoarsestAutomaton(TreeAutomaton fineAutomaton) {
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(fineAutomaton.getSignature(), fineAutomaton.getStateInterner());
        
        for( RuleRefinementNode node : toplevel ) {
            ret.addRule(ret.createRule(node.getParent(), node.getRepresentativeLabel(), node.getChildren(), node.getWeight()));
        }
        
        FastutilUtils.forEach(finalStatesPerLevel.get(0), ret::addFinalState);
        
        return ret;
    }
    
    /**
     * Returns the variant of the IRTG at the coarsest level
     * of the coarse-to-fine hierarchy.
     * 
     * @param irtg
     * @return 
     */
    public InterpretedTreeAutomaton makeIrtgWithCoarsestAutomaton(InterpretedTreeAutomaton irtg) {
        TreeAutomaton auto = makeCoarsestAutomaton(irtg.getAutomaton());
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(auto);
        
        ret.addAllInterpretations(irtg.getInterpretations());
        
        return ret;
    }
    
    public IntSet getFinalStatesAtLevel(int level) {
        return finalStatesPerLevel.get(level);
    }
    
    private int[] makeKey(RuleRefinementNode node) {
        int[] key = new int[node.getChildren().length+1];
        key[0] = node.getTermId();
        for( int i = 0; i < node.getChildren().length; i++ ) {
            key[i+1] = node.getChildren()[i];
        }
        
        return key;
    }

    public IntTrie<List<RuleRefinementNode>> getCoarsestTrie() {
        return coarsestTrie;
    }
    
    
    
    
    public String toString(TreeAutomaton auto) {
        StringBuilder buf = new StringBuilder();
        
        buf.append("Final states at level:\n");
        for( int level = 0; level < finalStatesPerLevel.size(); level ++ ) {
            buf.append(String.format("[%d] %s\n", level, Util.mapToList(getFinalStatesAtLevel(level), q -> auto.getStateForId(q)).toString()));
        }
        
        buf.append("\n");
        
        for( RuleRefinementNode node : toplevel ) {
            buf.append(node.toString(auto) + "\n");
        }
        
        return buf.toString();
    }
}
