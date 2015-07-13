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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author christoph
 */
public class makeInverseIntersection {
    
    /**
     * 
     * @param input
     * @return 
     */
    public static Pair<TreeAutomaton,List<Homomorphism>> makeInverseIntersection(
                                            List<Pair<TreeAutomaton,Set<String>>> input,
                                            boolean match){
        Signature sig = new Signature();
        List<Homomorphism> mappings = makeMappings(input, match, sig);
        
        TreeAutomaton ta = new InverseHomAutomaton(input.get(0).getLeft(), mappings.get(0));
        for(int i=1;i<mappings.size();++i)
        {
            TreeAutomaton inv = new InverseHomAutomaton(input.get(i).getLeft(), mappings.get(i));
            ta = ta.intersect(inv);
        }
        
        return new Pair<>(ta,mappings);
    }

    /**
     * 
     * @param input
     * @return 
     */
    private static List<Homomorphism> makeMappings(List<Pair<TreeAutomaton, Set<String>>> input,
                                                    boolean match, Signature sig) {
        List<Homomorphism> l = new ArrayList<>();
        for(int i=0;i<input.size();++i){
            l.add(new Homomorphism(sig, input.get(i).getLeft().getSignature()));
        }
        
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * 
     */
    private class IterationHelper implements Iterator<IntList>{

        /**
         * 
         */
        private final IntList output = new IntArrayList();
        
        /**
         * 
         */
        private final List<Signature> input = new ArrayList<>();
        
        /**
         * 
         */
        private final ObjectList<IntIterator> mainSource = new ObjectArrayList<>();
        
        /**
         * 
         * @param input 
         */
        public IterationHelper(List<Pair<TreeAutomaton, Set<String>>> input,
                                    boolean mixInSpecials) {
            for(int i=0;i<input.size();++i)
            {
                Signature s = input.get(i).getLeft().getSignature();
                //TODO
                
            }
            
        }
        
        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public IntList next() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}