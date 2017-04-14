/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.google.common.collect.Sets;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.pruning.NoPruningPolicy;
import de.up.ling.tree.Tree;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jonas
 */
public class TestSiblingFinderTrees {
    
    public static void main(String[] args) throws IOException, ParserException {
        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.fromPath("../../experimentData/grammar_37.irtg");
        Object input = irtg.parseString("string", "Mr. Vinken is chairman .");
        irtg = irtg.filterForAppearingConstants("string", input);
        System.err.println("starting sf parsing..");
        TreeAutomaton sf = irtg.parseWithSiblingFinder("string", input);
        System.err.println("starting default parsing..");
        Map<String, Object> map = new HashMap<>();
        map.put("string", input);
        TreeAutomaton def = irtg.parseInputObjects(map);
        System.err.println("starting defcondensed parsing..");
        TreeAutomaton cond = irtg.parseCondensedWithPruning(map, new NoPruningPolicy());
        Set<Tree<String>> sfSet = new HashSet<>();
        Iterator<Tree<String>> sfIt = sf.languageIterator();
        Set<Tree<String>> defSet = new HashSet<>();
        Iterator<Tree<String>> defIt = def.languageIterator();
        Set<Tree<String>> condSet = new HashSet<>();
        Iterator<Tree<String>> condIt = cond.languageIterator();
        while (sfIt.hasNext() || defIt.hasNext() || condIt.hasNext()) {
            if (sfIt.hasNext()) {
                sfSet.add(sfIt.next());
            }
            if (defIt.hasNext()) {
                defSet.add(defIt.next());
            }
            if (condIt.hasNext()) {
                condSet.add(condIt.next());
            }
        }
        System.err.println("sf parsing language size: "+sfSet.size());
        System.err.println("default parsing language size: "+defSet.size());
        System.err.println("default/sf languages intersection size: "+Sets.intersection(sfSet, defSet).size());
        System.err.println("condensed parsing language size: "+condSet.size());
        System.err.println("condensed/sf languages intersection size: "+Sets.intersection(sfSet, condSet).size());
        for (Tree<String> tree : sfSet) {
            if (!defSet.contains(tree)) {
                System.err.println("example tree in sf but not default language: "+tree);
                System.err.println("default parsing accepts: "+def.accepts(tree));
                System.err.println("condensed parsing accepts: "+cond.accepts(tree));
                System.err.println("sf parsing accepts: "+sf.accepts(tree));
                System.err.println("irtg automaton accepts: "+irtg.getAutomaton().accepts(tree));
                System.err.println("final state in sf automaton: "+sf.run(tree).iterator().next());
                Tree<String> homTree = irtg.getInterpretation("string").getHomomorphism().apply(tree);
                System.err.println("hom image: "+homTree);
                System.err.println("alg evaluation: "+irtg.getInterpretation("string").getAlgebra().evaluate(homTree));
                break;
            }
        }
    }
    
}
