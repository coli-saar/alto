/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.coarse_to_fine



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
}

