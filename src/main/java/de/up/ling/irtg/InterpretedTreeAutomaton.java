/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.apache.commons.math3.special.Gamma;

/**
 * An interpreted regular tree grammar (IRTG). An IRTG consists of a finite tree
 * automaton M, which generates a language of <i>derivation trees</i>, plus an
 * arbitrary number of <i>interpretations</i>, each of which maps derivation
 * trees to objects over some algebra. In this way, the IRTG describes an
 * n-place relation over these algebras.<p>
 *
 * In this implementation of IRTGs, M is given as an object of class
 * {@link TreeAutomaton}, which is passed to the constructor as an argument.
 * Each interpretation is added by name, and is represented as an object of
 * class {@link Interpretation}. IRTGs are typically read from input streams
 * (e.g. from files) by using an {@link InputCodec}, rather than being
 * constructed programmatically.
 *
 *
 * @author koller
 */
public class InterpretedTreeAutomaton implements Serializable {

    protected TreeAutomaton<String> automaton;
    protected Map<String, Interpretation> interpretations;
    protected boolean debug = false;

    /**
     * Constructs a new IRTG with the given derivation tree automaton.
     *
     * @param automaton
     */
    public InterpretedTreeAutomaton(TreeAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new HashMap<String, Interpretation>();
    }

    /**
     * Adds an interpretation with a given name.
     *
     * @param name
     * @param interp
     */
    public void addInterpretation(String name, Interpretation interp) {
        interpretations.put(name, interp);
    }

    /**
     * Adds all interpretations in the map, with their respective names.
     *
     * @param interps
     */
    public void addAllInterpretations(Map<String, Interpretation> interps) {
        interpretations.putAll(interps);
    }

    /**
     * Returns the derivation tree automaton.
     *
     * @return
     */
    public TreeAutomaton<String> getAutomaton() {
        return automaton;
    }

    /**
     * Returns a map from which the interpretations can be retrieved using their
     * names.
     *
     * @return
     */
    public Map<String, Interpretation> getInterpretations() {
        return interpretations;
    }

    /**
     * Returns the interpretation with the given name.
     *
     * @param interp
     * @return
     */
    public Interpretation getInterpretation(String interp) {
        return interpretations.get(interp);
    }

    /**
     * Maps a given derivation tree to terms over all interpretations and
     * evaluates them. The method returns a mapping of interpretation names to
     * objects in the respective algebras.
     *
     * @param derivationTree
     * @return
     */
    public Map<String, Object> interpret(Tree<String> derivationTree) {
        Map<String, Object> ret = new HashMap<String, Object>();

        for (String interpretationName : interpretations.keySet()) {
            Interpretation interp = interpretations.get(interpretationName);
            ret.put(interpretationName, interp.getAlgebra().evaluate(interp.getHomomorphism().apply(derivationTree)));
        }

        return ret;
    }

    /**
     * ***********************************************************************
     *
     * PARSING AND DECODING
     *
     ***********************************************************************
     */
    /**
     * Resolves the string representation to an object of the given algebra.
     * This is a helper function that retrieves the algebra for the given
     * interpretation, and then calls {@link Algebra#parseString(java.lang.String)
     * }
     * on that algebra.
     *
     * @param interpretation
     * @param representation
     * @return
     * @throws ParserException
     */
    public Object parseString(String interpretation, String representation) throws ParserException {
        Object ret = getInterpretations().get(interpretation).getAlgebra().parseString(representation);
        return ret;
    }

