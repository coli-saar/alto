/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class MakeSplitTest {
    
    
    private final static String GOAL_TWO = "Name all the rivers in Colorado .\n" +
                                            "answer(river(loc_2(stateid('colorado'))))\n" +
                                            "3:4:1 4:5:2 5:6:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n" +
                                            "Can you tell me the capital of Texas ?\n" +
                                            "answer(capital(loc_2(stateid('texas'))))\n" +
                                            "1:2:1 4:5:2 5:6:3 7:8:4\n" +
                                            "0-0-0:1 0-0-0-0-0:2 0-0-0-0:3 0-0-0-0-0-0-0:4\n\n";
    
    /**
     * 
     */
    private final static String GOAL_ONE = "Give me the cities in Virginia .\n" +
                                            "answer(city(loc_2(stateid('virginia'))))\n" +
                                            "3:4:1 4:5:2 5:6:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n" +
                                            "What are the high points of states surrounding Mississippi ?\n" +
                                            "answer(high_point_1(state(next_to_2(stateid('mississippi')))))\n" +
                                            "1:2:1 3:4:2 5:6:3 6:7:4 7:8:5 8:9:6\n" +
                                            "0-0-0:1 0-0-0-0:2 0-0-0-0-0-0-0:3 0-0-0-0-0:4 0-0-0-0-0-0:5 0-0-0-0-0-0-0-0:6\n" +
                                            "\n" +
                                            "Name the rivers in Arkansas .\n" +
                                            "answer(river(loc_2(stateid('arkansas'))))\n" +
                                            "2:3:1 3:4:2 4:5:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n"; 
    
    /**
     * 
     */
    private final static String TEST_INPUT = "Give me the cities in Virginia .\n" +
                                            "answer(city(loc_2(stateid('virginia'))))\n" +
                                            "3:4:1 4:5:2 5:6:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n\n" +
                                            "What are the high points of states surrounding Mississippi ?\n" +
                                            "answer(high_point_1(state(next_to_2(stateid('mississippi')))))\n" +
                                            "1:2:1 3:4:2 5:6:3 6:7:4 7:8:5 8:9:6\n" +
                                            "0-0-0:1 0-0-0-0:2 0-0-0-0-0-0-0:3 0-0-0-0-0:4 0-0-0-0-0-0:5 0-0-0-0-0-0-0-0:6\n" +
                                            "\n" +
                                            "Name the rivers in Arkansas .\n" +
                                            "answer(river(loc_2(stateid('arkansas'))))\n" +
                                            "2:3:1 3:4:2 4:5:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n" +
                                            "Name all the rivers in Colorado .\n" +
                                            "answer(river(loc_2(stateid('colorado'))))\n" +
                                            "3:4:1 4:5:2 5:6:3\n" +
                                            "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0-0-0:3\n" +
                                            "\n" +
                                            "Can you tell me the capital of Texas ?\n" +
                                            "answer(capital(loc_2(stateid('texas'))))\n" +
                                            "1:2:1 4:5:2 5:6:3 7:8:4\n" +
                                            "0-0-0:1 0-0-0-0-0:2 0-0-0-0:3 0-0-0-0-0-0-0:4\n" +
                                            "\n\n";

    /**
     * Test of makeSplit method, of class MakeSplit.
     */
    @Test
    public void testMakeSplit() throws Exception {
        InputStream in = new ByteArrayInputStream(TEST_INPUT.getBytes());
        ByteArrayOutputStream one = new ByteArrayOutputStream();
        ByteArrayOutputStream two = new ByteArrayOutputStream();
        String size = "3";
        
        MakeSplit.makeSplit(in, one, two, size);
        
        String s1 = new String(one.toByteArray());
        String s2 = new String(two.toByteArray());
        
        assertEquals(s1,GOAL_ONE);
        assertEquals(s2,GOAL_TWO);
    }    
}
