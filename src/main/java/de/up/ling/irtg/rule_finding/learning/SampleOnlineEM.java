/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import com.google.common.base.Function;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.rule_finding.sampling.RuleCountBenign;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign.Configuration;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.BiFunctionParallelIterable;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph
 */
public class SampleOnlineEM {
    
    /**
     * 
     */
    private double samplerSmooth = 0.1;
    
    /**
     * 
     */
    private int sampleSize = 1000;
    
    /**
     * 
     */
    private int learnSampleSize  = 100;
    
    /**
     * 
     */
    private double learnSize = 50;

    /**
     * 
     */
    private int adaptionRounds = 20;
    
    /**
     * 
     */
    private int trainIterations = 20;
    
    /**
     * 
     */
    private int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    
    /**
     * 
     * @param data
     * @param mod
     * @param seed
     * @return 
     */
    public Iterable<Tree<String>> getChoices(Iterable<TreeAutomaton> data, Model mod, long seed) {
        Well44497b seeder = new Well44497b(seed);
        
        Iterable<Object> allNull = () -> new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return true;
            }
            
            @Override
            public Object next() {
                return null;
            }
        };
            
        Configuration config = new Configuration(mod);
        config.setRounds(this.adaptionRounds);
        config.setSampleSize((int num) -> num == 0 ? this.learnSampleSize : sampleSize);
        
        
        List<SampleBenign> sampler = new ArrayList<>();
        for(TreeAutomaton ita : data) {
            
            SampleBenign sb = new RuleCountBenign(samplerSmooth, seed, ita);
            sampler.add(sb);
            
            System.out.println("Initialized.");
        }
        
        BiFunctionParallelIterable<SampleBenign,Object,List<Tree<Rule>>> bfpi =
                new BiFunctionParallelIterable<>(sampler, allNull, this.threads, 
                        (SampleBenign sb, Object o) -> {
                            return sb.getSample(config);
                        });
        
        for(int i=0;i<trainIterations;++i) {
            for(List<Tree<Rule>> samples : bfpi) {
                
                for(int j=0;j<samples.size();++j) {
                    Tree<Rule> sample = samples.get(j);
                    
                    config.getTarget().add(sample, learnSize);
                }
            }
            
            System.out.println("Finished training round: "+(i+1));
        }
        
        Iterator<TreeAutomaton> tas = data.iterator();
        List<Tree<String>> fin = new ArrayList<>();
        for(List<Tree<Rule>> sample : bfpi) {
            Signature sig = tas.next().getSignature();
            
            Function<Rule,String> func = (Rule rul) -> sig.resolveSymbolId(rul.getLabel());
            
            for(int i=0;i<sample.size();++i) {
                fin.add(sample.get(i).map(func));
            }
        }
        
        return fin;
    }

    /**
     * 
     * @param samplerSmooth 
     */
    public void setSamplerSmooth(double samplerSmooth) {
        this.samplerSmooth = samplerSmooth;
    }

    /**
     * 
     * @param sampleSize 
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    /**
     * 
     * @param learnSampleSize 
     */
    public void setLearnSampleSize(int learnSampleSize) {
        this.learnSampleSize = learnSampleSize;
    }

    /**
     * 
     * @param learnSize 
     */
    public void setLearnSize(double learnSize) {
        this.learnSize = learnSize;
    }
    
    /**
     * 
     * @param adaptionRounds 
     */
    public void setAdaptionRounds(int adaptionRounds) {
        this.adaptionRounds = adaptionRounds;
    }
    /**
     * 
     * @param trainIterations 
     */
    public void setTrainIterations(int trainIterations) {
        this.trainIterations = trainIterations;
    }
    /**
     * 
     * @param threads 
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }
}
