/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.nonterminals;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ReplaceNonterminalTest {

    /**
     *
     */
    public final static String TEST_AUTOMATON = "interpretation SecondInput: de.up.ling.irtg.algebra.TreeAlgebra\n"
            + "interpretation FirstInput: de.up.ling.irtg.algebra.TreeAlgebra\n"
            + "\n"
            + "'81604378626' -> '___END___() / ___END___() | 0' [1.0]\n"
            + "  [SecondInput] ___END___\n"
            + "  [FirstInput] ___END___\n"
            + "\n"
            + "'81604378625' -> '___END___() / ___END___() | 0' [1.0]\n"
            + "  [SecondInput] ___END___\n"
            + "  [FirstInput] ___END___\n"
            + "\n"
            + "'42949672972' -> '__X__{4-5 +++ 0-1-0-0-0}'('90194313225') [1.0]\n"
            + "  [SecondInput] '__X__{0-1-0-0-0}'(?1)\n"
            + "  [FirstInput] '__X__{4-5}'(?1)\n"
            + "\n"
            + "__UAS__! -> '__X__{__UAS__}'('4294967305') [1.0]\n"
            + "  [SecondInput] '__X__{__UAS__}'(?1)\n"
            + "  [FirstInput] '__X__{__UAS__}'(?1)\n"
            + "\n"
            + "'60129542147' -> 'x3 / __RL__(x1, x2) | 3'('64424509444', '68719476740', '81604378625') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?3\n"
            + "\n"
            + "'60129542148' -> 'x3 / __RL__(x1, x2) | 3'('64424509444', '68719476740', '81604378625') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?3\n"
            + "\n"
            + "'304942678025' -> '*(x1, x2) / x2 | 2'('236223201288', '12884901899') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'481036337163' -> '*(x1, x2) / x2 | 2'('236223201288', '472446402571') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'304942678025' -> '*(x1, x2) / x3 | 3'('236223201288', '111669149704', '150323855363') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'244813135880' -> '*(x1, x2) / x3 | 3'('236223201288', '111669149704', '81604378626') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'55834574852' -> 'x1 / largest() | 1'('81604378625') [1.0]\n"
            + "  [SecondInput] largest\n"
            + "  [FirstInput] ?1\n"
            + "\n"
            + "'68719476740' -> 'x1 / all() | 1'('81604378625') [1.0]\n"
            + "  [SecondInput] all\n"
            + "  [FirstInput] ?1\n"
            + "\n"
            + "'64424509444' -> 'x1 / state() | 1'('81604378625') [1.0]\n"
            + "  [SecondInput] state\n"
            + "  [FirstInput] ?1\n"
            + "\n"
            + "'206158430212' -> 'x1 / answer() | 1'('81604378625') [1.0]\n"
            + "  [SecondInput] answer\n"
            + "  [FirstInput] ?1\n"
            + "\n"
            + "'476741369867' -> '*(x1, x2) / x2 | 2'('223338299400', '481036337163') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'274877906952' -> '*(x1, x2) / x3 | 3'('223338299400', '244813135880', '81604378626') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'485331304459' -> 'x2 / __RL__(x1, x2) | 2'('206158430212', '146028888076') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?2\n"
            + "\n"
            + "'489626271747' -> 'x3 / __RL__(x1, x2) | 3'('206158430212', '150323855364', '81604378625') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?3\n"
            + "\n"
            + "'481036337163' -> 'x2 / __RL__(x1, x2) | 2'('206158430212', '30064771084') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?2\n"
            + "\n"
            + "'472446402571' -> 'x2 / __RL__(x1, x2) | 2'('206158430212', '12884901900') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?2\n"
            + "\n"
            + "'12884901899' -> '__X__{3-5 +++ 0-1-0-0}'('180388626441') [1.0]\n"
            + "  [SecondInput] '__X__{0-1-0-0}'(?1)\n"
            + "  [FirstInput] '__X__{3-5}'(?1)\n"
            + "\n"
            + "'12884901900' -> '__X__{3-5 +++ 0-1-0-0}'('180388626441') [1.0]\n"
            + "  [SecondInput] '__X__{0-1-0-0}'(?1)\n"
            + "  [FirstInput] '__X__{3-5}'(?1)\n"
            + "\n"
            + "'4294967305' -> '*(x1, x2) / x2 | 2'('210453397512', '476741369867') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'4294967305' -> '*(x1, x2) / x3 | 3'('210453397512', '274877906952', '489626271747') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'90194313225' -> 'state() / x1 | 1'('60129542147') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] state\n"
            + "\n"
            + "'236223201288' -> 'the() / x1 | 1'('81604378626') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] the\n"
            + "\n"
            + "'210453397512' -> 'give() / x1 | 1'('81604378626') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] give\n"
            + "\n"
            + "'73014444040' -> 'state() / x1 | 1'('81604378626') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] state\n"
            + "\n"
            + "'223338299400' -> 'me() / x1 | 1'('81604378626') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] me\n"
            + "\n"
            + "'51539607560' -> 'largest() / x1 | 1'('81604378626') [1.0]\n"
            + "  [SecondInput] ?1\n"
            + "  [FirstInput] largest\n"
            + "\n"
            + "'30064771084' -> '__X__{2-5 +++ 0-1-0-0}'('304942678025') [1.0]\n"
            + "  [SecondInput] '__X__{0-1-0-0}'(?1)\n"
            + "  [FirstInput] '__X__{2-5}'(?1)\n"
            + "\n"
            + "'146028888075' -> 'x2 / __RL__(x1, x2) | 2'('55834574852', '42949672972') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?2\n"
            + "\n"
            + "'146028888076' -> 'x2 / __RL__(x1, x2) | 2'('55834574852', '42949672972') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?2\n"
            + "\n"
            + "'150323855364' -> 'x3 / __RL__(x1, x2) | 3'('55834574852', '60129542148', '81604378625') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?3\n"
            + "\n"
            + "'150323855363' -> 'x3 / __RL__(x1, x2) | 3'('55834574852', '60129542148', '81604378625') [1.0]\n"
            + "  [SecondInput] __RL__(?1,?2)\n"
            + "  [FirstInput] ?3\n"
            + "\n"
            + "'12884901899' -> '*(x1, x2) / x2 | 2'('51539607560', '146028888075') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'180388626441' -> '*(x1, x2) / x2 | 2'('51539607560', '146028888075') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'180388626441' -> '*(x1, x2) / x3 | 3'('51539607560', '73014444040', '150323855363') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'111669149704' -> '*(x1, x2) / x3 | 3'('51539607560', '73014444040', '81604378626') [1.0]\n"
            + "  [SecondInput] ?3\n"
            + "  [FirstInput] *(?1,?2)\n"
            + "\n"
            + "'472446402571' -> '*(x1, x2) / x2 | 2'('51539607560', '485331304459') [1.0]\n"
            + "  [SecondInput] ?2\n"
            + "  [FirstInput] *(?1,?2)";

    /**
     * 
     */
    public static final String MAPPING_RIGHT = "0-1-0-0 ||| I";

    /**
     * 
     */
    public static final String MAPPING_LEFT = "'4-5' ||| H\n"
            + "3-5 ||| K";
    
    /**
     * 
     */
    private ReplaceNonterminal rnt;
    
    /**
     * 
     */
    private InterpretedTreeAutomaton ita;
    
    @Before
    public void setUp() throws IOException {
        IrtgInputCodec iic = new IrtgInputCodec();
        ita = iic.read(TEST_AUTOMATON);
        
        rnt = new ReplaceNonterminal(new ByteArrayInputStream(MAPPING_LEFT.getBytes()),
                new ByteArrayInputStream(MAPPING_RIGHT.getBytes()), "ROOT");
    }

    /**
     * Test of introduceNonterminals method, of class ReplaceNonterminal.
     * @throws java.lang.Exception
     */
    @Test
    public void testIntroduceNonterminals() throws Exception {
        InterpretedTreeAutomaton pita = rnt.introduceNonterminals(ita,"X");
        
        System.out.println(pita);
        assertEquals(pita.getAutomaton().countTrees(),ita.getAutomaton().countTrees());
    }

}
