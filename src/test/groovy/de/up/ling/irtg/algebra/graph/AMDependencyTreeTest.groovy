package de.up.ling.irtg.algebra.graph

import de.saar.basic.Pair
import de.up.ling.irtg.codec.IsiAmrInputCodec
import de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.Type
import org.junit.Test

import static de.up.ling.irtg.algebra.graph.ApplyModifyGraphAlgebra.GRAPH_TYPE_SEP

class AMDependencyTreeTest {

    ApplyModifyGraphAlgebra alg = new ApplyModifyGraphAlgebra()

    Pair<SGraph, Type> giraffe = alg.parseString("(g<root>/giraffe)");
    Pair<SGraph, Type> swim = alg.parseString("(e<root>/swim-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)");
    Pair<SGraph, Type> eat = alg.parseString("(e<root>/eat-01 :ARG0 (s<s>))"+GRAPH_TYPE_SEP+"(s)");
    Pair<SGraph, Type> want = alg.parseString("(w<root>/want-01 :ARG0 (s<s>) :ARG1 (o<o>))"+GRAPH_TYPE_SEP+"(s, o(s))");
    Pair<SGraph, Type> not = alg.parseString("(n<root>/\"-\" :polarity-of (m<m>))"+GRAPH_TYPE_SEP+"(m)");
    Pair<SGraph, Type> tall = alg.parseString("(t<root>/tall :mod-of (m<m>))"+GRAPH_TYPE_SEP+"(m)");
    String appS = "APP_s";
    String appO = "APP_o";
    String modM = "MOD_m";

    @Test
    void testEat() {
        AMDependencyTree tEat = new AMDependencyTree(eat);
        SGraph gEat = new IsiAmrInputCodec().read("(e<root>/eat-01 :ARG0 (g/giraffe))");
        tEat.addEdge(appS, new AMDependencyTree(giraffe));
        System.err.println(tEat.evaluate().equals(new Pair<>(gEat, ApplyModifyGraphAlgebra.Type.EMPTY_TYPE)));
    }


    @Test
    void testWant() {
        AMDependencyTree tWant = new AMDependencyTree(want);
        AMDependencyTree tGiraffe = new AMDependencyTree(giraffe);
        tWant.addEdge(appS, tGiraffe);
        tWant.addEdge(appO, swim);
        tGiraffe.addEdge(modM, tall);
        tGiraffe.addEdge(modM, tall);
        tWant.addEdge(modM, not);
        assert tWant.getOperationsAndChildren().size() == 3;
        assert tGiraffe.getOperationsAndChildren().size() == 2;
        SGraph gWant = new IsiAmrInputCodec().read("(w<root>/want-01 :ARG0 (g/giraffe :mod (t/tall) :mod (t2/tall)) :ARG1 (s/swim-01 :ARG0 g) :polarity (n/\"-\"))");
        assert tWant.evaluate().equals(new Pair< >(gWant, ApplyModifyGraphAlgebra.Type.EMPTY_TYPE));
    }

    @Test
    void testBadTree() {
        AMDependencyTree tWant = new AMDependencyTree(want);
        AMDependencyTree tGiraffe = new AMDependencyTree(giraffe);
        tWant.addEdge(appS, tGiraffe);
        assert tWant.evaluate() == null;
    }

    @Test
    void testRemoveEdges() {
        AMDependencyTree tWant = new AMDependencyTree(want);
        AMDependencyTree tGiraffe = new AMDependencyTree(giraffe);

        tWant.addEdge(appS, tGiraffe);
        tWant.addEdge(appO, swim);
        tGiraffe.addEdge(modM, tall);
        tGiraffe.addEdge(modM, tall);
        tWant.addEdge(modM, not);
        assert tWant.removeEdge(appS, tGiraffe);
        assert tWant.getOperationsAndChildren().size() == 2;
        //remove the negation edge, but we don't know the negation AMDependencyTree at this point, so we need to find it
        assert tWant.removeEdge(modM, new AMDependencyTree(not));
        assert tWant.getOperationsAndChildren().size() == 1;
        //now we try to remove some edges that are not there
        assert !tWant.removeEdge(appS, tGiraffe);
        assert !tWant.removeEdge(appO, tGiraffe);
        assert !tWant.removeEdge(appS, new AMDependencyTree(swim));
        assert tWant.getOperationsAndChildren().size() == 1;
        //removing final edge, also testing equals function of AMDependencyTree a bit
        assert tWant.removeEdge(appO, new AMDependencyTree(swim));
        assert tWant.getOperationsAndChildren().isEmpty();
    }

    @Test
    void testEquals() {
        AMDependencyTree tWant = new AMDependencyTree(want);
        AMDependencyTree tWant2 = new AMDependencyTree(want);
        assert tWant.hashCode() == tWant2.hashCode() && tWant2.equals(tWant);
        AMDependencyTree tGiraffe = new AMDependencyTree(giraffe);
        tGiraffe.addEdge(modM, tall);

        tWant.addEdge(appS, tGiraffe);
        tWant2.addEdge(appS, new AMDependencyTree(giraffe));
        assert !tWant2.equals(tWant);
        tWant2.removeEdge(appS, new AMDependencyTree(giraffe));
        tWant2.addEdge(appO, new AMDependencyTree(swim));
        assert !tWant2.equals(tWant);
        tWant2.addEdge(appS, tGiraffe);
        tWant.addEdge(appO, swim);
        assert tWant.hashCode() == tWant2.hashCode() && tWant2.equals(tWant);
    }




}
