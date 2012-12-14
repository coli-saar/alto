/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.saar.basic.StringTools;
import de.up.ling.tree.Tree;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Caution: When comparing rules with equals and hashCode, the rule weight is
 * NOT taken into account. This is so rules can be destructively reweighted in
 * training.
 *
 * @author koller
 */
public class Rule<State> implements Serializable {

    private State parent;
    private String label;
    private State[] children;
    private double weight;

    public Rule(State parent, String label, State[] children, double weight) {
        this.parent = parent;
        this.label = label;
        this.children = children;
        this.weight = weight;
    }

    public Rule(State parent, String label, List<State> children, double weight) {
        this(parent, label, (State[]) children.toArray(), weight);
    }

    public Rule(State parent, String label, State[] children) {
        this(parent, label, children, 1);
    }

    public Rule(State parent, String label, List<State> children) {
        this(parent, label, children, 1);
    }

    public static <State> Rule<State> c(State parent, String label, State... children) {
        return new Rule(parent, label, children);
    }

    public State[] getChildren() {
        return children;
    }

    public String getLabel() {
        return label;
    }

    public State getParent() {
        return parent;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getArity() {
        return children.length;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean parentIsFinal) {
        boolean first = true;
        StringBuilder ret = new StringBuilder(Tree.encodeLabel(parent) + (parentIsFinal ? "!" : "") + " -> " + Tree.encodeLabel(getLabel()));

        if (children.length > 0) {
            ret.append("(");

            for (State child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append(Tree.encodeLabel(child));
            }

            ret.append(")");
        }

        ret.append(" [" + weight + "]");
        return ret.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule<State> other = (Rule<State>) obj;
        if (this.parent != other.parent && (this.parent == null || !this.parent.equals(other.parent))) {
            return false;
        }
        if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
            return false;
        }
        if (!Arrays.deepEquals(this.children, other.children)) {
            return false;
        }
//        if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
//            return false;
//        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.parent != null ? this.parent.hashCode() : 0);
        hash = 23 * hash + (this.label != null ? this.label.hashCode() : 0);
        hash = 23 * hash + Arrays.deepHashCode(this.children);
//        hash = 23 * hash + (int) (Double.doubleToLongBits(this.weight) ^ (Double.doubleToLongBits(this.weight) >>> 32));
        return hash;
    }

    public static <State> Collection<State> extractParentStates(Collection<Rule<State>> rules) {
        List<State> ret = new ArrayList<State>();
        for (Rule<State> rule : rules) {
            ret.add(rule.getParent());
        }
        return ret;
    }
}
