package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.CogsOutputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;
import me.tongfei.progressbar.ProgressBar;

import static de.saar.coli.algebra.OrderedFeatureTreeAlgebra.OrderedFeatureTree;

import java.io.*;
import java.util.*;

public class CogsCorpusGenerator {
    public static void main(String[] args) throws IOException {
        Args cmd = new Args();
        JCommander.newBuilder().addObject(cmd).build().parse(args);

        System.err.printf("Generating corpus with %d instances.\n", cmd.count);

        // constrain PP embedding depth
        String[] ppDepthParts = cmd.ppDepth.strip().split("-");
        if( ppDepthParts.length != 2 ) {
            System.err.println("PP depth specification must be of the form <min>-<max>.");
            System.exit(1);
        }

        int ppMinDepth = Integer.parseInt(ppDepthParts[0]);
        int ppMaxDepth = Integer.parseInt(ppDepthParts[1]);
        System.err.printf("- with PP embedding depth min=%d, max=%d\n", ppMinDepth, ppMaxDepth);

        // constrain CP embedding depth
        String[] cpDepthParts = cmd.cpDepth.strip().split("-");
        if( cpDepthParts.length != 2 ) {
            System.err.println("CP depth specification must be of the form <min>-<max>.");
            System.exit(1);
        }

        int cpMinDepth = Integer.parseInt(cpDepthParts[0]);
        int cpMaxDepth = Integer.parseInt(cpDepthParts[1]);
        System.err.printf("- with CP embedding depth min=%d, max=%d\n", cpMinDepth, cpMaxDepth);

        // read previous sentences
        Set<String> previousSentences = new HashSet<>();

        if( cmd.previousInstances != null ) {
            BufferedReader r = new BufferedReader(new FileReader(cmd.previousInstances));
            String line = null;

            while ( (line = r.readLine()) != null ) {
                String sentence = line.strip().split("\\t")[0];
                previousSentences.add(sentence);
            }
        }

        // generate corpus
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(new FileInputStream(cmd.parameters.get(0)));
        OutputCodec<OrderedFeatureTree> oc = new CogsOutputCodec();
        int countSamplingErrors = 0, skippedForDepth = 0, skippedAsDuplicate = 0;
        System.err.println();

        try (ProgressBar pb = new ProgressBar("Generating corpus", cmd.count)) {
            for (int i = 0; i < cmd.count; i++) {
                pb.stepTo(i);

                try {
                    Tree<Rule> ruleTree = irtg.getAutomaton().getRandomRuleTreeFromRuleProbabilities();
                    assert ruleTree != null;

                    // this is only up here for debugging purposes, move down when everything works
                    Tree<String> dt = Util.mapTree(ruleTree, rule -> rule.getLabel(irtg.getAutomaton()));

                    List<String> englishValue = (List<String>) irtg.interpret(dt, "english");
                    String english = StringTools.join(englishValue, " ");

                    OrderedFeatureTree ft = (OrderedFeatureTree) irtg.interpret(dt, "semantics");

                    // skip duplicates
                    if (cmd.suppressDuplicates) {
                        if (!previousSentences.add(english)) {
                            i--;
                            skippedAsDuplicate++;
                            continue;
                        }
                    }

                    // If sentence uses the same noun twice, skip it.
                    List<String> nouns = collectNouns(ruleTree, irtg.getAutomaton(), irtg.getInterpretation("english").getHomomorphism());
                    if (nouns.size() != new HashSet<String>(nouns).size()) {
                        i--;
                        continue;
                    }

                    // filter instances for depths
                    Tree<String> nonterminalTree = getNonterminalTree(ruleTree, irtg.getAutomaton());
                    int ppDepth = getEmbeddingDepth(nonterminalTree, "PP_");
                    if (ppDepth < ppMinDepth || ppDepth > ppMaxDepth) {
                        i--;
                        skippedForDepth++;
                        continue;
                    }

                    int cpDepth = getEmbeddingDepth(nonterminalTree, "S") - 1;
                    if (cpDepth < cpMinDepth || cpDepth > cpMaxDepth) {
                        i--;
                        skippedForDepth++;
                        continue;
                    }

                    if( cmd.printDerivations) {
                        System.out.printf("%s\t%s\t%s\n", english, oc.asString(ft), dt);
                    } else {
                        System.out.printf("%s\t%s\n", english, oc.asString(ft));
                    }
                } catch (RuntimeException e) {
                    countSamplingErrors++;
                    i--;
                }
            }

            pb.stepTo(cmd.count);
        }

        System.err.printf("Generated %d instances.\n", cmd.count);
        if( countSamplingErrors > 0 ) {
            System.err.printf("- %d sampling errors\n", countSamplingErrors);
        }
        if( skippedAsDuplicate > 0 ) {
            System.err.printf("- %d duplicates skipped\n", skippedAsDuplicate);
        }
        if( skippedForDepth > 0 ) {
            System.err.printf("- %d skipped for depth\n", skippedForDepth);
        }

//        System.err.printf("Encountered %d sampling errors while sampling %d trees.\n", countSamplingErrors, cmd.count);
    }


