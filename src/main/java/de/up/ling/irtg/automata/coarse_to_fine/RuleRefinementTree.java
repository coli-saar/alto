/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.FastutilUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author koller
 */
public class RuleRefinementTree {
    private List<RuleRefinementNode> toplevel;
    private Function<Rule,RuleRefinementNode> finestNodes;
    private IntSet coarsestFinalStates;

    public RuleRefinementTree(List<RuleRefinementNode> toplevel, IntSet coarsestFinalStates, Function<Rule, RuleRefinementNode> finestNodes) {
        this.toplevel = toplevel;
        this.finestNodes = finestNodes;
        this.coarsestFinalStates = coarsestFinalStates;
    }

    public List<RuleRefinementNode> getCoarsestNodes() {
        return toplevel;
    }
    
    public RuleRefinementNode getFinestNodeForRule(Rule rule) {
        return finestNodes.apply(rule);
    }
    
    public TreeAutomaton makeCoarsestAutomaton(TreeAutomaton fineAutomaton) {
        ConcreteTreeAutomaton ret = new ConcreteTreeAutomaton(fineAutomaton.getSignature(), fineAutomaton.getStateInterner());
        
        for( RuleRefinementNode node : toplevel ) {
            ret.addRule(ret.createRule(node.getParent(), node.getRepresentativeLabel(), node.getChildren(), node.getWeight()));
        }
        
        FastutilUtils.forEach(coarsestFinalStates, ret::addFinalState);
        
        return ret;
    }
    
    public InterpretedTreeAutomaton makeIrtgWithCoarsestAutomaton(InterpretedTreeAutomaton irtg) {
        TreeAutomaton auto = makeCoarsestAutomaton(irtg.getAutomaton());
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(auto);
        
        ret.addAllInterpretations(irtg.getInterpretations());
        
        return ret;
    }

    public IntSet getCoarsestFinalStates() {
        return coarsestFinalStates;
    }
    
    
    
    public String toString(TreeAutomaton auto) {
        StringBuilder buf = new StringBuilder();
        
        for( RuleRefinementNode node : toplevel ) {
            buf.append(node.toString(auto) + "\n");
        }
        
        return buf.toString();
    }
}
