/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.util.Util;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Jonas
 */
class DBResultManager implements ResultManager {

    private final DBLoader dbLoader;
    private final int experimentID;
    private final Consumer<SQLException> sqlErrorHandler;
    private final boolean showResults;
    
//        public int startExperiment(int taskId, String comment, String hostname, Map<String,String> varRemapper, List<String> dataIDs) throws Exception;
    

    public static DBResultManager startExperiment(DBLoader dbLoader, int taskId, String comment, String hostname, Map<String,String> varRemapper, List<String> dataIDs) throws Exception {
        int experimentID = dbLoader.uploadExperimentStartData(taskId, comment, hostname, varRemapper, dataIDs);
        return new DBResultManager(dbLoader, experimentID, x -> System.err.println(x), false);
    }
    
    private DBResultManager(DBLoader dbLoader, int experimentID, Consumer<SQLException> sqlErrorHandler, boolean showResults) {
        this.dbLoader = dbLoader;
        this.experimentID = experimentID;
        this.sqlErrorHandler = sqlErrorHandler;
        this.showResults = showResults;
    }
    
    @Override
    public synchronized void acceptResult(Object result, int instanceID, String name, boolean doExport, boolean isGlobal, boolean isNumeric) {
        if (doExport) {
            if (showResults) {
                new ResultManager.PrintingManager().acceptResult(result, instanceID, name, doExport, isGlobal, isNumeric);
            }
            try {
                dbLoader.uploadExperimentResult(result, name, instanceID, experimentID, isGlobal, isNumeric);
            } catch (SQLException ex) {
                sqlErrorHandler.accept(ex);
            }
        }
    }

    @Override
    public synchronized void acceptTime(long time, int instanceID, String name, boolean isGlobal) {
        try {
            //note that the long will be converted to double, possibly lossy, but no too large values are expected here.
            dbLoader.uploadExperimentResult((Long)time, name+" [ms]", instanceID, experimentID, isGlobal, true);
        } catch (SQLException ex) {
            sqlErrorHandler.accept(ex);
        }
    }

    @Override
    public synchronized void acceptError(Throwable error, int instanceID, String name, boolean doExport, boolean isGlobal) {
        System.err.println("Error during execution: "+Util.getStackTrace(error));
        if (doExport) {
            try {
                dbLoader.uploadExperimentResult(error, "ERROR for " + name, instanceID, experimentID, isGlobal, false);
            } catch (SQLException ex) {
                sqlErrorHandler.accept(ex);
            }
        }
    }

    @Override
    public void flush() {
        // do nothing - the DBResultManager flushes after each accept* operation
    }

    @Override
    public int getExperimentID() {
        return experimentID;
    }

    @Override
    public void finish() throws SQLException {
        dbLoader.updateExperimentStateFinished(getExperimentID());
    }
    
}
