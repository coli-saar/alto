/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*;
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*

import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;


/**
 *
 * @author koller
 */
class SGraphTest {
    @Test
    public void testIso() {
        SGraph g1 = IsiAmrParser.parse(new StringReader("(v / want-01  :ARG0 (b)  :ARG1 (g))"));        
        SGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertEquals(g1, g2);
    }
    
    @Test
    public void testIso2() {
        SGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-02  :ARG0 (b)  :ARG1 (g))"));
        SGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertThat(g1, is(not(g2)))
    }
    
    @Test
    public void testIso3() {
        SGraph g1 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (w)  :ARG1 (g))"));
        SGraph g2 = IsiAmrParser.parse(new StringReader("(w / want-01  :ARG0 (b)  :ARG1 (g))"));
        assertThat(g1, is(not(g2)))
    }
    
    @Test
    public void testDisjointMerge() {
        SGraph g1 = pg("(w<root> / want-01  :ARG0 (b<left>)  :ARG1 (g<right>))")
        SGraph g2 = pg("(bb<left> :ARG0 (gg<right>))")
        SGraph gold = pg("(w<root> / want-01 :ARG0 (b<left> :ARG0 (g<right>)) :ARG1 (g))")
        
        assertEquals(gold, g1.merge(g2))
    }
    
    @Test
    public void testMergeNotDisjoint() {
        SGraph g1 = pg("(w / want-01  :ARG0 (b)  :ARG1 (g))")
        SGraph g2 = pg("(b :ARG0 (g))")
        assertThat(g1.merge(g2), nullValue())
    }
    
    @Test
    public void testFreshNames() {
        SGraph g1 = pg("(w<root> / want-01  :ARG0 (b<left>)  :ARG1 (g<right>))")
        SGraph fresh = g1.withFreshNodenames()
        
        assertEquals(g1, fresh)
        assert Collections.disjoint(g1.getAllNodeNames(), fresh.getAllNodeNames())
    }
    
    @Test
    public void testRename() {
        SGraph g1 = pg("(w<root> / want-01  :ARG0 (b<left>)  :ARG1 (g<right>))")
        SGraph renamed = g1.renameSource("left", "foo")
        SGraph gold = pg("(w<root> / want-01  :ARG0 (b<foo>)  :ARG1 (g<right>))")
        
        assertEquals(gold, renamed)
    }
    
    @Test
    public void testMergeComplex() {
        SGraph want = IsiAmrParser.parse(new StringReader("(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))"));
        SGraph boy = IsiAmrParser.parse(new StringReader("(x<root> / boy)"));
        SGraph go = IsiAmrParser.parse(new StringReader("(g<root> / go-01  :ARG0 (s<subj>))"));
        
        SGraph combined = want.withFreshNodenames().merge(go.withFreshNodenames().renameSource("root", "vcomp").renameSource("subj", "subj"))
                    .merge(boy.withFreshNodenames().renameSource("root", "subj"));
                    
        SGraph gold = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01  :ARG0 (b)))")
        
        assertEquals(gold, combined)
    }
    
    @Test
    public void testMergeExtraSourcesInArgument() {
        SGraph g1 = pg("(u_15<root> :ARG0 (u_16<obj>) :ARG1 (u_17<xcomp>))")
        SGraph g2 = pg("(u_11<xcomp> / want-01  :ARG0 (u_12<subj> / boy)  :ARG1 (u_13<vcomp> / go-01  :ARG0 (u_14<obj> / girl)))")
        SGraph gold = pg("(u_15<root>  :ARG0 (u_16<obj> / girl)  :ARG1 (u_17<xcomp> / want-01  :ARG0 (u_12 / boy) :ARG1 (u_13 / go-01 :ARG0 (u_16))))")
        
        assertThat(g1.merge(g2), is(gold));        
    }
    
    @Test
    public void testMergeRenamingUnknownSource() {
        SGraph g1 = pg("(u_15<root> :ARG0 (u_16<obj>) :ARG1 (u_17<xcomp>))")
        assertThat(g1.renameSource("foo", "bar"), nullValue())
    }
    
    @Test
    public void testEqualsHashcode() {
        SGraph g1 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        SGraph g2 = pg("(u_42<root> / want-01  :ARG0 (u_41<subj> / boy)  :ARG1 (u_43<vcomp> / go-01 :ARG0 u_41))")
        
        assert g1.equals(g2) : "not equals"
        assert g1.hashCode() == g2.hashCode() : "different hashcodes"
    }
    
    @Test
    public void testToString() {
        SGraph g1 = pg("(x<root> / boy)")
        assertThat(g1.toString(), is("[x<root>/boy]"))
    }
    
    @Test
    public void testIdentical() {
        SGraph g1 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        SGraph g2 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        
        assert g1.isIdentical(g2)
        assert g2.isIdentical(g1)
    }
    
    @Test
    public void testNotIdentical() {
        SGraph g1 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        
        SGraph g2 = pg("   (ww<root> / want-01  :ARG0 (bb<subj> / boy)  :ARG1 (gg<vcomp> / go-01 :ARG0 bb))")
        assert ! g1.isIdentical(g2)
        assert ! g2.isIdentical(g1)
        
        SGraph g3 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 ))")
        assert ! g1.isIdentical(g3)
        assert ! g3.isIdentical(g1)
        
        SGraph g4 = pg("   (w<root>   :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        assert ! g1.isIdentical(g4)
        assert ! g4.isIdentical(g1)
    }
    
    @Test
    public void testMatchingSubgraphsOneLabeledNode() {
        SGraph g1 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        SGraph g2 = pg("(w<root> / boy)")
        List<SGraph> matches = g1.getMatchingSubgraphs(g2);
        
        assert matches.size() == 1
        assert matches.get(0).isIdentical(pg("(b<root>/boy)"))
    }
    
    @Test
    public void testMatchingSubgraphsOneEdge() {
        SGraph g1 = pg("   (w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        SGraph g2 = pg("(u<1> :ARG0 (v<2>))")
        List<SGraph> matches = g1.getMatchingSubgraphs(g2);
        
        assert matches.size() == 2
        
        assert matches.get(0).isIdentical(pg("(w<1> :ARG0 (b<2>))")) || matches.get(0).isIdentical(pg("(g<1> :ARG0 (b<2>))"))
        assert matches.get(1).isIdentical(pg("(w<1> :ARG0 (b<2>))")) || matches.get(1).isIdentical(pg("(g<1> :ARG0 (b<2>))"))
    }
}

