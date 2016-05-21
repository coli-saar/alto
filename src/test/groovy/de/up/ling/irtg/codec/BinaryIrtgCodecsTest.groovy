/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.codec.SgraphAmrOutputCodec
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.algebra.graph.*;
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class BinaryIrtgCodecsTest {
    @Test
    public void encodeDecode() {
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(GRAMMAR);

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        new BinaryIrtgOutputCodec().write(irtg, o);
        o.close();

        InputStream is = new ByteArrayInputStream(o.toByteArray());
        InterpretedTreeAutomaton irtg2 = new BinaryIrtgInputCodec().read(is);

        TreeAutomaton chart = irtg2.parse(["string": "the boy wants to go"]);
        assert chart.accepts(pt("comb_subj(boy,want1(go))"));
    }

    public static String GRAMMAR = '''


interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

/*** Section 4.1 / Figure 5: Complements ***/

S! -> comb_subj(NP, VP)
[string] *(?1, ?2)
[graph]  f_subj(merge(?2, r_subj(?1)))

// for simplicity, use just VP for both "sleep" and "sleeps",
// and for "go" and "goes"
// => grammar will overgenerate when mapping graph to string
VP -> sleep
[string] sleep
[graph]  "(g<root> / sleep  :ARG0 (s<subj>))"

VP -> sleeps
[string] sleeps
[graph]  "(g<root> / sleep  :ARG0 (s<subj>))"

VP -> go
[string] go
[graph]  "(g<root> / go  :ARG0 (s<subj>))"

VP -> goes
[string] goes
[graph]  "(g<root> / go  :ARG0 (s<subj>))"

NP -> boy
[string] *(the, boy)
[graph]  "(x<root> / boy)"

NP -> girl
[string] *(the, girl)
[graph]  "(x<root> / girl)"

// subject-control "want"
VP -> want1(VP)
[string] *(*(wants, to), ?1)
[graph]  f_vcomp(merge("(u<root> / want  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))", r_vcomp(?1)))

// object-control "want"
VP -> want2(NP, VP)
[string] *(*(wants, ?1), *(to, ?2))
[graph]  f_vcomp(merge("(u<root> / want  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))",
                       f_obj(merge(r_obj(?1),
                                   r_subj_obj(r_vcomp(?2))))))


/*** Section 4.2 / Figure 8: Modifiers ***/

NP -> mod_rc(NP, RC)
[string] *(?1, ?2)
[graph]  merge(?1, ?2)

RC -> rc(RP, VP)
[string] *(?1, ?2)
[graph]  r_subj_root(f_root(merge(?2, r_subj(?1))))

RP -> who
[string] who
[graph]  "(u<root>)"

VP -> coord(VP, VP)
[string] *(?1, *(and, ?2))
[graph]  f_1(f_2(merge(merge("(u<root> / and :op1 (v<1>) :op2 (w<2>))",
                             r_1(?1)),
                       r_2(?2))))

VP -> sometimes(VP)
[string] *(sometimes, ?1)
[graph]  merge("(u<root> :time (v / sometimes))", ?1)

VP -> snore
[string] snores
[graph]  "(u<root> / snore :ARG0 (v<subj>))"
''';

}

