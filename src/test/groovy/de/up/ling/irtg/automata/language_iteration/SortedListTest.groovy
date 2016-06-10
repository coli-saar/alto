/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class SortedListTest {
    @Test
    public void testSorted() {
        SortedList s = new SortedList();
        s.add("a");
        s.add("b");
        
        assert ! s.isSortingRequired()
        
        assertEquals("a", s.get(0))
        assertEquals("b", s.get(1))
    }
    
    @Test
    public void testUnsorted() {
        SortedList s = new SortedList();
        s.add("b");
        s.add("a");
        
        assert s.isSortingRequired()
        
        assertEquals("a", s.get(0))
        assertEquals("b", s.get(1))
        
        assert ! s.isSortingRequired()
    }
    
    @Test
    public void testSortingTwice() {
        SortedList s = new SortedList();
        s.add("c");
        s.add("a");
        
        assert s.isSortingRequired()
        
        assertEquals("a", s.get(0))
        assertEquals("c", s.get(1))
        
        assert ! s.isSortingRequired()
        
        s.add("b")
        
        assert s.isSortingRequired()

        assertEquals("a", s.get(0))
        assertEquals("b", s.get(1))
        assertEquals("c", s.get(2))

    }
}

