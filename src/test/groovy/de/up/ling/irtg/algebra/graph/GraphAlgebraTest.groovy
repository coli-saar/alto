/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.algebra.graph


import org.junit.*
import java.util.*
import java.io.*
import java.util.logging.Level;
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*;
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.util.*
import de.up.ling.irtg.*

import org.jgrapht.*;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

/**
 *
 * @author koller
 */
class GraphAlgebraTest {
    @Test
    public void testEvaluate1() {
        GraphAlgebra alg = new GraphAlgebra();
        
        Tree<String> term = pt("merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)'))");
        SGraph gold = pg("(u<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp>))")
        
        assertEquals(gold, alg.evaluate(term))
    }
    
    @Test
    public void testEvaluate2() {
        GraphAlgebra alg = new GraphAlgebra();
        
        Tree<String> term = pt("merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj('(x<root> / boy)')), r_vcomp(r_subj_subj('(g<root> / go-01  :ARG0 (s<subj>))')))");
        SGraph gold = pg("(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))")
        
        assertEquals(gold, alg.evaluate(term))
    }
    
    
    
    @Test
    public void testParseGraph() {
        InterpretedTreeAutomaton irtg = pi(HRG);
        TreeAutomaton chart = irtg.parse(["graph":"(w<root> / want-01  :ARG0 (b<subj> / boy)  :ARG1 (g<vcomp> / go-01 :ARG0 b))"])
        
        assertEquals(new HashSet([pt("top(want2(boy,go))")]), chart.language())
    }
    
    @Test
    public void testParseGraphWithCoref() {
        InterpretedTreeAutomaton irtg = pi(COREF);
        TreeAutomaton chart = irtg.parse(["graph":"(u91<root> / want-01  :ARG0 (u92<coref1> / bill)  :ARG1 (u93 / like-01           :ARG0 (u94 / girl)	  :ARG1 u92)  :dummy u94)"])
        
        assertEquals(new HashSet([pt("want3(bill, girl, like(bill))"), pt("want3(bill, girl, like(him))"), pt("want3(him, girl, like(bill))")]),
                     chart.language())
    }
    
    public static String COREF="""
    
interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra


S! -> want2(NP, VP)
[string] *(?1, *(wants, *(to, ?2)))
[graph]  f_subj_vcomp(merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2))))

S -> want3(NP, NP, VP)
[string] *(?1, *(wants, *(?2, *(to, ?3))))
[graph] f_subj_obj_vcomp(merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', 
                          r_subj(?1)), 
                    r_obj(?2)), 
              r_vcomp(r_subj_obj(?3))))

VP -> like(NP)
[string] *(like, ?1)
[graph] f_obj(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_obj(?1)))

S -> likes(NP,NP)
[string] *(?1, *(likes, ?2))
[graph]  f_subj_obj(merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2)))

NP -> bill
[string] bill
[graph] '(b<root,coref1> / bill)'

NP -> him
[string] him
[graph] '(b<root,coref1>)'

NP -> girl
[string] *(the, girl)
[graph]  '(x<root> / girl)'
""";
    
    public static final String HRG = """\n\

interpretation string: de.up.ling.irtg.algebra.StringAlgebra
interpretation graph: de.up.ling.irtg.algebra.graph.GraphAlgebra

Start! -> top(S)
[string] ?1
[graph]  merge('()', ?1)
/* this deletes all sources from ?1 */

S -> want2(NP, VP)
[string] *(?1, *(wants, *(to, ?2)))
[graph]  merge(merge('(u<root> / want-01  :ARG0 (b<subj>)  :ARG1 (g<vcomp>))', r_subj(?1)), r_vcomp(r_subj_subj(?2)))

S -> want3(NP, NP, VP)
[string] *(?1, *(wants, *(?2, *(to, ?3))))
[graph] merge(merge(merge('(u<root> / want-01  :ARG0 (v<subj>)  :ARG1 (w<vcomp>)  :dummy (x<obj>))', 
                          r_subj(?1)), 
                    r_obj(?2)), 
              r_vcomp(r_subj_obj(?3)))

NP -> boy
[string] *(the, boy)
[graph]  '(x<root> / boy)'

NP -> girl
[string] *(the, girl)
[graph]  '(x<root> / girl)'

// every VP has a 'subj' source at which the subject is inserted
VP -> believe(S)
[string] *(believe, *(that, ?1))
[graph]  merge('(u<root> / believe-01  :ARG0 (v<subj>)  :ARG1 (w<xcomp>))', r_xcomp(f_root(?1)))

S -> likes(NP,NP)
[string] *(?1, *(likes, ?2))
[graph]  merge(merge('(u<root> / like-01  :ARG0 (v<subj>)  :ARG1 (w<obj>))', r_subj(?1)), r_obj(?2))

VP -> go
[string] go
[graph]  '(g<root> / go-01  :ARG0 (s<subj>))'




    """;
}

