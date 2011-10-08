/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.shell;

import de.saar.penguin.irtg.InterpretedTreeAutomaton;
import de.saar.penguin.irtg.IrtgParser;
import de.saar.penguin.irtg.automata.BottomUpAutomaton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class Main {
    private static Map<String, Object> variables = new HashMap<String, Object>();
    private static boolean quiet = false;

    public static void main(String[] args) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        parseCommandLineArgs(args);

        while (true) {
            if (!quiet) {
                System.out.print("> ");
            }
            String line = console.readLine();

            if (line == null) {
                System.exit(0);
            } else {
                try {
                    Expression expr = ShellParser.parse(new StringReader(line));
                    Object val = evaluate(expr);

                    if (!quiet) {
                        if (val != null) {
                            System.out.println(val);
                        }
                    }
                } catch (ParseException ex) {
                    System.out.println("Syntax error: " + ex.getMessage());
                } catch (Exception ex) {
                    System.out.println("An error occurred!\n" + ex.getMessage());
                }
            }
        }
    }

    private static Object evaluate(Expression expr) throws de.saar.penguin.irtg.ParseException, FileNotFoundException, IOException {
        InterpretedTreeAutomaton irtg;
        BottomUpAutomaton automaton;

        switch (expr.type) {
            case VARIABLE:
                return variables.get(expr.getString(0));
            case ASSIGN:
                variables.put(expr.getString(0), evaluate(expr.getExpression(1)));
                return null;
            case LOAD:
                irtg = IrtgParser.parse(new FileReader(expr.getString(0)));
                return irtg;
            case PARSE:
                try {
                    irtg = (InterpretedTreeAutomaton) evaluate(expr.getExpression(0));
                    Map<String, Object> inputs = irtg.parseStrings((Map<String, String>) expr.arguments.get(1));
                    return irtg.parse(inputs).reduce();
                } catch (de.saar.penguin.irtg.algebra.ParseException e) {
                    System.out.println("Parsing error: " + e.getMessage());
                    return null;
                }
            case VITERBI:
                automaton = (BottomUpAutomaton) evaluate(expr.getExpression(0));
                return automaton.viterbi();
            case EMTRAIN:
                irtg = (InterpretedTreeAutomaton) evaluate(expr.getExpression(0));
                irtg.trainEM(readTrainingData(new File(expr.getString(1)), irtg));
                return null;
            case QUIT:
                System.exit(0);
            case PRINT:
                Object val = evaluate(expr.getExpression(0));
                if (val != null) {
                    System.out.println(val);
                }
                return null;
            default:
                return null;
        }
    }

    private static List<Map<String, Object>> readTrainingData(File file, InterpretedTreeAutomaton irtg) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        List<String> interpretationOrder = new ArrayList<String>();
        Map<String, Object> currentTuple = new HashMap<String, Object>();
        int currentInterpretation = 0;
        int lineNumber = 0;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                return ret;
            }

            if (lineNumber < irtg.getInterpretations().size()) {
                interpretationOrder.add(line);
            } else {
                String current = interpretationOrder.get(currentInterpretation);
                try {
                    currentTuple.put(current, irtg.parseString(current, line));
                } catch (de.saar.penguin.irtg.algebra.ParseException ex) {
                    System.out.println("An error occurred while parsing " + file + ", line " + (lineNumber + 1) + ": " + ex.getMessage());
                    return null;
                }

                currentInterpretation++;
                if (currentInterpretation >= interpretationOrder.size()) {
                    ret.add(currentTuple);
                    currentTuple = new HashMap<String, Object>();
                    currentInterpretation = 0;
                }
            }

            lineNumber++;
        }
    }

    private static void parseCommandLineArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--quiet".equals(args[i]) || "-q".equals(args[i])) {
                quiet = true;
            }
        }
    }
}
