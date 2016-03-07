/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tag;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class TagGrammar {
    private Map<String,ElementaryTree> trees;   // tree-name -> elementary-tree
    private SetMultimap<String,String> lexicon; // word -> set(tree-name)

    public TagGrammar() {
        trees = new HashMap<>();
        lexicon = HashMultimap.create();
    }
    
    public void addElementaryTree(String name, ElementaryTree tree) {
        trees.put(name, tree);
    }
    
    public InterpretedTreeAutomaton toIrtg() {
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);
        
        return irtg;
    }

    @Override
    public String toString() {
        return Joiner.on("\n").withKeyValueSeparator(" = ").join(trees);
    }
    
    
}
