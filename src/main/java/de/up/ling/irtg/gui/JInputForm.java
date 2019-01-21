/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 *
 * @author koller
 */
public class JInputForm extends javax.swing.JDialog {
    private Map<String, String> inputValues = null;
    private Map<String, String> optionValues = null;
    private Map<String, JTextField> inputFields;
    private Map<String, JTextField> optionFields;

    public static void main(String[] args) {
        List<String> interps = new ArrayList<>();
        interps.add("string");
        interps.add("tree");

        List<Boolean> hasOptions = new ArrayList<>();
        hasOptions.add(false);
        hasOptions.add(true);

        JFrame f = new JFrame("hallo");
        JInputForm jif = showForm(f, interps, hasOptions);
        jif.setVisible(true);

//        System.out.println(jif.getInputValues());
//        System.out.println(jif.getOptions());

        System.exit(0);
    }

    /**
     * Creates new form JInputFormNew
     */
    private JInputForm(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        
        getRootPane().setDefaultButton(okButton);
        
        setTitle("Enter inputs");
    }

    public Map<String, String> getInputValues() {
        return inputValues;
    }

    public Map<String, String> getOptions() {
        return optionValues;
    }

    public static JInputForm showForm(java.awt.Frame parent, List<String> interpretations, List<Boolean> hasOptions) {
        JInputForm jif = new JInputForm(parent, true);
        
        jif.optionsContainerPanel.setVisible(false);
        for( boolean opt : hasOptions ) {
            if( opt ) {
                jif.optionsContainerPanel.setVisible(true);
            }
        }

        // create fields for input values
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;

        GridBagLayout gridbag = new GridBagLayout();
        jif.inputPanel.setLayout(gridbag);

        jif.inputFields = new HashMap<>();

        int nextY = 0;

        c.anchor = GridBagConstraints.FIRST_LINE_START;

        for (String intp : interpretations) {
            c.gridy = nextY;

            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 0, 0);
            JLabel jl = new JLabel("Input on interpretation " + intp + ":");
            gridbag.setConstraints(jl, c);
            jif.inputPanel.add(jl);

            nextY++;
            c.gridy = nextY;

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 10, 0);

            JTextField comp = new JTextField(40);
            gridbag.setConstraints(comp, c);
            jif.inputPanel.add(comp);
            jif.inputFields.put(intp, comp);

            nextY++;
        }

        jif.inputPanel.revalidate();

        // create fields for options
        // create fields for input values
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;

        gridbag = new GridBagLayout();
        jif.optionsPanel.setLayout(gridbag);

        jif.optionFields = new HashMap<>();

        nextY = 0;

        c.anchor = GridBagConstraints.FIRST_LINE_START;

        for (int i = 0; i < interpretations.size(); i++) {
            String intp = interpretations.get(i);
            boolean opts = hasOptions.get(i);

            if (opts) {
                c.gridy = nextY;

                c.fill = GridBagConstraints.NONE;
                c.gridx = 0;
                c.weightx = 0;
                c.insets = new Insets(0, 0, 0, 0);
                JLabel jl = new JLabel("Options for interpretation " + intp + ":");
                gridbag.setConstraints(jl, c);
                jif.optionsPanel.add(jl);

                nextY++;
                c.gridy = nextY;

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.weightx = 1;
                c.insets = new Insets(0, 0, 10, 0);

                JTextField comp = new JTextField(40);
                gridbag.setConstraints(comp, c);
                jif.optionsPanel.add(comp);
                jif.optionFields.put(intp, comp);

                nextY++;
            }
        }

        jif.optionsPanel.revalidate();

        jif.pack();

        return jif;
    }

    public static Map<String, String> getValues(List<String> interpretations, Frame parent) {
        JInputForm jif = new JInputForm(parent, true);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;

        GridBagLayout gridbag = new GridBagLayout();
        jif.inputPanel.setLayout(gridbag);

        jif.inputFields = new HashMap<>();

        int nextY = 0;

        c.anchor = GridBagConstraints.FIRST_LINE_START;

        for (String intp : interpretations) {
            c.gridy = nextY;

            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 0, 0);
            JLabel jl = new JLabel("Input on interpretation " + intp + ":");
            gridbag.setConstraints(jl, c);
            jif.inputPanel.add(jl);

            nextY++;
            c.gridy = nextY;

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 10, 0);

            JTextField comp = new JTextField(40);
            gridbag.setConstraints(comp, c);
            jif.inputPanel.add(comp);
            jif.inputFields.put(intp, comp);

            nextY++;
        }

        jif.inputPanel.revalidate();
        jif.pack();
        jif.setVisible(true);

        return jif.inputValues;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        inputPanel = new javax.swing.JPanel();
        optionsContainerPanel = new javax.swing.JPanel();
        optionsPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Inputs"));

        inputPanel.setLayout(new java.awt.GridBagLayout());

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(inputPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(inputPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );

        optionsContainerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));

        org.jdesktop.layout.GroupLayout optionsPanelLayout = new org.jdesktop.layout.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout optionsContainerPanelLayout = new org.jdesktop.layout.GroupLayout(optionsContainerPanel);
        optionsContainerPanel.setLayout(optionsContainerPanelLayout);
        optionsContainerPanelLayout.setHorizontalGroup(
            optionsContainerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, optionsContainerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(optionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        optionsContainerPanelLayout.setVerticalGroup(
            optionsContainerPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(optionsContainerPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(optionsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        okButton.setText("OK");
        okButton.addActionListener(evt -> okButtonActionPerformed(evt));

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(evt -> cancelButtonActionPerformed(evt));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, optionsContainerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(okButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)
                        .add(0, 370, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(optionsContainerPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(cancelButton)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(okButton)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        inputValues = new HashMap<>();
        optionValues = new HashMap<>();

        for (String intp : inputFields.keySet()) {
            if (!"".equals(inputFields.get(intp).getText())) {
                inputValues.put(intp, inputFields.get(intp).getText());
            }
            
            if( optionFields.containsKey(intp) ) {
                optionValues.put(intp, optionFields.get(intp).getText());
            }
        }

        setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        inputValues = null;
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel inputPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel optionsContainerPanel;
    private javax.swing.JPanel optionsPanel;
    // End of variables declaration//GEN-END:variables
}
