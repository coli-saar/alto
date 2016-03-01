/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.pruning;

import de.up.ling.irtg.rule_finding.pruning.intersection.IntersectionOptions;
import de.up.ling.irtg.automata.IntersectionAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Arrays;
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
    public IntersectionPruner(Function<TreeAutomaton,TreeAutomaton>[] options) {
        this((TreeAutomaton input) -> {
            TreeAutomaton inter = input;
            
            for(int i=0;i<options.length;++i) {
                inter = new IntersectionAutomaton(inter, options[i].apply(input));
            }
            
            return inter;
        });
    }
    
    /**
     * 
     * @param configurationFreeOptions 
     */
    public IntersectionPruner(IntersectionOptions... configurationFreeOptions) {
        this(makeFunctions(configurationFreeOptions, new String[configurationFreeOptions.length]));
    }

    /**
     * 
     * @param options
     * @param configurations 
     */
    public IntersectionPruner(IntersectionOptions[] options, String[] configurations) {
        this(makeFunctions(options, configurations));
    }
    
    
    public IntersectionPruner(String[] choices, String[] configurations) {       
        this(IntersectionOptions.lookUp(choices),configurations);
    }
    
    /**
     * 
     * @param ios
     * @param configs
     * @return 
     */
    private static Function<TreeAutomaton,TreeAutomaton>[] makeFunctions(IntersectionOptions[] ios, String[] configs) {
        int size = Math.min(ios.length, configs.length);
        
        
        
        Function<TreeAutomaton,TreeAutomaton>[] result = new Function[size];
        for(int i=0;i<size;++i) {
            result[i] = ios[i].getRestrictionFactory(configs[i]);
        }
        
        return result;
    }
    
    @Override
    public TreeAutomaton apply(TreeAutomaton ta) {
        return new IntersectionAutomaton(ta, this.mapToIntersect.apply(ta));
    }    
}
