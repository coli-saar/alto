/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class GetAllNodesMapTest {
    /**
     * 
     */
    private final static String TEST_TREE_1 =
            "answer(count(intersection(city(cityid('austin', _)), loc_2(countryid('usa')))))";
    
    /**
     * 
     */
    private final static String TEST_TREE_2 =
            "answer(count(exclude(river(all), traverse_2(state(loc_1(capital(cityid('albany', _))))))))";
    
    /**
     * 
     */
    private List<String> trees;
    
    @Before
    public void setUp() {
        trees = new ArrayList<>();
        
       trees.add(TEST_TREE_1);
       trees.add(TEST_TREE_2);
    }

    /**
     * Test of getCoreDescriptions method, of class GetAllNodesMap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetCoreDescriptions() throws Exception {
        Map<String, Set<String>>[] result = GetAllNodesMap.getCoreDescriptions(trees);
        
        assertEquals(result.length,3);
        assertEquals(result[0].size(),result[1].size());
        assertEquals(result[2].size(),result[1].size());
        assertEquals(result[2].size(),18);
        
        Set<String> sintersect = result[0].get("intersection");
        assertEquals(sintersect.size(),2);
        assertTrue(sintersect.contains("city"));
        assertTrue(sintersect.contains("loc_2"));
        
        sintersect = result[1].get("intersection");
        assertEquals(sintersect.size(),1);
        assertTrue(sintersect.contains("count"));
        
        sintersect = result[2].get("intersection");
        assertEquals(sintersect.size(),1);
        assertTrue(sintersect.contains("2"));
    }
    
}
