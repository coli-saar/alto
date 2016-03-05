/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import static de.up.ling.irtg.rule_finding.nonterminals.LookUpMTATest.TEST_AUTOMATON;
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
 * @author christoph
 */
public class IdentityNonterminalsTest {

    /**
     *
     */
    private List<InputStream> auts;

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
     *
     */
    private static final String LABEL_MAP = "largest ||| SO\n"
            + "'state' ||| PR";

    /**
     *
     */
    private InputStream mappings;

    /**
     *
     */
    private IdentityNonterminals idNon;

    /**
     * 
     */
    private final static String CORRECT = "0-1-0-0-0 ||| PR\n"
            + "0-0-0-0-0 ||| PR\n"
            + "0-1-0-0 ||| SO\n"
            + "0-0-0-0 ||| SO\n"
            + "0-0-0-0-0-0 ||| all\n"
            + "0-1-0 ||| answer\n"
            + "0-0-0 ||| answer";

    @Before
    public void setUp() {
        auts = new ArrayList<>();
        auts.add(new ByteArrayInputStream(TEST_AUTOMATON.getBytes()));

        mappings = new ByteArrayInputStream(LABEL_MAP.getBytes());

        idNon = new IdentityNonterminals();
    }

    @Test
    public void testSomeMethod() throws IOException {
        List<OutputStream> results = new ArrayList<>();
        results.add(new ByteArrayOutputStream());
        results.add(null);

        idNon.transferAutomata(auts, results, mappings, LABEL_MAP);

        assertEquals(results.get(0).toString().trim(),CORRECT);
    }

}
