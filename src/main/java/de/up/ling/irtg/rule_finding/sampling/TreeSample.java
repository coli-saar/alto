/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling;

import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.random.RandomGenerator;

/**
 *
 * @author christoph
 * @param <Type>
 */
public class TreeSample<Type> {
    /**
     * 
     */
    private final List<Tree<Type>> samplesDrawn = new ArrayList<>();
    
    /**
     * 
     */
    private final DoubleArrayList proposalWeight = new DoubleArrayList();
    
    /**
     * 
     */
    private final DoubleArrayList goalWeights = new DoubleArrayList();
    
    /**
     * 
     */
    private final DoubleArrayList sumProposalWeight = new DoubleArrayList();
    
    /**
     * 
     */
    private final DoubleArrayList normalized = new DoubleArrayList();
    
    /**
     * 
     * @param sample 
     */
    public void addSample(Tree<Type> sample) {
        this.samplesDrawn.add(sample);
        this.proposalWeight.add(0.0);
        this.goalWeights.add(0.0);
        this.sumProposalWeight.add(0.0);
        this.normalized.add(0.0);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void addSumProposal(int entry, double amount) {
        this.proposalWeight.set(entry, this.sumProposalWeight.get(entry)+amount);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void addProposal(int entry, double amount) {
        this.proposalWeight.set(entry, this.proposalWeight.get(entry)+amount);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void addGoalWeight(int entry, double amount) {
        this.goalWeights.set(entry, this.goalWeights.get(entry)+amount);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getProposalWeight(int entry) {
        return this.proposalWeight.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getSumProposalWeight(int entry) {
        return this.sumProposalWeight.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getGoalWeight(int entry) {
        return this.goalWeights.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getNormalized(int entry) {
        return this.normalized.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public Tree<Type> getSample(int entry) {
        return this.samplesDrawn.get(entry);
    }
    
    /**
     * 
     */
    public void expoNormalize(){
        double sum = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        for(int i=0;i<this.populationSize();++i) {
            double amount = this.goalWeights.get(i);
            amount -= this.getSumProposalWeight(i);
            
            this.normalized.set(i, amount);
            max = Math.max(max, amount);
        }
        
        for(int i=0;i<this.populationSize();++i) {
            double amount = Math.exp(this.getNormalized(i)-max);
            sum += amount;
            
            this.normalized.set(i, amount);
        }
        
        for(int i=0;i<this.populationSize();++i) {
            this.normalized.set(i, this.getNormalized(i)/sum);
        }
    }

    /**
     * 
     * @return 
     */
    public int populationSize() {
        return this.proposalWeight.size();
    }
    
    /**
     * 
     * @param rg
     * @param size 
     */
    public void resampleWithNormalize(RandomGenerator rg, int size) {
        this.expoNormalize();
        this.resample(rg, size);
    }
    
    /**
     * 
     * @param rg
     * @param size 
     */
    public void resample(RandomGenerator rg, int size) {
        double dsize = (double) size;
        
        List<Tree<Type>> newPop = new ArrayList<>();
        DoubleList newWeights = new DoubleArrayList();
        
        double sum = 0.0;
        for(int i=0;i<this.populationSize();++i) {
            double amount = Math.floor(dsize*this.getNormalized(i)) / dsize;
            
            if(amount > 0.0) {
                newPop.add(this.getSample(i));
                newWeights.add(amount);
                
                sum += amount;
            }
        }
        
        double frac = 1.0 / dsize;
        while(sum < 1.0) {
            double d = rg.nextDouble();
            boolean done = false;
            
            for(int i=0;(i<this.populationSize() && (!done));++i) {
                d -= this.getNormalized(i);
                
                if(d <= 0.0) {
                    done = true;
                    
                    newPop.add(this.getSample(i));
                    newWeights.add(frac);
                    
                    sum += frac;
                }
            }
        }
        
        this.samplesDrawn.clear();
        this.samplesDrawn.addAll(newPop);
        
        this.normalized.clear();
        this.normalized.addAll(newWeights);
    }

    @Override
    public String toString() {
        String s = System.lineSeparator();
        return "TreeSample"+ s + "samplesDrawn = " + samplesDrawn + s + "sampleWeights = " + proposalWeight;
    }    
}
