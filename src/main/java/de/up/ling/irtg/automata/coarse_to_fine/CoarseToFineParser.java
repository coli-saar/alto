/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedRule;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.util.NumbersCombine;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class CoarseToFineParser {
    private InterpretedTreeAutomaton irtg;
    private RuleRefinementTree rrt;
    private String inputInterpretation;
    private FineToCoarseMapping ftc;

    public CoarseToFineParser(InterpretedTreeAutomaton irtg, String inputInterpretation, FineToCoarseMapping ftc) {
        this.irtg = irtg;
        this.inputInterpretation = inputInterpretation;
        this.ftc = ftc;

        GrammarCoarsifier gc = new GrammarCoarsifier(ftc);
        this.rrt = gc.coarsify(irtg, inputInterpretation);
    }

    public TreeAutomaton parse(Map<String, String> input) throws ParserException {
        // create condensed invhom automaton
        Map<String, Object> inputObjects = new HashMap<String, Object>();
        for (String interp : input.keySet()) {
            inputObjects.put(interp, irtg.parseString(interp, input.get(interp)));
        }

        CondensedTreeAutomaton invhom = irtg.getInterpretation(inputInterpretation).parseToCondensed(inputObjects.get(inputInterpretation));

        // coarse parsing
        List<RuleRefinementNode> coarseNodes = new ArrayList<>();
        List<CondensedRule> partnerInvhomRules = new ArrayList<>();
        CondensedCoarsestParser ccp = new CondensedCoarsestParser(rrt, invhom);
        ccp.parse(coarseNodes, partnerInvhomRules);

        assert coarseNodes.size() == partnerInvhomRules.size();

        // refine the chart level-1 times
        List<RuleRefinementNode> finerNodes = new ArrayList<>();
        List<CondensedRule> finerInvhomPartners = new ArrayList<>();

        for (int level = 0; level < ftc.numLevels() - 1; level++) {
            Long2DoubleMap inside = new Long2DoubleOpenHashMap();
            Long2DoubleMap outside = new Long2DoubleOpenHashMap();
            double totalSentenceInside = computeInsideOutside(level, coarseNodes, partnerInvhomRules, invhom, inside, outside);

            printChart(coarseNodes, partnerInvhomRules, invhom, inside, outside);
            return null;
        }

        // decode final chart into tree automaton
        return null;
    }

    private void printChart(List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, Long2DoubleMap inside, Long2DoubleMap outside) {
        for (int i = 0; i < coarseNodes.size(); i++) {
            CondensedRule r = partnerInvhomRules.get(i);
            RuleRefinementNode n = coarseNodes.get(i);
            
            System.err.printf("[%04d] %s\n", i, r.toString(invhom, x -> false));
            System.err.printf(" %4s  %s\n", "", n.localToString(irtg.getAutomaton()));
            System.err.printf(" %4s  inside(parent): %f\n", "", inside.get(NumbersCombine.combine(n.getParent(), r.getParent())));
            System.err.printf(" %4s  outside(parent): %f\n\n", "", outside.get(NumbersCombine.combine(n.getParent(), r.getParent())));
        }
    }

    private double computeInsideOutside(int level, List<RuleRefinementNode> coarseNodes, List<CondensedRule> partnerInvhomRules, CondensedTreeAutomaton invhom, Long2DoubleMap inside, Long2DoubleMap outside) {
        double totalSentenceInside = 0;

        inside.defaultReturnValue(0);
        outside.defaultReturnValue(0);

        // calculate coarse inside
        for (int i = 0; i < coarseNodes.size(); i++) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);

            double insideHere = n.getWeight() * r.getWeight();
            for (int j = 0; j < r.getArity(); j++) {
                insideHere *= inside.get(NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]));
            }

            long key = NumbersCombine.combine(n.getParent(), r.getParent());
            inside.put(key, inside.get(key) + insideHere);

            if (invhom.getFinalStates().contains(r.getParent()) && rrt.getFinalStatesAtLevel(level).contains(n.getParent())) {
                outside.put(key, 1);
                totalSentenceInside += insideHere;
            }
        }

        // calculate coarse outside
        for (int i = coarseNodes.size() - 1; i >= 0; i--) {
            RuleRefinementNode n = coarseNodes.get(i);
            CondensedRule r = partnerInvhomRules.get(i);
            long parentKey = NumbersCombine.combine(n.getParent(), r.getParent());

            double[] childInside = new double[r.getArity()];
            long[] childKey = new long[r.getArity()];
            double val = outside.get(parentKey) * n.getWeight() * r.getWeight();

            for (int j = 0; j < r.getArity(); j++) {
                childKey[j] = NumbersCombine.combine(n.getChildren()[j], r.getChildren()[j]);
                childInside[j] = inside.get(childKey[j]);
                val *= childInside[j];
            }

            // at this point: val = outside(parent) * P(rule) * inside(ch1) * ... * inside(chn)
            for (int j = 0; j < r.getArity(); j++) {
                outside.put(childKey[j], outside.get(childKey[j]) + val / childInside[j]); // take inside(ch_j) away
            }
        }

        return totalSentenceInside;
    }
}
