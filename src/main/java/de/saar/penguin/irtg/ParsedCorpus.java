/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.basic.StringTools;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import de.up.ling.shell.CallableFromShell;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author koller
 */
public class ParsedCorpus implements Serializable {
    private List<BottomUpAutomaton> instances;

    public ParsedCorpus() {
        this.instances = new ArrayList<BottomUpAutomaton>();
    }
    
    public void addInstance(BottomUpAutomaton inst) {
        instances.add(inst);
    }
    
    public List<BottomUpAutomaton> getAllInstances() {
        return instances;
    }
    
    @CallableFromShell
    public BottomUpAutomaton getInstance(Reader r) throws IOException {
        String numString = StringTools.slurp(r);
        return instances.get(Integer.parseInt(numString));
    }
    
    @CallableFromShell
    public void write(Reader filenameReader) throws IOException {
        String filename = StringTools.slurp(filenameReader);
        File file = new File(filename);
        FileOutputStream ostream = new FileOutputStream(file);
        
        write(ostream);
        ostream.close();
        
        System.out.println("[wrote corpus to " + filename + ", " + file.length() + " bytes]");
    }
    
    public void write(OutputStream ostream) throws IOException {
        GZIPOutputStream gz = new GZIPOutputStream(ostream);
        ObjectOutputStream oostream = new ObjectOutputStream(gz);
        oostream.writeObject(this);
        oostream.flush();
        gz.finish();
    }
    
    public static ParsedCorpus read(InputStream istream) throws IOException, ClassNotFoundException {
        ObjectInputStream oistream = new ObjectInputStream(new GZIPInputStream(istream));
        ParsedCorpus ret = (ParsedCorpus) oistream.readObject();        
        return ret;
    }

    @Override
    public String toString() {
        return "[parsed corpus with " + instances.size() + " instances]";
    }
    
    
}
