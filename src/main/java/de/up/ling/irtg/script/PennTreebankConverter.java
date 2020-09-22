/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.BinarizingTreeWithAritiesAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.TreeWithAritiesAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.binarization.*;
import de.up.ling.irtg.codec.PtbTreeInputCodec;
import de.up.ling.irtg.codec.PtbTreeOutputCodec;
import de.up.ling.irtg.corpus.AbstractCorpusWriter;
import de.up.ling.irtg.corpus.CorpusConverter;
import de.up.ling.irtg.corpus.CorpusWriter;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.irtg.util.GuiUtils;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts a treebank in Penn Treebank format into an Alto corpus. Specify the
 * files which make up the treebank as command-line arguments. The converted
 * corpus is written to an Alto corpus file.<p>
 *
 * The conversion uses an implicit IRTG with an interpretation "string" (using a
 * {@link StringAlgebra}) and an interpretation "tree" representing the
 * phrase-structure trees (using a {@link TreeWithAritiesAlgebra})
 * .<p>
 *
 * @author koller
 */
public class PennTreebankConverter {
    // --binarize wsj00-bin.irtg --binarization-mode xbar --strip-annotations --remove-none --add-top --verbose -og wsj00.irtg -oc wsj00.txt /Users/koller/Documents/proj/corpora/pennTreebank/parsed/mrg/wsj/wsj.00.mrg

    private static final Map<String, Algebra> algebras = ImmutableMap.of("string", new StringAlgebra(), "tree", new TreeWithAritiesAlgebra());
    private static InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.forAlgebras(algebras);

    private static JCommander jc;

    public static void main(String[] args) throws Exception {
        CmdLineParameters param = new CmdLineParameters();

        jc = new JCommander(param);
        jc.parse(args);

        if (param.help) {
            usage(null);
        }

        if (param.inputFiles.isEmpty()) {
            usage("No input files specified.");
        }

        AbstractCorpusWriter cw = param.corpusWriterFromFilename(args);

        convert(cw, param);
    }

