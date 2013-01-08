/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.collect.SetMultimap;
import de.saar.basic.AkSetMultimap;
import de.saar.basic.StringOrVariable;
import de.up.ling.irtg.algebra.PtbTreeAlgebra;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author danilo
 */
public class ChartAutomaton extends ConcreteTreeAutomaton<de.saar.basic.Pair<String, String>> {
    private static Map<String, Set<String>> terminalRules;
    private static Map<String, Rule<String>> ruleMapping;
    private static SetMultimap<String, Rule<String>> rulesFor1stChildState;
    private static boolean cachedRuleMapping = false;
    private static TreeAutomaton<String> automaton;
    private Object[] indexedStateRules;
    private Set<String> ruleCheck;

    public ChartAutomaton() {
        super();
        ruleCheck = new HashSet<String>();
    }
    
    public static void init(Homomorphism hom, TreeAutomaton<String> auto) {
        automaton = auto;
        automaton.processNewRulesForRhs();
        terminalRules = new HashMap<String, Set<String>>();
        Set<Entry<String, Tree<StringOrVariable>>> homMapping = hom.getMappings().entrySet();
        for (Entry<String, Tree<StringOrVariable>> entry : homMapping) {
            Tree<StringOrVariable> tree = entry.getValue();
            if (tree.getChildren().isEmpty()) {
                String ruleName = entry.getKey();
                String label = tree.getLabel().getValue();
                Set<String> tRules = terminalRules.get(label);
                if (tRules == null) {
                    tRules = new HashSet<String>();
                }
                tRules.add(ruleName);
                terminalRules.put(label, tRules);
            }
        }
    }

    public static ChartAutomaton create(List<String> input) {
        Object[] bottomRules = new Object[input.size()];
        int i = 0;
        for (String terminal : input) {
            Set<String> ruleNames = terminalRules.get(terminal);
            bottomRules[i++] = ruleNames;
        }

        ChartAutomaton ret = new ChartAutomaton();
        ret.build(bottomRules);
        return ret;
    }
    
    public void build(Object[] bottom) {
        if (!cachedRuleMapping) {
            ruleMapping = new HashMap<String, Rule<String>>();
            Set<Rule<String>> ruleSet = automaton.getRuleSet();
            for (Rule<String> r : ruleSet) {
                ruleMapping.put(r.getLabel(), r);
            }
            rulesFor1stChildState = new AkSetMultimap<String, Rule<String>>();
            Set<String> keySet = automaton.rulesForRhsState.keySet();
            for (String state : keySet) {
                Set<Rule<String>> rules = automaton.rulesForRhsState.get(state);
                for (Rule<String> rule : rules) {
                    Object[] children = rule.getChildren();
                    if (((String)children[0]).equals(state)) {
                        rulesFor1stChildState.put(state, rule);
                    }
                }
            }
            cachedRuleMapping = true;
        }
        indexedStateRules = new Object[bottom.length];
        for (int i = bottom.length-1; i >= 0; i--) {
            indexedStateRules[i] = new ArrayList<Rule<State>>();
            Set<String> bottomRules = (Set<String>) bottom[i];
            for (String bRule : bottomRules) {
                Rule<State> rule = addStateRule(bRule, i, i+1);
                if (rule != null) {
                    State entry = (State) rule.getParent();
                    goUp(entry);
                }
            }
        }
    }
    
    private Rule<State> addStateRule(String ruleName, int start, int end) {
        Rule<String> rule = ruleMapping.get(ruleName);
        return addStateRule(rule, start, end);
    }
    
    private Rule<State> addStateRule(Rule<String> rule, int start, int end) {
        return addStateRule(rule, new ArrayList(), start, end);
    }
    
    private Rule<State> addStateRule(Rule<String> rule, List<State> children) {
        if (children.isEmpty()) {
            System.err.println("Warning: Unexpected number of child nodes.");
            return null;
        }
        int start = children.get(0).start;
        int end = children.get(children.size()-1).end;
        return addStateRule(rule, children, start, end);
    }
    
