/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class TreeCombiningItemEvaluator implements ItemEvaluator<Void> {
    @Override
    public EvaluatedItem<Void> evaluate(Rule refinedRule, List<EvaluatedItem<Void>> children, UnevaluatedItem unevaluatedItem) {
        double weight = 1;
        List<Tree<Integer>> childTrees = new ArrayList<>();
        
        for( EvaluatedItem ch : children ) {
            weight *= ch.getWeightedTree().getWeight();
            childTrees.add(ch.getWeightedTree().getTree());
        }
        
        double itemWeight = weight * refinedRule.getWeight();
        WeightedTree wtree = new WeightedTree(Tree.create(refinedRule.getLabel(), childTrees), itemWeight);
        
        return new EvaluatedItem<>(unevaluatedItem, wtree, itemWeight, null);
    }
    
}
