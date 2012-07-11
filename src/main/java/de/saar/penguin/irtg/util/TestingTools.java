/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.util;

import de.saar.basic.StringOrVariable;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;

/**
 *
 * @author koller
 */
public class TestingTools {
    public static Tree<String> pt(String s) {
        return TreeParser.parse(s);
    }
    
    public static Tree<StringOrVariable> ptv(String s) {
        Term x = TermParser.parse(s);
        return x.toTreeWithVariables();
    }
}
