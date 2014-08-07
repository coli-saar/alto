/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class PatternMatchingInvhomAutomaton {

    // caveat: signature(auto) != signature
    public static void addToPatternMatchingAutomaton(Tree<HomomorphismSymbol> rhs, String prefix, final ConcreteTreeAutomaton<String> auto, Signature signature) {
        String qf = prefix + "f";
        String q0 = prefix;
        String qmatch = prefix + "/";

        auto.addFinalState(auto.addState(qf));
        auto.addFinalState(auto.addState(qmatch));

        List<String> pathsToVariables = new ArrayList<>();
        extractVariables(rhs, pathsToVariables, "");

        for (String sym : signature.getSymbols()) {
            int arity = signature.getArityForLabel(sym);

            for (int q1pos = 0; q1pos < arity; q1pos++) {
                final int _q1pos = q1pos; // for access from lambda expr

                // path from root to match
                List<String> children = Util.makeList(arity, i -> i == _q1pos ? qf : q0);
                auto.addRule(auto.createRule(qf, sym, children));

                // transition into matching tree
                children = Util.makeList(arity, i -> i == _q1pos ? qmatch : q0);
                auto.addRule(auto.createRule(qf, sym, children));
            }

            // transitioning out of variable nodes
            for (String path : pathsToVariables) {
                auto.addRule(auto.createRule(qmatch + path, sym, Util.makeList(arity, () -> q0)));
            }

            // nodes below of or disjoint from match
            auto.addRule(auto.createRule(q0, sym, Util.makeList(arity, () -> q0)));
        }

        // add transitions within matcher
        addMatcherTransitions(rhs, qmatch, auto, signature);
    }

    private static void extractVariables(Tree<HomomorphismSymbol> rhs, List<String> pathsToVariables, String path) {
        if (rhs.getLabel().isVariable()) {
            pathsToVariables.add(path);
        }

        for (int i = 0; i < rhs.getChildren().size(); i++) {
            extractVariables(rhs.getChildren().get(i), pathsToVariables, path + (i + 1));
        }
    }

    private static void addMatcherTransitions(Tree<HomomorphismSymbol> rhs, String parent, ConcreteTreeAutomaton<String> auto, Signature signature) {
        String sym = signature.resolveSymbolId(rhs.getLabel().getValue());
        
        if( ! rhs.getLabel().isVariable() ) {            
            auto.addRule(auto.createRule(parent, sym, Util.makeList(rhs.getChildren().size(), i -> parent + (i+1))));
        }
        
        for (int i = 0; i < rhs.getChildren().size(); i++) {
            addMatcherTransitions(rhs.getChildren().get(i), parent + (i + 1), auto, signature);
        }
    }
}
