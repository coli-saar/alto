/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.saar.basic.Pair;
import de.saar.basic.StringOrVariable;
import de.saar.basic.StringTools;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {

    private TreeAutomaton<String> automaton;
    private Map<String, Interpretation> interpretations;
    private boolean debug = false;

    public InterpretedTreeAutomaton(TreeAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new HashMap<String, Interpretation>();
    }

    public void addInterpretation(String name, Interpretation interp) {
        interpretations.put(name, interp);
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

    TreeAutomaton parseInputObjects(Map<String, Object> inputs) {
        TreeAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);
            ret = ret.intersect(interp.parse(input));
        }

        return ret;
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

    /*
    public Set<Object> decode(String outputInterpretation, Map<String, Object> inputs) {
    TreeAutomaton chart = parseInputObjects(inputs);
    return decode(chart, interpretations.get(outputInterpretation));
    }
     * 
     */
    private Set<Object> decode(TreeAutomaton chart, Interpretation interp) {
        TreeAutomaton<String> outputChart = chart.homomorphism(interp.getHomomorphism());
        Set<Tree<String>> outputLanguage = outputChart.language();

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
        Set<Tree<String>> outputLanguage = outputChart.language();

        Set<Tree> ret = new HashSet<Tree>();
        for (Tree<String> term : outputLanguage) {
            ret.add(term);
        }

        return ret;
    }

    @CallableFromShell(name = "emtrain")
    public void trainEM(ParsedCorpus trainingData) {
        if (debug) {
            System.out.println("\n\nInitial model:\n" + automaton);
        }

        // memorize mapping between
        // rules of the parse charts and rules of the underlying RTG
        List<TreeAutomaton> parses = new ArrayList<TreeAutomaton>();
        List<Map<Rule, Rule>> intersectedRuleToOriginalRule = new ArrayList<Map<Rule, Rule>>();
        ListMultimap<Rule, Rule> originalRuleToIntersectedRules = ArrayListMultimap.create();

        for (TreeAutomaton parse : trainingData.getAllInstances()) {
            parses.add(parse);

            Set<Rule> rules = parse.getRuleSet();
            Map<Rule, Rule> irtorHere = new HashMap<Rule, Rule>();
            for (Rule intersectedRule : rules) {
                Object intersectedParent = intersectedRule.getParent();
                Rule<String> originalRule = getRuleInGrammar(intersectedRule);

                irtorHere.put(intersectedRule, originalRule);
                originalRuleToIntersectedRules.put(originalRule, intersectedRule);
            }

            intersectedRuleToOriginalRule.add(irtorHere);
        }

        for (int iteration = 0; iteration < 10; iteration++) {
            // initialize counts
            Map<Rule<String>, Double> globalRuleCount = new HashMap<Rule<String>, Double>();
            for (Rule<String> rule : automaton.getRuleSet()) {
                globalRuleCount.put(rule, 0.0);
            }

            // E-step
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

    // safe but inefficient
    private Rule<String> getRuleInGrammar(Rule intersectedRule) {
        List<String> firstChildStates = new ArrayList<String>();
        String firstParentState = (String) getFirstEntry(intersectedRule.getParent());
        for (Object pairState : intersectedRule.getChildren()) {
            firstChildStates.add((String) getFirstEntry(pairState));
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

    @CallableFromShell
    public ParsedCorpus parseCorpus(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        ParsedCorpus ret = new ParsedCorpus();
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        int currentInterpretationIndex = 0;
        int lineNumber = 0;

        while (true) {
            String line = br.readLine();

            if (line == null) {
                return ret;
            }

            if (line.equals("")) {
                continue;
            }

            if (lineNumber < getInterpretations().size()) {
                interpretationOrder.add(line);
            } else {
                String current = interpretationOrder.get(currentInterpretationIndex);
                Interpretation currentInterpretation = interpretations.get(current);

                try {
                    Object inputObject = parseString(current, line);
                    currentInputs.put(current, inputObject);
                } catch (ParserException ex) {
                    System.out.println("An error occurred while parsing " + reader + ", line " + (lineNumber + 1) + ": " + ex.getMessage());
                    return null;
                }

                currentInterpretationIndex++;
                if (currentInterpretationIndex >= interpretationOrder.size()) {
                    TreeAutomaton chart = parseInputObjects(currentInputs).reduceBottomUp().makeConcreteAutomaton();
                    ret.addInstance(chart);

                    currentInputs.clear();
                    currentInterpretationIndex = 0;
                }
            }

            lineNumber++;
        }
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

        ConcreteTreeAutomaton newAutomaton = new ConcreteTreeAutomaton();
        Homomorphism newHomomorphism = new Homomorphism();
        Set<Rule<String>> rules = automaton.getRuleSet();
        
        // pick the alphabetically first interpretation for the binarization
        List<String> names = new ArrayList<String>(interpretations.keySet());
        Collections.sort(names);
        
        String interpretationName = names.get(0);
        Interpretation interpretation = interpretations.get(interpretationName);
        Homomorphism homomorphism = interpretation.getHomomorphism();

        for (Rule<String> rule : rules) {
            String ruleLabel = rule.getLabel();

            if (rule.getArity() > 2) {
                Tree<StringOrVariable> hRule = homomorphism.get(ruleLabel);
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
    
    private void binarizeRule(Homomorphism newHomomorphism, Set<Rule<String>> newRules, Tree<StringOrVariable> oldHomomorphismSubtree, String parentNT, String label, Rule rule) {
        List<String> childStatesInNewRule = new ArrayList<String>();
        int varCounter = 0;
        List<Tree<StringOrVariable>> childrenInOldHom = oldHomomorphismSubtree.getChildren();
        List<Tree<StringOrVariable>> childrenInNewHom = new ArrayList<Tree<StringOrVariable>>();
        
        for (int pos = 0; pos < childrenInOldHom.size(); pos++) {
            Tree<StringOrVariable> arg = childrenInOldHom.get(pos);
            StringOrVariable argLabel = arg.getLabel();

            if (argLabel.isVariable()) {
                int index = Homomorphism.getIndexForVariable(argLabel);
                String nonterminal = (String) rule.getChildren()[index];
                childStatesInNewRule.add(nonterminal);

                varCounter++;
                StringOrVariable var = new StringOrVariable("?" + varCounter, true);
                childrenInNewHom.add(Tree.create(var));
            } else if (arg.getChildren().isEmpty()) {           // leaf
                childrenInNewHom.add(Tree.create(argLabel));
            } else {                                            // root of subtree is an operation
                String newTerminal = makeBinaryLabel(label, pos+1, true);
                String newNonterminal = makeBinaryLabel(label, pos+1, false);
                childStatesInNewRule.add(newNonterminal);

                varCounter++;
                StringOrVariable var = new StringOrVariable("?" + varCounter, true);
                childrenInNewHom.add(Tree.create(var));

                binarizeRule(newHomomorphism, newRules, arg, newNonterminal, newTerminal, rule);
            }
        }
        
        
        Tree<StringOrVariable> hForRule = Tree.create(oldHomomorphismSubtree.getLabel(), childrenInNewHom);
        Rule<String> binRule = new Rule(parentNT, label, childStatesInNewRule);
        newRules.add(binRule);
        newHomomorphism.add(label, hForRule);
    }

    private String makeBinaryLabel(String prefix, int argNum, boolean terminal) {
        String newSymbol = prefix + argNum;
        if (terminal) {
            while (automaton.getAllLabels().contains(newSymbol)) {
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
            String isFinal = automaton.getFinalStates().contains(rule.getParent()) ? "!" : "";
            String children = (rule.getArity() == 0 ? "" : "(" + StringTools.join(rule.getChildren(), ", ") + ")");
            pw.println(rule.getLabel() + children + " -> " + rule.getParent().toString() + isFinal + " [" + rule.getWeight() + "]");

            for (String interp : interpretationOrder) {
                pw.println("  [" + interp + "] " + interpretations.get(interp).getHomomorphism().get(rule.getLabel()));
            }

            pw.println();
        }

        return buf.toString();
    }
}
