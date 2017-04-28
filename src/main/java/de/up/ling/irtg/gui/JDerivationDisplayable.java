/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.TreeWithInterpretations;
import de.up.ling.tree.Tree;
import java.awt.Color;
import javax.swing.JPanel;

/**
 *
 * @author koller
 */
abstract public class JDerivationDisplayable extends JPanel {
    abstract public void setDerivationTree(TreeWithInterpretations twi);
//    abstract public void setDerivationTree(Tree<String> derivationTree);

//    abstract public void refreshMarkers(Map<Tree<String>, Color> markedNodesInDerivationTree);

    abstract public void mark(Tree<String> nodeInDerivationTree, Color markupColor);

    abstract public void unmark(Tree<String> nodeInDerivationTree);
}
