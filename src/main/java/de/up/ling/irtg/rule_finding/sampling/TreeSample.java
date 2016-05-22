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
    public static double TOLERANCE = 3.0;
    
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
    private final DoubleArrayList targetWeights = new DoubleArrayList();
    
    /**
     * 
     */
    private final DoubleArrayList sumProposalWeight = new DoubleArrayList();
    
    /**
     * 
     */
    private final DoubleArrayList selfNormalizedWeight = new DoubleArrayList();
    
    /**
     * 
     * @param sample 
     */
    public void addSample(Tree<Type> sample) {
        this.samplesDrawn.add(sample);
        this.proposalWeight.add(0.0);
        this.targetWeights.add(0.0);
        this.sumProposalWeight.add(0.0);
        this.selfNormalizedWeight.add(0.0);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void setLogSumWeight(int entry, double amount) {
        this.sumProposalWeight.set(entry, amount);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void setLogPropWeight(int entry, double amount) {
        this.proposalWeight.set(entry, amount);
    }
    
    /**
     * 
     * @param entry
     * @param amount
     */
    public void setLogTargetWeight(int entry, double amount) {
        this.targetWeights.set(entry, amount);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getLogPropWeight(int entry) {
        return this.proposalWeight.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getLogSumWeight(int entry) {
        return this.sumProposalWeight.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getLogTargetWeight(int entry) {
        return this.targetWeights.get(entry);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getSelfNormalizedWeight(int entry) {
        return this.selfNormalizedWeight.get(entry);
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
     * @param deterministic 
     */
    public void expoNormalize(boolean deterministic){
        double sum = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        for(int i=0;i<this.populationSize();++i) {
            double amount = this.targetWeights.get(i);
            amount -= !deterministic ? this.getLogSumWeight(i) : this.getLogPropWeight(i);
            
            this.selfNormalizedWeight.set(i, amount);
            max = Math.max(max, amount);
        }
        
        for(int i=0;i<this.populationSize();++i) {
            double amount = Math.exp(this.selfNormalizedWeight.get(i)-max);
            if(!Double.isFinite(amount)) {
                amount = 0.0;
            }
            
            sum += amount;
            
            this.selfNormalizedWeight.set(i, amount);
        }
        
        for(int i=0;i<this.populationSize();++i) {
            this.selfNormalizedWeight.set(i, this.selfNormalizedWeight.get(i)/sum);
        }
    }

    /**
     * 
     * @return 
     */
    public int populationSize() {
        return this.samplesDrawn.size();
    }
    
    /**
     * 
     * @param rg
     * @param size
     * @param deterministic 
     */
    public void flatten(RandomGenerator rg, int size, boolean deterministic) {
        this.expoNormalize(deterministic);
        this.resample(rg, size);
        
        ArrayList<Tree<Type>> newChoices = new ArrayList<>();
        DoubleList newValues = new DoubleArrayList();
        
        double frac = 1.0 / size;
        for(int i=0;i<this.populationSize();++i) {
            int num = (int) (this.getSelfNormalizedWeight(i)*size);
            
            for(int k=0;k<num;++k) {
                newChoices.add(this.getSample(i));
                newValues.add(frac);
            }
        }
        
        this.samplesDrawn.clear();
        this.samplesDrawn.addAll(newChoices);
                
        this.selfNormalizedWeight.clear();
        this.selfNormalizedWeight.addAll(newValues);
    }
    
    /**
     * 
     * @param rg
     * @param size
     * @param deterministic 
     */
    public void resampleWithNormalize(RandomGenerator rg, int size, boolean deterministic) {
        this.expoNormalize(deterministic);
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
            double amount = Math.floor(dsize*this.getSelfNormalizedWeight(i)) / dsize;
            
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
                d -= this.getSelfNormalizedWeight(i);
                
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
        
        this.selfNormalizedWeight.clear();
        this.selfNormalizedWeight.addAll(newWeights);
    }

    @Override
    public String toString() {
        String s = System.lineSeparator();
        return "TreeSample"+ s + "samplesDrawn = " + samplesDrawn + s + "sampleWeights = " + proposalWeight;
    }    

    /**
     * 
     * @param deterministic
     * @param originalBase
     * @return 
     */
    public double makeMaxBase(boolean deterministic, double originalBase) {
        double max = Double.NEGATIVE_INFINITY;
        
        for(int i=0;i<this.populationSize();++i){
            double amount = this.getLogTargetWeight(i);
            amount -= deterministic ? this.getLogPropWeight(i) : this.getLogSumWeight(i);
            
            max = Math.max(max, amount);
            this.selfNormalizedWeight.set(i, amount);
        }
        
        if(originalBase+3 >= max) {
            max = originalBase;
        }
        
        for(int i=0;i<this.populationSize();++i) {
            double d = Math.exp(this.selfNormalizedWeight.get(i)-max);
            if(!Double.isFinite(d)) {
                d = 0.0;
            }            
            this.selfNormalizedWeight.set(i, d);
        }
        
        return max;
    }

    /**
     * 
     */
    public void clear() {
        this.proposalWeight.clear();
        this.samplesDrawn.clear();
        this.selfNormalizedWeight.clear();
        this.sumProposalWeight.clear();
        this.targetWeights.clear();
    }
}
