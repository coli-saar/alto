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
    private final String[] labels;
    
    /**
     * 
     */
    private final double maxWeight;
    
    /**
     * 
     * @param rg
     * @param labels
     * @param maxWeight 
     */
    public RandomTreeAutomaton(RandomGenerator rg, String[] labels, double maxWeight) {
        this.rg = rg;
        this.labels = labels;
        this.maxWeight = maxWeight;
    }
    
    /**
     * 
     * @param stateNum
     * @param maxRules
     * @return 
     */
    public TreeAutomaton getRandomAutomaton(int stateNum) {
        stateNum = Math.max(1, stateNum);
        
        IntList states = new IntArrayList();
        ConcreteTreeAutomaton<Integer> conTau = new ConcreteTreeAutomaton<>();
        
        int nextState = 0;
        int pos = 0;
        for(int i=0;i<stateNum;++i) {
            Integer num = nextState++;
            int state = conTau.addState(num);
            
            int rules = this.rg.nextInt(labels.length)+1;
            shuffle(labels);
            
            for(int j=0;j<rules;++j) {
                String label = labels[j];
                
                if(states.isEmpty()) {
                    conTau.addRule(conTau.createRule(num, label+"_0", new Integer[0], this.maxWeight*this.rg.nextDouble()));
                } else {
                    int left = states.get(pos++);
                    if(pos >= states.size()) {
                        pos = 0;
                    }
                
                    int right = states.get(pos++);
                    if(pos >= states.size()) {
                        pos = 0;
                    }
                    
                    int lab = conTau.getSignature().addSymbol(label+"_2", 2);
                    
                    conTau.addRule(conTau.createRule(state, lab, new int[] {left,right},maxWeight*this.rg.nextDouble()));
                }
            }
            
            states.add(state);
        }
        
        int state = -1;
        
        boolean done = false;
        for(;pos<states.size() && !done;) {
            Integer num = nextState++;
            state = conTau.addState(num);
            
            for(int i=0;i<labels.length && pos < states.size() && !done;++i) {
                int left = states.get(pos++);
                if(pos >= states.size()) {
                    int lab = conTau.getSignature().addSymbol(labels[i]+"_1", 1);
                    
                    conTau.addRule(conTau.createRule(state, lab, new int[] {left},maxWeight*this.rg.nextDouble()));
                    done = true;
                } else {
                
                int right = states.get(pos++);
                if(pos >= states.size()) {
                    done = true;
                }
                    
                int lab = conTau.getSignature().addSymbol(labels[i]+"_2", 2);
                conTau.addRule(conTau.createRule(state, lab, new int[] {left,right},maxWeight*this.rg.nextDouble()));
                }
            }
            
            states.add(state);
        }
        
        conTau.addFinalState(state);
        
        return conTau;
    }

    private void shuffle(String[] labels) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
