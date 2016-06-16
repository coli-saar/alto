/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.TreeParser;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class NondeletingInverseHomAutomatonTest {

    /**
     *
     */
    private Homomorphism hom;

    /**
     *
     */
    private TreeAutomaton bottomUpDeterministic;

    /**
     *
     */
    private TreeAutomaton nonDeterministic;

    /**
     *
     */
    private final static String TOPDOWN_CORRECT = "'3-4' -> k [1.0]\n"
            + "'0-1' -> k [1.0]\n"
            + "'1-2' -> j [1.0]\n"
            + "'2-3' -> j [1.0]\n"
            + "'0-2' -> i('0-1', '1-2') [1.0]\n"
            + "'0-3' -> i('0-1', '1-3') [0.5]\n"
            + "'0-4'! -> i('0-1', '1-4') [0.3333333333333333]\n"
            + "'0-4'! -> i('0-3', '3-4') [0.3333333333333333]\n"
            + "'0-3' -> i('0-2', '2-3') [0.5]\n"
            + "'0-4'! -> i('0-2', '2-4') [0.3333333333333333]\n"
            + "'1-3' -> i('1-2', '2-3') [1.0]\n"
            + "'1-4' -> i('1-2', '2-4') [0.5]\n"
            + "'1-4' -> i('1-3', '3-4') [0.5]\n"
            + "'0-3' -> h('2-3', '0-1') [0.5]\n"
            + "'2-4' -> i('2-3', '3-4') [1.0]\n"
            + "'0-4'! -> h('3-4', '0-2') [0.3333333333333333]\n"
            + "'1-4' -> h('3-4', '1-2') [0.5]\n"
            + "'0-4'! -> h('2-4', '0-1') [0.16666666666666666]\n"
            + "";

    /**
     *
     */
    private final static String BOTTOM_UP_DETERMINISTIC_CORRECT = "'1-2' -> j [1.0]\n"
            + "'0-1' -> k [1.0]\n"
            + "'2-3' -> l [1.0]\n"
            + "'0-2' -> i('0-1', '1-2') [1.0]\n"
            + "'2-4' -> m('2-3') [1.0]\n"
            + "'0-3' -> h('2-3', '0-1') [0.5]\n"
            + "'1-3' -> i('1-2', '2-3') [1.0]\n"
            + "'0-3' -> i('0-2', '2-3') [0.5]\n"
            + "'0-4'! -> h('2-4', '0-1') [0.16666666666666666]\n"
            + "'0-4'! -> i('0-2', '2-4') [0.3333333333333333]\n"
            + "'1-4' -> i('1-2', '2-4') [0.5]\n"
            + "'0-4'! -> m('0-3') [0.3333333333333333]\n"
            + "'1-4' -> m('1-3') [0.5]\n"
            + "'0-3' -> i('0-1', '1-3') [0.5]\n"
            + "'0-4'! -> i('0-1', '1-4') [0.3333333333333333]";
    
    
    /**
     * 
     */
    private final static String SAME_PATTERN_REPEATED = "q0 -> a [0.5]\n"
            + "q5 -> a [0.5]\n"
            + "q1 -> b [0.5]\n"
            + "q2 -> *(q0,q0) [1.0]\n"
            + "q2 -> *(q0,q5) [1.0]\n"
            + "q3 -> *(q0,q0) [0.5]\n"
            + "q4! -> *(q1,q2) [1.0]\n"
            + "q4! -> *(q1,q3) [1.0]\n";
    

    @Before
    public void setUp() throws ParseException {
        StringAlgebra sal = new StringAlgebra();

        bottomUpDeterministic = sal.decompose(sal.parseString("b a c d"));
        nonDeterministic = sal.decompose(sal.parseString("b a a b"));

        bottomUpDeterministic.normalizeRuleWeights();
        nonDeterministic.normalizeRuleWeights();

        assertTrue(bottomUpDeterministic.isBottomUpDeterministic());
        assertFalse(nonDeterministic.isBottomUpDeterministic());

        Signature shared = new Signature();

        hom = new Homomorphism(shared, sal.getSignature());

        shared.addSymbol("h", 2);
        hom.add("h", TreeParser.parse("*(?2,*(a,?1))"));
        shared.addSymbol("i", 2);
        hom.add("i", TreeParser.parse("*(?1,?2)"));
        shared.addSymbol("j", 0);
        hom.add("j", TreeParser.parse("a"));
        shared.addSymbol("k", 0);
        hom.add("k", TreeParser.parse("b"));
        shared.addSymbol("l", 0);
        hom.add("l", TreeParser.parse("c"));
        shared.addSymbol("m", 1);
        hom.add("m", TreeParser.parse("*(?1,d)"));
    }

    /**
     * Test of getRulesBottomUp method, of class NondeletingInverseHomAutomaton.
     */
    @Test
    public void testGetRulesBottomUp() {
        NondeletingInverseHomAutomaton nia1 = new NondeletingInverseHomAutomaton(bottomUpDeterministic, hom);

        StringBuilder sb1 = new StringBuilder();
        Consumer<Rule> cr = (Rule t) -> {
            sb1.append(t.toString(nia1));
            sb1.append("\n");
        };

        nia1.processAllRulesBottomUp(cr);
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();

        assertEquals(taic.read(sb1.toString()),taic.read(BOTTOM_UP_DETERMINISTIC_CORRECT));
        
        NondeletingInverseHomAutomaton nia2 = new NondeletingInverseHomAutomaton(nonDeterministic, hom);
        
        StringBuilder sb2 = new StringBuilder();
        cr = (Rule t) -> {
            sb2.append(t.toString(nia1));
            sb2.append("\n");
        };
        
        nia2.processAllRulesBottomUp(cr);
        
        assertEquals(taic.read(sb2.toString()),taic.read(TOPDOWN_CORRECT));
        
        
        //TODO
    }

    /**
     * Test of getRulesTopDown method, of class NondeletingInverseHomAutomaton.
     */
    @Test
    public void testGetRulesTopDown() {
        TreeAutomatonInputCodec taic = new TreeAutomatonInputCodec();
        
        /*
        NondeletingInverseHomAutomaton nia = new NondeletingInverseHomAutomaton(nonDeterministic, hom);

        nia.makeAllRulesExplicit();
        TreeAutomaton fin = nia.asConcreteTreeAutomatonWithStringStates();

        TreeAutomaton goal = taic.read(TOPDOWN_CORRECT);

        assertEquals(fin, goal);
        
        */
        System.out.println("Here it gets interesting");
        TreeAutomaton ta = taic.read(SAME_PATTERN_REPEATED);
        NondeletingInverseHomAutomaton nia3 = new NondeletingInverseHomAutomaton(ta, hom);
        
        System.out.println(nia3);
        System.out.println("---------");
        System.out.println(nia3);
        //TODO
    }
}
