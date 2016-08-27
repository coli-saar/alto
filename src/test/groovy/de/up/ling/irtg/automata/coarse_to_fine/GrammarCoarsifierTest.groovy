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


/**
 *
 * @author koller
 */
class GrammarCoarsifierTest {

    @Test
    public void testFtc() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(PTB_CTF);
        assert ftc.numLevels() == 4;
        assertEquals("S_", ftc.coarsify("S"))
        assertEquals("TOP", ftc.coarsify("TOP"))
        assertEquals("P", ftc.coarsify("P"))  // "P" is top-level; it coarsifies to itself
    }
    
    @Test
    public void testCoarsify() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(PTB_CTF);
        InterpretedTreeAutomaton irtg = pi(IRTG);
        GrammarCoarsifier gc = new GrammarCoarsifier(ftc);
        RuleRefinementTree rrt = gc.coarsify(irtg, "i");
//        System.err.println(rrt.toString(irtg.getAutomaton()));
        
        assert rrt.getFinalStatesAtLevel(0).contains(irtg.getAutomaton().getIdForState("P"));
        assert ! rrt.getFinalStatesAtLevel(0).contains(irtg.getAutomaton().getIdForState("S"));
        
        RuleRefinementNode n = rrt.getFinestNodeForRule(findRule("r1", irtg.getAutomaton()));
        assert n != null;
    }
    
    @Test
    public void testMakeCoarsestAutomaton() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(PTB_CTF);
        InterpretedTreeAutomaton irtg = pi(IRTG);
        GrammarCoarsifier gc = new GrammarCoarsifier(ftc);
        RuleRefinementTree rrt = gc.coarsify(irtg, "i");
        
        // coarse IRTG should still parse original string (otherwise we may have over-coarsified)
        InterpretedTreeAutomaton coarse = rrt.makeIrtgWithCoarsestAutomaton(irtg);
//        System.err.println(coarse);

        TreeAutomaton chart = coarse.parse(["i": "john watches the woman with the telescope"])
        assert chart.viterbi() != null;        
    }
    
    private Rule findRule(String label, TreeAutomaton auto) {
        for( Rule rule : auto.getRuleSet() ) {
            if( auto.getSignature().resolveSymbolId(rule.getLabel()).equals(label)) {
                return rule;
            }
        }
        
        return null;
    }
    
    public static String IRTG = """\n\
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(NP,VP) 
  [i] *(?1,?2)


VP -> r4(V,NP) [0.6]
  [i] *(?1,?2)


VP -> r5(VP,PP) [0.4]
  [i] *(?1,?2)


PP -> r6(Prep,NP)
  [i] *(?1,?2)


NP -> r7 [0.5]
  [i] john


NP -> r2(Det,N) [0.5]
  [i] *(?1,?2)


V -> r8
  [i] watches


Det -> r9
  [i] the


N -> r10 [0.3]
  [i] woman


N -> r11 [0.3]
  [i] telescope

N -> r3(N,PP) [0.4]
  [i] *(?1,?2)

Prep -> r12
  [i] with




""";
    
    public static String PTB_CTF = """
    ___(
TOP(TOP(TOP(TOP))),

P(
  HP(
    S_(
      S,
      VP,
      SQ,
      SBAR,
      SBARQ,
      SINV
    ),
    N_(
      NP,
      NAC,
      NX,
      LST,
      X,
      UCP,
      FRAG
    )
  ),
  MP(
    A_(
      ADJP,
      QP,
      CONJP,
      ADVP,
      INTJ,
      PRN,
      PRT
    ),
    P_(
      PP,
      RRC,
      WHADJP,
      WHADVP,
      WHNP,
      WHPP
    )
  )
)
)

    """
}

