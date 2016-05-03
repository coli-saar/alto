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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        
        if(labels.length < 1) {
            throw new IllegalArgumentException("Need at least one label.");
        }
        
        if(rg == null) {
            throw new IllegalArgumentException("Random number generator cannot be null.");
        }
    }

    /**
     * 
     * @param stateNum
     * @param maxArity
     * @param annealingFactor
     * @return 
     */
    public TreeAutomaton<Integer> getRandomAutomaton(int stateNum, int maxArity, double annealingFactor) {
        stateNum = Math.max(1, stateNum);

        ArrayList<int[]> choices = new ArrayList<>();
        IntList states = new IntArrayList();
        ConcreteTreeAutomaton<Integer> conTau = new ConcreteTreeAutomaton<>();

        int nextState = 0;

        for (String label : labels) {
            Integer num = nextState++;
            int state = conTau.addState(num);

            conTau.addRule(conTau.createRule(num, label + "_0", new Integer[0], makeWeight(annealingFactor)));
            addNewState(state,states,choices,labels);
        }

        for(int i=0;i<stateNum;++i) {
            Integer num = nextState++;
            int state = conTau.addState(num);

            int rules = this.rg.nextInt(maxArity) + 1;
            shuffle(labels);
            shuffle(states);
            shuffle(choices);

            for (int j = 0; j < rules && !choices.isEmpty(); ++j) {
                int[] choice = choices.remove(choices.size()-1);
                
                String label = labels[choice[2]];

                int left = choice[0];
                int right = choice[1];

                int lab = conTau.getSignature().addSymbol(label + "_2", 2);
                conTau.addRule(conTau.createRule(state, lab, new int[]{left, right}, makeWeight(annealingFactor)));
            }

            addNewState(state,states,choices,labels);
        }

        int state = -1;
        int pos = 0;
        boolean done = false;
        String[] small = new String[] {this.labels[0]};
        while(!choices.isEmpty()) {
            Integer num = nextState++;
            state = conTau.addState(num);

            int rules = maxArity;

            for (int j = 0; j < rules && !choices.isEmpty(); ++j) {
                int[] choice = choices.remove(choices.size()-1);
                
                String label = labels[choice[2]];

                int left = choice[0];
                int right = choice[1];

                int lab = conTau.getSignature().addSymbol(label + "_2", 2);
                conTau.addRule(conTau.createRule(state, lab, new int[]{left, right}, makeWeight(annealingFactor)));
            }
            
            if(!choices.isEmpty()) {               
                addNewState(state,states,choices,small);
            }
        }

        conTau.addFinalState(state);

        return conTau;
    }

    /**
     * 
     * @param annealing
     * @return 
     */
    private double makeWeight(double annealing) {
        return Math.pow(this.maxWeight * this.rg.nextDouble(), annealing);
    }

    private void shuffle(String[] labels) {
        for (int i = 0; i < labels.length; ++i) {
            String l = labels[i];
            int pos = i + this.rg.nextInt(labels.length - i);

            labels[i] = labels[pos];
            labels[pos] = l;
        }
    }

    private void shuffle(IntList states) {
        for (int i = 0; i < states.size(); ++i) {
            int state = states.get(i);
            int pos = i + this.rg.nextInt(states.size() - i);

            states.set(i, states.get(pos));
            states.set(pos, state);
        }
    }
    
    /**
     * 
     * @param l 
     */
    private void shuffle(List l) {
        for (int i = 0; i < l.size(); ++i) {
            Object o = l.get(i);
            int pos = i + this.rg.nextInt(l.size() - i);

            l.set(i, l.get(pos));
            l.set(pos, o);
        }
    }

    /**
     * 
     * @param state
     * @param states
     * @param choices
     * @param labels 
     */
    private void addNewState(int state, IntList states, ArrayList<int[]> choices, String[] labels) {
        states.add(state);
        
        for(int i=0;i<labels.length && i < states.size();++i) {
            if(this.rg.nextBoolean()) {
                choices.add(new int[] {states.get(i),state,i});
            } else {
                choices.add(new int[] {state,states.get(i),i});
            }
        }
    }
}
