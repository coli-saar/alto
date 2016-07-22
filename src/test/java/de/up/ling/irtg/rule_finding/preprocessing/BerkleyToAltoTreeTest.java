/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class BerkleyToAltoTreeTest {
    /**
     * 
     */
    private final static String TEST_STRING = "( (S (@S (@S (@S (PP (IN After)"
            + " (NP (DT the) (NN race))) (, ,)) (NP (@NP (NNP Fortune) (CD 500))"
            + " (NNS executives))) (VP (VBD drooled) (PP (IN like) (NP (NP (NNS schoolboys))"
            + " (PP (IN over) (NP (@NP (@NP (DT the) (NNS car's)) (CC and)) (NNS drivers))))))) (. .)) )";

    /**
     * Test of convert method, of class BerkleyToAltoTree.
     */
    @Test
    public void testConvert() {
        String s = BerkleyToAltoTree.convert(TEST_STRING);
        
        assertEquals(s,"( 'S'( '@S'( '@S'( '@S'( 'PP'( 'IN'( 'After' ) 'NP'( 'DT'( 'the' ) 'NN'( 'race' ) ) ) ','( ',' ) ) 'NP'( '@NP'( 'NNP'( 'Fortune' ) 'CD'( '500' ) ) 'NNS'( 'executives' ) ) ) 'VP'( 'VBD'( 'drooled' ) 'PP'( 'IN'( 'like' ) 'NP'( 'NP'( 'NNS'( 'schoolboys' ) ) 'PP'( 'IN'( 'over' ) 'NP'( '@NP'( '@NP'( 'DT'( 'the' ) 'NNS'( 'car__QUOTE__s' ) ) 'CC'( 'and' ) ) 'NNS'( 'drivers' ) ) ) ) ) ) ) '.'( '.' ) ) )");
    }   
}
