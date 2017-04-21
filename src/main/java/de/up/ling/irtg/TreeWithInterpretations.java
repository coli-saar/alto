/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import de.up.ling.tree.Tree;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class TreeWithInterpretations {

    public static class InterpretationWithPointers {
        private Tree<String> homomorphicTerm;  // the image of that tree under the interpretation's homomorphism
        private Map<Tree<String>, Tree<String>> dtNodeToTermNode;  // a mapping from dt nodes to term nodes

        public Tree<String> getHomomorphicTerm() {
            return homomorphicTerm;
        }

        public Map<Tree<String>, Tree<String>> getDtNodeToTermNode() {
            return dtNodeToTermNode;
        }

        @Override
        public String toString() {
            return String.format("term: %s // mapping: %s", homomorphicTerm.toString(), dtNodeToTermNode.toString());
        }
        
        
    }

    private Tree<String> derivationTree;   // a derivation tree
    private Map<String, InterpretationWithPointers> interpretations; // map from interp names to InterpretationWithPointers objects

    public TreeWithInterpretations(Tree<String> derivationTree) {
        this.derivationTree = derivationTree;
        interpretations = new HashMap<>();
    }
    
    public void addInterpretation(String interp, Tree<String> homomorphicTerm, Map<Tree<String>, Tree<String>> dtNodeToTermNode) {
        InterpretationWithPointers iwp = new InterpretationWithPointers();
        iwp.homomorphicTerm = homomorphicTerm;
        iwp.dtNodeToTermNode = dtNodeToTermNode;
        interpretations.put(interp, iwp);
    }

    public Tree<String> getDerivationTree() {
        return derivationTree;
    }
    
    public InterpretationWithPointers getInterpretation(String interp) {
        return interpretations.get(interp);
    }
    
    public Iterable<String> getInterpretations() {
        return interpretations.keySet();
    }

    @Override
    public String toString() {
        return String.format("dt: %s\n%s\n", derivationTree, interpretations);
    }
}
