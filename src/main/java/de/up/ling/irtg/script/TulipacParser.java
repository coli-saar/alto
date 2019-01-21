/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import de.saar.coli.featstruct.FeatureStructure;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.BinarizingTagTreeAlgebra;
import de.up.ling.irtg.algebra.FeatureStructureAlgebra;
import de.up.ling.irtg.algebra.TagStringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.binarization.BinarizingAlgebraSeed;
import de.up.ling.irtg.binarization.BinaryRuleFactory;
import de.up.ling.irtg.binarization.BkvBinarizer;
import de.up.ling.irtg.binarization.GensymBinaryRuleFactory;
import de.up.ling.irtg.binarization.IdentitySeed;
import de.up.ling.irtg.binarization.RegularSeed;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.gui.JLanguageViewer;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private static boolean hasFeatures;

    private static JCommander jc;
    private static CmdLineParameters param = new CmdLineParameters();

    private static void reloadGrammar() throws Exception {
        System.err.printf("Reading grammar from %s ...\n", filename);

        long start = System.nanoTime();
        InputCodec<InterpretedTreeAutomaton> ic = InputCodec.getInputCodecByNameOrExtension(filename, null);
        irtg = ic.read(new FileInputStream(filename));
        hasFeatures = irtg.getInterpretations().containsKey("ft");
        System.err.printf("Done, read %s grammar in %s\n\n", hasFeatures ? "FTAG" : "TAG", Util.formatTimeSince(start));

        if (param.binarize) {
            irtg = binarize(irtg);
        }

        sa = (TagStringAlgebra) irtg.getInterpretation("string").getAlgebra();

        if (hasFeatures) {
            fh = irtg.getInterpretation("ft").getHomomorphism();
            fsa = (FeatureStructureAlgebra) irtg.getInterpretation("ft").getAlgebra();
        }
    }

    private static InterpretedTreeAutomaton binarize(final InterpretedTreeAutomaton irtg) throws Exception {
        long start = System.nanoTime();
        System.err.println("Binarizing grammar ...");

        Map<String, Algebra> newAlgebras;
        Map<String, RegularSeed> seeds;

        if (hasFeatures) {
            newAlgebras = ImmutableMap.of(
                    "string", new TagStringAlgebra(),
                    "tree", new BinarizingTagTreeAlgebra(),
                    "ft", new FeatureStructureAlgebra());

            seeds = ImmutableMap.of(
                    "string", new IdentitySeed(irtg.getInterpretation("string").getAlgebra(), newAlgebras.get("string")),
                    "ft", new IdentitySeed(irtg.getInterpretation("ft").getAlgebra(), newAlgebras.get("ft")),
                    "tree", new BinarizingAlgebraSeed(irtg.getInterpretation("tree").getAlgebra(), newAlgebras.get("tree")));
        } else {
            newAlgebras = ImmutableMap.of(
                    "string", new TagStringAlgebra(),
                    "tree", new BinarizingTagTreeAlgebra());

            seeds = ImmutableMap.of(
                    "string", new IdentitySeed(irtg.getInterpretation("string").getAlgebra(), newAlgebras.get("string")),
                    "tree", new BinarizingAlgebraSeed(irtg.getInterpretation("tree").getAlgebra(), newAlgebras.get("tree")));
        }

        Function<InterpretedTreeAutomaton, BinaryRuleFactory> rff = GensymBinaryRuleFactory.createFactoryFactory(); //PennTreebankConverter.makeRuleFactoryFactory("complete");
        BkvBinarizer binarizer = new BkvBinarizer(seeds, rff);

        InterpretedTreeAutomaton binarized = GuiUtils.withConsoleProgressBar(60, System.out, listener -> {
            return binarizer.binarize(irtg, newAlgebras, listener);
        });

        System.err.printf("Done, binarized grammar in %s\n\n", Util.formatTimeSince(start));

        return binarized;
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

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.TulipacParser <grammar_filename>");
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

        System.err.println("Alto tulipac-style TAG parser, v1.1");
        System.err.println("Type a sentence to parse it, or type 'help' for help.\n");

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
                System.out.printf("No parse found (even while ignoring features).\n");
                System.out.println();
                continue mainLoop;
            }

            if (hasFeatures) {
                start = System.nanoTime();
                filtered = chart.intersect(fsa.nullFilter().inverseHomomorphism(fh));
                System.out.printf("filtered chart for feature structures: %s\n", Util.formatTimeSince(start));

                if (filtered.viterbi() == null) {
                    System.out.printf("Found parses, but they all violate the constraints from the feature structures.\n");
                    debugAnalysis(dt, irtg);
                    System.out.println();
                    continue mainLoop;
                }
            }

            JLanguageViewer lv = new JLanguageViewer();
            lv.setAutomaton(filtered, irtg);
            lv.setTitle(String.format("Parses of '%s'", line));
            lv.addView("tree");
            if (hasFeatures) {
                lv.addView("ft");
            }
            lv.pack();
            lv.setVisible(true);

            System.out.println();
        }

        System.out.println();
        System.exit(0);
    }

    private static void debugAnalysis(Tree<String> dt, InterpretedTreeAutomaton irtg) {
        Interpretation<FeatureStructure> fsi = (Interpretation<FeatureStructure>)irtg.getInterpretation("ft");
        List<Tree<String>> failureNodes = new ArrayList<>(); // collect all nodes of dt where unification failed

        // node: subtree of dt at this point
        // children: list of FSs for the children
        dt.dfs((node, children) -> {
            if (children.stream().allMatch((ch) -> ch != null)) {  // all children unified ok
                FeatureStructure fs = fsi.interpret(node);

                if (fs == null) {
                    // found a place where the unification failed
                    failureNodes.add(node);
                }

                return fs;
            } else {
                return null;
            }
        });

        System.out.printf("\nExample of a derivation tree that did not unify (>> marks point of unification failure):\n");
        printTree(dt, failureNodes.get(0), 0);

        dt.dfs((node, children) -> {
            if (node == failureNodes.get(0)) {
                System.out.printf("\n\nFeature structures for children:\n");

                for (int i = 0; i < children.size(); i++) {
                    System.out.printf("(%d) %s\n", i + 1, children.get(i));
                }
            }

            if (children.stream().allMatch((ch) -> ch != null)) {  // all children unified ok
                FeatureStructure fs = fsi.interpret(node);
                return fs;
            } else {
                return null;
            }
        });
    }

    private static void printTree(Tree<String> t, Tree<String> markedNode, int depth) {
        String prefix = (depth == 0) ? "" : "|" + Util.repeat("-", depth);
        String marker = (t == markedNode) ? ">> " : "   ";

        System.out.printf("%s%s%s\n", marker, prefix, t.getLabel());

        for (int i = 0; i < t.getChildren().size(); i++) {
            printTree(t.getChildren().get(i), markedNode, depth + 3);
        }
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> grammarFilename = new ArrayList<>();

        @Parameter(names = "--binarize", description = "Binarize the grammar after loading.")
        public boolean binarize = false;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;
    }
}
