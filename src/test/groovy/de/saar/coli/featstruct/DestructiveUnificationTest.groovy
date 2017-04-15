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
class DestructiveUnificationTest {
    @Test
    public void testSimple() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg]")
        FeatureStructure fs2 = FeatureStructure.parse("[gen: masc]")
        
        FeatureStructure ret = fs1.destructiveUnify(fs2)
        assertThat("no fail", ret, not(nullValue()))        
        assertThat("correct value", ret, is(FeatureStructure.parse("[num: sg, gen:masc]")))        
    }
    
    @Test
    public void testAtomicFail() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: sg]")
        FeatureStructure fs2 = FeatureStructure.parse("[num: pl]")
        
        FeatureStructure ret = fs1.destructiveUnify(fs2)
        assertThat("fail", ret, nullValue())
    }
    
    @Test
    public void testCoindexFail() {
        FeatureStructure fs1 = FeatureStructure.parse("[num: #1 sg, foo: #1]")
        FeatureStructure fs2 = FeatureStructure.parse("[num: sg, foo: pl]")
        
        FeatureStructure ret = fs1.destructiveUnify(fs2)
        assertThat("fail", ret, nullValue())
    }
    
    @Test
    public void testWroblewskiExample() {
        FeatureStructure fs1 = FeatureStructure.parse("[a: [b: c], d: [e: f]]")
        FeatureStructure fs2 = FeatureStructure.parse("[a: #1 [b: c], d: #1, g: [h: j]]")
        FeatureStructure expected = FeatureStructure.parse("[a: #2 [b: c, e: f], d: #2, g: [h: j]]")
        
        FeatureStructure ret = fs1.destructiveUnify(fs2)
        assertThat("no fail", ret, not(nullValue()))        
        assertThat("correct value", ret, is(expected))
    }
    
    @Test
    public void testJurafskyMartinExample() {
        FeatureStructure fs1 = FeatureStructure.parse("[agreement: #1 [number: sg], subject: [agreement: #1]]")
        FeatureStructure fs2 = FeatureStructure.parse("[subject: [agreement: [person: 3]]]")
        FeatureStructure expected = FeatureStructure.parse("[agreement: #1 [number: sg, person: 3], subject: [agreement: #1]]")
        
        FeatureStructure ret = fs1.destructiveUnify(fs2)
        assertThat("no fail", ret, not(nullValue()))        
        assertThat("correct value", ret, is(expected))
    }
}

