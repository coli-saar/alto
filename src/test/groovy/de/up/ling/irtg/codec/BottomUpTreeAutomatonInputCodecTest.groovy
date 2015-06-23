/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec


import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class BottomUpTreeAutomatonInputCodecTest {
    InputCodec codec = new BottomUpTreeAutomatonInputCodec();
    
    @Test
    public void testNoWeights() {
        TreeAutomaton auto = codec.read(random_fta);
        
        assert auto.accepts(pt("F2_1(T0_0,T0_0)"))
        assert auto.accepts(pt("F2_1(F2_0(T0_0,T0_0),T0_0)"))
    }
    
    @Test
    public void testParseWeights() {
        TreeAutomaton auto = codec.read(tiger);
        
        // right now, this only tests whether we can parse
        // the automaton without throwing an exception
        
    }
    
    @Test
    public void testCorrectWeights() {
        TreeAutomaton auto = codec.read(weighted);
        assert auto.accepts(pt("f(a)"))
        assert 10 == auto.getWeight(pt("f(a)"))
    }
    
    
    private static final String weighted = """\n\\n\
    q0\n\
    f(q1) -> q0 <5>\n\
    a -> q1 <2>
""";
    
    private static final String random_fta = """
    ## TASL
# Final states
q7
# Transitions
'F2_1'(q0,q0) -> q7
'F2_1'(q0,q1) -> q6
'F2_1'(q0,q2) -> q7
'F2_1'(q0,q3) -> q7
'F2_1'(q0,q4) -> q7
'F2_1'(q1,q0) -> q5
'F2_1'(q1,q1) -> q3
'F2_1'(q1,q2) -> q6
'F2_1'(q1,q3) -> q6
'F2_1'(q1,q3) -> q7
'F2_1'(q1,q4) -> q5
'F2_1'(q2,q0) -> q3
'F2_1'(q2,q0) -> q6
'F2_1'(q2,q0) -> q7
'F2_1'(q2,q1) -> q3
'F2_1'(q2,q2) -> q6
'F2_1'(q2,q3) -> q4
'F2_1'(q2,q3) -> q6
'F2_1'(q2,q4) -> q5
'F2_1'(q3,q0) -> q6
'F2_1'(q3,q0) -> q7
'F2_1'(q3,q3) -> q5
'F2_1'(q3,q3) -> q6
'F2_1'(q3,q4) -> q5
'F2_1'(q4,q0) -> q6
'F2_1'(q4,q1) -> q6
'F2_1'(q4,q2) -> q5
'F2_1'(q4,q2) -> q6
'F2_1'(q4,q2) -> q7
'F2_1'(q4,q5) -> q7
'F2_1'(q5,q1) -> q7
'F2_1'(q5,q4) -> q6
'F2_1'(q5,q5) -> q7
'F2_0'(q0,q0) -> q1
'F2_0'(q0,q0) -> q5
'F2_0'(q0,q1) -> q3
'F2_0'(q0,q3) -> q6
'F2_0'(q0,q4) -> q5
'F2_0'(q1,q0) -> q3
'F2_0'(q1,q1) -> q5
'F2_0'(q1,q1) -> q6
'F2_0'(q1,q2) -> q4
'F2_0'(q1,q4) -> q5
'F2_0'(q2,q1) -> q3
'F2_0'(q2,q1) -> q7
'F2_0'(q2,q2) -> q5
'F2_0'(q3,q2) -> q5
'F2_0'(q3,q2) -> q6
'F2_0'(q4,q2) -> q5
'F2_0'(q4,q5) -> q7
'F2_0'(q5,q2) -> q7
'F2_0'(q5,q4) -> q6
'F2_0'(q6,q1) -> q7
'F2_0'(q6,q2) -> q7
'F2_0'(q6,q3) -> q7
'F2_0'(q6,q5) -> q7
'F1_1'(q0) -> q2
'F1_1'(q1) -> q2
'F1_1'(q1) -> q7
'T0_1' -> q2
'T0_1' -> q3
'T0_1' -> q4
'T0_0' -> q0
'T0_0' -> q1
'T0_0' -> q2
'T0_0' -> q3
'T0_0' -> q5
'F1_0'(q0) -> q2
'F1_0'(q1) -> q4
'F1_0'(q1) -> q5
'F1_0'(q2) -> q4
'F1_0'(q5) -> q7
'F1_0'(q6) -> q7

    """;
    
    private static final String tiger = '''\n\
# Final states
q0

# Phrase structure
TOP(q1,q2) -> q0	<35133>
PN-HD-Nom.Sg.Masc(q4,q4) -> q3	<3810>
VZ-HD(q6,q7) -> q5	<3713>
TOP(q8,q2) -> q0	<3659>
NP-AG/2(q10,q11) -> q9	<3170>
NP-SB/Sg1(q13) -> q12	<3036>
NP-SB/Sg2(q14,q15) -> q12	<2905>
PN-SB-Nom.Sg(q17) -> q16	<2893>
NP-SB/Sg2(q18,q19) -> q12	<2829>
PN-HD(q21) -> q20	<2488>
VP-OC/pp/1(q23) -> q22	<2176>
NP-SB/Pl/1(q25) -> q24	<2160>
TOP1(q1) -> q0	<1918>
S-TOP(q12,q26,q22) -> q1	<1829>
PP-MNR/N(q28,q29) -> q27	<1805>
VP-OC/pp/2(q30,q23) -> q22	<1804>
TOP1(q31) -> q0	<1720>
NP-EP(q33) -> q32	<1587>
PP-MO/V(q28,q29) -> q30	<1587>
VP-OC/inf/2(q35,q7) -> q34	<1563>
NP-AG/2(q36,q37) -> q9	<1541>
NP-SB/Pl/2(q38,q39) -> q24	<1487>
NP-OA(q40,q41) -> q35	<1430>
NP-SB/Sg2(q42,q43) -> q12	<1367>
PP-MO/V(q44,q45) -> q30	<1360>
NP-AG/2(q46,q47) -> q9	<1292>
PN-TOP-Nom.Sg.Neut(q49) -> q48	<1277>
CNP-HD(q51,q52,q51) -> q50	<1237>
VP-OC/inf/2(q22,q53) -> q34	<1223>
NP-SB/Sg1(q54) -> q12	<1207>
NP-OA(q55,q56) -> q35	<1193>
VP-OC/inf/1(q7) -> q34	<1173>
NP-SB/Sg/rel1(q58) -> q57	<1154>
NP-SB/Sg1(q59) -> q12	<1122>
NP-AG/2(q60,q61) -> q9	<1064>
NP-SB/Sg1(q62) -> q12	<1045>
NP-SB/Sg/rel1(q63) -> q57	<994>
CS-TOP(q1,q52,q64) -> q8	<942>
PN-AG-Gen.Sg.Neut(q66) -> q65	<938>
NP-AG/3(q10,q67,q11) -> q9	<934>
PN-HD-Dat.Sg.Masc(q4,q69) -> q68	<918>
NP-SB/Sg3(q14,q15,q9) -> q12	<882>
VP-OC/pp/2(q35,q23) -> q22	<867>
VP-OC/inf/2(q30,q7) -> q34	<866>
NP-SB/Sg1(q70) -> q12	<860>
NP-SB/Pl/1(q39) -> q24	<846>
NM-HD(q72,q73) -> q71	<842>
PN-SB-Nom.Sg.Neut(q49) -> q74	<841>
NM-HD(q72,q72) -> q71	<834>
NP-OA(q75,q76) -> q35	<823>
PN-AG-Gen.Sg(q78) -> q77	<814>
NP-SB/Sg3(q14,q79,q15) -> q12	<804>
CS-TOP(q1,q80,q1) -> q8	<775>
NP-TOP(q81,q19) -> q31	<763>
PP-MO/V(q82,q45) -> q30	<759>
AP-HD(q84,q85) -> q83	<758>
VP-OC/pp/2(q22,q86) -> q22	<757>
AVP-MO/V(q88,q89) -> q87	<755>
<S-TOP[VVFIN-HD-Sg]VVFIN-HD-Sg|NP-SB/Sg>(q91,q92) -> q90	<754>\n\


# Lexicon
"!" -> q32356
#  "">" -> q32358  ### this does not work yet
# """ -> q32357    ### this does not work yet
"%" -> q32359
"&" -> q32360
"&bullet;" -> q32361
"&tel;" -> q32362
"'" -> q32363
"''" -> q32364
"'89" -> q32365
"'92" -> q32366
"'94" -> q32367
"'96" -> q32368
"'98" -> q32369
"'m" -> q32370
"'n" -> q32371
"'ne" -> q32372
"'s" -> q32373
"*" -> q32374
"(" -> q32375
"(???)" -> q32376
"(Kultur-)Esel" -> q32377

"amtliche" -> q94841
"amtlichen" -> q94842
"amtlicher" -> q94843
"amtlicherseits" -> q94844
"amtliches" -> q94845
"amtsangemessen" -> q94846
"amtsenthoben" -> q94847
"amtsmüde" -> q94848
"amüsant" -> q94849
"amüsieren" -> q94850
"amüsiert" -> q94851
"an" -> q94852
"an-" -> q94853
"anachronistisch" -> q94854
"anachronistische" -> q94855
"anachronistischen" -> q94856
    ''';
}

