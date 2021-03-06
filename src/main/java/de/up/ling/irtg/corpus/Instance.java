/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Instance {
    private Map<String, Object> inputObjects;
    private Tree<Integer> derivationTree;        // may be null if corpus was unannotated
    private TreeAutomaton chart;                 // may be null if corpus was unparsed
    private Map<String, String> comments;          // may be null if instance had no comment

    public Instance() {
    }

    public Map<String, Object> getInputObjects() {
        return inputObjects;
    }

    public Map<String, Object> getRestrictedInputObjects(Iterable<String> interpretations) {
        Map<String, Object> ret = new HashMap<>();
        for (String intp : interpretations) {
            ret.put(intp, inputObjects.get(intp));
        }
        return ret;
    }

    public void setInputObjects(Map<String, Object> inputObjects) {
        this.inputObjects = inputObjects;
    }

    public void setAsNull() {
        this.inputObjects = null;
    }

    public boolean isNull() {
        return this.inputObjects == null;
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

    public Map<String, String> getComments() {
        return comments;
    }

    public void setComments(Map<String, String> comments) {
        this.comments = comments;
    }

    public void setComments(String... comments) {
        this.comments = new HashMap<>();
        for (int i = 0; i < comments.length; i += 2) {
            this.comments.put(comments[i], comments[i + 1]);
        }
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(TreeAutomaton auto) {
        StringBuilder ret = new StringBuilder("Instance{" + "inputObjects=" + inputObjects);

        if (derivationTree != null) {
            if (auto == null) {
                ret.append(", derivationTree=" + derivationTree);
            } else {
                ret.append(", derivationTree(rsvd)=" + auto.getSignature().resolve(derivationTree));
            }
        }

        if (chart != null) {
            ret.append(", with chart");
        }

        ret.append("}");
        return ret.toString();
    }
}
