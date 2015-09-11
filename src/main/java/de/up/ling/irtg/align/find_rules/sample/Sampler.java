/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sample;

import com.google.common.base.Function;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class Sampler {

    
    public List<Tree<Rule>> sample(TreeAutomaton t1, TreeAutomaton t2, ModelContainer mc){
        
        Tree<String> t = null;
       
        
        
        return null;
    }
    
    
    /**
     * 
     */
    public static class ModelContainer{
        
        public TreeAutomaton leftModel;
        
        public TreeAutomaton rightModel;
        
        public TreeAutomaton sharedModel;
        
        public Function<Rule,Integer> mapToLeftLabel;
        
        public Function<Rule,Integer> mapToRightlabel;
        
        public Function<Rule,Integer> mapToSharedLabel;
        
        
    }
}