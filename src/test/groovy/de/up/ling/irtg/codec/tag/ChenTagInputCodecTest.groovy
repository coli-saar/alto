/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tag


import org.junit.Test
import java.util.*
import org.junit.BeforeClass
import java.io.*
import com.google.common.collect.Iterators
import de.saar.basic.Pair
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.codec.tag.ChenTagInputCodec
import de.up.ling.irtg.codec.tag.ChenTagInputCodec.NodePosToChildrenPos
import de.up.ling.irtg.codec.tag.TagGrammar
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;


/**
 *
 * @author koller
 */
class ChenTagInputCodecTest {
    private static final Tree<String> DT = pt("t83-join_drole__root__dsubcat__NP0_NP1_NP2_as___pos__VB__preterm__V__sgp1__as__srole__root__ssubcat__NP0_NP1_NP2_as___voice__act_(t3-Vinken_drole__0__pos__NNP__preterm__N__srole__0_(*NOP*_N_A,'t21-,_drole__adj__mdir__left__modifee__NP__pos_____preterm__Punct__srole__adj_'(*NOP*_Punct_A,*NOP*_NP_A)),*NOP*_V_A,t3-board_drole__1__pos__NN__preterm__N__srole__1_(*NOP*_N_A,t1-the_drole__adj__mdir__right__modifee__NP__pos__DT__preterm__D__srole__adj_(*NOP*_D_A,*NOP*_NP_A)),*NOP*_IN_A,t3-director_drole__2__pos__NN__preterm__N__srole__2_(*NOP*_N_A,t36-nonexecutive_drole__adj__mdir__right__modifee__NP__pos__JJ__preterm__A__srole__adj_(*NOP*_A_A,*NOP*_NP_A)),*NOP*_PP_A,'t65-Nov._drole__adj__mdir__left__modifee__VP__pos__NNP__preterm__N__srole__adj_'(*NOP*_N_A,t48-29_drole__adj__mdir__left__modifee__NP__pos__CD__preterm__N__srole__adj_(*NOP*_N_A,*NOP*_NP_A),*NOP*_VP_A),'t26-._drole__adj__mdir__left__modifee__S__pos_____preterm_____srole__adj_'('*NOP*_._A',*NOP*_S_A))")
    @Test
    public void testReadCorpus() {
        ChenTagInputCodec ic = new ChenTagInputCodec();
        TagGrammar tagg = ic.readUnlexicalizedGrammar(new StringReader(GRAMMAR));
//        tagg.setTracePredicate(s -> s.contains("-NONE-"));

        List<Tree<String>> rawDerivationTrees = ic.lexicalizeFromCorpus(tagg, new StringReader(DEPS));
        
//        System.err.println(DT);
//        System.err.println(rawDerivationTrees.get(0))
        
        assertThat(rawDerivationTrees.size(), is(1))
        assertThat(rawDerivationTrees.get(0), is(DT))
    }
    
    @Test
    public void testToIrtg() {
        ChenTagInputCodec ic = new ChenTagInputCodec();
        TagGrammar tagg = ic.readUnlexicalizedGrammar(new StringReader(GRAMMAR));
        List<Tree<String>> rawDerivationTrees = ic.lexicalizeFromCorpus(tagg, new StringReader(DEPS));
        
        InterpretedTreeAutomaton irtg = tagg.toIrtg();
        
        assertThat("dt in signature", irtg.getAutomaton().getSignature().mapSymbolsToIds(DT), notNullValue())
        
        Pair<String,String> expected = irtg.getInterpretation("string").getAlgebra().parseString("Vinken , join the board as nonexecutive director Nov. 29 .")
        assertThat("string interp", irtg.getInterpretation("string").interpret(DT), is(expected))
        
        assertThat("tree interp", irtg.getInterpretation("tree").interpret(DT), is(pt("S(S(NP(NP(N(Vinken)),Punct(',')),VP(VP(V(join),NP(D(the),NP(N(board))),PP(IN(as),NP(A(nonexecutive),NP(N(director))))),NP(NP(N('Nov.')),N('29')))),'.'('.'))")))
    }
    
    @Test
    public void testNodePosInitial() {
        ChenTagInputCodec ic = new ChenTagInputCodec();
        TagGrammar tagg = ic.readUnlexicalizedGrammar(new StringReader(GRAMMAR));
        List<Tree<String>> rawDerivationTrees = ic.lexicalizeFromCorpus(tagg, new StringReader(DEPS));
        
        NodePosToChildrenPos npcp = new NodePosToChildrenPos(tagg);
        
        assertThat(npcp.getMap("t83"),  is([1:7, 2:0, 3:6, 4:1, 5:2, 6:5, 7:3, 8:4]))
    }
    
