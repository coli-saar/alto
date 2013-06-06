/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Corpus implements Iterable<Instance> {
    private List<Instance> instances;
    private Charts charts;
    private boolean isAnnotated;

    public Corpus() {
        instances = new ArrayList<Instance>();
        charts = null;
        isAnnotated = false;
    }

    public boolean isAnnotated() {
        return isAnnotated;
    }

    public boolean hasCharts() {
        return charts != null;
    }

    public int getNumberOfInstances() {
        return instances.size();
    }

    public Iterator<Instance> iterator() {
        if (hasCharts()) {
            return new ZipIterator<Instance, TreeAutomaton, Instance>(instances.iterator(), charts.iterator()) {
                @Override
                public Instance zip(Instance left, TreeAutomaton right) {
                    return left.withChart(right);
                }
            };
        } else {
            return instances.iterator();
        }
    }

    public static Corpus readAnnotatedCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        return readCorpus(reader, irtg, true);
    }

    public static Corpus readUnannotatedCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException, CorpusReadingException {
        return readCorpus(reader, irtg, false);
    }

    private static Corpus readCorpus(Reader reader, InterpretedTreeAutomaton irtg, boolean annotated) throws IOException, CorpusReadingException {
        Corpus ret = new Corpus();
        ret.isAnnotated = annotated;

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

                    try {
                        Object inputObject = irtg.parseString(current, line);
                        currentInputs.put(current, inputObject);
                    } catch (ParserException ex) {
                        throw new CorpusReadingException("An error occurred while parsing " + reader + ", line " + (lineNumber + 1) + ": " + ex.getMessage());
                    }

                    currentInterpretationIndex++;
                } else {
                    Instance inst = new Instance();
                    inst.setInputObjects(currentInputs);

                    if (annotated) {
                        Tree<String> derivationTree = TreeParser.parse(line);
                        inst.setDerivationTree(derivationTree);
                    }

                    ret.instances.add(inst);

                    currentInputs = new HashMap<String, Object>();
                    currentInterpretationIndex = 0;
                }
            }

            lineNumber++;
        }
    }
}
