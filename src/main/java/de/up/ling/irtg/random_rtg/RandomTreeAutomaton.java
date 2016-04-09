/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.random_rtg;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.random.RandomGenerator;

/**
 *
 * @author teichmann
 */
public class RandomTreeAutomaton {
    /**
     * 
     */
    private final RandomGenerator rg;
    
    /**
     * 
     */
    private final String label;
    
    /**
     * 
     */
    private final double maxWeight;
    
    /**
     * 
     * @param rg
     * @param label
     * @param maxWeight 
     */
    public RandomTreeAutomaton(RandomGenerator rg, String label, double maxWeight) {
        this.rg = rg;
        this.label = label;
        this.maxWeight = maxWeight;
    }
    
    /**
     * 
     * @param stateNum
     * @param maxRules
     * @return 
     */
    public TreeAutomaton getRandomAutomaton(int stateNum, int maxRules) {
        IntList states = new IntArrayList();
        ConcreteTreeAutomaton<Integer> conTau = new ConcreteTreeAutomaton<>();
        
        int label0 = conTau.getSignature().addSymbol(this.label+"_"+0, 0);
        int label2 = conTau.getSignature().addSymbol(this.label+"_"+2, 2);
        
        int nextState = 0;
        int pos = 0;
        for(int i=0;i<stateNum;++i) {
            Integer num = nextState++;
            int state = conTau.addState(num);
            
            int rules = this.rg.nextInt(maxRules)+1;
            for(int j=0;j<rules;++j) {                             
                if(states.isEmpty()) {
                    conTau.addRule(conTau.createRule(state, label0, new int[0],maxWeight*this.rg.nextDouble()));
                    break;
                }
                
                int left = states.get(pos++);
                if(pos >= states.size()) {
                    pos = 0;
                }
                
                int right = states.get(pos++);
                if(pos >= states.size()) {
                    pos = 0;
                }
                
                conTau.addRule(conTau.createRule(state, label2, new int[] {left,right},maxWeight*this.rg.nextDouble()));
            }
            
            states.add(state);
        }
        
        Integer num = nextState++;
        int state = conTau.addState(num);
        
        for(int j=0;j<maxRules;++j) {                             
            if(states.isEmpty()) {
                conTau.addRule(conTau.createRule(state, label0, new int[0],maxWeight*this.rg.nextDouble()));
                break;
            }
                
            int left = states.get(pos++);
            if(pos >= states.size()) {
                pos = 0;
            }
                
            int right = states.get(pos++);
            if(pos >= states.size()) {
                pos = 0;
            }
                
            conTau.addRule(conTau.createRule(state, label2, new int[] {left,right},maxWeight*this.rg.nextDouble()));
        }
        
        return conTau;
    }
}
