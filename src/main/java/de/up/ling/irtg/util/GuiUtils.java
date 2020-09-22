/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.ezware.dialog.task.TaskDialogs;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.io.PrintStream;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * @author koller
 */
public class GuiUtils {

    private static ProgressListener globalListener = null;

    /**
     * Copies the given string to the system clipboard.
     *
     */
    public static void copyToClipboard(String s) {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    /**
     * Execute some expensive work and track its progress with a
     * progress bar on the console. Unlike {@link #withProgressBar(java.awt.Frame, java.lang.String, java.lang.String, de.up.ling.irtg.util.ProgressBarWorker, de.up.ling.irtg.util.ValueAndTimeConsumer) },
     * the work is performed on the same thread from which the
     * method is called, because this is the typical usecase in a
     * console setting.
     * 
     * @param <E>
     * @throws Exception 
     */
    public static <E> E withConsoleProgressBar(int width, PrintStream strm, ProgressBarWorker<E> worker) throws Exception {
        ConsoleProgressBar pb = new ConsoleProgressBar(60, System.out);

        try {
            E result = worker.compute(pb.createListener());
            pb.finish();
            return result;
        } catch (Exception e) {
            pb.finish();
            throw e;
        }
    }
    
    /**
     * Execute this on the EDT.
     *
     * @param <E>
     */
    public static <E> void withProgressBar(Frame parent, String title, String description, ProgressBarWorker<E> worker, ValueAndTimeConsumer<E> andThen) {
        if (parent != null && !parent.isVisible()) {
            parent = null;
        }

        final ProgressBarDialog progressBar = new ProgressBarDialog(title, description, parent, false); // wouldn't update if it were modal
        final JProgressBar bar = progressBar.getProgressBar();
        progressBar.getProgressBar().setStringPainted(true);
        progressBar.setVisible(true);

//        // bring progress dialog to front
        SwingUtilities.invokeLater(() -> {
            progressBar.toFront();
            progressBar.repaint();
        });
        
        ProgressListener listener = (currentValue, maxValue, string) -> {
            SwingUtilities.invokeLater(() -> {
                bar.setMaximum(maxValue);
                bar.setValue(currentValue);

                if (string == null) {
                    if (maxValue == 0) {
                        bar.setString("");
                    } else {
                        bar.setString(currentValue * 100 / maxValue + "%");
                    }
                } else {
                    bar.setString(string);
                }
            });
        };

        Thread workerThread = new Thread(() -> {
            long start = System.nanoTime();

            try {
                final E result = worker.compute(listener);
                final long time = System.nanoTime() - start;

                SwingUtilities.invokeLater(() -> {
                    andThen.accept(result, time);
                });
            } catch (Exception e) {
                showError(e);
            } finally {
                progressBar.setVisible(false);
            }
        });

        workerThread.setName("Alto worker thread: " + description);
        workerThread.start();
    }

    public static void bringToFront(Frame comp) {
        SwingUtilities.invokeLater(() -> {
            comp.toFront();
            comp.repaint();
        });

    }

    static public void showError(Component parent, String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    static public void showError(Throwable error) {
        SwingUtilities.invokeLater(() -> {
            TaskDialogs.showException(error);
        });
    }

    public static ProgressListener getGlobalListener() {
        return globalListener;
    }

    /**
     * Sets the global ProgressListener. The preferred method of giving a
     * progress listener to a worker is to pass it as a method argument. This
     * method is only here for cases when this would mess up the code to a
     * terrible extent. You should avoid it whenever possible.
     */
    public static void setGlobalListener(ProgressListener globalListener) {
        GuiUtils.globalListener = globalListener;
    }

    public static void addDebuggingFocusListener(Component comp) {
        comp.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                System.err.println("gained focus: " + comp);
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.err.println("lost focus: " + comp);
            }
        });
    }

    /**
     * Replaces the Meta keymask in all menus of this menu bar
     * by Ctrl. Useful if the menu accelerators were defined for
     * Mac, but should be mapped to Ctrl-based accelerators for
     * Windows or Linux.
     * 
     */
    public static void replaceMetaByCtrl(JMenuBar mb) {
        for (int i = 0; i < mb.getMenuCount(); i++) {
            JMenu menu = mb.getMenu(i);

            for (int j = 0; j < menu.getItemCount(); j++) {
                replaceMetaByCtrl(menu.getItem(j));
            }
        }
    }

    private static void replaceMetaByCtrl(JMenuItem item) {
        if (item != null) {
            KeyStroke ks = item.getAccelerator();

            if (ks != null) {
                int mods = ks.getModifiers();
                int code = ks.getKeyCode();

                if ((mods & InputEvent.META_MASK) > 0) {
                    mods &= ~ InputEvent.META_MASK;
                    mods &= ~ InputEvent.META_DOWN_MASK;
                    mods |= InputEvent.CTRL_MASK;

                    KeyStroke newKs = KeyStroke.getKeyStroke(code, mods);
                    item.setAccelerator(newKs);
                }
            }
        }
    }
}
