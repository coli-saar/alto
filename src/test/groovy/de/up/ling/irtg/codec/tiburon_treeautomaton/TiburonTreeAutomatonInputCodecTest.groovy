/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tiburon_treeautomaton


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
class TiburonTreeAutomatonInputCodecTest {
    TiburonTreeAutomatonInputCodec codec = new TiburonTreeAutomatonInputCodec();
    
    @Test
    public void testWrtg2() {
        TreeAutomaton auto = codec.read(wrtg2);
        assert auto.accepts(pt("S(John, likes, candy)"))
        assert auto.accepts(pt("S(Stacy, hates, candy)"))
    }
    
    private final static String wrtg2 = '''\n\
%% Filename wrtg2 %%\n\
q\n\
q -> S(subj vb obj) # 0.8\n\
q -> S(subj hates obj) # 0.2\n\
subj -> John # 0.7\n\
subj -> Stacy # 0.4\n\
obj -> candy\n\
vb -> likes # 0.4\n\
vb -> hates # 0.6
    ''';
}

