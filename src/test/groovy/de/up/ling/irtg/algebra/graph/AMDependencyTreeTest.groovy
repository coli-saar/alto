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
        SGraph gWant = new IsiAmrInputCodec().read("(w<root>/want-01 :ARG0 (g/giraffe :mod (t/tall) :mod (t2/tall)) :ARG1 (s/swim-01 :ARG0 g) :polarity (n/\"-\"))");
        assert tWant.evaluate().equals(new Pair< >(gWant, ApplyModifyGraphAlgebra.Type.EMPTY_TYPE));
    }






}
