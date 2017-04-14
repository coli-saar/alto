/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.NondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.siblingfinder.SiblingFinderIntersection;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.FileReader;
import java.io.IOException;
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
        this.rrt = rrt;
        this.coarsestIRTG = rrt.makeIrtgWithCoarsestAutomaton(irtg);
        invhom = new SiblingFinderInvhom(decomp, irtg.getInterpretation(interpretation).getHomomorphism());
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
        intersect.makeAllRulesExplicit(new Consumer<Rule>() {
            @Override
            public void accept(Rule rule) {
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
        }});
        return invhom.seenRulesAsAutomaton();
    }
    
    
    public static void main(String[] args) throws IOException, ParseException, CorpusReadingException {
        String ftc = "___(\n" +
            "  N_1_null_0_1_2,\n" +
            "  N_0_1_0_2,\n" +
            "  N_0_0_2,\n" +
            "  N_1_null_0_1,\n" +
            "  N_1_2,\n" +
            "  N_0_1_0,\n" +
            "  N_0_2,\n" +
            "  N_0_1,\n" +
            "  N_1_0,\n" +
            "  N_0_null_2,\n" +
            "  N_1_0_1_2,\n" +
            "  N_0_2_0_1,\n" +
            "  N_1_null_2,\n" +
            "  N_1_0_2,\n" +
            "  N_0_null_0_1_2,\n" +
            "  N_0_null_1_2,\n" +
            "  N_1_2_0,\n" +
            "  N_0_null,\n" +
            "  N_1_1_0_2,\n" +
            "  N_0_2_1,\n" +
            "  N_1_1_2,\n" +
            "  N_1_null_0_2,\n" +
            "  N_0_null_0_2,\n" +
            "  N_1_1,\n" +
            "  N_0_0_1_2,\n" +
            "  N_1_1_0,\n" +
            "  N_0_null_1,\n" +
            "  N_0_1_2,\n" +
            "  N_0_null_0,\n" +
            "  N_0_2_0,\n" +
            "  N_1_null_0,\n" +
            "  N_1_null_1,\n" +
            "  N_0_0_1,\n" +
            "  N_1_0_1,\n" +
            "  N_1_2_0_1,\n" +
            "  N_1_2_1,\n" +
            "  N_1_null_1_2,\n" +
            "  N_0_null_0_1,\n" +
            "  N_0_0)";
        
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromPath("../../experimentData/grammar_8.irtg");
        Corpus corpus = Corpus.readCorpus(new FileReader("../../experimentData/corpus_19.txt"), irtg);
        
        int i = 0;
        for (Instance inst : corpus) {
            InterpretedTreeAutomaton filteredIRTG = irtg.filterForAppearingConstants("graph", inst.getInputObjects().get("graph"));
            
            //use these lines for ctf
            CoarseToFineParser ctfp = CoarseToFineParser.makeCoarseToFineParser(filteredIRTG, "graph", ftc, 0.0001);
            TreeAutomaton auto = ctfp.parseInputObjectWithSF(inst.getInputObjects().get("graph"));
            
            //use these lines for comparison
//            TreeAutomaton decomp = filteredIRTG.getInterpretation("graph").getAlgebra().decompose(inst.getInputObjects().get("graph"));
//            SiblingFinderInvhom invhom = new SiblingFinderInvhom(decomp, filteredIRTG.getInterpretation("graph").getHomomorphism());
//            SiblingFinderIntersection intersect = new SiblingFinderIntersection(filteredIRTG.getAutomaton(), invhom);
//            TreeAutomaton auto = SiblingFinderIntersection.makeVeryLazyExplicit(intersect);
            
            
            Tree<String> vit = auto.viterbi();
            if (vit != null) {
                System.err.println(vit);
            }
            if (i>700) {
                break;
            }
            i++;
        }
        
        
    }
    
}
