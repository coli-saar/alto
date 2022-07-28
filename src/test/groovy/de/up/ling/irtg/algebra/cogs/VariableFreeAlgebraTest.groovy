package de.up.ling.irtg.algebra.cogs


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;
import it.unimi.dsi.fastutil.ints.*;


class VariableFreeAlgebraTest {
    @Test
    public void testEvaluate() {
        VariableFreeAlgebra alg = new VariableFreeAlgebra();
        FeatureTree ft = alg.evaluate(pt("nsubjpass(auxpass(punct(known, '.'), was), det(box,the))"));
        System.err.println(ft);
    }
}