    /**
     * Parses a map of input representations to a parse chart. "Representations"
     * is a map that maps interpretation names to string representations of
     * input objects. Each input object is resolved to object in the respective
     * algebra, and its decomposition automaton computed. Then the pre-images of
     * all decomposition automata under the homomorphism of the respective
     * interpretation are computed, and all are intersected with the derivation
     * tree automaton of the IRTG. The result is returned as a tree automaton;
     * the language of that automaton is the set of all grammatically correct
     * derivation trees that map to the given input objects.<p>
     *
     * The interpretations for which inputs are specified in "representations"
     * may be any subset of the interpretations that this IRTG understands.
     *
     * @param representations
     * @return
     * @throws ParserException
     */
    public TreeAutomaton parse(Map<String, String> representations) throws ParserException {
        Map<String, Object> inputs = new HashMap<String, Object>();
        for (String interp : representations.keySet()) {
            inputs.put(interp, parseString(interp, representations.get(interp)));
        }

        return parseInputObjects(inputs);
    }

    /**
     * Parses a map of input objects to a parse chart. The process is as in
     * {@link #parse(java.util.Map)}, except that the "inputs" map is a map of
     * interpretation names to pre-constructed objects of the respective
     * algebras.
     *
     * @param inputs
     * @return
     */
    public TreeAutomaton parseInputObjects(Map<String, Object> inputs) {
//        Logging.get().fine(() -> "parseInputObjects: " + inputs);

        TreeAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);

//            Logging.get().fine(() -> "Input: " + input);
            TreeAutomaton interpParse = interp.parse(input);
            ret = ret.intersect(interpParse);

//            Logging.get().finest(() -> ("Intersect: " + ret));
        }

        ret = ret.reduceTopDown();

        return ret;
    }

    /**
     * Decodes a parse chart to a term chart over some output algebra. The term
     * chart describes a language of the terms over the specified output
     * algebra. This language is the homomorphic image of the parse chart under
     * the homomorphism of the given output interpretation.
     *
     * @param outputInterpretation
     * @param parseChart
     * @return
     */
    public TreeAutomaton decodeToAutomaton(String outputInterpretation, TreeAutomaton parseChart) {
        return parseChart.homomorphism(interpretations.get(outputInterpretation).getHomomorphism());
    }

    /**
     * Decodes a map of input representations to a set of objects of the
     * specified output algebra. This first computes a parse chart for the input
     * representations, as per {@link #parse(java.util.Map) }. It then decodes
     * the parse chart into an output term chart (see {@link #decodeToAutomaton(java.lang.String, de.up.ling.irtg.automata.TreeAutomaton)
     * }
     * and evaluates each term in the language of the term chart to an object in
     * the output algebra. The method returns the set of all of these objects.
     *
     * @param outputInterpretation
     * @param representations
     * @return
     * @throws ParserException
     */
    public Set<Object> decode(String outputInterpretation, Map<String, String> representations) throws ParserException {
        Algebra out = interpretations.get(outputInterpretation).getAlgebra();
        TreeAutomaton chart = parse(representations);
        TreeAutomaton outputChart = decodeToAutomaton(outputInterpretation, chart);

        Set<Object> ret = new HashSet<Object>();
        Iterator<Tree<String>> it = outputChart.languageIterator();

        while (it.hasNext()) {
            ret.add(out.evaluate(it.next()));
        }

        return ret;
    }

    /**
     * ***********************************************************************
     *
     * TRAINING
     *
     ***********************************************************************
     */
    /**
     * Performs maximum likelihood training of this (weighted) IRTG using the
     * given annotated corpus. In the context of an IRTG, "annotated corpus"
     * means that the derivation tree is annotated for each training instance.
     *
     * @param trainingData
     * @throws UnsupportedOperationException
     */
    public void trainML(Corpus trainingData) throws UnsupportedOperationException {
        final Map<Integer, Rule> ruleForTerminal = new HashMap<Integer, Rule>(); // label -> rules
        final Map<Integer, Long> ruleCounts = new HashMap<Integer, Long>();
        final Map<Integer, Long> stateCounts = new HashMap<Integer, Long>();

        // initialize data
        for (Rule rule : automaton.getRuleSet()) {
            if (ruleForTerminal.containsKey(rule.getLabel())) {
                throw new UnsupportedOperationException("ML training only supported if no two rules use the same terminal symbol.");
            }

            ruleForTerminal.put(rule.getLabel(), rule);
            ruleCounts.put(rule.getLabel(), 0L);
            stateCounts.put(rule.getParent(), 0L);
        }

        // compute absolute frequencies on annotated corpus
        for (Instance instance : trainingData) {
            instance.getDerivationTree().dfs(new TreeVisitor<Integer, Void, Void>() {
                @Override
                public Void visit(Tree<Integer> node, Void data) {
                    Rule rule = ruleForTerminal.get(node.getLabel());

                    ruleCounts.put(node.getLabel(), ruleCounts.get(node.getLabel()) + 1);
                    stateCounts.put(rule.getParent(), stateCounts.get(rule.getParent()) + 1);

                    return null;
                }
            });
        }

        // set all rule weights according to counts
        for (int label : ruleForTerminal.keySet()) {
            Rule rule = ruleForTerminal.get(label);
            long stateCount = stateCounts.get(rule.getParent());

            if (stateCount == 0) {
                rule.setWeight(0.0);
            } else {
                rule.setWeight(((double) ruleCounts.get(label)) / stateCount);
            }
        }
    }

    /**
     * Performs expectation maximization (EM) training of this (weighted) IRTG
     * using the given corpus. The corpus may be unannotated; if it contains
     * annotated derivation trees, these are ignored by the algorithm. However,
     * it must contain a parse chart for each instance (see {@link Corpus} for
     * details)
     * .<p>
     *
     * The algorithm terminates after a given number of iterations or as soon as
     * the rate the likelihood increases drops below a given threshold.
     *
     * @param trainingData
     */
    public void trainEM(Corpus trainingData) {
        trainEM(trainingData, null);
    }

    public void trainEM(Corpus trainingData, ProgressListener listener) {
        trainEM(trainingData, 0, 1E-5, listener);
    }

    public void trainEM(Corpus trainingData, int iterations, double threshold, ProgressListener listener) {
        if (!trainingData.hasCharts()) {
            System.err.println("EM training can only be performed on a corpus with attached charts.");
            return;
        }
        if (iterations <= 0 && threshold < 0) {
            System.err.println("EM training needs either a valid threshold or a valid number of iterations.");
        }
        if (debug) {
            System.out.println("\n\nInitial model:\n" + automaton);
        }

        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        ListMultimap<Rule, Rule> originalRuleToIntersectedRules = ArrayListMultimap.create();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, originalRuleToIntersectedRules);

        Map<Rule, Double> globalRuleCount = new HashMap<Rule, Double>();
        // Threshold parameters
        if (iterations <= 0) {
            iterations = Integer.MAX_VALUE;
        }
        double oldLogLikelihood = Double.NEGATIVE_INFINITY;
        double difference = Double.POSITIVE_INFINITY;
        int iteration = 0;

        while (difference > threshold && iteration < iterations) {
            if (debug) {
                for (Rule r : originalRuleToIntersectedRules.keySet()) {
                    System.err.println("Iteration:  " + iteration);
                    System.err.println("Rule:       " + r.toString(automaton));
                    System.err.println("Rule (raw): " + r);
                    System.err.println("Weight:     " + r.getWeight());
                    System.err.print("\n");
                }
            }

            // get the new log likelihood and substract the old one from it for comparrison with the given threshold
            double logLikelihood = estep(parses, globalRuleCount, intersectedRuleToOriginalRule, listener, iteration);
            assert logLikelihood >= oldLogLikelihood;
            difference = logLikelihood - oldLogLikelihood;
            oldLogLikelihood = logLikelihood;

            if (debug) {
                System.err.println("Current LL: " + logLikelihood + "\n");
            }

            // sum over rules with same parent state to obtain state counts
            Map<Integer, Double> globalStateCount = new HashMap<Integer, Double>();
            for (int state : automaton.getAllStates()) {
                globalStateCount.put(state, 0.0);
            }

            for (Rule rule : automaton.getRuleSet()) {
                int state = rule.getParent();
                globalStateCount.put(state, globalStateCount.get(state) + globalRuleCount.get(rule));
            }

            // M-step
            for (Rule rule : automaton.getRuleSet()) {
                double newWeight = globalRuleCount.get(rule) / globalStateCount.get(rule.getParent());

                rule.setWeight(newWeight);
                for (Rule intersectedRule : originalRuleToIntersectedRules.get(rule)) {
                    intersectedRule.setWeight(newWeight);
                }
            }

            if (debug) {
                System.out.println("\n\n***** After iteration " + (iteration + 1) + " *****\n\n" + automaton);
            }
            ++iteration;
        }

    }

    /**
     * Modifies the rule weights of the derivation tree automaton such that the
     * weights for all rules with the same parent state sum to one.
     */
    public void normalizeRuleWeights() {
        automaton.normalizeRuleWeights();
    }

    public void trainVB(Corpus trainingData) {
        trainVB(trainingData, null);
    }

    public void trainVB(Corpus trainingData, ProgressListener listener) {
        trainVB(trainingData, 0, 1E-5, listener);
    }

    /**
     * Performs Variational Bayes (VB) training of this (weighted) IRTG using
     * the given corpus. The corpus may be unannotated; if it contains annotated
     * derivation trees, these are ignored by the algorithm. However, it must
     * contain a parse chart for each instance (see {@link Corpus} for details)
     * .<p>
     *
     * This method implements the algorithm from Jones et al., "Semantic Parsing
     * with Bayesian Tree Transducers", ACL 2012.
     *
     * @param trainingData a corpus of parse charts
     */
    public void trainVB(Corpus trainingData, int iterations, double threshold, ProgressListener listener) {
        if (!trainingData.hasCharts()) {
            System.err.println("VB training can only be performed on a corpus with attached charts.");
            return;
        }
        if (iterations <= 0 && threshold < 0) {
            System.err.println("VB training needs either a valid threshold or a valid number of iterations.");
        }
        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, null);

        // initialize hyperparameters
        List<Rule> automatonRules = new ArrayList<Rule>();
        Iterables.addAll(automatonRules, getAutomaton().getRuleSet()); // bring rules in defined order

        int numRules = automatonRules.size();
        double[] alpha = new double[numRules];
        Arrays.fill(alpha, 1.0); // might want to initialize them differently

        Map<Rule, Double> ruleCounts = new HashMap<Rule, Double>();
        // Threshold parameters
        if (iterations <= 0) {
            iterations = Integer.MAX_VALUE;
        }
        double oldLogLikelihood = Double.NEGATIVE_INFINITY;
        double difference = Double.POSITIVE_INFINITY;
        int iteration = 0;

        // iterate
        while (difference > threshold && iteration < iterations) {
            // for each state, compute sum of alphas for outgoing rules
            Map<Integer, Double> sumAlphaForSameParent = new HashMap<Integer, Double>();
            for (int i = 0; i < numRules; i++) {
                int parent = automatonRules.get(i).getParent();
                if (sumAlphaForSameParent.containsKey(parent)) {
                    sumAlphaForSameParent.put(parent, sumAlphaForSameParent.get(parent) + alpha[i]);
                } else {
                    sumAlphaForSameParent.put(parent, alpha[i]);
                }
            }

            // re-estimate rule weights
            for (int i = 0; i < numRules; i++) {
                Rule rule = automatonRules.get(i);
                rule.setWeight(Math.exp(Gamma.digamma(alpha[i]) - Gamma.digamma(sumAlphaForSameParent.get(rule.getParent()))));
            }

            // re-estimate hyperparameters
            double logLikelihood = estep(parses, ruleCounts, intersectedRuleToOriginalRule, listener, iteration);
            assert logLikelihood >= oldLogLikelihood;
            for (int i = 0; i < numRules; i++) {
                alpha[i] += ruleCounts.get(automatonRules.get(i));
            }

            // calculate the difference for comparrison with the given threshold 
            difference = logLikelihood - oldLogLikelihood;
            oldLogLikelihood = logLikelihood;
            ++iteration;
        }
    }

    /**
     * Performs the E-step of the EM algorithm. This means that the expected
     * counts are computed for all rules that occur in the parsed corpus.
     *
     * @param parses
     * @param intersectedRuleToOriginalRule
     * @return
     */
    protected double estep(List<TreeAutomaton> parses, Map<Rule, Double> globalRuleCount, List<Map<Rule, Rule>> intersectedRuleToOriginalRule, ProgressListener listener, int iteration) {
        double logLikelihood = 0;

        globalRuleCount.clear();

        for (Rule rule : automaton.getRuleSet()) {
            globalRuleCount.put(rule, 0.0);
        }

        for (int i = 0; i < parses.size(); i++) {
            TreeAutomaton parse = parses.get(i);

            Map<Integer, Double> inside = parse.inside();
            Map<Integer, Double> outside = parse.outside(inside);

            if (debug) {
                System.out.println("Inside and outside probabilities for chart #" + i);

                for (Integer r : inside.keySet()) {
                    System.out.println("Inside: " + parse.getStateForId(r) + " | " + inside.get(r));
                }
                System.out.println("-");

                for (Integer r : outside.keySet()) {
                    System.out.println("Outside: " + parse.getStateForId(r) + " | " + outside.get(r));
                }
                System.out.println("");
            }

            double likelihoodHere = 0;
            for (int finalState : parse.getFinalStates()) {
                likelihoodHere += inside.get(finalState);
            }

            for (Rule intersectedRule : intersectedRuleToOriginalRule.get(i).keySet()) {
                Integer intersectedParent = intersectedRule.getParent();
                Rule originalRule = intersectedRuleToOriginalRule.get(i).get(intersectedRule);

                double oldRuleCount = globalRuleCount.get(originalRule);
                double thisRuleCount = outside.get(intersectedParent) * intersectedRule.getWeight() / likelihoodHere;

                for (int j = 0; j < intersectedRule.getArity(); j++) {
                    thisRuleCount *= inside.get(intersectedRule.getChildren()[j]);
                }

                globalRuleCount.put(originalRule, oldRuleCount + thisRuleCount);
            }

            logLikelihood += Math.log(likelihoodHere);

            if (listener != null) {
                listener.accept(i + 1, parses.size(), null);
//                listener.update(iteration, i);
            }
        }

        return logLikelihood;
    }

    /**
     * Extracts the parse charts from the training data. Furthermore, computes
     * mappings from the rules in the training data to the rules in the
     * underlying automaton of the IRTG, and vice versa. You may set
     * "originalRulesToIntersectedRules" to null if you don't care about this
     * mapping.
     *
     * @param trainingData
     * @param parses
     * @param intersectedRuleToOriginalRule
     * @param originalRuleToIntersectedRules
     */
    private void collectParsesAndRules(Corpus trainingData, List<TreeAutomaton> parses, List<Map<Rule, Rule>> intersectedRuleToOriginalRule, ListMultimap<Rule, Rule> originalRuleToIntersectedRules) {
        parses.clear();
        intersectedRuleToOriginalRule.clear();

        if (originalRuleToIntersectedRules != null) {
            originalRuleToIntersectedRules.clear();
        }

        for (Instance instance : trainingData) {
            parses.add(instance.getChart());

            Iterable<Rule> rules = instance.getChart().getRuleSet();
            Map<Rule, Rule> irtorHere = new HashMap<Rule, Rule>();
            for (Rule intersectedRule : rules) {
                Rule originalRule = getRuleInGrammar(intersectedRule, instance.getChart());

                irtorHere.put(intersectedRule, originalRule);

                if (originalRuleToIntersectedRules != null) {
                    originalRuleToIntersectedRules.put(originalRule, intersectedRule);
                }
            }

            intersectedRuleToOriginalRule.add(irtorHere);
        }
    }

    // safe but inefficient
    // relationship between rules of chart and deriv-tree automaton,
    // or at least mapping between states, should be precomputed only once.
    Rule getRuleInGrammar(Rule intersectedRule, TreeAutomaton chart) {
        int firstParentState = getAutomaton().getIdForState(getFirstEntry(chart.getStateForId(intersectedRule.getParent())).toString());

        int[] firstChildStates = new int[intersectedRule.getArity()];
        for (int i = 0; i < intersectedRule.getArity(); i++) {
            int pairState = intersectedRule.getChildren()[i];
            int firstState = getAutomaton().getIdForState(getFirstEntry(chart.getStateForId(pairState)).toString());
            firstChildStates[i] = firstState;
        }

        for (Rule candidate : automaton.getRulesBottomUp(intersectedRule.getLabel(), firstChildStates)) {
            if (firstParentState == candidate.getParent()) {
                return candidate;
            }
        }

        return null;
    }

    private Object getFirstEntry(Object pairState) {
        if (pairState instanceof Pair) {
            return getFirstEntry(((Pair) pairState).left);
        } else {
            return pairState;
        }
    }

    /**
     * Switches debugging output on or off.
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Loads a corpus for this IRTG from a reader.
     *
     * @param reader
     * @return
     * @throws IOException
     * @throws CorpusReadingException
     */
    public Corpus readCorpus(Reader reader) throws IOException, CorpusReadingException {
        return Corpus.readCorpus(reader, this);
    }

    /**
     * Reads all inputs for this IRTG from a corpus and parses them. This
     * behaves like {@link #bulkParse(de.up.ling.irtg.corpus.Corpus, java.util.function.Predicate, java.util.function.Consumer, de.up.ling.irtg.util.ProgressListener)
     * }
     * with an instance filter that always returns true.
     *
     * @param input
     * @param corpusConsumer
     * @param listener
     */
    public void bulkParse(Corpus input, Consumer<Instance> corpusConsumer, ProgressListener listener) {
        bulkParse(input, null, corpusConsumer, listener);
    }

    /**
     * Reads inputs for this IRTG from a corpus and parses them. The input
     * corpus must be suitable for this IRTG (i.e., use a subset of the
     * interpretations it defines). If the corpus has charts attached, these
     * will be used; otherwise, each instance for which the "filter" is true is
     * parsed. We then compute the best derivation tree from each chart using
     * Viterbi, and map it to all interpretations of the IRTG. This yields a
     * "completed" {@link Instance} (consisting of the derivation tree and
     * values on all interpretations), which we write to the given
     * corpusConsumer (e.g., a {@link CorpusWriter}). If a non-null value is
     * passed as the "listener", it is notified after each instance has been
     * written.<p>
     *
     * Note that the output corpus may contain fewer instances than the input
     * corpus, if the "filter" returned false on some of the input instances.
     *
     * @param input
     * @param filter
     * @param corpusConsumer
     * @param listener
     */
    public void bulkParse(Corpus input, Predicate<Instance> filter, Consumer<Instance> corpusConsumer, ProgressListener listener) {
        int N = input.getNumberOfInstances();
        int i = 0;

        if (listener != null) {
            listener.accept(i++, N, null);
        }

        // suppress INFOs from intersection algorithms
        Level oldLevel = Logging.get().getLevel();
        Logging.get().setLevel(Level.WARNING);

        try {
            for (Instance inst : input) {
                if ((filter == null) || filter.test(inst)) {
                    CpuTimeStopwatch sw = new CpuTimeStopwatch();
                    sw.record(0);

                    TreeAutomaton chart = input.hasCharts() ? inst.getChart() : parseInputObjects(inst.getInputObjects());
                    WeightedTree t = chart.viterbiRaw();

                    if (t == null) {
                        Instance parsedInst = new Instance();
                        parsedInst.setAsNull();
                        parsedInst.setDerivationTree(null);
                        parsedInst.setComments("could not parse", inst.toString());
                        corpusConsumer.accept(parsedInst);

                        Logging.get().warning("Could not parse: " + inst);
                    } else {
//                        Iterator<WeightedTree> it = chart.sortedLanguageIterator();
//                        System.err.println("w: " + it.next().getWeight());

                        Tree<String> tWithStrings = getAutomaton().getSignature().resolve(t.getTree());

                        Map<String, Object> values = new HashMap<>();
                        for (String intp : getInterpretations().keySet()) {
                            values.put(intp, getInterpretation(intp).interpret(tWithStrings));
                        }

                        sw.record(1);

                        Instance parsedInst = new Instance();
                        parsedInst.setInputObjects(values);
                        parsedInst.setDerivationTree(t.getTree());
                        parsedInst.setComments("parse_time_ms", Double.toString(sw.getTimeBefore(1) / 1000000),
                                "weight=", Double.toString(t.getWeight()));
                        corpusConsumer.accept(parsedInst);
                    }

                    if (listener != null) {
                        listener.accept(i++, N, null);
                    }
                }
            }
        } finally {
            // turn logging back on
            Logging.get().setLevel(oldLevel);
        }
    }

    /**
     * Returns a string representation of the IRTG.
     *
     * @return
     */
    @Override
    public String toString() {
        StringWriter buf = new StringWriter();
        PrintWriter pw = new PrintWriter(buf);
        List<String> interpretationOrder = new ArrayList<String>(interpretations.keySet());

        for (String interp : interpretationOrder) {
            pw.println("interpretation " + interp + ": " + interpretations.get(interp).getAlgebra().getClass().getName());
        }

        pw.println();

        for (Rule rule : automaton.getRuleSet()) {
            pw.println(rule.toString(automaton, automaton.getFinalStates().contains(rule.getParent())));

            for (String interp : interpretationOrder) {
                Homomorphism hom = interpretations.get(interp).getHomomorphism();
                Tree<HomomorphismSymbol> rhs = hom.get(rule.getLabel());
                pw.println("  [" + interp + "] " + hom.rhsAsString(rhs));
            }

            pw.println();
        }

        return buf.toString();
    }

    /**
     * Compares the IRTG to another IRTG for equality. Two IRTGs are considered
     * equal if (1) their derivation tree automata are equal; (2) they define
     * the same interpretation names; (3) for each interpretation name, the
     * homomorphisms are equal; (4) for each interpretation name, the
     * intepretations use the same algebra class.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InterpretedTreeAutomaton other = (InterpretedTreeAutomaton) obj;
        if (this.automaton != other.automaton && (this.automaton == null || !this.automaton.equals(other.automaton))) {
            System.err.println("*** auto !=");

            System.err.println("this auto: " + this.automaton);
            System.err.println("\n\nother auto:" + other.automaton);
            return false;
        }

        if (this.interpretations != other.interpretations && (this.interpretations == null || !this.interpretations.equals(other.interpretations))) {
            System.err.println("*** intp !-");
            return false;
        }

        return true;
    }

    public static InterpretedTreeAutomaton read(InputStream r) throws IOException, CodecParseException {
        return new IrtgInputCodec().read(r);
    }

    /**
     * Creates an empty IRTG for the given algebras. The IRTG contains a tree
     * automaton with no rules, and one interpretation for each entry of the
     * given map, with the given name and the given algebra.
     *
     * @param algebras
     * @return
     */
    public static InterpretedTreeAutomaton forAlgebras(Map<String, Algebra> algebras) {
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>());

        for (String i : algebras.keySet()) {
            irtg.addInterpretation(i, new Interpretation(algebras.get(i), new Homomorphism(irtg.getAutomaton().getSignature(), algebras.get(i).getSignature())));
        }

        return irtg;
    }
}
