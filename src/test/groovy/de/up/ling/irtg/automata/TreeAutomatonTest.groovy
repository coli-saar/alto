/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.hom.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;
import spock.lang.*

/**
 *
 * @author koller
 */
class TreeAutomatonTest{
    @Test
    public void testIntersection() {
        TreeAutomaton auto1 = parse("q1 -> a\n q2 ! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a  \n p3 -> a");
        TreeAutomaton intersect = auto1.intersect(auto2);

        assertEquals(new HashSet([r(p("q1","p2"), "a", []), r(p("q1", "p3"), "a", [])]), 
            intersect.getRulesBottomUp("a", []));

        assertEquals(new HashSet([r(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")])]), 
            intersect.getRulesBottomUp("f", [p("q1","p2"), p("q1","p3")]));

        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
    }

    @Test
    public void testRun() {
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a\n p3 -> a");
        
        Tree t = pt("f(a,a)");
        assertEquals(new HashSet(["p1"]), auto2.run(t));

        Tree ta = pt("a");
        assertEquals(new HashSet(["p2","p3"]), auto2.run(ta));
    }
    
    @Test
    public void testRunWeights() {
        TreeAutomaton auto = parse("p1! -> f(p2,p3)\n p2 -> a [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(0.24 - auto.getWeight(t)) < 0.001;
    }    
    
    @Test
    public void testRunWeightsSumAtRoot() {
        TreeAutomaton auto = parse("p1! -> f(p2,p3) \n p1! -> f(p2,p2) \n p2 -> a  [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(0.4 - auto.getWeight(t)) < 0.001;
    }
    
    @Test
    public void testRunWeightsSumInMiddle() {
        TreeAutomaton auto = parse("p0! -> g(p1) \n p1 -> f(p2,p3) \n p1 -> f(p2,p2) \n p2 -> a [0.4]\n p3 -> a  [0.6]");
        Tree t = pt("g(f(a,a))");
        
        assert Math.abs(0.4 - auto.getWeight(t)) < 0.001;
    }

    @Test
    public void testRunWeightsNotInLanguage() {
        TreeAutomaton auto = parse("p0! -> g(p1) \n p1 -> f(p2,p3) \n p1 -> f(p2,p2) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(auto.getWeight(t)) < 0.001;
    }

    
    @Test
    public void testInvHom() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0]) );

        TreeAutomaton gold = parse("q13 -> r1(q12,q23) \n q24 -> r1(q23,q34) \n q14! -> r1(q12,q24) \n" +
                "q14 -> r1(q13,q34) \n q13 -> r2(q12,q23) \n q24 -> r2(q23,q34) \n q14! -> r2(q12,q24) \n" +
                "q14 -> r2(q13,q34) \n q12 -> r3  \n q23 -> r4  \n q34 -> r5 ");

        TreeAutomaton pre = rhs.inverseHomomorphism(h);

        // removed temporarily (haha) because new implementation doesn't support top-down yet
//        pre.makeAllRulesExplicit();
//        assertEquals(gold, pre);

        for( Tree t : gold.language() ) {
            assert pre.accepts(t) : "not accepted: " + t
        }
    }
    
    @Test
    public void testInvHomNonlinearBottomUp() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]))
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        // don't do anything here that would trigger computation of top-down rules
        assert pre.accepts(pt("G(A,A)"));
    }
    
    //@Test  -- temporarily disabled because topdown not available TODO XXX
    public void testInvHomNonlinearTopDown() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]))
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit(); // this triggers computing all top-down rules
        
        assert pre.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomNonlinearByIntersection() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]))
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        
        TreeAutomaton left = parse("p2! -> G(p1,p1) \n p1 -> A"); // accepts { G(A,A) }
        TreeAutomaton result = left.intersect(pre)
        
        assert result.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testHom() {
        TreeAutomaton base = parse("q! -> f(q1, q2)\n q -> g(q1, q2)\n q -> h(q1,q2)\n q1 -> a \n q2 -> b ");
        Homomorphism h = hom(["f":"H(F(?1,?2))", "g":"H(F(?1,?2))", "h":"G(?2,?1)", "a":"A", "b":"B"], base.getSignature());
        
        Set gold = new HashSet([pt("H(F(A,B))"), pt("G(B,A)")])
        TreeAutomaton result = base.homomorphism(h)
                
        assertEquals(gold, result.language())
    }
    
    @Test
    public void testHomOneVariable() {
        TreeAutomaton base = parse("q1! -> f(q2) \n q2 -> a");
        TreeAutomaton gold = parse("q1! -> A \n q2 -> A ");
        Homomorphism h = hom(["f": "?1", "a": "A"], base.getSignature())
        
        TreeAutomaton result = base.homomorphism(h)
        
        assertEquals(gold, result)
    }

    @Test
    public void testViterbi() {
        TreeAutomaton auto = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1) [1]\n q! -> f(q1,q2) [1.5]");
        Tree best = auto.viterbi();
        assertEquals(best.toString(), pt("f(a,a)").toString());
    }

    @Test
    public void testInside() {
        TreeAutomaton auto = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        Map inside = auto.inside();
        assertEquals(7.0, inside.get("q"), 0.001);
        assertEquals(2.0, inside.get("q1"), 0.001);
    }

    @Test
    public void testOutside() {
        TreeAutomaton auto = parse("q1 -> a  [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        Map inside = auto.inside();
        Map outside = auto.outside(inside);
        assertEquals(1.0, outside.get("q"), 0.001);
        assertEquals(5.5, outside.get("q1"), 0.001);
    }
    
    @Test
    public void testLanguage() {
        TreeAutomaton auto = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet([pt("f(a,a)"), pt("f(a,b)")]*.toString());
        assertEquals(gold, lang);
    }
    
    @Test
    public void testLanguage2() {
        TreeAutomaton auto = parse("q1 -> a\n q1 -> b\n q2 -> c\n q2 -> d\n q! -> f(q1,q2)\n q! -> g(q1,q2)");
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        assertEquals(gold, lang)
    }
    
    @Test
    public void testLanguageIterator() {
        TreeAutomaton auto = parse("q1 -> a\n q1 -> b\n q2 -> c\n q2 -> d\n q! -> f(q1,q2)\n q! -> g(q1,q2)");
        Set lang = new HashSet();
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        
        for( Tree t : auto.languageIterable() ) {
            lang.add(t.toString())
        }
        
        assertEquals(gold, lang)
    }
    
    @Test
    public void testEmptyLanguageIterator() {
        TreeAutomaton auto = parse("q! -> g(q1,q2)");
        Set lang = new HashSet();
        Set gold = new HashSet();
        
        for( Tree t : auto.languageIterable() ) {
            lang.add(t.toString())
        }
        
        assertEquals(gold, lang)
    }
    
    @Test
    public void testReduce() {
        TreeAutomaton auto = parse("""P.4-5 -> r12 [1.0]
N.6-7 -> r11 [1.0]
N.3-4 -> r10  [1.0]
PP.4-7 -> r6(P.4-5, NP.5-7)  [1.0]
NP.0-1 -> r7 [1.0]
V.1-2 -> r8  [1.0]
S.0-4 -> r1(NP.0-1, VP.1-4) [1.0]
S.0-7! -> r1(NP.0-1, VP.1-7)  [1.0]
Det.2-3 -> r9  [1.0]
Det.5-6 -> r9  [1.0]
NP.5-7 -> r2(Det.5-6, N.6-7) [1.0]
NP.2-4 -> r2(Det.2-3, N.3-4) [1.0]
NP.2-7 -> r2(Det.2-3, N.3-7) [1.0]
N.3-7 -> r3(N.3-4, PP.4-7) [1.0]
VP.1-4 -> r4(V.1-2, NP.2-4) [1.0]
VP.1-7  -> r4(V.1-2, NP.2-7) [1.0]
VP.1-7 -> r5(VP.1-4, PP.4-7) [1.0]""");
        
        Set productiveStates = auto.getProductiveStates();
        Set gold = new HashSet(["NP.0-1", "V.1-2", "Det.2-3", "N.3-4", "P.4-5", "Det.5-6", "N.6-7", "PP.4-7", "S.0-7", "NP.5-7", "NP.2-4", "NP.2-7", "N.3-7", "VP.1-4", "VP.1-7"]);
        assertEquals(gold, productiveStates);        
    }
    
    @Test
    public void testReduceUnreachableFinalStates() {
        TreeAutomaton auto = parse("""q! -> a""");
        auto.addFinalState("qx");
        
        TreeAutomaton red = auto.reduceBottomUp();
        
        assert red.getFinalStates().contains("q");
        assert ! red.getFinalStates().contains("qx");
    }
    
    @Test
    public void testIntersectWeights() {
        TreeAutomaton auto1 = parse("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        Set rulesForA = intersect.getRulesBottomUp("a", [])
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w-0.3) < 0.001) || (Math.abs(w-0.2) < 0.001)) : rulesForA;
        }
        
//        assertEquals( new HashSet([0.4*0.5, 0.6*0.5]), new HashSet(rulesForA.collect { it.getWeight() }) )
    }
    
    @Test
    public void testIntersectWeightsViaExplicit() {
        TreeAutomaton auto1 = parse("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        intersect.makeAllRulesExplicit();        
        Set rulesForA = intersect.getRulesBottomUp("a", [])
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w)-0.3 < 0.001) || (Math.abs(w)-0.2 < 0.001)) : rulesForA;
        }
    }

    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }

    private static TreeAutomaton parse(String s) {
        return TreeAutomatonParser.parse(new StringReader(s));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }
}

