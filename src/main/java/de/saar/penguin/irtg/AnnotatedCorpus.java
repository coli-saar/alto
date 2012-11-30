/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.penguin.irtg.algebra.ParserException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class AnnotatedCorpus {
    public static class Instance {
        public Tree<String> tree;
        public Map<String, Object> inputObjects;

        public Instance(Tree<String> tree, Map<String, Object> inputObjects) {
            this.tree = tree;
            this.inputObjects = inputObjects;
        }
    }
    private List<Instance> instances;

    public AnnotatedCorpus() {
        instances = new ArrayList<AnnotatedCorpus.Instance>();
    }

    public List<Instance> getInstances() {
        return instances;
    }

    static AnnotatedCorpus readAnnotatedCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException {
        AnnotatedCorpus ret = new AnnotatedCorpus();
        BufferedReader br = new BufferedReader(reader);
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        int currentInterpretationIndex = 0;
        int lineNumber = 0;

        while (true) {
            String line = br.readLine();

            if (line == null) {
                return ret;
            }

            if (line.equals("")) {
                continue;
            }

            if (lineNumber < irtg.getInterpretations().size()) {
                interpretationOrder.add(line);
            } else {
                if (currentInterpretationIndex < interpretationOrder.size()) {
                    String current = interpretationOrder.get(currentInterpretationIndex);
                    Interpretation currentInterpretation = irtg.getInterpretations().get(current);

                    try {
                        Object inputObject = irtg.parseString(current, line);
                        currentInputs.put(current, inputObject);
                    } catch (ParserException ex) {
                        System.out.println("An error occurred while parsing " + reader + ", line " + (lineNumber + 1) + ": " + ex.getMessage());
                        return null;
                    }

                    currentInterpretationIndex++;
                } else {
                    Tree<String> derivationTree = TreeParser.parse(line);
                    Instance inst = new Instance(derivationTree, currentInputs);
                    ret.instances.add(inst);

                    currentInputs = new HashMap<String, Object>();
                    currentInterpretationIndex = 0;
                }
            }

            lineNumber++;
        }
    }
    
    @Override
    public String toString() {
        return "[annotated corpus with " + instances.size() + " instances]";
    }
}
