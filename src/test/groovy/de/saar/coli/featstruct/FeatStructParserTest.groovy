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
        assertThat(afs.getValue("num"), is("sg"))
        assertThat(afs.getValue("gen"), is("masc"))
        
        assertThat(afs.getValue("foo"), nullValue())
        assertThat(afs.getValue("num", "gen"), nullValue())
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
        
        assertThat(fs.getValue("gen"), is("masc foo"))
        
        // value equality
        assertThat(fs.getValue("bar", "test"), is(17))
        assertThat(fs.getValue("baz"), is(17))
        assertThat(fs.get("ggggg").getValue("test"), is(17))
        
        // node identity
        assertThat(fs.getValue("bar", "test"), sameInstance(fs.getValue("baz")))
        assertThat(fs.getValue("ggggg"), sameInstance(fs.getValue("bar")))
    }
    
    @Test
    public void testPlaceholder() {
        FeatureStructure fs = FeatureStructure.parse('[num: #1, gen: #1]');
        
        assertThat(fs.get("num"), instanceOf(PlaceholderFeatureStructure.class))
        assertThat(fs.get("num").getIndex(), is("1"))
        assertThat(fs.get("num"), sameInstance(fs.get("gen")))
        
        System.err.println(fs)
    }
}

