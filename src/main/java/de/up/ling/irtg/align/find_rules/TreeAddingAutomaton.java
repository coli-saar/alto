/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class TreeAddingAutomaton extends ConcreteTreeAutomaton<String> {

    /**
     * 
     */
    private final static String DEFAULT_STATE = "Default";
    
    /**
     * 
     */
    private final int mainState;
    
    /**
     * 
     * @param signature
     * @param smooth 
     */
    public TreeAddingAutomaton(Signature signature, Int2DoubleFunction smooth) {
        super(signature);
        
        this.mainState = this.addState(DEFAULT_STATE);
        this.getFinalStates().add(mainState);
        
        for(int i=1;i<signature.getMaxSymbolId();++i){
            int arity = signature.getArity(i);
            
            int[] children = new int[arity];
            Arrays.fill(children, this.mainState);
            
            makeRule(this.mainState,i,children, smooth.get(i));
        }
    }

    /**
     * 
     * @param mainState
     * @param i
     * @param children
     * @param smooth 
     */
    private void makeRule(int mainState, int label, int[] children, double smooth) {
        Rule r = this.createRule(mainState, label, children, smooth);
        this.addRule(r);
    }
    
    
    public void addVariableTree(Tree<String> t){
        //TODO
    }
}