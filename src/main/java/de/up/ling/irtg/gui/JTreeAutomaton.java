/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import com.bric.window.WindowMenu;
import com.google.common.collect.Iterables;
import de.saar.basic.Pair;
import de.saar.basic.StringTools;
import de.up.ling.gui.datadialog.DataDialog;
import de.up.ling.gui.datadialog.entries.DataField;
import de.up.ling.gui.datadialog.DataPanelContainer;
import de.up.ling.gui.datadialog.entries.DataPanelEntry;
import de.up.ling.gui.datadialog.entries.ReflectionEntry;
import de.up.ling.gui.datadialog.entries.ConcreteBooleanDataPanelEntry;
import de.up.ling.gui.datadialog.entries.ConcreteListDataPanelEntry;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.NullFilterAlgebra;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.TreeAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.coarse_to_fine.CoarseToFineParser;
import de.up.ling.irtg.automata.pruning.NoPruningPolicy;
import de.up.ling.irtg.automata.pruning.PruningPolicy;
import de.up.ling.irtg.automata.pruning.QuotientPruningPolicy;
import de.up.ling.irtg.automata.pruning.SemiringFOM;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.codec.AlgebraStringRepresentationOutputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.corpus.InterpretationPrintingPolicy;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.semiring.DoubleArithmeticSemiring;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.LambdaStopwatch;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.awt.Color;
import java.awt.Component;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
    private static final boolean SHOW_DEBUG_MENU = true;
    private LambdaStopwatch T = new LambdaStopwatch(null);

    /**
     * Creates new form JInterpretedTreeAutomaton
     */
    public JTreeAutomaton(TreeAutomaton<?> automaton, TreeAutomatonAnnotator annotator) {
        initComponents();

        jMenuBar2.add(new WindowMenu(this));

//        if (!SHOW_DEBUG_MENU) {
//            jMenuBar2.remove(debugMenu);
//        }

        this.automaton = automaton;

        // We used to check the automaton for emptiness here and 
//        Tree vit = T.t("emptiness", () -> automaton.viterbi());
        boolean isEmpty = T.t("emptiness", () -> automaton.isEmpty());
        if (isEmpty) {
            miShowLanguage.setEnabled(false);
        }

        Vector<String> columnIdentifiers = new Vector<>();
        columnIdentifiers.add("");
        columnIdentifiers.add("");
        columnIdentifiers.add("");
        columnIdentifiers.add("weight");

        annotationsInOrder = new ArrayList<>();
        if (annotator != null) {
            annotationsInOrder.addAll(annotator.getAnnotationIdentifiers());
            columnIdentifiers.addAll(annotationsInOrder);
        }

        entries.setColumnIdentifiers(columnIdentifiers);

        fillEntries(automaton, annotator);

        String type = "Tree automaton";
        if (annotator instanceof IrtgTreeAutomatonAnnotator) {
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

        rulesInOrder = new ArrayList<>();
        Iterables.addAll(rulesInOrder, T.t("ruleSet", () -> automaton.getRuleSet()));

        maxRuleRank = 0;

        for (Rule rule : rulesInOrder) {
            allStates.add(rule.getParent());

            Vector<String> row = new Vector<>();
            row.add(automaton.getStateForId(rule.getParent()).toString() + (automaton.getFinalStates().contains(rule.getParent()) ? "!" : ""));
            row.add("->");

            List<String> resolvedRhsStates = new ArrayList<>();
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
        miSaveIrtg.setEnabled(true);
    }

    public void setParsingEnabled(boolean enabled) {
        miParse.setEnabled(enabled);
        miBulkParse.setEnabled(enabled);

        miTrainEM.setEnabled(enabled);
        miTrainML.setEnabled(enabled);
        miTrainVB.setEnabled(enabled);
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
        miSaveIrtg = new javax.swing.JMenuItem();
        miSaveAutomaton = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        miCloseWindow = new javax.swing.JMenuItem();
        miCloseAllWindows = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        miQuit = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        miShowLanguage = new javax.swing.JMenuItem();
        miParse = new javax.swing.JMenuItem();
        miBulkParse = new javax.swing.JMenuItem();
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
        miOpenIrtg.addActionListener(evt -> miOpenIrtgActionPerformed(evt));
        jMenu3.add(miOpenIrtg);

        miOpenAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miOpenAutomaton.setText("Open Tree Automaton ...");
        miOpenAutomaton.addActionListener(evt -> miOpenAutomatonActionPerformed(evt));
        jMenu3.add(miOpenAutomaton);
        jMenu3.add(jSeparator1);

        miSaveIrtg.setText("Save IRTG ...");
        miSaveIrtg.setEnabled(false);
        miSaveIrtg.addActionListener(evt -> miSaveIrtgActionPerformed(evt));
        jMenu3.add(miSaveIrtg);

        miSaveAutomaton.setText("Save Tree Automaton ...");
        miSaveAutomaton.addActionListener(evt -> miSaveAutomatonActionPerformed(evt));
        jMenu3.add(miSaveAutomaton);
        jMenu3.add(jSeparator4);

        miCloseWindow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.META_MASK));
        miCloseWindow.setText("Close Window");
        miCloseWindow.addActionListener(evt -> miCloseWindowActionPerformed(evt));
        jMenu3.add(miCloseWindow);

        miCloseAllWindows.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miCloseAllWindows.setText("Close All Windows");
        miCloseAllWindows.addActionListener(evt -> miCloseAllWindowsActionPerformed(evt));
        jMenu3.add(miCloseAllWindows);
        jMenu3.add(jSeparator2);

        miQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        miQuit.setText("Quit");
        miQuit.addActionListener(evt -> miQuitActionPerformed(evt));
        jMenu3.add(miQuit);

        jMenuBar2.add(jMenu3);

        jMenu4.setText("Tools");

        miShowLanguage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.META_MASK));
        miShowLanguage.setText("Show Language ...");
        miShowLanguage.addActionListener(evt -> miShowLanguageActionPerformed(evt));
        jMenu4.add(miShowLanguage);

        miParse.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.META_MASK));
        miParse.setText("Parse ...");
        miParse.setEnabled(false);
        miParse.addActionListener(evt -> miParseActionPerformed(evt));
        jMenu4.add(miParse);

        miBulkParse.setText("Bulk Parse ...");
        miBulkParse.setEnabled(false);
        miBulkParse.addActionListener(evt -> miBulkParseActionPerformed(evt));
        jMenu4.add(miBulkParse);

        miBinarize.setText("Binarize ...");
        miBinarize.setEnabled(false);
        miBinarize.addActionListener(evt -> miBinarizeActionPerformed(evt));
        jMenu4.add(miBinarize);
        jMenu4.add(jSeparator3);

        miTrainML.setText("Maximum Likelihood Training ...");
        miTrainML.setEnabled(false);
        miTrainML.addActionListener(evt -> miTrainMLActionPerformed(evt));
        jMenu4.add(miTrainML);

        miTrainEM.setText("EM Training ...");
        miTrainEM.setEnabled(false);
        miTrainEM.addActionListener(evt -> miTrainEMActionPerformed(evt));
        jMenu4.add(miTrainEM);

        miTrainVB.setText("Variational Bayes Training ...");
        miTrainVB.setEnabled(false);
        miTrainVB.addActionListener(evt -> miTrainVBActionPerformed(evt));
        jMenu4.add(miTrainVB);

        miMaxent.setText("Maximum Entropy");
        miMaxent.setEnabled(false);

        miLoadMaxentWeights.setText("Load Weights ...");
        miLoadMaxentWeights.addActionListener(evt -> miLoadMaxentWeightsActionPerformed(evt));
        miMaxent.add(miLoadMaxentWeights);

        miShowMaxentWeights.setText("Show Weights ...");
        miShowMaxentWeights.setEnabled(false);
        miShowMaxentWeights.addActionListener(evt -> miShowMaxentWeightsActionPerformed(evt));
        miMaxent.add(miShowMaxentWeights);

        miTrainMaxent.setText("Train ...");
        miTrainMaxent.addActionListener(evt -> miTrainMaxentActionPerformed(evt));
        miMaxent.add(miTrainMaxent);

        jMenu4.add(miMaxent);
        jMenu4.add(jSeparator5);

        jMenuItem1.setText("Compute decomposition automaton ...");
        jMenuItem1.addActionListener(evt -> jMenuItem1ActionPerformed(evt));
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

    private void performCondensedParsing(JParsingDialog jpd) {

    }


    private void miParseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miParseActionPerformed
        if (irtg != null) {
            

            JParsingDialog jpd = JParsingDialog.create(annotationsInOrder, irtg, this, true);
            jpd.setVisible(true);

            if (jpd.getInputValues() == null) {
                // dialog was cancelled
                return;
            }

            final Map<String, String> inputs = jpd.getInputValues();
            final Map<String, String> options = jpd.getOptionValues();
            final Map<String, Object> inputObjects = new HashMap<>();

            for (String interp : options.keySet()) {
                try {
                        irtg.getInterpretation(interp).getAlgebra().setOptions(jpd.getOptionValues().get(interp));
                } catch (Exception ex) {
                        GuiMain.log("Exception while parsing options for interpretation '" + interp + "': " + ex.toString());
                    return;
                    }
            }
            
            for (String interp : inputs.keySet()) {
                try {
                    inputObjects.put(interp, irtg.parseString(interp, inputs.get(interp)));
                } catch (ParserException ex) {
                    GuiMain.log("Exception while parsing input interpretation '" + interp + "': " + ex.toString());
                    return;
                }
            }

            if (!inputs.isEmpty()) {
                GuiUtils.withProgressBar(this, "Parsing progress", "Parsing ...",
                                         listener -> {

                                     GuiUtils.setGlobalListener(listener);

                                     // filtering requested
                                     if (jpd.getSelectedFiltering() != null) {
                                         if (jpd.getTheOneNonemptyInput() == null) {
                                             GuiMain.log("Filtering is only supported when inputs are given on exactly one interpretation.");
                                             return null;
                                         }

                                         String theInterp = jpd.getTheOneNonemptyInput().getKey();
                                         switch (jpd.getSelectedFiltering()) {
                                             case BASIC:
                                                 irtg = irtg.filterForAppearingConstants(theInterp, inputObjects.get(theInterp));
                                                 break;

                                             case BINARIZED:
                                                 irtg = irtg.filterBinarizedForAppearingConstants(theInterp, inputObjects.get(theInterp));
                                                 break;
                                         }
                                     }

                                     TreeAutomaton chart = null;

                                     if (jpd.getSelectedPruning() == JParsingDialog.Pruning.CTF) {
                                         // coarse-to-fine parsing

                                         if (jpd.getTheOneNonemptyInput() == null) {
                                             GuiMain.log("Coarse-to-fine pruning is only supported when inputs are given on exactly one interpretation.");
                                         } else {
                                             String interp = jpd.getTheOneNonemptyInput().getKey();
                                             String ftc = StringTools.slurp(new FileReader(jpd.getPruningFtcMap()));
                                             CoarseToFineParser ctfp = CoarseToFineParser.makeCoarseToFineParser(irtg, interp, ftc, jpd.getPruningThreshold());

                                             if (jpd.getSelectedAlgorithm() == JParsingDialog.Algorithm.SIBLING_FINDER) {
                                                 chart = ctfp.parseInputObjectWithSF(inputObjects.get(interp));
                                             } else {
                                                 chart = ctfp.parseInputObject(inputObjects.get(interp));
                                             }
                                         }
                                     } else {
                                         // ordinary parsing
                                         PruningPolicy pp = null;

                                         switch (jpd.getSelectedAlgorithm()) {
                                             case DEFAULT:
                                                 if (jpd.getSelectedPruning() != JParsingDialog.Pruning.NONE) {
                                                     GuiMain.log("Pruning policy '" + jpd.getSelectedPruning().toString() + "' selected, but 'default' parsing algorithm does not support pruning yet. Ignored.");
                                                 }

                                                 chart = irtg.parse(inputs);
                                                 break;

                                             case CONDENSED_BOTTOM_UP:
                                                 // create selected pruning policy
                                                 switch (jpd.getSelectedPruning()) {
                                                     case NONE:
                                                         pp = new NoPruningPolicy();
                                                         break;

                                                     case BEAM_SEARCH:
                                                         pp = new QuotientPruningPolicy(new SemiringFOM(DoubleArithmeticSemiring.INSTANCE), jpd.getPruningThreshold());
                                                         break;

                                                     default:
                                                         // ignore CTF, it was handled above
                                                         throw new RuntimeException("this should not happen");
                                                 }

                                                 chart = irtg.parseCondensedWithPruning(inputObjects, pp);
                                                 break;

                                             case SIBLING_FINDER:
                                                 if (jpd.getSelectedPruning() != JParsingDialog.Pruning.NONE) {
                                                     GuiMain.log("Pruning policy '" + jpd.getSelectedPruning().toString() + "' selected, but 'sibling-finder' parsing algorithm does not support pruning yet. Ignored.");
                                                 }

                                                 if (jpd.getTheOneNonemptyInput() == null) {
                                                     GuiMain.log("Parsing with sibling-finder algorithm only supports inputs on a single interpretation.");
                                                     chart = null;
                                                 } else {
                                                     String interp = jpd.getTheOneNonemptyInput().getKey();
                                                     chart = irtg.parseWithSiblingFinder(interp, inputObjects.get(interp));
                                                 }

                                                 break;
                                         }
                                     }

                                     GuiUtils.setGlobalListener(null);
                                     return chart;
                                 },
                                         (chart, time) -> {
                                     if (chart != null) {
                                         GuiMain.log("Computed parse chart, for " + inputs + ", " + Util.formatTime(time));
                                         showChartAfterNullFiltering(chart, inputs, jpd.getSelectedNullFiltering());
                                     }
                                 });
            }
        }
    }//GEN-LAST:event_miParseActionPerformed

    private void showChartAfterNullFiltering(TreeAutomaton chart, Map<String, String> inputs, String nullFiltering) {
        GuiUtils.withProgressBar(this, "Null filtering", "Null filtering", listener -> {
                             if (nullFiltering == null) {
                                 return chart;
                             } else {
                                 // TODO - error handling
                                 // TODO - progress bar

                                 NullFilterAlgebra alg = (NullFilterAlgebra) irtg.getInterpretation(nullFiltering).getAlgebra();
                                 Homomorphism hom = irtg.getInterpretation(nullFiltering).getHomomorphism();
                                 return chart.intersect(alg.nullFilter().inverseHomomorphism(hom));
                             }
                         }, (cht, time) -> {
                             String title = "Parse chart: " + inputs;

                             if (nullFiltering != null) {
                                 GuiMain.log("Performed null filtering on interpretation '" + nullFiltering + " ', " + Util.formatTime(time));
                                 title += " (null-filtered)";
                             }

                             JTreeAutomaton jta = new JTreeAutomaton(cht, null);
                             jta.setIrtg(irtg);
                             jta.setTitle(title);
                             jta.pack();
                             jta.setVisible(true);
                         });
    }

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
            GuiMain.log("Performed ML training, " + Util.formatTimeSince(start));
            updateWeights();
        }
    }//GEN-LAST:event_miTrainMLActionPerformed

    private void miTrainEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainEMActionPerformed
        GuiMain.withLoadedUnannotatedCorpus(irtg, JTreeAutomaton.this, corpus -> {
                                        GuiUtils.withProgressBar(GuiMain.getApplication(), "Training progress", "Performing EM training ...",
                                                                 listener -> {
                                                             irtg.trainEM(corpus, listener);
                                                             return null;
                                                         },
                                                                 (result, time) -> {
                                                             GuiMain.log("Performed EM training, " + Util.formatTime(time));
                                                             updateWeights();
                                                         });
                                    });
    }//GEN-LAST:event_miTrainEMActionPerformed

    private void miTrainVBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainVBActionPerformed
        GuiMain.withLoadedUnannotatedCorpus(irtg, JTreeAutomaton.this, corpus -> {
                                        GuiUtils.withProgressBar(GuiMain.getApplication(), "Training progress", "Performing VB training ...",
                                                                 listener -> {
                                                             irtg.trainVB(corpus, listener);
                                                             return null;
                                                         },
                                                                 (result, time) -> {
                                                             GuiMain.log("Performed VB training, " + Util.formatTime(time));
                                                             updateWeights();
                                                         });
                                    });
    }//GEN-LAST:event_miTrainVBActionPerformed

    private void miLoadMaxentWeightsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miLoadMaxentWeightsActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            long start = System.nanoTime();
            GuiMain.loadMaxentWeights((MaximumEntropyIrtg) irtg, this);
            GuiMain.log("Loaded maxent weights, " + Util.formatTimeSince(start));

            miShowMaxentWeights.setEnabled(true);

            miShowMaxentWeightsActionPerformed(null);
        }
    }//GEN-LAST:event_miLoadMaxentWeightsActionPerformed

    private void miTrainMaxentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miTrainMaxentActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            final Corpus corpus = GuiMain.loadAnnotatedCorpus(irtg, miMaxent);

            GuiUtils.withProgressBar(GuiMain.getApplication(), "Training progress", "Performing Maximum Entropy training ...",
                                     listener -> {
                                 ((MaximumEntropyIrtg) irtg).trainMaxent(corpus, listener);
                                 return null;
                             },
                                     (result, time) -> {
                                 GuiMain.log("Trained maxent model, " + Util.formatTime(time));
                                 miShowMaxentWeights.setEnabled(true);
                                 miShowMaxentWeightsActionPerformed(null);
                             });
        }
    }//GEN-LAST:event_miTrainMaxentActionPerformed

    static class FtWeight {

        public String feature;
        public String weight;
    }

    private void miShowMaxentWeightsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miShowMaxentWeightsActionPerformed
        if (irtg instanceof MaximumEntropyIrtg) {
            MaximumEntropyIrtg mirtg = (MaximumEntropyIrtg) irtg;
            List<FtWeight> fts = new ArrayList<>();
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
                GuiUtils.withProgressBar(JTreeAutomaton.this, "Binarization", "Binarizing IRTG ...",
                                         listener -> {
                                     BkvBinarizer binarizer = new BkvBinarizer(rsc.getSelectedSeeds());
                                     InterpretedTreeAutomaton binarized = binarizer.binarize(irtg, rsc.getSelectedAlgebras(), listener);
                                     return binarized;
                                 },
                                         (binarized, time) -> {
                                     GuiMain.log("Binarized IRTG, " + Util.formatTime(time));

                                     JTreeAutomaton jta = new JTreeAutomaton(binarized.getAutomaton(), new IrtgTreeAutomatonAnnotator(binarized));
                                     jta.setTitle("Binarization of " + getTitle());
                                     jta.setIrtg(binarized);
                                     jta.setParsingEnabled(true);
                                     jta.pack();
                                     jta.setVisible(true);
                                 });
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

    private void miSaveIrtgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveIrtgActionPerformed
        GuiMain.saveIrtg(irtg, this);
    }//GEN-LAST:event_miSaveIrtgActionPerformed

    public static class InputOutputCorpus {

        @DataField(label = "Input corpus")
        public String inputCorpusName;

        @DataField(label = "Output corpus")
        public String outputCorpusName;
    }

    private List<OutputCodec> collectOutputCodecs(Algebra alg) {
        Class classOfValues = alg.getClassOfValues();

        List<OutputCodec> outputCodecs = new ArrayList<>();
        outputCodecs.add(new AlgebraStringRepresentationOutputCodec(alg));
        outputCodecs.addAll(OutputCodec.getOutputCodecs(classOfValues));

        return outputCodecs;
    }

    private DataPanelContainer dataPanelContainerWithInterpretations() {
        DataPanelContainer ret = new DataPanelContainer("Interpretations");

        // derivation tree
        List<OutputCodec> outputCodecs = collectOutputCodecs(new TreeAlgebra());
        ConcreteListDataPanelEntry<OutputCodec> dpeList = new ConcreteListDataPanelEntry<>("derivation tree", "Output codec for derivation tree:", outputCodecs, oc -> oc.getMetadata().name() + ": " + oc.getMetadata().description());

        DataPanelEntry<Boolean> dpeCheck = new ConcreteBooleanDataPanelEntry("derivation tree_use", "Generate output for derivation tree:");
        ret.addEntry(dpeCheck);

        dpeCheck = new ConcreteBooleanDataPanelEntry("_print_comments_", "Print comments:");
        dpeCheck.setValue(true);
        ret.addEntry(dpeCheck);

        dpeCheck = new ConcreteBooleanDataPanelEntry("_print_separators_", "Print empty lines between instances:");
        dpeCheck.setValue(true);
        ret.addEntry(dpeCheck);

        for (String interpName : irtg.getInterpretations().keySet()) {
            Algebra alg = irtg.getInterpretation(interpName).getAlgebra();
            Class classOfValues = alg.getClassOfValues();

            outputCodecs = new ArrayList<>();
            outputCodecs.add(new AlgebraStringRepresentationOutputCodec(alg));
            outputCodecs.addAll(OutputCodec.getOutputCodecs(classOfValues));

            dpeList = new ConcreteListDataPanelEntry<>(interpName, "Output codec for interpretation '" + interpName + "':", outputCodecs, oc -> oc.getMetadata().name() + ": " + oc.getMetadata().description());
            dpeCheck = new ConcreteBooleanDataPanelEntry(interpName + "_use", "Generate output for interpretation '" + interpName + "':");

            ret.addEntry(dpeCheck);
            ret.addEntry(dpeList);
        }

        return ret;
    }

    private InterpretationPrintingPolicy createPolicy(DataPanelContainer corpusOptions) {
        List<Pair<String, OutputCodec>> interpretationToCodec = new ArrayList<>();

        for (String interpName : irtg.getInterpretations().keySet()) {
            if ((Boolean) corpusOptions.getEntry(interpName + "_use").getValue()) {
                ConcreteListDataPanelEntry<OutputCodec> dpeList = (ConcreteListDataPanelEntry<OutputCodec>) corpusOptions.getEntry(interpName);
                OutputCodec oc = dpeList.getSelectedElement();
                Pair<String, OutputCodec> p = new Pair(interpName, oc);
                interpretationToCodec.add(p);
            }
        }

        return new InterpretationPrintingPolicy(interpretationToCodec, new TreeAlgebra());
    }

    private void miBulkParseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miBulkParseActionPerformed
        final InputOutputCorpus corpusNames = new InputOutputCorpus();

        DataDialog.withValues(this, "Bulk Parse",
                              Arrays.asList(
                                      ReflectionEntry.forObject("Corpus files", corpusNames),
                                      dataPanelContainerWithInterpretations()
                              ),
                              (dpc) -> {
                          try {
                              DataPanelContainer codecOptions = dpc.get(1);
                              Corpus inputCorpus = Corpus.readCorpus(new FileReader(corpusNames.inputCorpusName), irtg);
                              FileWriter w = new FileWriter(corpusNames.outputCorpusName);
                              String s = "Parsed from " + corpusNames.inputCorpusName + "\nat " + new Date().toString();

                              String commentPrefix = ((Boolean) codecOptions.getEntry("_print_comments_").getValue()) ? "# " : null;

                              final CorpusWriter cw = new CorpusWriter(irtg, s, commentPrefix, createPolicy(codecOptions), w);
                              cw.setPrintSeparatorLines(((Boolean) codecOptions.getEntry("_print_separators_").getValue()));

                              boolean annotated = (Boolean) (codecOptions.getEntry("derivation tree_use").getValue());
                              cw.setAnnotated(annotated);

                              GuiUtils.withProgressBar(JTreeAutomaton.this, "Parsing progress", "Bulk parsing of input corpus ...",
                                                       listener -> {
                                                   irtg.bulkParse(inputCorpus, cw, listener);
                                                   w.flush();
                                                   w.close();
                                                   return null;
                                               },
                                                       (result, time) -> {
                                                   GuiMain.log("Finished bulk parsing, " + Util.formatTime(time));
                                               });
                          } catch (IOException | CorpusReadingException ex) {
                              GuiUtils.showError(ex);
                          }

//                    System.err.println(dpc.get(1));
                      });
    }//GEN-LAST:event_miBulkParseActionPerformed

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
    private javax.swing.JMenuItem miBulkParse;
    private javax.swing.JMenuItem miCloseAllWindows;
    private javax.swing.JMenuItem miCloseWindow;
    private javax.swing.JMenuItem miLoadMaxentWeights;
    private javax.swing.JMenu miMaxent;
    private javax.swing.JMenuItem miOpenAutomaton;
    private javax.swing.JMenuItem miOpenIrtg;
    private javax.swing.JMenuItem miParse;
    private javax.swing.JMenuItem miQuit;
    private javax.swing.JMenuItem miSaveAutomaton;
    private javax.swing.JMenuItem miSaveIrtg;
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
