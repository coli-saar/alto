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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class TagGrammar {

    private Map<String, ElementaryTree> trees;   // tree-name -> elementary-tree
    private SetMultimap<String, LexiconEntry> lexicon; // word -> set(tree-name)

    public TagGrammar() {
        trees = new HashMap<>();
        lexicon = HashMultimap.create();
    }

    public void addElementaryTree(String name, ElementaryTree tree) {
        trees.put(name, tree);
    }

    public void addLexiconEntry(String word, LexiconEntry lex) {
        lexicon.put(word, lex);
    }

    public Collection<String> getWords() {
        return lexicon.keySet();
    }

    public Collection<ElementaryTree> lexicalizeElementaryTrees(String word) {
        List<ElementaryTree> ret = new ArrayList<>();

        if (lexicon.containsKey(word)) {
            for (LexiconEntry lex : lexicon.get(word)) {
                ElementaryTree et = trees.get(lex.getElementaryTreeName());

                if (et == null) {
                    System.err.println("*** UNK ET: " + lex + " for word " + word + "***");
                } else {
                    ret.add(et.lexicalize(word, lex.getFeature("pos"), findSecondary(lex.getFeatures())));
                }
            }
        }

        return ret;
    }
    
    static String findSecondary(Map<String,String> features) {
        if( features.containsKey("sgp1")) {
            return features.get("sgp1");
        } else {
            return features.get("prt1"); // could be null
        }
    }

    public InterpretedTreeAutomaton toIrtg() {
        ConcreteTreeAutomaton<String> auto = new ConcreteTreeAutomaton<>();
        InterpretedTreeAutomaton irtg = new InterpretedTreeAutomaton(auto);

        return irtg;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("ELEMENTARY TREES:\n");
        buf.append(Joiner.on("\n").withKeyValueSeparator(" = ").join(trees));

        buf.append("\nLEXICON ENTRIES:\n");
        buf.append(Joiner.on("\n").withKeyValueSeparator(" = ").join(lexicon.asMap()));

        return buf.toString();
    }
}
