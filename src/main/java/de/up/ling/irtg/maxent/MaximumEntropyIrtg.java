package de.up.ling.irtg.maxent;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {
    private static final Logger log = Logger.getLogger(MaximumEntropyIrtg.class.getName());
    private static final double INITIAL_WEIGHT = 0.5; // initial value for a feature's weight 
    private double[] weights;                       // weights for feature functions
    private FeatureFunction[] features;             // list of feature functions
    private List<String> featureNames;              // list of names of feature functions
    private Map<Integer, double[]> f;                // f[r][i] = value of i-th ft function on rule with label r

    /**
     * Constructor
     *
     * @param automaton the TreeAutomaton build by grammar rules
     * @param featureMap the map contains feature functions accessed by their
     * names. These functions are used to calculate probabilities for the RTG
     */
    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, final Map<String, FeatureFunction> featureMap) {
        super(automaton);

        f = new HashMap<Integer, double[]>();

        // store the features
        setFeatures(featureMap);
    }

    /**
     * Sets the feature functions
     *
     * @param featureMap the mapping of names to feature functions
     */
    public final void setFeatures(final Map<String, FeatureFunction> featureMap) {
        if ((featureMap == null) || (featureMap.isEmpty())) {
            featureNames = null;
            features = null;
            weights = null;
        } else {
            // the names of the feature functions are the keys of featureMap
            featureNames = new ArrayList<String>();
            featureNames.addAll(featureMap.keySet());

            // the size of the names list is used to initialize the arrays for the feature functions
            // and their corresponding weights
            features = new FeatureFunction[featureNames.size()];
            weights = new double[featureNames.size()];

            // fill the array for the feature functions using the parameter featureMap
            // and the weights array using a default value
            for (int i = 0; i < featureNames.size(); i++) {
                features[i] = featureMap.get(featureNames.get(i));
                weights[i] = INITIAL_WEIGHT;
            }
        }
    }

    /**
     * Sets the array of the feature function weights
     *
     * @param weights the array of feature weights
     */
    public void setFeatureWeights(double[] weights) {
        this.weights = weights;
    }

    /**
     * Sets the weight of a specific feature function
     *
     * @param index the position of the weight in the array
     * @param weight the new weight
     * @throws NoFeaturesException if no features are present
     */
    public void setFeatureWeight(int index, double weight) {
        weights[index] = weight;
    }


    /**
     * Returns the weight of a specific feature function referenced by
     * <tt>i</tt>
     *
     * @param i the reference of a feature function
     * @return double the weight of the feature function
     * @throws NoFeaturesException if no features are present
     */
    public double getFeatureWeight(int i) {
        if (weights != null && i < weights.length) {
            return weights[i];
        } else {
            return Double.NaN;
        }
    }

    /**
     * Returns the array of the feature function weights
     *
     * @return double[] containing the weights of all feature functions
     * @throws NoFeaturesException if no features are present
     */
    public double[] getFeatureWeights() {
        return weights;
    }

    /**
     * Returns the list of the feature function names
     *
     * @return List<String>() containing the names of all feature functions
     */
    public List<String> getFeatureNames() {
        return featureNames;
    }

    /**
     * Returns the feature function referenced by name
     *
     * @param name the name of the feature function
     * @return the feature function with the name <tt>name</tt> if no
     * corresponding function is found
     * @throws NoFeaturesException if no features are present
     */
    public FeatureFunction getFeatureFunction(String name) {
        int index = featureNames.indexOf(name);
        return getFeatureFunction(index);
    }

    /**
     * Returns the feature function referenced by index
     *
     * @param index the index of the feature function
     * @return the feature function with the <tt>index</tt>
     * @throws NoFeaturesException if no features are present
     */
    public FeatureFunction getFeatureFunction(int index) {
        if (getFeatures() != null && index < getFeatures().length) {
            return getFeatures()[index];
        } else {
            return null;
        }
    }

    /**
     * Returns the number of features
     *
     * @return number of features
     */
    public int getNumFeatures() {
        return (featureNames == null) ? 0 : featureNames.size();
    }

    /**
     * Calculate every feature's value for a tree
     *
     * @param tree the tree to compute the values for
     * @param fiY the array of feature values
     */
    private void getFiFor(Tree<Rule> tree, TreeAutomaton chart, double[] fiY) {
        double[] fi = getOrComputeFeatureValues(tree.getLabel(), chart);

        // for every feature calculate the value for the root of the tree
        // and add it to the result
        for (int i = 0; i < fi.length; i++) {
            // add f_i for this rule
            fiY[i] += fi[i];
        }

        // add the values of every child to the result
        for (Tree<Rule> child : tree.getChildren() ) {
            getFiFor(child, chart, fiY);
        }
    }

    /**
     * Parses an input of representations and their name and computes a chart
     * for this input The member variable <tt>useIrtgParser</tt> indicates which
     * parser to use True: the parser of InterpretedTreeAutomaton will be used
     * False: an implementation of a CKY-parser will be used
     *
     * @param inputs mapping of representations and their names
     * @return TreeAutomaton the computed chart
     */
    @Override
    public TreeAutomaton parseInputObjects(Map<String, Object> inputs) {
        TreeAutomaton ret = super.parseInputObjects(inputs);
        setWeightsOnChart(ret);
        return ret;
    }
    
    public void computeFeatureValues(Rule rule, TreeAutomaton auto) {
        double[] values = new double[getNumFeatures()];
        
        for( int i = 0; i < getNumFeatures(); i++ ) {
            values[i] = (double) getFeatures()[i].evaluate(rule, auto, this);
        }
        
        rule.setExtra(values);
    }
    
    public double[] getOrComputeFeatureValues(Rule rule, TreeAutomaton auto) {
        if( rule.getExtra() == null ) {
            computeFeatureValues(rule, auto);
        }
        
        return (double[]) rule.getExtra();
    }
    
    public double getRuleScore(Rule rule, TreeAutomaton auto) {
        double sum = 0.0;
        double[] featureValues = getOrComputeFeatureValues(rule, auto);
        
        for( int i = 0; i < getNumFeatures(); i++ ) {
            sum += featureValues[i] * weights[i];
        }
        
        return sum;
    }

    /**
     * Computes feature values for every rule of a chart if necessary and uses
     * them to set weights for the rules
     *
     * @param chart the automaton to set the rule weights for
     */
    private void setWeightsOnChart(TreeAutomaton chart) {
        Iterable<Rule> ruleSet = chart.getRuleSet();

        if (getFeatures() != null) {
            for (Rule rule : ruleSet) {
                rule.setWeight(Math.exp(getRuleScore(rule, chart)));
            }
        }
    }
    
    public boolean trainMaxent(final Corpus corpus) {
        return trainMaxent(corpus, null);
    }
    
    /**
     * Trains the weights for the rules according to the training data.
     *
     * @param corpus the training data containing sentences and their parse tree
     * @return true iff L-BFGS optimization was successful
     */
    public boolean trainMaxent(final Corpus corpus, ProgressListener listener) {
        // create the optimzer with own optimizable class
        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(new MaxEntIrtgOptimizable(corpus, listener));

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
        
        return bfgs.isConverged();
    }

    /**
     * Reads the feature function weights from a reader, e.g., string or file
     * The data must be formatted as Java properties. It's assumed that the
     * feature functions are already set. Setting the feature functions will
     * create a new array of weights.
     *
     * @param reader the reader to read the data from
     * @throws IOException if the reader cannot read the data properly
     */
    public void readWeights(Reader reader) throws IOException {
        Properties props = new Properties();

        // the properties class takes care of the actual reading
        props.load(reader);

        // convert the read string values to double and fill the array
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = String.valueOf(entry.getKey());
            double weight = Double.valueOf(String.valueOf(entry.getValue()));

            // get the array's index for the weight by look-up the feature's name
            int index = featureNames.indexOf(key);

            // no need to check the upper bound, because both collections have the same size
            if (index >= 0) {
                weights[index] = weight;
            }

        }

    }

    /**
     * Writes the feature function weights to a writer, e.g., string or file The
     * data will be formatted as Java properties
     *
     * @param writer the writer to store the data into
     * @throws IOException if the writer cannot store the data properly
     */
    public void writeWeights(Writer writer) throws IOException {
        Properties props = new Properties();

        if (weights != null) {
            for (int i = 0; i < weights.length; i++) {
                props.put(featureNames.get(i), String.valueOf(weights[i]));
            }
        }

        props.store(writer, null);
    }

    /**
     * Returns a string representing the object and its elements
     *
     * @return String the string representing the object
     */
    @Override
    public String toString() {
        StringWriter ret = new StringWriter();
        // use the super class for basic string representation
        ret.append(super.toString());

        if (featureNames != null) {
            // add the features to the string
            for (int i = 0; i < featureNames.size(); i++) {
                ret.append("feature ");
                ret.append(featureNames.get(i));
                ret.append(": ");
                ret.append(getFeatures()[i].toString());
                ret.append("\n");
            }
        }

        return ret.toString();
    }

    /**
     * @return the features
     */
    public FeatureFunction[] getFeatures() {
        return features;
    }
    
    
    /**
     * Internal class for training the feature function weights. We use the
     * mallet framework for training so this class implements a mallet interface
     */
    private class MaxEntIrtgOptimizable implements Optimizable.ByGradientValue {
        private boolean cachedStale = true;
        private double cachedValue;
        private double[] cachedGradient;
        private Corpus trainingData;
        private int iteration = 0;
        private ProgressListener listener;

        /**
         * Constructor
         *
         * @param corpus the annotated training data
         * @param interp the training data may contain multiple interpretations.
         * This parameter tells us which one to use
         */
        public MaxEntIrtgOptimizable(final Corpus corpus, final ProgressListener listener) {
            cachedStale = true;
            trainingData = corpus;
            cachedGradient = new double[getNumFeatures()];
            this.listener = listener;
        }

        /**
         * Primarily this function returns the computed log-likelihood for the
         * optimization. Beyond that it computes the also needed gradient.
         */
        @Override
        public double getValue() {
            /**
             * log-likelihood: L(Lambda) =
             * sum_x,y(p~(x,y)*sum_i(lambda_i*f_i(x,y)) -
             * sum_x(p~(x)*log(sum_y(e^(sum_i(lambda_i*f_i(x,y)))))) sum_x,y :
             * sum over every instance of training data p~(x,y) : 1/N
             * sum_i(lambda_i*f_i(x,y)) : log(chart.getWeights(y))
             *
             * sum_x : sum over every instance of training data p~(x) : 1/N
             * sum_y(e^(sum_i(lambda_i*f_i(x,y)))) : inside(S)
             *
             * gradient (<f~i> - <fi>): g_i = sum_x,y(p~(x,y)*f_i(x,y)) -
             * sum_x,y(p~(x)*p_lambda(y|x)*f_i(x,y)) sum_x,y : in both cases sum
             * over every instance of training data f_i(x,y) : sum over all
             * f_i(r) with Rule r used in a node of the tree
             * p_lambda(y|x)*f_i(x,y) : E(f_i|S) (Chiang, 04) E(f_i|S) =
             * sum_r(f_i(r)*E(r)) sum_r : sum over all rules of the parse chart
             * E(r) = outside(A)*p(r)*inside(B)*inside(C) / inside(S) | for r(A
             * -> B C) p(r) : r.getWeight()
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
                int instanceNum = 0;

                for (Instance instance : trainingData ) {
                    TreeAutomaton chart = parseInputObjects(instance.getInputObjects());
                    // TODO - once chart caching works again, use cached chart here
                    
                    // if the chart could not be computed track it and continue with the next instance
                    if (chart == null) {
                        faultyCharts++;
                        continue;
                    }

                    // compute inside & outside for the states of the parse chart
                    Map<Integer, Double> inside = chart.inside();
                    Map<Integer, Double> outside = chart.outside(inside);
                    double insideS = 0.0;
                    
                    // compute inside(S) : the inside value of the starting states
                    Set<Integer> finalStates = chart.getFinalStates();
                    for (Integer start : finalStates) {
                        insideS += inside.get(start);
                    }

                    // compute parts of the log-likelihood
                    // L(Lambda) = sum1/n - sum2/n
                    sum1 += Math.log(chart.getWeightRaw(instance.getDerivationTree()));
                    sum2 += Math.log(insideS);

                    // compute parts of the gradient
                    Iterable<Rule> ruleSet = chart.getRuleSet();
                    for (Rule r : ruleSet) {
                        double expect_r; // E(r)
                        Double outVal = outside.get(r.getParent());
                        if (outVal != null) {
                            double insideOutside = outVal * r.getWeight(); // outside(A)*p(r)

                            for (Integer state : r.getChildren()) {
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
                        
                        double[] fi = getOrComputeFeatureValues(r, chart);
                        for (int i = 0; i < fi.length; i++) {
                            expectation[i] += fi[i] * expect_r; // (...)*f_i(r)
                        }
                    }
                    
                    try {
                        // compute f_i(x,y)
                       getFiFor(chart.getRuleTree(instance.getDerivationTree()), chart, fiY);
                    } catch (Exception ex) {
                        throw new RuntimeException("Could not reconstruct rule tree from derivation tree for instance " + instance.toString(getAutomaton()), ex);
                    }

                    if( listener != null ) {
                        listener.accept(instanceNum++, n, null);                        
//                        listener.update(iteration, instanceNum++);
                    }
                }

                // L(Lambda) = sum1/n - sum2/n
                cachedValue = (sum1 - sum2) / n;

                for (int i = 0; i < cachedGradient.length; i++) {
                    // g_i = sum_x,y(f_i(x,y))/n - sum_x,y(E(f_i|S))/n
                    cachedGradient[i] = (fiY[i] - expectation[i]) / n;
                }

                cachedStale = false;

                if (faultyCharts > 0) {
                    log.log(Level.WARNING, "Skipped {0} instances. No suitable chart found.", faultyCharts);
                }
            }

            iteration++;
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
         * Returns the number of parameters.
         * 
         * @return int the number of feature weights
         */
        @Override
        public int getNumParameters() {
            double[] parameters = getFeatureWeights();
            return parameters.length;
        }

        /*
         * Getter for the feature weights
         * 
         * @param doubles an array of doubles where the feature weights will be stored in
         */
        @Override
        public void getParameters(double[] doubles) {
            System.arraycopy(getFeatureWeights(), 0, doubles, 0, getNumParameters());
        }

        /*
         * Getter for a specific feature weights
         * 
         * @param i the index of the feature weight
         * @return double the feature weight at <tt>i</tt>
         */
        @Override
        public double getParameter(final int i) {
            return getFeatureWeight(i);
        }

        /*
         * Setter for the feature weights
         * 
         * @param doubles an array of doubles containing the feature weights
         */
        @Override
        public void setParameters(double[] doubles) {
            setFeatureWeights(doubles);
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
            setFeatureWeight(i, d);
            cachedStale = true;
        }
    }

    
    public static void setLoggingLevel(Level level) {
        log.setLevel(level);
    }
}
