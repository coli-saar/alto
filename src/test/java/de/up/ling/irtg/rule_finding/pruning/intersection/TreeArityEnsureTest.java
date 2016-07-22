/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.Propagator;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.tree.ParseException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class TreeArityEnsureTest {

    /**
     *
     */
    private IntersectionPruner ip1;

    /**
     *
     */
    private IntersectionPruner ip2;

    /**
     *
     */
    private TreeAutomaton tq;

    @Before
    public void setUp() throws ParserException {
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        TreeAutomaton ta = mta.decompose(mta.parseString("aa(f,'b 9 b'(c))"));

        ip1 = new IntersectionPruner(new IntersectionOptions[]{IntersectionOptions.NO_EMPTY, IntersectionOptions.ENSURE_ARITIES},
                new String[]{"","slghkds aa:2 'b 9 b':1"});
        ip2 = new IntersectionPruner(new String[]{IntersectionOptions.ENSURE_ARITIES.name(), IntersectionOptions.NO_LEFT_INTO_RIGHT.name(), IntersectionOptions.NO_EMPTY.name()},
                new String[]{"'aa':2 'b 9 b':1",null,null});

        Propagator po = new Propagator();
        SpecifiedAligner spal = new SpecifiedAligner(ta);
        Set<String> counter = new HashSet<>();
        counter.add("");

        tq = po.convert(ta, po.propagate(ta, spal), counter);
    }

    /**
     * Test of apply method, of class TreeArityEnsure.
     * @throws de.up.ling.tree.ParseException
     */
    @Test
    public void testApply() throws ParseException {
        TreeAutomaton ta = ip1.apply(tq);
        
        assertEquals(ta.countTrees(),64);
        
        ta = ip2.apply(tq);
        
        assertEquals(ta.countTrees(),8);
    }

}
