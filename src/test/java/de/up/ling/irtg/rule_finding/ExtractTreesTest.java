/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.alignments.AddressAligner;
import de.up.ling.irtg.rule_finding.alignments.SpanAligner;
import de.up.ling.irtg.rule_finding.create_automaton.CorpusCreator;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.string.RightBranchingNormalForm;
import de.up.ling.irtg.rule_finding.pruning.intersection.tree.NoLeftIntoRight;
import de.up.ling.irtg.rule_finding.variable_introduction.JustXEveryWhere;
import de.up.ling.irtg.rule_finding.variable_introduction.LeftRightXFromFinite;
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
public class ExtractTreesTest {
    
    /**
     * 
     */
    private final static String TEST_INPUT = "Could you tell me what is the highest point in the state of Oregon ?\n" +
                                             "answer(highest(place(loc_2(state(stateid('oregon'))))))\n" +
                                             "0:1:1 1:2:2 2:3:3 3:4:4 7:8:5 8:9:6 9:10:7 11:12:8 12:13:9 13:14:10 14:15:11\n" +
                                             "0-0-0-0:1 0-0-0:2 0-0-0:3 0-0-0:4 0-0-0-0:5 0-0-0-0-0:6 0-0-0-0-0-0:7 0-0-0-0-0-0-0:8 0-0-0-0-0-0-0-0:9 0-0-0-0-0-0-0-0-0:10 0-0-0-0-0-0-0-0:11\n" +
                                             "\n" +
                                             "Count the states which have elevations lower than what Alabama has .\n" +
                                             "answer(count(state(low_point_2(lower_2(low_point_1(stateid('alabama')))))))\n" +
                                             "2:3:1 2:3:2 3:4:3 4:5:4 5:6:5 5:6:6 6:7:7 7:8:8 8:9:9 9:10:10 9:10:11 10:11:12\n" +
                                             "0-0-0-0:1 0-0-0-0-0:2 0-0-0-0-0:3 0-0-0-0-0-0:4 0-0-0-0-0-0:5 0-0-0-0-0-0-0-0:6 0-0-0-0-0-0-0:7 0-0-0-0-0-0-0:8 0-0-0-0-0-0-0-0:9 0-0-0-0-0-0-0-0-0:10 0-0-0-0-0-0-0-0-0-0:11 0-0-0-0-0-0-0-0:12"
                                           + "\n\n\n";

    /**
     * Test of getAutomataAndMakeStatistics method, of class ExtractStringToTreeGrammar.
     */
    @Test
    public void testGetAutomataAndMakeStatistics() throws Exception {
        CorpusCreator.Factory fact = new CorpusCreator.Factory();
        fact.setFirstPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new RightBranchingNormalForm(ta.getSignature(), ta.getAllLabels());
        }));
        fact.setSecondPruner(new IntersectionPruner<>((TreeAutomaton ta) -> {
            return new NoLeftIntoRight(ta.getSignature(), ta.getAllLabels());
        }));
        fact.setFirstVariableSource(new LeftRightXFromFinite());
        fact.setSecondVariableSource(new JustXEveryWhere());
        
        StringAlgebra st = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();
        
        SpanAligner.Factory ffact = new SpanAligner.Factory();
        AddressAligner.Factory sfact = new AddressAligner.Factory();
        
        CorpusCreator cc = fact.getInstance(st, mta, ffact, sfact);
        ExtractTrees gram = new ExtractTrees(cc);
        
        InputStream in = new ByteArrayInputStream(TEST_INPUT.getBytes());
        final ArrayList<ByteArrayOutputStream> results = new ArrayList<>();
        Supplier<OutputStream> supp = () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            results.add(out);
            
            return out;
        };
        
        double[] d = gram.getAutomataAndMakeStatistics(in, supp);
        assertEquals(d[0],708.0,0.00000001);
        assertEquals(d[1],392,0.00000001);
        assertEquals(d[2],1024,0.00000001);
        assertEquals(results.size(),2);
    }
    
}
