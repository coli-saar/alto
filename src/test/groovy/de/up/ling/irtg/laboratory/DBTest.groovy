/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.laboratory

import static org.junit.Assert.*
import org.junit.Test

/**
 *
 * @author groschwitz
 */
class DBTest {
    
    //activate this test to check database format and connectivity. Note that this requires a database.data
    //file with the connection and login data for your database (see DBLoader#connect). It adds an experiment
    //to your database, so you might not want to execute it too often, in order to not crowd your database too much.
    //If your database contains a different set of tasks, replace both occurences of '33' in the
    //test code with the taskID of the task you want to use as a test.
    //TODO: make this activatable using JUnit categories.
     //@Test
     public void simpleAMRTest() {
        String[] input = new String[2];
        input[0] = "-taskID";
        input[1] = "33";
        int experimentID = CommandLineInterface.run(input);
        final DBLoader dbLoader = new DBLoader();
        dbLoader.connect();
        assert dbLoader.getMatchingTaskIDFromDB(experimentID) == 33;
        assert dbLoader.getNumericMeasurement(experimentID, 0, "total [ms]") >= 0;
     }
}
