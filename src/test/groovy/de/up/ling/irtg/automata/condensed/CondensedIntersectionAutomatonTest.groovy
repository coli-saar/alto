/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.saar.basic.Pair
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.signature.IdentitySignatureMapper
import de.up.ling.irtg.signature.SignatureMapper
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton
import de.up.ling.irtg.automata.condensed.CondensedIntersectionAutomatonTest

import de.up.ling.irtg.automata.index.*

/**
 *
 * @author gontrum
 */
public class CondensedIntersectionAutomatonTest {

    
    
   // @Test
    public void makeAllRulesExplicitTest() {
        String grammarstring = '''
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(A, B)
 [i] *(?1, ?2)

A -> r2 
 [i] a

B -> r3(A, D)
 [i] *(?1, ?2)

D -> r5(A, D) [0.5]
 [i] *(?1, ?2)

D -> r4 [0.5]
 [i] b

C -> r6(X,Y)
 [i] *(?1, ?2)

X -> r7
  [i] x

Y -> r8(X,X)
[i] *(?1, ?2)


       ''';
        // Create an IRTG
        InterpretedTreeAutomaton irtg = pi(new StringReader(grammarstring));
        String toParse = "a a a a b ";
        
        System.out.println("Parsing the String '" + toParse + "' with the IRTG: \n" + irtg.toString());
        
        // Get Homomorphism and Algebra
        Homomorphism h = irtg.getInterpretation("i").getHomomorphism();
        Algebra a = irtg.getInterpretation("i").getAlgebra();
        
        // Create a decomposition of an input String
        TreeAutomaton decomp = a.decompose(a.parseString(toParse));
//        System.out.println("Decomp Automaton for the input string:\n" + decomp.toString());
        
        // Create an (Nondeleting) InverseHomomorphism
        CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(h);
//        System.out.println("Invhom Automaton (condensed):\n" + inv.toStringCondensed());
//        System.out.println("Invhom Automaton :\n" + inv.toString());

//        System.err.println("invhom(decomp(" + input  + "):\n" + inv.toStringBottomUp());
                
        TreeAutomaton<String> result = irtg.getAutomaton().intersectCondensed(inv, h.getSignatureMapper());
    } 
    
    @Test
    public void testIntersection() {
        TreeAutomaton auto1 = pa("q1 -> a\n q2 ! -> f(q1,q1) ");       
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a}  \n p3 -> {a}");
        
        TreeAutomaton intersect = auto1.intersectCondensed(auto2, auto1.getSignature().getMapperTo(auto2.getSignature()));
        intersect.getStateInterner().setTrustingMode(false); // important!
        
        TreeAutomaton.DEBUG_STORE = false;
        GenericCondensedIntersectionAutomaton.DEBUG = false;

        assert intersect.getSignature() == auto1.getSignature();
        
        assertEquals(intersect.getSignature().getSymbolsWithArities(), ["f":2, "a":0]);
     
        assertEquals(new HashSet([rs(p("q1","p2"), "a", [], intersect), rs(p("q1", "p3"), "a", [], intersect)]), 
            rbu("a", [], intersect));

        assertEquals(new HashSet([rs(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")], intersect)]), 
            rbu("f", [p("q1","p2"), p("q1","p3")], intersect));
        
        assertEquals(new HashSet([p("q2","p1")]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it)}));
    }
    
    @Test
    public void testIntersectionLanguage() {
        
        TreeAutomaton auto1 = pa("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b");
        TreeAutomaton auto2 = pac("p1! -> {f}(p2,p2)\n p2 -> {a}");
        
        TreeAutomaton intersect = auto1.intersectCondensed(auto2, auto1.getSignature().getMapperTo(auto2.getSignature()));
        
        assertEquals(new HashSet([pt("f(a,a)")]), intersect.language());
    }
    
