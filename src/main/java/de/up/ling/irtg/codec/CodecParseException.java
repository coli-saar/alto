/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

/**
 * An exception that occurred while parsing an input representation.
 * 
 * @author koller
 */
public class CodecParseException extends RuntimeException {

    public CodecParseException() {
    }

    public CodecParseException(String message) {
        super(message);
    }

    public CodecParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecParseException(Throwable cause) {
        super(cause);
    }

    public CodecParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
