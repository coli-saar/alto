// Copyright 2020 Arne Köhn
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.semiring.Semiring;
import de.up.ling.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * An evaluator that uses the multiply operation of the given semiring
 * and Semiring.one() as the starting point.
 */
public class SemiringItemEvaluator implements ItemEvaluator {

    private Semiring<Double> semiring;

    public SemiringItemEvaluator(Semiring<Double> semiring) {
        this.semiring = semiring;
    }

    @Override
    public EvaluatedItem evaluate(Rule refinedRule, List<EvaluatedItem> children, UnevaluatedItem unevaluatedItem) {
        double weight = semiring.one();
        List<Tree<Integer>> childTrees = new ArrayList<>();

        for( EvaluatedItem ch : children ) {
            weight = semiring.multiply(weight, ch.getWeightedTree().getWeight());
            childTrees.add(ch.getWeightedTree().getTree());
        }

        double itemWeight = semiring.multiply(weight, refinedRule.getWeight());
        WeightedTree wtree = new WeightedTree(Tree.create(refinedRule.getLabel(), childTrees), itemWeight);

        return new EvaluatedItem(unevaluatedItem, wtree, itemWeight);
    }
}
