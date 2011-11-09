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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

                   if (x.getLabel(node).type.equals("FUNCTOR")){
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LambdaTerm parseString(String representation) throws ParserException {
        try {
            return LambdaTermParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }
    }
    
}