    public static void convert(AbstractCorpusWriter cw, CmdLineParameters param) throws Exception {
        PtbTreeInputCodec codec = new PtbTreeInputCodec();
        CorpusConverter<Tree<String>> converter = new CorpusConverter<>(cw,
                ImmutableMap.of("string", (Tree<String> tree) -> tree.getLeafLabels(), "tree", x -> x));

        // if leaves removed, can't skip leaves in DT construction
        DerivationTreeMaker dtm = new DerivationTreeMaker(!param.removeLeaves);
        converter.setDerivationTreeMaker(dtm);

        if (param.verbose) {
            System.err.println("Input files: " + Joiner.on(" ").join(param.inputFiles));
            System.err.println("Output grammar: " + param.outGrammarFilename);
            System.err.println("Output corpus: " + param.outCorpusFilename);
        }

        if (param.stripAnnotations) {
            converter.addTransformation(tnl(STRIP_ANNOTATIONS));
            if (param.verbose) {
                System.err.println("- strip annotations");
            }
        }

        if (param.removeNone) {
            converter.addTransformation(tnl(REMOVE_NONE));
            if (param.verbose) {
                System.err.println("- remove NONE");
            }
        }

        if (param.addTop) {
            converter.addTransformation(t -> Tree.create("TOP", t));
            if (param.verbose) {
                System.err.println("- add TOP");
            }
        }

        if (param.removeLeaves) {
            converter.addTransformation(makeWordsPos());
            if (param.verbose) {
                System.err.println("- remove leaves");
            }
        }

        if (param.verbose) {
            System.err.println();
        }

        withPtbCompressionConsumer(param, converter, corpusConsumer -> {
            try {
                long skipped = 0;
                long instanceNum = 1;

                for (String filename : param.inputFiles) {
                    InputStream corpus = new FileInputStream(filename);
                    System.err.println("Processing " + filename + " ...");

                    List<Tree<String>> c = codec.readCorpus(corpus);
                    for (Tree<String> t : c) {
                        try {
                            int len = t.getLeafLabels().size();

                            if (len <= param.maxLen) {
                                corpusConsumer.accept(t);
                            } else {
                                skipped++;
                            }

                            instanceNum++;
                        } catch (Exception e) {
                            System.err.println("Exception while reading instance #" + instanceNum);
                            System.err.println(t);
                            System.err.println("\nCause:");
                            System.err.println(e);
                            e.printStackTrace(System.err);
                            System.exit(3);
                        }
                    }
                }

                cw.close();

                if (skipped > 0) {
                    System.err.println(String.format("Done: %d instances, %d skipped for length.", instanceNum, skipped));
                } else {
                    System.err.println(String.format("Done: %d instances.", instanceNum));
                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
                System.exit(3);
            }
        });

        // perform MLE
        System.err.println("\nEstimate IRTG weights with Maximum Likelihood on new corpus ...");
        irtg.trainML(irtg.readCorpus(new FileReader(param.outCorpusFilename)));
        System.err.println("Done.");

        // binarize grammar if requested
        if (param.binarize) {
            System.err.println("\nBinarizing IRTG ...");

            Map<String, Algebra> newAlgebras = ImmutableMap.of("string", new StringAlgebra(), "tree", new BinarizingTreeWithAritiesAlgebra());
            Map<String, RegularSeed> seeds = ImmutableMap.of(
                    "string", new StringAlgebraSeed(irtg.getInterpretation("string").getAlgebra(), newAlgebras.get("string")),
                    "tree", new BinarizingAlgebraSeed(irtg.getInterpretation("tree").getAlgebra(), newAlgebras.get("tree")));
            
            Function<InterpretedTreeAutomaton, BinaryRuleFactory> rff = makeRuleFactoryFactory(param.binarizationMode);
            BkvBinarizer binarizer = new BkvBinarizer(seeds, rff);

            InterpretedTreeAutomaton binarized = GuiUtils.withConsoleProgressBar(60, System.out, listener -> {
                return binarizer.binarize(irtg, newAlgebras, listener);
            });

            irtg = binarized;
        }

        // write grammar to output file
        Writer w = new FileWriter(param.outGrammarFilename);
        w.write(irtg.toString());
        w.flush();
        w.close();

        // write the tree automaton to an output file
        irtg.getAutomaton().dumpToFile(param.outAutomatonFilename);
        System.err.println("Done.");
    }

    private static void withPtbCompressionConsumer(CmdLineParameters param, CorpusConverter<Tree<String>> converter, Consumer<Consumer<Tree<String>>> fn) throws IOException {
        if (param.compressPtb != null) {
            Writer ptbWriter = new FileWriter(param.compressPtb);
            PtbTreeOutputCodec oc = new PtbTreeOutputCodec();

            Consumer<Tree<String>> ptbCompressionConsumer = tree -> {
                try {
                    ptbWriter.write(oc.asString(tree));
                    ptbWriter.write("\n");
                } catch (IOException e) {
                    System.err.println(e);
                    System.exit(2);
                }
            };

//            Consumer<Tree<String>> cons = converter;
            converter.addConsumer(ptbCompressionConsumer);

            // send transformed tree to ptbCompressionConsumer for output
//            fn.accept(converter.andThen(ptbCompressionConsumer));
            fn.accept(converter);

            ptbWriter.flush();
            ptbWriter.close();
        } else {
            fn.accept(converter);
        }
    }

    private static Pattern ANNOTATION_PATTERN = Pattern.compile("([^\\-=]+)([\\-=].*)");

    private static Function<String, String> STRIP_ANNOTATIONS = (s -> {
        Matcher m = ANNOTATION_PATTERN.matcher(s);

        if (m.matches()) {
            return m.group(1);
        } else {
            return s;
        }
    });

    private static Function<String, String> REMOVE_NONE = (s -> {
        if (s.startsWith("-") || s.startsWith("*")) {
            return null;
        } else {
            return s;
        }
    });

    public static Function<InterpretedTreeAutomaton, BinaryRuleFactory> makeRuleFactoryFactory(String binarizationMode) {
        switch(binarizationMode) {
            case "complete": return GensymBinaryRuleFactory.createFactoryFactory();
            case "xbar": return XbarRuleFactory.createFactoryFactory();
            case "inside": return InsideRuleFactory.createFactoryFactory();
        }
        
        throw new UnsupportedOperationException("Undefined binarization mode: " + binarizationMode);
    }

    private static class CmdLineParameters {

        @Parameter
        public List<String> inputFiles = new ArrayList<>();

        @Parameter(names = {"--out-corpus", "-oc"}, description = "Filename to which the corpus will be written.")
        public String outCorpusFilename = "out.txt";

        @Parameter(names = {"--out-grammar", "-og"}, description = "Filename to which the grammar will be written.")
        public String outGrammarFilename = "out.irtg";

        @Parameter(names = {"--out-automaton", "-oa"}, description = "Filename to which the tree automaton will be written.")
        public String outAutomatonFilename = "out.auto";

        @Parameter(names = "--compress-ptb", description = "Also write PTB trees into given file, one tree per line (for evalb).")
        public String compressPtb = null;

        @Parameter(names = "--strip-annotations", description = "Convert NP-SBJ to NP etc.")
        public boolean stripAnnotations = false;

        @Parameter(names = "--remove-none", description = "Remove empty elements such as -NONE-.")
        public boolean removeNone = false;

        @Parameter(names = "--add-top", description = "Add a TOP symbol on top of every parse tree.")
        public boolean addTop = false;

        @Parameter(names = "--pos", description = "Remove all leaves, yielding strings of POS tags.")
        public boolean removeLeaves = false;

        @Parameter(names = "--binarize", description = "Binarize the output grammar.")
        public boolean binarize = false;

        @Parameter(names = "--binarization-mode", description = "Binarization mode (complete/xbar/inside).",
                validateWith = BinarizationStyleValidator.class)
        public String binarizationMode = "complete";

        @Parameter(names = "--verbose", description = "Print some debugging output.")
        public boolean verbose = false;

        @Parameter(names = "--help", help = true, description = "Prints usage information.")
        private boolean help;

        @Parameter(names = "--len", description = "Maximum length of an input")
        public int maxLen = 10000;

        AbstractCorpusWriter corpusWriterFromFilename(String[] args) throws IOException {
            Writer w = new FileWriter(outCorpusFilename);
            String joinedArgs = Joiner.on(" ").join(args);
            AbstractCorpusWriter cw = new CorpusWriter(irtg, "Converted on " + new Date().toString() + "\nArgs = " + joinedArgs, "/// ", w);
            return cw;
        }
    }

    private static void usage(String errorMessage) {
        if (jc != null) {
            if (errorMessage != null) {
                System.out.println("No input files specified.");
            }

            jc.setProgramName("java -cp <alto.jar> de.up.ling.irtg.script.PennTreebankConverter <inputfiles>");
            jc.usage();

            if (errorMessage != null) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    public static class BinarizationStyleValidator implements IParameterValidator {

        private static final List<String> allowed = Lists.newArrayList("complete", "xbar", "inside"); //, "outside");
        

        @Override
        public void validate(String key, String value) throws ParameterException {
            if (!allowed.contains(value)) {
                throw new ParameterException("Invalid value for argument 'binarizationMode'. Allowed values are: " + allowed);
            }
        }

    }

    private static Function<Tree<String>, Tree<String>> makeWordsPos() {
        return tree -> {
            return tree.dfs((node, children) -> {
                if (children.size() == 1 && children.get(0).getChildren().isEmpty()) {
                    // I am preterminal, forget my child.
                    return Tree.create(node.getLabel());
                } else {
                    return Tree.create(node.getLabel(), children);
                }
            });
        };
    }

    /**
     * Returns a function that applies fn to each node of a tree. If fn returns
     * null, the subtree is removed.
     *
     */
    private static Function<Tree<String>, Tree<String>> t(Function<String, String> fn) {
        return tree -> {
            return tree.dfs((node, children) -> {
                String mappedNodeLabel = fn.apply(node.getLabel());

                if (mappedNodeLabel == null) {
                    return null;
                }

                List<Tree<String>> nonNullChildren = children.stream().filter(ch -> ch != null).collect(Collectors.toList());

                return Tree.create(mappedNodeLabel, nonNullChildren);
            });
        };
    }

    /**
     * Returns a function that applies fn to each node of a tree, except for
     * leaves, which are left unchanged. If fn returns null, the subtree is
     * removed.
     *
     */
    private static Function<Tree<String>, Tree<String>> tnl(Function<String, String> fn) {
        return tree -> {
            return tree.dfs((node, children) -> {
                List<Tree<String>> nonNullChildren = children.stream().filter(ch -> ch != null).collect(Collectors.toList());

                if (nonNullChildren.isEmpty()) {
                    if (node.getChildren().isEmpty()) {
                        // node was originally a leaf => leave unchanged
                        return node;
                    } else {
                        // node had all its children removed => remove it too
                        return null;
                    }
                } else {
                    String mappedNodeLabel = fn.apply(node.getLabel());

                    if (mappedNodeLabel == null) {
                        return null;
                    }

                    return Tree.create(mappedNodeLabel, nonNullChildren);
                }
            });
        };
    }

    public static InterpretedTreeAutomaton getIrtg() {
        return irtg;
    }

    private static Map<String, String> readInCtfMapping(Reader r) throws IOException, ParseException {
        String s = StringTools.slurp(r);
        Tree<String> t = TreeParser.parse(s); // top-level tree with "___"
        Map<String, String> fineSymbolToCoarse = new HashMap<>();

        t.dfs((Tree<String> node, List<String> children) -> {
            if (node != t) {
                String label = node.getLabel();
                children.forEach(child -> {
                    fineSymbolToCoarse.put(child, label); // Exp: child NP | label N_
                    fineSymbolToCoarse.put(child + "-bar", label + "-bar"); // for xbar-binarized grammars
                });
                return label;
            } else {
                return null;
            }
        });
        return fineSymbolToCoarse;
    }

    private static class DerivationTreeMaker implements Function<Tree<String>, Tree<Integer>> {

        Interner<PtbRule> seenRules = new Interner<>();
        private ConcreteTreeAutomaton<String> auto;
        private boolean skipLeaves;

        public DerivationTreeMaker(boolean skipLeaves) {
//            irtg = InterpretedTreeAutomaton.forAlgebras(algebras);
            auto = (ConcreteTreeAutomaton) irtg.getAutomaton();
            this.skipLeaves = skipLeaves;
        }

        @Override
        public Tree<Integer> apply(Tree<String> derivedTree) {
//            System.err.println("\n******\n");
//            System.err.println("deriv tree: " + derivedTree);

            Tree<String> dt = derivedTree.dfs((node, children) -> {
//                System.err.println("call: " + node);
//                System.err.println("    " + children);

                if (skipLeaves && node.getChildren().isEmpty()) {
                    // leaf -- nothing to be done here
                    return null;
                } else {
                    PtbRule ruleHere = new PtbRule(node.getLabel(), Util.mapToList(node.getChildren(), ch -> ch.getLabel()));
                    int index = seenRules.addObject(ruleHere);
                    String label = "r" + index;

//                    System.err.println(label + ": " + ruleHere);
//                    System.err.println("    " + children);
                    List<String> ruleChildren = new ArrayList<>();           // for the RTG rule
                    List<Tree<String>> derivTreeChildren = new ArrayList<>();      // for the deriv tree
                    List<Tree<String>> homChildren = new ArrayList<>();            // for the homomorphisms
                    int nextVar = 1;

                    for (int i = 0; i < children.size(); i++) {
                        Tree<String> ch = children.get(i);
                        Tree<String> originalChild = node.getChildren().get(i);

                        Tree<String> homChild = (ch == null) ? originalChild : Tree.create("?" + (nextVar++));
                        homChildren.add(homChild);

                        if (ch != null) {
                            ruleChildren.add(originalChild.getLabel());      // root nonterminal
                            derivTreeChildren.add(ch);
                        }
                    }

                    Rule rule = auto.createRule(node.getLabel(), label, ruleChildren);
                    auto.addRule(rule);

                    boolean isLeaf = children.isEmpty();
                    Tree st = Tree.create(isLeaf ? node.getLabel() : "conc" + homChildren.size(), homChildren);
                    irtg.getInterpretation("string").getHomomorphism().add(label, st);
                    irtg.getInterpretation("tree").getHomomorphism().add(label, TreeWithAritiesAlgebra.addArities(Tree.create(node.getLabel(), homChildren)));

//                    System.err.println(irtg.getInterpretation("string").getHomomorphism().get(label));
                    Homomorphism hom = irtg.getInterpretation("string").getHomomorphism();
                    Tree<HomomorphismSymbol> rhs = hom.get(rule.getLabel());
//                    System.err.println(hom.rhsAsString(rhs));

                    if (node == derivedTree) {
                        auto.addFinalState(rule.getParent());
                    }

                    return Tree.create(label, derivTreeChildren);
                }
            });

            return irtg.getAutomaton().getSignature().addAllSymbols(dt);
        }
    }

    private static class PtbRule {

        String lhs;
        List<String> rhs;

        public PtbRule(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + Objects.hashCode(this.lhs);
            hash = 47 * hash + Objects.hashCode(this.rhs);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PtbRule other = (PtbRule) obj;
            if (!Objects.equals(this.lhs, other.lhs)) {
                return false;
            }
            return Objects.equals(this.rhs, other.rhs);
        }

        @Override
        public String toString() {
            return lhs + " -> " + rhs;
        }

    }
}
