/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A rule in a tree automaton.<p>
 * 
 * A rule consists of a parent state, a label (or terminal symbol),
 * an array of child states, and a weight. The states and labels are
 * represented as int values for efficiency reasons; they can be
 * resolved to human-readable values by looking them up using
 * {@link TreeAutomaton#getStateForId(int) } (for states) or
 * {@link Signature#resolveSymbolId(int) } using the tree
 * automaton's signature (for labels).<p>
 * 
 * In order to create new rule objects, do not instantiate
 * Rules directly. Instead, call {@link TreeAutomaton#createRule(java.lang.Object, java.lang.String, java.util.List) }
 * or one of its variants. This gives the containing {@link TreeAutomaton}
 * a chance to update its internal data structures if necessary.<p>
 * 
 * If it is not specified, the weight is set to 1.0. You can
 * destructively modify a rule's weight; this is useful, for instance,
 * in implementing the EM algorithm. As a consequence, {@link #equals(java.lang.Object) }
 * and {@link #hashCode() } ignore the weight, i.e. a rule remains equals
 * to itself after you change the weight.
 *
 * @author koller
 */
public class Rule implements Serializable {
    private int parent;
    private int label;
    private int[] children;
    private double weight;
    private Object extra;
    private int hashcode = -1;

    Rule(int parent, int label, int[] children, double weight) {
        this.parent = parent;
        this.label = label;
        this.children = children;
        this.weight = weight;
        this.extra = null;
    }
    
    public int[] getChildren() {
        return children;
    }

    public int getLabel() {
        return label;
    }
    
    public String getLabel(TreeAutomaton auto) {
        return auto.getSignature().resolveSymbolId(label);
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
     * Stores auxiliary information within this rule. Do not use this
     * unless you know what you're doing.
     * 
     * @param extra 
     */
    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public int getArity() {
        return children.length;
    }
    
    @Override
    public String toString() {
        return parent + " -> " + label + "/" + Arrays.toString(children);
    }

    public String toString(TreeAutomaton auto) {
        return toString(auto, auto.getFinalStates().contains(parent));
    }

    public String toString(TreeAutomaton auto, boolean parentIsFinal) {
        boolean first = true;
        StringBuilder ret = new StringBuilder(Tree.encodeLabel(auto.getStateForId(parent).toString()) + (parentIsFinal ? "!" : "") + " -> " + Tree.encodeLabel(getLabel(auto)));

        if (children.length > 0) {
            ret.append("(");

            for (int child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append((child == 0) ? "null" : Tree.encodeLabel(auto.getStateForId(child).toString()));
            }

            ret.append(")");
        }

        ret.append(" [" + weight + "]");
        return ret.toString();
    }
    
    public static List<String> rulesToStrings(Collection<Rule> rules, TreeAutomaton auto) {
        List<String> ret = new ArrayList<String>();
        for( Rule rule : rules ) {
            ret.add(rule.toString(auto));
        }
        return ret;
    }
    
    private int computeHashCode() {
        int hash = 7;
        hash = 73 * hash + this.parent;
        hash = 73 * hash + this.label;
        hash = 73 * hash + Arrays.hashCode(this.children);
        return hash;
    }

    @Override
    public int hashCode() {
        if( hashcode == -1 ) {
            hashcode = computeHashCode();
        }
        
        return hashcode;
    }

    /**
     * Compares two rules for equality. Rule weights are ignored
     * in the comparison. Notice that this implementation of equals
     * is only meaningful if the two rules belong to the same
     * automaton, as otherwise states might be encoded by different
     * interners.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule other = (Rule) obj;
        if (this.parent != other.parent) {
            return false;
        }
        if (this.label != other.label) {
            return false;
        }
        if (!Arrays.equals(this.children, other.children)) {
            return false;
        }
        return true;
    }
    
    

    public static Collection<Integer> extractParentStates(Collection<Rule> rules) {
        IntList ret = new IntArrayList();
        
        for (Rule rule : rules) {
            ret.add(rule.getParent());
        }
        
        return ret;
    }
    
    public boolean isLoop() {
        return getArity() == 1 && children[0] == parent;
    }
}
