/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.saar.coli.featstruct.FeatureStructure;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.FeatureStructureAlgebra;
import de.up.ling.irtg.algebra.TagStringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.gui.JLanguageViewer;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jline.console.ConsoleReader;

/**
 *
 * @author koller
 */
public class TulipacParser {
    private static String filename;
    private static InterpretedTreeAutomaton irtg;
    private static FeatureStructureAlgebra fsa;
    private static TagStringAlgebra sa;
    private static Homomorphism fh;

    private static void reloadGrammar() throws IOException, Exception {
        System.err.printf("Reading grammar from %s ...\n", filename);

        long start = System.nanoTime();
        InputCodec<InterpretedTreeAutomaton> ic = InputCodec.getInputCodecByNameOrExtension(filename, null);
        irtg = ic.read(new FileInputStream(filename));
        System.err.printf("Done, read grammar in %s\n\n", Util.formatTimeSince(start));

        fsa = (FeatureStructureAlgebra) irtg.getInterpretation("ft").getAlgebra();
        sa = (TagStringAlgebra) irtg.getInterpretation("string").getAlgebra();
        fh = irtg.getInterpretation("ft").getHomomorphism();
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

    private static interface CommandAction {
        public void perform() throws Exception;
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

    public static void main(String[] args) throws IOException, Exception {
        System.err.println("Alto tulipac-style TAG parser, v1.0");
        System.err.println("Type a sentence to parse it, or type 'help' for help.\n");

        filename = args[0];
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
            System.out.printf("computed chart: %s\n", Util.formatTimeSince(start));

            Tree<String> dt = chart.viterbi();
            if (dt == null) {
                System.out.printf("No parse found (even while ignoring features).\n");
                System.out.println();
                continue mainLoop;
            }

            start = System.nanoTime();
            TreeAutomaton filtered = chart.intersect(fsa.nullFilter().inverseHomomorphism(fh));
            System.out.printf("filtered chart for feature structures: %s\n", Util.formatTimeSince(start));

            if (filtered.viterbi() == null) {
                System.out.printf("Found parses, but they all violate the constraints from the feature structures.\n");
                debugAnalysis(dt, irtg);
                System.out.println();
                continue mainLoop;
            }

            JLanguageViewer lv = new JLanguageViewer();
            lv.setAutomaton(filtered, irtg);
            lv.setTitle(String.format("Parses of '%s'", line));
            lv.addView("tree");
            lv.addView("ft");
            lv.pack();
            lv.setVisible(true);
            
            System.out.println();
        }

        System.out.println();
        System.exit(0);
    }

    private static void debugAnalysis(Tree<String> dt, InterpretedTreeAutomaton irtg) {
        Interpretation<FeatureStructure> fsi = irtg.getInterpretation("ft");

        System.out.printf("\nExample of a derivation tree that did not unify:\n\n");
        printTree(dt, 0);

        // node: subtree of dt at this point
        // children: list of FSs for the children
        dt.dfs((node, children) -> {
            if (children.stream().allMatch((ch) -> ch != null)) {  // all children unified ok
                FeatureStructure fs = fsi.interpret(node);

                if (fs == null) {
                    // found a place where the unification failed
                    System.out.printf("\nUnification failed at this subtree:\n\n");
                    printTree(node, 0);

                    System.out.printf("\n\nFeature structures for children:\n");

                    for (int i = 0; i < children.size(); i++) {
                        System.out.printf("(%d) %s\n", i + 1, children.get(i));
                    }
                }

                return fs;
            } else {
                return null;
            }
        });
    }

    private static void printTree(Tree<String> t, int depth) {
        String prefix = (depth == 0) ? "" : "|" + Util.repeat("-", depth);

        System.out.printf("%s%s\n", prefix, t.getLabel());

        for (int i = 0; i < t.getChildren().size(); i++) {
            printTree(t.getChildren().get(i), depth + 3);
        }
    }
}
