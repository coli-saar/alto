/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.saar.penguin.irtg.algebra.ParseException;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.up.ling.shell.CallableFromShell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class InterpretedTreeAutomaton {
    private BottomUpAutomaton<String> automaton;
    private Map<String, Interpretation> interpretations;
    private boolean debug = true;

    public InterpretedTreeAutomaton(BottomUpAutomaton<String> automaton) {
        this.automaton = automaton;
        interpretations = new HashMap<String, Interpretation>();
    }

    public void addInterpretation(String name, Interpretation interp) {
        interpretations.put(name, interp);
    }

    public BottomUpAutomaton<String> getAutomaton() {
        return automaton;
    }

    public Map<String, Interpretation> getInterpretations() {
        return interpretations;
    }

    public Object parseString(String interpretation, String representation) throws ParseException {
        Object ret = getInterpretations().get(interpretation).getAlgebra().parseString(representation);
        return ret;
    }

    public Map<String, Object> parseStrings(Map<String, String> representations) throws ParseException {
        Map<String, Object> ret = new HashMap<String, Object>();
        for (String interp : representations.keySet()) {
            ret.put(interp, parseString(interp, representations.get(interp)));
        }
        return ret;
    }

    @CallableFromShell(name = "parse")
    public BottomUpAutomaton parseFromReaders(Map<String, Reader> readers) throws ParseException, IOException {
        Map<String, Object> inputs = new HashMap<String, Object>();

        for (String interp : readers.keySet()) {
            String representation = StringTools.slurp(readers.get(interp));
            inputs.put(interp, parseString(interp, representation));
        }

        return parse(inputs);
    }

    public BottomUpAutomaton parse(Map<String, Object> inputs) {
        BottomUpAutomaton ret = automaton;

        for (String interpName : inputs.keySet()) {
            Interpretation interp = interpretations.get(interpName);
            Object input = inputs.get(interpName);
            ret = ret.intersect(interp.parse(input));
        }

        return ret;
    }

    @CallableFromShell(name = "emtrain")
    public void trainEM(Reader reader) throws IOException {
        List<Map<String, Object>> data = readTrainingData(reader);
        trainEM(data);
    }

    public void trainEM(List<Map<String, Object>> trainingData) {
        if (debug) {
            System.out.println("\n\nInitial model:\n" + automaton);
        }

        for (int iteration = 0; iteration < 10; iteration++) {
            // initialize counts
            Map<Rule<String>, Double> globalRuleCount = new HashMap<Rule<String>, Double>();
            for (Rule<String> rule : automaton.getRuleSet()) {
                globalRuleCount.put(rule, 0.0);
            }

            // E-step
            for (Map<String, Object> tuple : trainingData) {
                BottomUpAutomaton parse = parse(tuple);
                parse = parse.reduceBottomUp();
                Map<Object, Double> inside = parse.inside();
                Map<Object, Double> outside = parse.outside(inside);
                Set<Rule> rules = parse.getRuleSet();

                for (Rule intersectedRule : rules) {
                    Object intersectedParent = intersectedRule.getParent();
                    Rule<String> originalRule = getRuleInGrammar(intersectedRule);
                    double oldRuleCount = globalRuleCount.get(originalRule);
                    double thisRuleCount = outside.get(intersectedParent) * intersectedRule.getWeight();

                    for (int i = 0; i < intersectedRule.getArity(); i++) {
                        thisRuleCount *= inside.get(intersectedRule.getChildren()[i]);
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
                rule.setWeight(globalRuleCount.get(rule) / globalStateCount.get(rule.getParent()));
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

    private List<Map<String, Object>> readTrainingData(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentTuple = new HashMap<String, Object>();
        int currentInterpretation = 0;
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
                String current = interpretationOrder.get(currentInterpretation);
                try {
//                    System.err.println("")
                    currentTuple.put(current, parseString(current, line));
                } catch (de.saar.penguin.irtg.algebra.ParseException ex) {
                    System.out.println("An error occurred while parsing " + reader + ", line " + (lineNumber + 1) + ": " + ex.getMessage());
                    return null;
                }

                currentInterpretation++;
                if (currentInterpretation >= interpretationOrder.size()) {
                    ret.add(currentTuple);
                    currentTuple = new HashMap<String, Object>();
                    currentInterpretation = 0;
                }
            }

            lineNumber++;
        }
    }
}
