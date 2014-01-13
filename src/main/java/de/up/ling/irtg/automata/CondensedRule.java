/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public class CondensedRule {
    private IntSet labels;
    private int parent;
    private int[] children;
    private double weight;
    private Object extra;
    
    public CondensedRule(int parent, IntSet labels, int[] children, double weight) {
        this.parent = parent;
        this.labels = labels;
        this.children = children;
        this.weight = weight;
    }
    
    public int[] getChildren() {
        return children;
    }

    public IntSet getLabels() {
        return labels;
    }

    public Collection<String> getLabels(TreeAutomaton auto) {
        Set<String> ret = new HashSet<String>();
        for(int label : labels) {
            ret.add(auto.getSignature().resolveSymbolId(label));
        }
        return ret;
    }

    public int getParent() {
        return parent;
    }

    // TODO - is this needed?
    public void setParent(int parent) {
        this.parent = parent;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Retrieves the auxiliary information from this rule.
     *
     * @see #setExtra(java.lang.Object)
     * @return
     */
    public Object getExtra() {
        return extra;
    }

    /**
     * Stores auxiliary information within this rule. Do not use this unless you
     * know what you're doing.
     *
     * @param extra
     */
    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public int getArity() {
        return children.length;
    }
}