    /**
     * Returns the number of occurrences of a nonterminal in a path of the given tree.
     * More specifically, the method determines the number of nodes on each path of the tree
     * whose labels start with "prefix", and returns the maximum count over all paths.
     *
     * @param nonterminalTree
     * @param prefix
     * @return
     */
    private static int getEmbeddingDepth(Tree<String> nonterminalTree, String prefix) {
        return nonterminalTree.dfs((node, childMaxDepths) -> {
            if( childMaxDepths.isEmpty() ) {
                return 0;
            } else {
                int maxChildDepth = max(childMaxDepths);
                if (node.getLabel().startsWith(prefix)) {
                    return maxChildDepth + 1;
                } else {
                    return maxChildDepth;
                }
            }
        });
    }

    private static int max(List<Integer> ints) {
        int ret = Integer.MIN_VALUE;
        for( int x : ints ) {
            ret = Integer.max(ret, x);
        }
        return ret;
    }

    private static Tree<String> getNonterminalTree(Tree<Rule> ruleTree, TreeAutomaton<String> auto) {
        return ruleTree.dfs((node, children) -> {
            Rule rule = node.getLabel();
            String lhs = auto.getStateForId(rule.getParent());
            return Tree.create(lhs, children);
        });
    }

    private static List<String> collectNouns(Tree<Rule> ruleTree, TreeAutomaton<String> auto, Homomorphism stringHomomorphism) {
        List<String> ret = new ArrayList<>();

        ruleTree.dfs((node, children) -> {
            Rule rule = node.getLabel();
            String lhs = auto.getStateForId(rule.getParent());

            if( lhs.startsWith("N_")  ) {
                Tree<HomomorphismSymbol> term = stringHomomorphism.get(rule.getLabel());
                String word = stringHomomorphism.getTargetSignature().resolveSymbolId(term.getLabel().getValue());
                ret.add(word);
            }

            return null;
        });

        return ret;
    }

    public static class Args {
        @Parameter
        private List<String> parameters = new ArrayList<>();

        @Parameter(names = "--count", description="How many instances to generate")
        private int count = 10;

        @Parameter(names = "--suppress-duplicates", description = "Prevents generating the same sentence twice")
        private boolean suppressDuplicates = false;

        @Parameter(names = "--print-derivations", description = "Print derivation tree for each instance")
        private boolean printDerivations = false;

        @Parameter(names = "--previous-instances", description = "Sentences in this previously generated corpus count as 'duplicates'")
        private String previousInstances = null;

        @Parameter(names = "--pp-depth", description = "Limit PP recursion depth to this range (min-max)")
        private String ppDepth = "0-100";

        @Parameter(names = "--cp-depth", description = "Limit CP recursion depth to this range (min-max)")
        private String cpDepth = "0-100";


    }
}
