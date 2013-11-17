/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A popup menu that copies a string representation of a Swing component to the
 * clipboard. Use {@link #setTextSource(de.up.ling.irtg.gui.popup.PopupTextSource)
 * }
 * to tell the menu how to obtain the text that is to be copied to the
 * clipboard. Use {@link #addAsMouseListener(javax.swing.JComponent) }
 * to add a mouse listener to the component which will open the popup menu on
 * right-click.
 *
 *
 * @author koller
 */
public class PopupMenu extends JPopupMenu implements ActionListener {
    private Map<String, String> labels;

    public PopupMenu(Map<String, String> labels) {
        super();

        this.labels = labels;

        for (String key : labels.keySet()) {
            JMenuItem mi = new JMenuItem("Copy as " + key);
            mi.setActionCommand(key);
            mi.addActionListener(this);
            mi.setEnabled(true);
            add(mi);
        }
    }

    public static PopupMenu create(String... labelArray) {
        Map<String, String> labels = new HashMap<String, String>();

        for (int i = 0; i < labelArray.length; i += 2) {
            labels.put(labelArray[i], labelArray[i + 1]);
        }

        return new PopupMenu(labels);
    }

    public void addAsMouseListener(JComponent comp) {
        comp.addMouseListener(new PopupListener());
    }

    public void actionPerformed(ActionEvent e) {
        StringSelection selection = new StringSelection(labels.get(e.getActionCommand()));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private class PopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                PopupMenu.this.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
