/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author koller
 */
public class TrainEM {
    private static final String INTERP = "interpretation: ";
    
    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        List<Map<String, Object>> trainingData = readTrainingData(new File(args[1]), irtg);
        irtg.trainEM(trainingData);
    }

    private static List<Map<String, Object>> readTrainingData(File file, InterpretedTreeAutomaton irtg) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String,Object> currentTuple = new HashMap<String, Object>();
        int currentInterpretation = 0;
        int lineNumber = 0;
        
        while( true ) {
            String line = reader.readLine();
            if( line == null ) {
                return ret;
            }
            
            if( lineNumber < irtg.getInterpretations().size() ) {                
                interpretationOrder.add(line);
            } else {
                String current = interpretationOrder.get(currentInterpretation);
                currentTuple.put(current, irtg.parseString(current, line));
                
                currentInterpretation++;
                if( currentInterpretation >= interpretationOrder.size() ) {
                    ret.add(currentTuple);
                    currentTuple = new HashMap<String, Object>();
                    currentInterpretation = 0;
                }
            }
            
            lineNumber++;
        }
    }
}
