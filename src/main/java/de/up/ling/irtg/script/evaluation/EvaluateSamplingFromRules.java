/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script.evaluation;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.WeightedTree;
import de.up.ling.irtg.rule_finding.sampling.AdaptiveSampler;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author christoph
 */
public class EvaluateSamplingFromRules {
    /**
     * 
     */
    public static double SMOOTH = 0.0001;
    
    /**
     * 
     * @param <Type>
     * @param auts
     * @param repetitions
     * @param config
     * @return
     * @throws Exception 
     */
    public static <Type>  Measurements<Type> makeSmoothedKL(Iterable<TreeAutomaton<Type>> auts, int repetitions,
            AdaptiveSampler.Configuration config) throws Exception {
        int dataset = 0;
        Measurements<Type> result = new Measurements<>();
        
        for(TreeAutomaton<Type> tat : auts) {
            WeightedTree wt = tat.viterbiRaw();
            Int2ObjectMap<Double> map = tat.inside();
            double val = map.get(tat.getFinalStates().iterator().nextInt());
            
            double expected = wt.getWeight() / val;
            
            for(int repetition=0;repetition<repetitions;++repetition) {
                
                List<TreeSample<Rule>> lt = config.run(tat);
                
                DoubleList actuals = new DoubleArrayList();
                
                Object2DoubleMap<Rule>[] estimate = computeEstimate(lt);
                
                for(int round=0;round<lt.size();++round) {
                    for(int measuredPosition=0;measuredPosition<states.size();++measuredPosition) {
                        Type state = states.get(measuredPosition);
                                                
                        Iterable<Rule> ir = tat.getRulesTopDown(tat.getIdForState(state));
                        double kl = 0.0;
                        
                        double div = 0.0;
                        for(Rule r : ir){++div;};
                        double add = 1.0 / div;
                        
                        for(Rule r : ir) {
                            double frac = SMOOTH*add+(1.0-SMOOTH)*estimate[round].getDouble(r);
                            
                            double real = om.get(r);
                            
                            kl += real * (Math.log(real)-Math.log(frac));
                        }
                        
                        result.addMeasurement(measuredPosition, dataset, repetition, round, kl);
                    }
                }
                
                System.out.println("finished one repetition");
            }
            
            ++dataset;
        }
        
        return result;
    }

    /**
     * Array is over measured types.
     * 
     * 
     * @param <Type>
     * @param tat
     * @param states
     * @return 
     */
    private static <Type> Object2DoubleMap<Rule> makeTarget(TreeAutomaton<Type> tat, List<Type> states) {
        Int2ObjectMap<Double> inside = tat.inside();
        Object2DoubleMap<Rule> om = new Object2DoubleOpenHashMap();
        for(int i=0;i<states.size();++i) {
            Type t = states.get(i);
            
            Iterable<Rule> rules = tat.getRulesTopDown(tat.getIdForState(t));
            for(Rule r : rules) {
                double val = r.getWeight();
                for(int j=0;j<r.getArity();++j) {
                    val *= inside.get(r.getChildren()[j]);
                }
                
                val /= inside.get(r.getParent());
                om.put(r, val);
            }
        }
        return om;
    }

    /**
     * Array over round numbers.
     * 
     * @param <Type>
     * @param states
     * @param lt
     * @return 
     */
    private static <Type> Object2DoubleMap<Rule>[] computeEstimate(List<TreeSample<Rule>> lt) {
        Object2DoubleMap<Rule>[] result = new Object2DoubleMap[lt.size()];
        
        for(int round=0;round<lt.size();++round) {
            Int2DoubleOpenHashMap stateCounts = new Int2DoubleOpenHashMap();
            Object2DoubleOpenHashMap<Rule> ruleCounts = new Object2DoubleOpenHashMap<>();
            
            TreeSample<Rule> ts = lt.get(round);
            for(int sample=0;sample<ts.populationSize();++sample) {
                Tree<Rule> tr = ts.getSample(sample);
                double d = ts.getSelfNormalizedWeight(sample);
                
                for(Tree<Rule> node : tr.getAllNodes()) {
                    stateCounts.addTo(node.getLabel().getParent(), d);
                    ruleCounts.addTo(node.getLabel(), d);
                }
            }
            
            Object2DoubleOpenHashMap<Rule> odm = new Object2DoubleOpenHashMap<>();
            for(Rule r : ruleCounts.keySet()) {
                odm.put(r, ruleCounts.getDouble(r) / stateCounts.get(r.getParent()));
            }
            
            result[round] = odm;
        }
        
        return result;
    }
    
    /**
     * 
     * @param <Type>
     */
    public static class StatePicker<Type> {
        /**
         * 
         */
        private final int numberUsed;

        /**
         * 
         * @param numberUsed 
         */
        public StatePicker(int numberUsed) {
            this.numberUsed = numberUsed;
        }
        
