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
public class ExtractLinesforFastAlignTest {
    
    /**
     * 
     */
    private final static String TEST_INPUT = "\n\n877\n" +
                        "Which states have points that are higher than the highest point in Texas ?\n" +
                        "answer(A,(state(A),loc(B,A),higher(B,C),highest(C,(place(C),loc(C,D),const(D,stateid(texas))))))\n" +
                        "answer(state(loc_1(place(higher_2(highest(place(loc_2(stateid('texas')))))))))\n" +
                        "\n" +
                        "878\n" +
                        "Which states lie on the largest river in the United States ?\n" +
                        "answer(A,(state(A),traverse(B,A),longest(B,(river(B),loc(B,C),const(C,countryid(usa))))))\n" +
                        "answer(state(traverse_1(longest(river(loc_2(countryid('usa')))))))\n" +
                        "\n" +
                        "879\n" +
                        "Which US city has the highest population density ?\n" +
                        "answer(A,largest(B,(city(A),density(A,B))))\n" +
                        "answer(largest_one(density_1(city(all))))\n" +
                        "\n\n";

    
    private final static String GOAL =  "Which states have points that are higher than the highest point in Texas ? ||| answer state loc_1 place higher_2 highest place loc_2 stateid 'texas'\n" +
                                        "Which states lie on the largest river in the United States ? ||| answer state traverse_1 longest river loc_2 countryid 'usa'\n" +
                                        "Which US city has the highest population density ? ||| answer largest_one density_1 city all";
    
    /**
     * Test of getQueryFunql method, of class ExtractLinesforFastAlign.
     */
    @Test
    public void testGetQueryFunql() throws Exception {
        InputStream  in = new ByteArrayInputStream(TEST_INPUT.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        ExtractLinesforFastAlign.getGeoQueryFunql(1, 3, in, out);
        
        in.close();
        out.close();
        
        String s = new String(out.toByteArray());
        assertEquals(s,GOAL);
    }
}
