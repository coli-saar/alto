/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.VariableIntroduction;
import static de.up.ling.irtg.util.TestingTools.pt;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class LexicalizedTest {
    
    /**
     * 
     */
    private AlignedTrees at;
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        
        TreeAutomaton ta = sal.decompose(sal.parseString("a b c d e"));
        AlignedTrees pa = new AlignedTrees(ta, new SpecifiedAligner(ta));
        
        VariableIntroduction vi = new JustXEveryWhere();
        
        at = vi.apply(pa);
    }

    @Test
    public void testSomeMethod() throws Exception {
        TreeAutomaton from = at.getTrees();
        TreeAutomaton lex = new Lexicalized(from.getSignature(), from.getAllLabels());
        
        assertTrue(lex.accepts(pt("d")));
        assertFalse(lex.accepts(pt("X(d)")));
        assertTrue(lex.accepts(pt("*(X(d),c)")));
        assertTrue(lex.accepts(pt("*(X(*(a,X(d))),c)")));
        assertFalse(lex.accepts(pt("*(X(X(d)),c)")));
        assertFalse(lex.accepts(pt("*(X(*(X(a),X(d))),c)")));
        assertTrue(lex.accepts(pt("*(X(*(X(a),d)),c)")));
        assertTrue(lex.accepts(pt("*(*(a,b),*(c,*(d,e)))")));
    }
    
}
