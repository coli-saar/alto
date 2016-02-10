/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import de.up.ling.irtg.util.FunctionIterable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class AntiUnknownTest {
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

    
    private final static String[] TEST_CORPUS = {
            "How many cities does the USA have ?",
            "answer(count(city(loc_2(countryid('usa')))))",
            "How many cities named Austin are there in the USA ?",
            "answer(count(intersection(city(cityid('austin', _)), loc_2(countryid('usa')))))",
            "How many citizens does the biggest city have in the USA ?",
            "answer(population_1(largest(city(all))))",
            "How many citizens in Alabama ?",
            "answer(population_1(stateid('alabama')))",
            "How many citizens in Boulder ?",
            "answer(population_1(cityid('boulder', _)))",
            "How many citizens live in California ?",
            "answer(population_1(stateid('california')))",
            "How many citizens exist in new york ?",
            "answer(population_1(cityid('new york')))"
    };
    
    /**
     * 
     */
    private AntiUnknown anu;
    
    @Before
    public void setUp() throws IOException {
        InputStream ins = new ByteArrayInputStream(TEST_FACTS.getBytes());
        
        anu = new AntiUnknown(2, 4, 2, ins);
    }

    /**
     * Test of reduceUnknownWithFacts method, of class AntiUnknown.
     * @throws java.io.IOException
     */
    @Test
    public void testReduceUnknownWithFacts_Iterable() throws IOException {
        List<Pair<String,String>> list = new ArrayList<>();
        for(int i=0;i<TEST_CORPUS.length;i += 2) {
            list.add(new Pair<>(TEST_CORPUS[i], TEST_CORPUS[i+1]));
        }
        Iterable<String> statSource =
                new FunctionIterable<>(list,(Pair<String,String> pa) -> pa.getLeft());
        
        Iterable<Pair<String,String>> it = this.anu.reduceUnknownWithFacts(list,statSource);
        Set<String> seen = new HashSet<>();
        
        
        for(Pair<String,String> p : it) {
            seen.add(p.toString());
        }
        
        assertEquals(seen.size(),7);
        
        System.out.println(seen);
        
        assertTrue(seen.contains("how many citi does the country_____1 have ?,answer(count(city(loc_2(countryid('country_____1')))))"));
        assertTrue(seen.contains("how many citi __unknown__ city_____1 __unknown__ __unknown__ in the country_____1 ?,answer(count(intersection(city(cityid('city_____1', _)), loc_2(countryid('country_____1')))))"));
        assertTrue(seen.contains("how many citize does the __unknown__ __unknown__ have in the usa ?,answer(population_1(largest(city(all))))"));
        assertTrue(seen.contains("how many citize in state_____1 ?,answer(population_1(stateid('state_____1')))"));
        assertTrue(seen.contains("how many citize in __unknown__ ?,answer(population_1(cityid('boulder', _)))"));
        assertTrue(seen.contains("how many citize __unknown__ in state_____1 ?,answer(population_1(stateid('state_____1')))"));
        assertTrue(seen.contains("how many citize __unknown__ in state_____1 ?,answer(population_1(cityid('state_____1')))"));
    }

    /**
     * Test of reduceUnknownWithoutFacts method, of class AntiUnknown.
     */
    @Test
    public void testReduceUnknownWithoutFacts() {
        List<Pair<String,String>> list = new ArrayList<>();
        for(int i=0;i<TEST_CORPUS.length;i += 2) {
            list.add(new Pair<>(TEST_CORPUS[i], TEST_CORPUS[i+1]));
        }
        
        Iterable<Pair<String,String>> reduced = this.anu.reduceUnknownWithoutFacts(list);
        Set<String> seen = new HashSet<>();
        
        for(Pair<String,String> pa : reduced) {
            seen.add(pa.toString());
        }
        
        assertEquals(seen.size(),7);
        
        assertTrue(seen.contains("how many citi does the usa have ?,answer(count(city(loc_2(countryid('usa')))))"));
        assertTrue(seen.contains("how many citi __unknown__ austin __unknown__ __unknown__ in the usa ?,answer(count(intersection(city(cityid('austin', _)), loc_2(countryid('usa')))))"));
        assertTrue(seen.contains("how many citize does the __unknown__ __unknown__ have in the usa ?,answer(population_1(largest(city(all))))"));
        assertTrue(seen.contains("how many citize in alabama ?,answer(population_1(stateid('alabama')))"));
        assertTrue(seen.contains("how many citize in __unknown__ ?,answer(population_1(cityid('boulder', _)))"));
        assertTrue(seen.contains("how many citize __unknown__ in california ?,answer(population_1(stateid('california')))"));
        assertTrue(seen.contains("how many citize __unknown__ in new york ?,answer(population_1(cityid('new york')))"));
    }
}
