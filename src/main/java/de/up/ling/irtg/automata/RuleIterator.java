/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import static de.up.ling.irtg.automata.RulesIterator.getAllRulesIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author koller
 */
public class RuleIterator implements Iterator<Iterator<Rule>> {
    private TreeAutomaton automaton;
    private Queue<Integer> stateAgenda;
    private IntSet visitedStates;  // states in this set are never added ot the agenda (again)
    
    public static Iterable<Rule> getAllRules(final TreeAutomaton automaton) {
        return new Iterable<Rule>() {
            public Iterator<Rule> iterator() {
                return getAllRulesIterator(automaton);
            }
        };
    }
    
    public static Iterator<Rule> getAllRulesIterator(TreeAutomaton automaton) {
        return Iterators.concat(new RuleIterator(automaton));
    }

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
