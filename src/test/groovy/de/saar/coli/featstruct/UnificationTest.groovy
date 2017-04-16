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
class UnificationTest {
    @Test
    public void testSimple() {
        testShouldUnify("[num: sg]",
                        "[gen: masc]",
                        "[num: sg, gen:masc]"
        )
    }
    
    @Test
    public void testAtomicFail() {
        testShouldNotUnify(
            "[num: sg]",
            "[num: pl]"
        )
    }
    
    @Test
    public void testCoindexFail() {
        testShouldNotUnify(
            "[num: #1 sg, foo: #1]",
            "[num: sg, foo: pl]"
        )
    }
    
    @Test
    public void testWroblewskiExample() {
        testShouldUnify(
            "[a: [b: c], d: [e: f]]",
            "[a: #1 [b: c], d: #1, g: [h: j]]",
            "[a: #2 [b: c, e: f], d: #2, g: [h: j]]"
        )
    }
    
    @Test
    public void testJurafskyMartinExample() {
        testShouldUnify(
            "[agreement: #1 [number: sg], subject: [agreement: #1]]",
            "[subject: [agreement: [person: 3]]]",
            "[agreement: #1 [number: sg, person: 3], subject: [agreement: #1]]"
        )
    }
    
    @Test
    public void testTomabechiExample() {
        testShouldUnify(
            "[a: s, b: #1]",
            "[a: #2, b: #2, c: t]",
            "[a: #3 s, b: #3, c: t]"
        )
    }
    
    
    
    
    
    
    private void testShouldNotUnify(String sfs1, String sfs2) {
        FeatureStructure fs1 = FeatureStructure.parse(sfs1)
        FeatureStructure fs2 = FeatureStructure.parse(sfs2)
        
        assertThat("fail", fs1.unify(fs2), nullValue())
        assertThat("fs1 unmodified", fs1, is(FeatureStructure.parse(sfs1)))
        assertThat("fs2 unmodified", fs2, is(FeatureStructure.parse(sfs2)))
    }
    
    
    private FeatureStructure testShouldUnify(String sfs1, String sfs2, String sExpected) {
        FeatureStructure fs1 = FeatureStructure.parse(sfs1)
        FeatureStructure fs2 = FeatureStructure.parse(sfs2)
        FeatureStructure expected = FeatureStructure.parse(sExpected)
        
        
        FeatureStructure ret = fs1.unify(fs2)
        assertThat("no fail", ret, not(nullValue()))
        
        assertThat("correct value", ret, is(expected))
        
        assertThat("fs1 unmodified", fs1, is(FeatureStructure.parse(sfs1)))
        assertThat("fs2 unmodified", fs2, is(FeatureStructure.parse(sfs2)))
        
        return ret
    }
}

