package de.saar.penguin.irtg.algebra.lambda


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.saar.basic.tree.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author koller, melzer
 */
class LambdaTermTest {

    @Test
    public void testBeta(){
        LambdaTerm lx = p("((lambda \$x (lambda \$f (\$x \$f))) (lambda \$a (\$a \$a)))");
        LambdaTerm y = p("(lambda \$f (\$f \$f))")

        assertEquals(y,lx.reduce());

    }

    @Test
    public void testEvaluate(){
        Tree<String> t1 = new Tree<String>();
        Tree<String> t2 = new Tree<String>();

        String func = LambdaTermAlgebraSymbol.FUNCTOR;

        String lt1 = "(lambda \$x (lambda \$f (\$x \$f)))";
        String lt2 = "(lambda \$a (\$a \$a))";
        //LambdaTermAlgebraSymbol lt4 = LambdaTermAlgebraSymbol.lterm(p("love:i"));
        //System.err.println(lt4);
        // LambdaTermAlgebraSymbol lt3 = LambdaTermAlgebraSymbol.lterm(c("love"));
        String lt3 = "(lambda \$r (\$r \$x))";

        t1.addNode(null,func,null);
        t1.addNode(lt1,t1.getRoot());
        t1.addNode(lt2,t1.getRoot());

        t2.addNode(null,func,null);
        t2.addSubTree(t1,t2.getRoot());
        t2.addNode(lt3,t2.getRoot());


        LambdaTermAlgebra algebra = new LambdaTermAlgebra();

        LambdaTerm test = a(v("x"),v("x"));

        assertEquals(test,algebra.evaluate(t2));

    }

    @Test
    public void testSplit(){
        LambdaTerm test = p("((lambda \$x (lambda \$f (\$x \$f))) (lambda \$a (love:e \$a)))");
        LambdaTerm easy = LambdaTerm.lambda("\$x",v("x"));

        Map<LambdaTerm,LambdaTerm> decomps = test.getDecompositions();
        Set<LambdaTerm> origin = new HashSet<LambdaTerm>();
        origin.add(test.reduce());
        Set<LambdaTerm> goal = new HashSet<LambdaTerm>();

        for(LambdaTerm entry :decomps.keySet() ){
            LambdaTerm eval = a(entry, decomps.get(entry));
            goal.add(eval.reduce());
        }
        
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        checkAllTermsEquals(alg.decompose(test), test);        
    }
    
    private void checkAllTermsEquals(BottomUpAutomaton auto, LambdaTerm gold) {
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        
        Set<LambdaTerm> reducedSet = new HashSet<LambdaTerm>();
        reducedSet.add(gold.reduce());
        
        Set<LambdaTerm> foundSet = new HashSet<LambdaTerm>();
        
        for( Tree<String> t : auto.language() ) {
            foundSet.add(alg.evaluate(t));
        }
        
        assertEquals(reducedSet, foundSet);
    }
    
    @Test
    public void testSplitGeo() {
        LambdaTerm geo = p("(population:i (capital:c (argmax \$1 (and (state:t \$1) (loc:t mississippi_river:r \$1)) (size:i \$1))))");
        
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        BottomUpAutomaton<LambdaTerm> auto = alg.decompose(geo);        
        
        System.err.println("auto for geo: " + auto);
        System.err.println("trees: " + auto.countTrees());
    }

    @Test
    public void testUnbound(){
        //LambdaTerm test = a(v("y"),LambdaTerm.lambda("\$x",v("x")));
        LambdaTerm test = v("y");
        HashSet<String> answer = new HashSet<String>();
        answer.add("\$y");
        assertEquals(test.findUnboundVariables(),answer);
    }



    private static LambdaTerm a(f, LambdaTerm... a) {
        return LambdaTerm.apply(f,Arrays.asList(a));
    }
    
    private static LambdaTerm c(x) {
        return LambdaTerm.constant(x);
    }
    
    private static LambdaTerm v(x) {
        return LambdaTerm.variable("\$" + x);
    }
    
    private static LambdaTerm p(s) {
        return LambdaTermParser.parse(new StringReader(s));
    }
}
