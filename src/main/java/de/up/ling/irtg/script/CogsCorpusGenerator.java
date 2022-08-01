package de.up.ling.irtg.script;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.saar.basic.StringTools;
import de.saar.coli.algebra.OrderedFeatureTreeAlgebra;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.util.Util;
import de.up.ling.tree.Tree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CogsCorpusGenerator {
    public static void main(String[] args) throws IOException {
        Args cmd = new Args();
        JCommander.newBuilder().addObject(cmd).build().parse(args);

        System.err.println("x");

        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(new FileInputStream(cmd.parameters.get(0)));
        Map<Integer, Double> inside = irtg.getAutomaton().inside();

        for( int i = 0; i < cmd.count; i++ ) {
            Tree<Rule> ruleTree = irtg.getAutomaton().getRandomRuleTree(inside);

            // this is only up here for debugging purposes, move down when everything works
            Tree<String> dt = Util.mapTree(ruleTree, rule -> rule.getLabel(irtg.getAutomaton()));
            List<String> englishValue = (List<String>) irtg.interpret(dt, "english");
            String english = StringTools.join(englishValue, " ");
            OrderedFeatureTreeAlgebra.OrderedFeatureTree ft = (OrderedFeatureTreeAlgebra.OrderedFeatureTree) irtg.interpret(dt, "semantics");


            // If sentence uses the same noun twice, skip it.
            List<String> nouns = collectNouns(ruleTree, irtg.getAutomaton(), irtg.getInterpretation("english").getHomomorphism());
            if( nouns.size() != new HashSet<String>(nouns).size() ) {
                continue;
            }

            // TODO Filter the rule trees based on PP/CP embedding depth etc.

            System.out.printf("%s\t%s\n", english, ft.toString(true));
        }
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
    }
}
