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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class Main {
    private static Pattern pattern = Pattern.compile("[, ]*([^ :]*):\\s*\"([^\"]*)\"(.*)");

    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg = IrtgParser.parse(new FileReader(args[0]));
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.print("> ");
            String line = console.readLine();
            Map<String,Object> inputObjects = new HashMap<String, Object>();

            if( line == null ) {
                System.exit(0);
            }

            Matcher m = pattern.matcher(line);
            while( m.matches() ) {
                inputObjects.put(m.group(1), m.group(2));
                line = m.group(3);
                m = pattern.matcher(line);
            }

            BottomUpAutomaton chart = irtg.parse(inputObjects);
            BottomUpAutomaton reduced = chart.reduce();
            
            System.out.print(reduced);
            System.out.println(reduced.countTrees() + " derivation trees\n");
        }
    }
}
