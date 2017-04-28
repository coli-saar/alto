/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

/**
 *
 * @author koller
 */
public class FsParsingException extends Exception {
    public FsParsingException() {
    }

    public FsParsingException(String message) {
        super(message);
    }

    public FsParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FsParsingException(Throwable cause) {
        super(cause);
    }

    public FsParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
