/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Intersectable;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.pruning.PruningPolicy;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.siblingfinder.SiblingFinderIntersection;
import de.up.ling.irtg.siblingfinder.SiblingFinderInvhom;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.irtg.util.Logging;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeBottomUpVisitor;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
        interpretations = new HashMap<>();
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
    @OperationAnnotation(code = "auto")
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
    @OperationAnnotation(code = "interp")
    public Interpretation getInterpretation(String interp) {
        return interpretations.get(interp);
    }

    /**
     * Interprets the given derivation tree in the interpretation with the given
     * name, and returns an object of the algebra.
     *
     * @param derivationTree
     * @param interpretationName
     * @return
     */
    public Object interpret(Tree<String> derivationTree, String interpretationName) {
        return getInterpretation(interpretationName).interpret(derivationTree);
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
        if (derivationTree == null) {
            return null;
        } else {
            Map<String, Object> ret = new HashMap<String, Object>();

            for (String interpretationName : interpretations.keySet()) {
                ret.put(interpretationName, interpret(derivationTree, interpretationName));
            }

            return ret;
        }
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
     *
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
     * may be any subset of the interpretations that this IRTG understands.<p>
     *
     * Note that this method makes no guarantees regarding reducedness of the
     * resulting tree automaton. Depending on the way parsing was done, it may
     * still contain states that are unreachable or unproductive.
     *
     * @param representations
     * @return
     * @throws ParserException
     */
    public TreeAutomaton parse(Map<String, String> representations) throws ParserException {
        Map<String, Object> inputs = new HashMap<>();
        for (String interp : representations.keySet()) {
            inputs.put(interp, parseString(interp, representations.get(interp)));
        }

        return parseInputObjects(inputs);
    }

    /**
     * Parses a single input representations to a parse chart without using any
     * optimization in the parsing process.
     *
     *
     * @param interpretationName name of the interpretation from which the
     * object comes.
     * @param input
     * @return a tree automaton containing all possible derivation trees that
     * are mapped to the input by the interpretation.
     * @throws ParserException
     */
    @OperationAnnotation(code = "parseSimple")
    public TreeAutomaton parseSimple(String interpretationName, Object input) throws ParserException {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(interpretationName, input);
        return parseInputObjects(inputs);
    }

    /**
     * Parses a single input representations to a parse chart using a
     * sibling finder in the intersection.
     *
     * @param interpretationName name of the interpretation from which the
     * object comes.
     * @param input
     * @return a tree automaton containing all possible derivation trees that
     * are mapped to the input by the interpretation.
     * @throws ParserException
     */
    @OperationAnnotation(code = "parseSimpleWithSiblingFinder")
    public TreeAutomaton parseWithSiblingFinder(String interpretationName, Object input) throws ParserException {
        Logging.get().info(() -> "Parsing with sibling finder.");
        SiblingFinderInvhom invhom = new SiblingFinderInvhom(interpretations.get(interpretationName).getAlgebra().decompose(input), interpretations.get(interpretationName).getHomomorphism());
        SiblingFinderIntersection inters = new SiblingFinderIntersection((ConcreteTreeAutomaton) automaton, invhom);
        inters.makeAllRulesExplicit(null);
        return inters.seenRulesAsAutomaton();
    }

    public TreeAutomaton parseCondensedWithPruning(Map<String, Object> inputs, PruningPolicy pp) {
        TreeAutomaton ret = automaton;

        Logging.get().info(() -> "Parsing with condensed/pruning.");
        if (pp != null) {
            Logging.get().info(() -> "... with pruning policy: " + pp.getClass().getSimpleName());
        }

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);

            CondensedTreeAutomaton interpParse = interp.parseToCondensed(input);
            ret = ret.intersectCondensed(interpParse, pp);
        }

        return ret;
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
        TreeAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);

            Intersectable interpParse = interp.parse(input);
            ret = ret.intersect(interpParse);
        }

        // In earlier versions, we reduced the chart top-down. This is needed
        // in certain circumstances (like EM training) to ensure that no 
        // unproductive states are left in the chart. We have deleted it for
        // efficiency reasons (it consumed 25% of the parsing time, and is
        // not needed if the chart is only used to compute the language or
        // Viterbi). If your code requires that the chart is reduced,
        // you can simply call reduceTopDown yourself.
        //        ret = ret.reduceTopDown();
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

        Set<Object> ret = new HashSet<>();
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
        final Map<Integer, Rule> ruleForTerminal = new HashMap<>(); // label -> rules
        final Map<Integer, Long> ruleCounts = new HashMap<>();
        final Map<Integer, Long> stateCounts = new HashMap<>();

        // initialize data
        for (Rule rule : automaton.getRuleSet()) {
            if (ruleForTerminal.containsKey(rule.getLabel())) {
                throw new UnsupportedOperationException("ML training only supported if no two rules use the same terminal symbol; but " + rule.getLabel(automaton) + " is duplicate.");
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
     * The algorithm terminates as soon as the rate of the likelihood increases
     * drops below 1E-5.
     *
     * @param trainingData
     */
    public void trainEM(Corpus trainingData) {
        trainEM(trainingData, null);
    }

    /**
     * Performs expectation maximization (EM) training of this (weighted) IRTG
     * using the given corpus and gives progress information to the passed
     * progress listener. The corpus may be unannotated; if it contains
     * annotated derivation trees, these are ignored by the algorithm. However,
     * it must contain a parse chart for each instance (see {@link Corpus} for
     * details)
     * .<p>
     *
     * The algorithm terminates as soon as the rate of the likelihood increases
     * drops below 1E-5.
     *
     * @param trainingData
     * @param listener
     */
    public void trainEM(Corpus trainingData, ProgressListener listener) {
        trainEM(trainingData, 0, 1E-5, listener);
    }

    /**
     * Performs expectation maximization (EM) training of this (weighted) IRTG
     * using the given corpus and gives progress information to the passed
     * progress listener. The corpus may be unannotated; if it contains
     * annotated derivation trees, these are ignored by the algorithm. However,
     * it must contain a parse chart for each instance (see {@link Corpus} for
     * details)
     * .<p>
     *
     * The algorithm terminates after a given number of iterations or as soon as
     * the rate the likelihood increases drops below a given threshold.
     *
     *
     * @param trainingData
     * @param iterations maximum number of iterations allowed
     * @param threshold minimum change in log-likelihood that prevents stopping
     * of the iterations
     * @param listener
     */
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
        List<TreeAutomaton> parses = new ArrayList<>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<>();
        ListMultimap<Rule, Rule> originalRuleToIntersectedRules = ArrayListMultimap.create();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, originalRuleToIntersectedRules);

        Map<Rule, Double> globalRuleCount = new HashMap<>();
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

            // get the new log likelihood and substract the old one from it for comparison with the given threshold
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
     *
     * This calls
     * {@link de.up.ling.irtg.automata.TreeAutomaton#normalizeRuleWeights normalizeWeights}
     * on the tree automaton that produces the derivation trees.
     */
    public void normalizeRuleWeights() {
        automaton.normalizeRuleWeights();
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
     * Iteration will terminate once the change in the ELBO falls below 1E-5.
     *
     * @param trainingData a corpus of parse charts
     */
    public void trainVB(Corpus trainingData) {
        trainVB(trainingData, null);
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
     * Iteration will terminate once the change in the ELBO falls below 1E-5.
     *
     * @param trainingData a corpus of parse charts
     * @param listener a progress listener that will be given information about
     * the progress of the optimization.
     */
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
     * @param iterations the maximum number of iterations allowed
     * @param threshold the minimum change in the ELBO before iterations are
     * stopped
     * @param listener a progress listener that will be given information about
     * the progress of the optimization.
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
        List<TreeAutomaton> parses = new ArrayList<>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<>();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, null);

        // initialize hyperparameters
        List<Rule> automatonRules = new ArrayList<>();
        Iterables.addAll(automatonRules, getAutomaton().getRuleSet()); // bring rules in defined order

        int numRules = automatonRules.size();
        double[] alpha = new double[numRules];
        Arrays.fill(alpha, 1.0); // might want to initialize them differently

        Map<Rule, Double> ruleCounts = new HashMap<>();
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
            Map<Integer, Double> sumAlphaForSameParent = new HashMap<>();
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
     * counts are computed for all rules that occur in the parsed corpus.<p>
     *
     * This method assumes that the automaton is top-down reduced (see {@link TreeAutomaton#reduceTopDown()
     * }).
     *
     * @param parses
     * @param globalRuleCount
     * @param intersectedRuleToOriginalRule
     * @param listener
     * @param iteration
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
            TreeAutomaton chartHere = instance.getChart().reduceTopDown(); // ensure that chart is top-down reduced
            parses.add(chartHere);

            Iterable<Rule> rules = chartHere.getRuleSet();
            Map<Rule, Rule> irtorHere = new HashMap<>();
            for (Rule intersectedRule : rules) {
                Rule originalRule = getRuleInGrammar(intersectedRule, chartHere);

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
     * Loads a corpus for this IRTG using the given a reader.
     *
     * The corpus must define a subset of the interpretations which this IRTG
     * defines.
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
            listener.accept(i, N, "Parsing 1/" + N);
        }

        // suppress INFOs from intersection algorithms
        Level oldLevel = Logging.get().getLevel();
        Logging.get().setLevel(Level.WARNING);

        try {
            for (Instance inst : input) {
                if ((filter == null) || filter.test(inst)) {
                    CpuTimeStopwatch sw = new CpuTimeStopwatch();
                    sw.record(0);

//                    System.err.println("parse: " + inst.getInputObjects());
//                    System.err.println("   - " + input.hasCharts());
                    TreeAutomaton chart = input.hasCharts() ? inst.getChart() : parseInputObjects(inst.getInputObjects());
//                    System.err.println("ok");
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
                        i++;
                        listener.accept(i, N, "Parsing " + (i + 1) + "/" + N);
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
     * The IRTG is given in the same format that the IrtgInputCodec understands.
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

    /**
     * Helper method that reads an IRTG from an input stream as with
     * {@link de.up.ling.irtg.codec.IrtgInputCodec#read read} from an IRTG input
     * codec.
     *
     * @param r
     * @return
     * @throws IOException
     * @throws CodecParseException
     */
    public static InterpretedTreeAutomaton read(InputStream r) throws IOException, CodecParseException {
        return new IrtgInputCodec().read(r);
    }

    /**
     * Helper method that reads an IRTG from a string as with
     * {@link de.up.ling.irtg.codec.IrtgInputCodec#read read} from an IRTG input
     * codec.
     *
     * @param s
     * @return
     * @throws IOException
     * @throws CodecParseException
     */
    @OperationAnnotation(code = "irtgFromString")
    public static InterpretedTreeAutomaton fromString(String s) throws IOException, CodecParseException {
        return read(new ByteArrayInputStream(s.getBytes("UTF-8")));
    }

    /**
     * Helper method that creates a stream from the given path and reads it as
     * with {@link de.up.ling.irtg.codec.IrtgInputCodec#read read} from an IRTG
     * input codec.
     *
     * @param path
     * @return
     * @throws IOException
     * @throws CodecParseException
     */
    @OperationAnnotation(code = "irtgFromPath")
    public static InterpretedTreeAutomaton fromPath(String path) throws IOException, CodecParseException {
        return read(new FileInputStream(path));
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

    /**
     * Creates a new IRTG with many of the rules filtered out.
     *
     * The rules are filtered out if they contain a constant in the given
     * interpretation which cannot be used in deriving the given object.
     *
     * Note: for some binarized grammars, {@link de.up.ling.irtg.InterpretedTreeAutomaton#filterForAppearingConstants(java.lang.String, java.lang.Object)}
     * is applicable and much more efficient. It is recommend it to use it instead
     * of this method whenever applicable (read its documentation carefully).
     * 
     * @param interpName
     * @param input
     * @return
     */
    @OperationAnnotation(code = "filter")
    public InterpretedTreeAutomaton filterForAppearingConstants(String interpName, Object input) {

        TreeAutomaton decompositionAutomaton = getInterpretation(interpName).getAlgebra().decompose(input);
        Int2IntMap old2NewSignature = new Int2IntOpenHashMap();
        Signature filteredSourceSignature = new Signature();
        InterpretedTreeAutomaton filteredIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>(filteredSourceSignature));
        Map<String, Algebra> filteredAlgebras = new HashMap<>();
        Map<String, Homomorphism> filteredHomomorphisms = new HashMap<>();

        //getting matching rules
        Iterable<Rule> matchingRules = getConstantMatchingRules(interpName, decompositionAutomaton);

        //build the signatures and algebras
        for (String interpNameHere : getInterpretations().keySet()) {
            try {
                filteredAlgebras.put(interpNameHere, getInterpretation(interpNameHere).getAlgebra().getClass().newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                System.err.println("Cound not instantiate algebra for interpretation " + interpNameHere + ": " + ex.toString());
            }
        }
        for (Rule matchingRule : matchingRules) {
            //using that each label only appears in one rule
            int newSymbolID = filteredSourceSignature.addSymbol(matchingRule.getLabel(this.automaton), matchingRule.getArity());
            old2NewSignature.put(matchingRule.getLabel(), newSymbolID);

            //now the signatures in the algebras
            getInterpretations().entrySet().stream().forEach((entry) -> {
                Algebra algebraHere = filteredAlgebras.get(entry.getKey());
                entry.getValue().getHomomorphism().get(matchingRule.getLabel()).dfs(new TreeBottomUpVisitor<HomomorphismSymbol, Void>() {

                    @Override
                    public Void combine(Tree<HomomorphismSymbol> tree, List<Void> list) {
                        if (!tree.getLabel().isVariable()) {
                            algebraHere.getSignature().addSymbol(entry.getValue().getHomomorphism().getTargetSignature().resolveSymbolId(tree.getLabel().getValue()), list.size());
                        }
                        return null;
                    }
                });
            });
        }

        //building the homomorphisms
        for (String interpNameHere : getInterpretations().keySet()) {
            filteredHomomorphisms.put(interpNameHere, new Homomorphism(filteredSourceSignature, filteredAlgebras.get(interpNameHere).getSignature()));
        }
        for (Rule matchingRule : matchingRules) {
            getInterpretations().entrySet().stream().forEach((entry) -> {
                Homomorphism filteredHomomorphism = filteredHomomorphisms.get(entry.getKey());
                filteredHomomorphism.add(filteredSourceSignature.resolveSymbolId(old2NewSignature.get(matchingRule.getLabel())),
                        entry.getValue().getHomomorphism().get(entry.getValue().getHomomorphism().getSourceSignature().resolveSymbolId(matchingRule.getLabel())));
            });
        }

        //add interpretations
        for (String interpNameHere : getInterpretations().keySet()) {
            Algebra algebraHere = filteredAlgebras.get(interpNameHere);
            Interpretation filteredInterpretation = new Interpretation(algebraHere, filteredHomomorphisms.get(interpNameHere));
            filteredIRTG.addInterpretation(interpNameHere, filteredInterpretation);
        }

        //add states to automaton
        for (int stateID : automaton.getStateInterner().getKnownIds()) {
            filteredIRTG.automaton.getStateInterner().addObjectWithIndex(stateID, automaton.getStateInterner().resolveId(stateID));
        }
        //add rules and labels to automaton
        for (Rule matchingRule : matchingRules) {
            ((ConcreteTreeAutomaton) filteredIRTG.automaton).addRule(filteredIRTG.automaton.createRule(
                    matchingRule.getParent(), old2NewSignature.get(matchingRule.getLabel()), matchingRule.getChildren(), matchingRule.getWeight()));
        }

        //set final states
        for (int finalState : automaton.getFinalStates()) {
            ((ConcreteTreeAutomaton) filteredIRTG.automaton).addFinalState(finalState);
        }

        return filteredIRTG;

    }

    /**
     * Creates a new IRTG with many of the rules filtered out.
     *
     * The rules are filtered out if they contain a constant in the given
     * interpretation which cannot be used in deriving the given object or rules
     * which are connected by binarization to the rules that have been removed.
     * Here "connected by binarization" means that the rule labels share a prefix
     * before the binarization tag starting with "_br".
     *
     * Note that some binarization techniques, e.g. the "inside" strategy used
     * in {@link de.up.ling.irtg.binarization.InsideRuleFactory}, pool rules
     * together after binarization. In these cases, this function is not applicable!!
     * The reason is: when this method removes a rule due to an invalid constant,
     * it also removes all rules with the same prefix before the "_br" marker.
     * If the non-lexicalized parts of the rules are pooled, this can remove a rule we would actually need.
     * E.g. if two rules a and b are split into rules a_br1, a_br2, b_br3 and b_br4, where a_br1 and b_br3 have constants,
     * and the binarization method considers a_br2 and b_br4 to be equal (same homomorphic images etc) and pools them
     * (keeping only b_br_4),
     * then we have only a_br1, b_br3 and b_br4 left. If we now parse a sentence which contains the constant in
     * a but not the one in b, we remove rules b_br3 and b_br4. But to parse, we would need the rule b_br_4.
     * Thus, for grammars binarized with such
     * strategies, use
     * {@link de.up.ling.irtg.InterpretedTreeAutomaton#filterForAppearingConstants(java.lang.String, java.lang.Object)}
     * instead.
     *
     * @param interpName
     * @param input
     * @return
     */
    @OperationAnnotation(code = "filterBinarized")
    public InterpretedTreeAutomaton filterBinarizedForAppearingConstants(String interpName, Object input) {

        TreeAutomaton decompositionAutomaton = getInterpretation(interpName).getAlgebra().decompose(input);

        //clone automaton signature
        Signature autoSig = (Signature) automaton.getSignature().clone();

        InterpretedTreeAutomaton filteredIRTG = new InterpretedTreeAutomaton(new ConcreteTreeAutomaton<>(autoSig));

        //add states to automaton
        for (int stateID : automaton.getStateInterner().getKnownIds()) {
            filteredIRTG.automaton.getStateInterner().addObjectWithIndex(stateID, automaton.getStateInterner().resolveId(stateID));
        }

        //getting matching rules
        Iterable<Rule> matchingRules = getConstantMatchingRulesWithCaching(interpName, decompositionAutomaton);

        Set<String> allowedLabels = new HashSet<>();
        for (Rule rule : matchingRules) {
            allowedLabels.add(rule.getLabel(automaton));
        }

        Set<String> forbiddenStems = new HashSet<>();
        for (Rule rule : automaton.getRuleSet()) {
            if (!allowedLabels.contains(rule.getLabel(automaton))) {
                forbiddenStems.add(getBinarizationStem(rule.getLabel(automaton)));
            }
        }

        //add rules and labels to automaton
        for (Rule matchingRule : matchingRules) {
            if (!forbiddenStems.contains(getBinarizationStem(matchingRule.getLabel(automaton)))) {
                ((ConcreteTreeAutomaton) filteredIRTG.automaton).addRule(matchingRule);
            }
        }

        //set final states
        for (int finalState : automaton.getFinalStates()) {
            ((ConcreteTreeAutomaton) filteredIRTG.automaton).addFinalState(finalState);
        }

        //make homomorphisms
        Map<String, Interpretation> newInterps = new HashMap<>();
        for (Map.Entry<String, Interpretation> nameAndInterp : getInterpretations().entrySet()) {
            Interpretation interp = nameAndInterp.getValue();
            Signature interpSig = (Signature) interp.getHomomorphism().getTargetSignature().clone();
            Homomorphism newHom = new Homomorphism(autoSig, interpSig);
            for (Rule rule : filteredIRTG.automaton.getRuleSet()) {
                newHom.add(rule.getLabel(), interp.getHomomorphism().get(rule.getLabel()));
            }
            newInterps.put(nameAndInterp.getKey(), new Interpretation(interp.getAlgebra(), newHom));
        }

        //add interpretations
        filteredIRTG.addAllInterpretations(newInterps);

        return filteredIRTG;

    }

    private Iterable<Rule> getConstantMatchingRulesWithCaching(String interpName, TreeAutomaton decompositionAutomaton) {
        Homomorphism hom = getInterpretation(interpName).getHomomorphism();
        List<Rule> ret = new ArrayList<>();
        Int2BooleanMap constCacher = new Int2BooleanOpenHashMap();
        for (Rule rule : automaton.getRuleSet()) {
            //check if constant rules are in decomposition automaton
            boolean allConstantsFound = true;
            for (HomomorphismSymbol label : hom.get(rule.getLabel()).getLeafLabels()) {
                if (label.isConstant()) {
                    if (constCacher.containsKey(label.getValue())) {
                        if (!constCacher.get(label.getValue())) {
                            allConstantsFound = false;
                            break;
                        } //else continue looking
                    } else if (decompositionAutomaton.getRulesBottomUp(label.getValue(), new int[]{}).iterator().hasNext()) {
                        constCacher.put(label.getValue(), true);
                    } else {
                        constCacher.put(label.getValue(), false);
                        allConstantsFound = false;
                        break;
                    }
                }
            }
            if (allConstantsFound) {
                //System.out.println("All constants found!");
                ret.add(rule);
            }
        }
        return ret;
    }

    private static String getBinarizationStem(String ruleLabel) {
        return ruleLabel.split("_br")[0];
    }

    private Iterable<Rule> getConstantMatchingRules(String interpName, TreeAutomaton decompositionAutomaton) {
        Homomorphism hom = getInterpretation(interpName).getHomomorphism();
        List<Rule> ret = new ArrayList<>();
        for (Rule rule : automaton.getRuleSet()) {
            //check if constant rules are in decomposition automaton
            boolean allConstantsFound = true;
            for (HomomorphismSymbol label : hom.get(rule.getLabel()).getLeafLabels()) {
                if (label.isConstant() && (!decompositionAutomaton.getRulesBottomUp(label.getValue(), new int[]{}).iterator().hasNext())) {
                    allConstantsFound = false;
                }
            }
            if (allConstantsFound) {
                //System.out.println("All constants found!");
                ret.add(rule);
            }
        }
        return ret;
    }

    /**
     * Maps the derivationTree to a term using this homomorphism. Returns a data
     * structure that records, in addition to the derivation tree and the term,
     * a mapping from nodes of the derivation tree to nodes of the term.
     *
     * @param derivationTree
     * @return
     */
    public TreeWithInterpretations interpretWithPointers(Tree<String> derivationTree) {
        TreeWithInterpretations ret = new TreeWithInterpretations(derivationTree);

        for (String interp : interpretations.keySet()) {
            Homomorphism hom = getInterpretation(interp).getHomomorphism();            
            Map<Tree<String>, Tree<String>> dtNodeToTermNode = new IdentityHashMap<>();
            
            Tree<String> term = derivationTree.dfs((node, children) -> {
                Tree<String> h = hom.get(node.getLabel());
                Tree<String> sub = h.substitute(hNode -> {
                    if (HomomorphismSymbol.isVariableSymbol(hNode.getLabel())) {
                        return children.get(HomomorphismSymbol.getVariableIndex(hNode.getLabel()));
                    } else {
                        return null;
                    }
                });

                dtNodeToTermNode.put(node, sub);
                return sub;
            });
            
            ret.addInterpretation(interp, term, dtNodeToTermNode);
        }
        
        return ret;
    }

}
