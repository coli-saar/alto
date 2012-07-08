/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.util;

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
}
