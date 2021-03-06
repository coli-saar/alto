/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra


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


/**
 *
 * @author koller
 */
class BinarizingAlgebraTest {
   @Test
   public void testBinarizingTree() {
       BinarizingTreeAlgebra alg = new BinarizingTreeAlgebra();
       Tree t = pt("f(a,b,g(d,e,e))")
       
       assertEquals(t, alg.evaluate(pt("f(_@_(_@_(a,b),g(_@_(_@_(d,e),e))))")))

       TreeAutomaton decomp = alg.decompose(pt("f(a,b,g(d,e,e))"))
           
       assert decomp.accepts(pt("f(_@_(_@_(a,b),g(_@_(_@_(d,e),e))))"))
       
       Set lang = decomp.language()
       lang.each { it -> assertEquals(t, alg.evaluate(it)) }
   }
   
    @Test
    public void testTreeWithBinarySymbols() {
        Tree orig = pt("S(NP-SBJ(NP(NNP(Pierre),NNP(Vinken)),','(','),ADJP(NP(CD('61'),NNS(years)),JJ(old)),','(',')),VP(MD(will),VP(VB(join),NP(DT(the),NN(board)),PP-CLR(IN(as),NP(DT(a),JJ(nonexecutive),NN(director))),NP-TMP(NNP('Nov.'),CD('29')))),'.'('.'))")
        Tree t = TreeWithAritiesAlgebra.addArities(orig)
        TreeAutomaton sing = new SingletonAutomaton(t)

        BinarizingTreeWithAritiesAlgebra alg = new BinarizingTreeWithAritiesAlgebra();
        TreeAutomaton bin = alg.binarizeTreeAutomaton(sing)
        
        bin.language().each {
//            System.err.println("bin tree: " + it)
//            System.err.println("unbinarized: " + alg.unbinarize(it))
//            System.err.println("eval -> " + alg.evaluate(it))
            assertEquals(orig, alg.evaluate(it))
        }
    }
    
    @Test
    public void testBinarizingTreeInvhom() {
        InterpretedTreeAutomaton irtg = pi(binTreeGram);
        Map<String, Object> input = new HashMap<>();
        input.put("english", "s(np(prp(he)),vp(aux(does),rb(not),vb(go)))");
        TreeAutomaton auto = irtg.parse(input);
        assert auto.languageIterator().hasNext();
    }
    
    String binTreeGram = """ /* Example tree-to-string transducer from Galley et al. 04 */

interpretation french: de.up.ling.irtg.algebra.StringAlgebra
interpretation english: de.up.ling.irtg.algebra.BinarizingTreeAlgebra

S! -> r1(NP,VB)
[french]  *(*(*(?1, ne), ?2), pas)
[english] s(?1, vp('_@_'(aux(does), '_@_'(rb(not), ?2))))

NP -> r2
[french]  il
[english] np(prp(he))

VB -> r3
[french]  va
[english] vb(go)
"""
    
}

