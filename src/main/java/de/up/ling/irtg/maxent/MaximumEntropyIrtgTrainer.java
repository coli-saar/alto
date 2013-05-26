
package de.up.ling.irtg.maxent;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg.NoFeaturesException;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg.NoRepresentationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Danilo Baumgarten
 */
public class MaximumEntropyIrtgTrainer {

    private static final Logger log = Logger.getLogger( MaximumEntropyIrtgTrainer.class.getName() );
    private static final boolean READ_CHARTS = false;
    private static final boolean WRITE_CHARTS = false;

    private MaximumEntropyIrtg maxEntIrtg;

    /**
     * Reads a grammar and a corpus and tries to optimize the feature weights
     * Optionally reads and/or writes the charts of corpus entries
     * from/to file to save time
     * 
     * @param args an array of string containing the optional arguments from the call
     * @throws IOException if an error occurs on accessing the files
     * @throws ParseException if parsing the grammar fails
     */
    public static void main(final String[] args) throws IOException, ParseException {
        String prefix = (args.length > 0) ? args[0] : "ptb-test";
        boolean readCharts = (args.length > 1) ? (args[1].equals("readcharts")) : READ_CHARTS;
        boolean writeCharts = (args.length > 2) ? (args[2].equals("writecharts")) : WRITE_CHARTS;
        List<TreeAutomaton> cachedCharts = new ArrayList<TreeAutomaton>();

        log.log(Level.INFO, "Reading grammar...");
        MaximumEntropyIrtg maxEntIrtg = (MaximumEntropyIrtg) IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
        
        // init trainer - the first bool is for using the parser of InterpretedTreeAutomaton
        // the second one is for pre-computing all f_i
        MaximumEntropyIrtgTrainer trainer;
        try {
            trainer = new MaximumEntropyIrtgTrainer(maxEntIrtg, false, false);
        } catch (NoRepresentationException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        } catch (NoFeaturesException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        }
        
        log.info("Reading corpus...");
        AnnotatedCorpus anCo = maxEntIrtg.readAnnotatedCorpus(new FileReader(prefix + "-corpus-training.txt"));
        
        if (readCharts) {
            log.info("Reading charts...");
            readCharts(cachedCharts, new FileInputStream(prefix + "-training.charts"));
        }
        
        trainer.train(anCo, cachedCharts);

        log.info("Writing feature weights...");
        try {
            maxEntIrtg.writeWeights(new FileWriter(prefix + "-weights.props"));
        } catch (NoFeaturesException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        }
        
        if (writeCharts) {
            log.info("Writing charts...");
            writeCharts(cachedCharts, new FileOutputStream(prefix + "-training.charts"));
        }
    }
    
    /**
     * Constructor
     * Calls prepare() one the given MaximumEntropyIrtg with passed-on parameters
     * 
     * @param maxEntIrtg the MaximumEntropyIrtg to train
     * @param useIrtgParser a flag whether to use the parser of
     * InterpretedTreeAutomaton or not
     * @param precomputeFI a flag whether to pre-compute all feature values
     * @throws NoRepresentationException if the MaximumEntropyIrtg doesn't contain
     * a suitable interpretation to produce training charts
     * @throws NoFeaturesException if the MaximumEntropyIrtg doesn't contain features
     */
    public MaximumEntropyIrtgTrainer(final MaximumEntropyIrtg maxEntIrtg, final boolean useIrtgParser, final boolean precomputeFI)
            throws NoRepresentationException, NoFeaturesException
    {
        this.maxEntIrtg = maxEntIrtg;

        // start pre-computing (if wanted) and the autodetection of appropriate algebras
        maxEntIrtg.prepare(useIrtgParser, precomputeFI);
    }
    
    public void train(AnnotatedCorpus corpus) {
        train(corpus, null);
    }
    
    /**
     * Trains the weights for the rules according to the training data
     *
     * @param corpus the training data containing sentences and their parse tree
     */
    public void train(final AnnotatedCorpus corpus, List<TreeAutomaton> cachedCharts) {
        // create the optimzer with own optimizable class
        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(new MaxEntIrtgOptimizable(corpus, cachedCharts));
        
        // start optimization
        try {
            bfgs.optimize();
        } catch (cc.mallet.optimize.OptimizationException e) {
            // getting here doesn't neccessarily mean there was something from
            // so we just log the exception and go on
            log.log(Level.WARNING, e.toString());
        }
        
        // check if the optimization was successful
        if (bfgs.isConverged()) {
            log.info("Optimization was successful.");
        } else {
            log.info("Optimization was unsuccessful.");
        }
    }
    
    /**
     * Internal class for training the feature function weights. We use the mallet framework
     * for training so this class implements a mallet interface
     */
    private class MaxEntIrtgOptimizable implements Optimizable.ByGradientValue {
        private boolean cachedStale = true;
        private double cachedValue;
        private double[] cachedGradient;
        private AnnotatedCorpus trainingData;
        private List<TreeAutomaton> cachedCharts;

