/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.coli.featstruct


import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;


/**
 *
 * @author koller
 */
class FeatureStructureTest {
    
    // Equality checking
    
    @Test
    public void testEqualsSimple() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg, gen: masc]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc, num: sg]")
        
        assertThat(fs1, is(fs2))
        assertThat(fs2, is(fs1))
    }
    
    @Test
    public void testNotEqualsAtomic() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg, gen: masc]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc, num: pl]")
        
        assertThat(fs1, not(is(fs2)))
        assertThat(fs2, not(is(fs1)))
    }
    
    @Test
    public void testNotEqualsPaths() {
        FeatureStructure fs1 = FeatureStructure.parse("[numm: sg, gen: masc]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc, num: sg]")
        
        assertThat(fs1, not(is(fs2)))
        assertThat(fs2, not(is(fs1)))
    }
    
    @Test
    public void testNotEqualsPaths2() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: [foo: sg], gen: masc]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc, num: sg]")
        
        assertThat(fs1, not(is(fs2)))
        assertThat(fs2, not(is(fs1)))
    }
    
    @Test
    public void testNotEqualsCoindexation() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: #1 sg, gen: #1]")
        FeatureStructure fs2 = FeatureStructure.parse("[num: sg, gen: sg]")
        
        assertThat(fs1, not(is(fs2)))
        assertThat(fs2, not(is(fs1)))
    }
    
    @Test
    public void testGetAllPaths() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg, gen: masc]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc, num: sg]")
        
        assertThat(fs1.getAllPaths().toSet(), is([["num"], ["gen"]].toSet()))
        assertThat(fs2.getAllPaths().toSet(), is([["num"], ["gen"]].toSet()))
    }
}

