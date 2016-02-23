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
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*
import de.up.ling.irtg.codec.isiamr.IsiAmrParser;

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.GraphEdge
import de.up.ling.irtg.algebra.graph.GraphNode
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*


import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

class JoshuaInputCodecTest {
 
    @Test
    public void testJoshua() {
        JoshuaInputCodec ic = new JoshuaInputCodec();
        InterpretedTreeAutomaton irtg = ic.read(GRAMMAR);
        System.err.println(irtg)
        
        TreeAutomaton chart = irtg.parse(["left": "wiederaufnahme der sitzungsperiode", "right":"resumption of the session"])
        System.err.println("vit " + chart.viterbi())
        assert chart.viterbi() != null
    }
    
    @Test
    public void testShouldFail() {
        // - grammatik ohne RHS sollte nicht parsen (vit r30)
        // - codec sollte * -> _*_
    }
    
    private static final String GRAMMAR = """\n\
[X] ||| der ||| of the ||| Lex(e|f)=1,38629 Lex(f|e)=0 PhrasePenalty=1 p(e|f)=0 p(f|e)=0
[X] ||| sitzungsperiode ||| session ||| Lex(e|f)=0 Lex(f|e)=0 PhrasePenalty=1 p(e|f)=0 p(f|e)=0
[X] ||| wiederaufnahme ||| resumption ||| Lex(e|f)=0 Lex(f|e)=0 PhrasePenalty=1 p(e|f)=0 p(f|e)=0
[X] ||| wiederaufnahme [X,1] ||| resumption [X,1] ||| Lex(e|f)=0 Lex(f|e)=0 PhrasePenalty=1 p(e|f)=0 p(f|e)=0
[X] ||| [X,1] sitzungsperiode ||| [X,1] session ||| Lex(e|f)=0 Lex(f|e)=0 PhrasePenalty=1 p(e|f)=0 p(f|e)=0

    """;
}