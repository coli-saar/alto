/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.grammar_post;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 * @author teichmann
 */
public class ExtendStringRules {
    /**
     * 
     * @param ita
     * @param interpretation
     * @param sigs
     * @param smooth
     * @param symbols 
     */
    public static void extend(InterpretedTreeAutomaton ita, String interpretation, Iterable<Signature> sigs, double smooth,
                        Supplier<String> symbols) {
        Interpretation inter = ita.getInterpretation(interpretation);
        Signature sig = inter.getAlgebra().getSignature();
        
        ConcreteTreeAutomaton cta = ita.getAutomaton().asConcreteTreeAutomaton();
        
        for(int sym=1;sym<=sig.getMaxSymbolId();++sym) {
            if(sig.getArity(sym) != 0) {
                continue;
            }
            
            String symbol = sig.resolveSymbolId(sym);
            
            addSymbol(symbol, ita,interpretation,smooth,symbols,cta);
        }
        
        for(Signature sign : sigs) {
            for(int sym=1;sym<=sig.getMaxSymbolId();++sym) {
                if(sig.getArity(sym) != 0) {
                    continue;
                }
                
                String symbol = sig.resolveSymbolId(sym);
            
                addSymbol(symbol, ita,interpretation,smooth,symbols,cta);
            }
        }
        
        InterpretedTreeAutomaton qta = new InterpretedTreeAutomaton(cta);
        qta.addAllInterpretations(ita.getInterpretations());
        
        qta.normalizeRuleWeights();
    }

    /**
     * 
     * @param ita
     * @param interpretation
     * @param smooth
     * @param symbols 
     */
    private static void addSymbol(String symbol, InterpretedTreeAutomaton ita, String interpretation, double smooth, Supplier<String> symbols,
                                        ConcreteTreeAutomaton cta) {
        Tree<String> left = Tree.create(symbol);
        left = Tree.create(StringAlgebra.CONCAT, left,Tree.create("?1"));
        
        Tree<String> right = Tree.create(symbol);
        right = Tree.create(StringAlgebra.CONCAT, Tree.create("?1"), right);
        
        Tree<String> defaul = Tree.create("?1");
        
        TreeAutomaton ta = ita.getAutomaton();
        
        IntIterator states = ta.getAllStates().iterator();
        while(states.hasNext()) {
            int state = states.nextInt();
            
            String s = symbols.get();
            int label = ta.getSignature().addSymbol(s, 1);
            
            cta.addRule(cta.createRule(state, label, new int[] {state}, smooth));
            
            for(Map.Entry<String,Interpretation> ent : ita.getInterpretations().entrySet()) {
                if(ent.getKey().equals(interpretation)) {
                    ent.getValue().getHomomorphism().add(symbol, left);
                } else {
                    ent.getValue().getHomomorphism().add(symbol, defaul);
                }
            }
            
            s = symbols.get();
            label = ta.getSignature().addSymbol(s, 1);
            
            cta.addRule(cta.createRule(state, label, new int[] {state}, smooth));
            
            for(Map.Entry<String,Interpretation> ent : ita.getInterpretations().entrySet()) {
                if(ent.getKey().equals(interpretation)) {
                    ent.getValue().getHomomorphism().add(symbol, right);
                } else {
                    ent.getValue().getHomomorphism().add(symbol, defaul);
                }
            }
        }
    }
}
