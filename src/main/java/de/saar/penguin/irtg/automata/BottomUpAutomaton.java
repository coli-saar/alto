/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.saar.penguin.irtg.semiring.AndOrSemiring;
import de.saar.penguin.irtg.semiring.LongArithmeticSemiring;
import de.saar.penguin.irtg.semiring.Semiring;
import de.saar.penguin.irtg.semiring.ViterbiWithBackpointerSemiring;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class BottomUpAutomaton<State> {

    protected Map<String, StateListToStateMap> explicitRules; // one for each label
    protected Map<String, SetMultimap<State, Rule<State>>> explicitRulesTopDown;
    protected Set<State> finalStates;
    protected Set<State> allStates;
    private final LeafToStateSubstitution<State, String> dummyLtsSubstitution = new LeafToStateSubstitution<State, String>();
    protected boolean isExplicit;

    public BottomUpAutomaton() {
        explicitRules = new HashMap<String, StateListToStateMap>();
        explicitRulesTopDown = new HashMap<String, SetMultimap<State, Rule<State>>>();
        finalStates = new HashSet<State>();
        allStates = new HashSet<State>();
        isExplicit = false;
    }

    abstract public Set<Rule<State>> getRulesBottomUp(String label, List<State> childStates);

    abstract public Set<Rule<State>> getRulesTopDown(String label, State parentState);

    abstract public int getArity(String label);

    abstract public Set<String> getAllLabels();

    abstract public Set<State> getFinalStates();

    abstract public Set<State> getAllStates();

    protected void storeRule(Rule<State> rule) {
        StateListToStateMap smap = getOrCreateStateMap(rule.getLabel());
        smap.put(rule);

        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(rule.getLabel());
        if (topdown == null) {
            topdown = HashMultimap.create();
            explicitRulesTopDown.put(rule.getLabel(), topdown);
        }
        topdown.put(rule.getParent(), rule);

        if (allStates != null) {
            allStates.add(rule.getParent());
            for (int i = 0; i < rule.getArity(); i++) {
                allStates.add(rule.getChildren()[i]);
            }
        }
    }

    protected Set<Rule<State>> getRulesBottomUpFromExplicit(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return new HashSet<Rule<State>>();
        } else {
            return smap.get(childStates);
        }
    }

    protected Set<Rule<State>> getRulesTopDownFromExplicit(String label, State parentState) {
        if (containsTopDown(label, parentState)) {
            return explicitRulesTopDown.get(label).get(parentState);
        } else {
            return new HashSet<Rule<State>>();
        }
    }

    public Map<String, Map<List<State>, Set<Rule<State>>>> getAllRules() {
        Map<String, Map<List<State>, Set<Rule<State>>>> ret = new HashMap<String, Map<List<State>, Set<Rule<State>>>>();

        makeAllRulesExplicit();

        for (String f : getAllLabels()) {
            ret.put(f, getAllRules(f));
        }

        return ret;
    }

    private Map<List<State>, Set<Rule<State>>> getAllRules(String label) {
        if (explicitRules.containsKey(label)) {
            return explicitRules.get(label).getAllRules();
        } else {
            return new HashMap<List<State>, Set<Rule<State>>>();
        }
    }

    // TODO - this is only correct if the FTA is bottom-up deterministic
    public long countTrees() {
        Map<State, Long> map = evaluateInSemiring(new LongArithmeticSemiring(), new RuleEvaluator<State, Long>() {

            public Long evaluateRule(Rule<State> rule) {
                return 1L;
            }
        });

        long ret = 0L;
        for (State f : getFinalStates()) {
            ret += map.get(f);
        }
        return ret;
    }

    public Tree viterbi() {
        // run Viterbi algorithm bottom-up, saving rules as backpointers
        Map<State, Pair<Double, Rule<State>>> map =
                evaluateInSemiring(new ViterbiWithBackpointerSemiring<State>(), new RuleEvaluator<State, Pair<Double, Rule<State>>>() {

            public Pair<Double, Rule<State>> evaluateRule(Rule<State> rule) {
                return new Pair<Double, Rule<State>>(rule.getWeight(), rule);
            }
        });
        
        // find final state with highest weight
        State bestFinalState = null;
        double weightBestFinalState = Double.POSITIVE_INFINITY;
        for( State s : getFinalStates() ) {
            if( map.get(s).left < weightBestFinalState ) {
                bestFinalState = s;
                weightBestFinalState = map.get(s).left;
            }
        }
        
        // extract best tree from backpointers
        Tree ret = new Tree();
        extractTreeFromViterbi(ret, null, bestFinalState, map);
        return ret;
    }
        
    private void extractTreeFromViterbi(Tree tree, String parent, State state, Map<State, Pair<Double, Rule<State>>> map) {
        Rule<State> backpointer = map.get(state).right;
        String node = tree.addNode(backpointer.getLabel(), parent);
        for( State child : backpointer.getChildren() ) {
            extractTreeFromViterbi(tree, node, child, map);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BottomUpAutomaton)) {
            return false;
        }

        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        Map<String, Map<List<State>, Set<Rule<State>>>> otherRules = ((BottomUpAutomaton) o).getAllRules();

        if (!rules.keySet().equals(otherRules.keySet())) {
//            System.err.println("not equals: labels " + rules.keySet() + " vs " +otherRules.keySet());
            return false;
        }

        for (String f : rules.keySet()) {
            if (!rules.get(f).keySet().equals(otherRules.get(f).keySet())) {
//                System.err.println("not equals: LHS for " + f + " is " + rules.get(f).keySet() + " vs " + otherRules.get(f).keySet() );
                return false;
            }

            for (List<State> states : rules.get(f).keySet()) {
                if (!new HashSet<Rule<State>>(rules.get(f).get(states)).equals(new HashSet<Rule<State>>(otherRules.get(f).get(states)))) {
//                    System.err.println("noteq: RHS for " + f + states + " is " + rules.get(f).get(states) + " vs " + otherRules.get(f).get(states));
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();

        for (String f : getAllLabels()) {
            for (List<State> children : rules.get(f).keySet()) {
                for (Rule rule : rules.get(f).get(children)) {
                    buf.append(rule.toString() + (getFinalStates().contains(rule.getParent()) ? "!" : "") + "\n");
                }
            }
        }

        return buf.toString();
    }

    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Set<State> everAddedStates = new HashSet<State>();
            Queue<State> agenda = new LinkedList<State>();

            agenda.addAll(getFinalStates());
            everAddedStates.addAll(getFinalStates());

            while (!agenda.isEmpty()) {
                State state = agenda.remove();

                for (String label : getAllLabels()) {
                    Set<Rule<State>> rules = getRulesTopDown(label, state);
                    for (Rule<State> rule : rules) {
                        for (State child : rule.getChildren()) {
                            if (!everAddedStates.contains(child)) {
                                everAddedStates.add(child);
                                agenda.offer(child);
                            }
                        }
                    }
                }
            }

            isExplicit = true;
        }
    }

    protected boolean contains(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return false;
        } else {
            return smap.contains(childStates);
        }
    }

    protected boolean containsTopDown(String label, State parent) {
        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(label);
        if (topdown == null) {
            return false;
        } else {
            return topdown.containsKey(parent);
        }
    }

    public <OtherState> BottomUpAutomaton<Pair<State, OtherState>> intersect(BottomUpAutomaton<OtherState> other) {
        return new IntersectionAutomaton<State, OtherState>(this, other);
    }

    public BottomUpAutomaton<State> inverseHomomorphism(Homomorphism hom) {
        return new InverseHomAutomaton<State>(this, hom);
    }

    public boolean accepts(final Tree tree) {
        Set<State> resultStates = run(tree);
        resultStates.retainAll(getFinalStates());
        return !resultStates.isEmpty();
    }

    public Set<State> run(final Tree tree) {
        return run(tree, dummyLtsSubstitution);
    }

    public Set<State> run(final Tree tree, final LeafToStateSubstitution<State, String> subst) {
        final Set<State> ret = new HashSet<State>();

        tree.dfs(new TreeVisitor<Void, Set<State>>() {

            @Override
            public Set<State> combine(String node, List<Set<State>> childrenValues) {
                String f = tree.getLabel(node).toString();
                Set<State> states = new HashSet<State>();

                if (childrenValues.isEmpty()) {
                    if (subst.isSubstituted(node)) {
                        states.add(subst.substitute(node));
                    } else {
                        for (Rule<State> rule : getRulesBottomUp(f, new ArrayList<State>())) {
                            states.add(rule.getParent());
                        }
                    }
                } else {
                    CartesianIterator<State> it = new CartesianIterator<State>(childrenValues);

                    while (it.hasNext()) {
                        for (Rule<State> rule : getRulesBottomUp(f, it.next())) {
                            states.add(rule.getParent());
                        }
                    }
                }

                if (node.equals(tree.getRoot())) {
                    ret.addAll(states);
                }

                return states;
            }
        });

        return ret;
    }

    public BottomUpAutomaton<State> reduce() {
        Map<State, Boolean> productiveStates = evaluateInSemiring(new AndOrSemiring(), new RuleEvaluator<State, Boolean>() {

            public Boolean evaluateRule(Rule<State> rule) {
                return true;
            }
        });

        ConcreteBottomUpAutomaton<State> ret = new ConcreteBottomUpAutomaton<State>();
        Map<String, Map<List<State>, Set<Rule<State>>>> allRules = getAllRules();

        // copy all rules that only contain productive states
        for (String label : allRules.keySet()) {
            for (List<State> children : allRules.get(label).keySet()) {
                boolean allProductive = true;
                for (State child : children) {
                    if (!productiveStates.get(child)) {
                        allProductive = false;
                    }
                }

                if (allProductive) {
                    for (Rule<State> rule : allRules.get(label).get(children)) {
                        if (productiveStates.get(rule.getParent())) {
                            ret.addRule(label, children, rule.getParent());
                        }
                    }
                }
            }
        }

        // copy all productive final states
        for (State state : getFinalStates()) {
            if (productiveStates.get(state)) {
                ret.addFinalState(state);
            }
        }

        return ret;
    }

    public <E> Map<State, E> evaluateInSemiring(Semiring<E> semiring, RuleEvaluator<State, E> evaluator) {
        Map<State, E> ret = new HashMap<State, E>();

        for (State s : getStatesInBottomUpOrder()) {
            E accu = semiring.zero();

            for (String label : getAllLabels()) {
                Set<Rule<State>> rules = getRulesTopDown(label, s);

                for (Rule<State> rule : rules) {
                    E valueThisRule = evaluator.evaluateRule(rule);
                    for (State child : rule.getChildren()) {
                        if (!ret.containsKey(child)) {
                            throw new RuntimeException("State " + child + " not yet evaluated when processing rule " + rule + " for " + s + "/" + label);
                        }
                        valueThisRule = semiring.multiply(valueThisRule, ret.get(child));
                    }

                    accu = semiring.add(accu, valueThisRule);
                }
            }

            ret.put(s, accu);
        }

        return ret;
    }

    public List<State> getStatesInBottomUpOrder() {
        List<State> ret = new ArrayList<State>();
        SetMultimap<State, State> children = HashMultimap.create(); // children(q) = {q1,...,qn} means that q1,...,qn occur as child states of rules of which q is parent state
        Set<State> visited = new HashSet<State>();

        // traverse all rules to compute graph
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        for (Map<List<State>, Set<Rule<State>>> rulesPerLabel : rules.values()) {
            for (List<State> lhs : rulesPerLabel.keySet()) {
                Set<Rule<State>> rhsStates = rulesPerLabel.get(lhs);

                for (Rule<State> rule : rhsStates) {
                    children.putAll(rule.getParent(), lhs);
                }
            }
        }

        // perform topological sort
        for (State q : getFinalStates()) {
            dfsForStatesInBottomUpOrder(q, children, visited, ret);
        }

        return ret;
    }

    private void dfsForStatesInBottomUpOrder(State q, SetMultimap<State, State> children, Set<State> visited, List<State> ret) {
        if (!visited.contains(q)) {
            visited.add(q);

            for (State parent : children.get(q)) {
                dfsForStatesInBottomUpOrder(parent, children, visited, ret);
            }

            ret.add(q);
        }
    }

    private StateListToStateMap getOrCreateStateMap(String label) {
        StateListToStateMap ret = explicitRules.get(label);

        if (ret == null) {
            ret = new StateListToStateMap(label);
            explicitRules.put(label, ret);
        }

        return ret;
    }


    protected class StateListToStateMap {

        private Map<State, StateListToStateMap> nextStep;
        private Set<Rule<State>> rulesHere;
        private int arity;
        private String label;

        public StateListToStateMap(String label) {
            rulesHere = new HashSet<Rule<State>>();
            nextStep = new HashMap<State, StateListToStateMap>();
            arity = -1;
            this.label = label;
        }

        public void put(Rule<State> rule) {
            put(rule, 0, 1);

            if (arity != -1) {
                if (arity != rule.getChildren().length) {
                    throw new UnsupportedOperationException("Storing state lists of different length: " + rule + ", should be " + arity);
                }
            } else {
                arity = rule.getChildren().length;
            }
        }

        private void put(Rule<State> rule, int index, double weight) {
            if (index == rule.getArity()) {
//                Rule<State> rule = new Rule<State>(state, label, stateList, weight);
                rulesHere.add(rule);
            } else {
                State nextState = rule.getChildren()[index];
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    sub = new StateListToStateMap(label);
                    nextStep.put(nextState, sub);
                }

                sub.put(rule, index + 1, weight);
            }
        }

        public Set<Rule<State>> get(List<State> stateList) {
            return get(stateList, 0);
        }

        private Set<Rule<State>> get(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return rulesHere;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return new HashSet<Rule<State>>();
                } else {
                    return sub.get(stateList, index + 1);
                }
            }
        }

        public boolean contains(List<State> stateList) {
            return contains(stateList, 0);
        }

        private boolean contains(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return true;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return false;
                } else {
                    return sub.contains(stateList, index + 1);
                }
            }
        }

        public int getArity() {
            return arity;
        }

        public Map<List<State>, Set<Rule<State>>> getAllRules() {
            Map<List<State>, Set<Rule<State>>> ret = new HashMap<List<State>, Set<Rule<State>>>();
            List<State> currentStateList = new ArrayList<State>();
            retrieveAll(currentStateList, 0, getArity(), ret);
            return ret;
        }

        private void retrieveAll(List<State> currentStateList, int index, int arity, Map<List<State>, Set<Rule<State>>> ret) {
            if (index == arity) {
                ret.put(new ArrayList<State>(currentStateList), rulesHere);
            } else {
                for (State state : nextStep.keySet()) {
                    currentStateList.add(state);
                    nextStep.get(state).retrieveAll(currentStateList, index + 1, arity, ret);
                    currentStateList.remove(index);
                }
            }
        }

        @Override
        public String toString() {
            return getAllRules().toString();
        }
    }
}
