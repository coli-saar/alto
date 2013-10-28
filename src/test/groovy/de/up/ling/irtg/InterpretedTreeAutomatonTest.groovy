/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg


import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class InterpretedTreeAutomatonTest {
    @Test
    public void testParse() {
        String string = "john watches the woman with the telescope";

        TreeAutomaton rtg = parse("NP -> john \n V -> watches \n" +
            "Det -> the \n N -> woman \n P -> with \n N -> telescope \n" +
            "S! -> s(NP,VP)\n NP -> np(Det,N)\n N -> n(N,PP)\n" +
            "VP -> vp(V,NP)\n VP -> vp(VP,PP)\n PP -> pp(P,NP)"
        );

        String concat = "*(?1,?2)";
        Algebra algebra = new StringAlgebra();


        Homomorphism h = hom([
                "john":"john", "watches":"watches", "the":"the", "woman":"woman",
                "with":"with", "telescope":"telescope",
                "s":concat, "np":concat, "n":concat, "vp":concat, "pp":concat
            ], rtg.getSignature(), algebra.getSignature());
        
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(rtg);
        irtg.addInterpretation("string", new Interpretation(algebra, h));

        List words = irtg.parseString("string", string);
        
        
        TreeAutomaton chart = irtg.parseInputObjects(["string": words]);
        chart.makeAllRulesExplicit();
        
        assertEquals(new HashSet([pt("s(john,vp(watches,np(the,n(woman,pp(with,np(the,telescope))))))"),
                    pt("s(john,vp(vp(watches,np(the,woman)),pp(with,np(the,telescope))))")]),
            chart.language());
    }
    
    @Test
    public void testDecode() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(SCFG_STR));
        Map inputs = ["german":"hans betrachtet die frau mit dem fernrohr"]
        Set decoded = irtg.decode("english", inputs);
        
        assertEquals(decoded, new HashSet([["john", "watches", "the", "woman", "with", "the", "telescope"]]))
    }

    @Test
    public void testMarco() {
        String grammarstring = '''
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(S, S)
  [i] *(?1, ?2)

S -> r2 
  [i] a
        ''';

        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));

        String string = "a a a";
        List words = irtg.parseString("i", string);
        TreeAutomaton chart = irtg.parseInputObjects(["i": words]);
        chart.makeAllRulesExplicit();

        chart.reduceTopDown();
        
        assertEquals(new HashSet([pt("r1(r2,r1(r2,r2))"), pt("r1(r1(r2,r2),r2)")]),
            chart.language());
    }
    
    
    @Test
    public void testEM() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        Corpus corpus = irtg.readCorpus(new StringReader(PCFG_EMTRAIN_STR));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        Charts.computeCharts(corpus, irtg, bos);
        bos.close();
        
        corpus.attachCharts(new Charts(new ByteArrayInputStreamSupplier(bos.toByteArray())));
        
        irtg.trainEM(corpus);
        
        assert true;
    }
    
    
    @Test
    public void testWriteThenParse1() {
        writeThenParse(iparse(CFG_STR))
    }
    
    @Test
    public void testParseAnnotatedCorpus() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        Corpus pco = Corpus.readCorpus(new StringReader(PCFG_MLTRAIN_STR), irtg);
        
        assertEquals(3, pco.getNumberOfInstances());
        Iterator<Instance> it = pco.iterator();
        it.next();
        Instance secondInstance = it.next();
        assertEquals(pti("r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r11))))", irtg.getAutomaton().getSignature()), secondInstance.getDerivationTree());
        assertEquals(["john", "watches", "the", "telescope", "with", "the", "telescope"], secondInstance.getInputObjects().get("i"));
    }
    
    @Test
    public void testML() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        Corpus ac = Corpus.readCorpus(new StringReader(PCFG_MLTRAIN_STR), irtg);
        irtg.trainML(ac);
        
        TreeAutomaton auto = irtg.getAutomaton()
        
        compareWeight("r4", "VP", 0.5, auto);
        compareWeight("r5", "VP", 0.5, auto);
        
        compareWeight("r7", "NP", 1/3.0, auto);
        compareWeight("r2", "NP", 2/3.0, auto);
    }
    
    private Rule fr(int label, int parentState, TreeAutomaton rtg) {
        return rtg.getRulesTopDown(label, parentState).iterator().next();
    }
    
    private void compareWeight(String label, String parentState, double expectedWeight, TreeAutomaton auto) {
        double actualWeight = fr(auto.getSignature().getIdForSymbol(label), auto.getIdForState(parentState), auto).getWeight();
        double diff = Math.abs(actualWeight - actualWeight);
       
        assert diff < 0.0001 : "weight of " + label + " is " + actualWeight;
    }
    
    private void writeThenParse(InterpretedTreeAutomaton irtg) {
        String str = irtg.toString();
        InterpretedTreeAutomaton parsed = iparse(str);
        
        assert irtg.equals(parsed) : parsed;
    }

    private static TreeAutomaton parse(String s) {
        return TreeAutomatonParser.parse(new StringReader(s));
    }
    
    private static InterpretedTreeAutomaton iparse(String s) {
        return IrtgParser.parse(new StringReader(s));
    }
    
    private static final String CFG_STR = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra


S! -> r1(NP,VP)
  [i] *(?1,?2)


NP -> r2(Det,N)
  [i] *(?1,?2)


N -> r3(N,PP)
  [i] *(?1,?2)


VP -> r4(V,NP) [.6]
  [i] *(?1,?2)


VP -> r5(VP,PP) [0.4]
  [i] *(?1,?2)


PP -> r6(P,NP) 
  [i] *(?1,?2)


NP -> r7 
  [i] john


V -> r8 
  [i] watches


Det -> r9
  [i] the


N -> r10
  [i] woman


N -> r11
  [i] telescope


P -> r12
  [i] with



""";
    
    private static final String PCFG_EMTRAIN_STR = """
# IRTG unannotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra

john watches the woman with the telescope
john watches the telescope with the telescope
john watches the telescope with the woman
""";
    
    private static final String PCFG_MLTRAIN_STR = """
# IRTG annotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra

john watches the woman with the telescope
r1(r7,r5( r4(r8, r2(r9,r10)), r6(r12, r2(r9,r11))))
john watches the telescope with the telescope
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r11))))
john watches the telescope with the woman
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r10))))
    """;
    
    private static final String SCFG_STR = """
    
interpretation english: de.up.ling.irtg.algebra.StringAlgebra
interpretation german: de.up.ling.irtg.algebra.StringAlgebra


S! -> r1(NP,VP)
  [english] *(?1,?2)
  [german] *(?1,?2)


NP -> r2(Det,N)
  [english] *(?1,?2)
  [german] *(?1,?2)

N -> r3(N,PP)
  [english] *(?1,?2)
  [german] *(?1,?2)

VP -> r4(V,NP)
  [english] *(?1,?2)
  [german] *(?1,?2)

VP -> r5(VP,PP)
  [english] *(?1,?2)
  [german] *(?1,?2)

PP -> r6(P,NP)
  [english] *(?1,?2)
  [german] *(?1,?2)

NP -> r7
  [english] john
  [german] hans

V -> r8
  [english] watches
  [german] betrachtet

Det -> r9
  [english] the
  [german] die

Det -> r9b
  [english] the
  [german] dem

N -> r10
  [english] woman
  [german] frau

N -> r11
  [english] telescope
  [german] fernrohr

P -> r12
  [english] with
  [german] mit



""";
    
}

