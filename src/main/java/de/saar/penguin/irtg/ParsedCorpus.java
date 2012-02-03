/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.basic.StringTools;
import de.up.ling.shell.CallableFromShell;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author koller
 */
public class ParsedCorpus implements Serializable {
    private List<CorpusInstance> instances;

    public ParsedCorpus() {
        this.instances = new ArrayList<CorpusInstance>();
    }
    
    public void addInstance(CorpusInstance inst) {
        instances.add(inst);
    }
    
    public List<CorpusInstance> getAllInstances() {
        return instances;
    }
    
    @CallableFromShell
    public CorpusInstance getInstance(Reader r) throws IOException {
        String numString = StringTools.slurp(r);
        return instances.get(Integer.parseInt(numString));
    }
    
    @CallableFromShell
    public void write(Reader filenameReader) throws IOException {
        String filename = StringTools.slurp(filenameReader);
        ObjectOutputStream ostream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File(filename))));
        ostream.writeObject(this);
        ostream.flush();
        ostream.close();
    }
}
