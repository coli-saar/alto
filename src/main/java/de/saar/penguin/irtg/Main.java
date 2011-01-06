/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg;

import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author koller
 */
public class Main {
    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.print("> ");
            String line = console.readLine();
            String[] tokens = line.split(":/");
            Map<String,Object> inputObjects = new HashMap<String, Object>();

            for( int i = 0; i < tokens.length; i += 2 ) {
                inputObjects.put(tokens[i], tokens[i+1]);
            }

            BottomUpAutomaton chart = irtg.parse(inputObjects);
            System.out.println(chart.reduce() + "\n");
        }
    }
}
