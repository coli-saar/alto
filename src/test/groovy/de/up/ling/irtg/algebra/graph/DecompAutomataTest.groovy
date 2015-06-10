/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph

import de.up.ling.irtg.signature.Signature
import org.junit.Test
import static de.up.ling.irtg.util.TestingTools.*;
import static org.junit.Assert.*
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.ConcreteTreeAutomaton

import java.nio.charset.Charset
/**
 *
 * @author groschwitz
 */
class DecompAutomataTest {
    
    @Test
    public void bottomUpTest() {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(HRGCleanS.getBytes( Charset.defaultCharset() ) ))
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra()

        String input = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 b))"

        SGraph sgraph = alg.parseString(input)

        SGraphBRDecompositionAutomatonBottomUp auto = (SGraphBRDecompositionAutomatonBottomUp)alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class)
        assert(auto.accepts(pt("f_subj(f_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)')), r_vcomp(r_subj_subj('(g<root> / go-01  :ARG0 (s<subj>))')))))")))
    }

    @Test
    public void topDownTest() {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(HRGCleanS.getBytes( Charset.defaultCharset() ) ))
        GraphAlgebra alg = (GraphAlgebra)irtg.getInterpretation("graph").getAlgebra()

        String input = "(w<root> / want-01 :ARG0 (b / boy) :ARG1 (go / go-01 :ARG0 b))"

        SGraph sgraph = alg.parseString(input)

        SGraphBRDecompositionAutomatonTopDown auto = (SGraphBRDecompositionAutomatonTopDown)alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonTopDown.class)
        assert(auto.asConcreteTreeAutomaton().accepts(pt("f_subj(f_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)')), r_vcomp(r_subj_subj('(g<root> / go-01  :ARG0 (s<subj>))')))))")))
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

