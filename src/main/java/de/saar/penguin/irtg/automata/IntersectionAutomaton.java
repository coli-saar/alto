/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
class IntersectionAutomaton<LeftState, RightState> extends BottomUpAutomaton<Pair<LeftState, RightState>> {
    private BottomUpAutomaton<LeftState> left;
    private BottomUpAutomaton<RightState> right;
    private Set<String> allString;

    public IntersectionAutomaton(BottomUpAutomaton<LeftState> left, BottomUpAutomaton<RightState> right) {
        this.left = left;
        this.right = right;

        allString = new HashSet<String>(left.getAllLabels());
        allString.retainAll(right.getAllLabels());

        finalStates = null;
        allStates = null;
    }

    @Override
    public Set<String> getAllLabels() {
        return allString;
    }

    @Override
    public Set<Pair<LeftState, RightState>> getFinalStates() {
        if (finalStates == null) {
            finalStates = new HashSet<Pair<LeftState, RightState>>();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    private void collectStatePairs(Collection<LeftState> leftStates, Collection<RightState> rightStates, Collection<Pair<LeftState, RightState>> pairStates) {
        List<Collection> stateSets = new ArrayList<Collection>();
        stateSets.add(leftStates);
        stateSets.add(rightStates);

        CartesianIterator it = new CartesianIterator(stateSets);
        while (it.hasNext()) {
            List<Object> states = it.next();
            pairStates.add(new Pair(states.get(0), states.get(1)));
        }
    }

    @Override
    public Set<Pair<LeftState, RightState>> getParentStates(String label, List<Pair<LeftState, RightState>> childStates) {
        if (contains(label, childStates)) {
            return getParentStatesFromExplicitRules(label, childStates);
        } else {
            List<LeftState> leftChildStates = new ArrayList<LeftState>();
            List<RightState> rightChildStates = new ArrayList<RightState>();
            for (Pair<LeftState, RightState> childState : childStates) {
                leftChildStates.add(childState.left);
                rightChildStates.add(childState.right);
            }

            Set<LeftState> leftParentStates = left.getParentStates(label, leftChildStates);
            Set<RightState> rightParentStates = right.getParentStates(label, rightChildStates);
            Set<Pair<LeftState, RightState>> ret = new HashSet<Pair<LeftState, RightState>>();

            collectStatePairs(leftParentStates, rightParentStates, ret);

            // cache result
            for (Pair<LeftState, RightState> parentState : ret) {
                storeRule(label, childStates, parentState);
            }

            return ret;
        }
    }

    @Override
    public Set<Pair<LeftState, RightState>> getAllStates() {
        if (allStates == null) {
            allStates = new HashSet<Pair<LeftState, RightState>>();
            collectStatePairs(left.getAllStates(), right.getAllStates(), allStates);
        }

        return allStates;
    }

    @Override
    public int getArity(String label) {
        return left.getArity(label);
    }
}
