/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.learning;

import de.up.ling.irtg.rule_finding.Variables;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class StringSubtreeIterator {
    
    /**
     * Iterator MUST always return root first.
     * 
     * @param sub
     * @param vm
     * @return 
     */
    public static Iterator<Tree<String>> getSubtrees(Tree<String> sub, VariableMapping vm){
        return new Core(sub, vm);
    }
    
    /**
     * 
     */
    private static class Core implements Iterator<Tree<String>>{
        /**
         * 
         */
        private final ArrayList<Tree<String>> toDo = new ArrayList<>();
        
        /**
         * 
         */
        private final ArrayList<String> labels = new ArrayList<>();
        
        /**
         * 
         */
        private int position;

        /**
         * 
         */
        private final VariableMapping vm;
        
        /**
         * 
         */
        private final Tree<String> whole;
        
        /**
         * 
         * @param whole
         * @param vm 
         */
        public Core(Tree<String> whole, VariableMapping vm) {
            position = 0;
            this.vm = vm;
            
            while(Variables.isVariable(whole.getLabel())){
                whole = whole.getChildren().get(0);
            }
            this.whole = whole;
            
            this.toDo.add(whole);
            this.labels.add(vm.getRoot(whole));
        }
        
        @Override
        public boolean hasNext() {
            return position < toDo.size();
        }

        @Override
        public Tree<String> next() {
            Tree<String> t = toDo.get(position);
            String label = this.labels.get(position);
            ++position;
            
            return Tree.create(label, buildTree(t,vm));
        }

        /**
         * 
         * @param t
         * @param label
         * @param vm
         * @return 
         */
        private Tree<String> buildTree(Tree<String> t, VariableMapping vm) {
            if(Variables.isVariable(t.getLabel())){
                String label = this.vm.get(t, whole);
                this.toDo.add(t.getChildren().get(0));
                this.labels.add(label);
                
                return Tree.create(label);
            }else{
                String label = t.getLabel();
                List<Tree<String>> children = new ArrayList<>();
                
                for(int i=0;i<t.getChildren().size();++i){
                    Tree<String> child = t.getChildren().get(i);
                    
                    children.add(this.buildTree(child, vm));
                }
                
                return Tree.create(label, children);
            }
        }       
    }
    
    /**
     * 
     */
    public static interface VariableMapping{
        /**
         * 
         * @param whole
         * @return 
         */
        public String getRoot(Tree<String> whole);
        
        /**
         * 
         * @param child
         * @param whole
         * @return 
         */
        public String get(Tree<String> child, Tree<String> whole);
    }
}
