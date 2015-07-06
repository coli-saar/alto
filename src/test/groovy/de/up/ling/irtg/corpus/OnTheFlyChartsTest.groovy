/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.corpus

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import static de.up.ling.irtg.util.TestingTools.*;
import static org.junit.Assert.*
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.TreeAutomaton

/**
 *
 * @author christoph_teichmann
 */
class OnTheFlyChartsTest {
    
    @Test
    public void computeOnTheFlyTest()
    {
        // this is basically the same test as CorpusTest.testComputeCharts(), but with OnTheFlyCharts instead of Charts
        InterpretedTreeAutomaton irtg = pi(CorpusTest.CFG_STR);
        Corpus corpus = Corpus.readCorpus(new StringReader(CorpusTest.UNANNOTATED_CORPUS), irtg);
        
        ChartAttacher it = new OnTheFlyCharts(irtg);
        
        corpus.attachCharts(it);
        
        int count = 0;
        for( Instance inst : corpus ) {
            assert irtg.parseInputObjects(inst.getInputObjects()).equals(inst.getChart()) : "chart test failed for " + count;
            count++;
        }
        
        assert count == 3;
    }
}
