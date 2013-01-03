/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterators;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.shell.CallableFromShell;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class ChartCorpus implements Serializable, Iterable<TreeAutomaton> {
    private static final String MARKER = "IrtgChartCorpus";
    private Supplier<InputStream> streamSupplier;
    private ObjectInputStream inputStream;

    public ChartCorpus(Supplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
        inputStream = null;
    }

    public ChartCorpus(final File file) {
        this(new Supplier<InputStream>() {
            public InputStream get() {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ChartCorpus.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            }
        });
    }

    /**
     * Avoid using this for large corpora. It is preferred to simply obtain the
     * iterator.
     *
     * @return
     */
    public List<TreeAutomaton> getInstancesAsList() {
        List<TreeAutomaton> ret = new ArrayList<TreeAutomaton>();
        Iterators.addAll(ret, iterator());
        return ret;
    }

    /**
     * Expensive.
     *
     * @param r
     * @return
     * @throws IOException
     */
    @CallableFromShell
    public TreeAutomaton getInstance(Reader r) throws IOException {
        String numString = StringTools.slurp(r);
        return getInstancesAsList().get(Integer.parseInt(numString));
    }

    public static void parseCorpus(Reader reader, InterpretedTreeAutomaton irtg, OutputStream ostream) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        CorpusCreator creator = new CorpusCreator(MARKER, ostream);
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentInputs = new HashMap<String, Object>();
        int currentInterpretationIndex = 0;
        int lineNumber = 0;
        String firstLineOfBlock = null;
        long startTime = 0;

        while (true) {
            String line = br.readLine();

            if (line == null) {
                break;
            }

            if (line.equals("")) {
                continue;
            }

            if (lineNumber < irtg.getInterpretations().size()) {
                interpretationOrder.add(line);
            } else {
                if (firstLineOfBlock == null) {
                    firstLineOfBlock = line;
                    System.out.print(String.format("%4d %-40.40s%s", lineNumber + 1, line, (line.length() > 40 ? "... " : "    ")));
                    startTime = System.currentTimeMillis();
                }

                String current = interpretationOrder.get(currentInterpretationIndex);
                Interpretation currentInterpretation = irtg.getInterpretations().get(current);

                try {
                    Object inputObject = irtg.parseString(current, line);
                    currentInputs.put(current, inputObject);
                } catch (ParserException ex) {
                    System.out.println(" ==> error: " + ex.getMessage());
                    continue;
                }

                currentInterpretationIndex++;
                if (currentInterpretationIndex >= interpretationOrder.size()) {
                    TreeAutomaton chart = irtg.parseInputObjects(currentInputs);
                    chart.makeAllRulesExplicit();
                    chart = chart.reduceBottomUp();
                    creator.addInstance(chart);

                    System.out.println("done, " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
                    currentInputs.clear();
                    currentInterpretationIndex = 0;
                    firstLineOfBlock = null;
                }
            }

            lineNumber++;
        }

        creator.finished();
    }

    @Override
    public String toString() {
        return "[parsed corpus]";
    }

    public Iterator<TreeAutomaton> iterator() {
        try {
            return new InstanceIterator(streamSupplier);
        } catch (IOException ex) {
            Logger.getLogger(ChartCorpus.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static class InstanceIterator implements Iterator<TreeAutomaton> {
        private ObjectInputStream istream;
        private boolean finished = false;
        private TreeAutomaton next = null;

        public InstanceIterator(Supplier<InputStream> supplier) throws IOException {
            istream = new ObjectInputStream(supplier.get());

            try {
                Object marker = istream.readObject();
                if (! MARKER.equals(marker)) {
                    finished = true;
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ChartCorpus.class.getName()).log(Level.SEVERE, null, ex);
                finished = true;
            }

            readNext();
        }

        private void readNext() {
            if (!finished) {
                try {
                    next = (TreeAutomaton) istream.readObject();
                } catch (Exception ex) {
                    finished = true;
                    next = null;
                }
            }
        }

        public boolean hasNext() {
            return !finished;
        }

        public TreeAutomaton next() {
            TreeAutomaton ret = next;
            readNext();
            return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
