/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.coarse_to_fine;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class GrammarCoarsifier {

    private final FineToCoarseMapping ftc;

    public GrammarCoarsifier(FineToCoarseMapping ftcMapping) {
        this.ftc = ftcMapping;
    }

    public RuleRefinementTree coarsify(InterpretedTreeAutomaton irtg, String inputInterpretation) {
        Homomorphism inputHom = irtg.getInterpretation(inputInterpretation).getHomomorphism();
        Signature sig = irtg.getAutomaton().getSignature();
        Interner<String> stateInterner = irtg.getAutomaton().getStateInterner();
        Map<Rule, RuleRefinementNode> ruleToFinestNode = new HashMap<>();
        List<RuleRefinementNode> coarse = null;
        Set<String> coarseFinalStates = null;
        IntSet coarseFinalStateIds = null;
        List<IntSet> finalStatesPerLevel = new ArrayList<>();

        // make finest layer
        Set<String> fineFinalStates = Util.mapToSet(irtg.getAutomaton().getFinalStates(), irtg.getAutomaton()::getStateForId);
        finalStatesPerLevel.add(irtg.getAutomaton().getFinalStates());

        List<RuleRefinementNode> fine = new ArrayList<>();
        for (Rule rule : irtg.getAutomaton().getRuleSet()) {
            RuleRefinementNode n = RuleRefinementNode.makeFinest(rule, inputHom);
            fine.add(n);
            ruleToFinestNode.put(rule, n);
        }

        for (int i = 1; i < ftc.numLevels(); i++) { // iterate numLevels-1 times
            // make "fine" rules one step coarser
            coarse = new ArrayList<>();
            ListMultimap<RrtSummary, RuleRefinementNode> equivalenceClasses = Util.groupBy(fine, node -> summarize(node, stateInterner, inputHom));
            // TODO - maybe this can be made more efficient by only attempting to merge classes that already have the same term ID

            for (RrtSummary summary : equivalenceClasses.keySet()) {
                List<RuleRefinementNode> equivClass = equivalenceClasses.get(summary);
                RuleRefinementNode coarser = RuleRefinementNode.makeCoarser(equivClass, summary);
                coarse.add(coarser);
            }

            fine = coarse;

            // make "fine" final states one step coarser
            coarseFinalStates = new HashSet<>();
            coarseFinalStateIds = new IntOpenHashSet();
            for (String ffs : fineFinalStates) {
                String cfs = ftc.coarsify(ffs);
                coarseFinalStates.add(cfs);
                coarseFinalStateIds.add(stateInterner.resolveObject(cfs));
            }

            finalStatesPerLevel.add(coarseFinalStateIds);
            fineFinalStates = coarseFinalStates;
        }

        return new RuleRefinementTree(fine, Lists.reverse(finalStatesPerLevel), ruleToFinestNode::get);
    }

    private RrtSummary summarize(RuleRefinementNode node, Interner<String> stateInterner, Homomorphism hom) {
        int coarseParent = coarsifyState(node.getParent(), stateInterner);
        int[] coarseChildren = Util.mapIntArray(node.getChildren(), q -> coarsifyState(q, stateInterner));
        int termId = hom.getTermID(node.getRepresentativeLabel());

        return new RrtSummary(coarseParent, termId, coarseChildren);
    }

    private int coarsifyState(int fineState, Interner<String> stateInterner) {
        return stateInterner.addObject(ftc.coarsify(stateInterner.resolveId(fineState)));
    }

    @OperationAnnotation(code = "readFtcMapping")
    public static FineToCoarseMapping readFtcMapping(String s) throws IOException, ParseException {
        Tree<String> t = TreeParser.parse(s); // top-level tree with "___"
        final Map<String, String> fineSymbolToCoarse = new HashMap<>();

        t.dfs((Tree<String> node, List<String> children) -> {
            if (node != t) {
                String label = node.getLabel();
                children.forEach(child -> {
                    fineSymbolToCoarse.put(child, label); // Exp: child NP | label N_
                    fineSymbolToCoarse.put(child + "-bar", label + "-bar"); // for xbar-binarized grammars
                });
                return label;
            } else {
                return null;
            }
        });

        // depth = t.getHeight() + 1 (count layer of leaves) - 1 (ignore top-level "___" symbol)
        int depth = t.getHeight();

        return new FineToCoarseMapping() {
            @Override
            public String coarsify(String symbol) {
                if (symbol.contains("_")) {
                    // for nonterminals created by the "inside" binarization strategy
                    String[] parts = symbol.split("_");
                    StringBuilder buf = new StringBuilder();
                    
//                    System.err.println("\nsym: " + symbol);
//                    System.err.println("#parts: " + parts.length);
                    
                    for (int i = 0; i < parts.length; i++) {
                        if( i > 0) {
                            buf.append("_");
                        }
                        buf.append(fineSymbolToCoarse.getOrDefault(parts[i], parts[i]));
                    }
                    
//                    System.err.println(" -> " + buf.toString());
                    return buf.toString();
                } else {
                    return fineSymbolToCoarse.getOrDefault(symbol, symbol);
                }
            }

            @Override
            public int numLevels() {
                return depth;
            }
        };
    }

}
