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
import de.up.ling.irtg.codec.IsiAmrInputCodec
import java.io.StringWriter

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

    @Test
    public void writeDecompAutoTest() {
        Writer writer = new StringWriter()
        SGraph input = new IsiAmrInputCodec().read("(c / chapter :mod 2)")
        GraphAlgebra.writeRestrictedDecompositionAutomaton(input, 2, writer)
        System.err.println(writer.toString())
        System.err.println(decompAutoGold)
        assertEquals(writer.toString(), decompAutoGold)
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
    
    private final String decompAutoGold = """'1_01' -> '(v<1> :mod (u<0>))'\n\
'2_0' -> '(u<0> / chapter)'\n\
'3_0' -> '(u<0> / 2)'\n\
'4_01' -> '(u<0> :mod (v<1>))'\n\
'0R5_1' -> r_0_1('2_0')\n\
'0R6_1' -> r_0_1('3_0')\n\
'7_01' -> merge('3_0', '1_01')\n\
'7_01' -> merge('1_01', '3_0')\n\
'8_01' -> merge('4_01', '2_0')\n\
'8_01' -> merge('2_0', '4_01')\n\
'9_01' -> merge('1_01', '0R5_1')\n\
'10_01' -> merge('4_01', '0R6_1')\n\
'11_1' -> f_0('7_01')\n\
'12_01'! -> merge('7_01', '0R5_1')\n\
'13_1' -> f_0('8_01')\n\
'14_01'! -> merge('8_01', '0R6_1')\n\
'15_0' -> f_1('9_01')\n\
'12_01'! -> merge('9_01', '3_0')\n\
'12_01'! -> merge('3_0', '9_01')\n\
'16_0' -> f_1('10_01')\n\
'14_01'! -> merge('10_01', '2_0')\n\
'14_01'! -> merge('2_0', '10_01')\n\
'0R16_0' -> r_1_0('11_1')\n\
'17_1'! -> merge('11_1', '0R5_1')\n\
'17_1'! -> f_0('12_01')\n\
'18_0'! -> f_1('12_01')\n\
'0R15_0' -> r_1_0('13_1')\n\
'19_1'! -> merge('13_1', '0R6_1')\n\
'19_1'! -> f_0('14_01')\n\
'20_0'! -> f_1('14_01')\n\
'0R13_1' -> r_0_1('15_0')\n\
'18_0'! -> merge('15_0', '3_0')\n\
'18_0'! -> merge('3_0', '15_0')\n\
'18_0'! -> merge('3_0', '0R15_0')\n\
'0R11_1' -> r_0_1('16_0')\n\
'20_0'! -> merge('16_0', '2_0')\n\
'20_0'! -> merge('2_0', '16_0')\n\
'20_0'! -> merge('2_0', '0R16_0')\n\
'0R20_0'! -> r_1_0('17_1')\n\
'21_'! -> f_1('17_1')\n\
'21_'! -> f_0('18_0')\n\
'0R19_1'! -> r_0_1('18_0')\n\
'0R18_0'! -> r_1_0('19_1')\n\
'21_'! -> f_1('19_1')\n\
'21_'! -> f_0('20_0')\n\
'0R17_1'! -> r_0_1('20_0')\n\
"""
}

