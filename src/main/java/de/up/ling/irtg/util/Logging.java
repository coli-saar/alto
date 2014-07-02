/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class Logging {
    private static Logger logger = null;

    /**
     * * Set up logging **
     */
    public static void setUp() {
        LogManager logman = LogManager.getLogManager();
        logger = Logger.getLogger(Logging.class.getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }
    
    public static Logger get() {
        if( logger == null ) {
            setUp();
        }
        
        return logger;
    }
    
    public static void setHandler(Handler handler) {
        for( Handler h : logger.getHandlers() ) {
            System.err.println("remove: " + h);
            logger.removeHandler(h);
        }
        
        handler.setFormatter(new MyFormatter());
        logger.addHandler(handler);
    }
    
    public static void setConsoleHandler() {
        setHandler(new ConsoleHandler());
    }

    private static class MyFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage();
        }
    }
}
