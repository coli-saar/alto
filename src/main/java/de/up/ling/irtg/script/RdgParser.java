/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.FeatureStructureAlgebra;
import de.up.ling.irtg.algebra.RdgStringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.gui.JLanguageViewer;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import jline.console.ConsoleReader;

/**
 *
 * @author koller
 */
public class RdgParser {

    private static String filename;
    private static InterpretedTreeAutomaton irtg;
    private static FeatureStructureAlgebra fsa;
    private static RdgStringAlgebra sa;
    private static Homomorphism fh;

    private static JCommander jc;
    private static CmdLineParameters param = new CmdLineParameters();

    private static void reloadGrammar() throws Exception {
        System.err.printf("Reading grammar from %s ...\n", filename);

        long start = System.nanoTime();
        InputCodec<InterpretedTreeAutomaton> ic = InputCodec.getInputCodecByNameOrExtension(filename, null);
        irtg = ic.read(new FileInputStream(filename));
        System.err.printf("Done, read RDG grammar in %s\n\n", Util.formatTimeSince(start));

        sa = (RdgStringAlgebra) irtg.getInterpretation("string").getAlgebra();
    }

    private static class Command {

        private String command;
        private String description;
        private CommandAction action;

        public Command(String command, String description, CommandAction action) {
            this.command = command;
            this.description = description;
            this.action = action;
        }
    }

    private interface CommandAction {

        void perform() throws Exception;
    }

    private static List<Command> commands = Arrays.asList(
            new Command("quit", "Quits the parser.", () -> System.exit(0)),
            new Command("help", "Prints this help text.", () -> usage()),
            new Command("reload", "Reloads the grammar.", () -> reloadGrammar())
    );

    private static void usage() {
        int maxLen = commands.stream().mapToInt(c -> c.command.length()).max().getAsInt();
        String format = String.format("%%-%ds  %%s\n", maxLen);

        for (Command c : commands) {
            System.out.printf(format, c.command, c.description);
        }
    }

    private static void cmdlineUsage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println(errorMessage);
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.RdgParser <grammar_filename>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        jc = new JCommander(param, args);

        if (param.help) {
            cmdlineUsage(null);
        }

        if (param.grammarFilename.isEmpty()) {
            cmdlineUsage("No grammar file specified.");
        }

        System.err.println("Alto RDG parser, v1.0");
        System.err.println("Type a sentence to parse it, or type 'help' for help.\n");

        Logger rootLogger = Logger.getLogger("");
        rootLogger.getHandlers()[0].setFormatter(new MyFormatter());
        RdgStringAlgebra.getLogger().setLevel(Level.WARNING);

        filename = param.grammarFilename.get(0);
        reloadGrammar();

        ConsoleReader cr = new ConsoleReader();
        cr.setPrompt("parse> ");

        String line;

        mainLoop:
        while ((line = cr.readLine()) != null) {
            // check whether line was a command
            for (Command c : commands) {
                if (c.command.equalsIgnoreCase(line)) {
                    c.action.perform();
                    continue mainLoop;
                }
            }

            // otherwise, parse sentence
            long start = System.nanoTime();
            Object inp = sa.parseString(line);
            TreeAutomaton chart = irtg.parseWithSiblingFinder("string", inp);
            TreeAutomaton filtered = chart;
            System.out.printf("computed chart: %s\n", Util.formatTimeSince(start));

            Tree<String> dt = chart.viterbi();
            if (dt == null) {
                System.out.printf("No parse found.\n");
                System.out.println();
                continue mainLoop;
            }

            JLanguageViewer lv = new JLanguageViewer();
            lv.setAutomaton(filtered, irtg);
            lv.setTitle(String.format("Parses of '%s'", line));
            lv.addView("string");
            lv.pack();
            lv.setVisible(true);

            System.out.println();
        }

        System.out.println();
        System.exit(0);
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> grammarFilename = new ArrayList<>();

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }
    
    private static class MyFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.format("%s: %s\n", record.getLevel(), record.getMessage());
        }
    }
}
