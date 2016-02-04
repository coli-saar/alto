/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.rule_finding.preprocessing.geoquery.CreateLexicon.SimpleCheck;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class CreateLexiconTest {

    /**
     *
     */
    private final static String TEST_FACTS = "\n"
            + "/***************************************************\n"
            + "Following is a description of the geobase predicates:\n"
            + "\n"
            + "state(name, abbreviation, capital, population, area, state_number, city1, city2, city3, city4)\n"
            + "\n"
            + "city(state, state_abbreviation, name, population)\n"
            + "\n"
            + "river(name, length, [states through which it flows])\n"
            + "\n"
            + "border(state, state_abbreviation, [states that border it])\n"
            + "\n"
            + "highlow(state, state_abbreviation, highest_point, highest_elevation, lowest_point, lowest_elevation)\n"
            + "\n"
            + "mountain(state, state_abbreviation, name, height)\n"
            + "\n"
            + "road(number, [states it passes through])\n"
            + "\n"
            + "lake(name, area, [states it is in])\n"
            + "\n"
            + "****************************************************/\n"
            + "state('District of Columbia', 'dc','washington',638.0e+3,1100,0,'tenleytown','washington','georgetown','duval circle').\n"
            + "state('pennsylvania','pa','harrisburg',11.863e+6,45308,2,'philadelphia','pittsburgh','erie','allentown').\n"
            + "state('rhode island','ri','providence',947.2e+3,1212,13,'providence','warwick','cranston','pawtucket').\n"
            + "city('alabama','al','tuscaloosa',75143).\n"
            + "city('alaska','ak','anchorage',174431).\n"
            + "city('alaska','ak','columbia',174431).\n"
            + "city('District of Columbia','dc','washington',638333).\n"
            + "river('san juan',579,['colorado','new mexico','colorado','utah']).\n"
            + "river('tennessee',1049,['tennessee','alabama','tennessee','kentucky']).\n"
            + "river('wabash',764,['ohio','indiana','illinois']).\n"
            + "border('nevada','nv',['idaho','utah','arizona','california','oregon']).\n"
            + "border('new hampshire','nh',['maine','massachusetts','vermont']).\n"
            + "border('new jersey','nj',['new york','delaware','pennsylvania']).\n\n"
            + "highlow('nevada','nv','boundary peak',4005,'colorado river',143).\n"
            + "highlow('new hampshire','nh','mount washington',1917,'atlantic ocean',0).\n"
            + "mountain('alaska','ak','browne tower',4429).\n"
            + "mountain('california','ca','whitney',4418).\n"
            + "mountain('colorado','co','elbert',4399).\n"
            + "road('80',['new york','new jersey','pennsylvania','ohio','indiana','illinois','iowa','nebraska','wyoming','utah','nevada','california']).\n"
            + "road('70',['District of Columbia','maryland','pennsylvania','ohio','indiana','illinois','missouri','kansas','colorado','utah']).\n"
            + "lake('great salt lake',5180,['utah']).\n"
            + "lake('lake of the woods',4391,['minnesota']).\n"
            + "lake('iliamna',2675,['alaska']).\n"
            + "country('usa',307890000,9826675).\n\n"
            + "state('texas','tx','austin',14.229e+6,266.807e+3,28,'houston','dallas','san antonio','el paso').\n";

    /**
     * 
     */
    private CreateLexicon cl;
    
    
    @Before
    public void setUp() throws IOException {
        InputStream into = new ByteArrayInputStream(TEST_FACTS.getBytes());
        
        cl = new CreateLexicon(into);
    }

    /**
     * Test of replace method, of class CreateLexicon.
     */
    @Test
    public void testReplace() {
        List<Pair<String,String>> ins = new ArrayList<>();
        Pair<String,String> p1 = new Pair<>("How many capitals does Rhode Island have ?","answer(count(capital(loc_2(stateid('rhode island')))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many cities are there in USA ?","answer(count(city(loc_2(countryid('usa')))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many cities are there in district of columbia ?","answer(count(city(loc_2(countryid('district of columbia')))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many cities are there in district columbia ?","answer(count(city(loc_2(countryid('columbia')))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many cities are there in the US ?","answer(count(city(loc_2(countryid('usa')))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many people live in Austin Texas ?","answer(population_1(cityid('austin', 'tx')))");
        ins.add(p1);
        
        p1 = new Pair<>("Count the states which have elevations lower than what Alabama has .","answer(count(state(low_point_2(lower_2(low_point_1(stateid('alabama')))))))");
        ins.add(p1);
        
        p1 = new Pair<>("How many cities named Austin are there in the USA ?","answer(count(intersection(city(cityid('austin', _)), loc_2(countryid('usa')))))");
        ins.add(p1);
        
        p1 = new Pair<>("Which states border New     York ?","answer(state(next_to_2(stateid('new     york'))))");
        ins.add(p1);
        
        Set<Pair<String,String>> set = new HashSet<>();
        
        Iterable<Pair<String,String>> stream = ins;
        stream = cl.replace(stream);
        stream.forEach((Pair<String,String> p) -> {
            set.add(p);
        });
        
        assertEquals(set.size(),9);
        
        assertTrue(set.contains(new Pair<>("how many capitals does state_____1 have ?","answer(count(capital(loc_2(stateid('state_____1')))))")));
        assertTrue(set.contains(new Pair<>("how many cities are there in country_____1 ?","answer(count(city(loc_2(countryid('country_____1')))))")));
        assertTrue(set.contains(new Pair<>("how many cities are there in the us ?","answer(count(city(loc_2(countryid('usa')))))")));
        assertTrue(set.contains(new Pair<>("how many people live in city_____1 abbrev_____1 ?","answer(population_1(cityid('city_____1', 'abbrev_____1')))")));
        assertTrue(set.contains(new Pair<>("count the states which have elevations lower than what state_____1 has .","answer(count(state(low_point_2(lower_2(low_point_1(stateid('state_____1')))))))")));
        assertTrue(set.contains(new Pair<>("how many cities named city_____1 are there in the country_____1 ?","answer(count(intersection(city(cityid('city_____1', _)), loc_2(countryid('country_____1')))))")));
        assertTrue(set.contains(new Pair<>("which states border state_____1 ?","answer(state(next_to_2(stateid('state_____1'))))")));
        assertTrue(set.contains(new Pair<>("how many cities are there in district city_____1 ?","answer(count(city(loc_2(countryid('city_____1')))))")));
        assertTrue(set.contains(new Pair<>("how many cities are there in state_____1 ?","answer(count(city(loc_2(countryid('state_____1')))))")));
        
        
        assertTrue(CreateLexicon.isSpecial("state_____1"));
        assertTrue(CreateLexicon.isSpecial(CreateLexicon.makeSpecial("u", 2)));
        assertFalse(CreateLexicon.isSpecial("state____1"));
        assertFalse(CreateLexicon.isSpecial("state_____1d"));
    }
    
    
    @Test
    public void testGetCheck() {
        SimpleCheck check = this.cl.getCheck();
        
        String[] portions = "Which states border New     York ?".toLowerCase().trim().split("\\s+");
        
        for(int i=0;i<portions.length;++i) {
            if(i != 3) {
                assertEquals(check.knownPattern(i, portions),0);
            } else {
                assertEquals(check.knownPattern(i, portions),2);
            }
        }
        
        portions = "How many cities named Austin are there in the USA ?".toLowerCase().trim().split("\\s+");
        for(int i=0;i<portions.length;++i) {
            if(i != 4 && i != 9) {
                assertEquals(check.knownPattern(i, portions),0);
            } else {
                assertEquals(check.knownPattern(i, portions),1);
            }
        }
    }
}
