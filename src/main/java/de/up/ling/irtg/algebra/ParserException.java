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

    /**
     * Creates an instance from the given Throwable by calling the superconstructor with it.
     * 
     * @param thrwbl 
     */
    public ParserException(Throwable thrwbl) {
        super(thrwbl);
    }

    /**
     * Creates an instance from the given Throwable and String by calling the superconstructor with it.
     * 
     * @param string
     * @param thrwbl 
     */
    public ParserException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    /**
     * Creates an instance from the given String by calling the superconstructor with it.
     * 
     * @param string
     * @param thrwbl 
     */
    public ParserException(String string) {
        super(string);
    }

    /**
     * Creates an instance using the default superconstructor.
     */
    public ParserException() {
    }
    
}
