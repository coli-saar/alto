/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

/**
 *
 * @author koller
 */
public class ParserException extends Exception {

    public ParserException(Throwable thrwbl) {
        super(thrwbl);
    }

    public ParserException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public ParserException(String string) {
        super(string);
    }

    public ParserException() {
    }
    
}
