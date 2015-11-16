/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractStringFunqlGeoqueryTest {

    /**
     * 
     */
    private final static String TEST_INPUT =    "<examples><example id=\"0\"><nl lang=\"en\">\n" +
                                               "Give me the cities in Virginia .\n" +
                                               "</nl><nl lang=\"zh\">\n" +
                                               "Weijiniya zhou you shenme chengshi ?\n" +
                                               "</nl><syn lang=\"en\">\n" +
                                               "(S (VP (VB Give) (NP (PRP me)) (NP (NP (DT the) (NNS cities)) (PP (IN in) (NP (NNP Virginia))))))\n" +
                                               "</syn><syn lang=\"zh\">\n" +
                                               "(S Weijiniya zhou you shenme chengshi)\n" +
                                               "</syn><augsyn lang=\"en\">\n" +
                                               "(S Give-[Query:0] me-[Query:0] the-[City:1] cities-[City:1] in-[City:2] Virginia-[StateName:4])\n" +
                                               "</augsyn><mrl lang=\"geo-funql\">\n" +
                                               "answer(city(loc_2(stateid('virginia'))))\n" +
                                               "</mrl><mrl lang=\"geo-prolog\">\n" +
                                               "answer(A,(city(A),loc(A,B),const(B,stateid(virginia))))\n" +
                                               "</mrl><mrl-parse><node id=\"0\"> *n:Query -> ({ answer ( *n:City ) }) </node><node id=\"1\"> *n:City -> ({ city ( *n:City ) }) </node><node id=\"2\"> *n:City -> ({ loc_2 ( *n:State ) }) </node><node id=\"3\"> *n:State -> ({ stateid ( *n:StateName ) }) </node><node id=\"4\"> *n:StateName -> ({ ' virginia ' }) </node></mrl-parse></example><example id=\"1\"><nl lang=\"en\">\n" +
                                               "What are the high points of states surrounding Mississippi ?\n" +
                                               "</nl><nl lang=\"es\">\n" +
                                               "que son los puntos altos de los estados rodeando a mississippi ?\n" +
                                               "</nl><nl lang=\"ja\">\n" +
                                               "mishishippi no mawari ni aru shuu no takai chiten wa nan desu ka ?\n" +
                                               "</nl><nl lang=\"tr\">\n" +
                                               "mississippi yi saran eyaletlerin yuksek noktalari nelerdir ?\n" +
                                               "</nl><syn lang=\"en\">\n" +
                                               "(SBARQ (WHNP (WP What)) (SQ (VBP are) (NP (NP (DT the) (JJ high) (NNS points)) (PP (IN of) (NP (NP (NNS states)) (VP (VBG surrounding) (NP (NNP Mississippi))))))))\n" +
                                               "</syn><syn lang=\"es\">\n" +
                                               "(S que son los puntos altos de los estados rodeando a mississippi)\n" +
                                               "</syn><syn lang=\"ja\">\n" +
                                               "(S mishishippi no mawari ni aru shuu no takai chiten wa nan desu ka)\n" +
                                               "</syn><syn lang=\"tr\">\n" +
                                               "(S mississippi yi saran eyaletlerin yuksek noktalari nelerdir)\n" +
                                               "</syn><augsyn lang=\"en\">\n" +
                                               "(S What-[Query:0] are-[Query:0] the-[Place:1] high-[Place:1] points-[Place:1] of-[Place:1] states-[State:2] surrounding-[State:3] Mississippi-[StateName:5])\n" +
                                               "</augsyn><mrl lang=\"geo-funql\">\n" +
                                               "answer(high_point_1(state(next_to_2(stateid('mississippi')))))\n" +
                                               "</mrl><mrl lang=\"geo-prolog\">\n" +
                                               "answer(A,(high_point(B,A),state(B),next_to(B,C),const(C,stateid(mississippi))))\n" +
                                               "</mrl><mrl-parse><node id=\"0\"> *n:Query -> ({ answer ( *n:Place ) }) </node><node id=\"1\"> *n:Place -> ({ high_point_1 ( *n:State ) }) </node><node id=\"2\"> *n:State -> ({ state ( *n:State ) }) </node><node id=\"3\"> *n:State -> ({ next_to_2 ( *n:State ) }) </node><node id=\"4\"> *n:State -> ({ stateid ( *n:StateName ) }) </node><node id=\"5\"> *n:StateName -> ({ ' mississippi ' }) </node></mrl-parse></example>"+
                                               "<example id=\"879\"><nl lang=\"en\">\n" +
                                               "Which US city has the highest population density ?\n" +
                                               "</nl><syn lang=\"en\">\n" +
                                               "(SBARQ (WHNP (WDT Which) (NNP US) (NN city)) (SQ (VP (VBZ has) (NP (DT the) (JJS highest) (NN population) (NN density)))))\n" +
                                               "</syn><augsyn lang=\"en\">\n" +
                                               "(S Which-[City:2] US-[City:2] city-[City:2] has-[City:1] the-[City:1] highest-[City:1] population-[City:1] density-[City:1])\n" +
                                               "</augsyn><mrl lang=\"geo-funql\">\n" +
                                               "answer(largest_one(density_1(city(all))))\n" +
                                               "</mrl><mrl lang=\"geo-prolog\">\n" +
                                               "answer(A,largest(B,(city(A),density(A,B))))\n" +   
                                               "</mrl><mrl-parse><node id=\"0\"> *n:Query -> ({ answer ( *n:City ) }) </node><node id=\"1\"> *n:City -> ({ largest_one ( density_1 ( *n:City ) ) }) </node><node id=\"2\"> *n:City -> ({ city ( all ) }) </node></mrl-parse></example></examples>";

    /**
     * 
     */
    private final static String SOLUTION = "0\n" +
                                           "Give me the cities in Virginia .\n" +
                                           "answer(A,(city(A),loc(A,B),const(B,stateid(virginia))))\n" +
                                           "answer(city(loc_2(stateid('virginia'))))\n" +
                                           "\n" +
                                           "1\n" +
                                           "What are the high points of states surrounding Mississippi ?\n" +
                                           "answer(A,(high_point(B,A),state(B),next_to(B,C),const(C,stateid(mississippi))))\n" +
                                           "answer(high_point_1(state(next_to_2(stateid('mississippi')))))\n" +
                                           "\n" +
                                           "879\n" +
                                           "Which US city has the highest population density ?\n" +
                                           "answer(A,largest(B,(city(A),density(A,B))))\n" +
                                           "answer(largest_one(density_1(city(all))))\n\n";
    
    /**
     * 
     * 
     * Test of obtain method, of class ExtractStringFunqlGeoquery.
     */
    @Test
    public void testObtain() throws Exception {
        InputStream in = new ByteArrayInputStream(TEST_INPUT.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        ExtractStringFunqlGeoquery.obtain(in, out, ExtractStringFunqlGeoquery.OutputFormat.ONE_PER_LINE_NEWLINE);
        
        in.close();
        out.close();
        
        String s = new String(out.toByteArray());
        System.out.println(s);
        System.out.println("+++++++++++++");
        System.out.println(SOLUTION);
        
        assertEquals(s,SOLUTION);
    }
    
}
