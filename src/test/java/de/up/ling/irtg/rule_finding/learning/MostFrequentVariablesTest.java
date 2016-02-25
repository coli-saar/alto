/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class MostFrequentVariablesTest {
    /**
    *
    */
    private final String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "an example\n"
            + "this is another\n"
            + "yet another example\n"
            + "and a final one";

    /**
     *
     */
    private final String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein beispiel\n"
            + "dies ist ein weiteres\n"
            + "noch ein example\n"
            + "und ein letztes";

    /**
     *
     */
    private final String alignments = "0-0 1-1\n"
            + "0-0 1-1 2-2 2-3\n"
            + "0-0 1-0 1-1 2-2\n"
            + "0-0 1-1 2-2 3-2";
    
    /**
     * 
     */
    private Iterable<TreeAutomaton> data;
    
    /**
     * 
     */
    private MostFrequentVariables mfv;

    
    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {        
        mfv = new MostFrequentVariables();
        
        Pruner one = new IntersectionPruner(IntersectionOptions.LEXICALIZED,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner two = new IntersectionPruner(IntersectionOptions.NO_EMPTY,IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        
        Iterable<String> results = ExtractionHelper.makeIRTGs(leftTrees, rightTrees, alignments, one, two);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        data = new FunctionIterable<>(results,(String s) -> {
            try {
                return iic.read(new ByteArrayInputStream(s.getBytes())).getAutomaton();
            } catch (IOException | CodecParseException ex) {
                throw new RuntimeException();
            }
        });
    }

    /**
     * Test of getOptimalChoices method, of class MostFrequentVariables.
     */
    @Test
    public void testGetChoices() {
        Iterable<Tree<String>> it = mfv.getChoices(data);
        Object2DoubleMap<String> counts = mfv.countVariablesTopDown(data);
        Iterator<Tree<String>> iter = it.iterator();
        
        AtomicInteger ai = new AtomicInteger();
        iter.forEachRemaining((Tree<String> t) -> ai.incrementAndGet());
        
        assertEquals(ai.get(),4);
    }

    /**
     * Test of countVariablesTopDown method, of class MostFrequentVariables.
     */
    @Test
    public void testCountVariablesTopDown() {
        Object2DoubleMap<String> counts = mfv.countVariablesTopDown(data);
        
        assertEquals(counts.get("__X__{1-2 +++ 1-2}"),4.0,0.00000001);
        assertEquals(counts.get("__X__{2-3 +++ 2-4}"),1.0,0.00000001);
    }
    
}
