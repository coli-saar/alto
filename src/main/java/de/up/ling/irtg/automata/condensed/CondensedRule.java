/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.AbstractRule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author gontrum
 */
public class CondensedRule implements AbstractRule {

    private final int labelSetID;
    private final int parent;
    private final int[] children;
    private double weight;
    private Object extra;

    public CondensedRule(int parent, int labelSetID, int[] children, double weight) {
        this.parent = parent;
        this.labelSetID = labelSetID;
        this.children = children;
        this.weight = weight;
    }

    public int[] getChildren() {
        return children;
    }

    public int getLabelSetID() {
        return labelSetID;
    }

    public IntSet getLabels(CondensedTreeAutomaton auto) {
        return auto.getLabelsForID(labelSetID);
    }

    /**
     * Returns a Set of Strings for the labels of this rule.
     *
     */
    public Collection<String> getLabelStrings(CondensedTreeAutomaton auto) {
        Set<String> ret = new HashSet<>();
        for (int label : getLabels(auto)) {
            ret.add(auto.getSignature().resolveSymbolId(label));
        }
        return ret;
    }

    public int getParent() {
        return parent;
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
     */
    public Object getExtra() {
        return extra;
    }

    /**
     * Stores auxiliary information within this rule. Do not use this unless you
     * know what you're doing.
     *
     */
    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public int getArity() {
        return children.length;
    }

    @Override
    public String toString() {
        boolean first = true;
        StringBuilder ret = new StringBuilder(parent + " -> %" + labelSetID + "%");
        if (children.length > 0) {
            ret.append("(");

            for (int child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append(child);
            }

            ret.append(")");
        }

        ret.append(" [").append(weight).append("]");
        return ret.toString();
    }

    public String toString(CondensedTreeAutomaton auto) {
        return toString(auto, auto.getFinalStates().contains(parent), s -> true);
    }

    public String toString(CondensedTreeAutomaton auto, Predicate<String> symbolFilter) {
        return toString(auto, auto.getFinalStates().contains(parent), symbolFilter);
    }

    public String toString(CondensedTreeAutomaton auto, boolean parentIsFinal, Predicate<String> symbolFilter) {
        boolean first = true;
        StringBuilder ret = new StringBuilder(Tree.encodeLabel(auto.getStateForId(parent).toString()) + (parentIsFinal ? "!" : "") + " -> {");

        // encode label set
        boolean skippedLabel = false;
        boolean skippingEnabled = getLabelStrings(auto).size() > 4;
        for (String label : getLabelStrings(auto)) {
            if( skippingEnabled ) {
                if( symbolFilter.test(label)) {
                    ret.append(label).append(",");
                } else {
                    skippedLabel = true;
                }
            } else {
                ret.append(label).append(",");
            }
        }

        if (!getLabelStrings(auto).isEmpty()) {
            ret.deleteCharAt(ret.length() - 1);
        }

        if (skippedLabel) {
            ret.append(" ...");
        }

        ret.append("}");

        // encode children
        if (children.length > 0) {
            ret.append("(");

            for (int child : children) {
                String childStr = (child == 0) ? "null" : Tree.encodeLabel(auto.getStateForId(child).toString());

                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append(childStr);
            }

            ret.append(")");
        }

        ret.append(" [").append(weight).append("]");
        return ret.toString();
    }

    private int hashcode = -1;

    private int computeHashCode() {
        int hash = 7;
        hash = 73 * hash + this.parent;
        hash = 73 * hash + this.labelSetID;
        hash = 73 * hash + Arrays.hashCode(this.children);
        return hash;
    }

    @Override
    public int hashCode() {
        if (hashcode == -1) {
            hashcode = computeHashCode();
        }

        return hashcode;
    }

    /**
     * Compares two rules for equality. Rule weights are ignored in the
     * comparison. Notice that this implementation of equals is only meaningful
     * if the two rules belong to the same automaton, as otherwise states might
     * be encoded by different interners.
     *
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CondensedRule other = (CondensedRule) obj;
        if (this.parent != other.parent) {
            return false;
        }
        if (this.labelSetID != other.labelSetID) {
            return false;
        }
        return Arrays.equals(this.children, other.children);
    }

    @Override
    public boolean isLoop() {
        for (int child : children) {
            if (child == parent) {
                return true;
            }
        }
        return false;
    }

}
