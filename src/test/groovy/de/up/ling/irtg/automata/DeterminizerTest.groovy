/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.basic.Pair
import de.up.ling.irtg.automata.Rule
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomatonParser
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.signature.Signature
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class DeterminizerTest {
    @Test
    public void testDeterminizeMatcher() {
        TreeAutomaton match = pa(MATCH1)
        TreeAutomaton det = match.determinize()
        
        assert det.isBottomUpDeterministic()
        
        assert det.accepts(pt("f(g(a,b),a)"))
        assert det.accepts(pt("f(g(a,f(b,a)),g(a,b))"))
        assert det.accepts(pt("g(a,f(g(a,b),a))"))
        assert ! det.accepts(pt("f(g(b,a),a)"))
        assert ! det.accepts(pt("a"))
    }
	
    private static final String MATCH1 = '''
q1! -> f(q0, q1)
q1  -> f(q1, q0)
q1  -> g(q0, q1)
q1  -> g(q1, q0)

q0  -> f(q0, q0)
q0  -> g(q0, q0)
q0  -> a
q0  -> b

q1! -> f(q0, t_1/)
q1  -> f(t_1/, q0)
q1  -> g(q0, t_1/)
q1  -> g(t_1/, q0)

t_1/ ! -> f(t_1/1, t_1/2)
t_1/1  -> g(t_1/11, t_1/12)
t_1/11 -> a

t_1/12 -> f(q0, q0)
t_1/12 -> g(q0, q0)
t_1/12 -> a
t_1/12 -> b

t_1/2 -> f(q0, q0)
t_1/2 -> g(q0, q0)
t_1/2 -> a
t_1/2 -> b

''';
}

