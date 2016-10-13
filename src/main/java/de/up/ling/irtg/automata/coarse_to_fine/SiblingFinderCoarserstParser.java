/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.siblingfinder.SiblingFinderIntersection;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author Jonas Groschwitz
 */
public class SiblingFinderCoarserstParser {
    
    private static final String DUMMY_SIG_PREFIX = "dummyr";
    private final Homomorphism hom;
    private final SiblingFinderInvhom invhom;
    private final SiblingFinderIntersection intersect;
    private final ConcreteTreeAutomaton<String> dummyLeft;
    private final List<RuleRefinementNode> allCoarsestNodes;
    
    public SiblingFinderCoarserstParser(RuleRefinementTree rrt, Homomorphism origHom, TreeAutomaton decomp) {
        Signature src = new Signature();
        hom = new Homomorphism(src, origHom.getTargetSignature());
        Interner<String> interner = new Interner<>();
        dummyLeft = new ConcreteTreeAutomaton<>(src, interner);
        
        allCoarsestNodes = rrt.getCoarsestNodes();
        int i = 0;
        for (RuleRefinementNode node : allCoarsestNodes) {
            int srcID = src.addSymbol(DUMMY_SIG_PREFIX+i, node.getChildren().length);
            hom.add(srcID, origHom.getByLabelSetID(node.getTermId()));
            
            for (int child : node.getChildren()) {
                interner.addObjectWithIndex(child, "s"+child);
            }
            interner.addObjectWithIndex(node.getParent(), "s"+node.getParent());
            dummyLeft.addRule(dummyLeft.createRule(node.getParent(), srcID, node.getChildren(), node.getWeight()));
                    
            i++;
        }
        
        invhom = new SiblingFinderInvhom(decomp, hom);
        intersect = new SiblingFinderIntersection(dummyLeft, invhom);
    }
    
    /**
     * Note: returns the invhom automaton for future reference.
     * @param coarseNodes
     * @param partnerInvhomRules
     * @return 
     */
    public ConcreteTreeAutomaton parse(List<RuleRefinementNode> coarseNodes, List<Rule> partnerInvhomRules) {
        ConcreteTreeAutomaton dummyRhs = new ConcreteTreeAutomaton(hom.getTargetSignature());
        intersect.makeAllRulesExplicit(new Consumer<Rule>() {
            @Override
            public void accept(Rule rule) {
                coarseNodes.add(allCoarsestNodes.get(Integer.valueOf(hom.getSourceSignature().resolveSymbolId(rule.getLabel()).substring(DUMMY_SIG_PREFIX.length()))));
                int[] rhsChildren = new int[rule.getArity()];
                for (int i = 0; i<rule.getArity(); i++) {
                    rhsChildren[i]=intersect.getRhsState4IntersectState(rule.getChildren()[i]);
                }
                partnerInvhomRules.add(dummyRhs.createRule(intersect.getRhsState4IntersectState(rule.getParent()), rule.getLabel(), rhsChildren, 1));
        }});
        return invhom.seenRulesAsAutomaton();
    }
    
}
