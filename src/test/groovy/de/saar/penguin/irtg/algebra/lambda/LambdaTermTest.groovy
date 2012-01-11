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
import java.util.Map.Entry;

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
    public void getFirstDecompositionGeo(){

	LambdaTerm geo = p("(lambda \$11 (population:i (capital:c (argmax \$x (and (state:t \$x) (\$11 mississippi_river:r \$x)) (size:i \$x)))))");
	Map<LambdaTerm,LambdaTerm> decomps = geo.getDecompositions();

	Set<LambdaTerm> origin = new HashSet<LambdaTerm>();
	origin.add(geo);
	Set<LambdaTerm> goal = new HashSet<LambdaTerm>();

	for(LambdaTerm entry : decomps.keySet() ){
            LambdaTerm eval = a(entry, decomps.get(entry));
            goal.add(eval.reduce());
        }

	assertEquals(origin,goal);
	}	

	@Test
    public void splitGeoEasy(){
	LambdaTerm geo = p("(capital:c utah:s)")
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo);
	}		

	@Test
    public void splitGeoArgmax(){
	LambdaTerm geo = p("(argmax \$0 (city:t \$0) (size:i \$0))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo);
	}

	@Test
    public void splitGeoArgmin(){
	LambdaTerm geo = p("(argmin \$0 (state:t \$0) (population:i \$0))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo);
	}


	@Test
    public void splitGeoConj(){
	//LambdaTerm geo = p("(argmin \$0 (and (place:t \$0) (loc:t \$0 california:s)) (elevation:i \$0))");
	
	LambdaTerm geo = p("(lambda \$0 (and (place:t \$0) (elevation:i \$0)))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo);
	}

	//@Test
    public void splitGeoAll(){
	LambdaTerm geo = p("(argmin \$0 (and (place:t \$0) (loc:t \$0 california:s)) (elevation:i \$0))");
	System.out.println("All");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();

	//System.out.println(alg.decompose(geo));
	checkAllTermsEquals(alg.decompose(geo), geo);
	}

    private static LambdaTerm a(f, LambdaTerm... a) {
        return LambdaTerm.apply(f,Arrays.asList(a));
    }
    
    private static LambdaTerm c(x,type) {
        return LambdaTerm.constant(x,type);
    }
    
    private static LambdaTerm v(x) {
        return LambdaTerm.variable("\$" + x);
    }
    
    private static LambdaTerm p(s) {
        return LambdaTermParser.parse(new StringReader(s));
    }

    private void checkAllTermsEquals(BottomUpAutomaton auto, LambdaTerm gold) {
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        
        Set<LambdaTerm> reducedSet = new HashSet<LambdaTerm>();
        reducedSet.add(gold.reduce());
        
        Set<LambdaTerm> foundSet = new HashSet<LambdaTerm>();
        
        for( Tree<String> t : auto.languageIterable() ) {
            foundSet.add(alg.evaluate(t));
        }
        
        assertEquals(reducedSet, foundSet);
    }


}
