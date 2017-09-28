/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.tree.Tree
import static org.junit.Assert.*
import org.junit.*

/**
 *
 * @author Jonas
 */
class AMDecompTest {
	
    @Test
    public void princeLoveRoseSelfTest() {
        decompSelfTest("(l<root> / love-01 :ARG0 (p/prince) :ARG1 (r/rose))", 0)
    }
    
    @Test
    public void modSelfTest() {
        decompSelfTest("(s/sleep-01 :ARG0 (p<root>/prince))", 0)
    }
    
    @Test
    public void controlSelfTest() {
        decompSelfTest("(w<root> / want-01 :ARG0 (p/prince) :ARG1 (s/sleep-01 :ARG0 p))", 0)
    }
    
    @Test
    public void controlCorefSelfTest() {
        decompSelfTest("(w<root> / want-01 :ARG0 (p/prince) :ARG1 (s/sleep-01 :ARG0 p))", 1)
    }
    
    @Test
    public void objectControlSelfTest() {
        //note that this does not test object control itself. Can just be done with passive! TODO: add a proper test, that checks that we find a tree that actually uses the object control constant.
        decompSelfTest("(w<root> / persuade-01 :ARG0 (p/prince) :ARG1 (r/rose) :ARG2 (s/sleep-01 :ARG0 r))", 0)
    }
    
    @Test
    public void triangleModSelfTest() {
        decompSelfTest("(w / want-01 :ARG0 (p/prince) :ARG1 (s<root>/sleep-01 :ARG0 p))", 0)
        //the prince sleeps, which is what he wants
    }
    
    @Test
    public void triangleModCorefSelfTest() {
        decompSelfTest("(w / want-01 :ARG0 (p/prince) :ARG1 (s<root>/sleep-01 :ARG0 p))", 1)
        //the prince sleeps, which is what he wants
    }
    
    @Test
    public void coordSelfTest() {
        decompSelfTest("(a<root> / and :op1 (r/relax :ARG1 (p/prince)) :op2 (s/sleep-01 :ARG0 p))", 0)
    }
    
    @Test
    public void raisingSelfTest() {
        decompSelfTest("(w<root> / want-01 :ARG0 (p/prince) :ARG1 (o/obligate-01 :ARG2 (s/sleep-01 :ARG0 p)))", 0)
    }
    
    @Test
    public void controlCoordSelfTest() {
        decompSelfTest("(w<root> / want-01 :ARG0 (p/prince) :ARG1 (a<root> / and :op1 (r/relax-01 :ARG1 p) :op2 (s/sleep-01 :ARG0 p)))", 0)
    }
    
    @Test
    public void coordControlSelfTest() {
        //also tests passive and object promotion
        decompSelfTest("(a<root> / and :op1 (pe/persuade-01 :ARG1 (p/prince) :ARG2 (s/sleep-01)) :op2 (w/want :ARG0 p :ARG1 s))", 0)
    }
    
    private void decompSelfTest(String graphString, int nrCorefs) {
        SGraph graph = pg(graphString)
        ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra(AMSignatureBuilder.makeDecompositionSignature(graph, nrCorefs));
        TreeAutomaton decomp = new AMDecompositionAutomaton(alg, null, graph);
        decomp.processAllRulesBottomUp(null, 60000);
        assert decomp.countTrees() > 0;
        Set<String> rootSet = new HashSet<>();
        rootSet.add("root");
        for (Tree<String> tree : decomp.language()) {
            //System.err.println(tree)
            SGraph res = alg.evaluate(tree).left;
            res = res.forgetSourcesExcept(rootSet)
            res.setEqualsMeansIsomorphy(true)//just to be sure
            assert res.equals(graph)
        }
    }
    
}

