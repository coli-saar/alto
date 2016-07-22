/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class LookUpMTATest {

    /**
     *
     */
    private List<InputStream> auts;

    /**
     *
     */
    public static final String CORRECT = "0-1-0-0-0 ||| PR\n"
            + "0-0-0-0-0 ||| PR\n"
            + "0-1-0-0 ||| SO\n"
            + "0-0-0-0 ||| SO\n"
            + "0-0-0-0-0-0 ||| CONSTANT\n"
            + "0-1-0 ||| AN\n"
            + "0-0-0 ||| AN";

    /**
     * 
     */
    public static final String ALSO_CORRECT = "0-1-0-0-0 ||| CONSTANT\n"
            + "0-0-0-0-0 ||| CONSTANT\n"
            + "0-1-0-0 ||| CONSTANT\n"
            + "0-0-0-0 ||| CONSTANT\n"
            + "0-0-0-0-0-0 ||| CONSTANT\n"
            + "0-1-0 ||| CONSTANT\n"
            + "0-0-0 ||| CONSTANT";

    /**
     *
     */
    public static final String TEST_AUTOMATON = "'0-0-0-0-0-0' -> all [1.0]\n"
            + "'0-0-0-0' -> largest [1.0]\n"
            + "'0-0-0' -> answer [1.0]\n"
            + "'0-0-0-0-0' -> state [1.0]\n"
            + "'0-1-0-0-0' -> __LR__('0-0-0-0-0-0', '0-0-0-0-0') [1.0]\n"
            + "'0-1-0-0' -> __LR__('0-1-0-0-0', '0-0-0-0') [1.0]\n"
            + "'0-1-0-0-0' -> __RL__('0-0-0-0-0', '0-0-0-0-0-0') [1.0]\n"
            + "'0-1-0'! -> __RL__('0-0-0', '0-1-0-0') [1.0]\n"
            + "'0-1-0-0' -> __RL__('0-0-0-0', '0-1-0-0-0') [1.0]\n"
            + "'0-1-0'! -> __LR__('0-1-0-0', '0-0-0') [1.0]";
    /**
     * *
     *
     */
    private static final String LABEL_MAP = "largest ||| SO\n"
            + "answer ||| AN\n"
            + "'state' ||| PR";

    /**
     *
     */
    private InputStream mappings;

    /**
     *
     */
    private LookUpMTA LUM;

    @Before
    public void setUp() {
        auts = new ArrayList<>();
        auts.add(new ByteArrayInputStream(TEST_AUTOMATON.getBytes()));

        mappings = new ByteArrayInputStream(LABEL_MAP.getBytes());

        LUM = new LookUpMTA();
    }

    /**
     * Test of computeValue method, of class LookUpMTA.
     * @throws java.io.IOException
     */
    @Test
    public void testComputeValue() throws IOException {
        List<OutputStream> results = new ArrayList<>();
        results.add(new ByteArrayOutputStream());
        results.add(null);

        LUM.transferAutomata(auts, results, mappings, "CONSTANT");

        assertEquals(results.get(0).toString().trim(), CORRECT);

        results.clear();
        results.add(new ByteArrayOutputStream());
        results.add(null);

        auts = new ArrayList<>();
        auts.add(new ByteArrayInputStream(TEST_AUTOMATON.getBytes()));

        mappings = new ByteArrayInputStream("\n".getBytes());

        LUM.transferAutomata(auts, results, mappings, "CONSTANT");

        assertEquals(results.get(0).toString().trim(),ALSO_CORRECT);
    }
}
