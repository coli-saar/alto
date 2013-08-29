/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.binarization

import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.Iterators;
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
public class RegularSeedTest {
    @Test
    public void testStringAlgebraSeed() {
        Algebra alg = new StringAlgebra();
        RegularSeed rs = new StringAlgebraSeed(alg.getSignature(), StringAlgebra.CONCAT);
        alg.getSignature().addAllSymbols(pt("f(a,b,c)"));
        
        TreeAutomaton binAuto = rs.binarize("f");
        assertEquals(new HashSet([pt("*(*(?1,?2),?3)"), pt("*(?1,*(?2,?3))")]), binAuto.language());
    }
    
//    @Test
    public void testStringSeed() {
        Algebra alg = new StringAlgebra();
        RegularSeed rs = new StringAlgebraSeed(alg.getSignature(), StringAlgebra.CONCAT);

        alg.getSignature().addAllSymbols(pt("f(a,b,c)"));
        TreeAutomaton binAuto = rs.binarize(pt("f(a,b,c)"));
        assertEquals(new HashSet([pt("*(*(a,b),c)"), pt("*(a,*(b,c))")]), binAuto.language()); 
    }
    
    @Test
    public void testComplexStringSeed() {
        Algebra alg = new StringAlgebra();
        RegularSeed rs = new StringAlgebraSeed(alg.getSignature(), StringAlgebra.CONCAT);

        alg.getSignature().addAllSymbols(pt("f(g(a,b),f(c,d,e),h)"));
        
        System.err.println("alg sig " + alg.getSignature());
        
        TreeAutomaton binAuto = rs.binarize(pt("f(g(a,b),f(c,d,e),h)"));
        assertEquals(new HashSet([pt("*(*(a,b), *( *(c,*(d,e)), h))"), pt("*(*(a,b), *( *(*(c,d),e), h))"),
                                  pt("*(*( *(a,b), *(c,*(d,e)) ), h)"), pt("*(*( *(a,b), *(*(c,d),e)), h)")]),
                          binAuto.language()); 
    }
}

