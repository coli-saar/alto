/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.codec.SgraphAmrOutputCodec
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.algebra.graph.*;
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class AmrCodecsTest {
    @Test
    public void encodeThenParse() {
        SGraph x = new IsiAmrInputCodec().read("   (w_5 / want  :ARG0 (subj_6 / boy)  :ARG1 (vcomp_7 / believe  :ARG0 (obj_8 / girl)  :ARG1 (xcomp_6_3 / like  :ARG0 (subj_5_2_4 / boy)  :ARG1 (obj_6_3_5 / girl))))");
        String s = new SgraphAmrOutputCodec().asString(x);
        SGraph y = new IsiAmrInputCodec().read(s);
        
        assertEquals(y, x)
    }
}

