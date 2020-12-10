package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import java.util.List;

/**
 *
 * @author koller
 */
public interface ItemEvaluator {
    EvaluatedItem evaluate(Rule refinedRule, List<EvaluatedItem> children, UnevaluatedItem unevaluatedItem);
}
