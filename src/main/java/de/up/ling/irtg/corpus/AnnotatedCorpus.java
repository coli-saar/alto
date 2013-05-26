/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.corpus.AnnotatedCorpus.Instance;
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
public class AnnotatedCorpus  { //implements Iterable<Instance>
//    private static final String MARKER = "IrtgAnnotatedCorpusWithCharts";

    public static class Instance {
        private Tree<String> tree;
        private Map<String, Object> inputObjects;

        public Instance(Tree<String> tree, Map<String, Object> inputObjects) {
            this.tree = tree;
            this.inputObjects = inputObjects;
        }

        public Tree<String> getTree() {
            return tree;
        }

        public Map<String, Object> getInputObjects() {
            return inputObjects;
        }

        @Override
        public String toString() {
            return "Instance{" + "tree=" + tree + ", inputObjects=" + inputObjects + '}';
        }
    }
    private List<Instance> instances;

    public AnnotatedCorpus() {
        instances = new ArrayList<AnnotatedCorpus.Instance>();
    }
    
    public int getNumberOfInstances() {
        return instances.size();
    }

    public Iterable<Instance> getInstances() {
        return instances;
    }

    public static AnnotatedCorpus readAnnotatedCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException {
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

    /*
    public static void parseAnnotatedCorpusWithCharts(Reader reader, final InterpretedTreeAutomaton irtg, OutputStream ostream, final String relevantInputInterpretation) throws IOException {
        AnnotatedCorpus ann = readAnnotatedCorpus(reader, irtg);
        final CorpusCreator creator = new CorpusCreator(MARKER, ostream);
        int lineNumber = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

        for (final Instance inst : ann) {
            final int l = lineNumber++;

            tasks.add(new Callable<Void>() {
                public Void call() throws Exception {
                    if (relevantInputInterpretation != null) {
                        String line = inst.inputObjects.get(relevantInputInterpretation).toString();
                        System.out.println(String.format("%4d %-60.60s%s", l, line, (line.length() > 60 ? "... " : "    ")));
                        System.out.flush();
                    }

                    long startTime = System.currentTimeMillis();

                    try {
                        Map<String, Object> mapToParse = new HashMap<String, Object>();
                        mapToParse.put(relevantInputInterpretation, inst.inputObjects.get(relevantInputInterpretation));


                        TreeAutomaton chart = irtg.parseInputObjects(mapToParse);

                        synchronized (creator) {
                            creator.addInstance(new Pair(inst.tree.toString(), chart));
                        }

                        if (relevantInputInterpretation != null) {
                            System.out.println(" --> " + l + " done, " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
                            System.out.flush();
                        }
                    } catch (Throwable e) {
                        if (relevantInputInterpretation != null) {
                            System.out.println(" ==> " + l + " error: " + e);
                            e.printStackTrace();
                            System.out.flush();
                        }
                    }

                    return null;
                }
            });
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Logger.getLogger(AnnotatedCorpus.class.getName()).log(Level.SEVERE, null, ex);
        }

        creator.finished();
    }
    */

    @Override
    public String toString() {
        return "[annotated corpus with " + instances.size() + " instances]";
    }

    public Iterator<Instance> iterator() {
        return instances.iterator();
    }
}
