/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.TreeAutomatonInputCodec;

/**
 *
 * @author JG
 */
public class LanguageItBugTest {
    
    public static void main(String[] args) {
        
        System.err.println("Works for a minimal cyclic automaton");
        TreeAutomaton auto1 = new TreeAutomatonInputCodec().read(minCyclic);
        System.err.println(auto1.isCyclic());
        System.err.println(auto1.viterbi());
        System.err.println(auto1.languageIterator().next());
        
        System.err.println("Does not work for this automaton (but viterbi works)");
        TreeAutomaton auto2 = new TreeAutomatonInputCodec().read(autoString);
        System.err.println(auto2.isCyclic());
        System.err.println(auto2.viterbi());
        System.err.println("Now this will just run seemingly forever...");
        System.err.println(auto2.languageIterator().next());
    }
    
    private static String minCyclic = "a! -> r_1(a) [0.5]\n" +
                                        "a! -> r_2 [0.5]";
    
    private static String autoString = "'N_bh14,[[bh14], [bh12], [bh13], [bh11]]' -> button_bh14 [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> door_dh1r1 [0.5]\n" +
"'N_bh12,[[bh14], [bh12], [bh13], [bh11]]' -> button_bh12 [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> door_dh1r3 [0.5]\n" +
"'N_bh13,[[bh14], [bh12], [bh13], [bh11]]' -> button_bh13 [0.5]\n" +
"'N_bh11,[[bh14], [bh12], [bh13], [bh11]]' -> button_bh11 [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> door_dh1r4 [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> door_dh1r2 [0.5]\n" +
"'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> def_dh1r3('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> green_bh14('N_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh14,[[bh14], [bh13], [bh11]]' -> def_bh14('N_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> def_dh1r3('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r3_bh13('N_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh13,[[bh12], [bh13], [bh11]]' -> def_bh13('N_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> green_bh13('N_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh11,[[bh13], [bh11]]' -> def_bh11('N_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> blue_bh11('N_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh11]]' -> blue_bh12('N_bh12,[[bh12], [bh11]]') [0.5]\n" +
"'NP_bh12,[[bh12], [bh11]]' -> def_bh12('N_bh12,[[bh12], [bh11]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh12], [bh11]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh12], [bh11]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> def_dh1r2('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> green_bh14('N_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'NP_bh14,[[bh14], [bh13]]' -> def_bh14('N_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh13]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh13]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh11,[[bh14], [bh13], [bh11]]' -> def_bh11('N_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> blue_bh11('N_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh11,[[bh11]]' -> def_bh11('N_bh11,[[bh11]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> blue_bh11('N_bh11,[[bh11]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh13,[[bh14], [bh13]]' -> def_bh13('N_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> green_bh13('N_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh13,[[bh14], [bh13], [bh11]]' -> def_bh13('N_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> green_bh13('N_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh11,[[bh12], [bh11]]' -> def_bh11('N_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh11]]' -> blue_bh11('N_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_dh1r2,[[dh1r2]]' -> def_dh1r2('N_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> def_dh1r1('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]' -> def_bh11('N_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh11]]' -> blue_bh11('N_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh14], [bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh13,[[bh13], [bh11]]' -> def_bh13('N_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> green_bh13('N_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh11]]' -> blue_bh12('N_bh12,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh12,[[bh12], [bh13], [bh11]]' -> def_bh12('N_bh12,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh13], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh13], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_bh11,[[bh12], [bh13], [bh11]]' -> def_bh11('N_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh11]]' -> blue_bh11('N_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh11,[[bh12], [bh13], [bh11]]' -> leftof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh11,[[bh13], [bh11]]' -> rightof_bh11_dh1r2('N_bh11,[[bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> def_dh1r1('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh13('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r1_bh14('N_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'NP_dh1r4,[[dh1r2], [dh1r4]]' -> def_dh1r4('N_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh12,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh12,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh12,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13]]' -> green_bh14('N_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh14,[[bh14], [bh12], [bh13], [bh11]]' -> def_bh14('N_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> leftof_bh14_dh1r1('N_bh14,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh14,[[bh14], [bh13], [bh11]]' -> rightof_bh14_dh1r4('N_bh14,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh11]]' -> blue_bh12('N_bh12,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh12,[[bh14], [bh12], [bh13], [bh11]]' -> def_bh12('N_bh12,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh12,[[bh12], [bh13], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh12,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh12_dh1r4('N_bh12,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> def_dh1r2('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> def_dh1r4('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh12,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh12,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r2], [dh1r4]]' -> rightof_dh1r4_bh12('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh12,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]' -> leftof_dh1r4_bh14('N_dh1r4,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]', 'NP_bh14,[[bh14], [bh13]]') [0.5]\n" +
"'NP_dh1r2,[[dh1r2], [dh1r4]]' -> def_dh1r2('N_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh13]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2]]' -> rightof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> leftof_dh1r2_bh13('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh13,[[bh14], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> leftof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_dh1r2,[[dh1r2], [dh1r4]]' -> rightof_dh1r2_bh11('N_dh1r2,[[dh1r2], [dh1r4]]', 'NP_bh11,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NP_bh13,[[bh13]]' -> def_bh13('N_bh13,[[bh13]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> green_bh13('N_bh13,[[bh13]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh13]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r3('N_bh13,[[bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r3('N_bh13,[[bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r1('N_bh13,[[bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r1('N_bh13,[[bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r1('N_bh13,[[bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r1('N_bh13,[[bh13]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r3('N_bh13,[[bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r3('N_bh13,[[bh13]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> leftof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13]]' -> rightof_bh13_dh1r2('N_bh13,[[bh13]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'NP_bh13,[[bh14], [bh12], [bh13], [bh11]]' -> def_bh13('N_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13]]' -> green_bh13('N_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'NPH_bh13,[[bh13]]'! -> hatdef_bh13('N_bh13,[[bh14], [bh12], [bh13], [bh11]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r3('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r3('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r3,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> leftof_bh13_dh1r1('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh13], [bh11]]' -> rightof_bh13_dh1r1('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r1,[[dh1r1], [dh1r3], [dh1r2]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh12], [bh13], [bh11]]' -> leftof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"'N_bh13,[[bh14], [bh13], [bh11]]' -> rightof_bh13_dh1r2('N_bh13,[[bh14], [bh12], [bh13], [bh11]]', 'NP_dh1r2,[[dh1r1], [dh1r3], [dh1r2], [dh1r4]]') [0.5]\n" +
"";
    
}
