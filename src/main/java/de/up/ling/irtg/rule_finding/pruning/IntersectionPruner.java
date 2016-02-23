/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TopDownIntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.function.Function;

/**
 * 
 * @author christoph_teichmann
 */
public class IntersectionPruner implements Pruner {

    /**
     * 
     */
    private final Function<TreeAutomaton,TreeAutomaton> mapToIntersect;

    /**
     * 
     * @param mapToIntersect 
     */
    public IntersectionPruner(Function<TreeAutomaton, TreeAutomaton> mapToIntersect) {
        this.mapToIntersect = mapToIntersect;
    }
    
    /**
     * 
     * @param options 
     */
    public IntersectionPruner(IntersectionOptions... options) {
        this((TreeAutomaton input) -> {
            TreeAutomaton inter = input;
            
            for(int i=0;i<options.length;++i) {
                inter = new TopDownIntersectionAutomaton(inter, options[i].getAutomaton(input));
            }
            
            return inter;
        });
    }

    @Override
    public TreeAutomaton apply(TreeAutomaton ta) {
        return new TopDownIntersectionAutomaton(ta, this.mapToIntersect.apply(ta));
    }    
}
