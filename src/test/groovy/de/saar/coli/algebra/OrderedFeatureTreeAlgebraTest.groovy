package de.saar.coli.algebra

import de.saar.coli.algebra.OrderedFeatureTreeAlgebra
import org.junit.*
import static org.junit.Assert.*

import static de.up.ling.irtg.util.TestingTools.*

class OrderedFeatureTreeAlgebraTest {
    @Test
    public void testEvaluate() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("nsubjpass(auxpass(punct(known, '.'), was), det(box,the))"));

        assertEquals("known(punct = . , auxpass = was , nsubjpass = box(det = the))", ft.toString());
        assertEquals("known(punct = . , auxpass = was , nsubjpass = * box)", ft.toString(true));
    }

    @Test
    public void testEvaluateDetWithChildren() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("nsubjpass(auxpass(punct(known, '.'), was), foo(det(box,the), bar))"));

        assertEquals("known(punct = . , auxpass = was , nsubjpass = box(det = the , foo = bar))", ft.toString());
        assertEquals("known(punct = . , auxpass = was , nsubjpass = * box(foo = bar))", ft.toString(true));
    }

    @Test
    public void testEvaluateNmod() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("pre_det(nmod(girl, pre_det(pre_case(table, on), a)), the)"   ));

        assertEquals("* girl(nmod.on = table)", ft.toString(true));
    }

    @Test
    public void testEvaluatePrefix() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("pre_nsubjpass(auxpass(punct(known, '.'), was), det(box,the))"));

        assertEquals("known(nsubjpass = box(det = the) , punct = . , auxpass = was)", ft.toString());
    }

    @Test
    public void testControl() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("xcomp(agent('hope[agent = xcomp!agent]', det(boy, the)), investigate)"))

        assertEquals("hope(agent = * boy , xcomp = investigate(agent = * boy))", ft.toString(true))
    }

    @Test
    public void testRaising() {
        OrderedFeatureTreeAlgebra alg = new OrderedFeatureTreeAlgebra();
        OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = alg.evaluate(pt("xcomp(agent('seem[agent -> xcomp!agent]', det(boy, the)), sleep)"))

        assertEquals("seem(xcomp = sleep(agent = * boy))", ft.toString(true))
    }
}
