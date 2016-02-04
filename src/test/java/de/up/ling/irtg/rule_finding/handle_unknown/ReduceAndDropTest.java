/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.handle_unknown;

import de.up.ling.irtg.util.FunctionIterable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ReduceAndDropTest {
    /**
     * 
     */
    private static String[] lines = new String[] {
        "John saw Mary leave the center .",
        "Andrew saw Frank enter the center .",
        "Sam saw John leave the center ."
    };
    
    /**
     * 
     */
    private Iterator<String[]> statSource;
    
    /**
     * 
     */
    private Stream<String[]> inputs;
   
    
    @Before
    public void setUp() {
        Stream<String> s1 = Arrays.stream(lines);
        inputs = s1.map((String in) -> in.trim().split("\\s+"));
        
        Iterable<String> it = Arrays.asList(lines);
        statSource = (new FunctionIterable<>(it,(String s) -> s.trim().split("\\s+"))).iterator();
    }

    /**
     * Test of getReduction method, of class ReduceAndDrop.
     */
    @Test
    public void testGetReduction() {
        ReduceAndDrop rad = new ReduceAndDrop(2, (String) -> "unknown",
                (String known) -> known.substring(0,Math.max(Math.min(known.length(), 3), known.length()-2)));
        Function<String[],String> funct = rad.getReduction(statSource);
        
        Set<String> seen = new HashSet<>();
        inputs.map(funct).forEach((String s) -> seen.add(s));
        
        assertEquals(seen.size(),3);
        assertTrue(seen.contains("Joh saw unknown lea the cent ."));
        assertTrue(seen.contains("unknown saw unknown unknown the cent ."));
        assertTrue(seen.contains("unknown saw Joh lea the cent ."));
    }
}
