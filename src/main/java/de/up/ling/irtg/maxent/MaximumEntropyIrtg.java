package de.up.ling.irtg.maxent;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.ChartBuilder;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
    
    @Deprecated
    private boolean useIrtgParser;
    
    @Deprecated
    private boolean precomputeFI;
    
    @Deprecated
    private String representationInterpName = "";
    
    @Deprecated
    private String treeInterpName = "";
    
    
    private double[] weights;                       // weights for feature functions
    private FeatureFunction[] features;             // list of feature functions
    private List<String> featureNames;              // list of names of feature functions
    private Map<String, double[]> f;                // f[r][i] = value of i-th ft function on rule with label r

    /**
     * Constructor
     *
     * @param automaton the TreeAutomaton build by grammar rules
     * @param featureMap the map contains feature functions accessed by their
     * names. These functions are used to calculate probabilities for the RTG
     */
    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, final Map<String, FeatureFunction> featureMap) {
        super(automaton);

        useIrtgParser = false;
        precomputeFI = false;

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
    public void setFeatureWeight(int index, double weight) throws NoFeaturesException {
        if (featureNames == null) {
            throw new NoFeaturesException("No features present");
        }

        weights[index] = weight;
    }

    /**
     * Returns the array of feature values for a rule with the given label.
     *
     * @param ruleLabel the rule label
     * @return double[] containing the values of all feature functions for this rule
     */
    public double[] getFeatureValue(String ruleLabel) {
        if (f == null) {
            throw new UnsupportedOperationException("No feature values calculated. Call precomputeFeatures() or compute a chart first.");
        }

        return f.get(ruleLabel);
    }

    /**
     * Returns the weight of a specific feature function referenced by
     * <tt>i</tt>
     *
     * @param i the reference of a feature function
     * @return double the weight of the feature function
     * @throws NoFeaturesException if no features are present
     */
    public double getFeatureWeight(int i) throws NoFeaturesException {
        if (featureNames == null) {
            throw new NoFeaturesException("No features present");
        }

        return weights[i];
    }

    /**
     * Returns the array of the feature function weights
     *
     * @return double[] containing the weights of all feature functions
     * @throws NoFeaturesException if no features are present
     */
    public double[] getFeatureWeights() throws NoFeaturesException {
        if (featureNames == null) {
            throw new NoFeaturesException("No features functions set yet.");
        }

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
    public FeatureFunction getFeatureFunction(String name) throws NoFeaturesException {
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
    public FeatureFunction getFeatureFunction(int index) throws NoFeaturesException {
        if (featureNames == null) {
            throw new NoFeaturesException("No features functions set yet.");
        }

        return features[index];
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
     * Returns the number of cached charts
     *
     * @return number of cached charts
     */
    /*
    public int getNumCachedCharts() {
        return cachedCharts.size();
    }

*/
    
    /**
     * Returns a specific cached chart
     *
     * @param index the index of the chart
     * @param setWeights flag whether to calculate the weights on the chart or
     * not
     * @return TreeAutomaton the cached chart
     */
    /*
    public TreeAutomaton getCachedChart(int index, boolean setWeights) {
        TreeAutomaton chart = cachedCharts.get(index);

        if (setWeights && (chart != null)) {
            setWeightsOnChart(chart);
        }

        return chart;
    }
    */

    /**
     * Calculate every feature's value for a tree
     *
     * @param tree the tree to compute the values for
     * @param fiY the array of feature values
     */
    public void getFiFor(Tree tree, double[] fiY) {
        double[] fi = getFeatureValue((String) tree.getLabel());

        // for every feature calculate the value for the root of the tree
        // and add it to the result
        for (int i = 0; i < fi.length; i++) {
            // add f_i for this rule
            fiY[i] += fi[i];
        }

        List<Tree> children = (List<Tree>) tree.getChildren();

        // add the values of every child to the result
        for (Tree child : children) {
            getFiFor(child, fiY);
        }
    }


    /**
     * Pre-compute f_i(r) for every known rule
     */
    private void precomputeFeatures() {
        log.info("Compute f_i for every rule...");
        f = new HashMap<String, double[]>();
        Set<Rule<String>> ruleSet = (Set<Rule<String>>) automaton.getRuleSet();
        int numOfFeatures = getNumFeatures();

        for (Rule r : ruleSet) {
            double[] fi = new double[numOfFeatures];

            // compute all feature values for rule r
            for (int i = 0; i < numOfFeatures; i++) {
                FeatureFunction ff = features[i];
                fi[i] = ff.evaluate(r);
            }

            // map the values with the rule's name
            f.put(r.getLabel(), fi);
        }
    }

    /**
     * Tries to auto-detect the interpretation names for the representation (use
     * to create the chart) and the tree (used the evaluate the result) only if
     * their values aren't already set
     */
    private void autoDetectInterpretations() throws NoRepresentationException {

        // try the detection only if one of the name is not already set
        if (representationInterpName.isEmpty() || treeInterpName.isEmpty()) {
            Set<Entry<String, Interpretation>> entrySet = this.getInterpretations().entrySet();

            // check every entry of the set of interpretations
            for (Entry<String, Interpretation> i : entrySet) {

                // for now only StringAlgebra is used for input strings to compute charts
                if (representationInterpName.isEmpty() && (i.getValue().getAlgebra() instanceof StringAlgebra)) {
                    representationInterpName = i.getKey();
                } else if (treeInterpName.isEmpty()) {
                    TreeAlgebra testAlgebra = null;

                    // try to find a TreeAlgebra
                    try {
                        testAlgebra = (TreeAlgebra) i.getValue().getAlgebra();
                    } catch (Exception e) {
                        // pass
                    }

                    if (testAlgebra != null) {
                        treeInterpName = i.getKey();
                    }
                }
            }
        }

        // representationInterpName not set means there is no suitable
        // interpretation to use for parsable input
        if (representationInterpName.isEmpty()) {
            throw new NoRepresentationException("No interpretation for StringAlgebra found...");
        }
    }

    /**
     * Prepares the MaximumEntropyIrtg for further things to do i.e. the values
     * for the features will be computed (if pre-computing is enabled) and the
     * interpretations will be auto-detected
     *
     * @param useIrtgParser Flag whether to use the parsing of
     * InterpretedTreeAutomaton or not
     * @param precomputeFI Flag whether to pre-compute the values for every
     * feature or not
     * @throws NoRepresentationException if the MaximumEntropyIrtg doesn't
     * contain a suitable interpretation to produce training charts
     * @throws NoFeaturesException if the MaximumEntropyIrtg doesn't contain
     * features
     */
    public void prepare(boolean useIrtgParser, boolean precomputeFI) throws NoRepresentationException, NoFeaturesException {
        this.useIrtgParser = useIrtgParser;
        this.precomputeFI = precomputeFI;

        if (featureNames == null) {
            throw new NoFeaturesException("No features functions set yet.");
        }

        // if set compute all feature values for every rule
        if (precomputeFI) {
            precomputeFeatures();
        }

        // guess the interpretations usable for parsing (obligatory) and evaluating (optional)
        autoDetectInterpretations();
    }

    /**
     * Parses an input of representations and their name and computes a chart
     * for this input The member variable <tt>useIrtgParser</tt> indicates which
     * parser to use True: the parser of InterpretedTreeAutomaton will be used
     * False: an implementation of a CKY-parser will be used
     *
     * @param inputs mapping of representations and their names
     * @param cacheResult flag whether to cache the resulting chart or not
     * @return TreeAutomaton the computed chart
     */
    public TreeAutomaton parse(Map<String, Object> inputs, boolean cacheResult) {
        TreeAutomaton ret = null;

        if (useIrtgParser) {
            // use parser of super class
            /* FIXME: using the complete map consisting of
             * {"ptb":"...<tree-as-string-here>..."; "i":"<the-text-here>"}
             * results in an empty chart even though we take special care of the
             * ptb-representation.
             * For now we create a new map with the string representation only.
             */
            List<String> input = (List<String>) inputs.get(representationInterpName);
            Map<String, Object> inputObjects = new HashMap<String, Object>();
            inputObjects.put(representationInterpName, input);

//            log.log(Level.INFO, "Compute chart for \"{0}\"", StringTools.join(input, " "));
            ret = parseInputObjects(inputObjects);
            setWeightsOnChart(ret);
        } else {
            // use own parser
            ret = parseInput(inputs);
        }

        return ret;
    }

    /**
     * Parses an input of representations and their name and computes a chart
     * for this input
     *
     * @param inputs mapping of representations and their names
     * @return TreeAutomaton the computed chart
     */
    private TreeAutomaton parseInput(Map<String, Object> inputs) {
        Interpretation interp = interpretations.get(representationInterpName);
        List<String> input = (List<String>) inputs.get(representationInterpName);

//        log.log(Level.INFO, "Compute chart for \"{0}\"", StringTools.join(input, " "));
        ChartBuilder chartBuilder = ChartBuilder.getInstance();
        if (!chartBuilder.isInitialized()) {
            chartBuilder.init(interp.getHomomorphism(), automaton);
        }

        // compute the chart
        TreeAutomaton ret = chartBuilder.build(input);

        // if computation was successful reduce the automaton
        // and set weights on its rules
        if (ret != null) {
            ret = ret.reduceBottomUp();
            setWeightsOnChart(ret);
        }

        return ret;
    }

    /**
     * Computes feature values for every rule of a chart if necessary and uses
     * them to set weights for the rules
     *
     * @param chart the automaton to set the rule weights for
     */
    private void setWeightsOnChart(TreeAutomaton chart) {
        if (!precomputeFI) {
            // without pre-computation a new map must be created
            f = new HashMap<String, double[]>();
        }

        Set<Rule> ruleSet = (Set<Rule>) chart.getRuleSet();
        int numOfFeatures = getNumFeatures();

        if (features != null) {

            for (Rule rule : ruleSet) {
                double weight = 0.0;
                double[] fi = f.get(rule.getLabel());

                // check if the feature values are already calculated
                boolean cachedFI = (fi != null);

                if (!cachedFI) {
                    // create the array for a new set of feature values
                    fi = new double[numOfFeatures];
                }

                for (int i = 0; i < numOfFeatures; i++) {
                    // get the feature value if unknown
                    if (!cachedFI) {
                        FeatureFunction ff = features[i];
                        fi[i] = ff.evaluate(rule);
                    }
                    // and use it as part of the weight
                    weight += fi[i] * weights[i];
                }

                // add the array of feature values to the map if just computed
                if (!cachedFI) {
                    f.put(rule.getLabel(), fi);
                }

                // set the computed weight to the rule
                rule.setWeight(Math.exp(weight));
            }
        }
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
    @CallableFromShell(name = "weights")
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
     * @throws NoFeaturesException if the MaximumEntropyIrtg doesn't contain
     * features
     */
    public void writeWeights(Writer writer) throws IOException, NoFeaturesException {
        Properties props = new Properties();

        if (featureNames == null) {
            throw new NoFeaturesException("No features functions set yet.");
        }

        for (int i = 0; i < weights.length; i++) {
            props.put(featureNames.get(i), String.valueOf(weights[i]));
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
                ret.append(features[i].toString());
                ret.append("\n");
            }
        }

        return ret.toString();
    }

    /**
     * Exception class to declare class specific faults
     */
    public class NoFeaturesException extends Exception {
        private NoFeaturesException(String s) {
            super(s);
        }
    }

    /**
     * Exception class to declare class specific faults
     */
    public class NoRepresentationException extends Exception {
        private NoRepresentationException(String s) {
            super(s);
        }
    }

    public static void setLoggingLevel(Level level) {
        log.setLevel(level);
    }
}
/**
 * Reads a grammar and a corpus, computes charts for the corpus entries and
 * writes them to a file
 *
 * @param args an array of string containing the optional arguments from the
 * call
 * @throws IOException if an error occurs on accessing the files
 * @throws ParseException if parsing the grammar fails
 */
/*
 public static void main(String[] args) throws ParseException, IOException {
 String prefix = (args.length > 0) ? args[0] : "ptb-test";
 log.log(Level.INFO, "Starting saving charts of MaximumEntropyIrtg...");
 log.info("Reading grammar...");
 MaximumEntropyIrtg i = (MaximumEntropyIrtg) IrtgParser.parse(new FileReader(prefix + "-grammar.irtg"));
 try {
 i.prepare(false, false);
 } catch (MaximumEntropyIrtg.NoRepresentationException ex) {
 log.log(Level.SEVERE, null, ex);
 return;
 } catch (MaximumEntropyIrtg.NoFeaturesException ex) {
 log.log(Level.SEVERE, null, ex);
 return;
 }
        
 log.info("Reading corpus...");
 AnnotatedCorpus anCo = i.readAnnotatedCorpus(new FileReader(prefix + "-corpus-training.txt"));
 Iterable<AnnotatedCorpus.Instance> instances = anCo.getInstances();

 // compute and cache charts for every instance
 for (AnnotatedCorpus.Instance instance : instances) {
 i.parse(instance.getInputObjects(), true);
 }

 log.info("Writing charts...");
 i.writeCharts(new FileOutputStream(prefix + "-testing.charts"));
 }
 */


    /**
     * Setter for representationInterpName
     * If newer set, the appropriate interpretation will be tried to auto-detect
     *
     * @param interpName name of the interpretation subsequently used to set up a chart
    public final void setRepresentationInterpName(String interpName) {
        representationInterpName = interpName;
    }

     * Setter for treeInterpName
     * If newer set, the appropriate interpretation will be tried to auto-detect
     *
     * @param interpName name of the interpretation subsequently used to validate the best tree
    public final void setTreeInterpName(String interpName) {
        treeInterpName = interpName;
    }
    */


    /**
     * Returns the most probable tree for a chart If the MaximumEntropyIrtg
     * contains an interpretation for TreeAlgebra the homomorphism automaton
     * will be computed and its most probable tree evaluated Without a suitable
     * Algebra the most probable tree of the chart itself will be returned
     *
     * @param chart the weighted chart
     * @return Tree the most probable tree for <tt>chart</tt>
    
    public Tree getBestTree(TreeAutomaton chart) {
        // is there a suitable algebra
        if (!treeInterpName.isEmpty()) {
            Interpretation treeInterp = getInterpretations().get(treeInterpName);

            // get the algebra only if it isn't set already and there is a interpretation
            if ((treeAlgebra == null) && (treeInterp != null)) {
                try {
                    treeAlgebra = (TreeAlgebra) treeInterp.getAlgebra().getClass().newInstance();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            }

            // having an algebra compute the homomorphism and evaluate the most probable tree
            if (treeAlgebra != null) {
                TreeAutomaton<String> outputChart = chart.homomorphism(treeInterp.getHomomorphism());
                return treeAlgebra.evaluate(outputChart.viterbi());
            }
        }

        // there is no suitable algebra --> return the most probable tree for the chart
        return chart.viterbi();
    }
    */