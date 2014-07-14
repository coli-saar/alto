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
        if (logger == null) {
            logger = Logger.getLogger(Logging.class.getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);
        }
    }

    public static Logger get() {
        setUp();
        return logger;
    }

    /**
     * The level of the handler is set to the current level of the logger.
     * Thus it is probably good practice to set the logger level first,
     * and then set the handler.
     * 
     * @param handler 
     */
    public static void setHandler(Handler handler) {
        setUp();

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        handler.setFormatter(new MyFormatter());
        logger.addHandler(handler);
        handler.setLevel(logger.getLevel());
    }

    public static void setConsoleHandler() {
        setHandler(new ConsoleHandler());
    }

    private static class MyFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return record.getMessage() + "\n";
        }
    }
}
