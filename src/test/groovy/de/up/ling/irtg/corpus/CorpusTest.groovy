/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.corpus

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*
import de.saar.chorus.term.*
import de.up.ling.tree.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.*;
import com.google.common.base.Supplier;


/**
 *
 * @author koller
 */
class CorpusTest {
    @Test
    public void testComputeCharts() {
        // parse corpus and save to byte stream
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();        
        InterpretedTreeAutomaton irtg = pi(new StringReader(CFG_STR));
        Corpus corpus = Corpus.readCorpus(new StringReader(UNANNOTATED_CORPUS), irtg);        
        Charts.computeCharts(corpus, irtg, ostream);
        
        // read parsed corpus and check that charts are correct
        Charts charts = new Charts(new ByteArrayInputStreamSupplier(ostream.toByteArray()));
        corpus.attachCharts(charts);
        
        int count = 0;
        for( Instance inst : corpus ) {
            assert irtg.parseInputObjects(inst.getInputObjects()).equals(inst.getChart()) : "chart test failed for " + count;
            count++;
        }
        
        assert count == 3;
    }
    
    @Test
    public void testNewCorpusUnanno() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(CFG_STR));
        Corpus corpus = Corpus.readCorpus(new StringReader(UNANNOTATED_CORPUS), irtg);
        assert corpus.getNumberOfInstances() == 3;
        assert ! corpus.isAnnotated();
    }
    
     @Test
    public void testNewCorpusAnno() {
        InterpretedTreeAutomaton irtg = pi(new StringReader(CFG_STR));
        Corpus corpus = Corpus.readCorpus(new StringReader(ANNOTATED_CORPUS), irtg);
        assert corpus.getNumberOfInstances() == 3;
        assert corpus.isAnnotated();
    }
                    
    private static final String CFG_STR = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra


S! -> r1(NP,VP)
  [i] *(?1,?2)


NP -> r2(Det,N)
  [i] *(?1,?2)


N -> r3(N,PP)
  [i] *(?1,?2)


VP -> r4(V,NP) [.6]
  [i] *(?1,?2)


VP -> r5(VP,PP) [0.4]
  [i] *(?1,?2)


PP -> r6(P,NP) 
  [i] *(?1,?2)


NP -> r7 
  [i] john


V -> r8 
  [i] watches


Det -> r9
  [i] the


N -> r10
  [i] woman


N -> r11
  [i] telescope


P -> r12
  [i] with



""";
    
    private static final String PCFG_EMTRAIN_STR = """
i
john watches the woman with the telescope
john watches the telescope with the telescope
john watches the telescope with the woman
""";
    
    private static final String UNANNOTATED_CORPUS = """
# IRTG unannotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra

john watches the woman with the telescope
john watches the telescope with the telescope
john watches the telescope with the woman
""";
    
    private static final String ANNOTATED_CORPUS = """
# IRTG annotated corpus file, v1.0
#
# interpretation i: de.up.ling.irtg.algebra.StringAlgebra
 
john watches the woman with the telescope
r1(r7,r5( r4(r8, r2(r9,r10)), r6(r12, r2(r9,r11))))
john watches the telescope with the telescope
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r11))))
john watches the telescope with the woman
r1(r7,r5( r4(r8, r2(r9,r11)), r6(r12, r2(r9,r10))))

    """;
}

