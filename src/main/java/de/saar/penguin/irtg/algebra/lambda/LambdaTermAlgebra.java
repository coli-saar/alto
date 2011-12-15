/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra.lambda;

import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.algebra.Algebra;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public class LambdaTermAlgebra implements Algebra<LambdaTerm> {

    // evaluates a tree of LambdaTermAlgebraSymbols
    public LambdaTerm evaluate(Tree t) {
        final Tree<LambdaTermAlgebraSymbol> x = (Tree<LambdaTermAlgebraSymbol>) t;

        TreeVisitor<String, LambdaTerm> tv = new TreeVisitor<String, LambdaTerm>() {

            @Override
            public String getRootValue() {
                return x.getRoot();
            }

            @Override
            public LambdaTerm combine(String node, List<LambdaTerm> childValues) {
                ArrayList<LambdaTerm> cV = (ArrayList<LambdaTerm>) childValues;
                LambdaTerm ret;

                if (x.getLabel(node).type.equals(LambdaTermAlgebraSymbol.FUNCTOR)) {
                    // works since it is binary tree
                    LambdaTerm tmp = LambdaTerm.apply(cV.get(0), cV.get(1));
                    ret = tmp.reduce();
                } else {
                    ret = x.getLabel(node).content;
                }

                return ret;
            }
        };

        LambdaTerm retur = (LambdaTerm) t.dfs(tv);
        return retur;

    }

    public BottomUpAutomaton decompose(LambdaTerm value) {
        return new LambdaDecompositionAutomaton(value);
    }

    public LambdaTerm parseString(String representation) throws ParserException {
        try {
            return LambdaTermParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }

    private class LambdaDecompositionAutomaton extends BottomUpAutomaton<LambdaTerm> {
        private Set<String> allLabels;

        // constructor
        public LambdaDecompositionAutomaton(LambdaTerm value) {
            finalStates.add(value);

            allLabels = new HashSet<String>();
            allLabels.add(LambdaTermAlgebraSymbol.FUNCTOR);
        }

        @Override
        public Set<Rule<LambdaTerm>> getRulesBottomUp(String label, List<LambdaTerm> childStates) {
            makeAllRulesExplicit();
            return getRulesBottomUpFromExplicit(label, childStates);
        }

        @Override
        public Set<Rule<LambdaTerm>> getRulesTopDown(String label, LambdaTerm parentState) {
            if (!useCachedRuleTopDown(label, parentState)) {
                // create new rules and cache them

                if (label.equals(LambdaTermAlgebraSymbol.FUNCTOR)) {
                    // split Lambda Term
                    Map<LambdaTerm, LambdaTerm> sources = parentState.getDecompositions();

                    if (sources.isEmpty()) {
                        allLabels.add(parentState.toString());
                    } else {
                        for (Entry<LambdaTerm, LambdaTerm> pair : sources.entrySet()) {
                            Rule<LambdaTerm> rule = new Rule<LambdaTerm>(parentState, label, new LambdaTerm[] { pair.getKey(), pair.getValue() });
                            storeRule(rule);
                        }
                    }
                } else {
                    if (label.equals(parentState.toString())) {
                        storeRule(new Rule<LambdaTerm>(parentState, label, new LambdaTerm[]{}));
                        allLabels.add(label);
                    }
                }
            }

            // return cached rule
            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public void makeAllRulesExplicit() {
            if (!isExplicit) {
                Set<LambdaTerm> everAddedStates = new HashSet<LambdaTerm>();
                Queue<LambdaTerm> agenda = new LinkedList<LambdaTerm>();

                agenda.addAll(getFinalStates());
                everAddedStates.addAll(getFinalStates());

                while (!agenda.isEmpty()) {
                    LambdaTerm state = agenda.remove();
                    findRulesForState(state, LambdaTermAlgebraSymbol.FUNCTOR, everAddedStates, agenda);
                    findRulesForState(state, state.toString(), everAddedStates, agenda);
                }

                isExplicit = true;
            }
        }

        private void findRulesForState(LambdaTerm state, String label, Set<LambdaTerm> everAddedStates, Queue<LambdaTerm> agenda) {
            Set<Rule<LambdaTerm>> rules = getRulesTopDown(label, state);
            for (Rule<LambdaTerm> rule : rules) {
                for (LambdaTerm child : rule.getChildren()) {
                    if (!everAddedStates.contains(child)) {
                        everAddedStates.add(child);
                        agenda.offer(child);
                    }
                }
            }
        }

        @Override
        public Set<String> getAllLabels() {
            makeAllRulesExplicit();
            return allLabels;
        }

        @Override
        public Set<LambdaTerm> getFinalStates() {
            return finalStates;
        }

        @Override
        public Set<LambdaTerm> getAllStates() {
            makeAllRulesExplicit();
            return allStates;
        }
    }
}
