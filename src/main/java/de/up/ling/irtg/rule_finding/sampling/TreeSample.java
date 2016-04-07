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
    private final DoubleArrayList sampleWeights = new DoubleArrayList();
    
    /**
     * 
     * @param sample
     * @param weight 
     */
    public void addSample(Tree<Type> sample, double weight) {
        this.samplesDrawn.add(sample);
        this.sampleWeights.add(weight);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void addWeight(int entry, double amount) {
        this.sampleWeights.set(entry, this.sampleWeights.get(entry)+amount);
    }
    
    /**
     * 
     * @param entry
     * @param amount 
     */
    public void multiplyWeight(int entry, double amount) {
        this.sampleWeights.set(entry, this.sampleWeights.get(entry)*amount);
    }
    
    /**
     * 
     * @param entry
     * @return 
     */
    public double getWeight(int entry) {
        return this.sampleWeights.get(entry);
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
            max = Math.max(max, this.getWeight(i));
        }
        
        for(int i=0;i<this.sampleWeights.size();++i) {
            double amount = Math.exp(this.getWeight(i)-max);
            sum += amount;
            this.sampleWeights.set(i, amount);
        }
        
        for(int i=0;i<this.sampleWeights.size();++i) {
            this.sampleWeights.set(i, this.getWeight(i)/sum);
        }
    }

    /**
     * 
     * @return 
     */
    public int populationSize() {
        return this.sampleWeights.size();
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
            double amount = Math.floor(dsize*this.getWeight(i)) / dsize;
            
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
                d -= this.getWeight(i);
                
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
        
        this.sampleWeights.clear();
        this.sampleWeights.addAll(newWeights);
    }

    @Override
    public String toString() {
        String s = System.lineSeparator();
        return "TreeSample"+ s + "samplesDrawn = " + samplesDrawn + s + "sampleWeights = " + sampleWeights;
    }
    
    
}
