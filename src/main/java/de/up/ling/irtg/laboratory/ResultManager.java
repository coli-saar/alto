/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.util.Util;
import java.io.IOException;

/**
 * Handles the results obtained during parsing. For example, storing them,
 * uploading them into a database, etc.
 * @author groschwitz
 */
public interface ResultManager {
    
    /**
     * Accpets a result for the variable with the given name, for the given instance.
     * @param isNumeric whether the result is of numeric value (double, int,... or the wrapper class). Relevant mostly if result is null.
     */
    void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric);
    
    /**
     * Accepts a runtime for the watch with the given name, for the given instance.
     * Time is in ms.
     */
    void acceptTime(long time, int instanceID, String name, boolean isGlobal);
    
    /**
     * If an error occurred during the computation of the variable with the given name,
     * this is called. AcceptResult is still called with result null.
     */
    void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal);
    
    /**
     * Send data to the persistent storage. For efficiency reasons, a ResultManager
     * may cache results as they are presented to it. It will then only send the
     * cached results to the persistent storage (database, file, etc.) when the
     * flush() method is called. Notice the risk-efficiency tradeoff: flushing
     * rarely will increase efficiency, but runs the risk of losing unflushed
     * data.
     * 
     */
    void flush() throws IOException;
    
    /**
     * Returns the experiment ID in this result manager.
     * 
     */
    int getExperimentID();
    
    /**
     * Marks the experiment as finished.
     */
    void finish() throws Exception;
    
    
    /**
     * Does literally nothing.
     */
    class DummyManager implements ResultManager {

        @Override
        public void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
            
        }

        @Override
        public void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
            
        }

        @Override
        public void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
            String inst = (instanceID >= 0) ? ("in instance #" + instanceID) : "(global)";
            
            System.err.println("ERROR " + inst + ": " + error.toString());
            System.err.println(Util.getStackTrace(error));
            System.err.println();
        }

        @Override
        public void flush() {
        }

        @Override
        public int getExperimentID() {
            return 0;
        }

        @Override
        public void finish() {
            
        }
        
    }
    
    
    /**
     * prints the results to System.err.
     */
    class PrintingManager implements ResultManager {
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

        @Override
        public int getExperimentID() {
            return 0;
        }

        @Override
        public void finish() {
            System.out.println("Experiment finished.");
        }
    }
    
}
