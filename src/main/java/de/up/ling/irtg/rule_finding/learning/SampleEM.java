/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import com.google.common.base.Function;
import de.saar.basic.Pair;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.rule_weighters.SubtreeCounting;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.random.Well44497b;

/**
 *
 * @author christoph
 */
public class SampleEM implements TreeExtractor {

    /**
     *
     */
    private int sampleSize = 5000;

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
     *
     */
    private int resultSize = 50;

    /**
     *
     */
    private int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

    /**
     *
     */
    private boolean reset = false;

    /**
     *
     */
    private double lexiconAdditionFactor = 1.0;

    /**
     *
     * @param lexiconAdditionFactor
     */
    public void setLexiconAdditionFactor(double lexiconAdditionFactor) {
        this.lexiconAdditionFactor = lexiconAdditionFactor;
    }

    /**
     *
     * @param reset
     */
    public void setReset(boolean reset) {
        this.reset = reset;
    }

    /**
     *
     * @param threads
     */
    public void setThreads(int threads) {
        this.threads = threads;
    }

    /**
     * Assumes determinism and a single signature.
     *
     *
     * @param data
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public Iterable<Iterable<Tree<String>>> getChoices(Iterable<InterpretedTreeAutomaton> data) throws InterruptedException, ExecutionException {
        Well44497b seeder = new Well44497b(seed);

        // and we create a list of samplers that will explore the automata
        List<SubtreeCounting> automataToSample = new ArrayList<>();

        Iterator<InterpretedTreeAutomaton> dIt = data.iterator();
        if (!dIt.hasNext()) {
            //Nothing to learn here.
            return new ArrayList<>();
        }
        Signature mainSig = dIt.next().getAutomaton().getSignature();
        SubtreeCounting.CentralCounter counts = new SubtreeCounting.CentralCounter(this.smooth, this.lexiconAdditionFactor, mainSig);

        List<SamplingJob> tasks = new ArrayList<>();
        for (InterpretedTreeAutomaton ita : data) {
            LearningRate lr = new LearningRate() {
                @Override
                public double getLearningRate(int group, int parameter, double gradient) {
                    return samplerLearningRate;
                }

                @Override
                public void reset() {
                }
            };

            if (ita.getAutomaton().getSignature() != mainSig) {
                throw new IllegalArgumentException("Automata do not share the same signature.");
            }

            SubtreeCounting suc = new SubtreeCounting(ita, this.normalizationExponent, this.normalizationDivisor, lr, counts);

            automataToSample.add(suc);

            AdaptiveSampler ads = new AdaptiveSampler(seeder.nextLong());
            tasks.add(new SamplingJob(ads, suc));
        }

        if (this.iterationProgress != null) {
            this.iterationProgress.accept(0, this.trainIterations, "Initialized.");
        }

        // now we iterate over the training data for a number of iterations
        ExecutorService runner = Executors.newFixedThreadPool(threads);
        List<SamplingJob> buffer = new ArrayList<>();

        for (int trainingRound = 0; trainingRound < trainIterations; ++trainingRound) {
            double negLogLikelihood = 0.0;

            counts = new SubtreeCounting.CentralCounter(smooth, this.lexiconAdditionFactor, mainSig);

            int batchNumber = 0;
            for (int entry = 0; entry < tasks.size();) {
                buffer.clear();
                for (int i = 0; i < this.threads * 2 && entry < tasks.size(); ++i) {
                    buffer.add(tasks.get(entry++));
                }

                List<Future<Pair<TreeSample<Rule>, Double>>> result = runner.invokeAll(buffer);
                for (int j = 0; j < result.size(); ++j) {
                    Future<Pair<TreeSample<Rule>, Double>> fut = result.get(j);

                    Pair<TreeSample<Rule>, Double> done = fut.get();

                    negLogLikelihood += done.getRight();
                    TreeSample<Rule> fin = done.getLeft();

                    SubtreeCounting sc = automataToSample.get(batchNumber);

                    for (int pos = 0; pos < fin.populationSize(); ++pos) {
                        counts.add(fin.getSample(pos), sc.getAutomaton().getSignature(), fin.getSelfNormalizedWeight(pos));
                    }

                    sc.setCounter(counts);
                }

                if ((batchNumber + 1) % 10 == 0 && this.iterationProgress != null) {
                    this.iterationProgress.accept(trainingRound, this.trainIterations, "finished " + (batchNumber + 1) + " examples.");
                }

                ++batchNumber;
            }

            if (this.iterationProgress != null) {
                this.iterationProgress.accept(trainingRound + 1, this.trainIterations, "Finished training round: " + (trainingRound + 1));
            }

            if (this.nLLTracking != null) {
                this.nLLTracking.accept(negLogLikelihood);
            }
        }

        // now generate a final sample from the current estimate
        if (this.iterationProgress != null) {
            this.iterationProgress.accept(0, tasks.size(), "Extracting final trees.");
        }

        runner.shutdown();

        Function<Rule, String> mapping = (Rule r) -> mainSig.resolveSymbolId(r.getLabel());
        return new FunctionIterable<>(tasks, new java.util.function.Function<SamplingJob, Iterable<Tree<String>>>() {
            private int pos = 0;
            
            @Override
            public Iterable<Tree<String>> apply(SamplingJob sj) {
                try {
                    TreeSample<Rule> ts = sj.call().getLeft();
                    List<Tree<String>> trees = new ArrayList<>();
                    ts.flatten(seeder, resultSize, true);
                    
                    for (int i = 0; i < ts.populationSize(); ++i) {
                        trees.add(ts.getSample(i).map(mapping));
                    }
                    
                    
                    if (SampleEM.this.iterationProgress != null) {
                        SampleEM.this.iterationProgress.accept(++pos, tasks.size(), "Extracting final trees.");
                    }
                    return trees;
                }catch (Exception ex) {
                    Logger.getLogger(SampleEM.class.getName()).log(Level.SEVERE, null, ex);
                    throw new IllegalStateException(ex);
                }
            }
        });
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
     * @param smooth
     */
    public void setSmooth(double smooth) {
        this.smooth = smooth;
    }

