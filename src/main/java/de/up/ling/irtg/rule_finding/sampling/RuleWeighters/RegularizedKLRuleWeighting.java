/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.sampling.RuleWeighters;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.learning_rates.LearningRate;
import de.up.ling.irtg.rule_finding.sampling.RuleWeighting;
import de.up.ling.irtg.rule_finding.sampling.TreeSample;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author teichmann
 */
public abstract class RegularizedKLRuleWeighting implements RuleWeighting {
    /**
     * 
     */
    public static double ALMOST_ZERO = 1E-15;
    
    /**
     * 
     */
    private final TreeAutomaton basis;
    
    /**
     * 
     */
    private int updateNumber = 0;
    
    /**
     * 
     */
    private final Int2IntMap lastUpdated;
    
    /**
     * 
     */
    private final int[] startStates;
    
    /**
     * 
     */
    private final double[] startParameters;
    
    /**
     * 
     */
    private final double[] startProbabilities;
    
    /**
     * 
     */
    private final int normalizationExponent;
    
    /**
     * 
     */
    private final double normalizationDivisor;
    
    /**
     * 
     */
    private final LearningRate rate;
    
    /**
     * 
     */
    private final Int2BooleanMap currentProbs = new Int2BooleanOpenHashMap();
    
    /**
     * 
     */
    private final Int2ObjectMap<double[]> ruleProbs = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2ObjectMap<Rule[]> listRules = new Int2ObjectOpenHashMap<>();
    
    /**
     * 
     */
    private final Int2ObjectMap<double[]> ruleParameters = new Int2ObjectOpenHashMap<>();

    /**
     * 
     */
    private double underFlowPreventer = Double.NEGATIVE_INFINITY;
    
    /**
     * 
     * @param basis
     * @param normalizationExponent
     * @param normalizationDivisor
     * @param rate 
     */
    public RegularizedKLRuleWeighting(TreeAutomaton basis, int normalizationExponent,
                                        double normalizationDivisor, LearningRate rate) {
        this.basis = basis;
        
        IntArrayList start = new IntArrayList();
        IntIterator iit = basis.getFinalStates().iterator();
        
        while(iit.hasNext()) {
            start.add(iit.nextInt());
        }
        
        this.startStates = start.toIntArray();
        this.startParameters = new double[startStates.length];
        Arrays.fill(startParameters, 0.0);
        Arrays.sort(startStates);
        this.startProbabilities = new double[startStates.length];
        Arrays.fill(this.startProbabilities, 0);
        
        this.lastUpdated = new Int2IntOpenHashMap();
        this.lastUpdated.defaultReturnValue(0);
        
        this.normalizationExponent = normalizationExponent-1;
        this.normalizationDivisor = 1.0 / normalizationDivisor;
        this.rate = rate;
        
        currentProbs.defaultReturnValue(false);
    }
    
    @Override
    public double getLogProbability(int state, int ruleNumber) {
        double[] probs= this.ruleProbs.get(state);
        
        return Math.log(probs[ruleNumber]);
    }

    @Override
    public void prepareProbability(int state) {
        int num = this.lastUpdated.get(state);
        
        if(num >= this.updateNumber && currentProbs.get(state)) {
            return;
        }
        
        Rule[] rules = this.ensureRules(state);
        double[] probs = this.ruleProbs.get(state);
        double[] paras = this.ruleParameters.get(state);
        
        if(num < this.updateNumber) {
            while(num < this.updateNumber) {
                for(int i=0;i<rules.length;++i) {
                    this.adapt(i, rules[i], paras, null, null, -1.0);
                }
            
                ++num;
            }
        
            this.lastUpdated.put(state, updateNumber);
        }
        
        double sum = 0.0;
        for(int i=0;i<paras.length;++i) {
            sum += (probs[i] = Math.exp(paras[i]));
        }
        
        for(int i=0;i<probs.length;++i) {
            probs[i] /= sum;
        }
        
        this.currentProbs.put(state, true);
    }

    @Override
    public double getStateStartLogProbability(int position) {
        return Math.log(this.startProbabilities[position]);
    }

    @Override
    public void prepareStartProbability() {
        double sum = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        
        for(int i=0;i<this.startParameters.length;++i) {
            max = Math.max(max, this.startParameters[i]);
        }
        
        for(int i=0;i<this.startParameters.length;++i) {
            sum += (this.startProbabilities[i] = Math.exp(this.startParameters[i]-max));
        }
        
        for(int i=0;i<this.startProbabilities.length;++i) {
            this.startProbabilities[i] /= sum;
        }
    }

