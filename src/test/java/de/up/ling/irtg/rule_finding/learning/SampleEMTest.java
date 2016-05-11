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
import de.up.ling.irtg.rule_finding.sampling.Model;
import de.up.ling.irtg.rule_finding.sampling.models.IndependentTrees;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.tree.Tree;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class SampleEMTest {

    /**
     *
     */
    private Model mod;

    /**
     *
     */
    private SampleEM soe;
    
    /**
    *
    */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "an example\n"
            + "yet another example\n"
            + "and a final one";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein beispiel\n"
            + "noch ein beispiel\n"
            + "und ein letztes";

    /**
     *
     */
    private final static String alignments = "0-0 1-1\n"
            + "0-0 1-0 1-1 2-2\n"
            + "0-0 1-1 2-2 3-2";
    
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
        List<InterpretedTreeAutomaton> l = new ArrayList<>();
        data.forEach(l::add);
        data = l;
        
        Iterable<Signature> fi =
                new FunctionIterable<>(data,(InterpretedTreeAutomaton ita) -> ita.getAutomaton().getSignature());
        mod = new IndependentTrees(1, fi);
        this.soe = new SampleEM(mod);
        
        soe.setLearnSampleSize(200);
        soe.setTrainIterations(5);
    }

    /**
     * Test of getChoices method, of class SampleOnlineEM.
     */
    @Test
    public void testGetChoices() {
        Iterable<Iterable<Tree<String>>> it = soe.getChoices(this.data, mod, 9782598725987L);
        List<Tree<String>> results = new ArrayList<>();
        
        it.forEach((Iterable<Tree<String>> inner) -> inner.forEach(results::add));
        
        assertEquals(results.size(),600);
    }

}
