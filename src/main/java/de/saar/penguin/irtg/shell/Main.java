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
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author koller
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Main x = new Main();
        Shell shell = new Shell();
        
        shell.run(x);
    }
    
    @CallableFromShell
    public InterpretedTreeAutomaton irtg(Reader reader) throws ParseException {
        return IrtgParser.parse(reader);
    }
    
    @CallableFromShell
    public void quit() {
        System.exit(0);
    }

    @CallableFromShell
    public void println(Object val) {
        if (val != null) {
            System.out.println(val);
        }
    }
    
    @CallableFromShell
    public void type(Object val) {
        if( val ==  null ) {
            System.out.println(val);
        } else {
            System.out.println(val.getClass());
        }
    }
}
