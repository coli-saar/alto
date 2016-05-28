/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.TreeParser;
import java.io.ByteArrayInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author teichmann
 */
public class WASPTreeAutomatonCodecTest {

    private final String GRAMMAR_SAMPLE = "*n:CityName -> ({ ' west valley ' })\n"
            + "*n:CityName -> ({ ' westland ' })\n"
            + "*n:Country -> ({ countryid ( *n:CountryName ) }) zero-fertility\n"
            + "*n:Country -> ({ country ( all ) })\n"
            + "*n:Query -> ({ answer ( *n:City ) })\n"
            + "*n:Query -> ({ answer ( *n:Country ) })\n"
            + "*n:City -> ({ city ( all ) })\n"
            + "*n:City -> ({ cityid ( *n:CityName , *n:StateAbbrev ) }) zero-fertility\n"
            + "*n:City -> ({ cityid ( *n:CityName , _ ) }) zero-fertility\n"
            + "*n:City -> ({ intersection ( *n:City , *n:City ) })\n"
            + "*n:StateAbbrev -> ({ ' nc ' })\n"
            + "*n:StateAbbrev -> ({ ' wv ' })\n"
            + "*n:City -> ({ largest_one ( density_1 ( *n:City ) ) })\n"
            + "*n:Num -> ({ *t:Num })";

    /**
     * Test of read method, of class WASPTreeAutomatonCodec.
     */
    @Test
    public void testRead() throws Exception {
        WASPTreeAutomatonCodec wtac = new WASPTreeAutomatonCodec();

        TreeAutomaton<String> s = wtac.read(new ByteArrayInputStream(GRAMMAR_SAMPLE.getBytes()));

        int size = 0;
        Iterable<Rule> ita = s.getRuleSet();
        for (Rule r : ita) {
            ++size;
        }
        
        assertEquals(size,17);
        assertTrue(s.isCyclic());
        
        assertTrue(s.accepts(TreeParser.parse("answer(country(all))")));
        assertTrue(s.accepts(TreeParser.parse("answer(intersection(cityid(__QUOTE__westland__QUOTE__,__QUOTE__wv__QUOTE__),city(all)))")));
        assertTrue(s.accepts(TreeParser.parse("answer(cityid('__QUOTE__west valley__QUOTE__',_))")));
        assertTrue(s.accepts(TreeParser.parse("answer(largest_one(density_1(cityid('__QUOTE__west valley__QUOTE__',_))))")));
    }
}
