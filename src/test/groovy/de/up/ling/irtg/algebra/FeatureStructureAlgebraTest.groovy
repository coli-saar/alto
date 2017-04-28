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
import de.saar.coli.featstruct.FeatureStructure
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
class FeatureStructureAlgebraTest {
    @Test
    public void testProj() {
        FeatureStructureAlgebra alg = new FeatureStructureAlgebra()
        FeatureStructure fs = alg.evaluate(pt("proj_root('[root: [num: sg]]')"))
        
        assertThat(fs, is(FeatureStructure.parse("[num: sg]")))
    }
}

