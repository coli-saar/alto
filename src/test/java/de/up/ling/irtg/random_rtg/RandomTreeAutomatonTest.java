/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.random_rtg;

import de.up.ling.irtg.automata.TreeAutomaton;
import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class RandomTreeAutomatonTest {

    /**
     *
     */
    private RandomTreeAutomaton rta;

    @Before
    public void setUp() {
        rta = new RandomTreeAutomaton(new MersenneTwister(), new String[]{"a", "b", "c", "d", "e", "f"}, 2.0);
    }

    /**
     * Test of getRandomAutomaton method, of class RandomTreeAutomaton.
     */
    @Test
    public void testGetRandomAutomaton() {
        int targetNumberOfStates = 5;

        for (int i = 0; i < 50; ++i) {
            TreeAutomaton ta = rta.getRandomAutomaton(targetNumberOfStates, 10, 4);

            assertTrue(ta.getReachableStates().containsAll(ta.getAllStates()));
            assertTrue(ta.isBottomUpDeterministic());
            assertTrue(ta.getAllStates().size() > targetNumberOfStates);

            assertTrue(ta.countTrees() > 0);
        }
    }
}
