/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Provides a two-level iterator over the rules of a given tree automaton.
 * The outer iterator proceeds over all top-down reachable states of the
 * automaton, starting with the final states. For each state, the inner iterator
 * iterates over the rules that have this state as the parent. The
 * iterators can be combined into a single iterator over all rules of
 * the automaton by Guava's Iterators.concat or something similar.<p>
 * 
 * This class calls {@link TreeAutomaton#getRulesTopDown(int) } to identify
 * the rules in which a given state is the parent.
 * 
 * @author koller
 */
class RuleIterator implements Iterator<Iterator<Rule>> {
    private TreeAutomaton automaton;
    private Queue<Integer> stateAgenda;
    private IntSet visitedStates;  // states in this set are never added ot the agenda (again)
    
    public RuleIterator(TreeAutomaton automaton) {
        this.automaton = automaton;
        stateAgenda = new LinkedList<Integer>();
        visitedStates = new IntOpenHashSet();

        stateAgenda.addAll(automaton.getFinalStates());
        visitedStates.addAll(automaton.getFinalStates());
    }

    public boolean hasNext() {
        return !stateAgenda.isEmpty();
    }

    public Iterator<Rule> next() {
        int state = stateAgenda.remove();
        Iterable<Rule> rules = automaton.getRulesTopDown(state);

        return Iterators.transform(rules.iterator(), new Function<Rule, Rule>() {
            public Rule apply(Rule rule) {
                for (int child : rule.getChildren()) {
                    if (!visitedStates.contains(child)) {
                        stateAgenda.add(child);
                        visitedStates.add(child);
                    }
                }
                return rule;
            }
        });
    }

    public void remove() {
        throw new UnsupportedOperationException("This iterator does not support removals.");
    }
}
