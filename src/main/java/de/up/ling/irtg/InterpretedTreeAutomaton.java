/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * class {@link Interpretation}. IRTGs are typically read from readers (e.g.
 * from files) using {@link IrtgParser#parse(java.io.Reader)}, rather than being
 * constructed programmatically.
 *
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {
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
     * @throws IOException
     */
    public Interpretation getInterpretation(String interp) {
        return interpretations.get(interp);
    }
    
    /**
     * Maps a given derivation tree to terms over all interpretations
     * and evaluates them. The method returns a mapping of interpretation
     * names to objects in the respective algebras.
     * 
     * @param derivationTree
     * @return 
     */
    public Map<String,Object> interpret(Tree<String> derivationTree) {
        Map<String,Object> ret = new HashMap<String, Object>();
        
        for( String interpretationName : interpretations.keySet() ) {
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
        TreeAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);
            TreeAutomaton interpParse = interp.parse(input);
            
//            System.err.println("invhom(decomp(" + input  + "):\n" + interpParse.toStringBottomUp());
            
            ret = ret.intersect(interpParse);
        }
        
//        System.err.println("chart before reduction:\n" + ret);

        return ret.reduceTopDown();
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
     * details).<p>
     *
     * Currently the training algorithm performs a fixed number of ten EM
     * iterations. This should be made more flexible in the future.
     *
     * @param trainingData
     */
    public void trainEM(Corpus trainingData) {
        trainEM(trainingData, null);
    }
    
    public void trainEM(Corpus trainingData, TrainingIterationListener listener) {
        trainEM(trainingData, 10, false, listener);
    }

    public void trainEM(Corpus trainingData, int numIterations, boolean printIntermediate, TrainingIterationListener listener) {
        if (!trainingData.hasCharts()) {
            System.err.println("EM training can only be performed on a corpus with attached charts.");
            return;
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


        for (int iteration = 0; iteration < numIterations; iteration++) {
            Map<Rule, Double> globalRuleCount = estep(parses, intersectedRuleToOriginalRule, listener, iteration);

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

            if (printIntermediate) {
                System.out.println("\n\n***** After iteration " + (iteration + 1) + " *****\n\n" + automaton);
            }
        }
    }
    
    public void trainVB(Corpus trainingData) {
        trainVB(trainingData, null);
    }

    /**
     * Performs Variational Bayes (VB) training of this (weighted) IRTG using
     * the given corpus. The corpus may be unannotated; if it contains annotated
     * derivation trees, these are ignored by the algorithm. However, it must
     * contain a parse chart for each instance (see {@link Corpus} for
     * details).<p>
     *
     * Currently the training algorithm performs a fixed number of ten
     * iterations. This should be made more flexible in the future.<p>
     *
     * This method implements the algorithm from Jones et al., "Semantic Parsing
     * with Bayesian Tree Transducers", ACL 2012.
     *
     * @param trainingData a corpus of parse charts
     */
    public void trainVB(Corpus trainingData, TrainingIterationListener listener) {
        if (!trainingData.hasCharts()) {
            System.err.println("VB training can only be performed on a corpus with attached charts.");
            return;
        }

        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, null);

        // initialize hyperparameters
        List<Rule> automatonRules = new ArrayList<Rule>(getAutomaton().getRuleSet()); // bring rules in defined order
        int numRules = automatonRules.size();
        double[] alpha = new double[numRules];
        Arrays.fill(alpha, 1.0); // might want to initialize them differently

        // iterate
        for (int iteration = 0; iteration < 10; iteration++) {
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
            Map<Rule, Double> ruleCounts = estep(parses, intersectedRuleToOriginalRule, listener, iteration);
            for (int i = 0; i < numRules; i++) {
                alpha[i] += ruleCounts.get(automatonRules.get(i));
            }
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
    protected Map<Rule, Double> estep(List<TreeAutomaton> parses, List<Map<Rule, Rule>> intersectedRuleToOriginalRule, TrainingIterationListener listener, int iteration) {
        Map<Rule, Double> globalRuleCount = new HashMap<Rule, Double>();
        for (Rule rule : automaton.getRuleSet()) {
            globalRuleCount.put(rule, 0.0);
        }

        for (int i = 0; i < parses.size(); i++) {
            TreeAutomaton parse = parses.get(i);

            Map<Object, Double> inside = parse.inside();
            Map<Object, Double> outside = parse.outside(inside);

            for (Rule intersectedRule : intersectedRuleToOriginalRule.get(i).keySet()) {
                Object intersectedParent = intersectedRule.getParent();
                Rule originalRule = intersectedRuleToOriginalRule.get(i).get(intersectedRule);

                double oldRuleCount = globalRuleCount.get(originalRule);
                double thisRuleCount = outside.get(intersectedParent) * intersectedRule.getWeight();

                for (int j = 0; j < intersectedRule.getArity(); j++) {
                    thisRuleCount *= inside.get(intersectedRule.getChildren()[j]);
                }

                globalRuleCount.put(originalRule, oldRuleCount + thisRuleCount);
            }
            
            listener.update(iteration, i);
        }

        return globalRuleCount;
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

            Set<Rule> rules = instance.getChart().getRuleSet();
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
     * Binarizes the given IRTG and returns the binarized IRTG. This
     * implementation is currently broken. It will eventually be replaced by a
     * clean implementation of the algorithm of B&uuml;chse et al., ACL 2013.
     *
     * @param binarizers
     * @return
     */
//    private InterpretedTreeAutomaton binarize(Map<String, RegularBinarizer> binarizers) {
//        List<String> orderedInterpretationList = new ArrayList<String>(interpretations.keySet());
//        if (orderedInterpretationList.size() != 2) {
//            throw new UnsupportedOperationException("trying to binarize " + orderedInterpretationList.size() + " interpretations");
//        }
//
//        String interpName1 = orderedInterpretationList.get(0);
//        String interpName2 = orderedInterpretationList.get(1);
//        Interpretation interp1 = interpretations.get(interpName1);
//        Interpretation interp2 = interpretations.get(interpName2);
//        RegularBinarizer bin1 = binarizers.get(interpName1);
//        RegularBinarizer bin2 = binarizers.get(interpName2);
//
//        // select a constant as dummy symbol from both algebras 
//        HomomorphismSymbol constantL = getConstantFromAlgebra(interp1.getHomomorphism());
//        HomomorphismSymbol constantR = getConstantFromAlgebra(interp2.getHomomorphism());
//
//        SynchronousBinarization sb = new SynchronousBinarization(constantL, constantR);
//        ConcreteTreeAutomaton newAuto = new ConcreteTreeAutomaton();
//        Homomorphism newLeftHom = new Homomorphism(newAuto.getSignature(), interp1.getHomomorphism().getTargetSignature());
//        Homomorphism newRightHom = new Homomorphism(newAuto.getSignature(), interp2.getHomomorphism().getTargetSignature());
//
//        for (Rule rule : automaton.getRuleSet()) {
//            TreeAutomaton leftAutomaton = bin1.binarizeWithVariables(interp1.getHomomorphism().get(rule.getLabel()));
//            TreeAutomaton rightAutomaton = bin2.binarizeWithVariables(interp2.getHomomorphism().get(rule.getLabel()));
//            sb.binarize(rule, leftAutomaton, rightAutomaton, newAuto, newLeftHom, newRightHom);
//        }
//        for (int state : automaton.getFinalStates()) {
//            newAuto.addFinalState(state);
//        }
//        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(newAuto);
//        ret.addInterpretation(interpName1, new Interpretation(bin1.getOutputAlgebra(), newLeftHom));
//        ret.addInterpretation(interpName2, new Interpretation(bin2.getOutputAlgebra(), newRightHom));
//
//        return ret;
//    }
    private HomomorphismSymbol getConstantFromAlgebra(Homomorphism hom) {
        for (int label = 1; label <= hom.getSourceSignature().getMaxSymbolId(); label++) {
            HomomorphismSymbol constant = hom.get(label).dfs(new TreeVisitor<HomomorphismSymbol, Void, HomomorphismSymbol>() {
                @Override
                public HomomorphismSymbol combine(Tree<HomomorphismSymbol> node, List<HomomorphismSymbol> childrenValues) {
                    if (node.getChildren().isEmpty() && !node.getLabel().isVariable()) {
                        return node.getLabel();
                    }

                    for (int i = 0; i < childrenValues.size(); i++) {
                        if (childrenValues.get(i) != null) {
                            return childrenValues.get(i);
                        }
                    }
                    return null;
                }
            });
            if (constant != null) {
                return constant;
            }
        }
        throw new UnsupportedOperationException("Cannot find any symbols with arity 0 for this algebra.");
    }

    /**
     * Binarizes the given IRTG and returns the binarized IRTG. This
     * implementation is currently broken. It will eventually be replaced by a
     * clean implementation of the algorithm of B&uuml;chse et al., ACL 2013.
     *
     * @param binarizers
     * @return
     */
    private InterpretedTreeAutomaton binarize() {
        /*
         if (interpretations.keySet().size() > 1) {
         throw new UnsupportedOperationException("Can only binarize IRTGs with a single interpretation.");
         }
         * 
         */

        if (interpretations.keySet().isEmpty()) {
            throw new UnsupportedOperationException("Trying to binarize IRTG without interpretations.");
        }

        // pick the alphabetically first interpretation for the binarization
        List<String> names = new ArrayList<String>(interpretations.keySet());
        Collections.sort(names);

        String interpretationName = names.get(0);
        Interpretation interpretation = interpretations.get(interpretationName);
        Homomorphism homomorphism = interpretation.getHomomorphism();

        // binarized automaton contains all states of the original automaton
        // (plus fresh ones for the binarization)
        ConcreteTreeAutomaton newAutomaton = new ConcreteTreeAutomaton();
        for (int stateId : getAutomaton().getAllStates()) {
            newAutomaton.addState(getAutomaton().getStateForId(stateId).toString());
        }

        Homomorphism newHomomorphism = new Homomorphism(newAutomaton.getSignature(), homomorphism.getTargetSignature());
        Set<Rule> rules = automaton.getRuleSet();

        for (Rule rule : rules) {
            int ruleLabel = rule.getLabel();

            if (rule.getArity() > 2) {
                Tree<HomomorphismSymbol> hRule = homomorphism.get(ruleLabel);
                int parent = rule.getParent();
                String label = makeBinaryLabel(ruleLabel + "-b", 0);

//                Set<Rule> newRules = new HashSet<Rule>();
                binarizeRule(newHomomorphism, newAutomaton, hRule, parent, label, rule);
//                for (Rule r : newRules) {
//                    newAutomaton.addRule(r);
//                }
            } else { // rules of arity <= 2
                newAutomaton.addRule(rule);
                newHomomorphism.add(ruleLabel, homomorphism.get(ruleLabel));
            }
        }

        for (int state : automaton.getFinalStates()) {
            newAutomaton.addFinalState(state);
        }

        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(newAutomaton);
        Interpretation interpr = new Interpretation(interpretation.getAlgebra(), newHomomorphism);
        ret.addInterpretation(interpretationName, interpr);

        return ret;
    }

    private void binarizeRule(Homomorphism newHomomorphism, ConcreteTreeAutomaton newAutomaton, Tree<HomomorphismSymbol> oldHomomorphismSubtree, int parentNT, String label, Rule rule) {
        List<Integer> childStatesInNewRule = new ArrayList<Integer>();
        int varCounter = 0;
        List<Tree<HomomorphismSymbol>> childrenInOldHom = oldHomomorphismSubtree.getChildren();
        List<Tree<HomomorphismSymbol>> childrenInNewHom = new ArrayList<Tree<HomomorphismSymbol>>();

        for (int pos = 0; pos < childrenInOldHom.size(); pos++) {
            Tree<HomomorphismSymbol> arg = childrenInOldHom.get(pos);
            HomomorphismSymbol argLabel = arg.getLabel();

            if (argLabel.isVariable()) {
                int index = argLabel.getValue();
                int nonterminal = rule.getChildren()[index];
                childStatesInNewRule.add(nonterminal);

                varCounter++;
                HomomorphismSymbol var = HomomorphismSymbol.createVariable("?" + varCounter);
                childrenInNewHom.add(Tree.create(var));
            } else if (arg.getChildren().isEmpty()) {           // leaf
                childrenInNewHom.add(Tree.create(argLabel));
            } else {                                            // root of subtree is an operation
                String newTerminal = makeBinaryLabel(label, pos + 1);
                int newNonterminal = newAutomaton.addState(makeBinaryStateName(label, pos + 1));
                childStatesInNewRule.add(newNonterminal);

                varCounter++;
                HomomorphismSymbol var = HomomorphismSymbol.createVariable("?" + varCounter);
                childrenInNewHom.add(Tree.create(var));

                binarizeRule(newHomomorphism, newAutomaton, arg, newNonterminal, newTerminal, rule);
            }
        }


        Tree<HomomorphismSymbol> hForRule = Tree.create(oldHomomorphismSubtree.getLabel(), childrenInNewHom);
        Rule binRule = newAutomaton.createRule(parentNT, label, childStatesInNewRule);
        newAutomaton.addRule(binRule);
        newHomomorphism.add(newHomomorphism.getSourceSignature().getIdForSymbol(label), hForRule);
    }

    private String makeBinaryLabel(String prefix, int argNum) {
        String newSymbol = prefix + argNum;

        while (automaton.getSignature().contains(newSymbol)) {
            newSymbol = newSymbol + "b";
        }

        return newSymbol;
    }

    private String makeBinaryStateName(String prefix, int argNum) {
        String newSymbol = prefix + argNum;
        newSymbol = newSymbol.toUpperCase();

        while (automaton.getIdForState(newSymbol) != 0) {
            newSymbol = newSymbol + "B";
        }

        return newSymbol;
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
}
