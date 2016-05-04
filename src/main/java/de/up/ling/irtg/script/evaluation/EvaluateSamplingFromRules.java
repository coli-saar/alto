/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph
 */
public class EvaluateSamplingFromRules {
   
   
    public static void main(String... args) {
        
    }
    
    
    /**
     * 
     * @param <Type>
     * @param ta
     * @param config
     * @return 
     */
    public static <Type> DoubleList makeInside(TreeAutomaton<Type> ta, AdaptiveSampler.Configuration config) {
        DoubleArrayList dal = new DoubleArrayList();
        
        List<TreeSample<Rule>> lt = config.run(ta);
        
        for(TreeSample ts : lt) {
            double seen = 0.0;
            
            for(int i=0;i<ts.populationSize();++i) {
                seen += Math.exp(ts.getLogTargetWeight(i)-ts.getLogPropWeight(i));
            }
            
            dal.add(seen / ts.populationSize());
        }
        
        return dal;
    }
    
    /**
     * 
     * @param <Type>
     * @param ta
     * @param config
     * @param repetitions
     * @return 
     */
    public static <Type> Pair<DoubleList,List<DoubleList>> makeInside(TreeAutomaton<Type> ta, AdaptiveSampler.Configuration config, int repetitions) {
        List<DoubleList> r1 = new ArrayList<>();
        DoubleArrayList r2 = new DoubleArrayList();
        
        for(int rep=0;rep<repetitions;++rep) {
            DoubleList dl = makeInside(ta, config);
            
            r1.add(dl);
            for(int round=0;round<dl.size();++round) {
                if(rep==0) {
                    r2.add(dl.getDouble(round));
                } else {
                    r2.set(round, dl.getDouble(round)+r2.getDouble(round));
                }
            }
            
            System.out.println("finished repetition: "+rep);
        }
        
        for(int i=0;i<r2.size();++i) {
            r2.set(i, r2.getDouble(i) / r1.size());
        }
        
        return new Pair<>(r2,r1);
    }
}
