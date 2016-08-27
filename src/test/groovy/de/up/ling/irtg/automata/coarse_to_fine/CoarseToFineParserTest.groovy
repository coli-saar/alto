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
class CoarseToFineParserTest {
    @Test
    public void testCoarseToFine() {
        FineToCoarseMapping ftc = GrammarCoarsifier.readFtcMapping(GrammarCoarsifierTest.PTB_CTF);
        InterpretedTreeAutomaton irtg = pi(GrammarCoarsifierTest.IRTG);
        CoarseToFineParser ctfp = new CoarseToFineParser(irtg, "i", ftc, 0);
        
        TreeAutomaton chart = ctfp.parse(["i": "john watches the woman with the telescope"]);
        System.err.println(chart)
    }
}

