/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.shell;

import de.saar.penguin.irtg.InterpretedTreeAutomaton;
import de.saar.penguin.irtg.IrtgParser;
import de.saar.penguin.irtg.ParseException;
import de.up.ling.shell.CallableFromShell;
import de.up.ling.shell.Shell;
import de.up.ling.shell.ShutdownShellException;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author koller
 */
public class Main {
    private static final String OUTPUT_END_MARKER = "---";
    
    public static void main(String[] args) throws IOException {
        int serverPort = 0;
        
        for (int i = 0; i < args.length; i++) {
            if ("--server".equals(args[i])) {
                serverPort = Integer.parseInt(args[++i]);
            }
        }

        Main x = new Main();
        Shell shell = new Shell();


        if (serverPort > 0) {
            System.out.println("IRTG server listening on port " + serverPort + " ...");
            shell.setOutputEndMarker(OUTPUT_END_MARKER);
//            shell.setVerbose(true);
            shell.startServer(x, serverPort);
        } else {
            shell.run(x);
        }
    }

    @CallableFromShell
    public InterpretedTreeAutomaton irtg(Reader reader) throws ParseException {
        return IrtgParser.parse(reader);
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
}
