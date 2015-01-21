/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph

import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.signature.Signature
import org.junit.*
import java.util.*
import java.io.*
import java.util.logging.Level;
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*;
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.util.*
import de.up.ling.irtg.*
import java.nio.charset.Charset

import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;
/**
 *
 * @author jonas
 */
class BoundaryRepresentationTest {
	
    @Test
    public void testBRConstructor() {
        Signature sig = new Signature();
        sig.addSymbol("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))", 0);
        GraphAlgebra alg = new GraphAlgebra(sig);
        SGraph input = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))")
        BoundaryRepresentation br = new BoundaryRepresentation(input, new GraphInfo(input, alg, sig))
        assertEquals(br.toString(), "[b<subj> {u_b, b_b}, u<root> {u_b, u_g, u_u}, g<vcomp> {u_g}]")
    }
    
    @Test
    public void testEdgeAndVertexIDs() {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(HRGCleanS.getBytes( Charset.defaultCharset() ) ))
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra()
        String input = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 (g / girl)) :dummy g)"
        SGraph sgraph = alg.parseString(input)
        TreeAutomaton<BoundaryRepresentation> chart = alg.decompose(sgraph)
        GraphInfo graphInfo = new GraphInfo(sgraph, alg, alg.getSignature())
        
        for (BoundaryRepresentation br : chart.getStateInterner().getKnownObjects()) {
            assert (br.edgeID == br.computeEdgeID(graphInfo))
            assert (br.vertexID == br.computeVertexID(graphInfo))
        }
    }
 
    
    public static final String HRGCleanS = """\n\

interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

S! -> want2(NP, VP)
[string] *(?1, *(wants, *(to, ?2)))
[graph]  f_subj(f_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))))

S -> want3(NP, NP, VP)
[string] *(?1, *(wants, *(?2, *(to, ?3))))
[graph] f_subj(f_obj(f_vcomp(merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', 
                          r_subj(?1)), 
                    r_obj(?2)), 
              r_vcomp(r_subj_obj(?3))))))

NP -> boy
[string] *(the, boy)
[graph]  '(x<root> / boy)'

NP -> girl
[string] *(the, girl)
[graph]  '(x<root> / girl)'

// every VP has a 'subj' source at which the subject is inserted
VP -> believe(S)
[string] *(believe, *(that, ?1))
[graph]  f_xcomp(merge('(u<root> / believe-01  :ARG0 (v<subj>)  :ARG1 (w<xcomp>))', r_xcomp(f_root(?1))))

S -> likes(NP,NP)
[string] *(?1, *(likes, ?2))
[graph]  f_subj(f_obj(merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2))))

VP -> go
[string] go
[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'




    """;
     
}

