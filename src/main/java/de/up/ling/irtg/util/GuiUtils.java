/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import de.up.ling.irtg.gui.ProgressBarDialog;
import java.awt.Frame;
import java.util.function.Consumer;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author koller
 */
public class GuiUtils {    
    /**
     * Execute this on the EDT.
     *
     * @param <E>
     * @param parent
     * @param title
     * @param description
     * @param worker
     * @param andThen
     */
    public static <E> void withProgressBar(Frame parent, String title, String description, ProgressBarWorker<E> worker, ValueAndTimeConsumer<E> andThen) {
        final ProgressBarDialog progressBar = new ProgressBarDialog(description, 10000, parent, false);
        final JProgressBar bar = progressBar.getProgressBar();
        progressBar.getProgressBar().setStringPainted(true);
        progressBar.setVisible(true);

        ProgressListener listener = (currentValue, maxValue, string) -> {
            SwingUtilities.invokeLater(() -> {
                bar.setMaximum(maxValue);
                bar.setValue(currentValue);

                if (string == null) {
                    if( maxValue == 0 ) {
                        bar.setString("");
                    } else {
                        bar.setString(currentValue*100/maxValue + "%");
                    }
                } else {
                    bar.setString(string);
                }
            });
        };

        new Thread(() -> {
            long start = System.nanoTime();
            final E result = worker.compute(listener);
            final long time = System.nanoTime() - start;

            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(false);

                if (result != null) {
                    andThen.accept(result, time);
                }
            });
        }).start();
    }
}
