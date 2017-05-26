/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.coarse_to_fine



import org.junit.Test
import java.util.*
import java.io.*
import java.lang.reflect.Method
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
import de.up.ling.irtg.codec.InputCodec


/**
 *
 * @author koller
 */
class CoarseToFineParserTest {
    @Test
    public void testCoarseToFine() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(GrammarCoarsifierTest.PTB_CTF);
        InterpretedTreeAutomaton irtg = pi(GrammarCoarsifierTest.IRTG);
        CoarseToFineParser ctfp = new CoarseToFineParser(irtg, "i", ftc, 0);
        
        TreeAutomaton chart = ctfp.parse("john watches the woman with the telescope");
        
//        System.err.println(chart.getStateInterner())
//        System.err.println(chart.getSignature())
        
//        System.err.println(chart)
        
        assertEquals(new HashSet([pt("r1(r7,r4(r8,r2(r9,r3(r10,r6(r12,r2(r9,r11))))))"), 
                                    pt("r1(r7,r5(r4(r8,r2(r9,r10)),r6(r12,r2(r9,r11))))")]),
                            chart.language())
    }
    
    @Test
    public void testLoop() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(GrammarCoarsifierTest.PTB_CTF);
        InterpretedTreeAutomaton irtg = pi(LOOP_IRTG);
        CoarseToFineParser ctfp = new CoarseToFineParser(irtg, "i", ftc, 0);
        TreeAutomaton chart = ctfp.parse("hallo");
        
        assert ! chart.language().isEmpty()
    }
    
    @Test
    public void testAsbestosTAG() {
        //this failed previously because CoarseToFIneParserSF returned the rule list in the wrong order
        InterpretedTreeAutomaton irtg = piBin(new FileInputStream("examples/tests/asbestosTAG.irtb"));
        Object input = irtg.parseString("string", "There no asbestos now . ''");
        
        CoarseToFineParser ctfp = CoarseToFineParser.makeCoarseToFineParser(irtg, "string", CHEN_CTF, 0.00001);
        TreeAutomaton sfCtfp = ctfp.parseInputObjectWithSF(input);
        TreeAutomaton basicCtfp = ctfp.parseInputObject(input);
            
        String gold = "t167-sort_br12767(t615-judge_br5205(t327-There_br17463(*NOP*_EX_A_br10,*NOP*_NP_A_br34),'t1527-%_br563'(t167-asbestos_br13168(*NOP*_N_A_br14,t1-no_br25538(*NOP*_D_A_br21,*NOP*_NP_A_br34)),t24-further_br3949(t69-now_br4004(*NOP*_Ad_A_br3,*NOP*_AdvP_A_br2),*NOP*_VP_A_br12))),'t26-._br4744'(\"t91-''_br4621\"(\"*NOP*_''_A_br8\",'*NOP*_._A_br28'),*NOP*_S_A_br0))";
        assert basicCtfp.viterbi().toString().equals(gold);
        assert sfCtfp.viterbi().toString().equals(gold);
    }
    
    public static String LOOP_IRTG = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(XX)
  [i] ?1\n\

XX -> r3(YY)\n\
  [i] ?1\n\

YY -> r2\n\
  [i] hallo
""";
    
    /**
     * Tests whether the sentence "IN" is parsed correctly with the WSJ00 grammar.
     * This uses the loopy rules TOP -> X -> IN, and thus exercises the loopy part
     * of the CondensedCoarsestParser. Also, the test originally failed because
     * the StateInterner was not reset to non-trusting in the BinaryIrtgInputCodec.
     * As a consequence, the parser would not find the correct entries for a state
     * in the RuleRefinementTree's coarse-rules trie.
     * 
    **/
    @Test
    public void testPtb() {
        InputCodec<InterpretedTreeAutomaton> ic = InputCodec.getInputCodecByNameOrExtension("g10.irtb", null);
        InterpretedTreeAutomaton irtg = ic.read(rs("g10.irtb"));
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(GrammarCoarsifierTest.PTB_CTF);
//        CoarseToFineParser.DEBUG = true;
        CoarseToFineParser ctfp = new CoarseToFineParser(irtg, "string", ftc, 0);
        
//        CondensedCoarsestParser.DEBUG = true;
        TreeAutomaton chart = ctfp.parse("IN");
//        CoarseToFineParser.DEBUG = false;
//        CondensedCoarsestParser.DEBUG = false;
        
//        System.err.println(chart);
        
        Iterator it = chart.languageIteratorRaw();
        assert it.hasNext();
    }
    
    
    private static final String CHEN_CTF = """___(
  TOP_S(
    TOP_S(
      TOP_S(
        TOP_S))),
  TOP_A(
    TOP_A(
      TOP_A(
        TOP_A))),
  P_S(
    HP_S(
      S__S(
        S_S,
        VP_S,
        SQ_S,
        SBAR_S,
        SBARQ_S,
        SINV_S),
      N__S(
        NP_S,
        NAC_S,
        NX_S,
        LST_S,
        X_S,
        UCP_S,
        FRAG_S)),
    MP_S(
      A__S(
        ADJP_S,
        QP_S,
        CONJP_S,
        ADVP_S,
        AdvP_S,
        INTJ_S,
        PRN_S,
        PRT_S),
      P__S(
        PP_S,
        RRC_S,
        WHADJP_S,
        WHADVP_S,
        WHNP_S,
        WHPP_S))),
  P_A(
    HP_A(
      S__A(
        S_A,
        VP_A,
        SQ_A,
        SBAR_A,
        SBARQ_A,
        SINV_A),
      N__A(
        NP_A,
        NAC_A,
        NX_A,
        LST_A,
        X_A,
        UCP_A,
        FRAG_A)),
    MP_A(
      A__A(
        ADJP_A,
        QP_A,
        CONJP_A,
        ADVP_A,
        AdvP_A,
        INTJ_A,
        PRN_A,
        PRT_A),
      P__A(
        PP_A,
        RRC_A,
        WHADJP_A,
        WHADVP_A,
        WHNP_A,
        WHPP_A))))
    """
    
}

