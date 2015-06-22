/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.ptb_tree


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
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
class PtbTreeInputCodecTest {
    @Test
    public void testPierreVinkenTree() {
        String pierre = """
        (S
    (NP-SBJ
      (NP (NNP Pierre) (NNP Vinken) )
      (, ,)
      (ADJP
        (NP (CD 61) (NNS years) )
        (JJ old) )
      (, ,) )
    (VP (MD will)
      (VP (VB join)
        (NP (DT the) (NN board) )
        (PP-CLR (IN as)
          (NP (DT a) (JJ nonexecutive) (NN director) ))
        (NP-TMP (NNP Nov.) (CD 29) )))
    (. .) )""";
        
        Tree gold = pt("""
S(NP-SBJ(NP(NNP(Pierre), NNP(Vinken)), ','(','), ADJP(NP(CD('61'), NNS(years)), JJ(old)), ','(',')),
  VP(MD(will), VP(VB(join), NP(DT(the), NN(board)), PP-CLR(IN(as), NP(DT(a), JJ(nonexecutive), NN(director))),
                  NP-TMP(NNP('Nov.'), CD('29')))),
  '.'('.'))""")
        
        Tree result = new PtbTreeInputCodec().read(pierre)
        
        assertEquals(gold, result)
    }
    
    @Test
    public void testReadTwoTrees() {
        PtbTreeInputCodec c = new PtbTreeInputCodec();
        InputStream is = new ByteArrayInputStream(TWO_TREES.getBytes());
        List trees = c.readCorpus(is);
        
        assert trees.size() == 2;
    }
    
    private static final String TWO_TREES = """
    
( (S 
    (NP-SBJ 
      (NP (NNP Pierre) (NNP Vinken) )
      (, ,) 
      (ADJP 
        (NP (CD 61) (NNS years) )
        (JJ old) )
      (, ,) )
    (VP (MD will) 
      (VP (VB join) 
        (NP (DT the) (NN board) )
        (PP-CLR (IN as) 
          (NP (DT a) (JJ nonexecutive) (NN director) ))
        (NP-TMP (NNP Nov.) (CD 29) )))
    (. .) ))
( (S 
    (NP-SBJ (NNP Mr.) (NNP Vinken) )
    (VP (VBZ is) 
      (NP-PRD 
        (NP (NN chairman) )
        (PP (IN of) 
          (NP 
            (NP (NNP Elsevier) (NNP N.V.) )
            (, ,) 
            (NP (DT the) (NNP Dutch) (VBG publishing) (NN group) )))))
    (. .) ))
    """;
}

