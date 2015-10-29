/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public interface RuleTreeConverter<Type> {
    
    /**
     * 
     * @param input
     * @return 
     */
    public Type convert(List<Tree<Rule>> input);    
    
}
