/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.VariableIntroduction;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
        
        TreeAutomaton ta = new IntersectionAutomaton(lex, from);
        
        assertTrue(lex.accepts(pt("d")));
        assertFalse(lex.accepts(pt("X(d)")));
        assertTrue(lex.accepts(pt("*(X(d),c)")));
        assertTrue(lex.accepts(pt("*(X(*(a,X(d))),c)")));
        assertFalse(lex.accepts(pt("*(X(X(d)),c)")));
        assertFalse(lex.accepts(pt("*(X(*(X(a),X(d))),c)")));
        assertTrue(lex.accepts(pt("*(X(*(X(a),d)),c)")));
        assertTrue(lex.accepts(pt("*(*(a,b),*(c,*(d,e)))")));
        
        Object2IntOpenHashMap<Tree<String>> counts = new Object2IntOpenHashMap<>();
        for(Tree<String> t : (Iterable<Tree<String>>) ta.languageIterable()) {
            counts.addTo(t, 1);
        }
        
        IntSet set = new IntOpenHashSet(counts.values());
        assertEquals(set.size(),1);
        assertTrue(lex.isBottomUpDeterministic());
        
        int num = 0;
        for(Rule r : (Iterable<Rule>) lex.getAllRulesTopDown()) {
            ++num;
        }
        
        assertEquals(num,10);
    }
    
}
