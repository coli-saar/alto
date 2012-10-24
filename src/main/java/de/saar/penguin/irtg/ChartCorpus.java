/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import de.saar.basic.StringTools;
import de.saar.penguin.irtg.algebra.ParserException;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.shell.CallableFromShell;
import java.io.BufferedReader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author koller
 */
public class ChartCorpus implements Serializable {
    private List<TreeAutomaton> instances;

    public ChartCorpus() {
        this.instances = new ArrayList<TreeAutomaton>();
    }
    
    public void addInstance(TreeAutomaton inst) {
        instances.add(inst);
    }
    
    public List<TreeAutomaton> getAllInstances() {
        return instances;
    }
    
    @CallableFromShell
    public TreeAutomaton getInstance(Reader r) throws IOException {
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
    
    public static ChartCorpus read(InputStream istream) throws IOException, ClassNotFoundException {
        ObjectInputStream oistream = new ObjectInputStream(new GZIPInputStream(istream));
        ChartCorpus ret = (ChartCorpus) oistream.readObject();        
        return ret;
    }
    
    public static ChartCorpus parseCorpus(Reader reader, InterpretedTreeAutomaton irtg) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        ChartCorpus ret = new ChartCorpus();
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
                if (currentInterpretationIndex >= interpretationOrder.size()) {
                    TreeAutomaton chart = irtg.parseInputObjects(currentInputs);
                    chart.makeAllRulesExplicit();
                    chart = chart.reduceBottomUp(); //.makeConcreteAutomaton();
                    ret.addInstance(chart);

                    currentInputs.clear();
                    currentInterpretationIndex = 0;
                }
            }

            lineNumber++;
        }
    }

    @Override
    public String toString() {
        return "[parsed corpus with " + instances.size() + " instances]";
    }
    
    
}
