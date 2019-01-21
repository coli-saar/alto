/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.language_iteration;

import de.up.ling.irtg.automata.Rule;
import java.util.List;

/**
 *
 * @author koller
 */
public interface RuleRefiner {
    List<Rule> refine(Rule rule);
}
