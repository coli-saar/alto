/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author koller
 */
public class TrainEM {
    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        
    }
}
