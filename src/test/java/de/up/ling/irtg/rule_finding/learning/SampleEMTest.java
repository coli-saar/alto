/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.create_automaton.MakeIdIRTGForSingleSide;
import de.up.ling.irtg.rule_finding.create_automaton.MakeMonolingualAutomaton;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private SampleEM sam;

    /**
     *
     */
    private final static String STRINGS = "long example\n"
            + "long long example\n"
            + "example";

    /**
     *
     */
    private Iterable<InterpretedTreeAutomaton> data;

    /**
     *
     */
    private DoubleList dl;

    /**
     *
     */
    private StringBuffer allProgress;

    @Before
    public void setUp() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParserException {
        MakeMonolingualAutomaton mma = new MakeMonolingualAutomaton();
        Function<Object,String> funct = (Object o) -> "X";
        ArrayList<InterpretedTreeAutomaton> da = new ArrayList<>();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(STRINGS.getBytes())));
        String line;
        StringAlgebra sal = new StringAlgebra();
        while((line = br.readLine()) != null) {
            TreeAutomaton ta = sal.decompose(sal.parseString(line));
            
           ta = mma.introduce(ta, funct, "__ROOT__");
           
           da.add(MakeIdIRTGForSingleSide.makeIRTG(ta, "string", sal));
        }
        
        data = da;
        
        this.sam = new SampleEM();
        this.sam.setAdaptionRounds(20);
        this.sam.setNormalizationDivisor(100);
        this.sam.setNormalizationExponent(2);
        this.sam.setSampleSize(500);
        this.sam.setTrainIterations(20);

        dl = new DoubleArrayList();
        Consumer<Double> cd = (Double d) -> {
            dl.add(d);
        };
        sam.setnLLTracking(cd);

        this.sam.setSamplerLearningRate(0.5);
        this.sam.setSeed(98259275892659L);
        this.sam.setSmooth(1.0);

        allProgress = new StringBuffer();
        ProgressListener pl = (int done, int of, String message) -> {
            allProgress.append(done);
            allProgress.append("/");
            allProgress.append(of);
            allProgress.append(" ");
            allProgress.append(message);
            allProgress.append("\n");
        };

        sam.setResultSize(10);

        sam.setIterationProgress(pl);
        sam.setReset(false);
        sam.setLexiconAdditionFactor(2.0);
    }

    /**
     * Test of getChoices method, of class SampleOnlineEM.
     * 
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     * @throws de.up.ling.tree.ParseException
     */
    @Test
    public void testGetChoices() throws InterruptedException, ExecutionException, ParseException {
        assertEquals(sam.getNormalizationDivisor(), 100, 0.000001);
        assertEquals(sam.getNormalizationExponent(), 2);

        Iterable<Iterable<Tree<String>>> it = sam.getChoices(this.data);
        List<Tree<String>> results = new ArrayList<>();
        it.forEach((Iterable<Tree<String>> inner) -> inner.forEach(results::add));
        
        assertTrue(results.size() < 34);
        
        //CANNOT REALLY GUARANTEE THIS, because of paralellization
        //assertTrue(results.contains(TreeParser.parse("'__X__{X}'(*('__X__{X}'(long),example))")));
    }

}
