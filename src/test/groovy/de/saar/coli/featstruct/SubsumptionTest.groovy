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

import static FeatureStructure.SUBSUMES_FORWARD;
import static FeatureStructure.SUBSUMES_BACKWARD;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;


/**
 *
 * @author koller
 */
class SubsumptionTest {
    @Test
    public void testSimpleEquals() {
        testSubsumption(
            "[num: sg, foo:bar]",
            "[foo: bar, num:sg]",
            SUBSUMES_FORWARD | SUBSUMES_BACKWARD
        )
    }
    
    @Test
    public void testNotEqualsAtomic() {
        testSubsumption(
            "[num: sg, gen: masc]",
            "[gen: masc, num: pl]",
            0
        )
    }
    
    @Test
    public void testNotEqualsPaths() {
        testSubsumption(
            "[numm: sg, gen: masc]",
            "[gen: masc, num: sg]",
            0
        )
    }
    
    @Test
    public void testNotEqualsPaths2() {
        testSubsumption(
            "[num: [foo: sg], gen: masc]",
            "[gen: masc, num: sg]",
            0
        )
    }
    
    @Test
    public void testNotEqualsCoindexation() {
        testSubsumption(
            "[num: #1 sg, gen: #1]",
            "[num: sg, gen: sg]",
            SUBSUMES_BACKWARD
        )
    }
    
    
    
    
    
    
    private void testSubsumption(String sfs1, String sfs2, int expected) {
        FeatureStructure fs1 = FeatureStructure.parse(sfs1)
        FeatureStructure fs2 = FeatureStructure.parse(sfs2)
        
        int result = fs1.checkSubsumptionBothWays(fs2)        
        assertThat(result, is(expected))
    }
}

