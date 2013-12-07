/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import com.bric.window.WindowMenu;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.ProgressListener;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.corpus.Corpus;
import static de.up.ling.irtg.gui.GuiMain.formatTimeSince;
import static de.up.ling.irtg.gui.GuiMain.log;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author koller
 */
public class JTreeAutomaton extends javax.swing.JFrame {
    private TreeAutomaton automaton;
    private InterpretedTreeAutomaton irtg;
    private List<String> annotationsInOrder;
    private List<Rule> rulesInOrder;
    private long numRules;
    private long numStates;
    private int maxRuleRank;

    /**
     * Creates new form JInterpretedTreeAutomaton
     */
    public JTreeAutomaton(TreeAutomaton<?> automaton, TreeAutomatonAnnotator annotator) {
        initComponents();

        if (!GuiMain.isMac()) {
            miOpenIrtg.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
            miOpenAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
            miSaveAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
            miQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
            miShowLanguage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
            miParse.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
            miCloseAllWindows.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
            miCloseWindow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));

        }

        jMenuBar2.add(new WindowMenu(this));

//        new WindowMenu(this).get

        this.automaton = automaton;

        Vector<String> columnIdentifiers = new Vector<String>();
        columnIdentifiers.add("");
        columnIdentifiers.add("");
        columnIdentifiers.add("");
        columnIdentifiers.add("weight");

        annotationsInOrder = new ArrayList<String>();
        if (annotator != null) {
            annotationsInOrder.addAll(annotator.getAnnotationIdentifiers());
            columnIdentifiers.addAll(annotationsInOrder);
        }

        entries.setColumnIdentifiers(columnIdentifiers);

        fillEntries(automaton, annotator);

        String type = "Tree automaton";
        if (annotator != null && annotator instanceof IrtgTreeAutomatonAnnotator) {
            type = "IRTG";
        }

        setStatusBar(type);

        TableColumnAdjuster tca = new TableColumnAdjuster(jTable1);
