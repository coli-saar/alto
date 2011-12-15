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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author koller
 */
public class LambdaTermAlgebra implements Algebra<LambdaTerm> {

    // evaluates a tree of LambdaTermAlgebraSymbols
    public LambdaTerm evaluate(Tree t) {
        final Tree<LambdaTermAlgebraSymbol> x = (Tree<LambdaTermAlgebraSymbol>) t;

        TreeVisitor<String,LambdaTerm> tv = new TreeVisitor<String,LambdaTerm>(){

                @Override
                public String getRootValue(){
                    return x.getRoot();
                }

                @Override
                public LambdaTerm combine(String node, List<LambdaTerm> childValues){
                   ArrayList<LambdaTerm> cV = (ArrayList<LambdaTerm>) childValues;
                   LambdaTerm ret;

                   if (x.getLabel(node).type.equals(LambdaTermAlgebraSymbol.FUNCTOR)){
                       // works since it is binary tree
                       LambdaTerm tmp = LambdaTerm.apply(cV.get(0),cV.get(1));
                       ret = tmp.reduce();
                   }
                   else{
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

    private class LambdaDecompositionAutomaton extends BottomUpAutomaton<LambdaTerm>{

        private Set<String> allLabels;

        // constructor
        public LambdaDecompositionAutomaton(LambdaTerm value){
            finalStates.add(value);
            allLabels.add(LambdaTermAlgebraSymbol.FUNCTOR);
        }

        @Override
        public Set<Rule<LambdaTerm>> getRulesBottomUp(String label, List<LambdaTerm> childStates) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Set<Rule<LambdaTerm>> getRulesTopDown(String label, LambdaTerm parentState) {
            if( ! useCachedRuleTopDown(label, parentState)) {
            // create new rules and cache them
            Set<Rule<LambdaTerm>> ret = new HashSet<Rule<LambdaTerm>>();
            if(label.equals(LambdaTermAlgebraSymbol.FUNCTOR)){
                // split Lambda Term
                Map<LambdaTerm,LambdaTerm> sources = parentState.getSource();

                if(sources.isEmpty()){
                    allLabels.add(parentState.toString());
                }
                else{
                    for(Entry<LambdaTerm,LambdaTerm> pair : sources.entrySet()){
                        List<LambdaTerm> childStates = new ArrayList<LambdaTerm>();
                        childStates.add(pair.getKey());
                        childStates.add(pair.getValue());
                        Rule<LambdaTerm> rule = new Rule<LambdaTerm>(parentState,label,childStates);
                        storeRule(rule);
                    }
                }
              }
            }
            // return cached ruule
            return getRulesTopDownFromExplicit(label,parentState);
           // throw new UnsupportedOperationException("Not supported yet.");

        }

        @Override
        public Set<String> getAllLabels() {
            return allLabels;
        }

        @Override
        public Set<LambdaTerm> getFinalStates() {
           return finalStates;
        }

        @Override
        public Set<LambdaTerm> getAllStates() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
    
}