    @Test
    public void testNodePosAuxiliary() {
        ChenTagInputCodec ic = new ChenTagInputCodec();
        TagGrammar tagg = ic.readUnlexicalizedGrammar(new StringReader(GRAMMAR));
        List<Tree<String>> rawDerivationTrees = ic.lexicalizeFromCorpus(tagg, new StringReader(DEPS));
        
        NodePosToChildrenPos npcp = new NodePosToChildrenPos(tagg);
        
        assertThat(npcp.getMap("t187"),  is([1:2, 3:1, 4:0]))
    }
    

    private static final String DEPS = """\n\
1 Pierre NNP 2 Vinken NNP 1 t2 t3 1 pos=NNP preterm=N modifee=NP mdir=right drole=adj srole=adj
2 Vinken NNP 9 join VB 7 t3 t83 2 pos=NNP preterm=N drole=0 srole=0
3 , , 2 Vinken NNP -1 t21 t3 1 pos=, preterm=Punct modifee=NP mdir=left drole=adj srole=adj
4 61 CD 5 years NNS 1 t2 t186 2 pos=CD preterm=N modifee=NP mdir=right drole=adj srole=adj
5 years NNS 6 old JJ 1 t186 t187 3 pos=NNS preterm=N modifee=AP mdir=right drole=adj srole=adj
6 old JJ 2 Vinken NNP -4 t187 t3 1 pos=JJ preterm=A modifee=NP mdir=left drole=adj srole=adj
7 , , 2 Vinken NNP -5 t21 t3 1 pos=, preterm=Punct modifee=NP mdir=left drole=adj srole=adj
8 will MD 9 join VB 1 t45 t83 3 pos=MD preterm=MD aux=y modifee=VP mdir=right drole=adj srole=adj
9 join VB 9 join VB 0 t83 t83 root pos=VB preterm=V sgp1=as dsubcat=NP0_NP1_NP2(as) ssubcat=NP0_NP1_NP2(as) drole=root srole=root voice=act
10 the DT 11 board NN 1 t1 t3 1 pos=DT preterm=D modifee=NP mdir=right drole=adj srole=adj
11 board NN 9 join VB -2 t3 t83 5 pos=NN preterm=N drole=1 srole=1
12 as IN 9 join VB -3 tCO t83 7 pos=IN preterm=IN
13 a DT 15 director NN 2 t1 t3 1 pos=DT preterm=D modifee=NP mdir=right drole=adj srole=adj
14 nonexecutive JJ 15 director NN 1 t36 t3 1 pos=JJ preterm=A modifee=NP mdir=right drole=adj srole=adj
15 director NN 9 join VB -6 t3 t83 8 pos=NN preterm=N drole=2 srole=2
16 Nov. NNP 9 join VB -7 t65 t83 3 pos=NNP preterm=N modifee=VP mdir=left drole=adj srole=adj
17 29 CD 16 Nov. NNP -1 t48 t65 3 pos=CD preterm=N modifee=NP mdir=left drole=adj srole=adj
18 . . 9 join VB -9 t26 t83 1 pos=. preterm=. modifee=S mdir=left drole=adj srole=adj
...EOS...
    """;
    
    private static final String GRAMMAR = """\n\
t2 NP##1#l# N##2#l#h N##2#r#h NP##3#l#f NP##3#r#f NP##1#r# \n\
t3 NP##1#l# N##2#l#h N##2#r#h NP##1#r# \n\
t21 NP##1#l# NP##2#l#f NP##2#r#f Punct##3#l#h Punct##3#r#h NP##1#r# \n\
t186 AP##1#l# NP##2#l# N##3#l#h N##3#r#h NP##2#r# AP##4#l#f AP##4#r#f AP##1#r# \n\
t187 NP##1#l# NP##2#l#f NP##2#r#f AP##3#l# A##4#l#h A##4#r#h AP##3#r# NP##1#r# \n\
t45 VP##1#l# MD##2#l#h MD##2#r#h VP##3#l#f VP##3#r#f VP##1#r# \n\
t83 S##1#l# NP#0#2#l#s NP#0#2#r#s VP##3#l# V##4#l#h V##4#r#h NP#1#5#l#s NP#1#5#r#s PP##6#l# IN##7#l#c IN##7#r#c NP#2#8#l#s NP#2#8#r#s PP##6#r# VP##3#r# S##1#r# \n\
t1 NP##1#l# D##2#l#h D##2#r#h NP##3#l#f NP##3#r#f NP##1#r# \n\
t36 NP##1#l# A##2#l#h A##2#r#h NP##3#l#f NP##3#r#f NP##1#r# \n\
t65 VP##1#l# VP##2#l#f VP##2#r#f NP##3#l# N##4#l#h N##4#r#h NP##3#r# VP##1#r# \n\
t48 NP##1#l# NP##2#l#f NP##2#r#f N##3#l#h N##3#r#h NP##1#r# \n\
t26 S##1#l# S##2#l#f S##2#r#f .##3#l#h .##3#r#h S##1#r# 
    """;
}

