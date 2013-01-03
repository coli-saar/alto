/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 *
 * @author koller
 */
public class CorpusCreator {
    private ObjectOutputStream ostream;
    
    public CorpusCreator(Object fileTypeMarker, OutputStream o) throws IOException {
        ostream = new ObjectOutputStream(o);
        ostream.writeObject(fileTypeMarker);
    }
    
    public void addInstance(TreeAutomaton inst) throws IOException {
        ostream.writeObject(inst);
    }
    
    public void finished() throws IOException {
        ostream.flush();
        ostream.close();
    }
}
