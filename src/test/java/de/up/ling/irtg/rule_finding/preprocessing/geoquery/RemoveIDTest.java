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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class RemoveIDTest {

    /**
     *
     */
    public final static String INPUT = "# IRTG unannotated corpus file, v1.0\n"
            + "#\n"
            + "# interpretation string: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "# interpretation funql: de.up.ling.irtg.algebra.MinimalTreeAlgebra\n"
            + "\n"
            + "Give me the cities in Virginia .\n"
            + "answer(city(loc_2(stateid(\"'virginia'\"))))\n\n"
            + "Give me the cities in USA .\n"
            + "answer(city(loc_2(countryid(\"'usa'\"))))\n\n"
            + "How big is the city of New York ?\n"
            + "answer(size(city(cityid(\"'new york'\", _))))";

    /**
     *
     */
    public final static String GOAL = "# IRTG unannotated corpus file, v1.0\n"
            + "# interpretation string : de.up.ling.irtg.algebra.StringAlgebra\n"
            + "# interpretation funql : de.up.ling.irtg.algebra.MinimalTreeAlgebra\n"
            + "\n"
            + "Give me the cities in stateid .\n"
            + "answer(city(loc_2(stateid(KNOWN))))\n"
            + "\n"
            + "Give me the cities in countryid .\n"
            + "answer(city(loc_2(countryid(KNOWN))))\n"
            + "\n"
            + "How big is the city of cityid ?\n"
            + "answer(size(city(cityid(KNOWN))))";

    /**
     *
     */
    public Corpus corp;

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
    }

    /**
     * Test of removedID method, of class RemoveID.
     */
    @Test
    public void testRemovedID() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RemoveID.removedID(corp, baos, "string", "funql",
                "de.up.ling.irtg.algebra.StringAlgebra", "de.up.ling.irtg.algebra.MinimalTreeAlgebra");

        System.out.println(baos.toString().trim());
        assertEquals(baos.toString().trim(),GOAL);
    }
}
