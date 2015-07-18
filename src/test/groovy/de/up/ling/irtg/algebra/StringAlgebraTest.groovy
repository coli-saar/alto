/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra

import org.junit.Test
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class StringAlgebraTest {
    @Test
    public void testDecompose() {
        String string = "john watches the woman with the telescope";
        StringAlgebra algebra = new StringAlgebra();
        int concat = algebra.getSignature().getIdForSymbol(StringAlgebra.CONCAT);

        List words = algebra.parseString(string);
        TreeAutomaton auto = algebra.decompose(words);
        
        assertEquals(new HashSet([auto.getIdForState(s(0,7))]), auto.getFinalStates());
        assertEquals(new HashSet([r(auto, s(2,4), StringAlgebra.CONCAT, [s(2,3), s(3,4)])]), auto.getRulesBottomUp(concat, [s(2,3), s(3,4)].collect { auto.addState(it)}));
        assertEquals(new HashSet(), auto.getRulesBottomUp(concat, [s(2,3), s(4,5)].collect { auto.addState(it)}));
    }

    private static Rule r(TreeAutomaton auto, parent, label, children) {
        return auto.createRule(parent, label, children);
    }
        

    @Test
    public void testEvaluate() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new StringAlgebra();
        List words = algebra.parseString(string);
        Tree term = pt("*(john,*(watches,*(the,*(woman,*(with,*(the,telescope))))))");

        assertEquals(words, algebra.evaluate(term));
    }

    private StringAlgebra.Span s(int i, int k) {
        return new StringAlgebra.Span(i,k);
    }
    
    @Test
    public void testEvaluateWide() {
        String string = "john watches the woman with the telescope";
        Algebra algebra = new WideStringAlgebra();
        List words = algebra.parseString(string);
        Tree term = pt("conc2(john,conc4(watches,the,woman,conc3(with,the,telescope)))")

        assertEquals(words, algebra.evaluate(term));
    }
    
    @Test
    public void testDecomposeWideTopDown() {
        String string = "a b c";
        Algebra algebra = new WideStringAlgebra();
        
        List words = algebra.parseString(string);
        algebra.getSignature().addSymbol("conc2",2)
        algebra.getSignature().addSymbol("conc3",3)
        algebra.getSignature().addSymbol("conc4",4)
        
        TreeAutomaton auto = algebra.decompose(words);
        
        assertEquals(new HashSet([pt("conc3(a,b,c)"), pt("conc2(conc2(a,b),c)"), pt("conc2(a,conc2(b,c))")]), auto.language())
    }
    
    @Test
    public void testDecomposeWideBottomUp() {
        String string = "a b c";
        Algebra algebra = new WideStringAlgebra();
        
        List words = algebra.parseString(string);
        algebra.getSignature().addSymbol("conc2",2)
        algebra.getSignature().addSymbol("conc3",3)
        algebra.getSignature().addSymbol("conc4",4)
        
        TreeAutomaton auto = algebra.decompose(words);

        assert( auto.accepts(pt("conc3(a,b,c)")) );
        assert( auto.accepts(pt("conc2(conc2(a,b),c)")));
        assert( auto.accepts(pt("conc2(a,conc2(b,c))")));
    }
    
    
    @Test
    public void testDecomposeWithStar(){
        String s = "* b * a";
        Algebra a = new StringAlgebra();
        
        List words = a.parseString(s);
        
        TreeAutomaton ta = a.decompose(words);
        
        assertEquals(ta.language().size(),5);
        
        assertTrue(ta.accepts(pt("*(*(__*__,*(b,__*__)),a)")));
        assertTrue(ta.accepts(pt("*(__*__,*(b,*(__*__,a)))")));
        assertTrue(ta.accepts(pt("*(__*__,*(*(b,__*__),a))")));
        assertTrue(ta.accepts(pt("*(*(__*__,b),*(__*__,a))")));
        assertTrue(ta.accepts(pt("*(*(*(__*__,b),__*__),a)")));
        
        assertEquals(a.evaluate(pt("*(*(*(__*__,b),__*__),a)")),a.parseString("* b * a"));
//        
//        a = new WideStringAlgebra();
//        words = a.parseString(s);
//        
//        a.getSignature().addSymbol("conc2",2);
//        a.getSignature().addSymbol("conc3",3);
//        a.getSignature().addSymbol("conc4",4);
    }
    
    @Test
    public void testDecomposeWideWithStar() {     
        String s = "* b * a";
        Algebra a = new WideStringAlgebra();
        List words = a.parseString(s);
        a.getSignature().addSymbol("conc2",2);
        a.getSignature().addSymbol("conc3",3);
        a.getSignature().addSymbol("conc4",4);

        TreeAutomaton ta = a.decompose(words);

        assertTrue(ta.accepts(pt("conc2(conc2(conc2(__*__,b),__*__),a)")));
        assertTrue(ta.accepts(pt("conc3(__*__,conc2(b,__*__),a)")));
        assertTrue(ta.accepts(pt("conc3(__*__,conc2(b,__*__),a)")));
        assertTrue(ta.accepts(pt("conc4(__*__,b,__*__,a)")));
    }
    
}

