/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author christoph_teichmann
 */
public class MinimalTreeAlgebra extends Algebra<Tree<String>> {
    /**
     * 
     */
    public final static String RIGHT_INTO_LEFT = "__RIGHT__INTO__LEFT__";
    
    
    public final static String LEFT_INTO_RIGHT  = "__LEFT__INTO__RIGHT__";
    
    /**
     * 
     */
    public MinimalTreeAlgebra(){
        this.getSignature().addSymbol(RIGHT_INTO_LEFT, 2);
        this.getSignature().addSymbol(LEFT_INTO_RIGHT, 2);
    }
    
    @Override
    protected Tree<String> evaluate(String label, List<Tree<String>> childrenValues) {
        switch(label){
            case RIGHT_INTO_LEFT:
                if (childrenValues.size() == 2) {
                    Tree<String> left = childrenValues.get(0);
                    Tree<String> right = childrenValues.get(1);

                    left.getChildren().add(right);

                    return left;
                } else {
                    return null;
                }
            case LEFT_INTO_RIGHT:
                if (childrenValues.size() == 2) {
                    Tree<String> left = childrenValues.get(0);
                    Tree<String> right = childrenValues.get(1);

                    right.getChildren().add(left);

                    return left;
                } else {
                    return null;
                }
            default:
                if(childrenValues.isEmpty()){
                    this.getSignature().addSymbol(label, 0);
                    return Tree.create(label);
                }else{
                    return null;
                }
        }
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        try {
            Tree<String> t = TreeParser.parse(representation);
            addSymbols(t);
            return t;
        } catch (Exception ex) {
            Logger.getLogger(MinimalTreeAlgebra.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        addSymbols(value);
        return new MinimalTreeDecomposition(this.getSignature(), value);
    }

    /**
     * 
     * @param t 
     */
    private void addSymbols(Tree<String> t) {
        this.getSignature().addSymbol(t.getLabel(), 0);
        
        for(Tree<String> q : t.getChildren()){
            addSymbols(q);
        }
    }
    
    /**
     * 
     */
    private static class MinimalTreeDecomposition extends ConcreteTreeAutomaton<String>{
        /**
         * 
         */
        public MinimalTreeDecomposition(Signature sig, Tree<String> basis) {
            super(sig);
            
            IntArrayList ial = new IntArrayList();
            ial.add(0);
            
            Set<String> done = new ObjectOpenHashSet<>();
            String s = makeRules(ial,0,basis.getChildren().size(), basis, done);
            this.addFinalState(this.getIdForState(s));
        }

        /**
         * 
         * @param ial
         * @param start
         * @param end
         * @param input
         * @param done
         * @return 
         */
        private String makeRules(IntArrayList ial, int start, int end, Tree<String> input,  Set<String> done) {
            String state = encodeAddressAsString(ial, start, end);
            int size = ial.size();
            
            if(done.contains(state)){
                return state;
            }else{
                done.add(state);
                
                switch(end-start){
                    case 0:
                        Rule r = createRule(state, input.getLabel(), new String[0]);
                        this.addRule(r);
                        break;
                    case 1:
                        handleUnary(input, start, ial, done, size, state);
                        break;
                    default:
                        handleNAry(input, start, ial, done, size, end, state);
                }
            }
            
            return state;
        }

        /**
         * 
         * @param input
         * @param start
         * @param ial
         * @param done
         * @param size
         * @param end
         * @param state 
         */
        private void handleNAry(Tree<String> input, int start, IntArrayList ial,
                Set<String> done, int size, int end, String state) {
            Rule r;
            Tree<String> child = input.getChildren().get(start);
            ial.add(start);
            String cState = makeRules(ial, 0, child.getChildren().size(), child, done);
            ial.size(size);
            String rightState = makeRules(ial, start+1, end, input, done);
            r = createRule(state, LEFT_INTO_RIGHT, new String[] {cState,rightState});
            this.addRule(r);
            child = input.getChildren().get(end-1);
            ial.add(end-1);
            cState = makeRules(ial, 0, child.getChildren().size(), child, done);
            ial.size(size);
            String leftState = makeRules(ial, start, end-1, input, done);
            r = createRule(state, RIGHT_INTO_LEFT, new String[] {leftState,cState});
            this.addRule(r);
        }

        /**
         * 
         * @param input
         * @param start
         * @param ial
         * @param done
         * @param size
         * @param state 
         */
        private void handleUnary(Tree<String> input, int start,
                IntArrayList ial, Set<String> done, int size, String state) {
            Rule r;
            Tree<String> child = input.getChildren().get(start);
            ial.add(start);
            String cState = makeRules(ial, 0, child.getChildren().size(), child, done);
            ial.size(size);
            
            String termState = makeRules(ial, 0, 0, input, done);
            r = createRule(state, LEFT_INTO_RIGHT, new String[] {cState,termState});
            this.addRule(r);
            
            r = createRule(state, RIGHT_INTO_LEFT, new String[] {termState, cState});
            this.addRule(r);
        }
        
        /**
         * 
         * @param parent
         * @param start
         * @param end
         * @return 
         */
        public static String encodeAddressAsString(IntList parent, int start, int end){
           StringBuilder sb = new StringBuilder();
           
           sb.append(start);
           sb.append('-');
           sb.append(end);
           
           for(int i=0;i<parent.size();++i){
               sb.append('-');
               sb.append(parent.get(i));
           }
           
           return sb.toString();
        }
    }
}