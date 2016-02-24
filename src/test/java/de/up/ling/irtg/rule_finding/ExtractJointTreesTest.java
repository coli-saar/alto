/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractJointTreesTest {
    
    /**
     *
     */
    private final String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "an example\n"
            + "this is another";

    /**
     *
     */
    private final String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "ein beispiel\n"
            + "dies ist ein weiteres";

    /**
     *
     */
    private final String alignments = "0-0 1-1\n"
            + "0-0 1-1 2-2 2-3\n";

    /**
     * 
     */
    private CorpusCreator corpCreate;
    
    /**
     * 
     */
    private Iterable<InputStream> automata1;
    
    /**
     * 
     */
    private Iterable<InputStream> automata2;
    
    /**
     * 
     */
    private Iterable<InputStream> alignmentInformation1;
    /**
     * 
     */
    private Iterable<InputStream> alignmentInformation2;
    
    /**
     * Test of getAutomataAndMakeStatistics method, of class ExtractStringToTreeGrammar.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetAutomataAndMakeStatistics() throws Exception {
        IntersectionPruner ip1 = new IntersectionPruner(IntersectionOptions.NO_EMPTY, IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        IntersectionPruner ip2 = new IntersectionPruner(IntersectionOptions.NO_EMPTY, IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        corpCreate = new CorpusCreator(ip1, ip2, 2);
        
        Iterable<InputStream>[] iters =
                ExtractionHelper.getInputData(leftTrees, rightTrees, alignments);
        this.automata1 = iters[0];
        this.automata2 = iters[1];
        this.alignmentInformation1 = iters[2];
        this.alignmentInformation2 = iters[3];
        
        ExtractJointTrees ejt = new ExtractJointTrees(corpCreate);
        ArrayList<ByteArrayOutputStream> results = new ArrayList<>();
        
        Supplier<OutputStream> outs = () -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            results.add(baos);
            
            return baos;
        };
        
        double[] dar = 
                ejt.getAutomataAndMakeStatistics(automata1, automata2, alignmentInformation1, alignmentInformation2, outs);
        
        assertEquals(results.size(),2);
        
        IrtgInputCodec iic = new IrtgInputCodec();
        InterpretedTreeAutomaton ita = iic.read(new ByteArrayInputStream(results.get(1).toByteArray()));
        
        assertEquals(ita.getAutomaton().countTrees(),dar[2],0.00000000000000001);
        assertEquals(dar[1],4.0,0.00000001);
        assertEquals(dar[1],24.0,0.00000001);
    }
    
}
