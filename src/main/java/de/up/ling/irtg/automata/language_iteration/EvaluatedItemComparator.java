/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.language_iteration;

import java.util.Comparator;

/**
 *
 * @author koller
 */
public class EvaluatedItemComparator implements Comparator<EvaluatedItem> {
    public static EvaluatedItemComparator INSTANCE = new EvaluatedItemComparator();

        @Override
        public int compare(EvaluatedItem w1, EvaluatedItem w2) {
            // streams that can't deliver values right now are dispreferred (= get minimum weight)
            double weight1 = (w1 == null) ? Double.NEGATIVE_INFINITY : w1.getItemWeight();
            double weight2 = (w2 == null) ? Double.NEGATIVE_INFINITY : w2.getItemWeight();

            // sort descending, i.e. streams with high weights go at the beginning of the list
            return Double.compare(weight2, weight1);
        }
}
