/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

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
        
    }
    
    
    /**
     * prints the results to System.err.
     */
    public static class PrintingManager implements ResultManager {

        @Override
        public synchronized void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
            if (doExport) {
                String resString = (result == null) ? "NULL" : result.toString();
                System.out.println("'"+name+"' for instance "+instanceID+": "+resString);
            }
        }

        @Override
        public synchronized void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
            System.out.println("Time '"+name+"' for instance "+instanceID+": "+time+" ms");
        }

        @Override
        public synchronized void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
            System.out.println("ERROR when computing '"+name+"': "+Util.getStackTrace(error));
        }
        
    }
    
    //public static class StoringManager -- maybe implement later
    
}
