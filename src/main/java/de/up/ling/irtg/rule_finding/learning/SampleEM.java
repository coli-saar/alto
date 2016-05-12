/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import com.google.common.base.Function;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighters.SubtreeCounting;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph
 */
public class SampleEM implements TreeExtractor {
    /**
     * 
     */
    private int sampleSize = 1000;

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
    private double smooth = 1.0;
    
    /**
     * 
     */
    private double samplerLearningRate = 0.1;
    
    /**
     * 
     */
    private int normalizationExponent = 2;
    
    /**
     * 
     */
    private double normalizationDivisor = 100.0;
    
    /**
     * 
     */
    private ProgressListener iterationProgress = null;
    
    /**
     * 
     */
    private Consumer<Double> nLLTracking = null;
    
    /**
     * 
     */
    private long seed = new Date().getTime();
    
    /**
     * Assumes determinism.
     * 
     * 
     * @param data
     * @return 
     */
    public Iterable<Iterable<Tree<String>>> getChoices(Iterable<InterpretedTreeAutomaton> data) {
        Well44497b seeder = new Well44497b(seed);
        
        // and we create a list of samplers that will explore the automata
        // and another list with the results so we can add them to the model
        List<SubtreeCounting> automataToSample = new ArrayList<>();
        SubtreeCounting.CentralCounter counts = new SubtreeCounting.CentralCounter(this.smooth, new FunctionIterable<>(data,(InterpretedTreeAutomaton ita) -> ita.getAutomaton().getSignature()));
        
        for(InterpretedTreeAutomaton ita : data) {
            LearningRate lr = new LearningRate() {
                @Override
                public double getLearningRate(int group, int parameter, double gradient) {
                    return samplerLearningRate;
                }

                @Override
                public void reset() {}
            };
            
            SubtreeCounting suc = new SubtreeCounting(ita, this.normalizationExponent, this.normalizationDivisor, lr, counts);
            
            automataToSample.add(suc);
        }
        
        if(this.iterationProgress != null) {
            this.iterationProgress.accept(0, this.trainIterations, "Initialized.");
        }
        
        AdaptiveSampler ads = new AdaptiveSampler(seeder.nextLong());
        
        List<TreeSample<Rule>> oldChoices = new ArrayList<>();
        // now we iterate over the training data for a number of iterations
        for(int trainingRound=0;trainingRound<trainIterations;++trainingRound) {
            double negLogLikelihood = 0.0;
            
            for(int trainingInstance=0;trainingInstance<automataToSample.size();++trainingInstance) {
                SubtreeCounting suc = automataToSample.get(trainingInstance);
                
                List<TreeSample<Rule>> list = ads.adaSample(this.adaptionRounds, this.sampleSize, suc, true);
                TreeSample fin = list.get(list.size()-1);
                
                //TODO compute loglike
                negLogLikelihood += computeNegativeLogLikelihood(fin);
                
                fin.resampleWithNormalize(seeder, this.sampleSize, true);
                
                if(oldChoices.size() > trainingInstance) {
                    TreeSample<Rule> pick = oldChoices.get(trainingInstance);
                    
                    for(int entry=0;entry<pick.populationSize();++entry) {
                        suc.add(pick.getSample(entry), -1);
                    }
                } else {
                    oldChoices.add(fin);
                }
                
                for(int entry=0;entry<fin.populationSize();++entry) {
                    suc.add(fin.getSample(entry), 1.0);
                }
                
                // allow the user to see the current progress
                if((trainingInstance+1) % 10 == 0 && this.iterationProgress != null) {
                    this.iterationProgress.accept(trainingRound, this.trainIterations, "finished "+(trainingInstance+1)+" examples.");
                }
            }
            
            if(this.iterationProgress != null) {
                this.iterationProgress.accept(trainingRound, this.trainIterations, "Finished training round: "+(trainingRound+1));
            }
            
            if(this.nLLTracking != null) {
                this.nLLTracking.accept(negLogLikelihood);
            }
        }
        
        // now generate a final sample from the current estimate
        List<Iterable<Tree<String>>> fin = new ArrayList<>();
        for(int trainingInstance=0;trainingInstance<automataToSample.size();++trainingInstance) {
            SubtreeCounting suc = automataToSample.get(trainingInstance);
            
            Signature sig = suc.getAutomaton().getSignature();
            Function<Rule,String> func = (Rule rul) -> sig.resolveSymbolId(rul.getLabel());
            List<Tree<String>> inner = new ArrayList<>();
            
            List<TreeSample<Rule>> lt = ads.adaSample(adaptionRounds, sampleSize, suc, true);
            TreeSample ts = lt.get(lt.size()-1);
            
            ts.expoNormalize(true);
            
            for(int i=0;i<ts.populationSize();++i) {
                inner.add(ts.getSample(i).map(func));
            }
            
            fin.add(inner);
        }
        
        return fin;
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
     * @return 
     */
    public double getSmooth() {
        return smooth;
    }

    /**
     * 
     * @param smooth 
     */
    public void setSmooth(double smooth) {
        this.smooth = smooth;
    }
    
    @Override
    public Iterable<Iterable<Tree<String>>> getAnalyses(Iterable<InterpretedTreeAutomaton> it) {
        return this.getChoices(it);
    }

    /**
     * 
     * @param fin
     * @return 
     */
    private double computeNegativeLogLikelihood(TreeSample fin) {
        double val = 0.0;
        
        double max = Double.NEGATIVE_INFINITY;
        for(int i=0;i<fin.populationSize();++i) {
            max = Math.max(max, fin.getLogTargetWeight(i)-fin.getLogPropWeight(i));
        }
        
        for(int i=0;i<fin.populationSize();++i) {
            val += Math.exp(fin.getLogTargetWeight(i)-fin.getLogPropWeight(i)-max);
        }
        
        val = Math.log(val)+max;
        return val;
    }

    /**
     * 
     * @return 
     */
    public double getSamplerLearningRate() {
        return samplerLearningRate;
    }

    /**
     * 
     * @param samplerLearningRate 
     */
    public void setSamplerLearningRate(double samplerLearningRate) {
        this.samplerLearningRate = samplerLearningRate;
    }

    /**
     * 
     * @return 
     */
    public int getNormalizationExponent() {
        return normalizationExponent;
    }

    /**
     * 
     * @param normalizationExponent 
     */
    public void setNormalizationExponent(int normalizationExponent) {
        this.normalizationExponent = normalizationExponent;
    }

    /**
     * 
     * @return 
     */
    public double getNormalizationDivisor() {
        return normalizationDivisor;
    }

    /**
     * 
     * @param normalizationDivisor 
     */
    public void setNormalizationDivisor(double normalizationDivisor) {
        this.normalizationDivisor = normalizationDivisor;
    }

    /**
     * 
     * @return 
     */
    public ProgressListener getIterationProgress() {
        return iterationProgress;
    }

    /**
     * 
     * @param iterationProgress 
     */
    public void setIterationProgress(ProgressListener iterationProgress) {
        this.iterationProgress = iterationProgress;
    }

    /**
     * 
     * @return 
     */
    public Consumer<Double> getnLLTracking() {
        return nLLTracking;
    }

    /**
     * 
     * @param nLLTracking 
     */
    public void setnLLTracking(Consumer<Double> nLLTracking) {
        this.nLLTracking = nLLTracking;
    }

    /**
     * 
     * @return 
     */
    public long getSeed() {
        return seed;
    }

    /**
     * 
     * @param seed 
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }
}
