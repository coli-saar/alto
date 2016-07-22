/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 *
 * @author christoph_teichmann
 */
public class SubtreeIterator implements Iterator<IntList> {
    /**
     * 
     */
    private final List<Tree<Rule>> toDo;
    
    /**
     * 
     */
    private int pos;
    
    /**
     * 
     */
    private final IntArrayList construction;

    /**
     * 
     */
    private final IntPredicate isCutPoint;
    
    /**
     * 
     * @param basis
     * @param isCutPoint  
     */
    public SubtreeIterator(Tree<Rule> basis, IntPredicate isCutPoint) {
        this.pos = 0;
        
        construction = new IntArrayList();
        toDo = new ArrayList<>();
        
        Tree<Rule> ti = basis;
        toDo.add(ti);
        
        this.isCutPoint = isCutPoint;
    }
    
    @Override
    public boolean hasNext() {
        return pos < this.toDo.size();
    }

    @Override
    public IntArrayList next() {
        this.construction.clear();
        
        Tree<Rule> todo = this.toDo.get(pos++);
        visit(todo, true);
        
        return this.construction;
    }
    
    /**
     * 
     * @param portion
     * @param start 
     */
    private void visit(Tree<Rule> portion, boolean start) {
        int label = portion.getLabel().getLabel();
        this.construction.add(label);
        
        if(!start && this.isCutPoint.test(label)) {
            this.toDo.add(portion);
        } else {            
            List<Tree<Rule>> children = portion.getChildren();
            for(int i=0;i<children.size();++i) {
                visit(children.get(i),false);
            }
        }
    }
}
