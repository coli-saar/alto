/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg

import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.condensed.CondensedNondeletingInverseHomAutomaton
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton
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
class ExamplesTest {
    def forgetSources = { g -> ((SGraph) g).forgetSourcesExcept(new HashSet()) }
    
    private Object algValue(String repr, String interpName, InterpretedTreeAutomaton irtg) {
        Interpretation intrp = irtg.getInterpretation(interpName)
        Algebra alg = intrp.getAlgebra()
        return alg.parseString(repr)
    }
    
    private void runTest(String filename, String derivationTree, Map<String,String> values, def massage=[:]) {
        InterpretedTreeAutomaton irtg = pi(rs(filename))
        Tree dt = pt(derivationTree)
        
        // test that given derivation tree is accepted from all sides
        for( String interpretation : values.keySet() ) {
            TreeAutomaton chart = irtg.parse([(interpretation):values.get(interpretation)])
            assertThat("chart for " + interpretation + " accepts dt", chart.accepts(dt))
        }
        
        // test that derivation tree projects to all given inputs
        for( String interpretation : values.keySet() ) {
            Object gold = algValue(values.get(interpretation), interpretation, irtg)
            Object x = irtg.interpret(dt, interpretation)
            
            if( massage.containsKey(interpretation)) {
                x = massage.get(interpretation)(x)
            }
            
            assertThat("dt projects to input on " + interpretation,
                x,
                equalTo(gold))
        }
    }
    
    
    // string-to-string
    @Test
    public void testChinese() {
        runTest("examples/chinese.irtg", "r1(r2(r4(r5),r3(r6,r7)))",
            ["english":"friendly cooperation over the past 30 years",
             "chinese":"30 duonianlai de youhao hezuo"])
    }
    
    // tree-to-tree
    @Test
    public void testCohnLapata() {
        runTest("examples/cohn-lapata.irtg", "r1(r2(r5),r3,r4(r6))",
            ["long":"s3(sbar(whnp2(rb(exactly),wp(what)),s2(np(nns(records)),vp2(vbd(made),np(prp(it))))), cc(and), sbar(whnp1(wp(which)), s2(np(nns(ones)), vp2(vbp(are),vp1(vbn(involved))))))",
             "compressed":"s2(whnp1(wp(what)),s2(np(nns(records)),vp2(vbp(are),vp1(vbn(involved)))))"])
    }
    
    // tree-to-string
    @Test
    public void testGhkm() {
        runTest("examples/ghkm.irtg", "r1(r2,r3)",
            ["english":"s(np(prp(he)),vp(aux(does),rb(not),vb(go)))",
             "french":"il ne va pas"])
    }
    
    // string-to-graph
    @Test
    public void testHrgIwcs1() {
        runTest("examples/hrg-iwcs15.irtg", "comb_subj(boy,want1(sleep))", [
             "string":"the boy wants to sleep",
             "graph":"(u_273 / want  :ARG0 (u_272 / boy  :ARG0-of (u_274 / sleep  :ARG1-of u_273)))"
            ],
            ["graph":forgetSources])
    }
    
    @Test
    public void testHrgIwcs2() {
        runTest("examples/hrg-iwcs15.irtg", "comb_subj(boy,want1(sleep))", [
             "string":"the boy wants to sleep",
             "graph":"(u_24 / sleep  :ARG0 (u_22 / boy  :ARG0-of (u_23 / want  :ARG1 u_24)))"
            ],
            ["graph":forgetSources])
    }
    
    @Test
    public void testHrgIwcs3() {
        runTest("examples/hrg-iwcs15.irtg", "comb_subj(mod_rc(boy,rc(who,sleeps)),snore)", [
             "string":"the boy who sleeps snores",
             "graph":"(u_35 / boy  :ARG0-of (u_34 / snore)  :ARG0-of (u_38 / sleep))"
            ],
            ["graph":forgetSources])
    }
    
    @Test
    public void testHrgIwcs4() {
        runTest("examples/hrg-iwcs15.irtg", "comb_subj(boy,coord(sleeps,sometimes(snore)))", [
             "string":"the boy sleeps and sometimes snores",
             "graph":"(u_51 / sleep  :ARG0 (u_53 / boy  :ARG0-of (u_52 / snore  :time (u_56 / sometimes)  :op2-of (u_50 / and  :op1 u_51))))"
            ],
            ["graph":forgetSources])
    }
    
    // TAG strings, TAG trees
    @Test
    public void testNessonShieber() {
        runTest("examples/nesson-shieber.irtg", "a1(a2,a3,b4)", [
             "syntax":"s(np(john),vp(adv(apparently),vp(v(likes),np(mary))))",
             "semantics":"t(t_t(apparently),t(e_t(likes,e(mary)),e(john)))",
             "string":"john apparently likes mary"
            ])
    }
    
    // wide string algebra
    @Test
    public void testWideStrings() {
        runTest("examples/wide-cfg.irtg", "r1(r2,r3,r4)", [
             "left":"b c d",
             "right":"d a b c"
            ])
    }
    
    //currently failing
    @Test
    public void testTAGCondensedAndSiblingFinder() {
        int gold = 819;//I am confident that this is the correct value, confirmed in multiple experiments. JG
        InterpretedTreeAutomaton irtg = piBin(new FileInputStream("examples/tests/vinkenTAG.irtb"));
        Object input = irtg.parseString("string", "Vinken is chairman .");
        
        //sibling finder
        TreeAutomaton sf = irtg.parseWithSiblingFinder("string", input);
        assert sf.countTrees() == gold;
        
        //condensed
        TreeAutomaton decomp = irtg.getInterpretation("string").getAlgebra().decompose(input);
        CondensedTreeAutomaton condInvhom = new CondensedNondeletingInverseHomAutomaton(decomp, irtg.getInterpretation("string").getHomomorphism());
        TreeAutomaton cond = irtg.getAutomaton().intersectCondensed(condInvhom);
        cond.makeAllRulesExplicit();
        assert cond.countTrees() == gold;
    }
    
}

