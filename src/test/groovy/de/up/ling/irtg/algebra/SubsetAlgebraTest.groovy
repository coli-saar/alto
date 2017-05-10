/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools + Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra


import org.junit.Test
import java.util.*
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.codec.IrtgInputCodec
import de.up.ling.irtg.codec.IrtgInputCodec
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.util.FirstOrderModel
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.saar.basic.StringTools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*


class SubsetAlgebraTest {
    @Test
    public void testParseStringSet() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) + sleep(e,r1)")
        assertThat(s, is(new HashSet(["rabbit(r1)", "sleep(e,r1)"])))
    }
    
    @Test
    public void testEvaluate() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) + sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        BitSet result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'sleep(e,r1)')"))
        assertThat(a.toSet(result), is(s))
    }
    
    @Test
    public void testEvaluateNotDisjoint() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) + sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        BitSet result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'rabbit(r1) + sleep(e,r1)')"))
        assertThat(a.toSet(result), nullValue())
    }
    
    @Test(expected=RuntimeException)
    public void testEvaluateNotElement() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) + sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        BitSet result = a.evaluate(TreeParser.parse("dunion('rabbit(r1)', 'sleep(e,r2)')"))
//        assertThat(result, nullValue())
    }
    
    @Test
    public void testParseEmptySetConstant() {
        Set s = SubsetAlgebra.parseStringSet("rabbit(r1) + sleep(e,r1)")
        SubsetAlgebra<String> a = new SubsetAlgebra(s)
        BitSet result = a.evaluate(TreeParser.parse("EMPTYSET"))
        
        assertThat(a.toSet(result), is(empty()))
    }
    
    @Test
    public void testParseEmptySet() {
        Set s = SubsetAlgebra.parseStringSet("")
        assertThat(s, is(empty()))
    }
    
    @Test
    public void testRealization() {
        FirstOrderModel model = FirstOrderModel.read(new StringReader(MODEL));
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(IRTG);
        
        SubsetAlgebra sem = (SubsetAlgebra) irtg.getInterpretation("sem").getAlgebra();
        SetAlgebra ref = (SetAlgebra) irtg.getInterpretation("ref").getAlgebra();
        StringAlgebra str = (StringAlgebra) irtg.getInterpretation("string").getAlgebra();

        // put true facts here
        ref.setModel(model);
        List trueAtoms = []
        for( Tree fact : model.getTrueAtoms() ) {
            trueAtoms.add(fact.toString());
        }
        
        sem.setOptions(StringTools.join(trueAtoms, SubsetAlgebra.SEPARATOR));

        // put inputs here
        Object refInput = ref.parseString("{e}");                        // excludes "the rabbit sleeps"
        Object semInput = sem.parseString("sleep(e,r1) + rabbit(r1)");   // excludes "the white thing sleeps"

        TreeAutomaton<?> chart = irtg.parseInputObjects(["ref": refInput, "sem": semInput]);
        assertThat(chart.language(), is(new HashSet([pt("a_sleeps_e_r1(the-r1(rabbit_r1(b_white_r1)))")])))
        // only "the white rabbit sleeps" should be left over
    }
    
    private static final String IRTG = """\n\

interpretation ref: de.up.ling.irtg.algebra.SetAlgebra
interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation sem: de.up.ling.irtg.algebra.SubsetAlgebra

S_e! -> a_sleeps_e_r1(NP_r1)
[ref] project_1(intersect_2(sleep, uniq_r1(?1)))
[string] *(?1, sleeps)
[sem] dunion('sleep(e,r1)', ?1)

NP_r1 -> the-r1(N_r1)
[ref] ?1
[string] *(the, ?1)
[sem] ?1

NP_h -> the-h(N_h)
[ref] ?1
[string] *(the, ?1)
[sem] ?1

N_r1 -> rabbit_r1(Adj_N_r1)
[ref] intersect_1(rabbit, ?1)
[string] *(?1, rabbit)
[sem] dunion('rabbit(r1)', ?1)

N_h -> hat_h(Adj_N_h)
[ref] intersect_1(hat, ?1)
[string] *(?1, hat)
[sem] dunion('hat(h)', ?1)

N_r1 -> thing_r1(Adj_N_r1)
[ref] ?1
[string] *(?1, thing)
[sem] ?1

Adj_N_r1 -> b_white_r1  [0.3]
[ref] white
[string] white
[sem] 'white(r1)'

Adj_N_r1 -> b_nop_r1    [0.7]
[ref] T
[string] ''
[sem] EMPTYSET

Adj_N_h -> b_black_h  [0.3]
[ref] black
[string] black
[sem] 'black(h)'

Adj_N_h -> b_nop_h    [0.7]
[ref] T
[string] ''
[sem] EMPTYSET

S_e2! -> takefrom_e2_r1_h(NP_r1, NP_h)
[string] *(*(take, ?1), *(from, ?2))
[ref]    project_1(intersect_3(intersect_2(takefrom, uniq_r1(intersect_1(?1, project_1(intersect_2(in, ?2))))), uniq_h(intersect_1(?2, project_2(intersect_1(in, ?1))))))
[sem] dunion(dunion('takefrom(e2,r1,h)', ?1), ?2)

    """;
    
    private static final String MODEL = '''\n\
{"univ": [["e"], ["e2"], ["r1"], ["r2"], ["h"], ["h2"], ["f"]],
 "sleep": [["e", "r1"]],
 "takefrom": [["e2", "r1", "h"]],
 "rabbit": [["r1"], ["r2"]],
 "white": [["r1"]],
 "brown": [["r2"]],
 "black": [["h"]],
 "in": [["r1","h"], ["f","h2"]],
 "hat": [["h"], ["h2"]] }


    ''';
}