    @Override
    public Iterable<Iterable<Tree<String>>> getAnalyses(Iterable<InterpretedTreeAutomaton> it) {
        try {
            return this.getChoices(it);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(SampleEM.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     *
     * @param fin
     * @return
     */
    public static double computeNegativeLogLikelihood(TreeSample fin) {
        double val = 0.0;

        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < fin.populationSize(); ++i) {
            max = Math.max(max, fin.getLogTargetWeight(i) - fin.getLogPropWeight(i));
        }

        for (int i = 0; i < fin.populationSize(); ++i) {
            val += Math.exp(fin.getLogTargetWeight(i) - fin.getLogPropWeight(i) - max);
        }

        val = -Math.log(val / fin.populationSize()) - max;
        return val;
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
     * @param normalizationExponent
     */
    public void setNormalizationExponent(int normalizationExponent) {
        this.normalizationExponent = normalizationExponent;
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
     * @param iterationProgress
     */
    public void setIterationProgress(ProgressListener iterationProgress) {
        this.iterationProgress = iterationProgress;
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
     * @param seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     *
     * @param resultSize
     */
    public void setResultSize(int resultSize) {
        this.resultSize = resultSize;
    }

    /**
     *
     */
    public class SamplingJob implements Callable<Pair<TreeSample<Rule>, Double>> {

        /**
         *
         */
        private final AdaptiveSampler ads;

        /**
         *
         */
        private final SubtreeCounting suc;

        /**
         *
         * @param ads
         * @param suc
         */
        public SamplingJob(AdaptiveSampler ads, SubtreeCounting suc) {
            this.ads = ads;
            this.suc = suc;
        }

        /**
         *
         * @param cc
         */
        public void setCentralCounter(SubtreeCounting.CentralCounter cc) {
            this.suc.setCounter(cc);
        }

        @Override
        public Pair<TreeSample<Rule>, Double> call() throws Exception {
            TreeSample<Rule> result
                    = ads.adaSampleMinimal(adaptionRounds, sampleSize, suc, true, reset);

            double d = computeNegativeLogLikelihood(result);

            result.expoNormalize(true);
            return new Pair<>(result, d);
        }
    }
}
