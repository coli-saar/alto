/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.grammar_post;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;

/**
 *
 * @author teichmann
 */
public class Stringify {
    /**
     * 
     * @param to
     * @param deriveFrom
     * @param newName 
     */
    public static void addStringInterpretation(InterpretedTreeAutomaton to, String deriveFrom, String newName) {
       StringAlgebra sal = new StringAlgebra();
        Homomorphism orig = to.getInterpretation(deriveFrom).getHomomorphism();
       
        Signature source = orig.getSourceSignature();
        Homomorphism hom = new Homomorphism(source, sal.getSignature());
        
        for(int symbol=1;symbol <= source.getMaxSymbolId();++symbol) {
            String s = source.resolveSymbolId(symbol);
                        if(orig.get(symbol) != null) {
                hom.add(s, treeRuleToStringRule(orig.get(s)));
            }
        }
        
        Interpretation inter = new Interpretation(sal, hom);
        to.addInterpretation(newName, inter);
    }
    
    
    /**
     * 
     * @param homomorphicImage
     * @return 
     */
    public static Tree<String> treeRuleToStringRule(Tree<String> homomorphicImage) {
        String label = homomorphicImage.getLabel();
        
        if(homomorphicImage.getChildren().size() == 1) {
            return treeRuleToStringRule(homomorphicImage.getChildren().get(0));
        }
        
        if(homomorphicImage.getChildren().isEmpty()) {
            if(label.equals(StringAlgebra.CONCAT)) {
                return Tree.create(StringAlgebra.SPECIAL_STAR);
            } else {
                return homomorphicImage;
            }
        }
        
        
        Tree<String>[] children = new Tree[2];
        
        int pos = 1;
        for(int i=homomorphicImage.getChildren().size()-1;i>=0;--i) {
            children[pos--] = treeRuleToStringRule(homomorphicImage.getChildren().get(i));
            
            if(pos < 0) {
                children[1] = Tree.create(StringAlgebra.CONCAT, children);
                pos = 0;
            }
        }
        
        return children[1];
    }
}
