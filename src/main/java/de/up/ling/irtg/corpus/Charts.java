/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.base.Supplier;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.up.ling.zip.ZipEntriesCreator;
import de.up.ling.zip.ZipEntryIterator;

/**
 *
 * @author koller
 */
public class Charts implements Iterable<TreeAutomaton> {
    Supplier<InputStream> supplier;
    
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

    public static void computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, OutputStream ostream) throws Exception {
        ZipEntriesCreator zec = new ZipEntriesCreator(ostream);
        
        for (Instance inst : corpus) {
            TreeAutomaton chart = irtg.parseInputObjects(inst.getInputObjects());
            ConcreteTreeAutomaton x = chart.asConcreteTreeAutomaton();
            zec.add(x);
        }
        
        zec.close();
    }
}
