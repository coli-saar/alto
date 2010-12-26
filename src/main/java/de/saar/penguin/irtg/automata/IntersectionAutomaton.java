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
class IntersectionAutomaton<LeftState, RightState, Labels> extends BottomUpAutomaton<Pair<LeftState, RightState>, Labels> {

    private BottomUpAutomaton<LeftState, Labels> left;
    private BottomUpAutomaton<RightState, Labels> right;
    private Set<Labels> allLabels;

    public IntersectionAutomaton(BottomUpAutomaton<LeftState, Labels> left, BottomUpAutomaton<RightState, Labels> right) {
        this.left = left;
        this.right = right;

        allLabels = new HashSet<Labels>(left.getAllLabels());
        allLabels.retainAll(right.getAllLabels());

        finalStates = null;
    }

    @Override
    public Set<Labels> getAllLabels() {
        return allLabels;
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
    public List<Pair<LeftState, RightState>> getParentStates(Labels label, List<Pair<LeftState, RightState>> childStates) {
        if (contains(label, childStates)) {
            return super.getParentStates(label, childStates);
        } else {
            List<LeftState> leftChildStates = new ArrayList<LeftState>();
            List<RightState> rightChildStates = new ArrayList<RightState>();
            for (Pair<LeftState, RightState> childState : childStates) {
                leftChildStates.add(childState.left);
                rightChildStates.add(childState.right);
            }

            List<LeftState> leftParentStates = left.getParentStates(label, leftChildStates);
            List<RightState> rightParentStates = right.getParentStates(label, rightChildStates);
            List<Pair<LeftState, RightState>> ret = new ArrayList<Pair<LeftState, RightState>>();

            collectStatePairs(leftParentStates, rightParentStates, ret);

            // cache result
            for (Pair<LeftState, RightState> parentState : ret) {
                addRule(label, childStates, parentState);
            }

            return ret;
        }
    }
}
