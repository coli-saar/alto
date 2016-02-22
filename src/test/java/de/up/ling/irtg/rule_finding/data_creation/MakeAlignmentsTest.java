/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class MakeAlignmentsTest {
    /**
     * 
     */
    private static final String TEST_STRING_ALIGNMENTS = "1-1 5-3 9-16\n"
            + "89-2 7-2 2-2 2-3\n"
            + "5-8 9-9 1-1\n"
            + "\n"
            + "2-1 3-4 3-5";
    
    /**
     * Test of makeStringFromStandardAlign method, of class MakeAlignments.
     */
    @Test
    public void testMakeStringFromStandardAlign() throws Exception {
        List<ByteArrayOutputStream> results = new ArrayList<>();
        
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            results.add(baos);
            
            return baos;
        };
        
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_STRING_ALIGNMENTS.getBytes());
        
        MakeAlignments.makeStringFromStandardAlign(bais, supp, false);
        
        assertTrue(results.get(3).toString().trim().isEmpty());
        assertEquals(results.get(1).toString(),"'89-90' ||| 1\n'7-8' ||| 2\n'2-3' ||| 3 4");
        assertEquals(results.size(),5);
        
        results.clear();
        bais = new ByteArrayInputStream(TEST_STRING_ALIGNMENTS.getBytes());
        
        MakeAlignments.makeStringFromStandardAlign(bais, supp, true);
        
        assertTrue(results.get(3).toString().trim().isEmpty());
        assertEquals(results.get(1).toString(),"'3-4' ||| 4\n'2-3' ||| 1 2 3");
        assertEquals(results.size(),5);
    }

    
    /**
     * 
     */
    private static final String TEST_TREES = "a(c,d,e(a))\n"
            + "d(e,e)";
    
    /**
     * 
     */
    private static final String TEST_TREE_ALIGN = "0-2 0-3 2-4\n"
            + "\n";
    
    /**
     * Test of makePreorderTreeFromStandard method, of class MakeAlignments.
     */
    @Test
    public void testMakePreorderTreeFromStandard() throws Exception {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        
        List<ByteArrayOutputStream> results = new ArrayList<>();
        
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            results.add(baos);
            
            return baos;
        };
        
        ByteArrayInputStream trees = new ByteArrayInputStream(TEST_TREES.getBytes());
        ByteArrayInputStream aligns = new ByteArrayInputStream(TEST_TREE_ALIGN.getBytes());
        
        MakeAlignments.makePreorderTreeFromStandard(aligns, trees, supp, false,0);
        
        assertEquals(results.size(),2);
        assertTrue(results.get(1).toString().trim().isEmpty());
        assertEquals(results.get(0).toString().trim(),"0-0-0-1 ||| 3\n0-0-0 ||| 1 2");
        
        results.clear();
        trees = new ByteArrayInputStream(TEST_TREES.getBytes());
        aligns = new ByteArrayInputStream(TEST_TREE_ALIGN.getBytes());
        
        MakeAlignments.makePreorderTreeFromStandard(aligns, trees, supp, true,0);
        
        assertEquals(results.size(),2);
        assertTrue(results.get(1).toString().trim().isEmpty());
        assertEquals(results.get(0).toString().trim(),"0-0-0-1 ||| 1\n0-0-0-2 ||| 2\n0-0-0-2-0 ||| 3");
    }
    
}
