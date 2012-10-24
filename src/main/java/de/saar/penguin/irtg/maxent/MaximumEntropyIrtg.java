/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.maxent;

import de.saar.penguin.irtg.AnnotatedCorpus;
import de.saar.penguin.irtg.InterpretedTreeAutomaton;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public class MaximumEntropyIrtg extends InterpretedTreeAutomaton {
    private Map<String, FeatureFunction> features;
    private Map<String, Double> weights;

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
    public TreeAutomaton parseFromReaders(Map<String, Reader> readers) throws ParserException, IOException {
        TreeAutomaton chart = super.parseFromReaders(readers);

        // TODO: set weights in chart according to features and weights

        return chart;
    }

    public void train(AnnotatedCorpus corpus) {
        // TODO: learn the weights
    }

    public void readWeights(Reader reader) {
        // TODO: read weights from the reader
    }

    public void writeWeights(Writer writer) {
        // TODO: print weights to the writer
    }
}