    //    @Test
    public void synchronousParsing() {
        InterpretedTreeAutomaton irtg = pi(scfgIRTG);
        String stringinputEnglish = "john watches the woman with the telescope"
        String stringinputGerman = "hans betrachtet die frau mit dem fernrohr"
        
        // create a map for the irtg.parseInputObjects() - method
        Map<String, String> inputMap = new HashMap<String, String>();
        
        inputMap.put("english", stringinputEnglish);
        inputMap.put("german", stringinputGerman);
        
        TreeAutomaton ret = irtg.parse(inputMap);
        
        // now check that it is equal to the given (correct) automaton:
        
        TreeAutomaton compareAutomaton = pa("'Det,2-3,2-3' -> r9 [1.0]\n" +
            "'N,6-7,6-7' -> r11 [1.0]\n" +
            "'P,4-5,4-5' -> r12 [1.0]\n" +
            "'Det,5-6,5-6' -> r9b [1.0]\n" +
            "'V,1-2,1-2' -> r8 [1.0]\n" +
            "'NP,0-1,0-1' -> r7 [1.0]\n" +
            "'N,3-4,3-4' -> r10 [1.0]\n" +
            "'S,0-7,0-7'! -> r1('NP,0-1,0-1', 'VP,1-7,1-7') [1.0]\n" +
            "'N,3-7,3-7' -> r3('N,3-4,3-4', 'PP,4-7,4-7') [1.0]\n" +
            "'NP,5-7,5-7' -> r2('Det,5-6,5-6', 'N,6-7,6-7') [1.0]\n" +
            "'PP,4-7,4-7' -> r6('P,4-5,4-5', 'NP,5-7,5-7') [1.0]\n" +
            "'NP,2-7,2-7' -> r2('Det,2-3,2-3', 'N,3-7,3-7') [1.0]\n" +
            "'NP,2-4,2-4' -> r2('Det,2-3,2-3', 'N,3-4,3-4') [1.0]\n" +
            "'VP,1-4,1-4' -> r4('V,1-2,1-2', 'NP,2-4,2-4') [1.0]\n" +
            "'VP,1-7,1-7' -> r4('V,1-2,1-2', 'NP,2-7,2-7') [1.0]\n" +
            "'VP,1-7,1-7' -> r5('VP,1-4,1-4', 'PP,4-7,4-7') [1.0]"); 
        
        System.out.println(ret);
        System.out.println(compareAutomaton);
        
        System.out.println(ret.equals(compareAutomaton));
        
        // TODO!
        // result and compare are exactly the same automaton, but nevertheless the assertion fails.        
        assertEquals(compareAutomaton, ret);
    }
    
    @Test
    public void testIntersectionFinalStates() {
        TreeAutomaton auto1 = pa("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b\n qqqq! -> xxxx");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p2)\n p2 -> {a}");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2, auto1.getSignature().getMapperTo(auto2.getSignature()));
        
        assertEquals(new HashSet(["q1,p1"]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it).toString() }));
    }
    
    
        // not implemented yet
    //    @Test
//    public void testInvHomNonlinearByIntersection() {
//        TreeAutomaton rhs = pa("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
//        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]), rhs.getSignature());
//        
//        CondensedTreeAutomaton pre = rhs.inverseCondensedHomomorphism(h);
//        
//        TreeAutomaton left = parse("p2! -> G(p1,p1) \n p1 -> A"); // accepts { G(A,A) }
//        TreeAutomaton result = left.intersectCondensed(pre)
//        
//        assert result.accepts(pt("G(A,A)"));
//    }
    
    @Test
    public void testIntersectWeights() {
        TreeAutomaton auto1 = pa("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a} [0.4]\n p3 -> {a} [0.6]");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2, auto1.getSignature().getMapperTo(auto2.getSignature()));
        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w-0.3) < 0.001) || (Math.abs(w-0.2) < 0.001)) : rulesForA;
        }
        
