/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import com.bric.window.WindowMenu;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.automata.language_iteration.SortedLanguageIterator;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.Util;
import static de.up.ling.irtg.util.Util.formatTimeSince;
import de.up.ling.tree.Tree;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import static de.up.ling.irtg.gui.GuiMain.log;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.tree.NodeSelectionListener;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class JLanguageViewer extends javax.swing.JFrame implements NodeSelectionListener {
    private TreeAutomaton automaton;
    private SortedLanguageIterator languageIterator;
    private long numTrees;
    private List<WeightedTree> cachedTrees;
    private InterpretedTreeAutomaton currentIrtg;
    private Tree<String> currentTree;
    private boolean hasBeenPacked = false; // window has been packed once -- after this, only allow manual size changes
    private Map<String, Homomorphism.TreeInterpretationWithPointers> tips;

    /**
     * Creates new form JLanguageViewer
     */
    public JLanguageViewer() {
        initComponents();

        // until we display the first tree, disable everything
        treeIndex.setText("N/A");
        leftButton.setEnabled(false);
        rightButton.setEnabled(false);

        // by default, hide the Advanced menu
        jMenuBar1.remove(mAdvanced);
        jMenuBar1.add(new WindowMenu(this));

        JDerivationViewer dv = new JDerivationViewer(this);
        derivationViewers.add(dv);
        miRemoveView.setEnabled(false);

        if (!GuiMain.isMac()) {
            GuiUtils.replaceMetaByCtrl(jMenuBar1);
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

        // For some reason, new window doesn't always get the focus as it should
        // (at least on Mac). Request it explicitly to make sure hotkeys work.
        SwingUtilities.invokeLater(() -> {
            requestFocus();
        });
    }

    public void setAutomaton(TreeAutomaton automaton, InterpretedTreeAutomaton irtg) {
        this.automaton = automaton;

        currentIrtg = irtg;
        for (Component dv : derivationViewers.getComponents()) {
            ((JDerivationViewer) dv).setInterpretedTreeAutomaton(irtg);
        }

        this.languageIterator = (SortedLanguageIterator) automaton.sortedLanguageIterator();

        cachedTrees = new ArrayList<WeightedTree>();

        if (automaton.isCyclic()) {
            numTrees = -1;
            languageSizeLabel.setText("of INFINITY");
        } else {
            numTrees = automaton.countTrees();
            languageSizeLabel.setText("of " + numTrees);
        }

        goToTree(0);
    }

    private void goToTree(int treeNumber) {
        if (numTrees > -1) {
            if (treeNumber >= numTrees) {
                if (numTrees > Integer.MAX_VALUE) {
                    treeNumber = Integer.MAX_VALUE - 1;
                } else {
                    treeNumber = (int) numTrees - 1;
                }
            }
        }

        if (treeNumber < 0) {
            treeNumber = 0;
        }

        final int tn = treeNumber;

        ensureFirstTreeComputed((dummy) -> {
            long treesToCompute = tn - cachedTrees.size() + 1;
            if (treesToCompute > 0) {
                long start = System.nanoTime();
                while (cachedTrees.size() <= tn) {
                    cachedTrees.add(nextTree());
                }
                log("Enumerated " + treesToCompute + " trees, " + formatTimeSince(start));
            }

            WeightedTree wt = cachedTrees.get(tn);
            Tree<String> tree = automaton.getSignature().resolve(wt.getTree());

            if (currentIrtg != null) {
                // recalculate mapping of dt node to term nodes
                tips = new HashMap<>();
                for (String intrp : currentIrtg.getInterpretations().keySet()) {
                    Homomorphism.TreeInterpretationWithPointers tip = currentIrtg.getInterpretation(intrp).getHomomorphism().mapWithPointers(tree);
                    tips.put(intrp, tip);
                }
            }

            currentTree = tree;
            for (Component dv : derivationViewers.getComponents()) {
                ((JDerivationViewer) dv).displayDerivation(tree);
            }

            weightLabel.setText("w = " + formatWeight(wt.getWeight()));
            weightLabel.setToolTipText("w = " + wt.getWeight());
            treeIndex.setText(Integer.toString(tn + 1));

            leftButton.setEnabled(tn > 0);
            miPreviousTree.setEnabled(tn > 0);

            if (numTrees < 0 || tn < numTrees - 1) {
                rightButton.setEnabled(true);
                miNextTree.setEnabled(true);
            } else {
                rightButton.setEnabled(false);
                miNextTree.setEnabled(false);
            }

        });
    }

    @Override
    public void pack() {
        super.pack(); //To change body of generated methods, choose Tools | Templates.
        hasBeenPacked = true;
    }

    @Override
    public Dimension getPreferredSize() {
        // pin window size to current size => layout of contents does not change window size
        // -- an exception is the very first time the window is packed, then allow it to get correct size
        if (hasBeenPacked) {
            return getSize();
        } else {
            return super.getPreferredSize();
        }
    }

    private void ensureFirstTreeComputed(Consumer<Void> fn) {
        if (!cachedTrees.isEmpty()) {
            fn.accept(null);
        } else {
            // Computing the first tree in the language initializes all the
            // internal data structures of the SortedLanguageIterator.
            // We track this with a progress bar.
            GuiUtils.withProgressBar(this, "Language viewer", "Initializing language iterator ...",
                                     listener -> {
                                 return languageIterator.next(listener);
                             },
                                     (tree, time) -> {
                                 if (time > 500000000) {
                                     GuiMain.log("Initialized language viewer, " + Util.formatTime(time));
                                 }

                                 cachedTrees.add(tree);
                                 fn.accept(null);
                             });
        }
    }

    private WeightedTree nextTree() {
        return languageIterator.next();
    }

    private int getTreeIndex() {
        try {
            return Integer.parseInt(treeIndex.getText()) - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatWeight(double weight) {
        return String.format("%5.2g", weight);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jSeparator3 = new javax.swing.JSeparator();
        controls = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        leftButton = new javax.swing.JButton();
        treeIndex = new javax.swing.JTextField();
        rightButton = new javax.swing.JButton();
        languageSizeLabel = new javax.swing.JLabel();
        weightLabel = new javax.swing.JLabel();
        derivationViewers = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        miOpenIrtg = new javax.swing.JMenuItem();
        miOpenAutomaton = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        miCloseWindow = new javax.swing.JMenuItem();
        miCloseAllWindows = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        miQuit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        miNextTree = new javax.swing.JMenuItem();
        miPreviousTree = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        miAddView = new javax.swing.JMenuItem();
        miRemoveView = new javax.swing.JMenuItem();
        mAdvanced = new javax.swing.JMenu();
        miCopyTestCase = new javax.swing.JMenuItem();

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                handleResize(evt);
            }
        });

        jLabel1.setText("Derivation #");

        leftButton.setText("<");
        leftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leftButtonActionPerformed(evt);
            }
        });

        treeIndex.setText("jTextField1");
        treeIndex.setMinimumSize(new java.awt.Dimension(84, 28));
        treeIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                treeIndexActionPerformed(evt);
            }
        });

        rightButton.setText(">");
        rightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightButtonActionPerformed(evt);
            }
        });

        languageSizeLabel.setText("of INFINITY");

        weightLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        weightLabel.setText("weight");

        org.jdesktop.layout.GroupLayout controlsLayout = new org.jdesktop.layout.GroupLayout(controls);
        controls.setLayout(controlsLayout);
        controlsLayout.setHorizontalGroup(
            controlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(controlsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(leftButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(treeIndex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(rightButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(languageSizeLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 131, Short.MAX_VALUE)
                .add(weightLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        controlsLayout.setVerticalGroup(
            controlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, controlsLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(controlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(leftButton)
                    .add(treeIndex, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(rightButton)
                    .add(languageSizeLabel)
                    .add(weightLabel))
                .add(7, 7, 7))
        );

        derivationViewers.setLayout(new javax.swing.BoxLayout(derivationViewers, javax.swing.BoxLayout.LINE_AXIS));

        jMenu1.setText("File");

        miOpenIrtg.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        miOpenIrtg.setText("Open IRTG ...");
        miOpenIrtg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenIrtgActionPerformed(evt);
            }
        });
        jMenu1.add(miOpenIrtg);

        miOpenAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miOpenAutomaton.setText("Open Tree Automaton ...");
        miOpenAutomaton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenAutomatonActionPerformed(evt);
            }
        });
        jMenu1.add(miOpenAutomaton);
        jMenu1.add(jSeparator1);

        miCloseWindow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.META_MASK));
        miCloseWindow.setText("Close Window");
        miCloseWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCloseWindowActionPerformed(evt);
            }
        });
        jMenu1.add(miCloseWindow);

        miCloseAllWindows.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miCloseAllWindows.setText("Close All Windows");
        miCloseAllWindows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCloseAllWindowsActionPerformed(evt);
            }
        });
        jMenu1.add(miCloseAllWindows);
        jMenu1.add(jSeparator4);

        miQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        miQuit.setText("Quit");
        miQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miQuitActionPerformed(evt);
            }
        });
        jMenu1.add(miQuit);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Tools");

        jMenuItem1.setText("Compute decomposition automaton ...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem1);

        jMenuBar1.add(jMenu3);

        jMenu2.setText("View");

        miNextTree.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.META_MASK));
        miNextTree.setText("Go to Next Derivation");
        miNextTree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miNextTreeActionPerformed(evt);
            }
        });
        jMenu2.add(miNextTree);

        miPreviousTree.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.META_MASK));
        miPreviousTree.setText("Go to Previous Derivation");
        miPreviousTree.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPreviousTreeActionPerformed(evt);
            }
        });
        jMenu2.add(miPreviousTree);
        jMenu2.add(jSeparator2);

        miAddView.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_2, java.awt.event.InputEvent.META_MASK));
        miAddView.setText("Add View");
        miAddView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddViewActionPerformed(evt);
            }
        });
        jMenu2.add(miAddView);

        miRemoveView.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1, java.awt.event.InputEvent.META_MASK));
        miRemoveView.setText("Remove View");
        miRemoveView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRemoveViewActionPerformed(evt);
            }
        });
        jMenu2.add(miRemoveView);

        jMenuBar1.add(jMenu2);

        mAdvanced.setText("Advanced");

        miCopyTestCase.setText("Copy test case");
        miCopyTestCase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCopyTestCaseActionPerformed(evt);
            }
        });
        mAdvanced.add(miCopyTestCase);

        jMenuBar1.add(mAdvanced);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(derivationViewers, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(controls, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(derivationViewers, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(controls, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 42, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void leftButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leftButtonActionPerformed
        goToTree(getTreeIndex() - 1);
    }//GEN-LAST:event_leftButtonActionPerformed

    private void rightButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightButtonActionPerformed
        goToTree(getTreeIndex() + 1);
    }//GEN-LAST:event_rightButtonActionPerformed

    private void treeIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_treeIndexActionPerformed
        goToTree(getTreeIndex());
    }//GEN-LAST:event_treeIndexActionPerformed

    private void handleResize(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_handleResize

    }//GEN-LAST:event_handleResize

    private void miOpenIrtgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenIrtgActionPerformed
        GuiMain.loadIrtg(this);
    }//GEN-LAST:event_miOpenIrtgActionPerformed

    private void miOpenAutomatonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenAutomatonActionPerformed
        GuiMain.loadAutomaton(this);
    }//GEN-LAST:event_miOpenAutomatonActionPerformed

    private void miQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miQuitActionPerformed
        GuiMain.quit();
    }//GEN-LAST:event_miQuitActionPerformed

    private void miAddViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miAddViewActionPerformed
        // collect the interpretations that are currently being displayed
        Set<String> interpretationsCurrentlyDisplayed = new HashSet<>();
        for (Component other : derivationViewers.getComponents()) {
            if (other instanceof JDerivationViewer) {
                String otherView = ((JDerivationViewer) other).getCurrentView();
                interpretationsCurrentlyDisplayed.add(otherView);
            }
        }

        if (currentIrtg != null) {
            boolean foundInterpretation = false;

            // find an interpretation that is not currently being displayed, and show it
            for (String interp : currentIrtg.getInterpretations().keySet()) {
                if (!interpretationsCurrentlyDisplayed.contains(interp)) {
                    addView(interp);
                    foundInterpretation = true;
                    break;
                }
            }

            if (!foundInterpretation) {
                addView(null);
            }
        }
    }//GEN-LAST:event_miAddViewActionPerformed

    /**
     * Adds a view with the specified interpretation. If interpretation is null,
     * the view will show the derivation tree.
     *
     * @param interpretation
     */
    public void addView(String interpretation) {
        JDerivationViewer dv = new JDerivationViewer(this);

        if (currentIrtg != null) {
            dv.setInterpretedTreeAutomaton(currentIrtg);

            if (interpretation != null) {
                dv.setView(interpretation);
            }
        }

        if (currentTree != null) {
            dv.displayDerivation(currentTree);
        }

        derivationViewers.add(dv);
        derivationViewers.revalidate();

        miRemoveView.setEnabled(true);
    }

    private void miRemoveViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRemoveViewActionPerformed
        derivationViewers.remove(derivationViewers.getComponents().length - 1);
        validate();

        if (derivationViewers.getComponents().length == 1) {
            miRemoveView.setEnabled(false);
        }
    }//GEN-LAST:event_miRemoveViewActionPerformed

    private void miPreviousTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miPreviousTreeActionPerformed
        leftButtonActionPerformed(evt);
    }//GEN-LAST:event_miPreviousTreeActionPerformed

    private void miNextTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miNextTreeActionPerformed
        rightButtonActionPerformed(evt);
    }//GEN-LAST:event_miNextTreeActionPerformed

    private void miCloseWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCloseWindowActionPerformed
        setVisible(false);
    }//GEN-LAST:event_miCloseWindowActionPerformed

    private void miCloseAllWindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCloseAllWindowsActionPerformed
        GuiMain.closeAllWindows();
    }//GEN-LAST:event_miCloseAllWindowsActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        GuiMain.showDecompositionDialog(this);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void miCopyTestCaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCopyTestCaseActionPerformed
        StringBuilder buf = new StringBuilder();
        int numInterpretations = currentIrtg.getInterpretations().keySet().size();
        int i = 1;

        buf.append("        runTest(\"XXXXX.irtg\", \"" + currentTree.toString() + "\", [\n");

        for (String interp : currentIrtg.getInterpretations().keySet()) {
            String val = interpretToString(currentTree, interp, currentIrtg);
            buf.append("             \"" + interp + "\":\"" + val + "\"");
            if (i < numInterpretations) {
                buf.append(",");
            }
            buf.append("\n");
            i++;
        }

        buf.append("        ])");

        GuiUtils.copyToClipboard(buf.toString());
    }//GEN-LAST:event_miCopyTestCaseActionPerformed

    private String interpretToString(Tree<String> dt, String interpName, InterpretedTreeAutomaton irtg) {
        Interpretation intrp = irtg.getInterpretation(interpName);
        Algebra alg = intrp.getAlgebra();
        return alg.representAsString(intrp.interpret(dt));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel controls;
    private javax.swing.JPanel derivationViewers;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JLabel languageSizeLabel;
    private javax.swing.JButton leftButton;
    private javax.swing.JMenu mAdvanced;
    private javax.swing.JMenuItem miAddView;
    private javax.swing.JMenuItem miCloseAllWindows;
    private javax.swing.JMenuItem miCloseWindow;
    private javax.swing.JMenuItem miCopyTestCase;
    private javax.swing.JMenuItem miNextTree;
    private javax.swing.JMenuItem miOpenAutomaton;
    private javax.swing.JMenuItem miOpenIrtg;
    private javax.swing.JMenuItem miPreviousTree;
    private javax.swing.JMenuItem miQuit;
    private javax.swing.JMenuItem miRemoveView;
    private javax.swing.JButton rightButton;
    private javax.swing.JTextField treeIndex;
    private javax.swing.JLabel weightLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void nodeSelected(Tree node, boolean isSelected, Color markupColor) {
        System.err.printf("selected[%s]: %s\n", markupColor, node);
    }
    
    
    /*
    TODO:
    - give same TreeInterpretationWithPointers to all viewers
    - implement nodeSelected so it passes messages to all deriv viewers    
    */
}
