/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.siblingfinder.SiblingFinderIntersection;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;

import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Jonas Groschwitz
 */
public class SiblingFinderCoarserstParser {
    
    private final InterpretedTreeAutomaton coarsestIRTG;
    private final RuleRefinementTree rrt;
    private final SiblingFinderIntersection intersect;
    private final SiblingFinderInvhom invhom;
    
    public SiblingFinderCoarserstParser(RuleRefinementTree rrt, InterpretedTreeAutomaton irtg, TreeAutomaton decomp, String interpretation) {
		this(rrt, irtg, decomp, irtg.getInterpretation(interpretation));
    }

	public SiblingFinderCoarserstParser(RuleRefinementTree rrt, InterpretedTreeAutomaton irtg, TreeAutomaton decomp, Interpretation<?> interpretation) {
        this.rrt = rrt;
        this.coarsestIRTG = rrt.makeIrtgWithCoarsestAutomaton(irtg);
        invhom = new SiblingFinderInvhom(decomp, interpretation.getHomomorphism());
        intersect = new SiblingFinderIntersection(irtg.getAutomaton(), invhom);
    }
    
    /**
     * Note: returns the invhom automaton for future reference.
     * @param coarseNodes
     * @param partnerInvhomRules
     * @return 
     */
    public ConcreteTreeAutomaton parse(List<RuleRefinementNode> coarseNodes, List<Rule> partnerInvhomRules) {
        ConcreteTreeAutomaton dummyRhs = new ConcreteTreeAutomaton(coarsestIRTG.getAutomaton().getSignature());
        intersect.makeAllRulesExplicit(null);
        Consumer<Rule> cons = rule -> {
                RuleRefinementNode matchingCoarsest = null;
                for (RuleRefinementNode n : rrt.getCoarsestNodes()) {
                    if (n.getLabelSet().contains(rule.getLabel())) {
                        matchingCoarsest = n;
                        break;
                    }
                }
                coarseNodes.add(matchingCoarsest);
                int[] rhsChildren = new int[rule.getArity()];
                for (int i = 0; i<rule.getArity(); i++) {
                    rhsChildren[i]=intersect.getRhsState4IntersectState(rule.getChildren()[i]);
                }
                partnerInvhomRules.add(dummyRhs.createRule(intersect.getRhsState4IntersectState(rule.getParent()), rule.getLabel(), rhsChildren, 1));
            };
        intersect.seenRulesAsAutomaton().ckyDfsInBottomUpOrder(cons, cons);
        
        return invhom.seenRulesAsAutomaton();
    }
    
    
}