//        assertEquals( new HashSet([0.4*0.5, 0.6*0.5]), new HashSet(rulesForA.collect { it.getWeight() }) )
    }
    
    @Test
    public void testIntersectWeightsViaExplicit() {
        TreeAutomaton auto1 = pa("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        CondensedTreeAutomaton auto2 = pac("p1! -> {f}(p2,p3) \n p2 -> {a} [0.4]\n p3 -> {a} [0.6]");
        TreeAutomaton intersect = auto1.intersectCondensed(auto2, auto1.getSignature().getMapperTo(auto2.getSignature()));
        
        intersect.makeAllRulesExplicit();        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w)-0.3 < 0.001) || (Math.abs(w)-0.2 < 0.001)) : rulesForA;
        }
    }
    
    //    @Test
    public void testBottomUpOrder() {
        CondensedTreeAutomaton a = pac("s03! -> {f}(s01,s13)\n s03 -> {f}(s02,s23)\n s13 -> {f}(s12, s23)\n s02 -> {f}(s01, s12)\n s01 -> {a}\n s12 -> {a}\n s23 -> {a}");
        List states = a.getStatesInBottomUpOrder().collect { a.getStateForId(it)};
        
        assertEquals(["s01", "s12", "s23", "s02", "s13", "s03"], states)
    }
    
    @Test
    public void testUnaryStartRule() {
        CondensedTreeAutomaton cta = pac("q -> {a}\n q! -> {f}(q)")
        TreeAutomaton ta = pa("p1! -> f(p2)\n p2 -> a")
        SignatureMapper smap = ta.getSignature().getMapperTo(cta.getSignature())
        CondensedIntersectionAutomaton inter = new CondensedIntersectionAutomaton(ta, cta, smap)
        
        //System.err.println("*****")
        //inter.DEBUG = true;
        inter.makeAllRulesExplicit()
        
        assertEquals(new HashSet([pt("f(a)")]), inter.language())
        
        CondensedTreeAutomaton con = pac("q -> {t,i}(q1,q2) \n q! -> {a,b}(q)\n q1! -> {g}(q1) \n q1 -> {c}(q2) \n q2 -> {u,h}");
        TreeAutomaton taut = pa("q0! -> a(q0) \n q0! -> b(q0) \n q0! -> t(q1,q2) \n q0! -> i(q1,q2) \n q1 -> c(q2) \n q2 -> u");
        
        smap = taut.getSignature().getMapperTo(con.getSignature());
        CondensedIntersectionAutomaton sect = new CondensedIntersectionAutomaton(taut, con, smap);
        
        sect.makeAllRulesExplicit();
        
        Iterator<Tree<String>> lit = taut.languageIterator();
        
        for(int i=0;i<30 && lit.hasNext();++i){
            Tree<String> t = lit.next();
            System.out.println(t);
            System.out.println(con.accepts(t));
            System.out.println(sect.accepts(t));
        }
    }
    
    // Helping functions
    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }
    
    private static Rule rs(parent, String label, children, TreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }
    
    final private static String scfgIRTG =  "interpretation english: de.up.ling.irtg.algebra.StringAlgebra\n" +
"interpretation german: de.up.ling.irtg.algebra.StringAlgebra\n" +
"\n" +
"\n" +
"S! -> r1(NP,VP)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"\n" +
"NP -> r2(Det,N)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"N -> r3(N,PP)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"VP -> r4(V,NP)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"VP -> r5(VP,PP)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"PP -> r6(P,NP)\n" +
"  [english] *(?1,?2)\n" +
"  [german] *(?1,?2)\n" +
"\n" +
"NP -> r7\n" +
"  [english] john\n" +
"  [german] hans\n" +
"\n" +
"V -> r8\n" +
"  [english] watches\n" +
"  [german] betrachtet\n" +
"\n" +
"Det -> r9\n" +
"  [english] the\n" +
"  [german] die\n" +
"\n" +
"Det -> r9b\n" +
"  [english] the\n" +
"  [german] dem\n" +
"\n" +
"N -> r10\n" +
"  [english] woman\n" +
"  [german] frau\n" +
"\n" +
"N -> r11\n" +
"  [english] telescope\n" +
"  [german] fernrohr\n" +
"\n" +
"P -> r12\n" +
"  [english] with\n" +
"  [german] mit\n";
    

}




