/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import cc.mallet.optimize.LimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import de.saar.penguin.irtg.AnnotatedCorpus;
import de.saar.penguin.irtg.InterpretedTreeAutomaton;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author koller
 * @author Danilo Baumgarten
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {

    private List<String> featureNames;
    private FeatureFunction[] features;
    private double[] weights;
    private static final double INITIAL_WEIGHT = 0.001;
    private static final double CONVERGE_DELTA = 0.0001;

    /**
     * Constructor
     * @param automaton the TreeAutomaton build by grammar rules
     * @param featureMap the map contains feature functions accessed by their
     *        names. These functions are used to calculate probabilities for the RTG
     */
    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, Map<String, FeatureFunction> featureMap) {
        super(automaton);

        if (featureMap == null) {
            throw new NullPointerException("MaximumEntropyIrtg(automaton, featureMap): featureMap must not be null");
        }
        
        // instantiate member variables
        this.featureNames = new ArrayList<String>();

        // the names of the feature functions are the keys of featureMap
        this.featureNames.addAll(featureMap.keySet());
        // the size of the names list is used to initialize the arrays for the feature functions
        this.features = new FeatureFunction[this.featureNames.size()];
        // and their corresponding weights
        this.weights = new double[this.featureNames.size()];

        for (int i=0; i < this.featureNames.size(); i++) {
            // fill the array for the feature functions using the parameter featureMap
            this.features[i] = featureMap.get(featureNames.get(i));
            // and the weights array using a default value
            this.weights[i] = INITIAL_WEIGHT;
        }
    }

    /**
     * Returns the list of the feature function names
     * @return List<String>() containing the names of all feature functions
     */
    public List<String> getFeatureNames() {
        return this.featureNames;
    }

    /**
     * Returns the array of the feature function weights
     * @return double[] containing the weights of all feature functions
     */
    public double[] getFeatureWeights() {
        return this.weights;
    }

    /**
     * Returns the feature function referenced by name
     * @param name the name of the feature function
     * @return the feature function with the name <tt>name</tt>
     *         if no corresponding function is found
     * @throws ArrayIndexOutOfBoundsException if no feature function for
     *         <tt>name</tt> can be found
     */
    public FeatureFunction getFeatureFunction(String name) throws ArrayIndexOutOfBoundsException {
        int index = this.featureNames.indexOf(name);
        return this.getFeatureFunction(index);
    }

    
    /**
     * Returns the feature function referenced by index
     * @param index the index of the feature function
     * @return the feature function with the <tt>index</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>index</tt> is negative
     *         or not less the number of feature functions
     */
    private FeatureFunction getFeatureFunction(int index) throws ArrayIndexOutOfBoundsException {
        return this.features[index];
    }

    private int getNumFeatures() {
        return this.features.length;
    }
    
    /**
     * Creates a parse chart for the input and weights the chart's rules
     * @param readers the readers (e.g. File- or StringReader) containing
     *        sentences and their interpretation
     * @return the weighted parse chart as TreeAutomaton
     * @throws ParserException if a reader cannot be handled by the parser
     * @throws IOException if the reader cannot read its stream properly
     */
    @Override
    @CallableFromShell(name = "parse")
    public TreeAutomaton parseFromReaders(Map<String, Reader> readers) throws ParserException, IOException {
        TreeAutomaton chart = super.parseFromReaders(readers);

        for (Rule rule : (Set<Rule>) chart.getRuleSet()) {
            double weight = 0.0;

            for (int i = 0; i < this.getNumFeatures(); i++) {
                FeatureFunction featureFunction = this.getFeatureFunction(i);

                double w = this.weights[i];
                double f = featureFunction.evaluate(rule);
                weight += f * w;
            }

            rule.setWeight(Math.exp(weight));
        }

        return chart;
    }

    @CallableFromShell(name = "train")
    public double[] testTraining() throws IOException, ParserException {
        /*************************************************/
        /*************************************************/
        String TRAIN_STR = "i\n"
                + "john watches the woman with the telescope\n"
                + "r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))\n";
        AnnotatedCorpus anCo = AnnotatedCorpus.readAnnotatedCorpus(new StringReader(TRAIN_STR), this);
        this.train(anCo);
        /*************************************************/
        /*************************************************/
        
        return this.weights;
    }

    /**
     * Trains the weights for the rules according to the training data
     * @param corpus the training data containing sentences and their parse tree
     * @throws ParserException
     */
    public void train(AnnotatedCorpus corpus) throws ParserException {
        String interp = (String) corpus.getInstances().get(0).inputObjects.keySet().toArray()[0];
        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(new MaxEntIrtgOptimizable(corpus, interp));
        bfgs.optimize();
    }

    private class MaxEntIrtgOptimizable implements Optimizable.ByGradientValue {

        private boolean cachedStale = true;
        private double cachedValue;
        private double[] cachedGradient;
        private double[] gradientSum1;
        private double[] gradientSum2;
        private AnnotatedCorpus trainingData;
        private String interpretation;
        private Set<Tree<String>> trees;
        private Map<Object, Double> inside;
        private Map<Object, Double> outside;

        public MaxEntIrtgOptimizable(AnnotatedCorpus corpus, String interp) {
            this.cachedStale = true;
            this.trainingData = corpus;
            this.interpretation = interp;
            this.trees = MaximumEntropyIrtg.this.automaton.language();
            this.cachedGradient = new double[MaximumEntropyIrtg.this.getNumFeatures()];
            this.gradientSum1 = new double[this.cachedGradient.length];
            this.gradientSum2 = new double[this.cachedGradient.length];
        }

        @Override
        public double getValue() {
            // TODO: compute log-likelihood here
            // L(Lambda) = sum_x,y(p~(x,y)*sum_i(lambda_i*f_i(x,y)) - 
            //             sum_x(p~(x)*log(sum_y(e^(sum_i(lambda_i*f_i(x,y))))))
            // p~(x,y) = 1/N
            // sum_i(lambda_i*f_i(x,y)) = log(chart.getWeights(y))
            //
            // p~(x) = 1/N
            // e^(sum_i(lambda_i*f_i(x,y))) = chart.getWeights(y)
            if (this.cachedStale) {
                // recompute
                int n = this.trainingData.getInstances().size();
                double sum1 = 0.0;
                double sum2 = 0.0;
                java.util.Arrays.fill(this.gradientSum1, 0.0);
                java.util.Arrays.fill(this.gradientSum2, 0.0);
                
                for (AnnotatedCorpus.Instance instance : this.trainingData.getInstances()) {
                    String s = join((List<String>) instance.inputObjects.get(this.interpretation));
                    Map<String, Reader> readers = new HashMap<String, Reader>();
                    readers.put(this.interpretation, new StringReader(s));
                    TreeAutomaton chart = null;
                    
                    try {
                        chart = MaximumEntropyIrtg.this.parseFromReaders(readers);
                    } catch(ParserException e) {
                        assert false : "getValue(): the parser could not read the input";
                    } catch(IOException e) {
                        assert false : "getValue(): an error on accessing the reader has occurred";
                    }
                    
                    assert (chart != null);

                    // part 1: log-likelihood
                    sum1 += Math.log(chart.getWeight(instance.tree));
                    double innerSum = 0.0;
                    
                    for (Tree y : (Set<Tree>) chart.language()) {
                        innerSum += chart.getWeight(y);
                    }
                    
                    sum2 += Math.log(innerSum);
                    
                    // part 2: the gradient
                    // inside uses the rule's weight
                    // therefore it must be calculated every time
                    this.inside = chart.inside();
                    this.outside = chart.outside(this.inside);
                    // chart.outside() throws a NullPointerException because
                    // in TreeAutomaton.evaluateInSemiringTopDown()
                    // E parentValue = ret.get(rule.getParent()); is null

                    List<Rule> rules = new ArrayList<Rule>();
                    rules.addAll(chart.getRuleSet());
                    double[] pLambdaR = new double[rules.size()];
                    double insideS = 0.0;
                    
                    for (Object start : chart.getFinalStates()) {
                        insideS += this.inside.get(start);
                    }
                    
                    for (int i = 0; i < rules.size(); i++) {
                        Rule r = rules.get(i);
                        Double outside = this.outside.get(r.getParent());
                        if (outside != null) {
                            double insideOutside = outside * r.getWeight();

                            for (Object state : r.getChildren()) {
                                insideOutside *= this.inside.get(state);
                            }

                            pLambdaR[i] = insideOutside / insideS;
                        } else {
                            pLambdaR[i] = 0.0;
                        }
                    }
                    
                    List<String> ruleNames = this.getRuleNames(rules);
                    double pLambda = this.calcPLambda(instance.tree, pLambdaR, ruleNames);
                    
                    for (int i = 0; i < MaximumEntropyIrtg.this.getNumFeatures(); i++) {
                        FeatureFunction ff = MaximumEntropyIrtg.this.getFeatureFunction(i);
                        double fi = this.calcFI(instance.tree, rules, ff);
                        this.gradientSum1[i] += fi;
                        this.gradientSum2[i] += fi * pLambda;
                    }
                }
                
                this.cachedValue = (sum1/n) - (sum2/n);
                for (int i = 0; i < this.cachedGradient.length; i++) {
                    this.cachedGradient[i] = (this.gradientSum1[i]/n) - (this.gradientSum2[i]/n);
                }
                this.cachedStale = false;
            }
            
            return this.cachedValue;
        }
        
        private double calcFI(final Tree tree, final List<Rule> rules, final FeatureFunction f) {
            double fi = 0.0;
            
            for (Rule r : rules) {
                if (r.getLabel().equals((String) tree.getLabel())) {
                    fi += f.evaluate(r);
                }
            }
            
            for (Tree child : (List<Tree>) tree.getChildren()) {
                fi += calcFI(child, rules, f);
            }
            
            return fi;
        }
        
        private List<String> getRuleNames(List<Rule> rules) {
            List<String> ret = new ArrayList<String>();
            for (Rule r : rules) {
                ret.add(r.getLabel());
            }
            return ret;
        }
        
        private double calcPLambda(final Tree tree, final double[] pLambdaR, final List<String> ruleNames) {
            String ruleName = (String) tree.getLabel();
            int i = ruleNames.indexOf(ruleName);
            double pLambda = pLambdaR[i];
            
            for (Tree child : (List<Tree>) tree.getChildren()) {
                pLambda += calcPLambda(child, pLambdaR, ruleNames);
            }
            
            return pLambda;
        }
         
        @Override
        public void getValueGradient(double[] gradient) {
            // TODO: compute <f~i> - <fi> here, for all i
            // sum_x,y(p~(x,y)*f_i(x,y)) - sum_x,y(p~(x)*p_lambda(y|x)*f_i(x,y))
            // sum_x,y in both cases (x,y) from corpus only
            // no additional trees y from automaton required
            // p~(x,y) = 1/N
            // f_i(x,y) = sum_u(f_i(r)) mit r=Regel im Knoten u vom Baum y
            //
            // p~(x) = 1/N
            // p_lambda(y|x) = sum_u (outside(A)*p(r)*inside(B)*inside(C) / inside(S))
            //                 mit r = Regel im Knoten u vom Baum y und p(r) = r.getWeight()
            // f_i(x,y) = sum_u(f_i(r)) mit r=Regel im Knoten u vom Baum y
            // 
            if (this.cachedStale) {
                getValue();
            }
            assert (gradient != null && gradient.length == this.cachedGradient.length);
            System.arraycopy (this.cachedGradient, 0, gradient, 0, this.cachedGradient.length);
        }

        @Override
        public int getNumParameters() {
            return MaximumEntropyIrtg.this.weights.length;
        }

        @Override
        public void getParameters(double[] doubles) {
            doubles = MaximumEntropyIrtg.this.weights;
        }

        @Override
        public double getParameter(int i) throws ArrayIndexOutOfBoundsException {
            return MaximumEntropyIrtg.this.weights[i];
        }

        @Override
        public void setParameters(double[] doubles) {
            MaximumEntropyIrtg.this.weights = doubles;
            this.cachedStale = true;
        }

        @Override
        public void setParameter(int i, double d) throws ArrayIndexOutOfBoundsException {
            MaximumEntropyIrtg.this.weights[i] = d;
            this.cachedStale = true;
        }
    }

    @CallableFromShell(name = "weights")
    public void readWeights(Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = String.valueOf(entry.getKey());
            double weight = Double.valueOf(String.valueOf(entry.getValue()));
            int index = this.getFeatureNames().indexOf(key);

            if (index >= 0) { // no need to check the upper bound, because both collections have the same size
                this.weights[index] = weight;
            }

        }

    }

    public void writeWeights(Writer writer) {
        // TODO: print weights to the writer
    }
    
    private static String join(List<String> strings) {
        return join(strings, " ");
    }

    private static String join(List<String> strings, String delimiter) {
        if (strings == null || strings.isEmpty()) return "";
        Iterator<String> iter = strings.iterator();
        StringBuilder builder = new StringBuilder(iter.next());
        
        while (iter.hasNext()) {
            builder.append(delimiter).append(iter.next());
        }
        
        return builder.toString();
    }
}
