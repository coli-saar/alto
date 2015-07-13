/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.InverseHomAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author christoph
 */
public class makeInverseIntersection {
    
    /**
     * 
     * @param input
     * @param rlm
     * @return 
     */
    public static Pair<TreeAutomaton,List<Homomorphism>> makeInverseIntersection(
                                            List<TreeAutomaton> input, RuleMarker rlm){
        Signature sig = new Signature();
        List<Homomorphism> mappings = makeMappings(input, sig, rlm);
        
        TreeAutomaton ta = new InverseHomAutomaton(input.get(0), mappings.get(0));
        for(int i=1;i<mappings.size();++i)
        {
            TreeAutomaton inv = new InverseHomAutomaton(input.get(i), mappings.get(i));
            ta = ta.intersect(inv);
        }
        
        return new Pair<>(ta,mappings);
    }

    /**
     * 
     * @param input
     * @return 
     */
    private static List<Homomorphism> makeMappings(List<TreeAutomaton> input, Signature sig,
                                                RuleMarker rlm) {
        List<Homomorphism> ret = new ArrayList<>();
        for(int i=0;i<input.size();++i){
            ret.add(new Homomorphism(sig, input.get(i).getSignature()));
        }
        
        IterationHelper ih = new IterationHelper(input);
        IntList arities = new IntArrayList();
        List<Tree<String>> imageList = new ArrayList<>();
        
        while(ih.hasNext()){
            List<String> tuple = ih.next();
            
            if(rlm.checkCompatible(tuple)){
                arities.clear();
                
                for(int i=0;i<tuple.size();++i){
                    arities.add(input.get(i).getSignature().getArityForLabel(tuple.get(i)));
                    PermutationIterator pi = new PermutationIterator(arities);
                    
                    while(pi.hasNext()){
                        List<String[]> permutation = pi.next();
                        String label = Util.gensym("l_");
                        
                        for(int k=0;k<ret.size();++k){
                            imageList.clear();
                            
                            for(String s : permutation.get(k)){
                                imageList.add(Tree.create(s));
                            }
                            
                            ret.get(k).add(label, Tree.create(tuple.get(k), imageList));
                        }
                    }
                }
            }
        }
        
        return ret;
    }
    
    /**
     * 
     */
    private static class PermutationIterator implements Iterator<List<String[]>>{

        private PermutationIterator(IntList arities) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<String[]> next() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    
    /**
     * 
     */
    private static class IterationHelper implements Iterator<List<String>>{

        /**
         * 
         */
        private final ObjectList<String> output = new ObjectArrayList<>();
        
        /**
         * 
         */
        private final List<Signature> input = new ArrayList<>();
        
        /**
         * 
         */
        private final ObjectList<Iterator<String>> mainSource = new ObjectArrayList<>();
        
        /**
         * 
         */
        private boolean first = true;
        
        /**
         * 
         * @param input 
         */
        public IterationHelper(List<TreeAutomaton> input) {
            for(int i=0;i<input.size();++i)
            {
                Signature s = input.get(i).getSignature();
                this.input.add(s);
            }
            
            for(int i=0;i<this.input.size();++i)
            {
                if(this.input.get(i).getSymbols().isEmpty())
                {
                    throw new IllegalArgumentException("Empty signature for automaton: "+input.get(i));
                }
                
                Iterator<String> s = this.input.get(i).getSymbols().iterator();
                this.output.add(s.next());
                
                this.mainSource.add(s);
            }
        }
        
        @Override
        public boolean hasNext() {
            if(first)
            {
                return true;
            }
            
            for(int i=0;i<this.mainSource.size();++i)
            {
               if(this.mainSource.get(i).hasNext())
               {
                   return true;
               }
            }
            
            return false;
        }

        @Override
        public List<String> next() {
            if(first)
            {
                first = false;
                return this.output;
            }
            
            int cutOff = this.mainSource.size()-1;
            
            while(!this.mainSource.get(cutOff).hasNext())
            {
                --cutOff;
                if(cutOff < 0)
                {
                    throw new NoSuchElementException();
                }
            }
            
            for(int i=cutOff+1;i<this.input.size();++i)
            {
                this.mainSource.set(i, this.input.get(i).getSymbols().iterator());
            }
            
            for(int i=cutOff;i<this.mainSource.size();++i)
            {
                this.output.set(i, this.mainSource.get(i).next());
            }
            
            return this.output;
        }
    }
}