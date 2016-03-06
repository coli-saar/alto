/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.codec.CodecParseException;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class VariableWeightedRandomPickTest {
    /**
    *
    */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "an example\n"
            + "this is another\n"
            + "yet another example\n"
            + "and a final one";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein beispiel\n"
            + "dies ist ein weiteres\n"
            + "noch ein example\n"
            + "und ein letztes";

    /**
     *
     */
    private final static String alignments = "0-0 1-1\n"
            + "0-0 1-1 2-2 2-3\n"
            + "0-0 1-0 1-1 2-2\n"
            + "0-0 1-1 2-2 3-2";
    
    /**
     * 
     */
    private Iterable<InterpretedTreeAutomaton> data;
    
    /**
     * 
     */
    private VariableWeightedRandomPick vwr;
    
    @Before
    public void setUp() throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, ParserException {
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
        
        vwr = new VariableWeightedRandomPick(1.5, 5, 10, 0.5);
    }

    /**
     * Test of getChoices method, of class VariableWeightedRandomPick.
     */
    @Test
    public void testGetChoices() {
        int count  = 0;
        for(Iterable<Tree<String>> t : vwr.getChoices(data)) {
            for(Tree<String> q : t) {
                ++count;
            }
        }
        
        assertEquals(count,18);
    }
}
