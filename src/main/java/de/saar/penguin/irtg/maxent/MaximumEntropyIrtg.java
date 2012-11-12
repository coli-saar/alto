/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import de.saar.penguin.irtg.AnnotatedCorpus;
import de.saar.penguin.irtg.InterpretedTreeAutomaton;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.tree.Tree;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author koller
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {
    private Map<String, FeatureFunction> features;
    private Map<String, Double> weights;
    private static double INITIAL_WEIGHT = 0.0;
    private static double CONVERGE_DELTA = 0.0001;

    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, Map<String, FeatureFunction> features) {
        super(automaton);
        this.features = features;
        this.weights = new HashMap<String, Double>();
    }
    
    public Set<String> getFeatureNames() {
        return features.keySet();
    }
    
    public FeatureFunction getFeatureFunction(String name) {
        return features.get(name);
    }

    @Override
    @CallableFromShell(name = "parse")
    public TreeAutomaton parseFromReaders(Map<String, Reader> readers) throws ParserException, IOException {
        TreeAutomaton chart = super.parseFromReaders(readers);

        Set<Rule> rules = chart.getRuleSet();
        Iterator<Rule> ruleIter = rules.iterator();
        while (ruleIter.hasNext()) {
            Rule rule = ruleIter.next();
            double weight = 0.0;
            for(String featureName : this.getFeatureNames()){
                FeatureFunction featureFunction = this.getFeatureFunction(featureName);
                if(!weights.containsKey(featureName)){
                    weights.put(featureName, INITIAL_WEIGHT);
                }
                double w = weights.get(featureName);
                double f = featureFunction.evaluate(rule);
                weight += f * w;
            }
            rule.setWeight(weight);
        }
        return chart;
    }

//    public void train(AnnotatedCorpus corpus) {
    public void train(AnnotatedCorpus corpus) throws IOException, ParserException {
        // entfernen sobald klar ist, wie die chart korrekt ermittelt wird
        Map<AnnotatedCorpus.Instance, String> sentences = new HashMap<AnnotatedCorpus.Instance, String>();
        sentences.put(corpus.getInstances().get(0),"john watches the woman with the telescope");
        sentences.put(corpus.getInstances().get(0),"john watches the telescope with the telescope");
        sentences.put(corpus.getInstances().get(0),"john watches the telescope with the woman");
        
        // calculate all ptilde(f_i)
        Map<AnnotatedCorpus.Instance, Map<Rule, Double>> expectations = this.expectations(corpus.getInstances());

        List<Double> delta = new ArrayList<Double>();
        Map<String, Map<Double, Double>> mu = new HashMap<String, Map<Double, Double>>();
        // loop until Lambda = (lambda1,...,lambdan) converges:
        while(Collections.max(delta) > this.CONVERGE_DELTA){
            //   foreach sentence s in corpus:
            for(AnnotatedCorpus.Instance i : corpus.getInstances()){
                // compute parse chart C for s using p_Lambda
                Map<String, Reader> readers = new HashMap<String, Reader>();
                readers.put("i", new StringReader(sentences.get(i)));
                TreeAutomaton chart = this.parseFromReaders(readers);
                // compute inside and outside probabilities for all states and rules in C
                Map<String, Double> inside = chart.inside();
                Map<String, Double> outside = chart.outside(inside);

                // foreach rule r in C:
                // warum nicht G? in der chart kommen regeln doch mehrfach vor.
                Set<Rule> rules = chart.getRuleSet();
                Iterator<Rule> ruleIter = rules.iterator();
                while (ruleIter.hasNext()) {
                    Rule rule = ruleIter.next();
                    String label = rule.getLabel();

                    // Wichtig ist dabei noch, dass Z_s einfach inside(S) ist.
                    double z_s = inside.get("r1"); // TODO: How to get the starting rule
                    double expectS = 0.0; // ptilde(s) = sum_y ptilde(x,y)
                    for(Rule r : expectations.get(i).keySet()){
                        expectS += expectations.get(i).get(r);
                    }
                    // foreach i with f_i(r) != 0
                    for(String featureName : this.getFeatureNames()){
                        FeatureFunction featureFunction = this.getFeatureFunction(featureName);
                        if(weights.get(featureName) > 0.0){
                            double k = inside.get(label);
                            double l = outside.get(label);
                            // for k, l with inside_k(r) != 0 and outside_l(r) != 0:
                            if((k > 0.0) && (l > 0.0)){
                                // mu<i,k+l> += ptilde(s) / Z_s * f_i(r) * inside_k(r) * outside_l(r)
                                if(!mu.containsKey(featureName)){
                                    mu.put(featureName, new HashMap<Double, Double>());
                                }
                                Map<Double, Double> mui = mu.get(featureName);
                                double oldValue = (mui.containsKey(k+l))? mui.get(k+l) : 0.0 ;
                                double value = expectS / z_s * featureFunction.evaluate(rule) * k * l;
                                mui.put(k+l, oldValue + value);
                            }
                        }
                    }
                }
            }
            //   foreach i:
            for(String featureName : this.getFeatureNames()){
                // solve sum_k mu<i,k> * e^(k * delta_i) = ptilde(f_i) for delta_i (e.g. using Newton)
                // lambda_i += delta_i
            }
        }
    }
    
    private Map<AnnotatedCorpus.Instance, Map<Rule, Double>> expectations(List<AnnotatedCorpus.Instance> corpus){
        Map<AnnotatedCorpus.Instance, Map<Rule, Double>> expects = new HashMap<AnnotatedCorpus.Instance, Map<Rule, Double>>();
        Map<AnnotatedCorpus.Instance, Map<String, Integer>> count = new HashMap<AnnotatedCorpus.Instance, Map<String, Integer>>();
        double countSigma = 0.0;
        for(AnnotatedCorpus.Instance i : corpus){
            count.put(i, new HashMap<String, Integer>());
            countSigma = this.countRules(i.tree, count.get(i));
        }
        for(AnnotatedCorpus.Instance i : corpus){
            Map<Rule, Double> expect = new HashMap<Rule, Double>();
            for(Rule rule : this.automaton.getRuleSet()){
                if(count.get(i).containsKey(rule.getLabel())){
                    expect.put(rule, count.get(i).get(rule.getLabel()) / countSigma);
                }else{
                    expect.put(rule, 0.0);
                }
            }
            expects.put(i, expect);
        }
        return expects;
    }
    
    private int countRules(Tree<String> tree, Map<String, Integer> counts){
        int ret;
        if(counts.containsKey(tree.getLabel())){
            counts.put(tree.getLabel(), 1 + counts.get(tree.getLabel()));
            ret = 1;
        }else{
            ret = 0;
        }
        List<Tree<String>> children = tree.getChildren();
        for(Tree<String> child : children){
            ret += this.countRules(child, counts);
        }
        return ret;
    }

    @CallableFromShell(name = "weights")
    public void readWeights(Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);
        for(Map.Entry<Object, Object> entry : props.entrySet()){
            String key = String.valueOf(entry.getKey());
            Double value = Double.valueOf(String.valueOf(entry.getValue()));
            this.weights.put(key, value);
        }
    }

    public void writeWeights(Writer writer) {
        // TODO: print weights to the writer
    }
}