        /**
         * Constructor
         *
         * @param corpus the annotated training data
         * @param interp the training data may contain multiple interpretations.
         * This parameter tells us which one to use
         */
        public MaxEntIrtgOptimizable(final AnnotatedCorpus corpus, List<TreeAutomaton> cachedCharts) {
            cachedStale = true;
            trainingData = corpus;
            cachedGradient = new double[maxEntIrtg.getNumFeatures()];
            this.cachedCharts = cachedCharts;
        }

        /**
         * Primarily this function returns the computed log-likelihood for
         * the optimization. Beyond that it computes the also needed gradient.
         */
        @Override
        public double getValue() {
            /**
             * log-likelihood:
             * L(Lambda) = sum_x,y(p~(x,y)*sum_i(lambda_i*f_i(x,y)) - sum_x(p~(x)*log(sum_y(e^(sum_i(lambda_i*f_i(x,y))))))
             * sum_x,y : sum over every instance of training data
             * p~(x,y) : 1/N
             * sum_i(lambda_i*f_i(x,y)) : log(chart.getWeights(y))
             * 
             * sum_x : sum over every instance of training data
             * p~(x) : 1/N
             * sum_y(e^(sum_i(lambda_i*f_i(x,y)))) : inside(S)
             * 
             * gradient (<f~i> - <fi>):
             * g_i = sum_x,y(p~(x,y)*f_i(x,y)) - sum_x,y(p~(x)*p_lambda(y|x)*f_i(x,y))
             * sum_x,y : in both cases sum over every instance of training data
             * f_i(x,y) : sum over all f_i(r) with Rule r used in a node of the tree
             * p_lambda(y|x)*f_i(x,y) : E(f_i|S) (Chiang, 04)
             * E(f_i|S) = sum_r(f_i(r)*E(r))
             * sum_r : sum over all rules of the parse chart
             * E(r) = outside(A)*p(r)*inside(B)*inside(C) / inside(S) | for r(A -> B C)
             * p(r) : r.getWeight()
             */
            if (cachedStale) {
                // recompute
//                log.info("(Re)compute log-likelihood and gradient...");
                int n = trainingData.getNumberOfInstances();
                double sum1 = 0.0; // sum_x,y(sum_i(lambda_i*f_i(x,y))
                double sum2 = 0.0; // sum_x(log(sum_y(e^(sum_i(lambda_i*f_i(x,y))))))
                double[] fiY = new double[cachedGradient.length]; // sum_x,y(f_i(x,y))
                double[] expectation = new double[cachedGradient.length]; // sum_x,y(E(f_i|S))
                int faultyCharts = 0;
                int j = 0;
                
                for( AnnotatedCorpus.Instance instance : trainingData.getInstances() ) {
                    TreeAutomaton chart = null;
                    
                    // get the chart for the instance; use cache if possible or compute one
                    if (cachedCharts != null && j < cachedCharts.size() ) {
                        chart = cachedCharts.get(j);
                    } else {
                        chart = maxEntIrtg.parse(instance.getInputObjects(), true);
                    }
                    // if the chart could not be computed track it and continue with the next instance
                    if (chart == null) {
                        faultyCharts++;
                        continue;
                    }

                    // compute inside & outside for the states of the parse chart
//                    log.log(Level.INFO, "Compute inside & outside");
                    Map<Object, Double> inside = chart.inside();
                    Map<Object, Double> outside = chart.outside(inside);
                    double insideS = 0.0;
                    // compute inside(S) : the inside value of the starting states
                    Set finalStates = chart.getFinalStates();
                    for (Object start : finalStates) {
                        insideS += inside.get(start);
                    }

                    // compute parts of the log-likelihood
                    // L(Lambda) = sum1/n - sum2/n
                    sum1 += Math.log(chart.getWeight(instance.getTree()));
                    sum2 += Math.log(insideS);

                    //compute parts of the gradient
//                    log.info("Compute expectations for gradient...");
                    Set<Rule> ruleSet = (Set<Rule>) chart.getRuleSet();
                    for (Rule r : ruleSet) {
                        double expect_r; // E(r)
                        Double outVal = outside.get(r.getParent());
                        if (outVal != null) {
                            double insideOutside = outVal * r.getWeight(); // outside(A)*p(r)

                            for (Object state : r.getChildren()) {
                                Double inVal = inside.get(state);
                                if (inVal != null) {
                                    insideOutside *= inVal; // (...)*inside(B)*inside(C)
                                } else {
                                    insideOutside = 0.0;
                                }
                            }

                            expect_r = insideOutside / insideS; // (...) / inside(S)
                        } else {
                            expect_r = 0.0;
                        }
                        double[] fi = maxEntIrtg.getFeatureValue(r.getLabel());
                        for (int i = 0; i < fi.length; i++) {
                            expectation[i] += fi[i] * expect_r; // (...)*f_i(r)
                        }
                    }
                    
                    // compute f_i(x,y)
//                    log.info("Compute f_i for the tree of the training instance...");
                    maxEntIrtg.getFiFor(instance.getTree(), fiY);
                    
                    j++;
                }

                // L(Lambda) = sum1/n - sum2/n
                cachedValue = (sum1 - sum2) / n;

                for (int i = 0; i < cachedGradient.length; i++) {
                    // g_i = sum_x,y(f_i(x,y))/n - sum_x,y(E(f_i|S))/n
                    cachedGradient[i] = (fiY[i] - expectation[i]) / n;
                }

                cachedStale = false;
//                log.log(Level.INFO, "log-likelihood: {0}", cachedValue);
                
                if (faultyCharts > 0) {
                    log.log(Level.WARNING, "Skipped {0} instances. No suitable chart found.", faultyCharts);
                }
            }

            return cachedValue;
        }

