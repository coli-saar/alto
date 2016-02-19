/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning.intersection.string;

import de.up.ling.irtg.rule_finding.pruning.intersection.RightBranchingNormalForm;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.SpecifiedAligner;
import de.up.ling.irtg.rule_finding.create_automaton.AlignedTrees;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import static de.up.ling.irtg.util.TestingTools.pt;
import de.up.ling.tree.Tree;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph
 */
public class RightBranchingNormalFormTest {
    
    /**
     * 
     */
    private TreeAutomaton basis;
    
    
    @Before
    public void setUp() {
        StringAlgebra sal = new StringAlgebra();
        sal.decompose(sal.parseString("a b c d e"));
        basis = sal.decompose(sal.parseString("f g h i j"));
    }

    @Test
    public void testSomeMethod() throws Exception {
        RightBranchingNormalForm rbnf = new RightBranchingNormalForm(basis.getSignature(), basis.getAllLabels());
        
        Iterable<Rule> it = rbnf.getRulesBottomUp(basis.getSignature().getIdForSymbol("a"), new int[] {});
        assertFalse(it.iterator().hasNext());
        
        SpecifiedAligner spac = new SpecifiedAligner(basis);
        JustXEveryWhere jxe = new JustXEveryWhere();
        AlignedTrees at = new AlignedTrees(basis, spac);
        TreeAutomaton ta = jxe.apply(at).getTrees();
        
        rbnf = new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
        TreeAutomaton inter = new IntersectionAutomaton(basis, rbnf);
        assertEquals(inter.language().size(),1);
        assertTrue(inter.accepts(pt("*(f,*(g,*(h,*(i,j))))")));
        
        inter = new IntersectionAutomaton(ta, rbnf);
        for(Rule r : (Iterable<Rule>) inter.getAllRulesTopDown()){
            r.setWeight(0.5);
        }
        
        Iterator<Tree<String>> n = inter.languageIterator();
        
        assertTrue(inter.accepts(pt("*(X(*(f,g)),*(h,*(X(i),j)))")));
        assertTrue(inter.accepts(pt("*(X(*(f,g)),*(h,*(i,j)))")));
        assertTrue(inter.accepts(pt("*(X(*(f,g)),*(X(*(h,i)),j))")));
        assertFalse(inter.accepts(pt("*(*(f,g),*(X(*(h,i)),j))")));
        assertFalse(inter.accepts(pt("*(*(f,g),*(*(h,i),j))")));
        assertFalse(inter.accepts(pt("*(X(*(f,g)),*(*(h,i),j))")));
        assertTrue(inter.accepts(pt("*(f,*(g,*(X(*(h,i)),j)))")));
        assertTrue(inter.accepts(pt("*(f,*(g,*(h,*(i,j))))")));
        assertTrue(inter.accepts(pt("*(f,*(g,*(h,*(i,X(j)))))")));
        assertTrue(inter.accepts(pt("*(f,X(*(g,*(h,*(i,X(j))))))")));
    }
}
