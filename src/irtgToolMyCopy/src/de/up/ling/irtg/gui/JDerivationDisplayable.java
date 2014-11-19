/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.tree.Tree;
import javax.swing.JPanel;

/**
 *
 * @author koller
 */
abstract public class JDerivationDisplayable extends JPanel {
    abstract public void setDerivationTree(Tree<String> derivationTree);
}
