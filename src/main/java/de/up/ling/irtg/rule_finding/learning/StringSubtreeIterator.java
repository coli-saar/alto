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
     * @return 
     */
    public static Iterator<Tree<String>> getSubtrees(Tree<String> sub){
        return new Core(sub);
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
        private int position;
        
        /**
         * 
         */
        private final Tree<String> whole;
        
        /**
         * 
         * @param whole
         * @param vm 
         */
        public Core(Tree<String> whole) {
            position = 0;
            
            this.whole = whole;
            
            this.toDo.add(whole);
        }
        
        @Override
        public boolean hasNext() {
            return position < toDo.size();
        }

        @Override
        public Tree<String> next() {
            Tree<String> t = toDo.get(position);
            ++position;
            
            String label = t.getLabel();
            
            return Tree.create(label, buildTree(t.getChildren().get(0)));
        }

        /**
         * 
         * @param t
         * @param label
         * @param vm
         * @return 
         */
        private Tree<String> buildTree(Tree<String> t) {
            if(Variables.isVariable(t.getLabel())){
                String label = t.getLabel();
                this.toDo.add(t);
                
                return Tree.create(label);
            }else{
                String label = t.getLabel();
                List<Tree<String>> children = new ArrayList<>();
                
                for(int i=0;i<t.getChildren().size();++i){
                    Tree<String> child = t.getChildren().get(i);
                    
                    children.add(this.buildTree(child));
                }
                
                return Tree.create(label, children);
            }
        }       
    }
}
