/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.strings;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.signature.Signature;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;

/**
 *
 * @author christoph
 */
public class RightBranchingNormalForm extends ConcreteTreeAutomaton<RightBranchingNormalForm.State> {

    /**
     * 
     * @param signature 
     * @param allLabels 
     */
    public RightBranchingNormalForm(Signature signature, IntSet allLabels) {
        super(signature);
        
        int gen = this.addState(State.GENERAL);
        int term = this.addState(State.TERMINATE);
        int var = this.addState(State.VARIABLE);
        
        this.finalStates.add(gen);
        
        IntIterator symbols  = allLabels.iterator();
        while(symbols.hasNext()){
            int symbol = symbols.nextInt();
            String label = signature.resolveSymbolId(symbol);
            int arity = signature.getArity(symbol);
            
            if(Variables.IS_VARIABLE.test(label)){
                this.addRule(this.createRule(var, symbol, new int[] {var}, 1.0));
                this.addRule(this.createRule(var, symbol, new int[] {gen}, 1.0));
                this.addRule(this.createRule(gen, symbol, new int[] {gen}, 1.0));
            }else if(arity <= 0){
                this.addRule(this.createRule(gen, symbol, new int[] {}, 1.0));
                this.addRule(this.createRule(term, symbol, new int[] {}, 1.0));
            }else{
                int[] children = new int[arity];
                children[0] = term;
                for(int i=1;i<children.length;++i){
                    children[i] = gen;
                }
                this.addRule(this.createRule(gen, symbol, children, 1.0));
                
                children = Arrays.copyOf(children, children.length);
                children[0] = var;
                this.addRule(this.createRule(gen, symbol, children, 1.0));
            }
        }
    }
    
    /**
     * 
     */
    public enum State {
        GENERAL,
        TERMINATE,
        VARIABLE;
    }
}
