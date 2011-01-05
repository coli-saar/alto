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

    protected Map<String, StateListToStateMap> explicitRules;
    protected Map<String, SetMultimap<State, List<State>>> explicitRulesTopDown;
    protected Set<State> finalStates;
    protected Set<State> allStates;
    private final LeafToStateSubstitution<State, String> dummyLtsSubstitution = new LeafToStateSubstitution<State, String>();

    public BottomUpAutomaton() {
        explicitRules = new HashMap<String, StateListToStateMap>();
        explicitRulesTopDown = new HashMap<String, SetMultimap<State, List<State>>>();
        finalStates = new HashSet<State>();
        allStates = new HashSet<State>();
    }

    abstract public Set<State> getParentStates(String label, List<State> childStates);

    abstract public Set<List<State>> getRulesForParentState(String label, State parentState);

    abstract public int getArity(String label);

    abstract public Set<String> getAllLabels();

    abstract public Set<State> getFinalStates();

    abstract public Set<State> getAllStates();

    protected void storeRule(String label, List<State> childStates, State parentState) {
        StateListToStateMap smap = getOrCreateStateMap(label);
        smap.put(childStates, parentState);

        SetMultimap<State, List<State>> topdown = explicitRulesTopDown.get(label);
        if (topdown == null) {
            topdown = HashMultimap.create();
            explicitRulesTopDown.put(label, topdown);
        }
        topdown.put(parentState, childStates);

        if (allStates != null) {
            allStates.add(parentState);
            allStates.addAll(childStates);
        }
    }

    protected Set<State> getParentStatesFromExplicitRules(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return new HashSet<State>();
        } else {
            return smap.get(childStates);
        }
    }

    protected Set<List<State>> getRulesForParentStateFromExplicit(String label, State parentState) {
        if (containsTopDown(label, parentState)) {
            return explicitRulesTopDown.get(label).get(parentState);
        } else {
            return new HashSet<List<State>>();
        }
    }

    public Map<String, Map<List<State>, Set<State>>> getAllRules() {
        Map<String, Map<List<State>, Set<State>>> ret = new HashMap<String, Map<List<State>, Set<State>>>();

        for (String f : getAllLabels()) {
            ret.put(f, getAllRules(f));
        }

        return ret;
    }

    private Map<List<State>, Set<State>> getAllRules(String label) {
        if (explicitRules.containsKey(label)) {
            return explicitRules.get(label).getAllRules();
        } else {
            return new HashMap<List<State>, Set<State>>();
        }
    }

    public long countTrees() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BottomUpAutomaton)) {
            return false;
        }

        Map<String, Map<List<State>, Set<State>>> rules = getAllRules();
        Map<String, Map<List<State>, Set<State>>> otherRules = ((BottomUpAutomaton) o).getAllRules();

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
                if (!new HashSet<State>(rules.get(f).get(states)).equals(new HashSet<State>(otherRules.get(f).get(states)))) {
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
        Map<String, Map<List<State>, Set<State>>> rules = getAllRules();

        for (String f : getAllLabels()) {
            for (List<State> children : rules.get(f).keySet()) {
                for (State parent : rules.get(f).get(children)) {
                    buf.append(f + (children.isEmpty() ? "" : children) + " -> " + parent + "\n");
                }
            }
        }

        return buf.toString();
    }

    public void makeAllRulesExplicit() {
        Set<State> everAddedStates = new HashSet<State>();
        Queue<State> agenda = new LinkedList<State>();

        agenda.addAll(getFinalStates());
        everAddedStates.addAll(getFinalStates());

        while( ! agenda.isEmpty() ) {
            State state = agenda.remove();

            for( String label : getAllLabels() ) {
                Set<List<State>> rules = getRulesForParentState(label, state);
                for( List<State> children : rules ) {
                    for( State child : children ) {
                        if( ! everAddedStates.contains(child)) {
                            everAddedStates.add(child);
                            agenda.offer(child);
                        }
                    }
                }
            }
        }

        /*
        for (String f : getAllLabels()) {
            int arity = getArity(f);

            List<Set<State>> statesTuple = new ArrayList<Set<State>>();
            for (int i = 0; i < arity; i++) {
                statesTuple.add(new HashSet<State>(getAllStates()));
            }

            CartesianIterator<State> it = new CartesianIterator<State>(statesTuple);
            while (it.hasNext()) {
                List<State> childStates = it.next();
                Set<State> parentStates = getParentStates(f, childStates);
                for (State p : parentStates) {
                    storeRule(f, childStates, p);
                }
            }
        }
         *
         */
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
        SetMultimap<State, List<State>> topdown = explicitRulesTopDown.get(label);
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
                        states.addAll(getParentStates(f, new ArrayList<State>()));
                    }
                } else {
                    CartesianIterator<State> it = new CartesianIterator<State>(childrenValues);

                    while (it.hasNext()) {
                        Set<State> parentStates = getParentStates(f, it.next());
                        states.addAll(parentStates);
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

//    public void runTopDown(final Tree<StringOrVariable> tree, final Map<)

    private StateListToStateMap getOrCreateStateMap(String label) {
        StateListToStateMap ret = explicitRules.get(label);

        if (ret == null) {
            ret = new StateListToStateMap();
            explicitRules.put(label, ret);
        }

        return ret;
    }

    protected class StateListToStateMap {

        private Map<State, StateListToStateMap> nextStep;
        private Set<State> rhsState;
        private int arity;

        public StateListToStateMap() {
            rhsState = new HashSet<State>();
            nextStep = new HashMap<State, StateListToStateMap>();
            arity = -1;
        }

        public void put(List<State> stateList, State state) {
            put(stateList, state, 0);

            if (arity != -1) {
                if (arity != stateList.size()) {
                    throw new UnsupportedOperationException("Storing state lists of different length: " + stateList + ", should be " + arity);
                }
            } else {
                arity = stateList.size();
            }
        }

        private void put(List<State> stateList, State state, int index) {
            if (index == stateList.size()) {
                rhsState.add(state);
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    sub = new StateListToStateMap();
                    nextStep.put(nextState, sub);
                }

                sub.put(stateList, state, index + 1);
            }
        }

        public Set<State> get(List<State> stateList) {
            return get(stateList, 0);
        }

        private Set<State> get(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return rhsState;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return new HashSet<State>();
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

        public Map<List<State>, Set<State>> getAllRules() {
            Map<List<State>, Set<State>> ret = new HashMap<List<State>, Set<State>>();
            List<State> currentStateList = new ArrayList<State>();
            retrieveAll(currentStateList, 0, getArity(), ret);
            return ret;
        }

        private void retrieveAll(List<State> currentStateList, int index, int arity, Map<List<State>, Set<State>> ret) {
            if (index == arity) {
                ret.put(new ArrayList<State>(currentStateList), rhsState);
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
