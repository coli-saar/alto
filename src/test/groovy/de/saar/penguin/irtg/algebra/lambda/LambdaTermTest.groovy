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
        LambdaTerm y = p("(lambda \$f (\$f \$f))");
	
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
	checkAllTermsEquals(alg.decompose(geo), geo, false);
    }		

    @Test
    public void splitGeoArgmax(){
	LambdaTerm geo = p("(argmax \$0 (city:t \$0) (size:i \$0))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo, false);
    }

    @Test
    public void splitGeoArgmin(){
	LambdaTerm geo = p("(argmin \$0 (state:t \$0) (population:i \$0))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
        BottomUpAutomaton auto = alg.decompose(geo);
	checkAllTermsEquals(auto, geo, false);
        System.err.println("argmin: " + auto);
    }


    @Test
    public void splitGeoConj(){
	LambdaTerm geo = p("(lambda \$0 (and (place:t \$0) (elevation:i \$0)))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
        BottomUpAutomaton auto = alg.decompose(geo);
	checkAllTermsEquals(auto, geo, false);
        System.err.println("conj: " + auto);
    }

//    @Test
    public void splitlongGeoConj(){
	LambdaTerm geo = p("(argmax \$0 (and (city:t \$0) (exists \$1 (and (state:t \$1) (next_to:t \$1 (argmax \$2 (state:t \$2) (size:i \$2))) (loc:t \$0 \$1)))) (size:i \$0))");
	LambdaTermAlgebra alg = new LambdaTermAlgebra();
	checkAllTermsEquals(alg.decompose(geo), geo, false);
    }

    //@Test
    public void splitGeoAll(){
       
        try {
                LambdaTermAlgebra alg = new LambdaTermAlgebra();
		BufferedReader inter = new BufferedReader(new FileReader("examples/geolambda"));
		String zeile = null;
		while ((zeile = inter.readLine()) != null) {
                        System.out.println(zeile);
			LambdaTerm geo = p(zeile);
                        BottomUpAutomaton auto = alg.decompose(geo);
                        checkAllTermsEquals(auto, geo, false);
		}
	} catch (IOException e) {
		e.printStackTrace();
	} 

      /*  LambdaTermAlgebra alg = new LambdaTermAlgebra();
	LambdaTerm geo = p("(sum \$0 (state:t \$0) (area:i \$0))");
                       BottomUpAutomaton auto = alg.decompose(geo);
                      checkAllTermsEquals(auto, geo, false); */
	//System.out.println("All");
	

        
        //System.out.println(auto.getAllStates());
        //System.err.println(auto.countTrees());
        
	//System.out.println(alg.decompose(geo));
	//checkAllTermsEquals(auto, geo, true);
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

    private void checkAllTermsEquals(BottomUpAutomaton auto, LambdaTerm gold, boolean verbose) {
        LambdaTermAlgebra alg = new LambdaTermAlgebra();
        
        Set<LambdaTerm> reducedSet = new HashSet<LambdaTerm>();
        reducedSet.add(gold.reduce());
        
        Set<LambdaTerm> foundSet = new HashSet<LambdaTerm>();
        
        int i = 1;
        for( Tree<String> t : auto.languageIterable() ) {
            if( verbose ) {
                System.err.println("" + (i++) + ": " + t + " ->");
            }
            
            LambdaTerm eval = alg.evaluate(t);
            if( verbose ) {
                System.err.println("   -> " + eval);
            }
            
            foundSet.add(eval);
        }
        
       assertEquals(reducedSet, foundSet);


    }


}
