/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.shell;

import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.ParseException;
import de.up.ling.irtg.corpus.ChartCorpus;
import de.up.ling.irtg.corpus.Charts;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.shell.Shell;
import de.up.ling.shell.ShutdownShellException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author koller
 */
public class Main {
    private static final String OUTPUT_END_MARKER = "---";
    private static final String ERROR_MARKER = "*** ";
    private Shell shell;

    public static void main(String[] args) throws Exception {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader("examples/cfg.irtg"));
        Corpus corpus = Corpus.readUnannotatedCorpus(new FileReader("examples/pcfg-training.irtg"), irtg);
        Charts.computeCharts(corpus, irtg, "parsed-corpus.odb");
        
        System.exit(0);
        
        
        
        
        int serverPort = 0;

        for (int i = 0; i < args.length; i++) {
            if ("--server".equals(args[i])) {
                serverPort = Integer.parseInt(args[++i]);
            }
        }

        Shell shell = new Shell();
        Main x = new Main(shell);

        if (serverPort > 0) {
            System.out.println("IRTG server listening on port " + serverPort + " ...");
            shell.setOutputEndMarker(OUTPUT_END_MARKER);
            shell.setErrorMarker(ERROR_MARKER);
            shell.startServer(x, serverPort);
        } else {
            shell.run(x);
        }
    }

    public Main(Shell shell) {
        this.shell = shell;
    }

    @CallableFromShell
    public InterpretedTreeAutomaton irtg(Reader reader) throws ParseException {
        return IrtgParser.parse(reader);
    }
    
    @CallableFromShell
    public ChartCorpus readParsedCorpus(Reader reader) throws IOException, ClassNotFoundException {
        String filename = StringTools.slurp(reader);
        return new ChartCorpus(new File(filename));
    }

    @CallableFromShell
    public void quit() throws ShutdownShellException {
        throw new ShutdownShellException();
    }

    @CallableFromShell
    public void println(Object val) {
        if (val != null) {
            System.out.println(val);
        }
    }

    @CallableFromShell
    public void type(Object val) {
        if (val == null) {
            System.out.println(val);
        } else {
            System.out.println(val.getClass());
        }
    }

    @CallableFromShell
    public void measure() {
        shell.setMeasureExecutionTime(true);
    }

    @CallableFromShell
    public void nomeasure() {
        shell.setMeasureExecutionTime(false);
    }
}
