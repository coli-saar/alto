/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class FunctionIterableWithSkipTest {
    /**
     * 
     */
    List<String> input = new ArrayList<>();
    
    @Before
    public void setUp() {
        input.add(null);
        input.add("");
        input.add("");
        input.add(null);
        input.add(null);
        input.add("a");
        input.add("d");
        input.add("a");
        input.add("");
        input.add(null);
        input.add(null);
        input.add("a");
        input.add("a");
        input.add("");
        input.add("a");
        input.add("a");
        input.add("");
        input.add(null);
    }

    /**
     * Test of iterator method, of class FunctionIterableWithSkip.
     */
    @Test
    public void testIterator() {
        Iterable<String> it = new FunctionIterableWithSkip<>(input,(String s) -> {
            if(s == null) {
                throw new RuntimeException();
            }
            
            if(s.isEmpty()) {
                return null;
            } else {
                return s.equals("a") ? "b" : "c";
            }
        });
        
        String[] gold = new String[] {"b","c","b","b","b","b","b"};
        int i = 0;
        for(String s : it) {
            assertEquals(s,gold[i++]);
        }
        
        i = 0;
        for(String s : it) {
            assertEquals(s,gold[i++]);
        }
    }
}
