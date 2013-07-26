/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

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
    private int label;
    private State[] children;
    private double weight;

    Rule(State parent, int label, State[] children, double weight) {
        this.parent = parent;
        this.label = label;
        this.children = children;
        this.weight = weight;
    }

    Rule(State parent, int label, List<State> children, double weight) {
        this(parent, label, (State[]) children.toArray(), weight);
    }

    /*
    public Rule(State parent, int label, State[] children) {
        this(parent, label, children, 1);
    }

    public Rule(State parent, int label, List<State> children) {
        this(parent, label, children, 1);
    }

    public static <State> Rule<State> c(State parent, int label, State... children) {
        return new Rule(parent, label, children);
    }
    */

    public State[] getChildren() {
        return children;
    }

    public int getLabel() {
        return label;
    }
    
    public String getLabel(TreeAutomaton auto) {
        return auto.getSignature().resolveSymbolId(label);
    }

    public State getParent() {
        return parent;
    }

    public void setParent(State parent) {
        this.parent = parent;
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

    public String toString(TreeAutomaton auto) {
        return toString(auto, auto.getFinalStates().contains(parent));
    }

    public String toString(TreeAutomaton auto, boolean parentIsFinal) {
        boolean first = true;
        StringBuilder ret = new StringBuilder(Tree.encodeLabel(parent.toString()) + (parentIsFinal ? "!" : "") + " -> " + Tree.encodeLabel(getLabel(auto)));

        if (children.length > 0) {
            ret.append("(");

            for (State child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append((child == null) ? "null" : Tree.encodeLabel(child.toString()));
            }

            ret.append(")");
        }

        ret.append(" [" + weight + "]");
        return ret.toString();
    }
    
    public static <E>  List<String> rulesToStrings(Collection<Rule<E>> rules, TreeAutomaton<E> auto) {
        List<String> ret = new ArrayList<String>();
        for( Rule rule : rules ) {
            ret.add(rule.toString(auto));
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.parent != null ? this.parent.hashCode() : 0);
        hash = 59 * hash + this.label;
        hash = 59 * hash + Arrays.deepHashCode(this.children);
        return hash;
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
        if (this.label != other.label) {
            return false;
        }
        if (!Arrays.deepEquals(this.children, other.children)) {
            return false;
        }
        return true;
    }
    
    /*
    public boolean equals(Rule<State> other, int[] labelRemap) {
        if (this.parent != other.parent && (this.parent == null || !this.parent.equals(other.parent))) {
            return false;
        }
        if (labelRemap[this.label] != other.label) {
            return false;
        }
        if (!Arrays.deepEquals(this.children, other.children)) {
            return false;
        }
        return true;
    }
    */

    public static <State> Collection<State> extractParentStates(Collection<Rule<State>> rules) {
        List<State> ret = new ArrayList<State>();
        for (Rule<State> rule : rules) {
            ret.add(rule.getParent());
        }
        return ret;
    }
}
