/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import static de.up.ling.irtg.util.GuiUtils.showError;
import de.up.ling.irtg.util.Util;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author koller
 */
public class MaxentWeightsFrame extends JTableDialog<JTreeAutomaton.FtWeight> {
    private MaximumEntropyIrtg irtg;

    public MaxentWeightsFrame(String title, List<JTreeAutomaton.FtWeight> data, MaximumEntropyIrtg irtg) {
        super(title, data);
        this.irtg = irtg;

        int keymask = GuiMain.isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK;

        JMenuItem miSaveWeights = new JMenuItem("Save Weights ...");
        miSaveWeights.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, keymask));
        miSaveWeights.addActionListener(evt -> miSaveActionPerformed(evt));
        
        fileMenu.insert(miSaveWeights, 0);
        fileMenu.insertSeparator(1);
    }

    private void miSaveActionPerformed(ActionEvent evt) {
        File file = GuiMain.chooseFileForSaving(new FileNameExtensionFilter("Maxent weights (*.txt)", "txt"), this);

        if (file != null) {
            long start = System.nanoTime();

            try {
                irtg.writeWeights(new FileWriter(file));
                GuiMain.log("Saved maxent weights, " + Util.formatTimeSince(start));
            } catch (IOException e) {
                showError(new Exception("An error occurred while saving the maxent weights to " + file.getName(), e));
            }
        }
    }
}
