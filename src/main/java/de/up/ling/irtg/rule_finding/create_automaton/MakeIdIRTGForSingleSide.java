/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author teichmann
 */
public class MakeIdIRTGForSingleSide {
    /**
     * 
     * @param in
     * @param algebraName
     * @param alg
     * @return 
     */
    public static InterpretedTreeAutomaton makeIRTG(TreeAutomaton in, String algebraName, Algebra alg) {
        InterpretedTreeAutomaton ita = new InterpretedTreeAutomaton(in);
        
        Signature source = in.getSignature();
        Homomorphism hom = new Homomorphism(source, alg.getSignature());
        
        List<Tree<String>> children = new ArrayList<>();
        for(int label=1;label<=source.getMaxSymbolId();++label) {
            String terminal = source.resolveSymbolId(label);
            
            children.clear();
            for(int i=0;i<source.getArity(label);++i) {
                children.add(Tree.create("?"+(i+1)));
            }
            
            hom.add(terminal, Tree.create(terminal, children));
        }
        
        Interpretation it = new Interpretation(alg, hom);
        ita.addInterpretation(algebraName, it);
        
        return ita;
    }
}
