/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class GetAllRulesTest {
   /**
    *
    */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "a good example\n"
            + "a weird example";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein gutes beispiel\n"
            + "ein seltsames beispiel";

    /**
     *
     */
    private final static String alignments = "0-0 1-1 2-2\n 0-0 1-1 2-2\n"
            + "";
    
    
    /**
     * 
     */
    private Iterable<InterpretedTreeAutomaton> data;
    
    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        Pruner one = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner two = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        
        Iterable<String> results = ExtractionHelper.getStringIRTGs(leftTrees, rightTrees, alignments, one, two);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        data = new FunctionIterable<>(results,(String s) -> {
            try {
                return iic.read(new ByteArrayInputStream(s.getBytes()));
            } catch (IOException | CodecParseException ex) {
                throw new RuntimeException();
            }
        });
    }

    /**
     * Test of getAllRules method, of class GetAllRules.
     * @throws de.up.ling.tree.ParseException
     */
    @Test
    public void testGetAllRules() throws ParseException {
        Iterator<InterpretedTreeAutomaton> it = data.iterator();
        InterpretedTreeAutomaton ita1 = it.next();
        InterpretedTreeAutomaton ita2 = it.next();
        
        TreeAutomaton ta1 = GetAllRules.getAllRules(ita1.getAutomaton());
        TreeAutomaton ta2 = GetAllRules.getAllRules(ita2.getAutomaton());
        
        Iterator<Tree<String>> i1 = ta1.languageIterator();
        
        Set<Tree<String>> s = ta1.language();
        s.retainAll(ta2.language());
        
        assertTrue(s.contains(TreeParser.parse("'__X__{__UAS__}'('*(x1, x2) / *(x1, x2) | 2'('__X__{0-1 +++ 0-1}'('___END___() / ___END___() | 0'),'*(x1, x2) / x1 | 2'('x1 / *(x1, x2) | 2'('__X__{1-2 +++ 1-2}'('___END___() / ___END___() | 0'),'x1 / beispiel() | 1'('___END___() / ___END___() | 0')),'example() / x1 | 1'('___END___() / ___END___() | 0'))))")));
        assertTrue(ta1.accepts(TreeParser.parse("'__X__{__UAS__}'('*(x1, x2) / *(x1, x2) | 2'('__X__{0-1 +++ 0-1}'('___END___() / ___END___() | 0'),'*(x1, x2) / x2 | 2'('good() / x1 | 1'('___END___() / ___END___() | 0'),'x2 / *(x1, x2) | 2'('x1 / gutes() | 1'('___END___() / ___END___() | 0'),'__X__{2-3 +++ 2-3}'('___END___() / ___END___() | 0')))))")));
        
        assertEquals(s.size(),9);
    }    
}
