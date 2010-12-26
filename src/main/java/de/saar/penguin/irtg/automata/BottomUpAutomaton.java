/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.saar.basic.StringOrVariable;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.hom.Homomorphism;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class BottomUpAutomaton<State> {
    protected Map<String, StateListToStateMap> explicitRules;
    protected Set<State> finalStates;
    private final LeafToStateSubstitution<State, String> dummyLtsSubstitution = new LeafToStateSubstitution<State, String>();

    public BottomUpAutomaton() {
        explicitRules = new HashMap<String, StateListToStateMap>();
        finalStates = new HashSet<State>();
    }

    public void addRule(String label, List<State> childStates, State parentState) {
        StateListToStateMap smap = getOrCreateStateMap(label);
        smap.put(childStates, parentState);
    }

    public List<State> getParentStates(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return new ArrayList<State>();
        } else {
            return smap.get(childStates);
        }
    }

    public boolean contains(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return false;
        } else {
            return smap.contains(childStates);
        }
    }

    public Set<String> getAllLabels() {
        return explicitRules.keySet();
    }

    public void addFinalState(State state) {
        finalStates.add(state);
    }

    public Set<State> getFinalStates() {
        return finalStates;
    }

    public <OtherState> BottomUpAutomaton<Pair<State, OtherState>> intersect(BottomUpAutomaton<OtherState> other) {
        return new IntersectionAutomaton<State, OtherState>(this, other);
    }

    public BottomUpAutomaton<State> inverseHomomorphism(Homomorphism hom) {
        return new InverseHomAutomaton<State>(this, hom);
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

                System.err.println("  visit " + node + "/" + f + ", kids=" + childrenValues);

                if (childrenValues.isEmpty()) {
                    if (subst.isSubstituted(f)) {
                        states.add(subst.substitute(f));
                    } else {
                        System.err.println("  -> parents " + getParentStates(f, new ArrayList<State>()));
                        states.addAll(getParentStates(f, new ArrayList<State>()));
                    }
                } else {
                    CartesianIterator<State> it = new CartesianIterator<State>(childrenValues);

                    while (it.hasNext()) {
                        List<State> parentStates = getParentStates(f, it.next());
                        states.addAll(parentStates);
                    }
                }

                if (node.equals(tree.getRoot())) {
                    ret.addAll(states);
                }

                System.err.println("  -> " + node + " -> " + states);

                return states;
            }
        });

        return ret;
    }

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
        private List<State> rhsState;
        private int arity;

        public StateListToStateMap() {
            rhsState = new ArrayList<State>();
            nextStep = new HashMap<State, StateListToStateMap>();
            arity = -1;
        }

        public void put(List<State> stateList, State state) {
            put(stateList, state, 0);

            if (arity != -1) {
                if (arity != stateList.size()) {
                    throw new UnsupportedOperationException("Storing state lists of different length: " + stateList);
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

        public List<State> get(List<State> stateList) {
            return get(stateList, 0);
        }

        private List<State> get(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return rhsState;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return new ArrayList<State>();
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

        public Map<List<State>, List<State>> getAllRules() {
            Map<List<State>, List<State>> ret = new HashMap<List<State>, List<State>>();
            List<State> currentStateList = new ArrayList<State>();
            retrieveAll(currentStateList, 0, ret);
            return ret;
        }

        private void retrieveAll(List<State> currentStateList, int index, Map<List<State>, List<State>> ret) {
            if (index == getArity()) {
                ret.put(new ArrayList<State>(currentStateList), rhsState);
            } else {
                for (State state : nextStep.keySet()) {
                    currentStateList.add(index, state);
                    retrieveAll(currentStateList, index + 1, ret);
                }
            }
        }
    }
}
