/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class BolinasHrgInputCodecTest {
    
    
    private final static String LINE =
            "N_0_root_1 -> (. :N_0_0$  :ARG0 (. :N_1_1$ ) :N_2$ (. :sheep ) ); 0.002\n";
    
    @Before
    public void setUp() {
    }

    /**
     * Test of create method, of class BolinasHrgInputCodec.
     */
    @Test
    public void testCreate() throws Exception {
        InterpretedTreeAutomaton ita = BolinasHrgInputCodec.create(LINE);
        
        Homomorphism  hom = ita.getInterpretation("Graph").getHomomorphism();
        
        for(Tree<String> s : ita.getAutomaton().languageIterable())
        {
            System.out.println(s);
        }
    }
    
}
