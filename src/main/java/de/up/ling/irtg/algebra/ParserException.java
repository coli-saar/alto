/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

/**
 * An exception that indicates that a string representation
 * could not successfully be resolved into an object of the
 * algebra.
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
