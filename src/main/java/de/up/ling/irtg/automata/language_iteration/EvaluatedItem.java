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
public class EvaluatedItem<Q> implements Comparable<EvaluatedItem<Q>> {
    private UnevaluatedItem item;        // unevaluated item from which it was produced
    private WeightedTree weightedTree;   // tree it represents, with weight of that tree
    private double itemWeight;           // weight to be used in ordering the priority queue -- need not be the same as weight of the tree
    private Q annotation;

    public EvaluatedItem(UnevaluatedItem item, WeightedTree wtree, double itemWeight, Q annotation) {
        this.item = item;
        weightedTree = wtree;
        this.itemWeight = itemWeight;
        this.annotation = annotation;
    }

    public UnevaluatedItem getItem() {
        return item;
    }

    public WeightedTree getWeightedTree() {
        return weightedTree;
    }

    public double getItemWeight() {
        return itemWeight;
    }

    public Q getAnnotation() {
        return annotation;
    }
    
    

    @Override
    public int compareTo(EvaluatedItem o) {
        // evalItem1 < evalItem2 if the tree in evalItem1 has a HIGHER weight than the tree in evalItem2
//        return Double.compare(o.weightedTree.getWeight(), weightedTree.getWeight());
        return Double.compare(o.itemWeight, itemWeight);
    }

    @Override
    public String toString() {
        return "[" + weightedTree + " (from " + item.toString() + ")]";
    }
    
}
