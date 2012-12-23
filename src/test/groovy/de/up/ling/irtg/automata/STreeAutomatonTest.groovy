/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata

import spock.lang.*
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class STreeAutomatonTest extends Specification {
    def "testing whether automaton is bottom-up deterministic"() {
        expect:
        pa(automaton).isBottomUpDeterministic() == gold
        
        where:
        automaton                              | gold
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> a" | false
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> b" | true
    }
}

