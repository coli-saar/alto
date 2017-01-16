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
import de.up.ling.irtg.automata.TreeAutomaton
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

//        System.err.println("==============")
        SGraphBRDecompositionAutomatonBottomUp auto = (SGraphBRDecompositionAutomatonBottomUp)alg.decompose(alg.parseString(input), SGraphBRDecompositionAutomatonBottomUp.class)
//        System.err.println(auto)
//        System.err.println("==============")
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
        SGraph input = pg("(c / chapter :mod 2)")
        GraphAlgebra.writeRestrictedDecompositionAutomaton(input, 2, writer)
        //System.err.println(writer.toString())
        //System.err.println(decompAutoGold)
//        System.err.println(writer.toString())
        assertEquals(decompAutoGold, writer.toString())
    }
    
    //@Test This cannot be tested at the moment, since the sibling finder invhom is too interconnected with the intersection
    public void largeAutomatonTest() {
        SGraph graph = new GraphAlgebra().parseString(largeGraphString);
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new ByteArrayInputStream(recursiveGraphGrammar.getBytes( Charset.defaultCharset() ) ));
        TreeAutomaton invhom = irtg.getInterpretation("graph").parse(graph);
        invhom.makeAllRulesExplicit();
        assert(invhom.getFinalStates().size() == 3);
    }
    
    public static final String largeGraphString = "(pc3  :o-of (sc3 / sc  :u (pc4  :o-of (sc4 / sc  :u (pc5  :o-of (sc5 / sc  :u (pc6  :o-of (sc6 / sc  :u (pc7  :o-of (sc7 / sc  :u (pc8  :o-of (sc8 / sc  :u (pc9  :c-of (p9 / p  :A-of (r  :A sc8  :A (p8 / p  :c pc8)  :A sc7  :A (p7 / p  :c pc7)  :A sc6  :A (p6 / p  :c pc6)  :A sc5  :A (p5 / p  :c pc5)  :A sc4  :A (p4 / p  :c pc4)  :A sc3  :A (p3 / p  :c pc3)  :A (sc2 / sc  :o (pc2  :c-of (p2 / p  :A-of r)  :u-of (sc1 / sc  :o (pc1  :c-of (p1 / p  :A-of r))  :A-of r))  :u pc3))))))))))))))))";
    
    public static final String recursiveGraphGrammar = """\n\
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

DONE_STACK! -> done_stacking1(PLATE_STACK)
[graph] f_root_plt(?1)


PLATE_STACK -> basic_stack1(PLATE)
[graph] ?1

PLATE_STACK -> bring_another_plate2(PRIMED_STACK, PLATE)
[graph] merge(?1, ?2)

PRIMED_STACK -> prep_push(PLATE_STACK, SUPCON)
[graph] f_stk(merge(r_plt_stk(?1), r_undr_stk(r_ovr_plt(?2))))

PLATE -> plate0
[graph] '(r<root> :A (p / p :c (plt<plt>)))'

SUPCON -> supcon0
[graph] '(r<root> :A (sc/sc :o (ovr<ovr>) :u (undr<undr>)))'""";
    
    
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
    
    private final String decompAutoGold = """'1_0' -> '(u<0> / chapter)'
'2_0' -> '(u<0> / 2)'
'3_01' -> '(u<0> :mod (v<1>))'
'4_01' -> '(v<1> :mod (u<0>))'
'0R5_1' -> r_0_1('1_0')
'0R6_1' -> r_0_1('2_0')
'7_01' -> merge('3_01', '1_0')
'7_01' -> merge('1_0', '3_01')
'8_01' -> merge('4_01', '2_0')
'8_01' -> merge('2_0', '4_01')
'9_01' -> merge('4_01', '0R5_1')
'10_01' -> merge('3_01', '0R6_1')
'11_1' -> f_0('7_01')
'12_01'! -> merge('7_01', '0R6_1')
'13_1' -> f_0('8_01')
'14_01'! -> merge('8_01', '0R5_1')
'15_0' -> f_1('9_01')
'14_01'! -> merge('9_01', '2_0')
'14_01'! -> merge('2_0', '9_01')
'16_0' -> f_1('10_01')
'12_01'! -> merge('10_01', '1_0')
'12_01'! -> merge('1_0', '10_01')
'0R15_0' -> r_1_0('11_1')
'17_1'! -> merge('11_1', '0R6_1')
'17_1'! -> f_0('12_01')
'18_0'! -> f_1('12_01')
'0R16_0' -> r_1_0('13_1')
'19_1'! -> merge('13_1', '0R5_1')
'19_1'! -> f_0('14_01')
'20_0'! -> f_1('14_01')
'0R11_1' -> r_0_1('15_0')
'20_0'! -> merge('15_0', '2_0')
'20_0'! -> merge('2_0', '15_0')
'20_0'! -> merge('2_0', '0R15_0')
'0R13_1' -> r_0_1('16_0')
'18_0'! -> merge('16_0', '1_0')
'18_0'! -> merge('1_0', '16_0')
'18_0'! -> merge('1_0', '0R16_0')
'21_'! -> f_1('17_1')
'0R20_0'! -> r_1_0('17_1')
'21_'! -> f_0('18_0')
'0R19_1'! -> r_0_1('18_0')
'21_'! -> f_1('19_1')
'0R18_0'! -> r_1_0('19_1')
'21_'! -> f_0('20_0')
'0R17_1'! -> r_0_1('20_0')
"""
}

