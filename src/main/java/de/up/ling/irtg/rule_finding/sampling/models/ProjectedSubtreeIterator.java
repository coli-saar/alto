/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.models;

import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 *
 * @author christoph_teichmann
 * @param <Type>
 */
public class ProjectedSubtreeIterator<Type> implements Iterator<IntList> {
    /**
     * 
     */
    private final List<Tree<Integer>> toDo;
    
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
     * @param map
     * @param isCutPoint  
     */
    public ProjectedSubtreeIterator(Tree<Type> basis, Function<Tree<Type>,Tree<Integer>> map,
                                        IntPredicate isCutPoint) {
        this.pos = 0;
        
        construction = new IntArrayList();
        toDo = new ArrayList<>();
        
        Tree<Integer> ti = map.apply(basis);
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
        
        Tree<Integer> todo = this.toDo.get(pos++);
        visit(todo);
        
        return this.construction;
    }
    
    /**
     * 
     * @param portion 
     */
    private void visit(Tree<Integer> portion) {
        int label = portion.getLabel();
        this.construction.add(label);
        
        if(this.isCutPoint.test(label)) {
            this.toDo.add(portion.getChildren().get(0));
        } else {            
            List<Tree<Integer>> children = portion.getChildren();
            for(int i=0;i<children.size();++i) {
                visit(children.get(i));
            }
        }
    }
}
