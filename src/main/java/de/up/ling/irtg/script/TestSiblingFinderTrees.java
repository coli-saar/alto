/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.google.common.collect.Iterables;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.NondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.coarse_to_fine.CoarseToFineParser;
import de.up.ling.irtg.automata.condensed.CondensedNondeletingInverseHomAutomaton;
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton;
import de.up.ling.irtg.automata.pruning.NoPruningPolicy;
import de.up.ling.irtg.codec.BinaryIrtgInputCodec;
import de.up.ling.irtg.codec.BinaryIrtgOutputCodec;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jonas
 */
public class TestSiblingFinderTrees {
    
    //turn this into tests
    
    //find minimal 
    
    public static void main(String[] args) throws IOException, ParserException, FileNotFoundException, ParseException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromPath("../../experimentData/grammar_37.irtg");
        //Object input = irtg.parseString("string", "Vinken is chairman .");
        Object input2 = irtg.parseString("string", "There no asbestos now . ''");
        //InterpretedTreeAutomaton irtg2 = InterpretedTreeAutomaton.fromString(ASBESTOS_REDUCED_IRTG_SMALL);
        InterpretedTreeAutomaton irtg3 = new BinaryIrtgInputCodec().read(new FileInputStream("examples/tests/asbestosTAG.irtb"));
        
        //testLanguageSize(irtg, input);
        testViterbi(irtg3, input2, 0.00001);
//        try {
//            makeCondensedInvhomExplicit(irtg2, input);
//        } catch (Throwable ex) {
//            System.err.println(Util.getStackTrace(ex));
//        }
        
//        testRuleSetSizes(new TreeAutomatonInputCodec().read(SF_INTERSECT_EX));
//        CoarseToFineParser.testSFParserVariations();
    }    
    
    private static void testLanguageSize(InterpretedTreeAutomaton irtg, Object input) throws IOException, ParseException, ParserException {
        irtg = irtg.filterForAppearingConstants("string", input);
        System.err.println("starting sf parsing..");
        TreeAutomaton sf = irtg.parseWithSiblingFinder("string", input);
        System.err.println(sf.countTrees());
        System.err.println("starting simple parsing..");
        Map<String, Object> map = new HashMap<>();
        map.put("string", input);
        Interpretation interp = irtg.getInterpretation("string");
        TreeAutomaton decomp = interp.getAlgebra().decompose(input);
        TreeAutomaton invhom = new NondeletingInverseHomAutomaton(decomp, interp.getHomomorphism());
        TreeAutomaton def = new IntersectionAutomaton(irtg.getAutomaton(), invhom);
        def.makeAllRulesExplicit();
        System.err.println(def.countTrees());
        System.err.println("starting condensed parsing..");
        TreeAutomaton cond = irtg.parseCondensedWithPruning(map, new NoPruningPolicy());
        CondensedTreeAutomaton condInvhom = new CondensedNondeletingInverseHomAutomaton(decomp, interp.getHomomorphism());
        TreeAutomaton cond2 = irtg.getAutomaton().intersectCondensed(condInvhom);
        cond2.makeAllRulesExplicit();
        System.err.println(cond.countTrees());
        System.err.println(cond2.countTrees());
        
        
        
        double th = 0;
        String ftc = StringTools.slurp(new FileReader("../../experimentData/chen_ctf.txt"));
        CoarseToFineParser ctfp = CoarseToFineParser.makeCoarseToFineParser(irtg, "string", ftc, th);
                                                                          
        System.err.println("parsing with chen CTF: ");             
        TreeAutomaton sfCtfp = ctfp.parseInputObjectWithSF(input);
        TreeAutomaton basicCtfp = ctfp.parseInputObject(input);
        System.err.println(sfCtfp.countTrees());
        System.err.println(basicCtfp.countTrees());
        
        System.err.println("iteration over sf language");
        int condC = 0;
        int cond2C = 0;
        int invhomC = 0;
        int grammarC = 0;
        //ConcreteTreeAutomaton concInvhom = invhom.asConcreteTreeAutomaton();//TODO make a test out of this
        for (Tree<String> tree : (Set<Tree>)sf.language()) {
            if (cond.accepts(tree)) {
                condC++;
            }
            if (cond2.accepts(tree)) {
                cond2C++;
            }
            System.err.println(interp.getAlgebra().evaluate(tree));
            
            if (interp.getAlgebra().evaluate(tree).equals(input)) {
                invhomC++;
            }
            if (irtg.getAutomaton().accepts(tree)) {
                grammarC++;
            }
//            if (condInvhom.accepts(tree)) {
//                condInvhomC++;
//            }
        }
        System.err.println("cond: "+condC);
        System.err.println("cond2: "+cond2C);
        System.err.println("invhom: "+invhomC);
        System.err.println("grammar: "+grammarC);
        
        System.err.println("comparing cond and ctfp");
        int c = 0;
        for (Tree<String> tree : (Set<Tree>)basicCtfp.language()) {
            if (cond.accepts(tree)) {
                c++;
            }
        }
        System.err.println("cond: "+c);
        
        c = 0;
        for (Tree<String> tree : (Set<Tree>)cond.language()) {
            if (basicCtfp.accepts(tree)) {
                c++;
            }
        }
        System.err.println("basicCtfp: "+c);
        
    }
    
    private static void testViterbi(InterpretedTreeAutomaton irtg, Object input, double threshold) throws ParserException, IOException, ParseException {
//        irtg = irtg.filterForAppearingConstants("string", input);
        TreeAutomaton sf = irtg.parseWithSiblingFinder("string", input);//.reduceTopDown();
//        
//        System.err.println(Iterables.size(irtg.getAutomaton().getRuleSet()));
//        
//        Set<String> sfRuleLabels = new HashSet<>();
//        sf.getRuleSet().forEach(rule -> sfRuleLabels.add(((Rule)rule).getLabel(sf)));
//        ConcreteTreeAutomaton<String> newG = new ConcreteTreeAutomaton<>(irtg.getAutomaton().getSignature(), irtg.getAutomaton().getStateInterner());
//        for (Rule rule : irtg.getAutomaton().getRuleSet()) {
//            if (sfRuleLabels.contains(rule.getLabel(irtg.getAutomaton()))) {
//                newG.addRule(rule);
//            }
//        }
//        irtg.getAutomaton().getFinalStates().stream().forEach(state -> newG.addFinalState(state));
//        InterpretedTreeAutomaton newIRTG = new InterpretedTreeAutomaton(newG);
//        newIRTG.addInterpretation("string", irtg.getInterpretation("string"));
//        //System.err.println(newIRTG);
//        //irtg = newIRTG;
//        
//        new BinaryIrtgOutputCodec().write(newIRTG, new FileOutputStream("examples/tests/asbestosTAG.irtb"));
//        irtg = new BinaryIrtgInputCodec().read(new FileInputStream("examples/tests/asbestosTAG.irtb"));
//        
//        System.err.println(Iterables.size(irtg.getAutomaton().getRuleSet()));
        
        
        Map<String, Object> map = new HashMap<>();
        map.put("string", input);
        Interpretation interp = irtg.getInterpretation("string");
        TreeAutomaton decomp = interp.getAlgebra().decompose(input);
        TreeAutomaton invhom = new NondeletingInverseHomAutomaton(decomp, interp.getHomomorphism());
        TreeAutomaton def = new IntersectionAutomaton(irtg.getAutomaton(), invhom);
        def.makeAllRulesExplicit();
        TreeAutomaton cond = irtg.parseCondensedWithPruning(map, new NoPruningPolicy());
        
        //System.err.println(sf.countTrees());
        System.err.println("siblf viterbi: "+sf.viterbi());
        //System.err.println(def.viterbi());
        System.err.println("conds viterbi: "+cond.viterbi());
        
        
        
        
        
        String ftc = StringTools.slurp(new FileReader("../../experimentData/chen_ctf.txt"));
        CoarseToFineParser ctfp = CoarseToFineParser.makeCoarseToFineParser(irtg, "string", ftc, threshold);
                                                                    
        TreeAutomaton sfCtfp = ctfp.parseInputObjectWithSF(input);
        TreeAutomaton basicCtfp = ctfp.parseInputObject(input);
        //System.err.println(sfCtfp.countTrees());
        System.err.println("sfctf viterbi: "+sfCtfp.viterbi());
        //System.err.println(basicCtfp.countTrees());
        System.err.println("cdctf viterbi: "+basicCtfp.viterbi());
    }
    
    private static void makeCondensedInvhomExplicit(InterpretedTreeAutomaton irtg, Object input) {
        irtg = irtg.filterForAppearingConstants("string", input);
        Interpretation interp = irtg.getInterpretation("string");
        TreeAutomaton decomp = interp.getAlgebra().decompose(input);
        TreeAutomaton invhom = new CondensedNondeletingInverseHomAutomaton(decomp, interp.getHomomorphism());
        ConcreteTreeAutomaton concInvhom = invhom.asConcreteTreeAutomatonBottomUp();
        invhom.makeAllRulesExplicit();
    }
    
    public static void testRuleSetSizes(TreeAutomaton auto) {
        System.err.println(Iterables.size(auto.getRuleSet()));
        Set processBU = new HashSet();
        auto.processAllRulesBottomUp(rule -> processBU.add(rule));
        System.err.println(processBU.size());
        Set processTD = new HashSet();
        auto.processAllRulesTopDown(rule -> processTD.add(rule));
        System.err.println(processTD.size());
        System.err.println(Iterables.size(auto.getAllRulesTopDown()));
    }
    
    public static final String ASBESTOS_REDUCED_GRAMMAR = "interpretation string: de.up.ling.irtg.algebra.TagStringAlgebra\n" +
