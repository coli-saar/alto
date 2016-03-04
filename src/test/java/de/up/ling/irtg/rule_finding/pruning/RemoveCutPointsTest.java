/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveCutPointsTest {

    /**
     *
     */
    private static final String TEST_RTA = "interpretation i: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "\n"
            + "a! -> r1(b,b) \n"
            + "  [i] *(?1,?2)\n"
            + "\n"
            + "a! -> '__X__{a "+HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER+" b}'(b,b) \n"
            + "  [i] *(?1,?2)\n"
            + "\n"
            + "a! -> '__X__{e "+HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER+" f}'(c,c) \n"
            + "  [i] *(?1,?2)\n"
            + "\n"
            + "b -> x \n"
            + "  [i] x\n"
            + "\n"
            + "b -> '__X__{a "+HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER+" b}'(c,c) \n"
            + "  [i] *(?1,?2)\n"
            + "\n"
            + "b -> '__X__{e "+HomomorphismManager.FINAL_VARIABLE_STATE_DELIMITER+" f}'(c,c) \n"
            + "  [i] *(?1,?2)\n"
            + "\n"
            + "c -> y \n"
            + "  [i] y\n";

    /**
     *
     */
    private InterpretedTreeAutomaton irtg;
    
    /**
     * 
     */
    private final static String LEFT = "\n\n";

    /**
     * 
     */
    private final static String RIGHT = "\nf\n";
    
    /**
     * 
     */
    private InputStream lIn;
    
    /**
     * 
     */
    private InputStream rIn;
    
    @Before
    public void setUp() {
        IrtgInputCodec iic = new IrtgInputCodec();

        irtg = iic.read(TEST_RTA);
        
        rIn = new ByteArrayInputStream(RIGHT.getBytes());
        lIn = new ByteArrayInputStream(LEFT.getBytes());
    }

    /**
     * Test of removeCutPoints method, of class RemoveCutPoints.
     */
    @Test
    public void testRemoveCutPoints() throws IOException {
        Set<String> allowed = RemoveCutPoints.makeRelevantSet(lIn);
        
        InterpretedTreeAutomaton ita = RemoveCutPoints.removeCutPoints(irtg, allowed, false);
        Set<String> s = new HashSet<>();
        for(Tree<String> t : ita.getAutomaton().languageIterable()) {
            s.add(t.toString());
        }
        assertEquals(s.size(),1);
        assertTrue(s.contains("r1(x,x)"));
        
        allowed = RemoveCutPoints.makeRelevantSet(rIn);
        ita = RemoveCutPoints.removeCutPoints(irtg, allowed, true);
        
        s = new HashSet<>();
        for(Tree<String> t : ita.getAutomaton().languageIterable()) {
            s.add(t.toString());
        }
        
        assertEquals(s.size(),5);
    }
}
