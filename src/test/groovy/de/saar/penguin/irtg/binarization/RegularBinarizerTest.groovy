/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.binarization


import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import static de.saar.penguin.irtg.util.TestingTools.pt;
import static de.saar.penguin.irtg.util.TestingTools.ptv;
import de.up.ling.tree.Tree
import de.saar.basic.StringOrVariable
import de.saar.penguin.irtg.automata.TreeAutomaton

/**
 *
 * @author koller
 */
class RegularBinarizerTest {
    @Test
    public void testStringBinarizationAutomaton() {
        RegularBinarizer bin = new StringAlgebraBinarizer();
        TreeAutomaton auto = bin.binarize("f", 3);
        
        Set gold = new HashSet([pt("*(*(_1,_2),_3)"), pt("*(_1,*(_2,_3))")])
        assertEquals(gold, new HashSet(auto.language()))
    }
    
    @Test
    public void testStringBinarizerShallow() {
        RegularBinarizer bin = new StringAlgebraBinarizer();
        Tree<StringOrVariable> term = ptv("f(?1,?2,?3)");
        TreeAutomaton<String> auto = bin.binarizeWithVariables(term);
        
        System.err.println(auto);
        
        Set gold = new HashSet([pt("*(*('?1','?2'),'?3')"), pt("*('?1',*('?2','?3'))")])
        assertEquals(gold, new HashSet(auto.language()))
    }
    
//    @Test
//    public void 
}