    @Override
    public void reset() {
        Arrays.fill(this.startParameters, 0);
        this.lastUpdated.clear();
        this.updateNumber = 0;
        
        this.currentProbs.clear();
        for(double[] paras : this.ruleParameters.values()) {
            Arrays.fill(paras, 0.0);
        }
        
        this.rate.reset();
        
        this.underFlowPreventer = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void adapt(TreeSample<Rule> treSamp, boolean deterministic) {
        Int2DoubleOpenHashMap stateCounts = new Int2DoubleOpenHashMap();
        stateCounts.defaultReturnValue(0.0);
        
        Object2DoubleOpenHashMap ruleCounts = new Object2DoubleOpenHashMap();
        ruleCounts.defaultReturnValue(0.0);
        
        double[] startCount = new double[this.startStates.length];
        double logMax = treSamp.makeMaxBase(deterministic,this.underFlowPreventer);
        if(logMax != this.underFlowPreventer) {
            this.rate.reset();
            this.underFlowPreventer = logMax;
        }
        
        double wholeCount = makeAmounts(treSamp, stateCounts, ruleCounts, startCount, deterministic);
        
        IntIterator states = stateCounts.keySet().iterator();
        
        int updatePlusOne = this.updateNumber+1;
        
        while(states.hasNext()) {
            int state = states.nextInt();
            Rule[] arr = ensureRules(state);
            
            this.prepareProbability(state);
            double[] props = this.ruleProbs.get(state);
            double[] parameters = this.ruleParameters.get(state);
            
            for(int i=0;i<arr.length;++i) {
                this.adapt(i, arr[i], parameters, ruleCounts, stateCounts, props[i]);
            }
            
            this.lastUpdated.put(state, updatePlusOne);
            
            this.currentProbs.put(state, false);
        }
        
        this.prepareStartProbability();
        for(int i=0;i<this.startStates.length;++i) {
            updateStart(startParameters,i,startCount,wholeCount,this.startProbabilities[i]);
        }
        
        ++this.updateNumber;
    }

    /**
     * 
     * @param state
     * @return 
     */
    private Rule[] ensureRules(int state) {
        Rule[] arr = this.listRules.get(state);
        
        if(arr == null) {
            ArrayList<Rule> list = new ArrayList<>();
            this.basis.foreachRuleTopDown(state, (Object r) -> list.add((Rule) r));
            
            arr = list.toArray(new Rule[list.size()]);
            
            Arrays.sort(arr);
            this.listRules.put(state, arr);
            
            double[] probs = new double[arr.length];
            Arrays.fill(probs, 0.0);
            this.ruleProbs.put(state, probs);
            
            probs = new double[arr.length];
            Arrays.fill(probs, 0.0);
            this.ruleParameters.put(state, probs);
        }
        
        return arr;
    }

    /**
     * 
     * @param treSamp
     * @param stateCounts
     * @param ruleCounts 
     */
    private double makeAmounts(TreeSample<Rule> treSamp, Int2DoubleOpenHashMap stateCounts,
                            Object2DoubleOpenHashMap ruleCounts, double[] startCount, boolean deterministic) {
        double wholeCount = 0.0;
        
        for(int i=0;i<treSamp.populationSize();++i) {
            double amount = treSamp.getSelfNormalizedWeight(i);
            Tree<Rule> instance = treSamp.getSample(i);
            
            wholeCount += amount;
            int index = Arrays.binarySearch(startStates, instance.getLabel().getParent());
            startCount[index] += amount;
            
            addAmounts(instance,stateCounts,ruleCounts, amount);
        }
        
        return wholeCount;
    }

    @Override
    public TreeAutomaton getAutomaton() {
        return this.basis;
    }

    /**
     * 
     * @param instance
     * @param stateCounts
     * @param ruleCounts 
     */
    private void addAmounts(Tree<Rule> instance, Int2DoubleOpenHashMap stateCounts,
            Object2DoubleOpenHashMap ruleCounts, double contribution) {
        Rule r = instance.getLabel();
        
        stateCounts.addTo(r.getParent(), contribution);
        ruleCounts.addTo(r, contribution);
        
        int size = instance.getChildren().size();
        for(int i=0;i<size;++i){
            this.addAmounts(instance.getChildren().get(i), stateCounts, ruleCounts, contribution);
        }
    }

    /**
     * 
     * @param rr
     * @param ruleCounts
     * @param stateCounts 
     */
    private void adapt(int position, Rule rr, double[] parameters,
            Object2DoubleOpenHashMap ruleCounts, Int2DoubleOpenHashMap stateCounts,
            double probability) {
        double val = parameters[position];
        
        double gradient = Double.compare(val, 0.0)*(Math.pow(Math.abs(val), this.normalizationExponent))*normalizationDivisor;
        
        if(ruleCounts != null && stateCounts != null) {
            gradient += probability*stateCounts.get(rr.getParent());
            gradient -= ruleCounts.getDouble(rr);
        }
        
        double lr = this.rate.getLearningRate(rr.getParent(), position, gradient);
        
        parameters[position] -= lr*gradient;
    }

    /**
     * 
     * @param parameters
     * @param position
     * @param startCount
     * @param wholeCount
     * @param startProbability 
     */
    private void updateStart(double[] parameters, int position, double[] startCount, double wholeCount,
            double startProbability) {
        double val = parameters[position];
        
        double gradient = Double.compare(val, 0.0)*(Math.pow(Math.abs(val), this.normalizationExponent))*normalizationDivisor;
        
        if(startCount != null) {
            gradient += startProbability*wholeCount;
            gradient -= startCount[position];
        }
        
        double lr = this.rate.getLearningRate(-1, position, gradient);
        
        parameters[position] -= lr*gradient;
    }

    @Override
    public int getNumberOfStartStates() {
        return this.startStates.length;
    }

    @Override
    public int getStartStateByNumber(int number) {
        return this.startStates[number];
    }

    @Override
    public int getRuleNumber(int state, double choicePoint) {
        double[] probs = this.ruleProbs.get(state);
        
        for(int i=0;i<probs.length;++i) {
            choicePoint -= probs[i];
            
            if(choicePoint <= ALMOST_ZERO) {
                return i;
            }
        }
        
        throw new IllegalStateException("Probabilities did not sum to one.");
    }

    @Override
    public Rule getRuleByNumber(int state, int number) {
        return this.listRules.get(state)[number];
    }

    @Override
    public int getStartStateNumber(double choicePoint) {
        for(int i=0;i<this.startStates.length;++i) {
            choicePoint -= this.startProbabilities[i];
            
            if(choicePoint <= ALMOST_ZERO) {
                return i;
            }
        }
        
        throw new IllegalStateException("Probabilities did not sum to one.");
    }

    @Override
    public double getLogProbability(Rule r) {
        int index = Arrays.binarySearch(this.listRules.get(r.getParent()), r);
        
        return Math.log(this.ruleProbs.get(r.getParent())[index]);
    }
}
