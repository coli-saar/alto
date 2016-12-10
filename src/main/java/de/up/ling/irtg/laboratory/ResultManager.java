/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.sun.org.apache.xml.internal.utils.StringBufferPool;
import de.up.ling.irtg.util.Util;

/**
 * Handles the results obtained during parsing. For example, storing them,
 * uploading them into a database, etc.
 * @author groschwitz
 */
public interface ResultManager {
    
    /**
     * Accpets a result for the variable with the given name, for the given instance.
     * @param result
     * @param instanceID
     * @param name
     * @param doExport
     * @param isGlobal 
     * @param isNumeric whether the result is of numeric value (double, int,... or the wrapper class). Relevant mostly if result is null.
     */
    public void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric);
    
    /**
     * Accepts a runtime for the watch with the given name, for the given instance.
     * Time is in ms.
     * @param time
     * @param instanceID
     * @param name 
     * @param isGlobal 
     */
    public void acceptTime(long time, int instanceID, String name, boolean isGlobal);
    
    /**
     * If an error occurred during the computation of the variable with the given name,
     * this is called. AcceptResult is still called with result null.
     * @param error
     * @param instanceID
     * @param name
     * @param doExport
     * @param isGlobal 
     */
    public void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal);
    
    /**
     * Send data to the persistent storage. For efficiency reasons, a ResultManager
     * may cache results as they are presented to it. It will then only send the
     * cached results to the persistent storage (database, file, etc.) when the
     * flush() method is called. Notice the risk-efficiency tradeoff: flushing
     * rarely will increase efficiency, but runs the risk of losing unflushed
     * data.
     * 
     */
    public void flush();
    
    /**
     * Does literally nothing.
     */
    public static class DummyManager implements ResultManager {

        @Override
        public void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
            
        }

        @Override
        public void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
            
        }

        @Override
        public void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
            
        }

        @Override
        public void flush() {
        }
        
    }
    
    
    /**
     * prints the results to System.err.
     */
    public static class PrintingManager implements ResultManager {
        private StringBuilder buf = new StringBuilder();

        @Override
        public synchronized void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
            if (doExport) {
                String resString = (result == null) ? "NULL" : result.toString();
                buf.append("'"+name+"' for instance "+instanceID+": "+resString + "\n");
            }
        }

        @Override
        public synchronized void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
            buf.append("Time '"+name+"' for instance "+instanceID+": "+time+" ms" + "\n");
        }

        @Override
        public synchronized void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
            buf.append("ERROR when computing '"+name+"': "+Util.getStackTrace(error) + "\n");
        }

        @Override
        public synchronized void flush() {
            System.out.print(buf.toString());
            System.out.flush();
            buf.setLength(0);
        }        
    }
    
}
