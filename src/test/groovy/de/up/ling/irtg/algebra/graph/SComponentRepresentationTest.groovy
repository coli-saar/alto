/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph

import de.up.ling.irtg.signature.Signature
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import org.junit.Test
import de.up.ling.irtg.algebra.graph.GraphAlgebra
import de.up.ling.irtg.algebra.graph.SComponent
import de.up.ling.irtg.algebra.graph.GraphInfo
import de.up.ling.irtg.algebra.graph.SGraph
import static de.up.ling.irtg.util.TestingTools.*;
import static org.junit.Assert.*

/**
 *
 * @author groschwitz
 */
class SComponentRepresentationTest {
    
    @Test
    public void testSComponentConstructor() {
        Signature sig = new Signature()
        sig.addSymbol("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))", 0)
        GraphAlgebra alg = new GraphAlgebra(sig)
        SGraph input = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))")
        GraphInfo graphInfo = new GraphInfo(input, alg)
        
        IntSet inBVertices = new IntOpenHashSet();
        inBVertices.add(1);
        inBVertices.add(2);
        IntSet inBEdges = new IntOpenHashSet();
        inBEdges.add(1);
        Map<SComponent, SComponent> map = new HashMap<>();
        SComponent c = SComponent.makeComponent(inBVertices, inBEdges, map, graphInfo)
        
        SComponent c1 = SComponent.makeComponent(inBVertices, inBEdges, map, graphInfo)
        
        assertEquals(c,c1);
        IntSet testInBVertices = new IntOpenHashSet();
        testInBVertices.add(1);
        testInBVertices.add(2);
        IntSet testInBEdges = new IntOpenHashSet();
        testInBEdges.add(1);
        assertEquals(c.getBVertices(), testInBVertices)
        assertEquals(c.getInBEdges(), testInBEdges)
    }
    
    @Test
    public void testSComponentSplit() {
        Signature sig = new Signature()
        sig.addSymbol("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))", 0)
        GraphAlgebra alg = new GraphAlgebra(sig)
        SGraph input = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))")
        GraphInfo graphInfo = new GraphInfo(input, alg)
        
        IntSet inBVertices = new IntOpenHashSet()
        inBVertices.add(0)
        IntSet inBEdges = new IntOpenHashSet()
        inBEdges.add(0)
        Map<SComponent, SComponent> map = new HashMap<>()
        SComponent c = SComponent.makeComponent(inBVertices, inBEdges, map, graphInfo)
        
        IntSet inBVertices1 = new IntOpenHashSet()
        inBVertices1.add(0)
        inBVertices1.add(1)
        IntSet inBEdges1 = new IntOpenHashSet()
        inBEdges1.add(0)
        
        SComponent c1 = SComponent.makeComponent(inBVertices1, inBEdges1, map, graphInfo)
        
        IntSet inBVertices2 = new IntOpenHashSet()
        inBVertices2.add(1)
        IntSet inBEdges2 = new IntOpenHashSet()
        inBEdges2.add(3)
        
        SComponent c2 = SComponent.makeComponent(inBVertices2, inBEdges2, map, graphInfo)
        
        IntSet inBVertices3 = new IntOpenHashSet()
        inBVertices3.add(1)
        IntSet inBEdges3 = new IntOpenHashSet()
        inBEdges3.add(1)
        
        SComponent c3 = SComponent.makeComponent(inBVertices3, inBEdges3, map, graphInfo)
        
        Int2ObjectMap<Set<SComponent>> splitRes = c.getAllSplits(map, graphInfo)
        Set<SComponent> testRes = new HashSet<SComponent>()
        testRes.add(c1)
        testRes.add(c2)
        testRes.add(c3)
        
        assertEquals(splitRes.get(1),testRes)
        assertEquals(splitRes.size(),1)
    }
    
    @Test
    public void testSComponentRepresentationConstructorsAndForgetReverse() {
        Signature sig = new Signature()
        sig.addSymbol("(u<root> / want-01  :ARG0 (b/ boy)  :ARG1 (g))", 0)
        GraphAlgebra alg = new GraphAlgebra(sig)
        SGraph input = pg("(u<root> / want-01  :ARG0 (b/ boy)  :ARG1 (g))")
        GraphInfo graphInfo = new GraphInfo(input, alg)
        
        Map<SComponent, SComponent> map = new HashMap<>()
        IntSet inBVertices = new IntOpenHashSet()
        inBVertices.add(1)
        
        SComponent c = SComponent.makeComponent(new IntOpenHashSet(), new IntOpenHashSet(), map, graphInfo)//whole graph
        
        
        //now the components where want is a source
        IntSet inBEdges1 = new IntOpenHashSet()
        inBEdges1.add(0)
        SComponent c1 = SComponent.makeComponent(inBVertices, inBEdges1, map, graphInfo)
        
        IntSet inBEdges2 = new IntOpenHashSet()
        inBEdges2.add(3)
        SComponent c2 = SComponent.makeComponent(inBVertices, inBEdges2, map, graphInfo)
        
        IntSet inBEdges3 = new IntOpenHashSet()
        inBEdges3.add(1)
        SComponent c3 = SComponent.makeComponent(inBVertices, inBEdges3, map, graphInfo)
        
        Set<SComponent> scSet = new HashSet<>()
        scSet.add(c1)
        scSet.add(c2)
        scSet.add(c3)
        
        int[] sourceToNodeName = new int[1]
        sourceToNodeName[0] = 1;
        
        SComponentRepresentation scr = new SComponentRepresentation(input, alg);//complete graph
        
        //now varitations to make 1 a source
        SComponentRepresentation scr1 = scr.forgetReverse(0, 1, c, scSet);
        SComponentRepresentation scr2 = new SComponentRepresentation(sourceToNodeName, scSet, graphInfo)
        SComponentRepresentation scr3 = new SComponentRepresentation(input, map, graphInfo)
        
        assertEquals(scr1, scr2)
        assertEquals(scr3, scr2)
        assertEquals(scr1, scr3)
    }
}

