/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding;

import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author christoph_teichmann
 */
public class SubtreeIterator implements Iterator<IntArrayList> {
    
    /**
     * 
     */
    private final List<Tree<Rule>> toDo = new ObjectArrayList<>();
    
    /**
     * 
     */
    private int pos = 0;
    
    /**
     * 
     */
    private final IntArrayList ret;
    
    /**
     * 
     */
    private final HomomorphismManager hm;
    
    /**
     * 
     * @param input 
     * @param hm 
     */
    public SubtreeIterator(Tree<Rule> input, HomomorphismManager hm){
        toDo.add(input);
        ret = new IntArrayList();
        
        this.hm = hm;
    }
    
    @Override
    public boolean hasNext() {
        return this.pos < this.toDo.size();
    }
    
    @Override
    public IntArrayList next() {
        if(pos >= this.toDo.size()){
            throw new NoSuchElementException();
        }
        
        this.ret.clear();
        
        Tree<Rule> tr = this.toDo.get(pos++);
        
        add(ret,tr);
        
        return ret;
    }

    /**
     * 
     * @param container
     * @param tr
     * @param addChildren
     * @param hom 
     */
    private void add(IntArrayList container, Tree<Rule> tr) {
        int label = tr.getLabel().getLabel();
        
        container.add(label);
        if(hm.isVariable(label)){
            this.toDo.addAll(tr.getChildren());
            
            return;
        }
        
        for(int i=0;i<tr.getChildren().size();++i){
            this.add(container, tr.getChildren().get(i));
        }
    }
}
