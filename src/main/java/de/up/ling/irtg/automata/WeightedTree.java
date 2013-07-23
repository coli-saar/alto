/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.tree.Tree;

/**
 *
 * @author koller
 */
public class WeightedTree implements Comparable<WeightedTree> {
    private Tree<Integer> tree;
    private double weight;

    public WeightedTree(Tree<Integer> tree, double weight) {
        this.tree = tree;
        this.weight = weight;
    }

    public Tree<Integer> getTree() {
        return tree;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public int compareTo(WeightedTree o) {
        return Double.compare(weight, o.weight);
    }

    @Override
    public String toString() {
        return tree.toString() + ":" + weight;
    }
}
