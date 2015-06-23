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
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class TreeAutomatonInputCodecTest {
    InputCodec codec = new TreeAutomatonInputCodec();
    
    @Test
    public void testFta() {
        String str = '''S! -> f(A,B)\n\
        B -> b\n\
        A -> c\n\
        A -> g(D)\n\
        D -> d
\n\
        ''';
        
        TreeAutomaton fta = codec.read(str);
        assertEquals(new HashSet([pt("f(c,b)"), pt("f(g(d),b)")]), fta.language())
    }
}

