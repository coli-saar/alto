/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.RuleEvaluator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.semiring.AndOrSemiring;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveDead {

    /**
     *
     */
    private final static RuleEvaluator<Boolean> ALWAYS_TRUE = (Rule r) -> true;

    /**
     *
     */
    private final static AndOrSemiring EVALUATOR = new AndOrSemiring();

    /**
     *
     * @param <State>
     * @param toReduce
     * @return
     */
    public static <State> TreeAutomaton<State> reduce(TreeAutomaton<State> toReduce) {
        ConcreteTreeAutomaton<State> cta = new ConcreteTreeAutomaton<>(toReduce.getSignature());
        
        Int2ObjectMap<Boolean> terminate = toReduce.evaluateInSemiring(new AndOrSemiring(), ALWAYS_TRUE);
        
        IntPredicate terminates = (int i) -> {
            return terminate.get(i);
        };

        makeFinalState(toReduce, terminates, cta);

        Iterable<Rule> it = toReduce.getAllRulesTopDown();
        List<State> l = new ObjectArrayList<>();
        outer:
        for (Rule r : it) {
            if (!terminates.test(r.getParent())) {
                continue;
            }

            for (int i : r.getChildren()) {
                if (!terminates.test(i)) {
                    continue outer;
                }
            }

            State parent = toReduce.getStateForId(r.getParent());
            String label = toReduce.getSignature().resolveSymbolId(r.getLabel());
            l.clear();
            for (int i : r.getChildren()) {
                l.add(toReduce.getStateForId(i));
            }

            cta.addRule(cta.createRule(parent, label, l, r.getWeight()));
        }

        return cta;
    }

    /**
     *
     * @param <State>
     * @param toReduce
     * @param idm
     * @param cta
     */
    private static <State> void makeFinalState(TreeAutomaton<State> toReduce, IntPredicate terminates,
            ConcreteTreeAutomaton<State> cta) {
        IntIterator iit = toReduce.getFinalStates().iterator();
        while (iit.hasNext()) {
            int state = iit.nextInt();

            if (terminates.test(state)) {
                cta.addFinalState(cta.addState(toReduce.getStateForId(state)));
            }
        }
    }
}
