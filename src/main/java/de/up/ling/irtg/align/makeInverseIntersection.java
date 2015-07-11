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
                                            List<Pair<TreeAutomaton,Set<String>>> input){
        List<Homomorphism> mappings = makeMappings(input);
        
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
    private static List<Homomorphism> makeMappings(List<Pair<TreeAutomaton, Set<String>>> input) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }   
}