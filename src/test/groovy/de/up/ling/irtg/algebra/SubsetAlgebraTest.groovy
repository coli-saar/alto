/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra


import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*


class SubsetAlgebraTest {
    @Test
    public void testParseStringSet() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) | sleep(e,r1)")
        assertThat(s, is(new HashSet(["rabbit(r1)", "sleep(e,r1)"])))
    }
    
    @Test
    public void testEvaluate() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) | sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        Set result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'sleep(e,r1)')"))
        assertThat(result, is(s))
    }
    
    @Test
    public void testEvaluateNotDisjoint() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) | sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        Set result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'rabbit(r1) | sleep(e,r1)')"))
        assertThat(result, nullValue())
    }
    
    @Test
    public void testEvaluateNotElement() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) | sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        Set result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'sleep(e,r2)')"))
        assertThat(result, nullValue())
    }
    
    @Test
    public void testParseEmptySetConstant() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) | sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        Set result = a.evaluate(TreeParser.parse("EMPTYSET"))
        
        assertThat(result, is(empty()))
    }
    
    @Test
    public void testParseEmptySet() {
        Set s = SubsetAlgebra.parseStringSet("")
        assertThat(s, is(empty()))
    }
}