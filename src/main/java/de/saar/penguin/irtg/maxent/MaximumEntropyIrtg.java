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
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author koller
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {
    private Map<String, FeatureFunction> features;
    private Properties weights;

    public MaximumEntropyIrtg(TreeAutomaton<String> automaton, Map<String, FeatureFunction> features) {
        super(automaton);
        this.features = features;
        this.weights = null;
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

        Set<Rule<String>> rules = chart.getRuleSet();
        Iterator<Rule<String>> ruleIter = rules.iterator();
        while (ruleIter.hasNext()) {
            Rule<String> rule = ruleIter.next();
            double weight = 0.0;
            for(String featureName : this.getFeatureNames()){
                FeatureFunction featureFunction = this.getFeatureFunction(featureName);
                double w = Double.valueOf(weights.getProperty(featureName));
                double f = featureFunction.evaluate(rule);
                weight += f * w;
            }
            rule.setWeight(weight);
        }
        return chart;
    }

    public void train(AnnotatedCorpus corpus) {
        // TODO: learn the weights
    }

    @CallableFromShell(name = "weights")
    public void readWeights(Reader reader) throws IOException {
        weights = new Properties();
        weights.load(reader);
    }

    public void writeWeights(Writer writer) {
        // TODO: print weights to the writer
    }
}
