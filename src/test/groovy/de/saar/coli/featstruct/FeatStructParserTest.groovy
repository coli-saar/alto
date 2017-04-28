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
class FeatStructParserTest {
    @Test
    public void testSimple() {
        FeatureStructure fs = FeatureStructure.parse("[num: sg, gen: masc]")
        assertThat(fs, instanceOf(AvmFeatureStructure.class))
        
        AvmFeatureStructure afs = (AvmFeatureStructure) fs
        assertThat(afs.get("num").getValue(), is("sg"))
        assertThat(afs.get("gen").getValue(), is("masc"))
        
        assertThat(afs.get("foo"), nullValue())
        assertThat(afs.get("num", "gen"), nullValue())
    }
    
    @Test
    public void testPrimitiveInt() {
        FeatureStructure fs = FeatureStructure.parse("17")        
        assertThat(fs, instanceOf(PrimitiveFeatureStructure.class))        
        assertThat(fs.getValue(), is(17))
    }
    
    @Test
    public void testPrimitiveString() {
        FeatureStructure fs = FeatureStructure.parse("sg")
        assertThat(fs, instanceOf(PrimitiveFeatureStructure.class))        
        assertThat(fs.getValue(), is("sg"))
    }
    
    @Test
    public void testComplex() {
        FeatureStructure fs = FeatureStructure.parse('[num: sg, ggggg: #hallo, gen: "masc foo", bar: #hallo [test: #test 17], baz: #test]');
        
        assertThat("gen", fs.get("gen").getValue(), is("masc foo"))
        
        // value equality
        assertThat("bar test", fs.get("bar", "test").getValue(), is(17))
        assertThat("baz", fs.get("baz").getValue(), is(17))
        assertThat("ggggg / test", fs.get("ggggg").get("test").getValue(), is(17))
        
        // node identity
        assertThat(fs.get("bar", "test"), sameInstance(fs.get("baz")))
        assertThat(fs.get("ggggg"), sameInstance(fs.get("bar")))
    }
    
    @Test
    public void testPlaceholder() {
        FeatureStructure fs = FeatureStructure.parse('[num: #1, gen: #1]');
        
        assertThat(fs.get("num"), instanceOf(PlaceholderFeatureStructure.class))
        assertThat(fs.get("num").getIndex(), is("1"))
        assertThat(fs.get("num"), sameInstance(fs.get("gen")))
    }
}

