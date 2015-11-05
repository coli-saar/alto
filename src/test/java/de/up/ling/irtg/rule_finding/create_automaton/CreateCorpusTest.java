/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.create_automaton;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.tree.Tree;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class CreateCorpusTest {
    
    /**
     * 
     */
    private CorpusCreator<List<String>,Tree<String>> cc;
    
    /**
     * 
     */
    private StringAlgebra sal;
    
    /**
     * 
     */
    private MinimalTreeAlgebra mta;
    
    
    @Before
    public void setUp() {
        CorpusCreator.Factory fact;
        
        
        //TODO
    }

    /**
     * Test of getAlgebra2 method, of class CreateCorpus.
     * @throws de.up.ling.irtg.algebra.ParserException
     */
    @Test
    public void testGetAlgebra2() throws ParserException {
        //TODO
    }
}