        /*
         * Getter for cachedGradient
         * 
         * @param gradient an array of doubles where the gradient will be stored in
         */
        @Override
        public void getValueGradient(double[] gradient) {
            // we compute the gradient together with the value
            if (cachedStale) {
                getValue();
            }
            assert (gradient != null && gradient.length == cachedGradient.length);
            System.arraycopy(cachedGradient, 0, gradient, 0, cachedGradient.length);
        }

        /*
         * Simba the number of feature weights
         * 
         * @return int the number of feature weights
         */
        @Override
        public int getNumParameters() {
            double[] parameters;
            try {
                parameters = maxEntIrtg.getFeatureWeights();
            } catch (NoFeaturesException ex) {
                log.log(Level.SEVERE, null, ex);
                return 0;
            }
            return parameters.length;
        }

        /*
         * Getter for the feature weights
         * 
         * @param doubles an array of doubles where the feature weights will be stored in
         */
        @Override
        public void getParameters(double[] doubles) {
            try {
                System.arraycopy(maxEntIrtg.getFeatureWeights(), 0, doubles, 0, getNumParameters());
            } catch (NoFeaturesException ex) {
                throw new RuntimeException(ex);
            }
        }

        /*
         * Getter for a specific feature weights
         * 
         * @param i the index of the feature weight
         * @return double the feature weight at <tt>i</tt>
         */
        @Override
        public double getParameter(final int i) {
            try {
                return maxEntIrtg.getFeatureWeight(i);
            } catch (NoFeaturesException ex) {
                throw new RuntimeException(ex);
            }
        }

        /*
         * Setter for the feature weights
         * 
         * @param doubles an array of doubles containing the feature weights
         */
        @Override
        public void setParameters(double[] doubles) {
            maxEntIrtg.setFeatureWeights(doubles);
            cachedStale = true;
        }

        /*
         * Setter for a specific feature weights
         * 
         * @param i the index where to store the feature weight
         * @param d the new feature weight
         */
        @Override
        public void setParameter(final int i, final double d) {
            try {
                maxEntIrtg.setFeatureWeight(i, d);
            } catch (NoFeaturesException ex) {
                throw new ArrayIndexOutOfBoundsException(ex.toString());
            }
            cachedStale = true;
        }
    }
    
    /**
     * Reads a chart from a stream, e.g., string or file
     *
     * @param in the input stream
     * @return TreeAutomaton the read chart
     * @throws IOException if an error occurs on reading the stream
     */
    private static TreeAutomaton readChart(ObjectInputStream in) throws IOException {
        try {
            return (TreeAutomaton) in.readObject();
        } catch (ClassNotFoundException ex) {
            System.err.println(ex);
            return null;
        } catch (EOFException ex) {
            return null;
        }
    }

    /**
     * Reads all charts from a file stream and caches them
     *
     * @param in the input stream
     * @throws IOException if an error occurs on reading the stream
     */
    public static void readCharts(List<TreeAutomaton> cachedCharts, FileInputStream fstream) throws IOException {
        ObjectInputStream in = new ObjectInputStream(fstream);

        // read the first chart
        TreeAutomaton chart = readChart(in);

        // as long as there can a chart be read cache it and try to read the next one
        while (chart != null) {
            cachedCharts.add(chart);
            chart = readChart(in);
        }

        // finish stream handles
        in.close();
        fstream.close();
    }

    /**
     * Writes the cached charts into a file stream
     *
     * @param fstream the output stream
     * @throws IOException if an error occurs on writing into the stream
     */
    public static void writeCharts(List<TreeAutomaton> cachedCharts, FileOutputStream fstream) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(fstream);

        // write every chart into the stream
        for (TreeAutomaton chart : cachedCharts) {
            out.writeObject(chart);
        }

        // finish stream handles
        out.close();
        fstream.close();
    }
}
