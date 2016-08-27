/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class RuleRefinementNode {
    private int parent;
    private IntSet labelSet;
    private int[] children;
    private double weight;
    
    private List<RuleRefinementNode> refinements;
    
    static RuleRefinementNode makeFinest(Rule rule) {
        RuleRefinementNode ret = new RuleRefinementNode();
        ret.parent = rule.getParent();
        ret.labelSet = IntSets.singleton(rule.getLabel());
        ret.children = rule.getChildren();
        ret.weight = rule.getWeight();
        
        return ret;
    }
    
    static RuleRefinementNode makeCoarser(List<RuleRefinementNode> finerNodes, RrtSummary summary) {
        RuleRefinementNode ret = new RuleRefinementNode();
        
        ret.parent = summary.getCoarseParent();
        ret.children = summary.getCoarseChildren();
        
        ret.labelSet = new IntOpenHashSet();
        ret.weight = 0;
        
        for( RuleRefinementNode fine : finerNodes ) {
            ret.labelSet.addAll(fine.labelSet);
            ret.weight += fine.weight;
        }
        
        ret.refinements = finerNodes;
        
        return ret;
    }

    public RuleRefinementNode() {
        refinements = new ArrayList<>();
    }
    
    public void addRefinement(RuleRefinementNode rrt) {
        refinements.add(rrt);
    }
    
    public void foreachRule(Consumer<Rule> fn, TreeAutomaton auto) {
        double weightPerRule = weight/labelSet.size();
        
        FastutilUtils.forEach(labelSet, f -> {
           Rule r = auto.createRule(parent, f, children, weightPerRule);
        });
    }

    public int getParent() {
        return parent;
    }

    public IntSet getLabelSet() {
        return labelSet;
    }
    
    /**
     * Returns an arbitrary label from this node's label set.
     * 
     * @return 
     */
    public int getRepresentativeLabel() {
        return labelSet.iterator().nextInt();
    }

    public int[] getChildren() {
        return children;
    }

    public double getWeight() {
        return weight;
    }

    public List<RuleRefinementNode> getRefinements() {
        return refinements;
    }
    
    public String toString(TreeAutomaton<String> auto) {
        StringBuilder buf = new StringBuilder();
        buildString(0, auto, buf);
        return buf.toString();
    }

    private void buildString(int depth, TreeAutomaton<String> auto, StringBuilder buf) {
        String prefix = (depth == 0) ? "" : String.format("%" + depth + "s", " ");
        
        buf.append(String.format("%s%s -> %s(", 
                                 prefix, 
                                 auto.getStateForId(parent), 
                                 Util.mapToSet(labelSet, f -> auto.getSignature().resolveSymbolId(f)).toString()));

        for( int i = 0; i < children.length; i++ ) {
            if( i > 0 ) {
                buf.append(", ");
            }
            
            buf.append(auto.getStateForId(children[i]));
        }
        
        buf.append(String.format(") [%f]\n", weight));
        
        for( RuleRefinementNode fine : refinements ) {
            fine.buildString(depth + 2, auto, buf);
        }
    }
    
}
