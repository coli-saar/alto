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
public class TreeCombiningItemEvaluator implements ItemEvaluator {
    @Override
    public EvaluatedItem evaluate(Rule refinedRule, List<EvaluatedItem> children, UnevaluatedItem unevaluatedItem) {
        double weight = 1;
        List<Tree<Integer>> childTrees = new ArrayList<>();
        
        for( EvaluatedItem ch : children ) {
            weight *= ch.getWeightedTree().getWeight();
            childTrees.add(ch.getWeightedTree().getTree());
        }
        
        double itemWeight = weight * refinedRule.getWeight();
        WeightedTree wtree = new WeightedTree(Tree.create(refinedRule.getLabel(), childTrees), itemWeight);
        
        return new EvaluatedItem(unevaluatedItem, wtree, itemWeight);
    }
    
}
