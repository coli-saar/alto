/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.ObjectWithStringCode;

/**
 *
 * @author groschwitz
 */
public abstract class NodeResultEvaluator extends ObjectWithStringCode {
    
    private final String name;
    private final String code;
     
    public NodeResultEvaluator(String name, String code) {
        this.name = name;
        this.code = code;
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }
    
    
    
    /**
     * evaluates the result at the node with respect to the gold value contained in
     * the given instance.
     * @param node the node at which the gold result from the instance is expected
     * @param gold the gold value (what the result should be)
     * @return left is the score, right is the weight (if one wants to 
     * build a weighted average, i.e. over a corpus.
     */
    public abstract Pair<Double, Double> evaluate(TAANode node, Instance gold);
    
    public static NodeResultEvaluator makeLocalTimeEvaluator() {
        return new NodeResultEvaluator("Local time in ms", "local_time") {
            
            @Override
            public Pair<Double, Double> evaluate(TAANode node, Instance gold) {
                return new Pair((double)node.getLastApplicationTime()/1000000, (double)1);
            }
        };
    }
    
    public static NodeResultEvaluator makeCumulativeTimeEvaluator() {
        return new NodeResultEvaluator("Cumulative time in ms", "cumul_time") {
            
            @Override
            public Pair<Double, Double> evaluate(TAANode node, Instance gold) {
                return new Pair((double)node.getLastApplicationTimeCumulative()/1000000, (double)1);
            }
        };
    }
}
