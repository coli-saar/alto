/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.WeightedTree;

/**
 * An evaluated item, consisting of a weighted tree and the original
 * unevaluated item from which it was created.
 */
class EvaluatedItem implements Comparable<EvaluatedItem> {
    private UnevaluatedItem item;
    private WeightedTree weightedTree;

    public EvaluatedItem(UnevaluatedItem item, WeightedTree wtree) {
        this.item = item;
        weightedTree = wtree;
    }

    public UnevaluatedItem getItem() {
        return item;
    }

    public WeightedTree getWeightedTree() {
        return weightedTree;
    }

    @Override
    public int compareTo(EvaluatedItem o) {
        // evalItem1 < evalItem2 if the tree in evalItem1 has a HIGHER weight than the tree in evalItem2
        return Double.compare(o.weightedTree.getWeight(), weightedTree.getWeight());
    }

    @Override
    public String toString() {
        return "[" + weightedTree + " (from " + item.toString() + ")]";
    }
    
}
