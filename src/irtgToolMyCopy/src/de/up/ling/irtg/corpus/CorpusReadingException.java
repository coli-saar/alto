/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

/**
 * An exception that indicates that something went wrong
 * when reading a corpus from a reader.
 * 
 * @author koller
 */
public class CorpusReadingException extends Exception {
    public CorpusReadingException(String message) {
        super(message);
    }    
}
