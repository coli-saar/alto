/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg


import org.junit.*
import java.util.*
import java.io.*
import de.saar.penguin.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.saar.penguin.irtg.algebra.*;
import de.saar.penguin.irtg.hom.*;
import static de.saar.penguin.irtg.hom.HomomorphismTest.hom;
import static de.saar.penguin.irtg.util.TestingTools.pt;

/**
 *
 * @author koller
 */
class InterpretedTreeAutomatonTest {
    @Test
    public void testParse() {
        String string = "john watches the woman with the telescope";

        TreeAutomaton rtg = parse("john -> NP\n watches -> V\n" +
            "the -> Det\n woman -> N\n with -> P\n telescope -> N\n" +
            "s(NP,VP) -> S!\n np(Det,N) -> NP\n n(N,PP) -> N\n" +
            "vp(V,NP) -> VP\n vp(VP,PP) -> VP\n pp(P,NP) -> PP"
            );

        String concat = "*(?1,?2)";
        Homomorphism h = hom([
                "john":"john", "watches":"watches", "the":"the", "woman":"woman",
                "with":"with", "telescope":"telescope",
                "s":concat, "np":concat, "n":concat, "vp":concat, "pp":concat
        ]);

        Algebra algebra = new StringAlgebra();

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
    public void testMarco() {
        String grammarstring = '''
interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

r1(S, S) -> S!
  [i] *(?1, ?2)

r2 -> S
  [i] a
        ''';
         InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(grammarstring));

        String string = "a a a";
        List words = irtg.parseString("i", string);
        TreeAutomaton chart = irtg.parseInputObjects(["i": words]);
        chart.makeAllRulesExplicit();

        chart.reduceBottomUp();
        
        assertEquals(new HashSet([pt("r1(r2,r1(r2,r2))"), pt("r1(r1(r2,r2),r2)")]),
                     chart.language());
    }

    @Test
    public void testEM() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        ParsedCorpus pco = irtg.parseCorpus(new StringReader(PCFG_EMTRAIN_STR));
        irtg.trainEM(pco);
        
        assert true;
    }
    
    @Test
    public void testParseCorpus() {
       InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        ParsedCorpus pco = irtg.parseCorpus(new StringReader(PCFG_EMTRAIN_STR));
        assertEquals(3, pco.getAllInstances().size());
    }
    
    @Test
    public void testSerializeParsedCorpus() {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new StringReader(CFG_STR));
        ParsedCorpus pco = irtg.parseCorpus(new StringReader(PCFG_EMTRAIN_STR));
        
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        pco.write(ostream);
        ostream.flush();

        byte[] buf = ostream.toByteArray();

        ByteArrayInputStream istream = new ByteArrayInputStream(buf);
        ParsedCorpus copy = ParsedCorpus.read(istream);
        istream.close();
        
        irtg.trainEM(copy);
//        System.err.println("after EM: " + irtg.getAutomaton());
        
        assert true;
    }

    private static TreeAutomaton parse(String s) {
        return TreeAutomatonParser.parse(new StringReader(s));
    }
    
    private static final String CFG_STR = """
interpretation i: de.saar.penguin.irtg.algebra.StringAlgebra

r1(NP,VP) -> S!
  [i] *(?1,?2)


r4(V,NP) -> VP 
  [i] *(?1,?2)


r5(VP,PP) -> VP
  [i] *(?1,?2)


r6(P,NP) -> PP
  [i] *(?1,?2)


r7 -> NP
  [i] john


r2(Det,N) -> NP
  [i] *(?1,?2)


r8 -> V
  [i] watches


r9 -> Det
  [i] the


r10 -> N
  [i] woman


r11 -> N
  [i] telescope

r3(N,PP) -> N
  [i] *(?1,?2)

r12 -> P
  [i] with



""";
    
    private static final String PCFG_EMTRAIN_STR = """
i
john watches the woman with the telescope
john watches the telescope with the telescope
john watches the telescope with the woman
""";
}

