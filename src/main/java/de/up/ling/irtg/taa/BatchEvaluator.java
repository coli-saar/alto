/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.taa;

import de.saar.basic.Pair;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.ObjectWithStringCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author groschwitz
 */
public abstract class BatchEvaluator extends ObjectWithStringCode {
    private final String name;
    private final String code;
    private final String[] requiredInstanceEvaluators;
    
    public BatchEvaluator(String name, String code, String[] requiredInstanceEvaluators) {
        this.name = name;
        this.code = code;
        this.requiredInstanceEvaluators = requiredInstanceEvaluators;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    public String[] getRequiredInstanceEvaluators() {
        return requiredInstanceEvaluators;
    }
    
    public double getScore(Iterable<Object> results, Iterable<Instance> golds, Map<String,List<Pair<Double, Double>>> evaluationsOnInstances) {
        Iterable<Pair<Double, Double>>[] evalsToPass = new Iterable[getRequiredInstanceEvaluators().length];
        int i = 0;
        for (i = 0; i<getRequiredInstanceEvaluators().length; i++) {
            evalsToPass[i] = evaluationsOnInstances.get(getRequiredInstanceEvaluators()[i]);
        }
        return getScore(results, golds, evalsToPass);
    }
    
    protected abstract double getScore(Iterable<Object> results, Iterable<Instance> golds, Iterable<Pair<Double, Double>>[] evals);
    
    
    public static class WeightedSumBatchEvaluator extends BatchEvaluator {
        public WeightedSumBatchEvaluator(String name, String code, String localEvalCode) {
            super(name, code, new String[]{localEvalCode});
        }

        @Override
        protected double getScore(Iterable<Object> results, Iterable<Instance> golds, Iterable<Pair<Double, Double>>[] evals) {
            double runningSum = 0;
            double runningTotalWeight = 0;
            for (Pair<Double, Double> scoreAndWeight : evals[0]) {
                runningSum += scoreAndWeight.left*scoreAndWeight.right;
                runningTotalWeight += scoreAndWeight.right;
            }
            return (runningTotalWeight == 0) ? runningSum : runningSum/runningTotalWeight;//not quite sure if should throw error?
        }
    }
    
    
    public static class WeightedFMeanBatchEvaluator extends BatchEvaluator {
        public WeightedFMeanBatchEvaluator(String name, String code, String localEvalCode1, String localEvalCode2) {
            super(name, code, new String[]{localEvalCode1, localEvalCode2});
        }

        @Override
        protected double getScore(Iterable<Object> results, Iterable<Instance> golds, Iterable<Pair<Double, Double>>[] evals) {
            double runningSum1 = 0;
            double runningTotalWeight1 = 0;
            for (Pair<Double, Double> scoreAndWeight : evals[0]) {
                runningSum1 += scoreAndWeight.left*scoreAndWeight.right;
                runningTotalWeight1 += scoreAndWeight.right;
            }
            double res1 =  (runningTotalWeight1 == 0) ? runningSum1 : runningSum1/runningTotalWeight1;
            
            double runningSum2 = 0;
            double runningTotalWeight2 = 0;
            for (Pair<Double, Double> scoreAndWeight : evals[1]) {
                runningSum2 += scoreAndWeight.left*scoreAndWeight.right;
                runningTotalWeight2 += scoreAndWeight.right;
            }
            double res2 =  (runningTotalWeight2 == 0) ? runningSum2 : runningSum2/runningTotalWeight2;
            
            return (res1+res2 == 0) ? 0 : 2*res1*res2/(res1+res2);//not quite sure if should throw error?
        }
    }
    
    public static List<BatchEvaluator> getBatchEvaluatorsFromInstanceEvaluators(List<NodeResultEvaluator> evals) {
        List<BatchEvaluator> ret = new ArrayList<>();
        for (NodeResultEvaluator eval : evals) {
            ret.add(new WeightedSumBatchEvaluator(eval.toString(), eval.getCode(), eval.getCode()));
        }
        if (evals.stream().map(eval -> eval.getCode()).anyMatch(string -> string.equals("recall")) && evals.stream().map(eval -> eval.getCode()).anyMatch(string -> string.equals("precision"))) {
            ret.add(new WeightedFMeanBatchEvaluator("F1-score", "f1", "recall", "precision"));
        }
        return ret;
    }
    
}
