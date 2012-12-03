/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.lambda;

import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.StringReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/// TODO: implement signature properly!

/**
 *
 * @author koller
 */
public class LambdaTermAlgebra implements Algebra<LambdaTerm> {
    @Override
    public LambdaTerm evaluate(final Tree<String> t) {
        return t.dfs(new TreeVisitor<String, Void, LambdaTerm>() {
            @Override
            public LambdaTerm combine(Tree<String> node, List<LambdaTerm> childrenValues) {
                LambdaTerm ret;

                if (node.getLabel().equals(LambdaTermAlgebraSymbol.FUNCTOR)) {
                    // works since it is binary tree
                    LambdaTerm tmp = LambdaTerm.apply(childrenValues.get(0), childrenValues.get(1).alphaConvert(childrenValues.get(0).findHighestVarName() + 1));
                    ret = tmp.reduce();
                    //System.out.println(tmp.getTree()+" reduziert zu "+ret.getTree()+" nit "+ret);
                } else {
                    try {
                        //                    ret = t.getLabel(node);
                        ret = LambdaTermParser.parse(new StringReader(node.getLabel()));
                    } catch (ParseException ex) {
                        ret = LambdaTerm.constant(">>" + node.getLabel() + "<<", "LALA");
                    }
                }

                return ret;
            }            
        });
    }

    @Override
    public TreeAutomaton decompose(LambdaTerm value) {
        return new LambdaDecompositionAutomaton(value);
    }

    @Override
    public LambdaTerm parseString(String representation) throws ParserException {
        try {
            return LambdaTermParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }

    @Override
    public Signature getSignature() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class LambdaDecompositionAutomaton extends TreeAutomaton<LambdaTerm> {
        private Set<String> allLabels;

        // constructor
        public LambdaDecompositionAutomaton(LambdaTerm value) {
            super(LambdaTermAlgebra.this.getSignature());
            
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
            // System.out.println("Schon mal? "+parentState+" "+label);
            if (!useCachedRuleTopDown(label, parentState)) {
                // create new rules and cache them

                if (label.equals(LambdaTermAlgebraSymbol.FUNCTOR)) {
                    // split Lambda Term
                    Map<LambdaTerm, LambdaTerm> sources = parentState.getDecompositions();
                     //System.err.println("Decompising "+parentState);
                    if (sources.isEmpty()) {
                        allLabels.add(parentState.toString());
                    } else {
                        for (Entry<LambdaTerm, LambdaTerm> pair : sources.entrySet()) {
                            Rule<LambdaTerm> rule = new Rule<LambdaTerm>(parentState, label, new LambdaTerm[]{pair.getKey(), pair.getValue()});
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
        public Set<String> getLabelsTopDown(LambdaTerm parentState) {
            Set<String> ret = new HashSet<String>();
            ret.add(LambdaTermAlgebraSymbol.FUNCTOR);
            ret.add(parentState.toString());
            return ret;
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
