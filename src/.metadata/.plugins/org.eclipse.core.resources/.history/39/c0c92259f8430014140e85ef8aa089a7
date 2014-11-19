/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Instance {
    private Map<String, Object> inputObjects;
    private Tree<Integer> derivationTree;        // may be null if corpus was unannotated
    private TreeAutomaton chart;                 // may be null if corpus was unparsed

    public Instance() {
    }

    public Map<String, Object> getInputObjects() {
        return inputObjects;
    }

    public void setInputObjects(Map<String, Object> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public Tree<Integer> getDerivationTree() {
        return derivationTree;
    }

    public void setDerivationTree(Tree<Integer> derivationTree) {
        this.derivationTree = derivationTree;
    }

    public TreeAutomaton getChart() {
        return chart;
    }

    public void setChart(TreeAutomaton chart) {
        this.chart = chart;
    }
    
    public Instance withChart(TreeAutomaton chart) {
        Instance copy = new Instance();
        
        copy.inputObjects = this.inputObjects;
        copy.derivationTree = this.derivationTree;
        
        copy.setChart(chart);
        
        return copy;
    }
    

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer("Instance{" + "inputObjects=" + inputObjects);
        
        if( derivationTree != null ) {
            ret.append(", derivationTree=" + derivationTree);
        }
        
        if( chart != null ) {
            ret.append(", with chart");
        }
        
        ret.append("}");
        return ret.toString();
    }
}
