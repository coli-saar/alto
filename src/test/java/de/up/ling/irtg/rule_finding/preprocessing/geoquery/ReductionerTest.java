/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.MinimalTreeAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.rule_finding.preprocessing.BruteForceCorpusReader;
import static de.up.ling.irtg.rule_finding.preprocessing.geoquery.RemoveIDTest.INPUT;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class ReductionerTest {

    /**
     *
     */
    private Reductioner red;

    /**
     *
     */
    private final static String INPUT = "# IRTG unannotated corpus file, v1.0\n"
            + "#\n"
            + "# interpretation string : de.up.ling.irtg.algebra.StringAlgebra\n"
            + "# interpretation funql : de.up.ling.irtg.algebra.MinimalTreeAlgebra\n"
            + "\n"
            + "Give me the cities in __stateid__ .\n"
            + "answer(city(loc_2(stateid(KNOWN))))\n"
            + "\n"
            + "Give me the cities in __countryid__ .\n"
            + "answer(city(loc_2(countryid(KNOWN))))\n"
            + "\n"
            + "How big iss isss isssss the city of __cityid__ ?\n"
            + "answer(size(city(cityid(KNOWN))))\n"
            + "\n"
            + "How many people is is in __cityid__ Texas ?\n"
            + "answer(population_1(cityid(KNOWN)))";

    private final static String OUTPUT = "# IRTG unannotated corpus file, v1.0\n"
            + "# \n"
            + "# \n"
            + "# \n"
            + "# interpretation string: class de.up.ling.irtg.algebra.StringAlgebra\n"
            + "# interpretation funql: class de.up.ling.irtg.algebra.MinimalTreeAlgebra\n"
            + "\n"
            + "give me the cit in __stateid__\n"
            + "answer(city(loc_2(stateid(KNOWN))))\n"
            + "\n"
            + "give me the cit in __countryid__\n"
            + "answer(city(loc_2(countryid(KNOWN))))\n"
            + "\n"
            + "how __UNKNOWN_WORD__ is is iss the cit __UNKNOWN_WORD__ __cityid__\n"
            + "answer(size(city(cityid(KNOWN))))\n"
            + "\n"
            + "how __UNKNOWN_WORD__ __UNKNOWN_WORD__ is is in __cityid__ __UNKNOWN_WORD__\n"
            + "answer(population_1(cityid(KNOWN)))";

    /**
     *
     */
    private Corpus corp;

    @Before
    public void setUp() throws IOException, CorpusReadingException {
        StringAlgebra sal = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();

        Map<String, Algebra> algs = new HashMap<>();
        algs.put("string", sal);
        algs.put("funql", mta);

        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(algs);
        ByteArrayInputStream bais = new ByteArrayInputStream(INPUT.getBytes());
        InputStreamReader isr = new InputStreamReader(bais);
        corp = Corpus.readCorpusLenient(isr, ita);

        this.red = new Reductioner(2, corp, "string", 3, 2);
    }

    /**
     * Test of reduce method, of class Reductioner.
     */
    @Test
    public void testReduce() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(baos);

        StringAlgebra sal = new StringAlgebra();
        MinimalTreeAlgebra mta = new MinimalTreeAlgebra();

        Map<String, Algebra> algs = new HashMap<>();
        algs.put("string", sal);
        algs.put("funql", mta);

        InterpretedTreeAutomaton ita = InterpretedTreeAutomaton.forAlgebras(algs);

        BruteForceCorpusReader.writeCorpus(ita, new FunctionIterable<Instance, Instance>(this.corp, (Instance) -> this.red.reduce(Instance, "string")), w);

        w.close();

        assertEquals(baos.toString().trim(), OUTPUT);
    }

}
