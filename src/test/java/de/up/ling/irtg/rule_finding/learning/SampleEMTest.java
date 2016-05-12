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
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.rule_finding.ExtractJointTrees;
import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.irtg.rule_finding.create_automaton.ExtractionHelper;
import de.up.ling.irtg.rule_finding.pruning.IntersectionPruner;
import de.up.ling.irtg.rule_finding.pruning.Pruner;
import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.FunctionIterable;
import de.up.ling.irtg.util.ProgressListener;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
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
    private final static String GOAL = "0/3Initialized.\n"
            + "1/3Finished training round: 1\n"
            + "2/3Finished training round: 2\n"
            + "3/3Finished training round: 3\n"
            + "";

    /**
     *
     */
    private SampleEM soe;

    /**
     *
     */
    private final static String leftTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "long example\n"
            + "long long example\n"
            + "long long long example";

    /**
     *
     */
    private final static String rightTrees = "de.up.ling.irtg.algebra.StringAlgebra\n"
            + "langes beispiel\n"
            + "langes langes beispiel\n"
            + "langes langes langes beispiel";

    /**
     *
     */
    private final static String alignments = "0-0 1-1\n"
            + "0-0 1-1 2-2\n"
            + "0-0 1-1 2-2 3-3";

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
        Pruner one = new IntersectionPruner(IntersectionOptions.LEXICALIZED, IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);
        Pruner two = new IntersectionPruner(IntersectionOptions.NO_EMPTY, IntersectionOptions.RIGHT_BRANCHING_NORMAL_FORM);

        Iterable<String> results = ExtractionHelper.getStringIRTGs(leftTrees, rightTrees, alignments, one, two);
        results = new FunctionIterable<>(results, (String s) -> s.replaceAll("__X__\\{.+\\}", Variables.createVariable("")));

        IrtgInputCodec iic = new IrtgInputCodec();
        data = new FunctionIterable<>(results, (String s) -> {
            try {
                return iic.read(new ByteArrayInputStream(s.getBytes()));
            } catch (IOException | CodecParseException ex) {
                throw new RuntimeException();
            }
        });
        List<InterpretedTreeAutomaton> l = new ArrayList<>();
        data.forEach(l::add);
        data = l;

        Iterable<Signature> fi
                = new FunctionIterable<>(data, (InterpretedTreeAutomaton ita) -> ita.getAutomaton().getSignature());

        //TODO
        this.soe = new SampleEM();
        this.soe.setAdaptionRounds(10);
        this.soe.setNormalizationDivisor(100);
        this.soe.setNormalizationExponent(2);
        this.soe.setSampleSize(50);
        this.soe.setTrainIterations(3);

        dl = new DoubleArrayList();
        Consumer<Double> cd = (Double d) -> {
            dl.add(d);
        };
        soe.setnLLTracking(cd);

        this.soe.setSamplerLearningRate(0.5);
        this.soe.setSeed(98259275892659L);
        this.soe.setSmooth(1.0);

        allProgress = new StringBuffer();
        ProgressListener pl = (int done, int of, String message) -> {
            allProgress.append(done);
            allProgress.append("/");
            allProgress.append(of);
            allProgress.append("");
            allProgress.append(message);
            allProgress.append("\n");
        };

        soe.setResultSize(10);

        soe.setIterationProgress(pl);
        
        soe.setLearningSize(2);
    }

    /**
     * Test of getChoices method, of class SampleOnlineEM.
     */
    @Test
    public void testGetChoices() {
        assertEquals(soe.getNormalizationDivisor(), 100, 0.000001);
        assertEquals(soe.getNormalizationExponent(), 2);

        Iterable<Iterable<Tree<String>>> it = soe.getChoices(this.data);
        List<Tree<String>> results = new ArrayList<>();

        it.forEach((Iterable<Tree<String>> inner) -> inner.forEach(results::add));
        assertTrue(results.size() > 27);

        Iterator<InterpretedTreeAutomaton> base = data.iterator();
        base.next();
        base.next();
        InterpretedTreeAutomaton aut = base.next();
        Homomorphism left = aut.getInterpretation(ExtractJointTrees.FIRST_ALGEBRA_ID).getHomomorphism();
        Homomorphism right = aut.getInterpretation(ExtractJointTrees.SECOND_ALGEBRA_ID).getHomomorphism();

        assertEquals(left.apply(results.get(25)).toString(), "'__X__{}'(*(long,'__X__{}'(*('__X__{}'(*(long,'__X__{}'(long))),example))))");
        assertEquals(right.apply(results.get(25)).toString(), "'__X__{}'(*(langes,'__X__{}'(*('__X__{}'(*(langes,'__X__{}'(langes))),beispiel))))");

        assertTrue(dl.getDouble(0) > dl.getDouble(1));
        assertEquals(dl.size(), 3);

        assertEquals(allProgress.toString(),GOAL);
    }

}
