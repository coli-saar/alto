/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.IntDoubleFunction;
import de.up.ling.tree.Tree;
import java.util.Arrays;

/**
 *
 * @author christoph_teichmann
 */
public class TreeAddingAutomaton extends ConcreteTreeAutomaton<String> {

    /**
     * 
     */
    private final static String DEFAULT_STATE = "DEFAULT";
    
    /**
     * 
     */
    private final static String START_STATE = "START";
    
    /**
     * 
     */
    private final int defaultState;
    
    /**
     * 
     */
    private final int startState;
    
    /**
     * 
     */
    private final VariableIndication indicator;
    
    /**
     * 
     * @param signature
     * @param smooth 
     * @param indicator 
     */
    public TreeAddingAutomaton(Signature signature, IntDoubleFunction smooth,
                                VariableIndication indicator) {
        super(signature);
        this.indicator = indicator;
        
        this.defaultState = this.addState(DEFAULT_STATE);
        this.startState = this.addState(START_STATE);
        
        this.getFinalStates().add(startState);
        
        for(int i=1;i<=signature.getMaxSymbolId();++i){
            if(indicator.isIgnorableVariable(i)){
                continue;
            }
            
            int arity = signature.getArity(i);
            
            int[] children = new int[arity];
            
            if(indicator.isVariable(i)){
                Arrays.fill(children, this.startState);
            }else{
                Arrays.fill(children, this.defaultState);
            }
            
            makeRule(this.defaultState,i, children, smooth.apply(i));
            if(!indicator.isVariable(i)){
                makeRule(this.startState,i,children, smooth.apply(i));
            }
        }
        
        this.normalizeRuleWeights();
    }

    /**
     * 
     * @param mainState
     * @param i
     * @param children
     * @param smooth 
     */
    private Rule makeRule(int mainState, int label, int[] children, double smooth) {
        Rule r = this.createRule(mainState, label, children, smooth);
        this.addRule(r);
        
        return r;
    }
    
    /**
     * 
     * @param t 
     * @param amount 
     */
    public void addVariableTree(Tree<Integer> t, double amount){
        this.addVariableTree(t, true, amount);
    }
    
    /**
     * 
     * @param t
     * @return 
     */
    private int addVariableTree(Tree<Integer> t, boolean toStartState, double amount){
        StringBuilder sb = new StringBuilder();
        sb.append(this.getSignature().resolveSymbolId(t.getLabel()));
        sb.append("(");
        int state;
        
        int code = t.getLabel();
        
        int[] children = new int[t.getChildren().size()];
        boolean childStart = this.indicator.isVariable(code);
        
        for(int i=0;i<t.getChildren().size();++i){          
            children[i] = addVariableTree(t.getChildren().get(i), childStart, amount);
            
            if(i != 0){
                sb.append(", ");
            }
            sb.append(this.getStateForId(children[i]));
        }
        
        if(toStartState){
            state = this.startState;
        }else{
            sb.append(")");
            state = this.addState(sb.toString());
        }
        
        Iterable<Rule> it = this.getRulesBottomUp(code, children);
        Rule r = null;
        for(Rule ir : it){
            if(ir.getParent() == state){
                r = ir;
                break;
            }
        }
        
        if(r == null){
            makeRule(state, code, children, amount);
        }    
        else if(toStartState){
            r.setWeight(r.getWeight()+amount);
        }
        
        return state;
    }

    /**
     * 
     */
    public final void normalizeStart() {
        double sum = 0.0;
        
        Iterable<Rule> it = this.getRulesTopDown(startState);
        
        for(Rule r : it){
            sum += r.getWeight();
        }
        
        for(Rule r : it){
            r.setWeight(r.getWeight() / sum);
        }
    }
}