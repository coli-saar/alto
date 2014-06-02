/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.base.Supplier;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.ProgressListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.up.ling.zip.ZipEntriesCreator;
import de.up.ling.zip.ZipEntryIterator;

/**
 * A collection of parse charts for the input objects in a corpus. Use the
 * method {@link Charts#computeCharts(de.up.ling.irtg.corpus.Corpus, de.up.ling.irtg.InterpretedTreeAutomaton, java.io.OutputStream)
 * }
 * to compute these parse charts for a given corpus and store them in a file.
 * You can then attach the charts in the file to a corpus using {@link Corpus#attachCharts(java.lang.String)
 * }.
 *
 * @author koller
 */
public class Charts implements Iterable<TreeAutomaton> {

    private Supplier<InputStream> supplier;

    public Charts(Supplier<InputStream> supplier) {
        this.supplier = supplier;
    }

    public Iterator<TreeAutomaton> iterator() {
        try {
            return new ZipEntryIterator<TreeAutomaton>(supplier.get());
        } catch (IOException ex) {
            Logger.getLogger(Charts.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, OutputStream ostream) throws IOException {
        computeCharts(corpus, irtg, ostream, null);
    }

    public static void computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, OutputStream ostream, ProgressListener listener) throws IOException {
        ZipEntriesCreator zec = new ZipEntriesCreator(ostream);
        int i = 0;
        int n = corpus.getNumberOfInstances();

        for (Instance inst : corpus) {
            if (listener != null) {
                listener.accept(i, n, "Computed " + i + "/" + n + " charts.");
            }

            TreeAutomaton chart = irtg.parseInputObjects(inst.getInputObjects());
            ConcreteTreeAutomaton x = chart.asConcreteTreeAutomaton();
            zec.add(x);

            i++;
        }

        zec.close();
    }
}