        /**
         * 
         * @param ta
         * @return 
         * @throws java.lang.Exception 
         */
        public List<Type> pick(TreeAutomaton<Type> ta) throws Exception{
            Tree<Integer> tree = ta.viterbiRaw().getTree();
            Tree<Rule> rules = ta.getRuleTree(tree);
            
            Type root = ta.getStateForId(rules.getLabel().getParent());
            
            AtomicInteger ai = new AtomicInteger(numberUsed);
            
            int num = pick(rules,ai);
            
            Type other;
            if(num > 0) {
                other = ta.getStateForId(num);
            } else {
                other = root;
            }
            
            ArrayList<Type> l = new ArrayList<>();
            l.add(root);
            l.add(other);
            
            return l;
        }

        /**
         * 
         * @param tree
         * @param ai
         * @return 
         */
        private int pick(Tree<Rule> tree, AtomicInteger ai) {
            Rule r = tree.getLabel();
            
            if(r.getArity() > 0) {
                ai.decrementAndGet();
            } else {
                return -1;
            }
            
            int num = ai.get();
            if(num < 1) {
                return r.getParent();
            }
            
            for(int i=0;i<tree.getChildren().size();++i) {
                Tree<Rule> child = tree.getChildren().get(i);
                
                int res = pick(child,ai);
                if(res > 0) {
                    return res;
                }
            }
            
            return -1;
        }
    }
    
    /**
     * 
     * @param <Type> 
     */
    public static class Measurements<Type> {
        /**
         * 
         */
        private final List<List<List<DoubleList>>> measurements = new ArrayList<>();

        /**
         * 
         */
        private final List<List<Type>> measureType = new ArrayList<>();
        
        /**
         * 
         * @param measurementTypeNumber
         * @param dataSetNumber
         * @param repetitionNumber
         * @param roundNumber
         * @param value 
         */
        public void addMeasurement(int measurementTypeNumber, int dataSetNumber, int repetitionNumber, int roundNumber, double value) {
            while(measurements.size() <= measurementTypeNumber) {
                measurements.add(new ArrayList<>());
            }
            
            List<List<DoubleList>> internal = measurements.get(measurementTypeNumber);
            while(internal.size() <= dataSetNumber) {
                internal.add(new ArrayList<>());
            }
            
            List<DoubleList> dl = internal.get(dataSetNumber);
            while(dl.size() <= repetitionNumber) {
                dl.add(new DoubleArrayList());
            }
            
            DoubleList innerMost = dl.get(repetitionNumber);
            while(innerMost.size() <= roundNumber) {
                innerMost.add(Double.POSITIVE_INFINITY);
            }
            
            innerMost.set(roundNumber, value);
        }
        
        /**
         * 
         * @return 
         */
        public int getNumberOfTypes() {
            return this.measurements.size();
        }
        
        /**
         * 
         * @param measurementType
         * @return 
         */
        public int getNumberOfDataSets(int measurementType) {
            return this.measurements.get(measurementType).size();
        }
        
        /**
         * 
         * @param measurementType
         * @param dataSetNumber
         * @return 
         */
        public int getNumberOfRepetitions(int measurementType, int dataSetNumber) {
            return this.measurements.get(measurementType).get(dataSetNumber).size();
        }
        
        /**
         * 
         * @param measurementType
         * @param dataSetNumber
         * @param repetition
         * @return 
         */
        public int getNumberOfRounds(int measurementType, int dataSetNumber, int repetition) {
            return this.measurements.get(measurementType).get(dataSetNumber).get(repetition).size();
        }
        
        /**
         * 
         * @param measurementType
         * @param dataSetNumber
         * @param repetition
         * @param rounds
         * @return 
         */
        public double getValue(int measurementType, int dataSetNumber, int repetition, int rounds) {
            return this.measurements.get(measurementType).get(dataSetNumber).get(repetition).get(rounds);
        }
        
        /**
         * 
         * @param measurementTypeNumber
         * @param dataSetNumber
         * @param measure 
         */
        public void addMeasured(int measurementTypeNumber, int dataSetNumber, Type measure) {
            while(this.measureType.size() <= measurementTypeNumber) {
                this.measureType.add(new ArrayList<>());
            }
            
            List<Type> inner = this.measureType.get(measurementTypeNumber);
            while(inner.size() <= dataSetNumber) {
                inner.add(null);
            }
            
            inner.set(dataSetNumber, measure);
        }
        
        /**
         * 
         * @parammeasurementTypeNumberdataset
         * @return 
         */
        public int getNumberOfDataSetsForTypeEntry(int measurementTypeNumber) {
            return this.measureType.get(measurementTypeNumber).size();
        }
        
        /**
         * 
         * @param measurementTypeNumber
         * @param dataSetNumber
         * @return 
         */
        public Type getMeasured(int measurementTypeNumber, int dataSetNumber) {
            return this.measureType.get(measurementTypeNumber).get(dataSetNumber);
        }
    }
}