"\n" +
"AdvP_A -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"  [string] *EE*\n" +
"\n" +
"Ad_A -> *NOP*_Ad_A_br3 [0.9965694682675815]\n" +
"  [string] *EE*\n" +
"\n" +
"N_A -> *NOP*_N_A_br14 [0.9827648114901257]\n" +
"  [string] *EE*\n" +
"\n" +
"\"''_A\" -> \"*NOP*_''_A_br8\" [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"EX_A -> *NOP*_EX_A_br10 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"S_A -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"  [string] *EE*\n" +
"\n" +
"'._A' -> '*NOP*_._A_br28' [0.9407244785949506]\n" +
"  [string] *EE*\n" +
"\n" +
"VP_A -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"  [string] *EE*\n" +
"\n" +
"D_A -> *NOP*_D_A_br21 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"NP_A -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"  [string] *EE*\n" +
"\n" +
"S_A -> t90-Virtually_br4373('Ad_A>>AdvP_A', S_A) [0.0017317380352644843]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"VP_A -> t24-further_br3949('Ad_A>>AdvP_A', VP_A) [0.0383258499037845]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t321-there_br3886('Ad_A>>AdvP_A', NP_A) [0.0014795884054072824]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"S_A -> 't26-._br4744'('._A', S_A) [0.2646410579345089]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"AdvP_A -> 't596-._br4740'('._A', AdvP_A) [0.00383141762452107]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"NP_A -> 't484-._br4736'('._A', NP_A) [8.743022395588484E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"'NP_S>>N_A>>NP_A' -> t237-one_br13333(NP_S, 'N_A>>NP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A' -> t615-judge_br5205(NP_S, 'N_A>>NP_A>>VP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A' -> t668-policy_br7041(NP_S, 'NP_A>>N_A>>NP_A>>VP_A>>S_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>NP_S>>VP_A' -> t2768-life_br12762(NP_S, 'N_A>>NP_A>>NP_S>>VP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'N_A>>NP_A' -> t167-asbestos_br13168(N_A, NP_A) [2.0079485678444132E-4]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,asbestos))\n" +
"\n" +
"'N_A>>NP_A>>NP_S' -> t2768-life_br12760('N_A>>NP_A', NP_S) [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'N_A>>NP_A>>VP_A' -> 't1527-%_br563'('N_A>>NP_A', VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"S_A -> \"t669-''_br4614\"(\"''_A\", S_A) [9.445843828715369E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"'._A' -> \"t91-''_br4621\"(\"''_A\", '._A') [0.05817782656421515]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"NP_A -> \"t6-''_br4610\"(\"''_A\", NP_A) [0.0010760642948416596]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A', S_A) [0.8125253289506856]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"S_S! -> t237-one_br13334('NP_S>>N_A>>NP_A', S_A) [0.0028099754127151443]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"NP_S -> t327-There_br17463(EX_A, NP_A) [0.0021183448665442104]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,There))\n" +
"\n" +
"'N_A>>NP_A>>NP_S>>VP_A' -> t2768-life_br12761('N_A>>NP_A>>NP_S', VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t69-now_br4004(Ad_A, AdvP_A) [0.008458566288778491]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,*WRAP21*(?1,now)))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t321-now_br3887(Ad_A, AdvP_A) [0.0011824964551936396]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,*WRAP21*(?1,now)))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t24-no_br3952(Ad_A, AdvP_A) [0.002819522096259497]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,no)),*EE*)\n" +
"\n" +
"NP_S -> t106-now_br3744(Ad_A, NP_A) [2.824459822058947E-4]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,now))\n" +
"\n" +
"NP_A -> t105-no_br3725(Ad_A, NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"S_S! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A', S_A) [0.0010537407797681791]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"S_S! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0537407797681772]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>VP_A' -> 't1527-%_br564'(NP_A, 'N_A>>NP_A>>VP_A') [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"NP_A -> t1-no_br25538(D_A, NP_A) [5.380321474208298E-4]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"S_S! -> t2768-life_br12763('NP_S>>N_A>>NP_A>>NP_S>>VP_A', S_A) [3.5124692658939304E-4]\n" +
"  [string] *WRAP21*(?2,?1)\n";
    
    public final static String ASBESTOS_REDUCED_IRTG_SMALL = "interpretation string: de.up.ling.irtg.algebra.TagStringAlgebra\n" +
"\n" +
"V_A -> *NOP*_V_A_br32 [0.9753623188405797]\n" +
"  [string] *EE*\n" +
"\n" +
"Comp_A -> *NOP*_Comp_A_br9 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"N_A -> *NOP*_N_A_br14 [0.9827648114901257]\n" +
"  [string] *EE*\n" +
"\n" +
"TO_A -> *NOP*_TO_A_br35 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"PP_A -> *NOP*_PP_A_br37 [0.9632727272727275]\n" +
"  [string] *EE*\n" +
"\n" +
"Punct_A -> *NOP*_Punct_A_br1 [0.8458015267175573]\n" +
"  [string] *EE*\n" +
"\n" +
"RRC_A -> *NOP*_RRC_A_br5 [0.6666666666666667]\n" +
"  [string] *EE*\n" +
"\n" +
"FW_A -> *NOP*_FW_A_br7 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"\"''_A\" -> \"*NOP*_''_A_br8\" [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"SYM_A -> *NOP*_SYM_A_br27 [0.0]\n" +
"  [string] *EE*\n" +
"\n" +
"'WP$_A' -> '*NOP*_WP$_A_br17' [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"'._A' -> '*NOP*_._A_br28' [0.9407244785949506]\n" +
"  [string] *EE*\n" +
"\n" +
"RP_A -> *NOP*_RP_A_br38 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"LST_A -> *NOP*_LST_A_br11 [0.0]\n" +
"  [string] *EE*\n" +
"\n" +
"IN_A -> *NOP*_IN_A_br18 [0.998960498960499]\n" +
"  [string] *EE*\n" +
"\n" +
"G_A -> *NOP*_G_A_br19 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"D_A -> *NOP*_D_A_br21 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"A_A -> *NOP*_A_A_br4 [0.9779816513761468]\n" +
"  [string] *EE*\n" +
"\n" +
"NP_A -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"  [string] *EE*\n" +
"\n" +
"INTJ_A -> *NOP*_INTJ_A_br33 [0.0]\n" +
"  [string] *EE*\n" +
"\n" +
"UCP_A -> *NOP*_UCP_A_br20 [0.5000000000000001]\n" +
"  [string] *EE*\n" +
"\n" +
"AdvP_A -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"  [string] *EE*\n" +
"\n" +
"POS_A -> *NOP*_POS_A_br30 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"Ad_A -> *NOP*_Ad_A_br3 [0.9965694682675815]\n" +
"  [string] *EE*\n" +
"\n" +
"CONJP_A -> *NOP*_CONJP_A_br23 [0.5]\n" +
"  [string] *EE*\n" +
"\n" +
"'-RRB-_A' -> *NOP*_-RRB-_A_br15 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"CC_A -> *NOP*_CC_A_br16 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"QP_A -> *NOP*_QP_A_br22 [0.5030120481927707]\n" +
"  [string] *EE*\n" +
"\n" +
"EX_A -> *NOP*_EX_A_br10 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"PRN_A -> *NOP*_PRN_A_br13 [0.5000000000000001]\n" +
"  [string] *EE*\n" +
"\n" +
"'``_A' -> '*NOP*_``_A_br29' [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"WP_A -> *NOP*_WP_A_br31 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"S_A -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"  [string] *EE*\n" +
"\n" +
"X_A -> *NOP*_X_A_br26 [0.6666666666666666]\n" +
"  [string] *EE*\n" +
"\n" +
"PRT_A -> *NOP*_PRT_A_br24 [0.9863013698630136]\n" +
"  [string] *EE*\n" +
"\n" +
"MD_A -> *NOP*_MD_A_br25 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"AP_A -> *NOP*_AP_A_br6 [0.607068607068608]\n" +
"  [string] *EE*\n" +
"\n" +
"VP_A -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"  [string] *EE*\n" +
"\n" +
"'-LRB-_A' -> *NOP*_-LRB-_A_br36 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"'#_A' -> '*NOP*_#_A_br42' [0.0]\n" +
"  [string] *EE*\n" +
"\n" +
"NPP_A -> *NOP*_NPP_A_br40 [0.5000000000000003]\n" +
"  [string] *EE*\n" +
"\n" +
"'$_A' -> '*NOP*_$_A_br39' [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"FRAG_A -> *NOP*_FRAG_A_br41 [0.5333333333333333]\n" +
"  [string] *EE*\n" +
"\n" +
"PDT_A -> *NOP*_PDT_A_br43 [1.0]\n" +
"  [string] *EE*\n" +
"\n" +
"'NP_S>>NP_S' -> t1061-give_br575(NP_S, NP_S) [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>N_A>>NP_A' -> t237-one_br13333(NP_S, 'N_A>>NP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>S_S' -> t460-persuade_br1940(NP_S, S_S) [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A' -> t615-judge_br5205(NP_S, 'N_A>>NP_A>>VP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A' -> t668-policy_br7041(NP_S, 'NP_A>>N_A>>NP_A>>VP_A>>S_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>NP_S>>VP_A' -> t2768-life_br12762(NP_S, 'N_A>>NP_A>>NP_S>>VP_A') [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'NP_S>>VP_A' -> t1488-does_br16095(NP_S, VP_A) [0.0]\n" +
"  [string] *CONC11*(?1,*WRAP21*(?2,*E*))\n" +
"\n" +
"'NP_S>>NP_A' -> t595-offered_br52(NP_S, NP_A) [1.0]\n" +
"  [string] *CONC11*(?1,*WRAP21*(?2,*E*))\n" +
"\n" +
"S_A -> \"t669-''_br4614\"(\"''_A\", S_A) [9.445843828715369E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"A_A -> \"t176-''_br4616\"(\"''_A\", A_A) [0.003669724770642202]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"'._A' -> \"t91-''_br4621\"(\"''_A\", '._A') [0.05817782656421515]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"N_A -> \"t122-''_br4618\"(\"''_A\", N_A) [0.0015559545182525435]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"PP_A -> \"t250-''_br4608\"(\"''_A\", PP_A) [0.0010909090909090912]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"V_A -> \"t503-''_br4613\"(\"''_A\", V_A) [0.0014492753623188406]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"PRT_A -> \"t1010-''_br4620\"(\"''_A\", PRT_A) [0.0136986301369863]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"Punct_A -> \"t15-''_br4617\"(\"''_A\", Punct_A) [0.15419847328244274]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"AP_A -> \"t162-''_br4615\"(\"''_A\", AP_A) [0.002079002079002082]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"IN_A -> \"t2128-''_br4623\"(\"''_A\", IN_A) [3.465003465003465E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"QP_A -> \"t4267-''_br4609\"(\"''_A\", QP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"NP_A -> \"t1889-''_br4612\"(\"''_A\", NP_A) [6.725401842760373E-5]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,\"''\"),*EE*))\n" +
"\n" +
"NP_A -> \"t6-''_br4610\"(\"''_A\", NP_A) [0.0010760642948416596]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,\"''\")))\n" +
"\n" +
"S_S! -> t237-one_br13334('NP_S>>N_A>>NP_A', S_A) [0.0028099754127151443]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"VP_A -> t1045-regulators_br3412('NP_A>>N_A>>NP_A>>VP_A>>S_A', VP_A) [3.2071840923669065E-4]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_S -> t327-There_br17463(EX_A, NP_A) [0.0021183448665442104]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,There))\n" +
"\n" +
"'N_A>>NP_A>>NP_S>>VP_A' -> t2768-life_br12761('N_A>>NP_A>>NP_S', VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"S_A -> t833-customer_br3460('NP_A>>N_A>>NP_A>>S_A', S_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"'NP_A>>NP_A>>N_A>>NP_A>>S_A>>S_A' -> t2788-home_br573('NP_A>>NP_A>>N_A>>NP_A>>S_A', S_A) [1.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"'NP_A>>NP_A>>N_A>>NP_A>>VP_A>>S_A>>S_A' -> 't1527-%_br567'('NP_A>>NP_A>>N_A>>NP_A>>VP_A>>S_A', S_A) [1.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"S_S! -> t2768-life_br12763('NP_S>>N_A>>NP_A>>NP_S>>VP_A', S_A) [3.5124692658939304E-4]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_A>>NP_S' -> t590-fined_br9331(NP_A, NP_S) [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>N_A>>NP_A' -> t2788-home_br570(NP_A, 'N_A>>NP_A') [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>S_S' -> t1110-Asked_br2305(NP_A, S_S) [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>VP_A' -> 't1527-%_br564'(NP_A, 'N_A>>NP_A>>VP_A') [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>NP_A>>N_A>>NP_A>>VP_A>>S_A' -> 't1527-%_br566'(NP_A, 'NP_A>>N_A>>NP_A>>VP_A>>S_A') [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>NP_A>>N_A>>NP_A>>S_A' -> t2788-home_br572(NP_A, 'NP_A>>N_A>>NP_A>>S_A') [1.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),?2)\n" +
"\n" +
"'NP_A>>VP_A' -> t4033-do_br5252(NP_A, VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,*E*))\n" +
"\n" +
"'NP_A>>NP_A' -> t2523-charged_br76(NP_A, NP_A) [0.0]\n" +
"  [string] *CONC11*(*WRAP21*(?1,*E*),*WRAP21*(?2,*E*))\n" +
"\n" +
"AdvP_A -> t1353-no_br25570(D_A, AdvP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"NP_A -> t1-no_br25538(D_A, NP_A) [5.380321474208298E-4]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"NP_A -> t2-asbestos_br22824(N_A, NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,asbestos),*EE*))\n" +
"\n" +
"'N_A>>NP_A' -> t167-asbestos_br13168(N_A, NP_A) [2.0079485678444132E-4]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,asbestos))\n" +
"\n" +
"NP_S -> t3-asbestos_br22364(N_A, NP_A) [4.2366897330884203E-4]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,asbestos))\n" +
"\n" +
"S_A -> 't26-._br4744'('._A', S_A) [0.2646410579345089]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"LST_A -> 't3157-._br4747'('._A', LST_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"PP_A -> 't584-._br4735'('._A', PP_A) [3.6363636363636367E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"AdvP_A -> 't596-._br4740'('._A', AdvP_A) [0.00383141762452107]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"FRAG_A -> 't210-._br4746'('._A', FRAG_A) [0.3333333333333333]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"NP_A -> 't484-._br4736'('._A', NP_A) [8.743022395588484E-4]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,'.')))\n" +
"\n" +
"NP_A -> 't713-._br4737'('._A', NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,'.'),*EE*))\n" +
"\n" +
"VP_A -> t441-reinvestment_br24903('N_A>>NP_A>>UCP_A', VP_A) [1.6035920461834533E-4]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t2788-home_br574('NP_A>>NP_A>>N_A>>NP_A>>S_A>>S_A', NP_A) [6.725401842760373E-5]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"S_A -> t90-Virtually_br4373('Ad_A>>AdvP_A', S_A) [0.0017317380352644843]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"PP_A -> t217-probably_br3838('Ad_A>>AdvP_A', PP_A) [0.005454545454545456]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"V_A -> t357-globally_br4329('Ad_A>>AdvP_A', V_A) [0.004830917874396135]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"PRN_A -> t435-initially_br4492('Ad_A>>AdvP_A', PRN_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"VP_A -> t24-further_br3949('Ad_A>>AdvP_A', VP_A) [0.0383258499037845]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"AP_A -> t820-always_br4483('Ad_A>>AdvP_A', AP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t321-there_br3886('Ad_A>>AdvP_A', NP_A) [0.0014795884054072824]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t286-drain_br7061('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A>>S_A', NP_A) [2.690160737104149E-4]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t505-bid_br24774('N_A>>NP_A>>VP_A', NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"S_S! -> t563-year_br24848('N_A>>NP_A', S_A) [0.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'N_A>>NP_A>>S_A' -> t1235-husband_br16120('N_A>>NP_A', S_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'N_A>>NP_A>>NP_S' -> t2768-life_br12760('N_A>>NP_A', NP_S) [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'N_A>>NP_A>>AdvP_S' -> t3199-payoff_br24042('N_A>>NP_A', AdvP_S) [1.0]\n" +
"  [string] *CONC11*(?1,?2)\n" +
"\n" +
"'N_A>>NP_A>>VP_A' -> 't1527-%_br563'('N_A>>NP_A', VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"VP_S -> t566-student_br24683('N_A>>NP_A', VP_A) [0.015151515151515159]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'N_A>>NP_A>>VP_A' -> t505-bid_br24773('N_A>>NP_A', VP_A) [0.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"FRAG_S -> t207-reason_br25328('N_A>>NP_A', FRAG_A) [0.8571428571428572]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'N_A>>NP_A>>UCP_A' -> t441-reinvestment_br24902('N_A>>NP_A', UCP_A) [1.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"S_S! -> t3199-payoff_br24043('N_A>>NP_A>>AdvP_S', S_A) [0.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'N_A>>NP_A>>AdvP_S>>S_A' -> t1595-foot_br24046('N_A>>NP_A>>AdvP_S', S_A) [1.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"NP_A -> 't1527-%_br568'('NP_A>>NP_A>>N_A>>NP_A>>VP_A>>S_A>>S_A', NP_A) [6.725401842760373E-5]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A', S_A) [0.8125253289506856]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A' -> t1045-regulators_br3411('NP_A>>N_A>>NP_A>>VP_A', S_A) [0.18747467104931442]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"S_S! -> t465-role_br3387('NP_A>>N_A>>NP_A>>VP_A', S_A) [0.0021074815595363582]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>S_A' -> t833-customer_br3459('NP_A>>N_A>>NP_A', S_A) [0.0]\n" +
"  [string] *CONC12*(*WRAP21*(?2,?1),*EE*)\n" +
"\n" +
"'NP_A>>N_A>>NP_A>>S_A' -> t2788-home_br571('NP_A>>N_A>>NP_A', S_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"S_S! -> t188-person_br3420('NP_A>>N_A>>NP_A', S_A) [0.005971197752019682]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'AdvP_A>>VP_A' -> t3373-would_br5225(AdvP_A, VP_A) [1.0]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,*E*))\n" +
"\n" +
"S_A -> t1052-people_br13321('NP_S>>N_A>>NP_A>>VP_A>>S_A', S_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"PRN_A -> t581-1979_br13331('NP_S>>N_A>>NP_A>>VP_A>>S_A', PRN_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"NP_A -> t1322-threat_br13316('NP_S>>N_A>>NP_A>>VP_A>>S_A', NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"VP_A -> t1595-foot_br24047('N_A>>NP_A>>AdvP_S>>S_A', VP_A) [1.6035920461834533E-4]\n" +
"  [string] *WRAP22*(?2,?1)\n" +
"\n" +
"INTJ_A -> t4085-now_br3665(Ad_A, INTJ_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC21*(*EE*,*WRAP21*(?1,now)))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t820-now_br4486(Ad_A, AdvP_A) [0.0]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,now)),*EE*)\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t90-now_br4420(Ad_A, AdvP_A) [0.002768032083166597]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,now)),*EE*)\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t69-now_br4004(Ad_A, AdvP_A) [0.008458566288778491]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,*WRAP21*(?1,now)))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t321-now_br3887(Ad_A, AdvP_A) [0.0011824964551936396]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,*WRAP21*(?1,now)))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t24-no_br3952(Ad_A, AdvP_A) [0.002819522096259497]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,no)),*EE*)\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t518-now_br3931(Ad_A, AdvP_A) [0.0]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,now)),*EE*)\n" +
"\n" +
"AdvP_S -> t72-now_br3835(Ad_A, AdvP_A) [0.0]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,now))\n" +
"\n" +
"AdvP_A -> t71-no_br3807(Ad_A, AdvP_A) [0.001915708812260535]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"'Ad_A>>AdvP_A' -> t24-now_br4034(Ad_A, AdvP_A) [0.011278088385037988]\n" +
"  [string] *CONC12*(*WRAP21*(?2,*WRAP21*(?1,now)),*EE*)\n" +
"\n" +
"NP_S -> t106-now_br3744(Ad_A, NP_A) [2.824459822058947E-4]\n" +
"  [string] *WRAP21*(?2,*WRAP21*(?1,now))\n" +
"\n" +
"NP_A -> t105-no_br3725(Ad_A, NP_A) [0.0]\n" +
"  [string] *WRAP22*(?2,*CONC12*(*WRAP21*(?1,no),*EE*))\n" +
"\n" +
"S_S! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A', S_A) [0.0010537407797681791]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A>>S_A' -> t286-drain_br7060('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A', S_A) [1.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A>>S_A' -> t581-1979_br13330('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A>>S_A' -> t1322-threat_br13315('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A>>S_A' -> t1052-people_br13320('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0]\n" +
"  [string] *CONC21*(*EE*,*WRAP21*(?2,?1))\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A>>S_A' -> t1891-Britain_br13325('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0]\n" +
"  [string] *CONC12*(*WRAP21*(?2,?1),*EE*)\n" +
"\n" +
"S_S! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A', S_A) [0.0537407797681772]\n" +
"  [string] *WRAP21*(?2,?1)\n" +
"\n" +
"'NP_S>>N_A>>NP_A>>VP_A>>S_A' -> t615-judge_br5206('NP_S>>N_A>>NP_A>>VP_A', S_A) [1.0]\n" +
"  [string] *WRAP21*(?2,?1)";
    
    private final static String SF_INTERSECT_EX = "'AdvP_A,3-3,4-4' -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"'AdvP_A,3-3,6-6' -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"'AdvP_A,1-1,2-2' -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"'AdvP_A,3-3,5-5' -> *NOP*_AdvP_A_br2 [0.8141762452107274]\n" +
"'Ad_A,3-3,4-4' -> *NOP*_Ad_A_br3 [0.9965694682675815]\n" +
"'Ad_A,1-1,2-2' -> *NOP*_Ad_A_br3 [0.9965694682675815]\n" +
"'N_A,2-2,3-3' -> *NOP*_N_A_br14 [0.9827648114901257]\n" +
"\"''_A,5-5,6-6\" -> \"*NOP*_''_A_br8\" [1.0]\n" +
"'EX_A,0-0,1-1' -> *NOP*_EX_A_br10 [1.0]\n" +
"'S_A,1-1,6-6' -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"'S_A,1-1,5-5' -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"'S_A,1-1,3-3' -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"'S_A,0-0,6-6' -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"'S_A,1-1,4-4' -> *NOP*_S_A_br0 [0.6915931989924436]\n" +
"'._A,4-4,5-5' -> '*NOP*_._A_br28' [0.9407244785949506]\n" +
"'._A,4-4,6-6' -> '*NOP*_._A_br28' [0.9407244785949506]\n" +
"'VP_A,1-1,5-5' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,1-1,3-3' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,2-2,5-5' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,2-2,6-6' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,2-2,3-3' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,1-1,4-4' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,2-2,4-4' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'VP_A,1-1,6-6' -> *NOP*_VP_A_br12 [0.6053559974342536]\n" +
"'D_A,1-1,2-2' -> *NOP*_D_A_br21 [1.0]\n" +
"'NP_A,1-1,3-3' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,1-1,1-1' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,2-2,3-3' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,0-0,1-1' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,1-1,6-6' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,1-1,5-5' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,2-2,4-4' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,3-3,6-6' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,3-3,4-4' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,2-2,2-2' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,1-1,2-2' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,3-3,5-5' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,2-2,6-6' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,1-1,4-4' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'NP_A,2-2,5-5' -> *NOP*_NP_A_br34 [0.6064294841617027]\n" +
"'S_A,1-2,4-6' -> 't26-._br4744'('._A,4-4,5-6', 'S_A,1-2,6-6') [0.2646410579345089]\n" +
"'NP_A,2-2,4-6' -> 't484-._br4736'('._A,4-4,5-6', 'NP_A,2-2,6-6') [8.743022395588484E-4]\n" +
"'NP_A,3-3,4-6' -> 't484-._br4736'('._A,4-4,5-6', 'NP_A,3-3,6-6') [8.743022395588484E-4]\n" +
"'S_A,0-0,4-6' -> 't26-._br4744'('._A,4-4,5-6', 'S_A,0-0,6-6') [0.2646410579345089]\n" +
"'NP_A,1-1,4-6' -> 't484-._br4736'('._A,4-4,5-6', 'NP_A,1-1,6-6') [8.743022395588484E-4]\n" +
"'S_A,1-1,4-6' -> 't26-._br4744'('._A,4-4,5-6', 'S_A,1-1,6-6') [0.2646410579345089]\n" +
"'AdvP_A,3-3,4-6' -> 't596-._br4740'('._A,4-4,5-6', 'AdvP_A,3-3,6-6') [0.00383141762452107]\n" +
"'NP_A,1-2,4-6' -> 't484-._br4736'('._A,4-4,5-6', 'NP_A,1-2,6-6') [8.743022395588484E-4]\n" +
"'NP_S,0-1,null' -> t327-There_br17463('EX_A,0-0,1-1', 'NP_A,0-0,1-1') [0.0021183448665442104]\n" +
"'S_A,0-0,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,0-0,3-5', 'S_A,0-0,5-6') [0.0017317380352644843]\n" +
"'S_S,0-6,null'! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-4,null', 'S_A,0-0,4-6') [0.0010537407797681791]\n" +
"'N_A>>NP_A>>VP_A,2-5,null' -> 't1527-%_br563'('N_A>>NP_A,2-5,null', 'VP_A,2-2,5-5') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br563'('N_A>>NP_A,2-5,null', 'VP_A,1-2,5-5') [1.0]\n" +
"'S_A,0-0,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,0-0,3-6', 'S_A,0-0,6-6') [0.0017317380352644843]\n" +
"'S_S,0-6,null'! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-6,null', 'S_A,0-0,6-6') [0.0010537407797681791]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-5,null', 'S_A,1-2,5-5') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-5,null', 'S_A,1-2,5-6') [0.8125253289506856]\n" +
"'N_A>>NP_A>>NP_S,2-4,null' -> t2768-life_br12760('N_A>>NP_A,2-3,null', 'NP_S,3-4,null') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,1-2,3-5') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-3,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,1-2,3-3') [1.0]\n" +
"'N_A>>NP_A>>VP_A,2-6,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,2-2,3-6') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,1-2,3-4') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,1-2,3-6') [1.0]\n" +
"'N_A>>NP_A>>NP_S,2-6,null' -> t2768-life_br12760('N_A>>NP_A,2-3,null', 'NP_S,3-6,null') [1.0]\n" +
"'N_A>>NP_A>>VP_A,2-3,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,2-2,3-3') [1.0]\n" +
"'N_A>>NP_A>>VP_A,2-4,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,2-2,3-4') [1.0]\n" +
"'N_A>>NP_A>>VP_A,2-5,null' -> 't1527-%_br563'('N_A>>NP_A,2-3,null', 'VP_A,2-2,3-5') [1.0]\n" +
"'N_A>>NP_A>>NP_S,2-5,null' -> t2768-life_br12760('N_A>>NP_A,2-3,null', 'NP_S,3-5,null') [1.0]\n" +
"'Ad_A>>AdvP_A,2-2,3-6' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-6' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,0-0,3-6' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-6' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,0-0,3-6' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,2-2,3-6' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-6') [0.0011824964551936396]\n" +
"'NP_S,3-4,null' -> t106-now_br3744('Ad_A,3-3,4-4', 'NP_A,3-3,4-4') [2.824459822058947E-4]\n" +
"'Ad_A>>AdvP_A,0-0,3-4' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-4' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,2-2,3-4' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-4' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,2-2,3-4' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,0-0,3-4' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-4') [0.0011824964551936396]\n" +
"'NP_S,3-6,null' -> t106-now_br3744('Ad_A,3-3,4-4', 'NP_A,3-3,4-6') [2.824459822058947E-4]\n" +
"'NP_S,3-5,null' -> t106-now_br3744('Ad_A,3-3,4-4', 'NP_A,3-3,4-5') [2.824459822058947E-4]\n" +
"'Ad_A>>AdvP_A,0-0,3-5' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,2-2,3-5' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-5' -> t69-now_br4004('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.008458566288778491]\n" +
"'Ad_A>>AdvP_A,1-1,3-5' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,2-2,3-5' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.0011824964551936396]\n" +
"'Ad_A>>AdvP_A,0-0,3-5' -> t321-now_br3887('Ad_A,3-3,4-4', 'AdvP_A,3-3,4-5') [0.0011824964551936396]\n" +
"'NP_A,1-2,3-6' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,3-6') [5.380321474208298E-4]\n" +
"'NP_A,1-2,4-6' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,4-6') [5.380321474208298E-4]\n" +
"'NP_A,1-2,2-2' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,2-2') [5.380321474208298E-4]\n" +
"'NP_A,1-2,3-4' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,3-4') [5.380321474208298E-4]\n" +
"'NP_A,1-2,3-5' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,3-5') [5.380321474208298E-4]\n" +
"'NP_A,1-2,6-6' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,6-6') [5.380321474208298E-4]\n" +
"'NP_A,1-2,5-6' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,5-6') [5.380321474208298E-4]\n" +
"'NP_A,1-2,5-5' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,5-5') [5.380321474208298E-4]\n" +
"'NP_A,1-2,4-5' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,4-5') [5.380321474208298E-4]\n" +
"'NP_A,1-2,4-4' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,4-4') [5.380321474208298E-4]\n" +
"'NP_A,1-2,3-3' -> t1-no_br25538('D_A,1-1,2-2', 'NP_A,1-1,3-3') [5.380321474208298E-4]\n" +
"'S_S,0-6,null'! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-3,null', 'S_A,0-0,3-6') [0.0010537407797681791]\n" +
"'N_A>>NP_A>>NP_S,1-4,null' -> t2768-life_br12760('N_A>>NP_A,1-3,null', 'NP_S,3-4,null') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br563'('N_A>>NP_A,1-3,null', 'VP_A,1-1,3-4') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br563'('N_A>>NP_A,1-3,null', 'VP_A,1-1,3-6') [1.0]\n" +
"'N_A>>NP_A>>NP_S,1-6,null' -> t2768-life_br12760('N_A>>NP_A,1-3,null', 'NP_S,3-6,null') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br563'('N_A>>NP_A,1-3,null', 'VP_A,1-1,3-5') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-3,null' -> 't1527-%_br563'('N_A>>NP_A,1-3,null', 'VP_A,1-1,3-3') [1.0]\n" +
"'N_A>>NP_A>>NP_S,1-5,null' -> t2768-life_br12760('N_A>>NP_A,1-3,null', 'NP_S,3-5,null') [1.0]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-5,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,1-5,null', 'VP_A,1-1,5-5') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br563'('N_A>>NP_A,1-4,null', 'VP_A,1-1,4-4') [1.0]\n" +
"'S_A,1-2,5-6' -> \"t669-''_br4614\"(\"''_A,5-5,6-6\", 'S_A,1-2,6-6') [9.445843828715369E-4]\n" +
"'NP_A,2-2,5-6' -> \"t6-''_br4610\"(\"''_A,5-5,6-6\", 'NP_A,2-2,6-6') [0.0010760642948416596]\n" +
"'NP_A,3-3,5-6' -> \"t6-''_br4610\"(\"''_A,5-5,6-6\", 'NP_A,3-3,6-6') [0.0010760642948416596]\n" +
"'S_A,0-0,5-6' -> \"t669-''_br4614\"(\"''_A,5-5,6-6\", 'S_A,0-0,6-6') [9.445843828715369E-4]\n" +
"'NP_A,1-1,5-6' -> \"t6-''_br4610\"(\"''_A,5-5,6-6\", 'NP_A,1-1,6-6') [0.0010760642948416596]\n" +
"'._A,4-4,5-6' -> \"t91-''_br4621\"(\"''_A,5-5,6-6\", '._A,4-4,6-6') [0.05817782656421515]\n" +
"'S_A,1-1,5-6' -> \"t669-''_br4614\"(\"''_A,5-5,6-6\", 'S_A,1-1,6-6') [9.445843828715369E-4]\n" +
"'NP_A,1-2,5-6' -> \"t6-''_br4610\"(\"''_A,5-5,6-6\", 'NP_A,1-2,6-6') [0.0010760642948416596]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-5,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,2-5,null', 'VP_A,1-2,5-5') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br563'('N_A>>NP_A,2-6,null', 'VP_A,1-2,6-6') [1.0]\n" +
"'N_A>>NP_A>>VP_A,2-6,null' -> 't1527-%_br563'('N_A>>NP_A,2-6,null', 'VP_A,2-2,6-6') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,2-4,null' -> 't1527-%_br564'('NP_A,2-2,2-2', 'N_A>>NP_A>>VP_A,2-4,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,2-3,null' -> 't1527-%_br564'('NP_A,2-2,2-2', 'N_A>>NP_A>>VP_A,2-3,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,2-6,null' -> 't1527-%_br564'('NP_A,2-2,2-2', 'N_A>>NP_A>>VP_A,2-6,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,2-5,null' -> 't1527-%_br564'('NP_A,2-2,2-2', 'N_A>>NP_A>>VP_A,2-5,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-6,null', 'S_A,1-2,6-6') [0.8125253289506856]\n" +
"'NP_A,1-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,3-3', 'NP_A,1-1,3-6') [0.0014795884054072824]\n" +
"'S_A,1-2,3-4' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,3-3', 'S_A,1-1,3-4') [0.0017317380352644843]\n" +
"'S_A,1-2,3-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,3-3', 'S_A,1-1,3-5') [0.0017317380352644843]\n" +
"'VP_A,1-2,3-4' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,3-3', 'VP_A,1-1,3-4') [0.0383258499037845]\n" +
"'NP_A,1-2,3-4' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,3-3', 'NP_A,1-1,3-4') [0.0014795884054072824]\n" +
"'NP_A,1-2,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,3-3', 'NP_A,1-1,3-5') [0.0014795884054072824]\n" +
"'S_A,1-2,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,3-3', 'S_A,1-1,3-6') [0.0017317380352644843]\n" +
"'VP_A,1-2,3-6' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,3-3', 'VP_A,1-1,3-6') [0.0383258499037845]\n" +
"'VP_A,1-2,3-5' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,3-3', 'VP_A,1-1,3-5') [0.0383258499037845]\n" +
"'S_A,1-2,3-3' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,3-3', 'S_A,1-1,3-3') [0.0017317380352644843]\n" +
"'NP_A,1-2,3-3' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,3-3', 'NP_A,1-1,3-3') [0.0014795884054072824]\n" +
"'VP_A,1-2,3-3' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,3-3', 'VP_A,1-1,3-3') [0.0383258499037845]\n" +
"'S_A,1-1,3-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-5', 'S_A,1-1,5-5') [0.0017317380352644843]\n" +
"'VP_A,1-1,3-5' -> t24-further_br3949('Ad_A>>AdvP_A,1-1,3-5', 'VP_A,1-1,5-5') [0.0383258499037845]\n" +
"'NP_A,1-1,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-5', 'NP_A,1-1,5-6') [0.0014795884054072824]\n" +
"'NP_A,1-1,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-5', 'NP_A,1-1,5-5') [0.0014795884054072824]\n" +
"'S_A,1-1,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-5', 'S_A,1-1,5-6') [0.0017317380352644843]\n" +
"'NP_A,1-2,2-2' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,2-2', 'NP_A,1-1,2-2') [0.0014795884054072824]\n" +
"'S_S,0-6,null'! -> t668-policy_br7042('NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-5,null', 'S_A,0-0,5-6') [0.0010537407797681791]\n" +
"'VP_A,1-2,6-6' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,6-6', 'VP_A,1-1,6-6') [0.0383258499037845]\n" +
"'NP_A,1-2,6-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,6-6', 'NP_A,1-1,6-6') [0.0014795884054072824]\n" +
"'S_A,1-2,6-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,6-6', 'S_A,1-1,6-6') [0.0017317380352644843]\n" +
"'S_S,0-6,null'! -> t237-one_br13334('NP_S>>N_A>>NP_A,0-5,null', 'S_A,0-0,5-6') [0.0028099754127151443]\n" +
"'S_S,0-6,null'! -> t2768-life_br12763('NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-4,null', 'S_A,0-0,4-6') [3.5124692658939304E-4]\n" +
"'S_A,0-0,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,0-0,3-4', 'S_A,0-0,4-6') [0.0017317380352644843]\n" +
"'VP_A,2-2,3-5' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-5', 'VP_A,2-2,5-5') [0.0383258499037845]\n" +
"'S_A,1-2,3-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-5', 'S_A,1-2,5-5') [0.0017317380352644843]\n" +
"'S_A,1-2,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-5', 'S_A,1-2,5-6') [0.0017317380352644843]\n" +
"'NP_A,1-2,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-5', 'NP_A,1-2,5-5') [0.0014795884054072824]\n" +
"'VP_A,1-2,3-5' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-5', 'VP_A,1-2,5-5') [0.0383258499037845]\n" +
"'NP_A,2-2,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-5', 'NP_A,2-2,5-5') [0.0014795884054072824]\n" +
"'NP_A,2-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-5', 'NP_A,2-2,5-6') [0.0014795884054072824]\n" +
"'NP_A,1-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-5', 'NP_A,1-2,5-6') [0.0014795884054072824]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-6,null', 'S_A,1-1,6-6') [0.8125253289506856]\n" +
"'S_S,0-6,null'! -> t2768-life_br12763('NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-6,null', 'S_A,0-0,6-6') [3.5124692658939304E-4]\n" +
"'S_A,1-1,3-4' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-4', 'S_A,1-1,4-4') [0.0017317380352644843]\n" +
"'NP_A,1-1,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-4', 'NP_A,1-1,4-6') [0.0014795884054072824]\n" +
"'S_A,1-1,3-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-4', 'S_A,1-1,4-5') [0.0017317380352644843]\n" +
"'S_A,1-1,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-4', 'S_A,1-1,4-6') [0.0017317380352644843]\n" +
"'NP_A,1-1,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-4', 'NP_A,1-1,4-5') [0.0014795884054072824]\n" +
"'VP_A,1-1,3-4' -> t24-further_br3949('Ad_A>>AdvP_A,1-1,3-4', 'VP_A,1-1,4-4') [0.0383258499037845]\n" +
"'NP_A,1-1,3-4' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-4', 'NP_A,1-1,4-4') [0.0014795884054072824]\n" +
"'S_S,0-6,null'! -> t237-one_br13334('NP_S>>N_A>>NP_A,0-3,null', 'S_A,0-0,3-6') [0.0028099754127151443]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-6,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,1-6,null', 'VP_A,1-1,6-6') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br564'('NP_A,1-1,1-1', 'N_A>>NP_A>>VP_A,1-5,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br564'('NP_A,1-1,1-1', 'N_A>>NP_A>>VP_A,1-4,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br564'('NP_A,1-1,1-1', 'N_A>>NP_A>>VP_A,1-6,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-3,null' -> 't1527-%_br564'('NP_A,1-1,1-1', 'N_A>>NP_A>>VP_A,1-3,null') [1.0]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-4,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,1-4,null', 'VP_A,1-1,4-4') [1.0]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-6,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,2-6,null', 'VP_A,1-2,6-6') [1.0]\n" +
"'NP_A,2-2,3-4' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,2-2,4-4') [0.0014795884054072824]\n" +
"'NP_A,1-2,3-4' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,1-2,4-4') [0.0014795884054072824]\n" +
"'VP_A,2-2,3-4' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-4', 'VP_A,2-2,4-4') [0.0383258499037845]\n" +
"'NP_A,2-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,2-2,4-6') [0.0014795884054072824]\n" +
"'S_A,1-2,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-4', 'S_A,1-2,4-6') [0.0017317380352644843]\n" +
"'NP_A,2-2,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,2-2,4-5') [0.0014795884054072824]\n" +
"'VP_A,1-2,3-4' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-4', 'VP_A,1-2,4-4') [0.0383258499037845]\n" +
"'S_A,1-2,3-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-4', 'S_A,1-2,4-5') [0.0017317380352644843]\n" +
"'S_A,1-2,3-4' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-4', 'S_A,1-2,4-4') [0.0017317380352644843]\n" +
"'NP_A,1-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,1-2,4-6') [0.0014795884054072824]\n" +
"'NP_A,1-2,3-5' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-4', 'NP_A,1-2,4-5') [0.0014795884054072824]\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-4,null' -> t668-policy_br7041('NP_S,0-1,null', 'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-4,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A,0-6,null' -> t237-one_br13333('NP_S,0-1,null', 'N_A>>NP_A,1-6,null') [1.0]\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-5,null' -> t668-policy_br7041('NP_S,0-1,null', 'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>VP_A,0-5,null' -> t615-judge_br5205('NP_S,0-1,null', 'N_A>>NP_A>>VP_A,1-5,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-5,null' -> t2768-life_br12762('NP_S,0-1,null', 'N_A>>NP_A>>NP_S>>VP_A,1-5,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>VP_A,0-4,null' -> t615-judge_br5205('NP_S,0-1,null', 'N_A>>NP_A>>VP_A,1-4,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>VP_A,0-6,null' -> t615-judge_br5205('NP_S,0-1,null', 'N_A>>NP_A>>VP_A,1-6,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A,0-5,null' -> t237-one_br13333('NP_S,0-1,null', 'N_A>>NP_A,1-5,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-6,null' -> t2768-life_br12762('NP_S,0-1,null', 'N_A>>NP_A>>NP_S>>VP_A,1-6,null') [1.0]\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-6,null' -> t668-policy_br7041('NP_S,0-1,null', 'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A,0-3,null' -> t237-one_br13333('NP_S,0-1,null', 'N_A>>NP_A,1-3,null') [1.0]\n" +
"'NP_S>>NP_A>>N_A>>NP_A>>VP_A>>S_A,0-3,null' -> t668-policy_br7041('NP_S,0-1,null', 'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-3,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-4,null' -> t2768-life_br12762('NP_S,0-1,null', 'N_A>>NP_A>>NP_S>>VP_A,1-4,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A>>VP_A,0-3,null' -> t615-judge_br5205('NP_S,0-1,null', 'N_A>>NP_A>>VP_A,1-3,null') [1.0]\n" +
"'NP_S>>N_A>>NP_A,0-4,null' -> t237-one_br13333('NP_S,0-1,null', 'N_A>>NP_A,1-4,null') [1.0]\n" +
"'S_S,0-6,null'! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A,0-6,null', 'S_A,0-0,6-6') [0.0537407797681772]\n" +
"'S_S,0-6,null'! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A,0-3,null', 'S_A,0-0,3-6') [0.0537407797681772]\n" +
"'N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br563'('N_A>>NP_A,1-5,null', 'VP_A,1-1,5-5') [1.0]\n" +
"'S_A,1-2,5-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,5-5', 'S_A,1-1,5-5') [0.0017317380352644843]\n" +
"'VP_A,1-2,5-5' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,5-5', 'VP_A,1-1,5-5') [0.0383258499037845]\n" +
"'NP_A,1-2,5-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,5-5', 'NP_A,1-1,5-6') [0.0014795884054072824]\n" +
"'NP_A,1-2,5-5' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,5-5', 'NP_A,1-1,5-5') [0.0014795884054072824]\n" +
"'S_A,1-2,5-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,5-5', 'S_A,1-1,5-6') [0.0017317380352644843]\n" +
"'S_S,0-6,null'! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A,0-5,null', 'S_A,0-0,5-6') [0.0537407797681772]\n" +
"'S_A,1-2,4-4' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,4-4', 'S_A,1-1,4-4') [0.0017317380352644843]\n" +
"'NP_A,1-2,4-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,4-4', 'NP_A,1-1,4-6') [0.0014795884054072824]\n" +
"'S_A,1-2,4-5' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,4-4', 'S_A,1-1,4-5') [0.0017317380352644843]\n" +
"'S_A,1-2,4-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-2,4-4', 'S_A,1-1,4-6') [0.0017317380352644843]\n" +
"'NP_A,1-2,4-5' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,4-4', 'NP_A,1-1,4-5') [0.0014795884054072824]\n" +
"'VP_A,1-2,4-4' -> t24-further_br3949('Ad_A>>AdvP_A,1-2,4-4', 'VP_A,1-1,4-4') [0.0383258499037845]\n" +
"'NP_A,1-2,4-4' -> t321-there_br3886('Ad_A>>AdvP_A,1-2,4-4', 'NP_A,1-1,4-4') [0.0014795884054072824]\n" +
"'S_S,0-6,null'! -> t2768-life_br12763('NP_S>>N_A>>NP_A>>NP_S>>VP_A,0-5,null', 'S_A,0-0,5-6') [3.5124692658939304E-4]\n" +
"'S_S,0-6,null'! -> t237-one_br13334('NP_S>>N_A>>NP_A,0-4,null', 'S_A,0-0,4-6') [0.0028099754127151443]\n" +
"'NP_A,1-2,3-6' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,3-6') [0.0]\n" +
"'NP_A,1-2,4-6' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,4-6') [0.0]\n" +
"'NP_A,1-2,2-2' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,2-2') [0.0]\n" +
"'NP_A,1-2,3-4' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,3-4') [0.0]\n" +
"'NP_A,1-2,3-5' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,3-5') [0.0]\n" +
"'NP_A,1-2,6-6' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,6-6') [0.0]\n" +
"'Ad_A>>AdvP_A,1-2,3-3' -> t24-no_br3952('Ad_A,1-1,2-2', 'AdvP_A,1-1,2-2') [0.002819522096259497]\n" +
"'Ad_A>>AdvP_A,1-2,5-5' -> t24-no_br3952('Ad_A,1-1,2-2', 'AdvP_A,1-1,2-2') [0.002819522096259497]\n" +
"'Ad_A>>AdvP_A,1-2,4-4' -> t24-no_br3952('Ad_A,1-1,2-2', 'AdvP_A,1-1,2-2') [0.002819522096259497]\n" +
"'Ad_A>>AdvP_A,1-2,6-6' -> t24-no_br3952('Ad_A,1-1,2-2', 'AdvP_A,1-1,2-2') [0.002819522096259497]\n" +
"'Ad_A>>AdvP_A,1-2,2-2' -> t24-no_br3952('Ad_A,1-1,2-2', 'AdvP_A,1-1,2-2') [0.002819522096259497]\n" +
"'NP_A,1-2,5-6' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,5-6') [0.0]\n" +
"'NP_A,1-2,5-5' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,5-5') [0.0]\n" +
"'NP_A,1-2,4-5' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,4-5') [0.0]\n" +
"'NP_A,1-2,4-4' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,4-4') [0.0]\n" +
"'NP_A,1-2,3-3' -> t105-no_br3725('Ad_A,1-1,2-2', 'NP_A,1-1,3-3') [0.0]\n" +
"'N_A>>NP_A>>VP_A,2-4,null' -> 't1527-%_br563'('N_A>>NP_A,2-4,null', 'VP_A,2-2,4-4') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br563'('N_A>>NP_A,2-4,null', 'VP_A,1-2,4-4') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-4,null' -> 't1527-%_br564'('NP_A,1-2,2-2', 'N_A>>NP_A>>VP_A,2-4,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-3,null' -> 't1527-%_br564'('NP_A,1-2,2-2', 'N_A>>NP_A>>VP_A,2-3,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br564'('NP_A,1-2,2-2', 'N_A>>NP_A>>VP_A,2-6,null') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A,1-5,null' -> 't1527-%_br564'('NP_A,1-2,2-2', 'N_A>>NP_A>>VP_A,2-5,null') [1.0]\n" +
"'N_A>>NP_A>>VP_A,1-6,null' -> 't1527-%_br563'('N_A>>NP_A,1-6,null', 'VP_A,1-1,6-6') [1.0]\n" +
"'S_S,0-6,null'! -> t237-one_br13334('NP_S>>N_A>>NP_A,0-6,null', 'S_A,0-0,6-6') [0.0028099754127151443]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-3,null', 'S_A,1-2,3-5') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-4,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-3,null', 'S_A,1-2,3-4') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-3,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-3,null', 'S_A,1-2,3-3') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-3,null', 'S_A,1-2,3-6') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-4,null', 'S_A,1-2,4-6') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-4,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-4,null', 'S_A,1-2,4-4') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,2-4,null', 'S_A,1-2,4-5') [0.8125253289506856]\n" +
"'VP_A,1-1,3-6' -> t24-further_br3949('Ad_A>>AdvP_A,1-1,3-6', 'VP_A,1-1,6-6') [0.0383258499037845]\n" +
"'NP_A,1-1,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,1-1,3-6', 'NP_A,1-1,6-6') [0.0014795884054072824]\n" +
"'S_A,1-1,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,1-1,3-6', 'S_A,1-1,6-6') [0.0017317380352644843]\n" +
"'S_A,1-2,3-6' -> t90-Virtually_br4373('Ad_A>>AdvP_A,2-2,3-6', 'S_A,1-2,6-6') [0.0017317380352644843]\n" +
"'NP_A,2-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-6', 'NP_A,2-2,6-6') [0.0014795884054072824]\n" +
"'VP_A,1-2,3-6' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-6', 'VP_A,1-2,6-6') [0.0383258499037845]\n" +
"'VP_A,2-2,3-6' -> t24-further_br3949('Ad_A>>AdvP_A,2-2,3-6', 'VP_A,2-2,6-6') [0.0383258499037845]\n" +
"'NP_A,1-2,3-6' -> t321-there_br3886('Ad_A>>AdvP_A,2-2,3-6', 'NP_A,1-2,6-6') [0.0014795884054072824]\n" +
"'N_A>>NP_A,1-4,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,1-2,3-4') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,2-6,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,2-2,3-6') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,1-6,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,1-2,3-6') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,1-3,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,1-2,3-3') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,1-5,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,1-2,3-5') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,2-4,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,2-2,3-4') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,2-5,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,2-2,3-5') [2.0079485678444132E-4]\n" +
"'N_A>>NP_A,2-3,null' -> t167-asbestos_br13168('N_A,2-2,3-3', 'NP_A,2-2,3-3') [2.0079485678444132E-4]\n" +
"'S_A,1-1,4-5' -> 't26-._br4744'('._A,4-4,5-5', 'S_A,1-1,5-5') [0.2646410579345089]\n" +
"'AdvP_A,3-3,4-5' -> 't596-._br4740'('._A,4-4,5-5', 'AdvP_A,3-3,5-5') [0.00383141762452107]\n" +
"'S_A,1-2,4-5' -> 't26-._br4744'('._A,4-4,5-5', 'S_A,1-2,5-5') [0.2646410579345089]\n" +
"'S_A,1-2,4-6' -> 't26-._br4744'('._A,4-4,5-5', 'S_A,1-2,5-6') [0.2646410579345089]\n" +
"'NP_A,1-2,4-5' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,1-2,5-5') [8.743022395588484E-4]\n" +
"'NP_A,3-3,4-5' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,3-3,5-5') [8.743022395588484E-4]\n" +
"'NP_A,2-2,4-5' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,2-2,5-5') [8.743022395588484E-4]\n" +
"'NP_A,3-3,4-6' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,3-3,5-6') [8.743022395588484E-4]\n" +
"'NP_A,1-1,4-6' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,1-1,5-6') [8.743022395588484E-4]\n" +
"'NP_A,2-2,4-6' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,2-2,5-6') [8.743022395588484E-4]\n" +
"'NP_A,1-1,4-5' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,1-1,5-5') [8.743022395588484E-4]\n" +
"'NP_A,1-2,4-6' -> 't484-._br4736'('._A,4-4,5-5', 'NP_A,1-2,5-6') [8.743022395588484E-4]\n" +
"'S_A,0-0,4-6' -> 't26-._br4744'('._A,4-4,5-5', 'S_A,0-0,5-6') [0.2646410579345089]\n" +
"'S_A,1-1,4-6' -> 't26-._br4744'('._A,4-4,5-5', 'S_A,1-1,5-6') [0.2646410579345089]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-3,null', 'S_A,1-1,3-5') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-4,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-3,null', 'S_A,1-1,3-4') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-3,null', 'S_A,1-1,3-6') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-3,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-3,null', 'S_A,1-1,3-3') [0.8125253289506856]\n" +
"'N_A>>NP_A>>NP_S>>VP_A,1-4,null' -> t2768-life_br12761('N_A>>NP_A>>NP_S,2-4,null', 'VP_A,1-2,4-4') [1.0]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-4,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-4,null', 'S_A,1-1,4-4') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-4,null', 'S_A,1-1,4-5') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-4,null', 'S_A,1-1,4-6') [0.8125253289506856]\n" +
"'S_S,0-6,null'! -> t167-sort_br12767('NP_S>>N_A>>NP_A>>VP_A,0-4,null', 'S_A,0-0,4-6') [0.0537407797681772]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-5,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-5,null', 'S_A,1-1,5-5') [0.8125253289506856]\n" +
"'NP_A>>N_A>>NP_A>>VP_A>>S_A,1-6,null' -> 't1527-%_br565'('NP_A>>N_A>>NP_A>>VP_A,1-5,null', 'S_A,1-1,5-6') [0.8125253289506856]";
    
    
}