//        tca.setOnlyAdjustLarger(false);
        tca.adjustColumns();

        final Color alternateRowColor = new Color(204, 229, 255);
        jTable1.setDefaultRenderer(Object.class, new TableCellRenderer() {
            private DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row % 2 == 0) {
                    c.setBackground(Color.WHITE);
                } else {
                    c.setBackground(alternateRowColor);
                }
                return c;
            }
        });
    }

    private void fillEntries(TreeAutomaton<?> automaton, TreeAutomatonAnnotator annotator) {
        IntSet allStates = new IntOpenHashSet();

        rulesInOrder = new ArrayList<Rule>(automaton.getRuleSet());
        maxRuleRank = 0;

        for (Rule rule : rulesInOrder) {
            allStates.add(rule.getParent());

            Vector<String> row = new Vector<String>();
            row.add(automaton.getStateForId(rule.getParent()).toString() + (automaton.getFinalStates().contains(rule.getParent()) ? "!" : ""));
            row.add("->");

            List<String> resolvedRhsStates = new ArrayList<String>();
            for (int childState : rule.getChildren()) {
                resolvedRhsStates.add(automaton.getStateForId(childState).toString());
                allStates.add(childState);
            }

            String label = automaton.getSignature().resolveSymbolId(rule.getLabel());
            row.add(label
                    + (rule.getArity() > 0 ? "(" : "")
                    + StringTools.join(resolvedRhsStates, ", ")
                    + (rule.getArity() > 0 ? ")" : ""));

            row.add("[" + Double.toString(rule.getWeight()) + "]");

            if (annotator != null) {
                for (String anno : annotationsInOrder) {
                    row.add(annotator.getAnnotation(rule, anno));
                }
            }

            entries.addRow(row);

            if (rule.getArity() > maxRuleRank) {
                maxRuleRank = rule.getArity();
            }
        }

        numRules = rulesInOrder.size();
        numStates = allStates.size();
    }

    private void setStatusBar(String desc) {
        statusBarLabel.setText(desc + " with " + numRules + " rules (max rank " + maxRuleRank + "), " + numStates + " states.");
    }

    public void setIrtg(InterpretedTreeAutomaton irtg) {
        this.irtg = irtg;

        miBinarize.setEnabled(true);
        miMaxent.setEnabled(irtg instanceof MaximumEntropyIrtg);
    }

    public void setParsingEnabled(boolean enabled) {
        miParse.setEnabled(enabled);

        miTrainEM.setEnabled(enabled);
        miTrainML.setEnabled(enabled);
        miTrainVB.setEnabled(enabled);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        entries = new javax.swing.table.DefaultTableModel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        statusBar = new javax.swing.JPanel();
        statusBarLabel = new javax.swing.JLabel();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        miOpenIrtg = new javax.swing.JMenuItem();
        miOpenAutomaton = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        miSaveAutomaton = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        miCloseWindow = new javax.swing.JMenuItem();
        miCloseAllWindows = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        miQuit = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        miShowLanguage = new javax.swing.JMenuItem();
        miParse = new javax.swing.JMenuItem();
        miBinarize = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        miTrainML = new javax.swing.JMenuItem();
        miTrainEM = new javax.swing.JMenuItem();
        miTrainVB = new javax.swing.JMenuItem();
        miMaxent = new javax.swing.JMenu();
        miLoadMaxentWeights = new javax.swing.JMenuItem();
        miShowMaxentWeights = new javax.swing.JMenuItem();
        miTrainMaxent = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItem1 = new javax.swing.JMenuItem();

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );

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

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(entries);
        jTable1.setEnabled(false);
        jScrollPane1.setViewportView(jTable1);

        statusBar.setLayout(new javax.swing.BoxLayout(statusBar, javax.swing.BoxLayout.LINE_AXIS));

        statusBarLabel.setText("jLabel1");
        statusBar.add(statusBarLabel);

        jMenu3.setText("File");

        miOpenIrtg.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        miOpenIrtg.setText("Open IRTG ...");
        miOpenIrtg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenIrtgActionPerformed(evt);
            }
        });
        jMenu3.add(miOpenIrtg);

        miOpenAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miOpenAutomaton.setText("Open Tree Automaton ...");
        miOpenAutomaton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenAutomatonActionPerformed(evt);
            }
        });
        jMenu3.add(miOpenAutomaton);
        jMenu3.add(jSeparator1);

        miSaveAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        miSaveAutomaton.setText("Save Tree Automaton ...");
        miSaveAutomaton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveAutomatonActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveAutomaton);
        jMenu3.add(jSeparator4);

        miCloseWindow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.META_MASK));
        miCloseWindow.setText("Close Window");
        miCloseWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCloseWindowActionPerformed(evt);
            }
        });
        jMenu3.add(miCloseWindow);

        miCloseAllWindows.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miCloseAllWindows.setText("Close All Windows");
        miCloseAllWindows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCloseAllWindowsActionPerformed(evt);
            }
        });
        jMenu3.add(miCloseAllWindows);
        jMenu3.add(jSeparator2);

        miQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        miQuit.setText("Quit");
        miQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miQuitActionPerformed(evt);
            }
        });
        jMenu3.add(miQuit);

        jMenuBar2.add(jMenu3);

        jMenu4.setText("Tools");

        miShowLanguage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.META_MASK));
        miShowLanguage.setText("Show Language ...");
        miShowLanguage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowLanguageActionPerformed(evt);
            }
        });
        jMenu4.add(miShowLanguage);

        miParse.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.META_MASK));
        miParse.setText("Parse ...");
        miParse.setEnabled(false);
        miParse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miParseActionPerformed(evt);
            }
        });
        jMenu4.add(miParse);

        miBinarize.setText("Binarize ...");
        miBinarize.setEnabled(false);
        miBinarize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miBinarizeActionPerformed(evt);
            }
        });
        jMenu4.add(miBinarize);
        jMenu4.add(jSeparator3);

        miTrainML.setText("Maximum Likelihood Training ...");
        miTrainML.setEnabled(false);
        miTrainML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miTrainMLActionPerformed(evt);
            }
        });
        jMenu4.add(miTrainML);

        miTrainEM.setText("EM Training ...");
        miTrainEM.setEnabled(false);
        miTrainEM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miTrainEMActionPerformed(evt);
            }
        });
        jMenu4.add(miTrainEM);

        miTrainVB.setText("Variational Bayes Training ...");
        miTrainVB.setEnabled(false);
        miTrainVB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miTrainVBActionPerformed(evt);
            }
        });
        jMenu4.add(miTrainVB);

        miMaxent.setText("Maximum Entropy");
        miMaxent.setEnabled(false);

        miLoadMaxentWeights.setText("Load Weights ...");
        miLoadMaxentWeights.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miLoadMaxentWeightsActionPerformed(evt);
            }
        });
        miMaxent.add(miLoadMaxentWeights);

        miShowMaxentWeights.setText("Show Weights ...");
        miShowMaxentWeights.setEnabled(false);
        miShowMaxentWeights.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowMaxentWeightsActionPerformed(evt);
            }
        });
        miMaxent.add(miShowMaxentWeights);

        miTrainMaxent.setText("Train ...");
        miTrainMaxent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miTrainMaxentActionPerformed(evt);
            }
        });
        miMaxent.add(miTrainMaxent);

        jMenu4.add(miMaxent);
        jMenu4.add(jSeparator5);

        jMenuItem1.setText("Compute decomposition automaton ...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem1);

        jMenuBar2.add(jMenu4);

        setJMenuBar(jMenuBar2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                    .add(statusBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void miOpenIrtgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenIrtgActionPerformed
        GuiMain.loadIrtg(this);
    }//GEN-LAST:event_miOpenIrtgActionPerformed

    private void miOpenAutomatonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenAutomatonActionPerformed
        GuiMain.loadAutomaton(this);
    }//GEN-LAST:event_miOpenAutomatonActionPerformed

    private void miSaveAutomatonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveAutomatonActionPerformed
        GuiMain.saveAutomaton(automaton, this);
    }//GEN-LAST:event_miSaveAutomatonActionPerformed

    private void miQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miQuitActionPerformed
        GuiMain.quit();
    }//GEN-LAST:event_miQuitActionPerformed

    private void miShowLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miShowLanguageActionPerformed
        JLanguageViewer lv = new JLanguageViewer();
        lv.setAutomaton(automaton, irtg);
        lv.setTitle("Language of " + getTitle());
        lv.pack();
        lv.setVisible(true);
    }//GEN-LAST:event_miShowLanguageActionPerformed

    private void miParseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miParseActionPerformed
        if (irtg != null) {
            List<Boolean> hasOptions = new ArrayList<Boolean>(annotationsInOrder.size());
            for (String intp : annotationsInOrder) {
                hasOptions.add(irtg.getInterpretation(intp).getAlgebra().hasOptions());
            }

            JInputForm jif = JInputForm.showForm(this, annotationsInOrder, hasOptions);
            jif.setVisible(true);

            final Map<String, String> inputs = jif.getInputValues();
            final Map<String, String> options = jif.getOptions();

            if (inputs != null) {
                new Thread() {
                    @Override
                    public void run() {
                        TreeAutomaton chart = null;

                        try {
                            for (String intp : options.keySet()) {
                                irtg.getInterpretation(intp).getAlgebra().setOptions(options.get(intp));
                            }

                            long start = System.nanoTime();
                            chart = irtg.parse(inputs);
                            log("Computed parse chart for " + inputs + ", " + formatTimeSince(start));
                        } catch (ParserException ex) {
                            GuiMain.showError(JTreeAutomaton.this, "An error occurred while parsing the input objects " + inputs + ": " + ex.getMessage());
                        } catch (Exception ex) {
                            GuiMain.showError(JTreeAutomaton.this, ex.getMessage());
                        }

                        if (chart != null) {
                            JTreeAutomaton jta = new JTreeAutomaton(chart, null);
                            jta.setIrtg(irtg);
                            jta.setTitle("Parse chart: " + inputs);
                            jta.pack();
                            jta.setVisible(true);
                        }
                    }
                }.start();
            }
        }
    }//GEN-LAST:event_miParseActionPerformed

    private void updateWeights() {
        for (int i = 0; i < rulesInOrder.size(); i++) {
            entries.setValueAt("[" + rulesInOrder.get(i).getWeight() + "]", i, 3);
        }
    }

    private void miTrainMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainMLActionPerformed
        Corpus corpus = GuiMain.loadAnnotatedCorpus(irtg, this);

        if (corpus != null) {
            long start = System.nanoTime();
            irtg.trainML(corpus);
            GuiMain.log("Performed ML training, " + GuiMain.formatTimeSince(start));
            updateWeights();
        }
    }//GEN-LAST:event_miTrainMLActionPerformed

    private void miTrainEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainEMActionPerformed
        final Corpus corpus = GuiMain.loadUnannotatedCorpus(irtg, JTreeAutomaton.this);

        new Thread() {
            @Override
            public void run() {
                if (corpus != null) {
                    final ChartComputationProgressBar pb = new ChartComputationProgressBar(GuiMain.getApplication(), false, corpus.getNumberOfInstances());
                    pb.setLabelText("Performing EM training ...");
                    pb.setVisible(true);

                    long start = System.nanoTime();
                    try {
                        irtg.trainEM(corpus, new ProgressBarTrainingIterationListener(pb));
                    } finally {
                        pb.setVisible(false);
                    }

                    GuiMain.log("Performed EM training, " + GuiMain.formatTimeSince(start));
                    updateWeights();
                }
            }
        }.start();
    }//GEN-LAST:event_miTrainEMActionPerformed

    private void miTrainVBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainVBActionPerformed
        final Corpus corpus = GuiMain.loadUnannotatedCorpus(irtg, JTreeAutomaton.this);

        new Thread() {
            @Override
            public void run() {
                if (corpus != null) {
                    final ChartComputationProgressBar pb = new ChartComputationProgressBar(GuiMain.getApplication(), false, corpus.getNumberOfInstances());
                    pb.setLabelText("Performing VB training ...");
                    pb.setVisible(true);

                    long start = System.nanoTime();
                    try {
                        irtg.trainVB(corpus, new ProgressBarTrainingIterationListener(pb));
                    } finally {
                        pb.setVisible(false);
                    }

                    GuiMain.log("Performed VB training, " + GuiMain.formatTimeSince(start));
                    updateWeights();
                }
            }
        }.start();

    }//GEN-LAST:event_miTrainVBActionPerformed

    private static class ProgressBarTrainingIterationListener implements ProgressListener {
        private ChartComputationProgressBar pb;

        public ProgressBarTrainingIterationListener(ChartComputationProgressBar pb) {
            this.pb = pb;
        }

        public void update(int iterationNumber, int instanceNumber) {
            pb.update(instanceNumber, null, null);
        }
    }

    private void miLoadMaxentWeightsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miLoadMaxentWeightsActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            long start = System.nanoTime();
            GuiMain.loadMaxentWeights((MaximumEntropyIrtg) irtg, this);
            GuiMain.log("Loaded maxent weights, " + GuiMain.formatTimeSince(start));

            miShowMaxentWeights.setEnabled(true);
            
            miShowMaxentWeightsActionPerformed(null);
        }
    }//GEN-LAST:event_miLoadMaxentWeightsActionPerformed

    private void miTrainMaxentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainMaxentActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            final Corpus corpus = GuiMain.loadAnnotatedCorpus(irtg, miMaxent);

            new Thread() {
                @Override
                public void run() {
                    if (corpus != null) {
                        final ChartComputationProgressBar pb = new ChartComputationProgressBar(GuiMain.getApplication(), false, corpus.getNumberOfInstances());
                        pb.setLabelText("Performing Maximum Entropy training ...");
                        pb.setVisible(true);

                        long start = System.nanoTime();

                        try {
                            ((MaximumEntropyIrtg) irtg).trainMaxent(corpus, new ProgressBarTrainingIterationListener(pb));
                        } finally {
                            pb.setVisible(false);
                        }

                        GuiMain.log("Trained maxent model, " + GuiMain.formatTimeSince(start));

                        miShowMaxentWeights.setEnabled(true);
                        
                        miShowMaxentWeightsActionPerformed(null);
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_miTrainMaxentActionPerformed

    static class FtWeight {
        public String feature;
        public String weight;
    }

    private void miShowMaxentWeightsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miShowMaxentWeightsActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            MaximumEntropyIrtg mirtg = (MaximumEntropyIrtg) irtg;
            List<FtWeight> fts = new ArrayList<FtWeight>();
            List<String> ftNames = mirtg.getFeatureNames();

            for (int i = 0; i < mirtg.getNumFeatures(); i++) {
                FtWeight x = new FtWeight();
                x.feature = ftNames.get(i);
                x.weight = Double.toString(mirtg.getFeatureWeight(i));
                fts.add(x);
            }

            JTableDialog<FtWeight> dialog = new MaxentWeightsFrame("Maxent weights for " + getTitle(), fts, mirtg);
            dialog.setVisible(true);
        }
    }//GEN-LAST:event_miShowMaxentWeightsActionPerformed

    private void miBinarizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miBinarizeActionPerformed
        if (irtg != null) {
            final RegularSeedChooser rsc = new RegularSeedChooser(irtg, this, true);
            rsc.setVisible(true);

            if (rsc.getSelectedAlgebras() != null) {
                new Thread() {
                    @Override
                    public void run() {
                        final ProgressBarDialog pb = new ProgressBarDialog("Binarizing IRTG", irtg.getAutomaton().getRuleSet().size(), JTreeAutomaton.this, false);
                        pb.setVisible(true);

                        ProgressListener listener = new ProgressListener() {
                            public void update(int iterationNumber, int instanceNumber) {
                                pb.update(iterationNumber);
                            }
                        };

                        long startTime = System.nanoTime();
                        BkvBinarizer binarizer = new BkvBinarizer(rsc.getSelectedSeeds());
                        final InterpretedTreeAutomaton binarized = binarizer.binarize(irtg, rsc.getSelectedAlgebras(), listener);
                        GuiMain.log("Binarized IRTG, " + GuiMain.formatTimeSince(startTime));

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                pb.setVisible(false);

                                JTreeAutomaton jta = new JTreeAutomaton(binarized.getAutomaton(), new IrtgTreeAutomatonAnnotator(binarized));
                                jta.setTitle("Binarization of " + getTitle());
                                jta.setIrtg(binarized);
                                jta.setParsingEnabled(true);
                                jta.pack();
                                jta.setVisible(true);
                            }
                        });
                    }
                }.start();
            }
        }

    }//GEN-LAST:event_miBinarizeActionPerformed

    private void miCloseWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCloseWindowActionPerformed
        setVisible(false);
    }//GEN-LAST:event_miCloseWindowActionPerformed

    private void miCloseAllWindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCloseAllWindowsActionPerformed
        GuiMain.closeAllWindows();
    }//GEN-LAST:event_miCloseAllWindowsActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        GuiMain.showDecompositionDialog(this);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.table.DefaultTableModel entries;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JTable jTable1;
    private javax.swing.JMenuItem miBinarize;
    private javax.swing.JMenuItem miCloseAllWindows;
    private javax.swing.JMenuItem miCloseWindow;
    private javax.swing.JMenuItem miLoadMaxentWeights;
    private javax.swing.JMenu miMaxent;
    private javax.swing.JMenuItem miOpenAutomaton;
    private javax.swing.JMenuItem miOpenIrtg;
    private javax.swing.JMenuItem miParse;
    private javax.swing.JMenuItem miQuit;
    private javax.swing.JMenuItem miSaveAutomaton;
    private javax.swing.JMenuItem miShowLanguage;
    private javax.swing.JMenuItem miShowMaxentWeights;
    private javax.swing.JMenuItem miTrainEM;
    private javax.swing.JMenuItem miTrainML;
    private javax.swing.JMenuItem miTrainMaxent;
    private javax.swing.JMenuItem miTrainVB;
    private javax.swing.JPanel statusBar;
    private javax.swing.JLabel statusBarLabel;
    // End of variables declaration//GEN-END:variables
}
