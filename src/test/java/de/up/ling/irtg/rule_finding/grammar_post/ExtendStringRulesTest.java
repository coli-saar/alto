/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.grammar_post;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.signature.Signature;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class ExtendStringRulesTest {

    public static String irtg = "interpretation french: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "interpretation english: de.up.ling.irtg.algebra.TreeAlgebra\n"
            + "\n"
            + "S! -> r1(NP)\n"
            + "[french]  *(?1, ne)\n"
            + "[english] A(?1, rb(not))\n"
            + "\n"
            + "NP -> r2\n"
            + "[french]  ne\n"
            + "[english] np(prp(he))\n";

    /**
     *
     */
    private InterpretedTreeAutomaton ita;

    /**
     *
     */
    private Iterable<Signature> sigs;

    /**
     *
     */
    private Supplier<String> supp;

    @Before
    public void setUp() throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();

        ita = iic.read(new ByteArrayInputStream(irtg.getBytes()));

        List<Signature> sgs = new ArrayList<>();

        Signature sig = new Signature();
        sig.addSymbol("a", 0);
        sig.addSymbol("b", 1);
        sgs.add(sig);

        sigs = sgs;

        AtomicInteger ai = new AtomicInteger(1);
        supp = () -> {
            return "s" + ai.getAndIncrement();
        };
    }

    /**
     * Test of extend method, of class ExtendStringRules.
     */
    @Test
    public void testExtend() {
        InterpretedTreeAutomaton qta = ExtendStringRules.extend(ita, "french", sigs, 0.0001, supp);

        assertEquals(qta.toString().trim(), "interpretation english: de.up.ling.irtg.algebra.TreeAlgebra\n"
                + "interpretation french: de.up.ling.irtg.algebra.StringAlgebra\n"
                + "\n"
                + "NP -> r2 [0.9998000399920016]\n"
                + "  [english] np(prp(he))\n"
                + "  [french] ne\n"
                + "\n"
                + "NP -> s2(NP) [9.998000399920017E-5]\n"
                + "  [english] ?1\n"
                + "  [french] *(?1,a)\n"
                + "\n"
                + "S! -> r1(NP) [0.9998000399920016]\n"
                + "  [english] A(?1,rb(not))\n"
                + "  [french] *(?1,ne)\n"
                + "\n"
                + "NP -> s1(NP) [9.998000399920017E-5]\n"
                + "  [english] ?1\n"
                + "  [french] *(a,?1)\n"
                + "\n"
                + "S! -> s4(S) [9.998000399920017E-5]\n"
                + "  [english] ?1\n"
                + "  [french] *(?1,a)\n"
                + "\n"
                + "S! -> s3(S) [9.998000399920017E-5]\n"
                + "  [english] ?1\n"
                + "  [french] *(a,?1)");
    }

}
