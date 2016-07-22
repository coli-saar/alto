/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.grammar_post;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class StringifyTest {
    /**
     * 
     */
    public static String GRAMMAR = "/* Example tree-to-string transducer from Galley et al. 04 */\n"
            + "\n"
            + "interpretation french: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "interpretation english: de.up.ling.irtg.algebra.TreeAlgebra\n"
            + "\n"
            + "S! -> r1(NP,VB)\n"
            + "[french]  *(*(*(?1, ne), ?2), pas)\n"
            + "[english] s(?1, vp(aux(does), rb(not), ?2))\n"
            + "\n"
            + "NP -> r2\n"
            + "[french]  il\n"
            + "[english] np(prp(he))\n"
            + "\n"
            + "VB -> r3\n"
            + "[french]  va\n"
            + "[english] vb(go)";

    /**
     * 
     */
    private InterpretedTreeAutomaton ita;
    
    @Before
    public void setUp() throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();
        
        ita = iic.read(new ByteArrayInputStream(GRAMMAR.getBytes()));
    }

    /**
     * Test of addStringInterpretation method, of class Stringify.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testAddStringInterpretation() throws ParserException {
        Stringify.addStringInterpretation(ita, "english", "engString", Pattern.compile("o$"));
        
        Map<String,String> m = new HashMap<>();
        m.put("engString", "he does not g");
        
        TreeAutomaton ta = ita.parse(m);
        
        assertEquals(ita.interpret(ta.viterbi(), "french").toString(),"[il, ne, va, pas]");
    }
}
