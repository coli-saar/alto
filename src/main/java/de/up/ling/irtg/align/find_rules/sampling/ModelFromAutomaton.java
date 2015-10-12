/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import com.google.common.base.Function;
import de.up.ling.irtg.align.HomomorphismManager;
import de.up.ling.irtg.align.find_rules.DefaultVariableMapping;
import de.up.ling.irtg.align.find_rules.TreeAddingAutomaton;
import de.up.ling.irtg.align.find_rules.VariableIndicationByLookUp;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;

/**
 *
 * @author christoph_teichmann
 */
public class ModelFromAutomaton implements Model {
    
    /**
     * 
     */
    private final TreeAddingAutomaton taa;

    /**
     * 
     */
    private final Function<Rule,Integer> func;
    
    /**
     * 
     */
    boolean modified = false;
    
    /**
     * 
     * @param hm
     */
    public ModelFromAutomaton(HomomorphismManager hm) {       
        this.taa = new TreeAddingAutomaton(hm.getSignature(), new VariableIndicationByLookUp(hm));
        this.func = new DefaultVariableMapping(hm);
    }
    
    @Override
    public double getLogWeight(Tree<Rule> t) {
        if(modified){
            modified = false;
            this.taa.normalizeStart();
        }
        
        return this.taa.getLogWeightRaw(t.map(func));
    }

    @Override
    public void add(Tree<Rule> t, double amount) {
        modified = true;
        taa.addVariableTree(t.map(func), amount);
    }
}