    private Rule<State> addStateRule(Rule<String> rule, List<State> children, int start, int end) {
        State parent = new State(rule.getParent(), start, end);
        Rule<State> newRule = new Rule<State>(parent, rule.getLabel(), children, 1.0);
        if (!hasStateRule(newRule)) {
            ((List)indexedStateRules[start]).add(newRule);
            addRule(stateRule2PairRule(newRule));
            if ((start == 0) && (end == indexedStateRules.length) && automaton.getFinalStates().contains(parent.symbol)) {
                addFinalState(parent.toPair());
            }
            if (!rule.getParent().startsWith(PtbTreeAlgebra.LABEL_PREFIX)) {
                return newRule;
            }
        }
        return null;
    }
    
    private boolean hasStateRule(Rule<State> a) {
        StringBuilder ret = new StringBuilder();
        ret.append(a.getParent().toString());
        ret.append(a.getLabel());
        Object[] children = a.getChildren();
        for (Object obj : children) {
            State s = (State) obj;
            ret.append(s.toString());
        }
        return !ruleCheck.add(ret.toString());
    }
    
    private void goUp(State state) {
        Set<Rule<String>> rulesForState = rulesFor1stChildState.get(state.symbol);
        if (rulesForState != null) {
            List<Rule<State>> parentRules = computeParents(state, rulesForState);
            for (Rule<State> r : parentRules) {
                goUp((State)r.getParent());
            }
        }
    }
    
    private List<Rule<State>> computeParents(State leftState, Set<Rule<String>> rules) {
        List<Rule<State>> stateRules = new ArrayList<Rule<State>>();
        for (Rule<String> r : rules) {
            Object[] children = r.getChildren();
            String leftChild = (String) children[0];
            if (leftChild.equals(leftState.symbol)) {
                List<State> newChildren = new ArrayList<State>();
                newChildren.add(leftState);
                if (children.length == 1) {
                    Rule<State> parent = addStateRule(r, newChildren);
                    if (parent != null) {
                        stateRules.add(parent);
                    }
                } else {
                    List<Rule<State>> parents = computeStateRule(r, newChildren, (String) children[1]);
                    if (!parents.isEmpty()) {
                        stateRules.addAll(parents);
                    }
                }
            }
        }
        return stateRules;
    }

    private List<Rule<State>> computeStateRule(Rule<String> parent, List<State> children, String rightStateName) {
        List<Rule<State>> ret = new ArrayList<Rule<State>>();
        int start = children.get(0).end;
        if (start < indexedStateRules.length) {
            Object[] stateRules = ((List) indexedStateRules[start]).toArray();
            for (int j = 0; j < stateRules.length; j++) {
                Rule<State> rule = (Rule<State>) stateRules[j];
                State rightState = rule.getParent();
                if (rightStateName.equals(rightState.symbol)) {
                    children.add(rightState);
                    Rule<State> stateRule = addStateRule(parent, children);
                    if (stateRule != null) {
                        ret.add(stateRule);
                    }
                    children.remove(1);
                }
            }
        }
        return ret;
    }
    
    public Rule<de.saar.basic.Pair<String, String>> stateRule2PairRule(Rule<State> stateRule) {
        State stateParent = stateRule.getParent();
        de.saar.basic.Pair<String, String> parent = stateParent.toPair();
        Object[] stateChildren = stateRule.getChildren();
        List<de.saar.basic.Pair<String, String>> children = new ArrayList<de.saar.basic.Pair<String, String>>();
        for (int i = 0; i < stateChildren.length; i++) {
            State child = (State) stateChildren[i];
            children.add(child.toPair());
        }
        return new Rule<de.saar.basic.Pair<String, String>>(parent, stateRule.getLabel(), children);
    }
            
    public class State {
        private String symbol;
        private int start;
        private int end;
        private String cachedString;
        private de.saar.basic.Pair<String, String> cachedPair;
        
        public State(String symbol, int start, int end) {
            this.symbol = symbol;
            this.start = start;
            this.end = end;

            String ret = String.valueOf(start) + "-" + String.valueOf(end);
            cachedPair = new de.saar.basic.Pair<String, String>(symbol, ret);
            cachedString = symbol + "," + ret;
        }
        
        @Override
        public String toString() {
            return cachedString;
        }

        public de.saar.basic.Pair<String, String> toPair() {
            return cachedPair;
        }
        
        public boolean equals(State s) {
            if ((start != s.start) || (end != s.end)) {
                return false;
            }
            return (symbol == s.symbol);
        }
    }
}
