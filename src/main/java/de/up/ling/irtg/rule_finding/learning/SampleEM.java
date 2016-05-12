/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import com.google.common.base.Function;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.rule_finding.sampling.RuleCountBenign;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign;
import de.up.ling.irtg.rule_finding.sampling.SampleBenign.Configuration;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph
 */
public class SampleEM implements TreeExtractor {
    
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
    private double learnSize = 10;

    /**
     * 
     */
    private int adaptionRounds = 20;
    
    /**
     * 
     */
    private int trainIterations = 5;
    
    /**
     * 
     */
    private int threads;
    
    /**
     * 
     */
    private boolean resetEveryRound = true;
    
    /**
     * 
     */
    private Model model;
    
    /**
     * 
     * @param m
     */
    public SampleEM(Model m) {
        this.model = m;
        this.threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }
    
    /**
     * 
     * @param data
     * @param mod
     * @param seed
     * @return 
     */
    public Iterable<Iterable<Tree<String>>> getChoices(
            Iterable<InterpretedTreeAutomaton> data, Model mod, long seed) {
        Well44497b seeder = new Well44497b(seed);
        
        // we need this because we use the paired, parallel iterator
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
        
        // now we need to package all the parameters we need for sampling
        Configuration config = new Configuration(mod);
        config.setRounds(this.adaptionRounds);
        config.setSampleSize((int num) -> num == 0 ? this.learnSampleSize : sampleSize);
        
        // and we create a list of samplers that will explore the automata
        // and another list with the results so we can add them to the model
        List<SampleBenign> sampler = new ArrayList<>();
        List<List<Tree<Rule>>> next = new ArrayList<>();
        
        
        for(InterpretedTreeAutomaton ita : data) {
            SampleBenign sb = new RuleCountBenign(samplerSmooth, seed, ita);
            sampler.add(sb);
            
            sb.setResetEverySample(this.resetEveryRound);
        }
        
        System.out.println("Initialized.");
        
        FunctionIterable<List<Tree<Rule>> ,SampleBenign> bfpi =
                new FunctionIterable<>(sampler, 
                        (SampleBenign sb) -> {
                            return sb.getSample(config);
                        });
        
        // now we iterate over the training data for a number of iterations
        for(int i=0;i<trainIterations;++i) {
            next.clear();
            
            // used to point at the current lsit of old samples
            int counter = 0;
            
            for(List<Tree<Rule>> sampleDrawn : bfpi) {
                next.add(sampleDrawn);
                
                // allow the user to see the current progress
                if((counter+1) % 10 == 0) {
                    System.out.println("finished "+(counter+1)+" examples.");
                }
                ++counter;
            }
            
            config.getTarget().clear();
            
            // we need to get the irtg from which the trees are drawn in order
            // to interprete them
            Iterator<InterpretedTreeAutomaton> ita = data.iterator();
            for(int k=0;k<next.size();++k) {
                List<Tree<Rule>> inner = next.get(k);
                InterpretedTreeAutomaton aut = ita.next();
                
                for(int j=0;j<inner.size();++j) {
                    config.getTarget().add(inner.get(j), aut, learnSize);
                }
            }
            
            System.out.println("Finished training round: "+(i+1));
        }
        
        // now generate a final sample from the current estimate
        Iterator<InterpretedTreeAutomaton> tas = data.iterator();
        List<Iterable<Tree<String>>> fin = new ArrayList<>();
        for(List<Tree<Rule>> sample : bfpi) {
            Signature sig = tas.next().getAutomaton().getSignature();
            
            Function<Rule,String> func = (Rule rul) -> sig.resolveSymbolId(rul.getLabel());
            List<Tree<String>> inner = new ArrayList<>();
            
            for(int i=0;i<sample.size();++i) {
                inner.add(sample.get(i).map(func));
            }
            
            fin.add(inner);
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

    /**
     * 
     * @param resetEveryRound 
     */
    public void setResetEveryRound(boolean resetEveryRound) {
        this.resetEveryRound = resetEveryRound;
    }
    
    /**
     * 
     * @return 
     */
    public Model getModel() {
        return model;
    }

    /**
     * 
     * @param model 
     */
    public void setModel(Model model) {
        this.model = model;
    }

    @Override
    public Iterable<Iterable<Tree<String>>> getAnalyses(Iterable<InterpretedTreeAutomaton> it) {
        return this.getChoices(it, model, new Date().getTime());
    }
}
