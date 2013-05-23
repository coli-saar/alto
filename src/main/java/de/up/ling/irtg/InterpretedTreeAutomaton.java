/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg;

import de.up.ling.irtg.corpus.ChartCorpus;
import de.up.ling.irtg.corpus.AnnotatedCorpus;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.binarization.RegularBinarizer;
import de.up.ling.irtg.binarization.StringAlgebraBinarizer;
import de.up.ling.irtg.binarization.SynchronousBinarization;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {
    public static void main(String[] args) throws ParseException, FileNotFoundException, ParserException, IOException {
        String filename = args[0];
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(filename));
        irtg.getAutomaton().analyze();
        
        Map<String,Reader> inputs = new HashMap<String, Reader>();
        inputs.put("i", new StringReader("Pierre Vinken , 61 years old , will join the board as a nonexecutive director Nov. 29 ."));
        
        long start = System.currentTimeMillis();
        irtg.parseFromReaders(inputs);
        long end = System.currentTimeMillis();
        System.err.println("parsing took " + (end-start) + " ms");
    }

    protected TreeAutomaton<String> automaton;
    protected Map<String, Interpretation> interpretations;
    protected boolean debug = false;

    public InterpretedTreeAutomaton(TreeAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new HashMap<String, Interpretation>();
    }

    public void addInterpretation(String name, Interpretation interp) {
        interpretations.put(name, interp);
    }

    public void addAllInterpretations(Map<String, Interpretation> interps) {
        interpretations.putAll(interps);
    }

    @CallableFromShell(name = "automaton")
    public TreeAutomaton<String> getAutomaton() {
        return automaton;
    }

    public Map<String, Interpretation> getInterpretations() {
        return interpretations;
    }

    @CallableFromShell(name = "interpretation")
    public Interpretation getInterpretation(Reader reader) throws IOException {
        String interp = StringTools.slurp(reader);
        return interpretations.get(interp);
    }

    public Object parseString(String interpretation, String representation) throws ParserException {
        Object ret = getInterpretations().get(interpretation).getAlgebra().parseString(representation);
        return ret;
    }

    public Map<String, Object> parseStrings(Map<String, String> representations) throws ParserException {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (String interp : representations.keySet()) {
            ret.put(interp, parseString(interp, representations.get(interp)));
        }
        return ret;
    }

    @CallableFromShell(name = "parse")
    public TreeAutomaton parseFromReaders(Map<String, Reader> readers) throws ParserException, IOException {
        Map<String, Object> inputs = new HashMap<String, Object>();

        for (String interp : readers.keySet()) {
            String representation = StringTools.slurp(readers.get(interp));
            inputs.put(interp, parseString(interp, representation));
        }

        return parseInputObjects(inputs);
    }

    public TreeAutomaton parse(Map<String, Reader> readers) throws ParserException, IOException {
        return parseFromReaders(readers);
    }

    public TreeAutomaton parseInputObjects(Map<String, Object> inputs) {
        TreeAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);
            ret = ret.intersect(interp.parse(input));
        }

        return ret.reduceBottomUp();
    }

    @CallableFromShell(name = "decode", joinList = "\n")
    public Set<Object> decodeFromReaders(Reader outputInterpretation, Map<String, Reader> readers) throws ParserException, IOException {
        TreeAutomaton chart = parseFromReaders(readers);
        String interp = StringTools.slurp(outputInterpretation);
        return decode(chart, interpretations.get(interp));
    }

    public Set<Object> decode(Reader outputInterpretation, Map<String, Reader> readers) throws ParserException, IOException {
        return decodeFromReaders(outputInterpretation, readers);
    }

    private Set<Object> decode(TreeAutomaton chart, Interpretation interp) {
        TreeAutomaton<String> outputChart = chart.homomorphism(interp.getHomomorphism());
        Collection<Tree<String>> outputLanguage = outputChart.language();

        Set<Object> ret = new HashSet<Object>();
        for (Tree<String> term : outputLanguage) {
            ret.add(interp.getAlgebra().evaluate(term));
        }

        return ret;
    }

    @CallableFromShell(name = "decodeToTerms", joinList = "\n")
    public Set<Tree> decodeToTermsFromReaders(Reader outputInterpretation, Map<String, Reader> readers) throws ParserException, IOException {
        TreeAutomaton chart = parseFromReaders(readers);
        String interp = StringTools.slurp(outputInterpretation);
        return decodeToTerms(chart, interpretations.get(interp));
    }

    public Set<Tree> decodeToTerms(String outputInterpretation, Map<String, Object> inputs) {
        TreeAutomaton chart = parseInputObjects(inputs);
        return decodeToTerms(chart, interpretations.get(outputInterpretation));
    }

    private Set<Tree> decodeToTerms(TreeAutomaton chart, Interpretation interp) {
        TreeAutomaton<String> outputChart = chart.homomorphism(interp.getHomomorphism());
        Collection<Tree<String>> outputLanguage = outputChart.language();

        Set<Tree> ret = new HashSet<Tree>();
        for (Tree<String> term : outputLanguage) {
            ret.add(term);
        }

        return ret;
    }

    @CallableFromShell(name = "mltrain")
    public void trainML(AnnotatedCorpus trainingData) throws UnsupportedOperationException {
        final Map<String, Rule<String>> ruleForTerminal = new HashMap<String, Rule<String>>();
        final Map<String, Long> ruleCounts = new HashMap<String, Long>();
        final Map<String, Long> stateCounts = new HashMap<String, Long>();

        // initialize data
        for (Rule<String> rule : automaton.getRuleSet()) {
            if (ruleForTerminal.containsKey(rule.getLabel())) {
                throw new UnsupportedOperationException("ML training only supported if no two rules use the same terminal symbol.");
            }

            ruleForTerminal.put(rule.getLabel(), rule);
            ruleCounts.put(rule.getLabel(), 0L);
            stateCounts.put(rule.getParent(), 0L);
        }

        // compute absolute frequencies on annotated corpus
        for (AnnotatedCorpus.Instance instance : trainingData.getInstances()) {
            instance.tree.dfs(new TreeVisitor<String, Void, Void>() {
                @Override
                public Void visit(Tree<String> node, Void data) {
                    Rule<String> rule = ruleForTerminal.get(node.getLabel());

                    ruleCounts.put(node.getLabel(), ruleCounts.get(node.getLabel()) + 1);
                    stateCounts.put(rule.getParent(), stateCounts.get(rule.getParent()) + 1);

                    return null;
                }
            });
        }

        // set all rule weights according to counts
        for (String label : ruleForTerminal.keySet()) {
            Rule<String> rule = ruleForTerminal.get(label);
            long stateCount = stateCounts.get(rule.getParent());

            if (stateCount == 0) {
                rule.setWeight(0.0);
            } else {
                rule.setWeight(((double) ruleCounts.get(label)) / stateCount);
            }
        }
    }

    @CallableFromShell(name = "emtrain")
    public void trainEM(ChartCorpus trainingData) {
        if (debug) {
            System.out.println("\n\nInitial model:\n" + automaton);
        }

        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        ListMultimap<Rule, Rule> originalRuleToIntersectedRules = ArrayListMultimap.create();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, originalRuleToIntersectedRules);


        for (int iteration = 0; iteration < 10; iteration++) {
            Map<Rule<String>, Double> globalRuleCount = estep(parses, intersectedRuleToOriginalRule);

            // sum over rules with same parent state to obtain state counts
            Map<String, Double> globalStateCount = new HashMap<String, Double>();
            for (String state : automaton.getAllStates()) {
                globalStateCount.put(state, 0.0);
            }
            for (Rule<String> rule : automaton.getRuleSet()) {
                String state = rule.getParent();
                globalStateCount.put(state, globalStateCount.get(state) + globalRuleCount.get(rule));
            }

            // M-step
            for (Rule<String> rule : automaton.getRuleSet()) {
                double newWeight = globalRuleCount.get(rule) / globalStateCount.get(rule.getParent());

                rule.setWeight(newWeight);
                for (Rule intersectedRule : originalRuleToIntersectedRules.get(rule)) {
                    intersectedRule.setWeight(newWeight);
                }
            }

            if (debug) {
                System.out.println("\n\nAfter iteration " + (iteration + 1) + ":\n" + automaton);
            }
        }
    }

    /**
     * Performs Variational Bayes training of the IRTG, given a corpus of
     * charts. This implements the algorithm from Jones et al., "Semantic
     * Parsing with Bayesian Tree Transducers", ACL 2012.
     *
     * @param trainingData a corpus of parse charts
     */
    @CallableFromShell(name = "vbtrain")
    public void trainVB(ChartCorpus trainingData) {
        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        collectParsesAndRules(trainingData, parses, intersectedRuleToOriginalRule, null);

        // initialize hyperparameters
        List<Rule<String>> automatonRules = new ArrayList<Rule<String>>(getAutomaton().getRuleSet()); // bring rules in defined order
        int numRules = automatonRules.size();
        double[] alpha = new double[numRules];
        Arrays.fill(alpha, 1.0); // might want to initialize them differently

        // iterate
        for (int iteration = 0; iteration < 10; iteration++) {
            // for each state, compute sum of alphas for outgoing rules
            Map<String, Double> sumAlphaForSameParent = new HashMap<String, Double>();
            for (int i = 0; i < numRules; i++) {
                String parent = automatonRules.get(i).getParent();
                if (sumAlphaForSameParent.containsKey(parent)) {
                    sumAlphaForSameParent.put(parent, sumAlphaForSameParent.get(parent) + alpha[i]);
                } else {
                    sumAlphaForSameParent.put(parent, alpha[i]);
                }
            }

            // re-estimate rule weights
            for (int i = 0; i < numRules; i++) {
                Rule<String> rule = automatonRules.get(i);
                rule.setWeight(Math.exp(Gamma.digamma(alpha[i]) - Gamma.digamma(sumAlphaForSameParent.get(rule.getParent()))));
            }

            // re-estimate hyperparameters
            Map<Rule<String>, Double> ruleCounts = estep(parses, intersectedRuleToOriginalRule);
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
    protected Map<Rule<String>, Double> estep(List<TreeAutomaton> parses, List<Map<Rule, Rule>> intersectedRuleToOriginalRule) {
        Map<Rule<String>, Double> globalRuleCount = new HashMap<Rule<String>, Double>();
        for (Rule<String> rule : automaton.getRuleSet()) {
            globalRuleCount.put(rule, 0.0);
        }
        
        for (int i = 0; i < parses.size(); i++) {
            TreeAutomaton parse = parses.get(i);

            Map<Object, Double> inside = parse.inside();
            Map<Object, Double> outside = parse.outside(inside);

            for (Rule intersectedRule : intersectedRuleToOriginalRule.get(i).keySet()) {
                Object intersectedParent = intersectedRule.getParent();
                Rule<String> originalRule = intersectedRuleToOriginalRule.get(i).get(intersectedRule);
                
                double oldRuleCount = globalRuleCount.get(originalRule);
                double thisRuleCount = outside.get(intersectedParent) * intersectedRule.getWeight();

                for (int j = 0; j < intersectedRule.getArity(); j++) {
                    thisRuleCount *= inside.get(intersectedRule.getChildren()[j]);
                }

                globalRuleCount.put(originalRule, oldRuleCount + thisRuleCount);
            }
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
    private void collectParsesAndRules(ChartCorpus trainingData, List<TreeAutomaton> parses, List<Map<Rule, Rule>> intersectedRuleToOriginalRule, ListMultimap<Rule, Rule> originalRuleToIntersectedRules) {
        parses.clear();
        intersectedRuleToOriginalRule.clear();

        if (originalRuleToIntersectedRules != null) {
            originalRuleToIntersectedRules.clear();
        }

        for (TreeAutomaton parse : trainingData ) {
            parses.add(parse);

            Set<Rule> rules = parse.getRuleSet();
            Map<Rule, Rule> irtorHere = new HashMap<Rule, Rule>();
            for (Rule intersectedRule : rules) {
                Rule<String> originalRule = getRuleInGrammar(intersectedRule);

                irtorHere.put(intersectedRule, originalRule);

                if (originalRuleToIntersectedRules != null) {
                    originalRuleToIntersectedRules.put(originalRule, intersectedRule);
                }
            }

            intersectedRuleToOriginalRule.add(irtorHere);
        }
    }

    // safe but inefficient
    Rule<String> getRuleInGrammar(Rule intersectedRule) {
        List<String> firstChildStates = new ArrayList<String>();
        String firstParentState = (String) getFirstEntry(intersectedRule.getParent());
        for (Object pairState : intersectedRule.getChildren()) {
            String firstState = (String) getFirstEntry(pairState);
            firstChildStates.add(firstState);
        }
        
        for (Rule<String> candidate : automaton.getRulesBottomUp(intersectedRule.getLabel(), firstChildStates)) {
            if (firstParentState.equals(candidate.getParent())) {
                return candidate;
            }
        }

        return null;
    }

    private static Object getFirstEntry(Object pairState) {
        if (pairState instanceof Pair) {
            return getFirstEntry(((Pair) pairState).left);
        } else {
            return pairState;
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    
    /**
     * BUG: This can't be expected to work right now, because state names have to be
     * remapped to be identical (and not just equals) to the state names in the IRTG automaton.
     * 
     * @param reader
     * @return
     * @throws IOException 
     */
    @CallableFromShell
    public ChartCorpus readChartCorpus(Reader reader) throws IOException {
        return new ChartCorpus(new File(StringTools.slurp(reader)));
    }
    
    public ChartCorpus readChartCorpus(Supplier<InputStream> istream) {
        return new ChartCorpus(istream);
    }
    
    /**
     * This assumes that the corpus text file can be obtained from the reader.
     */
    @CallableFromShell
    public void parseUnannotatedCorpus(Reader unannotatedCorpus, OutputStream ostream) throws IOException {
        ChartCorpus.parseCorpus(unannotatedCorpus, this, ostream);
    }
    
    @CallableFromShell
    public AnnotatedCorpus readAnnotatedCorpus(Reader reader) throws IOException {
        return AnnotatedCorpus.readAnnotatedCorpus(reader, this);
    }
    
    /**
     * BUG: This no longer works with the current version of ChartCorpus.
     * 
     * @param corpus 
     */
    /*
    public void normalizeStates(ChartCorpus corpus) {
        for( TreeAutomaton chart : corpus ) {
            Set<Rule> rules = chart.getRuleSet();
            for( Rule rule : rules ) {
                normalizeState(rule.getParent());
                for( Object child : rule.getChildren() ) {
                    normalizeState(child);
                }
            }
        }
    }
    
    private void normalizeState(Object state) {
        if( state instanceof Pair ) {
            Pair pairState = (Pair) state;
            if( ! (pairState.left instanceof Pair) ) {
                pairState.left = automaton.normalizeState(pairState.left.toString());
            }
        }
    }

*/
    
    public InterpretedTreeAutomaton binarize(Map<String, RegularBinarizer> binarizers) {
        List<String> orderedInterpretationList = new ArrayList<String>(interpretations.keySet());
        if (orderedInterpretationList.size() != 2) {
            throw new UnsupportedOperationException("trying to binarize " + orderedInterpretationList.size() + " interpretations");
        }

        String interpName1 = orderedInterpretationList.get(0);
        String interpName2 = orderedInterpretationList.get(1);
        Interpretation interp1 = interpretations.get(interpName1);
        Interpretation interp2 = interpretations.get(interpName2);
        RegularBinarizer bin1 = binarizers.get(interpName1);
        RegularBinarizer bin2 = binarizers.get(interpName2);

        // select a constant as dummy symbol from both algebras 
        HomomorphismSymbol constantL = getConstantFromAlgebra(interp1.getHomomorphism());
        HomomorphismSymbol constantR = getConstantFromAlgebra(interp2.getHomomorphism());

        SynchronousBinarization sb = new SynchronousBinarization(constantL, constantR);
        ConcreteTreeAutomaton newAuto = new ConcreteTreeAutomaton();
        Homomorphism newLeftHom = new Homomorphism(newAuto.getSignature(), interp1.getHomomorphism().getTargetSignature());
        Homomorphism newRightHom = new Homomorphism(newAuto.getSignature(), interp2.getHomomorphism().getTargetSignature());

        for (Rule rule : automaton.getRuleSet()) {
            TreeAutomaton leftAutomaton = bin1.binarizeWithVariables(interp1.getHomomorphism().get(rule.getLabel()));
            TreeAutomaton rightAutomaton = bin2.binarizeWithVariables(interp2.getHomomorphism().get(rule.getLabel()));
            sb.binarize(rule, leftAutomaton, rightAutomaton, newAuto, newLeftHom, newRightHom);
        }
        for (String state : automaton.getFinalStates()) {
            newAuto.addFinalState(state);
        }
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(newAuto);
        ret.addInterpretation(interpName1, new Interpretation(bin1.getOutputAlgebra(), newLeftHom));
        ret.addInterpretation(interpName2, new Interpretation(bin2.getOutputAlgebra(), newRightHom));

        return ret;
    }

    private HomomorphismSymbol getConstantFromAlgebra(Homomorphism hom) {
        for (String label : hom.getDomain()) {
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

    @CallableFromShell
    public InterpretedTreeAutomaton testBinarize() {
        RegularBinarizer bin1 = new StringAlgebraBinarizer();
        RegularBinarizer bin2 = new StringAlgebraBinarizer();
        Map<String, RegularBinarizer> binarizers = new HashMap<String, RegularBinarizer>();
        binarizers.put("i1", bin1);
        binarizers.put("i2", bin2);
        InterpretedTreeAutomaton newAuto = binarize(binarizers);
        return newAuto;
    }

    @CallableFromShell
    public InterpretedTreeAutomaton binarize() {
        /*
         if (interpretations.keySet().size() > 1) {
         throw new UnsupportedOperationException("Can only binarize IRTGs with a single interpretation.");
         }
         * 
         */

        if (interpretations.keySet().isEmpty()) {
            throw new UnsupportedOperationException("Trying to binarize IRTG without interpretation.");
        }

        // pick the alphabetically first interpretation for the binarization
        List<String> names = new ArrayList<String>(interpretations.keySet());
        Collections.sort(names);

        String interpretationName = names.get(0);
        Interpretation interpretation = interpretations.get(interpretationName);
        Homomorphism homomorphism = interpretation.getHomomorphism();

        ConcreteTreeAutomaton newAutomaton = new ConcreteTreeAutomaton();
        Homomorphism newHomomorphism = new Homomorphism(newAutomaton.getSignature(), homomorphism.getTargetSignature());
        Set<Rule<String>> rules = automaton.getRuleSet();


        for (Rule<String> rule : rules) {
            String ruleLabel = rule.getLabel();

            if (rule.getArity() > 2) {
                Tree<HomomorphismSymbol> hRule = homomorphism.get(ruleLabel);
                String parent = rule.getParent();
                String label = makeBinaryLabel(ruleLabel + "-b", 0, true);

                Set<Rule<String>> newRules = new HashSet<Rule<String>>();
                binarizeRule(newHomomorphism, newRules, hRule, parent, label, rule);
                for (Rule<String> r : newRules) {
                    newAutomaton.addRule(r);
                }
            } else { // rules of arity <= 2
                newAutomaton.addRule(rule);
                newHomomorphism.add(ruleLabel, homomorphism.get(ruleLabel));
            }
        }

        for (String state : automaton.getFinalStates()) {
            newAutomaton.addFinalState(state);
        }
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(newAutomaton);
        Interpretation interpr = new Interpretation(interpretation.getAlgebra(), newHomomorphism);
        ret.addInterpretation(interpretationName, interpr);

        return ret;
    }

    private void binarizeRule(Homomorphism newHomomorphism, Set<Rule<String>> newRules, Tree<HomomorphismSymbol> oldHomomorphismSubtree, String parentNT, String label, Rule rule) {
        List<String> childStatesInNewRule = new ArrayList<String>();
        int varCounter = 0;
        List<Tree<HomomorphismSymbol>> childrenInOldHom = oldHomomorphismSubtree.getChildren();
        List<Tree<HomomorphismSymbol>> childrenInNewHom = new ArrayList<Tree<HomomorphismSymbol>>();

        for (int pos = 0; pos < childrenInOldHom.size(); pos++) {
            Tree<HomomorphismSymbol> arg = childrenInOldHom.get(pos);
            HomomorphismSymbol argLabel = arg.getLabel();

            if (argLabel.isVariable()) {
                int index = argLabel.getIndex();
                String nonterminal = (String) rule.getChildren()[index];
                childStatesInNewRule.add(nonterminal);

                varCounter++;
                HomomorphismSymbol var = HomomorphismSymbol.createVariable("?" + varCounter);
                childrenInNewHom.add(Tree.create(var));
            } else if (arg.getChildren().isEmpty()) {           // leaf
                childrenInNewHom.add(Tree.create(argLabel));
            } else {                                            // root of subtree is an operation
                String newTerminal = makeBinaryLabel(label, pos + 1, true);
                String newNonterminal = makeBinaryLabel(label, pos + 1, false);
                childStatesInNewRule.add(newNonterminal);

                varCounter++;
                HomomorphismSymbol var = HomomorphismSymbol.createVariable("?" + varCounter);
                childrenInNewHom.add(Tree.create(var));

                binarizeRule(newHomomorphism, newRules, arg, newNonterminal, newTerminal, rule);
            }
        }


        Tree<HomomorphismSymbol> hForRule = Tree.create(oldHomomorphismSubtree.getLabel(), childrenInNewHom);
        Rule<String> binRule = new Rule(parentNT, label, childStatesInNewRule);
        newRules.add(binRule);
        newHomomorphism.add(label, hForRule);
    }

    private String makeBinaryLabel(String prefix, int argNum, boolean terminal) {
        String newSymbol = prefix + argNum;
        if (terminal) {
            while (automaton.getSignature().contains(newSymbol)) {
                newSymbol = newSymbol + "b";
            }
        } else {
            newSymbol = newSymbol.toUpperCase();
            while (automaton.getAllStates().contains(newSymbol)) {
                newSymbol = newSymbol + "B";
            }
        }
        return newSymbol;
    }

    @Override
    public String toString() {
        StringWriter buf = new StringWriter();
        PrintWriter pw = new PrintWriter(buf);
        List<String> interpretationOrder = new ArrayList<String>(interpretations.keySet());

        for (String interp : interpretationOrder) {
            pw.println("interpretation " + interp + ": " + interpretations.get(interp).getAlgebra().getClass().getName());
        }

        pw.println();

        for (Rule<String> rule : automaton.getRuleSet()) {
//            String isFinal = automaton.getFinalStates().contains(rule.getParent()) ? "!" : "";
//            String children = (rule.getArity() == 0 ? "" : "(" + StringTools.join(rule.getChildren(), ", ") + ")");
            pw.println(rule.toString(automaton.getFinalStates().contains(rule.getParent())));

            for (String interp : interpretationOrder) {
                Homomorphism hom = interpretations.get(interp).getHomomorphism();
                Tree<HomomorphismSymbol> rhs = hom.get(rule.getLabel());
                pw.println("  [" + interp + "] " + Homomorphism.rhsAsString(rhs));
            }

            pw.println();
        }

        return buf.toString();
    }

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
            return false;
        }
        if (this.interpretations != other.interpretations && (this.interpretations == null || !this.interpretations.equals(other.interpretations))) {
            return false;
        }
        return true;
    }
}
